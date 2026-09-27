package dev.lovelace.loveduels.arena;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

/**
 * Temporary particle outline of arena combat bounds, spawns and spectator zone.
 */
public final class ArenaParticlePreview {

    private ArenaParticlePreview() {}

    public static void show(Plugin plugin, Player viewer, Arena arena, int seconds) {
        if (arena == null || viewer == null) return;
        int ticks = Math.max(20, seconds * 20);

        new BukkitRunnable() {
            int left = ticks;

            @Override
            public void run() {
                if (left <= 0 || !viewer.isOnline()) {
                    cancel();
                    return;
                }
                draw(arena, viewer);
                left -= 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private static void draw(Arena arena, Player viewer) {
        BoundingBox bounds = arena.getBounds();
        if (bounds != null && arena.getPos1() != null) {
            World w = arena.getPos1().getWorld();
            if (w != null) {
                outlineBox(w, bounds, Particle.END_ROD, viewer);
            }
        }
        BoundingBox spec = arena.getSpectatorZone();
        if (spec != null && arena.getPos1() != null) {
            World w = arena.getPos1().getWorld();
            if (w != null) {
                outlineBox(w, spec, Particle.SOUL_FIRE_FLAME, viewer);
            }
        }
        spawnPillar(arena.getPos1(), Particle.HAPPY_VILLAGER, viewer);
        spawnPillar(arena.getPos2(), Particle.FLAME, viewer);
        spawnPillar(arena.getSpectatorSpawn(), Particle.END_ROD, viewer);
    }

    private static void spawnPillar(Location loc, Particle particle, Player viewer) {
        if (loc == null || loc.getWorld() == null) return;
        for (int y = 0; y < 3; y++) {
            Location p = loc.clone().add(0, y * 0.5, 0);
            viewer.spawnParticle(particle, p, 3, 0.1, 0.1, 0.1, 0);
        }
    }

    private static void outlineBox(World world, BoundingBox box, Particle particle, Player viewer) {
        double minX = box.getMinX(), minY = box.getMinY(), minZ = box.getMinZ();
        double maxX = box.getMaxX(), maxY = box.getMaxY(), maxZ = box.getMaxZ();
        double step = 1.0;

        for (double x = minX; x <= maxX; x += step) {
            viewer.spawnParticle(particle, new Location(world, x, minY, minZ), 1, 0, 0, 0, 0);
            viewer.spawnParticle(particle, new Location(world, x, minY, maxZ), 1, 0, 0, 0, 0);
            viewer.spawnParticle(particle, new Location(world, x, maxY, minZ), 1, 0, 0, 0, 0);
            viewer.spawnParticle(particle, new Location(world, x, maxY, maxZ), 1, 0, 0, 0, 0);
        }
        for (double z = minZ; z <= maxZ; z += step) {
            viewer.spawnParticle(particle, new Location(world, minX, minY, z), 1, 0, 0, 0, 0);
            viewer.spawnParticle(particle, new Location(world, maxX, minY, z), 1, 0, 0, 0, 0);
            viewer.spawnParticle(particle, new Location(world, minX, maxY, z), 1, 0, 0, 0, 0);
            viewer.spawnParticle(particle, new Location(world, maxX, maxY, z), 1, 0, 0, 0, 0);
        }
        for (double y = minY; y <= maxY; y += step) {
            viewer.spawnParticle(particle, new Location(world, minX, y, minZ), 1, 0, 0, 0, 0);
            viewer.spawnParticle(particle, new Location(world, maxX, y, minZ), 1, 0, 0, 0, 0);
            viewer.spawnParticle(particle, new Location(world, minX, y, maxZ), 1, 0, 0, 0, 0);
            viewer.spawnParticle(particle, new Location(world, maxX, y, maxZ), 1, 0, 0, 0, 0);
        }
    }
}
