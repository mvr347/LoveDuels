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

    @Override
    protected String sectionKey() {
        return "kit_select";
    }

    private int page;
    private final DuelManager duelManager;
    private final Consumer<Kit> onSelect;

    public KitSelectGUI(Player player, DuelManager duelManager, Consumer<Kit> onSelect) {
        super(player, 54, dev.lovelace.loveduels.util.Lang.component("gui.kit_select.title"));
        this.duelManager = duelManager;
        this.onSelect = onSelect;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        var kits = new java.util.ArrayList<>(duelManager.getKitManager().getAllKits());
        int[] slots = {19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};

        if (kits.isEmpty()) {
            setItem(22, HeadTextures.head(
                    "duel_types.kit",
                    dev.lovelace.loveduels.util.Lang.get("gui.kit_select.no_kits_name"),
                    dev.lovelace.loveduels.util.Lang.list("gui.kit_select.no_kits_lore")
            ), null);
            return;
        }

        page = PageMath.clampPage(page, kits.size(), slots.length);
        int from = page * slots.length;
        int to = Math.min(kits.size(), from + slots.length);
        if (page > 0) setItem(36, HeadTextures.head(HeadTextures.PREVIOUS,
                dev.lovelace.loveduels.util.Lang.get("gui.common.previous"), List.of()), e -> { page--; build(); });
        if (to < kits.size()) setItem(44, HeadTextures.head(HeadTextures.NEXT,
                dev.lovelace.loveduels.util.Lang.get("gui.common.next"), List.of()), e -> { page++; build(); });
        int idx = 0;
        for (Kit kit : kits.subList(from,to)) {
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

        return HeadTextures.head("duel_types.kit", name, lore);
    }
}
