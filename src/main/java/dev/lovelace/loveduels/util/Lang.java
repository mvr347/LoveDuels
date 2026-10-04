package dev.lovelace.loveduels.util;

import dev.lovelace.loveduels.LoveDuels;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Lang {

    private static LoveDuels plugin;
    private static volatile FileConfiguration server;
    private static volatile FileConfiguration bundled;

    private Lang() {}

    public static void load(LoveDuels instance) {
        plugin = instance;
        File file = new File(plugin.getDataFolder(), "lang.yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("lang.yml", false);
            } catch (Exception ignored) {}
        }
        server = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        try (InputStream in = plugin.getResource("lang.yml")) {
            bundled = in == null ? new YamlConfiguration()
                    : YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            bundled = new YamlConfiguration();
            if (plugin != null) {
                plugin.getLogger().warning("Could not read bundled lang.yml: " + e.getMessage());
            }
        }
    }

    public static String get(String path, String... kv) {
        String raw = findString(path);
        if (raw == null) {
            return "<red>[" + path + "]</red>";
        }
        return apply(raw, kv);
    }

    public static String getOrDefault(String path, String defaultValue, String... kv) {
        String raw = findString(path);
        if (raw == null) {
            return defaultValue != null ? apply(defaultValue, kv) : "";
        }
        return apply(raw, kv);
    }

    public static Component component(String path, String... kv) {
        return MiniMessage.miniMessage().deserialize(get(path, kv)).decoration(TextDecoration.ITALIC, false);
    }

    public static List<String> list(String path, String... kv) {
        List<String> lines = findList(path);
        if (lines.isEmpty()) {
            return List.of("<red>[" + path + "]</red>");
        }
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(apply(line, kv));
        }
        return result;
    }

    public static List<Component> componentList(String path, String... kv) {
        List<String> lines = list(path, kv);
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(MiniMessage.miniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        return components;
    }

    public static void send(Audience audience, String path, String... kv) {
        if (audience != null) {
            audience.sendMessage(component(path, kv));
        }
    }

    private static String findString(String path) {
        for (FileConfiguration cfg : new FileConfiguration[]{server, bundled}) {
            if (cfg != null && cfg.contains(path)) {
                return cfg.getString(path);
            }
        }
        return null;
    }

    private static List<String> findList(String path) {
        for (FileConfiguration cfg : new FileConfiguration[]{server, bundled}) {
            if (cfg != null && cfg.contains(path)) {
                if (cfg.isList(path)) {
                    return new ArrayList<>(cfg.getStringList(path));
                }
                String s = cfg.getString(path);
                return s != null ? List.of(s) : List.of();
            }
        }
        return List.of();
    }

    private static String apply(String text, String... kv) {
        if (kv == null || kv.length == 0 || text == null) {
            return text;
        }
        for (int i = 0; i < kv.length - 1; i += 2) {
            String key = "{" + kv[i] + "}";
            String val = kv[i + 1] != null ? kv[i + 1] : "";
            text = text.replace(key, val);
        }
        return text;
    }
}
