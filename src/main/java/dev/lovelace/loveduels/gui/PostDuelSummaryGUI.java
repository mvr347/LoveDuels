package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.util.CoinFormat;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.match.MatchResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class PostDuelSummaryGUI extends CustomGUI {

    private final MatchResult result;
    private final DuelManager duelManager;

    public PostDuelSummaryGUI(Player player, MatchResult result, DuelManager duelManager) {
        super(player, 27, MiniMessage.miniMessage().deserialize("<gold><b>⚔ Итоги поединка</b></gold>"));
        this.result = result;
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(false, null);

        boolean isWinner = result.isWinner(player.getUniqueId());

        // Slot 11: Stats (Damage, Points, Duration)
        setItem(11, createStatsItem(isWinner), null);

        // Slot 13: Outcome & Prizes
        setItem(13, createOutcomeItem(isWinner), null);

        // Slot 15: REMATCH (Реванш) Button
        UUID opponentId = result.getOpponentId(player.getUniqueId());
        Player opp = (opponentId != null) ? Bukkit.getPlayer(opponentId) : null;

        setItem(15, createRematchButton(opp), e -> {
            if (opp == null || !opp.isOnline()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Соперник уже не в сети."));
                return;
            }
            player.closeInventory();
            player.performCommand("duel rematch");
        });
    }

    private ItemStack createStatsItem(boolean isWinner) {
        double myDmg = player.getUniqueId().equals(result.player1Id()) ? result.player1DamageDealt() : result.player2DamageDealt();
        int myPts = player.getUniqueId().equals(result.player1Id()) ? result.player1Points() : result.player2Points();

        String name = "<gold><b>Боевая статистика</b></gold>";
        List<String> lore = List.of(
                "<gray>Причина завершения: <white>" + result.reason().getDescription(),
                "<gray>Длительность: <white>" + result.durationSeconds() + "с",
                "",
                "<gray>Нанесённый урон: <red>" + String.format("%.1f", myDmg) + "❤",
                "<gray>Набранные очки: <yellow>" + myPts
        );

        return HeadTextures.head(HeadTextures.SCROLL, name, lore);
    }

    private ItemStack createOutcomeItem(boolean isWinner) {
        if (result.isDraw()) {
            String moneyStr = (result.royal() && result.reason() == dev.lovelace.loveduels.match.MatchEndReason.TIMEOUT)
                    ? "<red>Ставки сгорели в казне"
                    : "<green>Ставки возвращены";

            String name = "<yellow><b>⌛ НИЧЬЯ</b></yellow>";
            List<String> lore = List.of(
                    "<gray>Исход: <yellow>" + result.reason().getDescription(),
                    "<gray>Деньги: " + moneyStr,
                    "<gray>Честь: <white>Без изменений"
            );
            return HeadTextures.head(HeadTextures.CLOCK, name, lore);
        }

        if (isWinner) {
            String name = "<green><b>🏆 ВЫ ПОБЕДИЛИ!</b>";
            List<String> lore = List.of(
                    "<gray>Деньги: <green>+</green>" + CoinFormat.amount(result.moneyPrizeWon()),
                    "<gray>Честь: <gold>+" + result.honorWon()
            );
            return HeadTextures.head(HeadTextures.TROPHY, name, CoinFormat.resolveGlyphs(player, lore));
        } else {
            String name = "<red><b>💀 ПОРАЖЕНИЕ</b>";
            List<String> lore = List.of(
                    "<gray>Деньги: <red>Потеряна ставка",
                    "<gray>Честь: <gray>-" + result.honorLost()
            );
            return HeadTextures.head(HeadTextures.SKULL, name, lore);
        }
    }

    private ItemStack createRematchButton(Player opp) {
        String name = "<yellow><b>⚔ РЕВАНШ!</b></yellow>";
        List<String> lore = List.of(
                "<gray>Бросить повторный вызов игроку",
                "<gray>на тех же условиях поединка.",
                "",
                opp != null && opp.isOnline() ? "<green>➤ Нажмите для предложения реванша" : "<red>Соперник офлайн"
        );
        return HeadTextures.head(HeadTextures.SWORD, name, lore);
    }
}
