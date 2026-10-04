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
        super(player, 27, dev.lovelace.loveduels.util.Lang.component("gui.post_duel.title"));
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
                dev.lovelace.loveduels.util.Lang.send(player, "gui.post_duel.rematch_opp_offline");
                return;
            }
            player.closeInventory();
            player.performCommand("duel rematch");
        });
    }

    private ItemStack createStatsItem(boolean isWinner) {
        double myDmg = player.getUniqueId().equals(result.player1Id()) ? result.player1DamageDealt() : result.player2DamageDealt();
        int myPts = player.getUniqueId().equals(result.player1Id()) ? result.player1Points() : result.player2Points();

        String name = dev.lovelace.loveduels.util.Lang.get("gui.post_duel.stats_name");
        List<String> lore = dev.lovelace.loveduels.util.Lang.list(
                "gui.post_duel.stats_lore",
                "reason", result.reason().getDescription(),
                "duration", String.valueOf(result.durationSeconds()),
                "damage", String.format("%.1f", myDmg),
                "points", String.valueOf(myPts)
        );

        return HeadTextures.head(HeadTextures.SCROLL, name, lore);
    }

    private ItemStack createOutcomeItem(boolean isWinner) {
        if (result.isDraw()) {
            String moneyStr = (result.royal() && result.reason() == dev.lovelace.loveduels.match.MatchEndReason.TIMEOUT)
                    ? dev.lovelace.loveduels.util.Lang.get("gui.post_duel.draw_money_burned")
                    : dev.lovelace.loveduels.util.Lang.get("gui.post_duel.draw_money_refunded");

            String name = dev.lovelace.loveduels.util.Lang.get("gui.post_duel.draw_name");
            List<String> lore = dev.lovelace.loveduels.util.Lang.list(
                    "gui.post_duel.draw_lore",
                    "reason", result.reason().getDescription(),
                    "money", moneyStr
            );
            return HeadTextures.head(HeadTextures.CLOCK, name, lore);
        }

        if (isWinner) {
            String name = dev.lovelace.loveduels.util.Lang.get("gui.post_duel.win_name");
            List<String> lore = dev.lovelace.loveduels.util.Lang.list(
                    "gui.post_duel.win_lore",
                    "money", CoinFormat.amount(result.moneyPrizeWon()),
                    "honor", String.valueOf(result.honorWon())
            );
            return HeadTextures.head(HeadTextures.TROPHY, name, CoinFormat.resolveGlyphs(player, lore));
        } else {
            String name = dev.lovelace.loveduels.util.Lang.get("gui.post_duel.defeat_name");
            List<String> lore = dev.lovelace.loveduels.util.Lang.list(
                    "gui.post_duel.defeat_lore",
                    "honor", String.valueOf(result.honorLost())
            );
            return HeadTextures.head(HeadTextures.SKULL, name, lore);
        }
    }

    private ItemStack createRematchButton(Player opp) {
        String oppName = opp != null ? opp.getName() : "---";
        String name = dev.lovelace.loveduels.util.Lang.get("gui.post_duel.rematch_name");
        List<String> lore = dev.lovelace.loveduels.util.Lang.list("gui.post_duel.rematch_lore", "opponent", oppName);
        return HeadTextures.head(HeadTextures.SWORD, name, lore);
    }
}
