package dev.lovelace.loveduels.spectator;

import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.core.InventorySnapshot;
import dev.lovelace.loveduels.integration.LoveEconomyBridge;
import dev.lovelace.loveduels.match.Match;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SimpleSpectatorManager implements SpectatorManager {

    private final Plugin plugin;
    private final SpectatorZoneGuard zoneGuard;
    private final LoveEconomyBridge economyBridge;

    private final Map<UUID, Match> spectatingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, InventorySnapshot> spectatorSnapshots = new ConcurrentHashMap<>();

    private final int freeSlots = 15;
    private final long basePrice = 50;
    private final long stepPrice = 25;

    public SimpleSpectatorManager(Plugin plugin, LoveEconomyBridge economyBridge) {
        this.plugin = plugin;
        this.zoneGuard = new SpectatorZoneGuard(this);
        this.economyBridge = economyBridge;
    }

    public SpectatorZoneGuard getZoneGuard() {
        return zoneGuard;
    }

    @Override
    public long calculateSpectatorFee(Match match) {
        int current = match.getSpectators().size();
        if (current < freeSlots) {
            return 0L;
        }
        int extra = current - freeSlots;
        return basePrice + (extra * stepPrice);
    }

    @Override
    public boolean addSpectator(Player player, Match match) {
        if (match == null || match.isEnded()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Эта дуэль уже завершилась."));
            return false;
        }

        UUID uuid = player.getUniqueId();
        if (isSpectating(uuid)) {
            removeSpectator(player);
        }

        // Check fee
        long fee = calculateSpectatorFee(match);
        if (fee > 0) {
            if (!economyBridge.charge(player, fee)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>❌ Недостаточно монет для покупки места зрителя! Требуется: <gold>" + fee + " монет."
                ));
                return false;
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>✔ Вы приобрели место на трибунах за <gold>" + fee + " монет!</gold>"
            ));
        }

        // Take snapshot to restore afterwards
        spectatorSnapshots.put(uuid, InventorySnapshot.of(player));
        spectatingPlayers.put(uuid, match);
        match.addSpectator(player);

        Arena arena = match.getArena();
        zoneGuard.registerZone(uuid, arena);

        // Put in spectator mode
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.setCollidable(false);

        boolean canFly = player.hasPermission("loveduels.spectator.fly");
        player.setAllowFlight(canFly);
        player.setFlying(canFly);

        // Give hotbar exit item in slot 8
        player.getInventory().setItem(8, createLeaveItem());
        player.getInventory().setHeldItemSlot(8);

        // Teleport to spectator spawn
        Location spawn = arena.getSafeSpectatorPoint();
        if (spawn != null) {
            player.teleportAsync(spawn);
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>👁 Вы перешли в режим наблюдения за дуэлью <white>" +
                match.getPlayer1().getName() + " <gray>vs <white>" + match.getPlayer2().getName() + "!"
        ));
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<yellow>Используйте предмет в хотбаре или команду <aqua>/duel leave</aqua>, чтобы выйти."
        ));

        return true;
    }

    @Override
    public void removeSpectator(Player player) {
        UUID uuid = player.getUniqueId();
        Match match = spectatingPlayers.remove(uuid);
        if (match != null) {
            match.removeSpectator(player);
        }

        zoneGuard.unregisterZone(uuid);

        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setCollidable(true);

        InventorySnapshot snap = spectatorSnapshots.remove(uuid);
        if (snap != null) {
            snap.restore(player);
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Вы покинули режим наблюдения."));
    }

    @Override
    public boolean isSpectating(UUID player) {
        return spectatingPlayers.containsKey(player);
    }

    @Override
    public Optional<Match> getSpectatedMatch(UUID player) {
        return Optional.ofNullable(spectatingPlayers.get(player));
    }

    public void cleanupAll() {
        for (UUID uuid : new java.util.ArrayList<>(spectatingPlayers.keySet())) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                removeSpectator(p);
            } else {
                spectatingPlayers.remove(uuid);
                spectatorSnapshots.remove(uuid);
                zoneGuard.unregisterZone(uuid);
            }
        }
    }

    private ItemStack createLeaveItem() {
        ItemStack item = new ItemStack(Material.RED_BED);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(
                    "<red><b>Покинуть наблюдение</b> <gray>(ПКМ)"
            ).decoration(TextDecoration.ITALIC, false));
            meta.lore(java.util.List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Нажмите, чтобы вернуться назад.").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
