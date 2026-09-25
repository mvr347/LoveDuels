package dev.lovelace.loveduels.match;

import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.core.DuelBet;
import dev.lovelace.loveduels.core.DuelType;
import dev.lovelace.loveduels.integration.LoveBehaviorBridge;
import dev.lovelace.loveduels.integration.LoveEconomyBridge;
import dev.lovelace.loveduels.integration.LoveLeaderboardsBridge;
import dev.lovelace.loveduels.match.spear.HorseStaminaHandler;
import dev.lovelace.loveduels.match.spear.SpearChargeHandler;
import dev.lovelace.loveduels.match.spear.SpearItem;
import dev.lovelace.loveduels.royal.RoyalDuelManager;
import dev.lovelace.loveduels.storage.PlayerStorage;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Specialized match implementation for Horse + Spear jousting duels.
 */
public final class HorseSpearMatch extends AbstractMatch {

    private final SpearItem spearItem;
    private final SpearChargeHandler chargeHandler;
    private final HorseStaminaHandler staminaHandler;

    private final Map<UUID, Horse> tournamentHorses = new ConcurrentHashMap<>();
    private BukkitTask staminaTask;

    public HorseSpearMatch(
            Plugin plugin,
            Player player1,
            Player player2,
            Arena arena,
            DuelBet bet,
            boolean royal,
            PlayerStorage playerStorage,
            LoveEconomyBridge economyBridge,
            LoveBehaviorBridge behaviorBridge,
            LoveLeaderboardsBridge leaderboardsBridge,
            RoyalDuelManager royalManager,
            MatchManager matchManager,
            SpearItem spearItem,
            SpearChargeHandler chargeHandler,
            HorseStaminaHandler staminaHandler
    ) {
        super(plugin, player1, player2, arena, DuelType.HORSE_SPEAR, bet, royal, playerStorage, economyBridge, behaviorBridge, leaderboardsBridge, royalManager, matchManager);
        this.spearItem = spearItem;
        this.chargeHandler = chargeHandler;
        this.staminaHandler = staminaHandler;
    }

    public SpearChargeHandler getChargeHandler() {
        return chargeHandler;
    }

    public HorseStaminaHandler getStaminaHandler() {
        return staminaHandler;
    }

    public Horse getHorse(UUID playerId) {
        return tournamentHorses.get(playerId);
    }

    public boolean isTournamentHorse(Horse horse) {
        return tournamentHorses.containsValue(horse);
    }

    @Override
    protected void setupEquipment() {
        // Clear items and give spear
        equipJousting(player1);
        equipJousting(player2);

        // Spawn tournament horses
        Horse h1 = spawnHorse(player1, arena.getPos1());
        Horse h2 = spawnHorse(player2, arena.getPos2());

        tournamentHorses.put(player1Id, h1);
        tournamentHorses.put(player2Id, h2);

        h1.addPassenger(player1);
        h2.addPassenger(player2);

        // Start stamina monitoring task
        staminaTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (isEnded()) {
                    cancel();
                    return;
                }
                if (player1.isOnline() && h1.isValid()) {
                    staminaHandler.tick(player1, h1);
                }
                if (player2.isOnline() && h2.isValid()) {
                    staminaHandler.tick(player2, h2);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void equipJousting(Player player) {
        player.getInventory().clear();
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

        ItemStack spear = spearItem.create();
        player.getInventory().setItem(0, spear);
        player.getInventory().setHeldItemSlot(0);
    }

    private Horse spawnHorse(Player rider, org.bukkit.Location spawnLoc) {
        Horse horse = (Horse) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.HORSE);
        horse.setTamed(true);
        horse.setOwner(rider);
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.getInventory().setArmor(new ItemStack(Material.IRON_HORSE_ARMOR));
        horse.setColor(Horse.Color.WHITE);
        horse.setStyle(Horse.Style.NONE);

        double horseSpeed = plugin.getConfig().getDouble("horse_spear.horse_speed", 0.32);
        var speedAttr = horse.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(horseSpeed);
        }

        var jumpAttr = horse.getAttribute(Attribute.JUMP_STRENGTH);
        if (jumpAttr != null) {
            jumpAttr.setBaseValue(0.8);
        }

        var healthAttr = horse.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(50.0);
            horse.setHealth(50.0);
        }

        horse.customName(MiniMessage.miniMessage().deserialize("<gold>Боевой конь: <white>" + rider.getName()));
        horse.setCustomNameVisible(true);
        return horse;
    }

    public void handleDismount(Player player) {
        if (isEnded()) return;
        Horse horse = tournamentHorses.get(player.getUniqueId());
        if (horse != null && horse.isValid()) {
            horse.addPassenger(player);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>⚠ Запрещено слезать с коня во время турнира!"));
        }
    }

    public void handleHorseDeath(Horse horse) {
        if (isEnded()) return;
        for (Map.Entry<UUID, Horse> entry : tournamentHorses.entrySet()) {
            if (entry.getValue().equals(horse)) {
                UUID deadRider = entry.getKey();
                Player opponent = getOpponent(deadRider);
                UUID winnerId = (opponent != null) ? opponent.getUniqueId() : null;
                end(winnerId, MatchEndReason.KILL);
                return;
            }
        }
    }

    @Override
    protected void cleanupCustomEntities() {
        if (staminaTask != null) {
            staminaTask.cancel();
        }

        for (Horse horse : tournamentHorses.values()) {
            if (horse != null && horse.isValid()) {
                horse.eject();
                horse.remove();
            }
        }
        tournamentHorses.clear();

        staminaHandler.remove(player1);
        staminaHandler.remove(player2);
        chargeHandler.cancelCharge(player1);
        chargeHandler.cancelCharge(player2);
    }
}
