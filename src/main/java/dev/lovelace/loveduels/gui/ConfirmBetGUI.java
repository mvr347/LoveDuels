package dev.lovelace.loveduels.gui;

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
        super(player, 9, MiniMessage.miniMessage().deserialize("<red><b>Подтверждение ставки</b>"));
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

        setItem(1, HeadTextures.head(HeadTextures.CONFIRM, "<green><b>✔ Подтвердить</b>", List.of(
                "<gray>Ставка: <gold>" + moneyBet + " монет",
                honorBet > 0 ? "<gray>Честь: <yellow>" + honorBet : "<gray>Честь: <white>нет",
                "",
                "<green>ЛКМ — отправить вызов"
        )), e -> {
            player.closeInventory();
            if (onConfirm != null) onConfirm.run();
        });

        setItem(7, HeadTextures.head(HeadTextures.CANCEL, "<red><b>✖ Отмена</b>", List.of(
                "<gray>Вернуться к настройке дуэли",
                "",
                "<red>ЛКМ — отмена"
        )), e -> {
            player.closeInventory();
            if (onCancel != null) onCancel.run();
        });
    }
}
