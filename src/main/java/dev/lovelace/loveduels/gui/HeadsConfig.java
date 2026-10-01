package dev.lovelace.loveduels.gui;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Читает и кэширует base64-текстуры голов из heads.yml.
 * Позволяет изменять любую голову/кнопку в GUI прямо на сервере без пересборки плагина.
 */
public final class HeadsConfig {

    private static final Map<String, String> TEXTURES = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private HeadsConfig() {}

    /**
     * Загружает текстуры голов: сначала встроенные дефолты из JAR, затем пользовательские из файла heads.yml.
     */
    public static void load(Plugin plugin) {
        TEXTURES.clear();

        // 1. Встроенные дефолтные текстуры из JAR
        InputStream defaultStream = plugin.getResource("heads.yml");
        if (defaultStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
                parseIntoMap(defConfig);
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось прочитать встроенный heads.yml из JAR: " + e.getMessage());
            }
        }

        // 2. Сохраняем heads.yml в папку плагина, если файл отсутствует
        File file = new File(plugin.getDataFolder(), "heads.yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("heads.yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось сохранить heads.yml на диск: " + e.getMessage());
            }
        }

        // 3. Загружаем файл с диска поверх дефолтов
        if (file.exists()) {
            try {
                YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
                parseIntoMap(userConfig);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка чтения heads.yml с диска: " + e.getMessage());
            }
        }

        loaded = true;
        plugin.getLogger().info("Загружено " + TEXTURES.size() + " текстур голов из heads.yml");
    }

    private static void parseIntoMap(FileConfiguration config) {
        for (String rootKey : config.getKeys(false)) {
            if (config.isConfigurationSection(rootKey)) {
                var sec = config.getConfigurationSection(rootKey);
                if (sec != null) {
                    for (String childKey : sec.getKeys(true)) {
                        String fullPath = rootKey + "." + childKey;
                        if (config.isString(fullPath)) {
                            String val = cleanTexture(config.getString(fullPath));
                            if (val != null) {
                                TEXTURES.put(fullPath.toLowerCase(Locale.ROOT), val);
                                TEXTURES.put(childKey.toLowerCase(Locale.ROOT), val);
                            }
                        }
                    }
                }
            } else if (config.isString(rootKey)) {
                String val = cleanTexture(config.getString(rootKey));
                if (val != null) {
                    TEXTURES.put(rootKey.toLowerCase(Locale.ROOT), val);
                }
            }
        }
    }

    private static String cleanTexture(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.replaceAll("\\s+", "");
    }

    /**
     * Получить base64 текстуру по ключу (например: "sword", "navigation.back", "crown").
     */
    public static String get(String key, String fallback) {
        if (key == null) return fallback;
        String val = TEXTURES.get(key.toLowerCase(Locale.ROOT));
        if (val != null && !val.isEmpty()) {
            return val;
        }
        return fallback;
    }

    public static String get(String key) {
        return get(key, "");
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static Map<String, String> getAllTextures() {
        return Map.copyOf(TEXTURES);
    }
}
