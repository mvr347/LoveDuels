package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.kit.Kit;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

        if (kits.isEmpty()) {
            setItem(22, HeadTextures.head(
                    HeadTextures.CHEST,
                    "<red><b>Наборы не найдены</b></red>",
                    List.of("<gray>На сервере пока нет настроенных китов.")
            ), null);
            return;
        }

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
        String name = "<aqua><b>" + kit.displayName() + "</b></aqua>";
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Идентификатор: <white>" + kit.id());
        lore.add("");
        lore.add("<yellow>Эффекты: <white>" + (kit.effects().isEmpty() ? "Нет" : kit.effects().size() + " шт."));
        lore.add("");
        lore.add("<yellow>➤ Нажмите для выбора этого кита");

        return HeadTextures.head(HeadTextures.CHEST, name, lore);
    }
}
