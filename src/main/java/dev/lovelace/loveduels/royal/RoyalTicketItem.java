package dev.lovelace.loveduels.royal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Creates and validates the special Royal Duel Ticket item.
 */
public final class RoyalTicketItem {

    private final NamespacedKey key;

    public RoyalTicketItem(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "royal_ticket");
    }

    public ItemStack create(int amount) {
        ItemStack item = new ItemStack(Material.PAPER, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MiniMessage.miniMessage().deserialize(
                "<gradient:#FFD700:#FFA500><b>👑 Билет Королевской Дуэли</b></gradient>"
        ).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = List.of(
                MiniMessage.miniMessage().deserialize("<gray>Древняя королевская грамота,").decoration(TextDecoration.ITALIC, false),
                MiniMessage.miniMessage().deserialize("<gray>позволяющая бросить публичный вызов!").decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<gold>Особенности:").decoration(TextDecoration.ITALIC, false),
                MiniMessage.miniMessage().deserialize("<yellow>▪ <white>Обязательная денежная ставка").decoration(TextDecoration.ITALIC, false),
                MiniMessage.miniMessage().deserialize("<yellow>▪ <white>Глобальные объявления Глашатая").decoration(TextDecoration.ITALIC, false),
                MiniMessage.miniMessage().deserialize("<yellow>▪ <white>Дополнительный бонус победителю").decoration(TextDecoration.ITALIC, false),
                MiniMessage.miniMessage().deserialize("<yellow>▪ <white>Увеличенная Честь за победу").decoration(TextDecoration.ITALIC, false),
                MiniMessage.miniMessage().deserialize("<yellow>▪ <white>Оповещение в Discord").decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<green>➤ Нажмите ПКМ, чтобы бросить вызов").decoration(TextDecoration.ITALIC, false)
        );
        meta.lore(lore);

        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isTicket(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        Byte val = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }
}
