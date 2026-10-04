package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.util.CoinFormat;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Hopper-style 9-slot confirm/cancel (gui-gen-5 Exception 1).
 */
public final class ConfirmBetGUI extends CustomGUI {

    private final long moneyBet;
    private final int honorBet;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmBetGUI(Player player, long moneyBet, int honorBet, Runnable onConfirm, Runnable onCancel) {
        super(player, 9, dev.lovelace.loveduels.util.Lang.component("gui.confirm_bet.title"));
        this.moneyBet = moneyBet;
        this.honorBet = honorBet;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected void build() {
        ItemStack glass = createGlass();
        setItem(0, glass, null);
        setItem(2, glass, null);
        setItem(6, glass, null);
        setItem(8, glass, null);

        String honorStr = honorBet > 0
                ? dev.lovelace.loveduels.util.Lang.get("gui.confirm_bet.honor_line", "honor", String.valueOf(honorBet))
                : dev.lovelace.loveduels.util.Lang.get("gui.confirm_bet.honor_none");

        List<String> confirmLore = dev.lovelace.loveduels.util.Lang.list(
                "gui.confirm_bet.confirm_lore",
                "money", CoinFormat.amount(moneyBet),
                "honor", honorStr
        );

        setItem(1, HeadTextures.head(
                HeadTextures.CONFIRM,
                dev.lovelace.loveduels.util.Lang.get("gui.confirm_bet.confirm_name"),
                CoinFormat.resolveGlyphs(player, confirmLore)
        ), e -> {
            player.closeInventory();
            if (onConfirm != null) onConfirm.run();
        });

        setItem(7, HeadTextures.head(
                HeadTextures.CANCEL,
                dev.lovelace.loveduels.util.Lang.get("gui.confirm_bet.cancel_name"),
                dev.lovelace.loveduels.util.Lang.list("gui.confirm_bet.cancel_lore")
        ), e -> {
            player.closeInventory();
            if (onCancel != null) onCancel.run();
        });
    }
}
