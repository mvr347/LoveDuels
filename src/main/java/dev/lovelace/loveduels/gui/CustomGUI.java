package dev.lovelace.loveduels.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base GUI conforming strictly to gui-gen-6 Love* unified standard:
 * - Sizes: 27, 36, 45, 54
 * - Glass ONLY in Header (row 0, and row 1 if size >= 45) and Footer (last 9 slots)
 * - Work area: NO glass, side columns empty (AIR)
 * - Slot 0: player head
 * - Footer: Slot size-2 is Back (or glass), Slot size-1 is Close
 */
public abstract class CustomGUI implements InventoryHolder {

    private static final ThreadLocal<CustomGUI> ACTION_PARENT = new ThreadLocal<>();
    private final CustomGUI parent = ACTION_PARENT.get();
    protected final Player player;
    protected final int size;
    protected final Component title;
    protected final Inventory inventory;
    protected final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();

    public CustomGUI(Player player, int size, Component title) {
        if (size != 27 && size != 36 && size != 45 && size != 54) {
            throw new IllegalArgumentException("Invalid GUI size according to gui-gen-6: " + size);
        }
        this.player = player;
        this.size = size;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public void open() {
        build();
        player.openInventory(inventory);
    }

    protected abstract void build();

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        var click = event.getClick();
        if (click != org.bukkit.event.inventory.ClickType.LEFT
                && click != org.bukkit.event.inventory.ClickType.RIGHT
                && click != org.bukkit.event.inventory.ClickType.SHIFT_LEFT
                && click != org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) return;
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < size) {
            Consumer<InventoryClickEvent> handler = clickHandlers.get(slot);
            if (handler != null) {
                ACTION_PARENT.set(this);
                try { handler.accept(event); }
                finally { ACTION_PARENT.remove(); }
            }
        }
    }

    public void handleClose() {
        // Can be overridden by subclasses
    }

    public void refresh() {
        build();
    }

    protected void applyStandardBorders(boolean hasBackButton, Runnable onBack) {
        inventory.clear();
        clickHandlers.clear();
        ItemStack glass = createGlass();

        for (int i = 0; i <= 8; i++) {
            setItem(i, glass, null);
        }

        if (size >= 45) {
            for (int i = 9; i <= 17; i++) {
                setItem(i, glass, null);
            }
        }

        setItem(0, createSectionHead(), null);

        int footerStart = size - 9;
        for (int i = footerStart; i < size; i++) {
            setItem(i, glass, null);
        }

        if (hasBackButton && parent != null) {
            setItem(size - 2, createBackButton(), e -> parent.open());
        }

        setItem(size - 1, createCloseButton(), e -> player.closeInventory());
    }

    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        if (item != null
                && item.hasItemMeta()
                && slot != size - 1
                && slot != size - 2
                && slot != 36
                && slot != 44) {
            var meta = item.getItemMeta();
            if (meta.hasLore()) {
                meta.lore(
                        GuiLore.normalize(
                                meta.lore(),
                                dev.lovelace.loveduels.util.Lang.component("gui.style.subtitle"),
                                dev.lovelace.loveduels.util.Lang.component("gui.style.separator")));
                item = item.clone();
                item.setItemMeta(meta);
            }
        }
        inventory.setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        } else {
            clickHandlers.remove(slot);
        }
    }

    protected ItemStack createGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Key of this menu in {@code gui.section.<key>} of lang.yml (name and short description of the section). */
    protected String sectionKey() {
        return "default";
    }

    /** Slot 0 of every menu: the LoveDuels head and a short description of what this section is for. */
    protected ItemStack createSectionHead() {
        String key = "gui.section." + sectionKey();
        return HeadTextures.head(
                HeadTextures.THEME,
                dev.lovelace.loveduels.util.Lang.component(key + ".name"),
                dev.lovelace.loveduels.util.Lang.componentList(key + ".lore"));
    }

    protected ItemStack createBackButton() {
        return HeadTextures.head(
                HeadTextures.BACK,
                dev.lovelace.loveduels.util.Lang.get("gui.common.back.name"),
                List.of()
        );
    }

    protected ItemStack createCloseButton() {
        return HeadTextures.head(
                HeadTextures.CLOSE,
                dev.lovelace.loveduels.util.Lang.get("gui.common.close.name"),
                List.of()
        );
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
