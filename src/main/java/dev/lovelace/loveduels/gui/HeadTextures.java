package dev.lovelace.loveduels.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Фасад для создания голов с base64-текстурами.
 * Текстуры загружаются из heads.yml через HeadsConfig,
 * позволяя изменять любые иконки на лету без пересборки плагина.
 */
public final class HeadTextures {

    private HeadTextures() {}

    public static String urlToBase64(String textureHash) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + textureHash + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    // =========================================================================
    // ВСТРОЕННЫЕ ДЕФОЛТЫ (FALLBACK BASE64)
    // =========================================================================
    public static final String DEFAULT_SWORD = urlToBase64("37aee9a75bf0df7897183015cca0b2d7b79bb3c34ea5424c6794bb4fa95c16bf");
    public static final String DEFAULT_CHEST = urlToBase64("4481dfe2b2f99e3dece4c347264335f53183f13b18a917ddb72313e9d07463fd");
    public static final String DEFAULT_CROWN = urlToBase64("7154419ee37848849e40fc3fe7ddb78246a736849a7a41e8e6197fb8b91f43e");
    public static final String DEFAULT_TROPHY = urlToBase64("f57c7e96a802c27080c7f8053814368ea94df864459120e5155718b503c3ed7");
    public static final String DEFAULT_EYE = urlToBase64("dfcee3a88bb54c0f6ee66b44ac74ff97cd92a08f14ccc17a262371f0aa8921");

    public static final String DEFAULT_BOW = urlToBase64("b5ca09bf9211ce6936793cccbcf81c9a5fd5bf155f4e1564f648f5a1848d");
    public static final String DEFAULT_HORSE = urlToBase64("567a1c8dfca1137784789355754c1b2b32422e77a6ec3c897af183f8abb1a6");
    public static final String DEFAULT_BACKPACK = urlToBase64("b78ef2e4cf2c41a2d142c9057b16e22cb84e348fcfef798332c67e36d9f1d8e");

    public static final String DEFAULT_COIN = urlToBase64("e36e94f6c0ce32514395a899901852f14e35c34da4470364e281738458b5d");
    public static final String DEFAULT_STAR = urlToBase64("1fdb1a149bbca8c61bb0e214e85aadf1a3e74ce577945a0bd56f1b55172d13");

    public static final String DEFAULT_READY = urlToBase64("a92e31ffb551c0110e96ea2d65fa3a5ff699f5923d318e5f128e1f9b5e76e");
    public static final String DEFAULT_NOT_READY = urlToBase64("beb588b35d3c8008a2879509b23b361a603b5193952f4c40974f07a4aab47a");
    public static final String DEFAULT_CONFIRM = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19";
    public static final String DEFAULT_CANCEL = urlToBase64("bc8614d1094da73169801129b0ff943fa4956314be934171d6111f7144d15f");

    public static final String DEFAULT_BACK = urlToBase64("8650e27a93696213bd3a34177d6118b7e28dd719a6b6f0088921b79148dc67f7");
    public static final String DEFAULT_CLOSE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ==";
    public static final String DEFAULT_NO_PLAYERS = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmRkMmViMGM2ZjhhOTU0M2VmNWZkNzI1MjVjYzJmYWIzNTY2M2NkNzA5MTM1ZTQzYjhlMjU3ZGMwYjc1ODk0OCJ9fX0=";
    public static final String DEFAULT_MONEY_BET = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWZkMTA4MzgzZGZhNWIwMmU4NjYzNTYwOTU0MTUyMGU0ZTE1ODk1MmQ2OGMxYzhmOGYyMDBlYzdlODg2NDJkIn19fQ==";
    public static final String DEFAULT_HONOR_BET = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZkZWMyYTA5ODE4NWQ4M2Q5NjUyNGQzMDM0ODA5NTNkZTBkZTY4ZjE1MDJjOTZiMzZhNDExNmU2OWZjMjdkNyJ9fX0=";
    public static final String DEFAULT_SEND_CHALLENGE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTMwZjQ1MzdkMjE0ZDM4NjY2ZTYzMDRlOWM4NTFjZDZmN2U0MWEwZWI3YzI1MDQ5YzlkMjJjOGM1ZjY1NDVkZiJ9fX0=";
    public static final String DEFAULT_RESET = urlToBase64("1a6f1ca5bb421d09e863339bf4f9d2d0b67bf5ab5ff9a3a936a6db9d18");
    public static final String DEFAULT_SCROLL = urlToBase64("b517743015a9951ee19cd8e411b026cc3dbb1e42bfae1bc97e74288b8fa7");
    public static final String DEFAULT_SKULL = urlToBase64("e5d59ce51e39b7d85c88b9bf92bb3bfa8be80a6b57df332d7ba5ab4ebc8");
    public static final String DEFAULT_PREVIOUS =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZkYWI3MjcxZjRmZjA0ZDU0NDAyMTkwNjdhMTA5YjVjMGMxZDFlMDFlYzYwMmMwMDIwNDc2ZjdlYjYxMjE4MCJ9fX0=";
    public static final String DEFAULT_NEXT =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFhMTg3ZmVkZTg4ZGUwMDJjYmQ5MzA1NzVlYjdiYTQ4ZDNiMWEwNmQ5NjFiZGM1MzU4MDA3NTBhZjc2NDkyNiJ9fX0=";
    public static final String DEFAULT_MAIN_MENU = urlToBase64("d34e063cafb467a5c8de43ec78619399f369f4a52434da8017a983cdd92516a0");
    public static final String DEFAULT_SUMMARY_SECTION = urlToBase64("46178ad51fd52b19d0a3888710bd92068e933252aac6b13c76e7e6ea5d3226");
    public static final String DEFAULT_SUMMARY_STATS = urlToBase64("e47ae209e8a52591cc20ac0c9ec6a5cb3dc04f238ac2d743b1d54f11e9c5c85");
    public static final String DEFAULT_SUMMARY_WIN = urlToBase64("e3e45831c1ea817f7477bcaebfa3d2ee3a936ee8ea2b8bde29006b7e9bdf58");
    public static final String DEFAULT_SUMMARY_DEFEAT = urlToBase64("29da13bf43599dd42f75b6e6548ead40c7ff488b22ef3d931e6c004de1fa978b");
    public static final String DEFAULT_SUMMARY_REMATCH = urlToBase64("d98bf29467e01762245c27faddd95ffac9d2ab63a47b6ca2eba8392f8f937af9");
    public static final String DEFAULT_CLOCK = urlToBase64("853c80e3c21934e650ef3164cb89547cb0b5cc4ab7cb9345094d4d62b66d48");

