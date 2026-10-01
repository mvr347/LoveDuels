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
        if (arena == null || viewer == null || !viewer.isOnline()) return;
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
        World w = arena.getWorld();
        if (w == null) {
            w = viewer.getWorld();
        }

        BoundingBox bounds = arena.getBounds();
        if (bounds != null) {
            outlineBox(w, bounds, Particle.END_ROD, viewer);
        }
        BoundingBox spec = arena.getSpectatorZone();
        if (spec != null) {
            outlineBox(w, spec, Particle.SOUL_FIRE_FLAME, viewer);
        }
        spawnPillar(arena.getPos1(), Particle.HAPPY_VILLAGER, viewer);
        spawnPillar(arena.getPos2(), Particle.FLAME, viewer);
        spawnPillar(arena.getSpectatorSpawn(), Particle.SOUL_FIRE_FLAME, viewer);
    }

    private static void spawnPillar(Location loc, Particle particle, Player viewer) {
        if (loc == null || loc.getWorld() == null) return;
        if (!loc.getWorld().equals(viewer.getWorld())) return;
        for (int y = 0; y <= 5; y++) {
            Location p = loc.clone().add(0, y * 0.5, 0);
            viewer.spawnParticle(particle, p, 2, 0.05, 0.05, 0.05, 0);
        }
    }

    private static void outlineBox(World world, BoundingBox box, Particle particle, Player viewer) {
        if (world == null || !world.equals(viewer.getWorld())) return;

        double minX = box.getMinX(), minY = box.getMinY(), minZ = box.getMinZ();
        double maxX = box.getMaxX(), maxY = box.getMaxY(), maxZ = box.getMaxZ();
        double widthX = maxX - minX;
        double widthZ = maxZ - minZ;
        double step = Math.max(1.0, Math.min(2.0, Math.max(widthX, widthZ) / 25.0));

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
