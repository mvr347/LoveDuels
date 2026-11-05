package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.util.CoinFormat;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Подтверждение ставки: компактное меню gui-gen-6.
 */
public final class ConfirmBetGUI extends CustomGUI {

    @Override
    protected String sectionKey() {
        return "confirm_bet";
    }

    private final long moneyBet;
    private final int honorBet;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmBetGUI(Player player, long moneyBet, int honorBet, Runnable onConfirm, Runnable onCancel) {
        super(player, 27, dev.lovelace.loveduels.util.Lang.component("gui.confirm_bet.title"));
        this.moneyBet = moneyBet;
        this.honorBet = honorBet;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected void build() {
        applyStandardBorders(false, null);

        String honorStr = honorBet > 0
                ? dev.lovelace.loveduels.util.Lang.get("gui.confirm_bet.honor_line", "honor", String.valueOf(honorBet))
                : dev.lovelace.loveduels.util.Lang.get("gui.confirm_bet.honor_none");

        List<String> confirmLore = dev.lovelace.loveduels.util.Lang.list(
                "gui.confirm_bet.confirm_lore",
                "money", CoinFormat.amount(moneyBet),
                "honor", honorStr
        );

        setItem(12, HeadTextures.head(
                HeadTextures.CONFIRM,
                dev.lovelace.loveduels.util.Lang.get("gui.confirm_bet.confirm_name"),
                CoinFormat.resolveGlyphs(player, confirmLore)
        ), e -> {
            player.closeInventory();
            if (onConfirm != null) onConfirm.run();
        });

        setItem(14, HeadTextures.head(
                HeadTextures.CANCEL,
                dev.lovelace.loveduels.util.Lang.get("gui.confirm_bet.cancel_name"),
                dev.lovelace.loveduels.util.Lang.list("gui.confirm_bet.cancel_lore")
        ), e -> {
            player.closeInventory();
            if (onCancel != null) onCancel.run();
        });
    }
}
