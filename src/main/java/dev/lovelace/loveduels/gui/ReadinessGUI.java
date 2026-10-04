package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.match.ReadinessSession;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ReadinessGUI extends CustomGUI {

    private final ReadinessSession session;
    private final Player player1;
    private final Player player2;

    public ReadinessGUI(Player viewer, ReadinessSession session) {
        super(viewer, 27, dev.lovelace.loveduels.util.Lang.component("gui.readiness.title"));
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

        // Cancel duel button (Footer Slot size - 3)
        setItem(size - 3, createCancelButton(), e -> {
            session.cancel(player.getUniqueId());
        });
    }

    private ItemStack createStatusItem(Player target, boolean ready) {
        String name = (target != null) ? target.getName() : "Игрок";
        String status = ready
                ? dev.lovelace.loveduels.util.Lang.get("gui.readiness.status_ready")
                : dev.lovelace.loveduels.util.Lang.get("gui.readiness.status_not_ready");
        var compName = dev.lovelace.loveduels.util.Lang.component("gui.readiness.status_header", "player", name, "status", status);
        var compLore = dev.lovelace.loveduels.util.Lang.componentList("gui.readiness.status_lore");
        return HeadTextures.playerHead(target, compName, compLore);
    }

    private ItemStack createToggleButton(boolean ready) {
        String texture = ready ? HeadTextures.READY : HeadTextures.NOT_READY;
        String name = ready
                ? dev.lovelace.loveduels.util.Lang.get("gui.readiness.toggle_ready")
                : dev.lovelace.loveduels.util.Lang.get("gui.readiness.toggle_not_ready");
        List<String> lore = dev.lovelace.loveduels.util.Lang.list("gui.readiness.toggle_lore");
        return HeadTextures.head(texture, name, lore);
    }

    private ItemStack createCancelButton() {
        return HeadTextures.head(
                HeadTextures.CANCEL,
                dev.lovelace.loveduels.util.Lang.get("gui.readiness.cancel_name"),
                dev.lovelace.loveduels.util.Lang.list("gui.readiness.cancel_lore")
        );
    }

    @Override
    public void handleClose() {
        if (session.isTerminated()) {
            return;
        }
        // If player closes without ready, treat as cancel
        if (!session.isPlayer1Ready() || !session.isPlayer2Ready()) {
            session.cancel(player.getUniqueId());
        }
    }
}
