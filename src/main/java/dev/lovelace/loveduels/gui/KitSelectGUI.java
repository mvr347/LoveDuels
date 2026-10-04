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
        super(player, 45, dev.lovelace.loveduels.util.Lang.component("gui.kit_select.title"));
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
                    dev.lovelace.loveduels.util.Lang.get("gui.kit_select.no_kits_name"),
                    dev.lovelace.loveduels.util.Lang.list("gui.kit_select.no_kits_lore")
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
                    dev.lovelace.loveduels.util.Lang.send(player, "gui.kit_select.kit_selected_chat", "name", kit.displayName());
                }
            });
        }
    }

    private ItemStack createKitItem(Kit kit) {
        String name = dev.lovelace.loveduels.util.Lang.get("gui.kit_select.kit_item_name", "name", kit.displayName());
        String effectsStr = kit.effects().isEmpty()
                ? dev.lovelace.loveduels.util.Lang.get("gui.kit_select.kit_effects_none")
                : dev.lovelace.loveduels.util.Lang.get("gui.kit_select.kit_effects_count", "count", String.valueOf(kit.effects().size()));

        List<String> lore = dev.lovelace.loveduels.util.Lang.list(
                "gui.kit_select.kit_item_lore",
                "id", kit.id(),
                "effects", effectsStr
        );

        return HeadTextures.head(HeadTextures.CHEST, name, lore);
    }
}
