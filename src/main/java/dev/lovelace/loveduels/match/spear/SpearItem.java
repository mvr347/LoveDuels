package dev.lovelace.loveduels.match.spear;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Creates and verifies the tournament spear (lance) used in Horse + Spear duels.
 * Uses vanilla Iron Spear (Minecraft 1.21.11+ / 26.x).
 */
public final class SpearItem {

    private final NamespacedKey key;
    private final Material spearMaterial;

    public SpearItem(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "tournament_spear");
        this.spearMaterial = resolveSpearMaterial();
    }

    /**
     * Prefer IRON_SPEAR; fall back only if the server API somehow lacks it.
     */
    private static Material resolveSpearMaterial() {
        Material iron = Material.matchMaterial("IRON_SPEAR");
        if (iron != null && iron.isItem()) {
            return iron;
        }
        Material diamond = Material.matchMaterial("DIAMOND_SPEAR");
        if (diamond != null && diamond.isItem()) {
            return diamond;
        }
        // Last resort (should not happen on 26.x)
        return Material.IRON_SWORD;
    }

    public Material getSpearMaterial() {
        return spearMaterial;
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(spearMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MiniMessage.miniMessage().deserialize(
                "<gradient:#E5C158:#C29B38><b>🗡 Турнирное Копьё</b></gradient>"
        ).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = List.of(
                MiniMessage.miniMessage().deserialize("<gray>Рыцарское турнирное копьё для поединков.")
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<gold>Механика удара:")
                        .decoration(TextDecoration.ITALIC, false),
                MiniMessage.miniMessage().deserialize("<yellow>▪ <white>Зажмите <gold>ПКМ</gold> для заряда удара")
                        .decoration(TextDecoration.ITALIC, false),
                MiniMessage.miniMessage().deserialize("<yellow>▪ <white>Оптимальное окно: <green>1.2с – 1.8с")
                        .decoration(TextDecoration.ITALIC, false),
                MiniMessage.miniMessage().deserialize("<yellow>▪ <white>Отпустите / ударьте в момент сближения")
                        .decoration(TextDecoration.ITALIC, false),
                MiniMessage.miniMessage().deserialize("<yellow>▪ <white>Идеальное попадание (<gold>★</gold>) даёт больше очков")
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<dark_gray>Только для конного турнира. Ванильный урон отключён.")
                        .decoration(TextDecoration.ITALIC, false)
        );
        meta.lore(lore);

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isSpear(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (item.getType() != spearMaterial) return false;
        Byte val = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }
}