    // =========================================================================
    // СТАНДАРТНЫЕ КЛЮЧИ (КОНСТАНТЫ) ДЛЯ КОДА
    // =========================================================================
    public static final String SWORD = "sword";
    public static final String CHEST = "chest";
    public static final String CROWN = "crown";
    public static final String TROPHY = "trophy";
    public static final String EYE = "eye";

    public static final String BOW = "bow";
    public static final String HORSE = "horse";
    public static final String BACKPACK = "backpack";

    public static final String COIN = "coin";
    public static final String STAR = "star";

    public static final String READY = "ready";
    public static final String NOT_READY = "not_ready";
    public static final String CONFIRM = "confirm";
    public static final String CANCEL = "cancel";

    /** Empty states: no players to challenge, no duels to watch. */
    /** Slot 0 of every menu: the common LoveDuels head. */
    public static final String THEME = "navigation.theme";
    /** Slot 0 of the main menu and of the post-duel summary have their own heads. */
    public static final String MAIN_MENU = "navigation.main_menu";
    public static final String SUMMARY_SECTION = "post_duel.summary_section";
    public static final String SUMMARY_STATS = "post_duel.summary_stats";
    public static final String SUMMARY_WIN = "post_duel.summary_win";
    public static final String SUMMARY_DEFEAT = "post_duel.summary_defeat";
    public static final String SUMMARY_REMATCH = "post_duel.summary_rematch";
    public static final String NO_PLAYERS = "actions.spectate_empty";
    public static final String MONEY_BET = "economy.money_bet";
    public static final String HONOR_BET = "economy.honor_bet";
    public static final String SEND_CHALLENGE = "actions.send_challenge";

    public static final String BACK = "back";
    public static final String CLOSE = "close";
    public static final String RESET = "reset";
    public static final String SCROLL = "scroll";
    public static final String SKULL = "skull";
    public static final String CLOCK = "clock";
    public static final String PREVIOUS = "previous";
    public static final String NEXT = "next";

    private static final Map<String, String> FALLBACKS = new HashMap<>();

