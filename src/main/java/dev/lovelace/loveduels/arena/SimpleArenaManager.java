package dev.lovelace.loveduels.arena;

import dev.lovelace.loveduels.core.DuelType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class SimpleArenaManager implements ArenaManager {

    private final Plugin plugin;
    private final File arenasFile;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();

    public SimpleArenaManager(Plugin plugin) {
        this.plugin = plugin;
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
    }

    @Override
    public Optional<Arena> findAvailableArena(DuelType type) {
        return arenas.values().stream()
                .filter(arena -> arena.isAvailableFor(type))
                .findFirst();
    }

    @Override
    public Optional<Arena> getArena(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(arenas.get(id.toLowerCase()));
    }

    @Override
    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    @Override
    public void registerArena(Arena arena) {
        arenas.put(arena.getId(), arena);
        saveArenas();
    }

    @Override
    public boolean deleteArena(String id) {
        Arena removed = arenas.remove(id.toLowerCase());
        if (removed != null) {
            saveArenas();
            return true;
        }
        return false;
    }

    @Override
    public void loadArenas() {
        arenas.clear();
        if (!arenasFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(arenasFile);
        ConfigurationSection root = config.getConfigurationSection("arenas");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;

            String name = sec.getString("name", id);
            Arena arena = new Arena(id, name);
            arena.setEnabled(sec.getBoolean("enabled", true));

            arena.setPos1(deserializeLocation(sec.getConfigurationSection("pos1")));
            arena.setPos2(deserializeLocation(sec.getConfigurationSection("pos2")));
            arena.setSpectatorSpawn(deserializeLocation(sec.getConfigurationSection("spectatorSpawn")));

            arena.setBounds(deserializeBoundingBox(sec.getConfigurationSection("bounds")));
            arena.setSpectatorZone(deserializeBoundingBox(sec.getConfigurationSection("spectatorZone")));

            List<String> types = sec.getStringList("supportedTypes");
            if (!types.isEmpty()) {
                arena.getSupportedTypes().clear();
                for (String t : types) {
                    DuelType dt = DuelType.fromString(t);
                    if (dt != null) {
                        arena.getSupportedTypes().add(dt);
                    }
                }
            }

            arenas.put(arena.getId(), arena);
        }

        plugin.getLogger().info("Loaded " + arenas.size() + " duel arena(s).");
    }

    @Override
    public void saveArenas() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("arenas");

        for (Arena arena : arenas.values()) {
            ConfigurationSection sec = root.createSection(arena.getId());
            sec.set("name", arena.getName());
            sec.set("enabled", arena.isEnabled());

            if (arena.getPos1() != null) {
                serializeLocation(sec.createSection("pos1"), arena.getPos1());
            }
            if (arena.getPos2() != null) {
                serializeLocation(sec.createSection("pos2"), arena.getPos2());
            }
            if (arena.getSpectatorSpawn() != null) {
                serializeLocation(sec.createSection("spectatorSpawn"), arena.getSpectatorSpawn());
            }

            if (arena.getBounds() != null) {
                serializeBoundingBox(sec.createSection("bounds"), arena.getBounds());
            }
            if (arena.getSpectatorZone() != null) {
                serializeBoundingBox(sec.createSection("spectatorZone"), arena.getSpectatorZone());
            }

            List<String> types = new ArrayList<>();
            for (DuelType t : arena.getSupportedTypes()) {
                types.add(t.getId());
            }
            sec.set("supportedTypes", types);
        }

        try {
            config.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save arenas.yml", e);
        }
    }

    private void serializeLocation(ConfigurationSection sec, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        sec.set("world", loc.getWorld().getName());
        sec.set("x", loc.getX());
        sec.set("y", loc.getY());
        sec.set("z", loc.getZ());
        sec.set("yaw", loc.getYaw());
        sec.set("pitch", loc.getPitch());
    }

    private Location deserializeLocation(ConfigurationSection sec) {
        if (sec == null) return null;
        String worldName = sec.getString("world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw", 0.0);
        float pitch = (float) sec.getDouble("pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void serializeBoundingBox(ConfigurationSection sec, BoundingBox box) {
        if (box == null) return;
        sec.set("minX", box.getMinX());
        sec.set("minY", box.getMinY());
        sec.set("minZ", box.getMinZ());
        sec.set("maxX", box.getMaxX());
        sec.set("maxY", box.getMaxY());
        sec.set("maxZ", box.getMaxZ());
    }

    private BoundingBox deserializeBoundingBox(ConfigurationSection sec) {
        if (sec == null) return null;
        double minX = sec.getDouble("minX");
        double minY = sec.getDouble("minY");
        double minZ = sec.getDouble("minZ");
        double maxX = sec.getDouble("maxX");
        double maxY = sec.getDouble("maxY");
        double maxZ = sec.getDouble("maxZ");
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
