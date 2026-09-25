package dev.lovelace.loveduels.spectator;

import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.match.Match;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.util.BoundingBox;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strict boundary enforcer for spectators. Prevents spectators from clipping out,
 * falling below the arena, or interfering with duels.
 */
public final class SpectatorZoneGuard implements Listener {

    private final SpectatorManager spectatorManager;
    private final Map<UUID, BoundingBox> spectatorZones = new ConcurrentHashMap<>();
    private final Map<UUID, Arena> activeArenas = new ConcurrentHashMap<>();

    public SpectatorZoneGuard(SpectatorManager spectatorManager) {
        this.spectatorManager = spectatorManager;
    }

    public void registerZone(UUID uuid, Arena arena) {
        if (arena != null) {
            BoundingBox box = arena.getSpectatorZone();
            if (box == null && arena.getBounds() != null) {
                box = arena.getBounds().clone().expand(5.0);
            }
            if (box != null) {
                spectatorZones.put(uuid, box);
            }
            activeArenas.put(uuid, arena);
        }
    }

    public void unregisterZone(UUID uuid) {
        spectatorZones.remove(uuid);
        activeArenas.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BoundingBox zone = spectatorZones.get(player.getUniqueId());
        if (zone == null) return;

        Location to = event.getTo();
        if (to == null) return;

        // Check if player left spectator zone or went under map
        if (to.getY() < zone.getMinY() - 0.5 || !zone.contains(to.getX(), to.getY(), to.getZ())) {
            event.setCancelled(true);

            Arena arena = activeArenas.get(player.getUniqueId());
            Location safe = (arena != null) ? arena.getSafeSpectatorPoint() : null;
            if (safe == null) {
                safe = getClosestSafePoint(zone, to, player.getWorld());
            }

            player.teleportAsync(safe);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>⚠ Вы вышли за пределы зоны наблюдения! Вас вернули на трибуну."
            ));
        }
    }

    private Location getClosestSafePoint(BoundingBox zone, Location from, org.bukkit.World world) {
        double clampedX = Math.max(zone.getMinX() + 0.5, Math.min(zone.getMaxX() - 0.5, from.getX()));
        double clampedY = Math.max(zone.getMinY() + 0.5, Math.min(zone.getMaxY() - 0.5, from.getY()));
        double clampedZ = Math.max(zone.getMinZ() + 0.5, Math.min(zone.getMaxZ() - 0.5, from.getZ()));
        return new Location(world, clampedX, clampedY, clampedZ, from.getYaw(), from.getPitch());
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!spectatorZones.containsKey(player.getUniqueId())) return;

        if (!player.hasPermission("loveduels.spectator.fly")) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>⚠ У вас нет права свободного полёта в режиме зрителя!"
            ));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (spectatorManager.isSpectating(player.getUniqueId())) {
            // Check if player clicked the leave item (slot 8)
            if (event.getItem() != null && event.getItem().hasItemMeta()
                    && event.getItem().getItemMeta().displayName() != null) {
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(event.getItem().getItemMeta().displayName());
                if (plain.contains("Покинуть") || plain.contains("Leave")) {
                    event.setCancelled(true);
                    spectatorManager.removeSpectator(player);
                    return;
                }
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (spectatorManager.isSpectating(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (spectatorManager.isSpectating(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && spectatorManager.isSpectating(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && spectatorManager.isSpectating(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p && spectatorManager.isSpectating(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (spectatorManager.isSpectating(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (spectatorManager.isSpectating(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
