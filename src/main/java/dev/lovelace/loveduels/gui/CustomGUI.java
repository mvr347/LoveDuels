package dev.lovelace.loveduels.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base GUI conforming strictly to gui-gen-5 Love* unified standard:
 * - Sizes: 27, 36, 45, 54
 * - Glass ONLY in Header (row 0, and row 1 if size >= 45) and Footer (last 9 slots)
 * - Work area: NO glass, side columns empty (AIR)
 * - Slot 0: player head
 * - Footer: Slot size-2 is Back (or glass), Slot size-1 is Close
 */
public abstract class CustomGUI implements InventoryHolder {

    protected final Player player;
    protected final int size;
    protected final Component title;
    protected final Inventory inventory;
    protected final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();

    public CustomGUI(Player player, int size, Component title) {
        if (size != 9 && size != 27 && size != 36 && size != 45 && size != 54) {
            throw new IllegalArgumentException("Invalid GUI size according to gui-gen-5: " + size);
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
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < size) {
            Consumer<InventoryClickEvent> handler = clickHandlers.get(slot);
            if (handler != null) {
                handler.accept(event);
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
        ItemStack glass = createGlass();

        for (int i = 0; i <= 8; i++) {
            setItem(i, glass, null);
        }

        if (size >= 45) {
            for (int i = 9; i <= 17; i++) {
                setItem(i, glass, null);
            }
        }

        setItem(0, createPlayerHead(player), null);

        int footerStart = size - 9;
        for (int i = footerStart; i < size; i++) {
            setItem(i, glass, null);
        }

        if (hasBackButton) {
            setItem(size - 2, createBackButton(), e -> {
                if (onBack != null) onBack.run();
            });
        }

        setItem(size - 1, createCloseButton(), e -> player.closeInventory());
    }

    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
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

    protected ItemStack createPlayerHead(Player p) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(p);
            meta.displayName(dev.lovelace.loveduels.util.Lang.component("gui.common.profile.name", "player", p.getName()));
            meta.lore(dev.lovelace.loveduels.util.Lang.componentList("gui.common.profile.lore", "player", p.getName()));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack createBackButton() {
        return HeadTextures.head(
                HeadTextures.BACK,
                dev.lovelace.loveduels.util.Lang.get("gui.common.back.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.common.back.lore")
        );
    }

    protected ItemStack createCloseButton() {
        return HeadTextures.head(
                HeadTextures.CLOSE,
                dev.lovelace.loveduels.util.Lang.get("gui.common.close.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.common.close.lore")
        );
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
