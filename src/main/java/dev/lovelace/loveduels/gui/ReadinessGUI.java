package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.match.ReadinessSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public final class ReadinessGUI extends CustomGUI {

    private final ReadinessSession session;
    private final Player player1;
    private final Player player2;

    public ReadinessGUI(Player viewer, ReadinessSession session) {
        super(viewer, 27, MiniMessage.miniMessage().deserialize("<gradient:#FFD700:#FFA500><b>⚔ Подтверждение готовности</b></gradient>"));
        this.session = session;
        this.player1 = Bukkit.getPlayer(session.getPlayer1());
        this.player2 = Bukkit.getPlayer(session.getPlayer2());
    }

    @Override
    protected void build() {
        applyStandardBorders(false, null);

        // Player 1 indicator (Slot 11)
        setItem(11, createStatusItem(player1, session.isPlayer1Ready()), null);

        // Center Ready Toggle Button (Slot 13)
        boolean myReady = session.isReady(player.getUniqueId());
        setItem(13, createToggleButton(myReady), e -> {
            session.toggleReady(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        });

        // Player 2 indicator (Slot 15)
        setItem(15, createStatusItem(player2, session.isPlayer2Ready()), null);

        // Cancel duel button (Footer Slot 18 or 25)
        setItem(size - 3, createCancelButton(), e -> {
            session.cancel(player.getUniqueId());
        });
    }

    private ItemStack createStatusItem(Player target, boolean ready) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null && target != null) {
            meta.setOwningPlayer(target);
            String status = ready ? "<green><b>✔ ГОТОВ К БОЮ</b>" : "<red><b>✖ НЕ ГОТОВ</b>";
            meta.displayName(MiniMessage.miniMessage().deserialize(
                    "<gold><b>" + target.getName() + "</b>: " + status
            ).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Оба бойца должны подтвердить").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>готовность перед началом поединка.").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createToggleButton(boolean ready) {
        ItemStack item = new ItemStack(ready ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = ready ? "<green><b>✔ ВЫ ГОТОВЫ!</b> (Нажмите для отмены)" : "<yellow><b>➤ НАЖМИТЕ: Я ГОТОВ!</b>";
            meta.displayName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Нажмите, чтобы изменить статус готовности.").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCancelButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<red><b>✖ Отменить дуэль</b>").decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClose() {
        // If player closes without ready, treat as cancel
        if (!session.isPlayer1Ready() || !session.isPlayer2Ready()) {
            session.cancel(player.getUniqueId());
        }
    }
}
