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

        // Cancel duel button (Footer Slot size - 3)
        setItem(size - 3, createCancelButton(), e -> {
            session.cancel(player.getUniqueId());
        });
    }

    private ItemStack createStatusItem(Player target, boolean ready) {
        String name = (target != null) ? target.getName() : "Игрок";
        String status = ready ? "<green><b>✔ ГОТОВ К БОЮ</b>" : "<red><b>✖ НЕ ГОТОВ</b>";
        var compName = MiniMessage.miniMessage().deserialize("<gold><b>" + name + "</b>: " + status);
        var compLore = List.of(
                MiniMessage.miniMessage().deserialize("<gray>Оба бойца должны подтвердить"),
                MiniMessage.miniMessage().deserialize("<gray>готовность перед началом поединка.")
        );
        return HeadTextures.playerHead(target, compName, compLore);
    }

    private ItemStack createToggleButton(boolean ready) {
        String texture = ready ? HeadTextures.READY : HeadTextures.NOT_READY;
        String name = ready ? "<green><b>✔ ВЫ ГОТОВЫ!</b> (Нажмите для отмены)" : "<yellow><b>➤ НАЖМИТЕ: Я ГОТОВ!</b>";
        List<String> lore = List.of(
                "<gray>Нажмите, чтобы изменить статус готовности."
        );
        return HeadTextures.head(texture, name, lore);
    }

    private ItemStack createCancelButton() {
        return HeadTextures.head(
                HeadTextures.CANCEL,
                "<red><b>✖ Отменить дуэль</b>",
                List.of("<gray>Отказаться от поединка и закрыть окно")
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