    static {
        FALLBACKS.put(SWORD, DEFAULT_SWORD);
        FALLBACKS.put(CHEST, DEFAULT_CHEST);
        FALLBACKS.put(CROWN, DEFAULT_CROWN);
        FALLBACKS.put(TROPHY, DEFAULT_TROPHY);
        FALLBACKS.put(EYE, DEFAULT_EYE);
        FALLBACKS.put(BOW, DEFAULT_BOW);
        FALLBACKS.put(HORSE, DEFAULT_HORSE);
        FALLBACKS.put(BACKPACK, DEFAULT_BACKPACK);
        FALLBACKS.put(COIN, DEFAULT_COIN);
        FALLBACKS.put(STAR, DEFAULT_STAR);
        FALLBACKS.put(READY, DEFAULT_READY);
        FALLBACKS.put(NOT_READY, DEFAULT_NOT_READY);
        FALLBACKS.put(CONFIRM, DEFAULT_CONFIRM);
        FALLBACKS.put(CANCEL, DEFAULT_CANCEL);
        FALLBACKS.put(BACK, DEFAULT_BACK);
        FALLBACKS.put(CLOSE, DEFAULT_CLOSE);
        FALLBACKS.put(THEME, DEFAULT_SWORD);
        FALLBACKS.put(MAIN_MENU, DEFAULT_MAIN_MENU);
        FALLBACKS.put(SUMMARY_SECTION, DEFAULT_SUMMARY_SECTION);
        FALLBACKS.put(SUMMARY_STATS, DEFAULT_SUMMARY_STATS);
        FALLBACKS.put(SUMMARY_WIN, DEFAULT_SUMMARY_WIN);
        FALLBACKS.put(SUMMARY_DEFEAT, DEFAULT_SUMMARY_DEFEAT);
        FALLBACKS.put(SUMMARY_REMATCH, DEFAULT_SUMMARY_REMATCH);
        FALLBACKS.put(NO_PLAYERS, DEFAULT_NO_PLAYERS);
        FALLBACKS.put(MONEY_BET, DEFAULT_MONEY_BET);
        FALLBACKS.put(HONOR_BET, DEFAULT_HONOR_BET);
        FALLBACKS.put(SEND_CHALLENGE, DEFAULT_SEND_CHALLENGE);
        FALLBACKS.put(RESET, DEFAULT_RESET);
        FALLBACKS.put(SCROLL, DEFAULT_SCROLL);
        FALLBACKS.put(SKULL, DEFAULT_SKULL);
        FALLBACKS.put(CLOCK, DEFAULT_CLOCK);
        FALLBACKS.put(PREVIOUS, DEFAULT_PREVIOUS);
        FALLBACKS.put(NEXT, DEFAULT_NEXT);
    }

    /**
     * Получить base64 текстуру по ключу из heads.yml.
     */
    public static String get(String key) {
        String fallback = FALLBACKS.getOrDefault(key.toLowerCase(Locale.ROOT), DEFAULT_SWORD);
        return HeadsConfig.get(key, fallback);
    }

    /**
     * Получить base64 текстуру с указанием fallback.
     */
    public static String get(String key, String fallback) {
        return HeadsConfig.get(key, fallback);
    }

    /**
     * Преобразует либо ключ ("sword", "navigation.back"), либо готовую base64-строку
     * в актуальное значение из heads.yml.
     */
    public static String resolve(String keyOrBase64) {
        if (keyOrBase64 == null || keyOrBase64.isEmpty()) {
            return DEFAULT_SWORD;
        }

        // Если это ключ из heads.yml:
        String val = HeadsConfig.get(keyOrBase64, null);
        if (val != null) {
            return val;
        }

        // Если это зарегистрированный ключ с известным fallback:
        String knownFallback = FALLBACKS.get(keyOrBase64.toLowerCase(Locale.ROOT));
        if (knownFallback != null) {
            return HeadsConfig.get(keyOrBase64, knownFallback);
        }

        // Если передан старый Base64, проверим, не является ли он одним из дефолтов
        for (Map.Entry<String, String> entry : FALLBACKS.entrySet()) {
            if (entry.getValue().equals(keyOrBase64)) {
                return HeadsConfig.get(entry.getKey(), entry.getValue());
            }
        }

        // Иначе это прямая base64 строка
        return keyOrBase64;
    }

    // =========================================================================
    // СОЗДАНИЕ ПРЕДМЕТОВ ГОЛОВ (ITEMSTACK)
    // =========================================================================

    public static ItemStack head(String keyOrBase64, String name, List<String> lore) {
        String base64 = resolve(keyOrBase64);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            applyBase64(meta, base64);

            if (name != null && !name.isEmpty()) {
                meta.displayName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(l -> MiniMessage.miniMessage().deserialize(l).decoration(TextDecoration.ITALIC, false))
                        .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack head(String keyOrBase64, Component name, List<Component> lore) {
        String base64 = resolve(keyOrBase64);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            applyBase64(meta, base64);

            if (name != null) {
                meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(c -> c.decoration(TextDecoration.ITALIC, false))
                        .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack playerHead(Player player, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            if (player != null) {
                copyKnownTexture(meta, player.getPlayerProfile());
            }
            if (name != null) {
                meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(c -> c.decoration(TextDecoration.ITALIC, false))
                        .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack playerHead(UUID uuid, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            if (uuid != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                copyKnownTexture(meta, op.getPlayerProfile());
            }
            if (name != null) {
                meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(c -> c.decoration(TextDecoration.ITALIC, false))
                        .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void copyKnownTexture(SkullMeta meta, PlayerProfile profile) {
        profile.getProperties().stream()
                .filter(p -> p.getName().equals("textures"))
                .findFirst()
                .ifPresent(property -> applyBase64(meta, property.getValue()));
    }

    private static void applyBase64(SkullMeta meta, String base64) {
        try {
            PlayerProfile profile =
                    Bukkit.createProfile(
                            UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8)),
                            "LoveTexture");
            profile.setProperty(new ProfileProperty("textures", base64));
            meta.setPlayerProfile(profile);
        } catch (Exception ignored) {
        }
    }
}
