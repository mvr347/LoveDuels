package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.kit.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class KitSelectGUI extends CustomGUI {

    private final DuelManager duelManager;
    private final Consumer<Kit> onSelect;

    public KitSelectGUI(Player player, DuelManager duelManager, Consumer<Kit> onSelect) {
        super(player, 45, MiniMessage.miniMessage().deserialize("<aqua><b>🛡 Выбор набора снаряжения (Кита)</b></aqua>"));
        this.duelManager = duelManager;
        this.onSelect = onSelect;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        Collection<Kit> kits = duelManager.getKitManager().getAllKits();
        int[] slots = new int[]{20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

        int idx = 0;
        for (Kit kit : kits) {
            if (idx >= slots.length) break;
            int slot = slots[idx++];

            setItem(slot, createKitItem(kit), e -> {
                if (onSelect != null) {
                    onSelect.accept(kit);
                } else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<green>🛡 Выбран набор: " + kit.displayName()
                    ));
                }
            });
        }
    }

    private ItemStack createKitItem(Kit kit) {
        ItemStack item = (kit.icon() != null) ? kit.icon().clone() : new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(kit.displayName()).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Набор снаряжения: <white>" + kit.id()).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(MiniMessage.miniMessage().deserialize("<yellow>Эффекты: <white>" + (kit.effects().isEmpty() ? "Нет" : kit.effects().size())).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(MiniMessage.miniMessage().deserialize("<yellow>➤ Нажмите для выбора этого кита").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
