package dev.lovelace.loveduels.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Hopper-style 9-slot confirm/cancel (gui-gen-5 Exception 1).
 */
public final class ConfirmBetGUI extends CustomGUI {

    private static final String CONFIRM_TEX =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19";
    private static final String CANCEL_TEX =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzYxODczMWUwNjMzNzlhZWJmODJmMWQ2NGM0MTljOTBkN2YwYzE2NDhjNTQ4ZTliNjE1MWIxYmFiYTY2ZDcyMyJ9fX0=";

    private final long moneyBet;
    private final int honorBet;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmBetGUI(Player player, long moneyBet, int honorBet, Runnable onConfirm, Runnable onCancel) {
        super(player, 9, MiniMessage.miniMessage().deserialize("<red><b>Подтверждение ставки</b>"));
        this.moneyBet = moneyBet;
        this.honorBet = honorBet;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected void build() {
        ItemStack glass = createGlass();
        setItem(0, glass, null);
        setItem(2, glass, null);
        setItem(6, glass, null);
        setItem(8, glass, null);

        setItem(1, skull(CONFIRM_TEX, "<green><b>✔ Подтвердить</b>", List.of(
                "<gray>Ставка: <gold>" + moneyBet + " монет",
                honorBet > 0 ? "<gray>Честь: <yellow>" + honorBet : "<gray>Честь: <white>нет",
                "",
                "<green>ЛКМ — отправить вызов"
        )), e -> {
            player.closeInventory();
            if (onConfirm != null) onConfirm.run();
        });

        setItem(7, skull(CANCEL_TEX, "<red><b>✖ Отмена</b>", List.of(
                "<gray>Вернуться к настройке дуэли",
                "",
                "<red>ЛКМ — отмена"
        )), e -> {
            player.closeInventory();
            if (onCancel != null) onCancel.run();
        });
    }

    private ItemStack skull(String base64, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            try {
                PlayerProfile profile = org.bukkit.Bukkit.createProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                String url = "http://textures.minecraft.net/texture/" + extractTextureId(base64);
                textures.setSkin(URI.create(url).toURL());
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            } catch (Exception ignored) {
            }
            meta.displayName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(l -> MiniMessage.miniMessage().deserialize(l).decoration(TextDecoration.ITALIC, false))
                    .toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String extractTextureId(String base64) {
        try {
            String json = new String(java.util.Base64.getDecoder().decode(base64));
            int i = json.indexOf("/texture/");
            if (i >= 0) {
                int start = i + "/texture/".length();
                int end = start;
                while (end < json.length() && Character.isLetterOrDigit(json.charAt(end))) end++;
                return json.substring(start, end);
            }
        } catch (Exception ignored) {
        }
        return base64;
    }
}
