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
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public final class HeadTextures {

    private HeadTextures() {}

    public static String urlToBase64(String textureHash) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + textureHash + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    // High quality standard head textures
    public static final String SWORD = urlToBase64("37aee9a75bf0df7897183015cca0b2d7b79bb3c34ea5424c6794bb4fa95c16bf");
    public static final String CHEST = urlToBase64("4481dfe2b2f99e3dece4c347264335f53183f13b18a917ddb72313e9d07463fd");
    public static final String CROWN = urlToBase64("7154419ee37848849e40fc3fe7ddb78246a736849a7a41e8e6197fb8b91f43e");
    public static final String TROPHY = urlToBase64("f57c7e96a802c27080c7f8053814368ea94df864459120e5155718b503c3ed7");
    public static final String EYE = urlToBase64("dfcee3a88bb54c0f6ee66b44ac74ff97cd92a08f14ccc17a262371f0aa8921");
    
    public static final String BOW = urlToBase64("b5ca09bf9211ce6936793cccbcf81c9a5fd5bf155f4e1564f648f5a1848d");
    public static final String HORSE = urlToBase64("567a1c8dfca1137784789355754c1b2b32422e77a6ec3c897af183f8abb1a6");
    public static final String BACKPACK = urlToBase64("b78ef2e4cf2c41a2d142c9057b16e22cb84e348fcfef798332c67e36d9f1d8e");
    
    public static final String COIN = urlToBase64("e36e94f6c0ce32514395a899901852f14e35c34da4470364e281738458b5d");
    public static final String STAR = urlToBase64("1fdb1a149bbca8c61bb0e214e85aadf1a3e74ce577945a0bd56f1b55172d13");
    
    public static final String READY = urlToBase64("a92e31ffb551c0110e96ea2d65fa3a5ff699f5923d318e5f128e1f9b5e76e");
    public static final String NOT_READY = urlToBase64("beb588b35d3c8008a2879509b23b361a603b5193952f4c40974f07a4aab47a");
    public static final String CANCEL = urlToBase64("bc8614d1094da73169801129b0ff943fa4956314be934171d6111f7144d15f");
    
    public static final String BACK = urlToBase64("8650e27a93696213bd3a34177d6118b7e28dd719a6b6f0088921b79148dc67f7");
    public static final String CLOSE = urlToBase64("m583b4b884be1bb635db15cc7962fe46b38c227b68ab383fa1853d939634e");
    public static final String RESET = urlToBase64("1a6f1ca5bb421d09e863339bf4f9d2d0b67bf5ab5ff9a3a936a6db9d18");
    public static final String SCROLL = urlToBase64("b517743015a9951ee19cd8e411b026cc3dbb1e42bfae1bc97e74288b8fa7");
    public static final String SKULL = urlToBase64("e5d59ce51e39b7d85c88b9bf92bb3bfa8be80a6b57df332d7ba5ab4ebc8");
    public static final String CLOCK = urlToBase64("853c80e3c21934e650ef3164cb89547cb0b5cc4ab7cb9345094d4d62b66d48");

    public static ItemStack head(String base64, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8)));
                profile.setProperty(new ProfileProperty("textures", base64));
                meta.setPlayerProfile(profile);
            } catch (Exception ignored) {
            }

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

    public static ItemStack head(String base64, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8)));
                profile.setProperty(new ProfileProperty("textures", base64));
                meta.setPlayerProfile(profile);
            } catch (Exception ignored) {
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

    public static ItemStack playerHead(Player player, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            if (player != null) {
                meta.setOwningPlayer(player);
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
                meta.setOwningPlayer(op);
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
}
