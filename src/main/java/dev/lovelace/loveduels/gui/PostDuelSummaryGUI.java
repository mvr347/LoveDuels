package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.match.MatchResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

        // Slot 15: REMATCH (Реванш) Button!
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
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gold><b>Боевая статистика</b></gold>").decoration(TextDecoration.ITALIC, false));
            double myDmg = player.getUniqueId().equals(result.player1Id()) ? result.player1DamageDealt() : result.player2DamageDealt();
            int myPts = player.getUniqueId().equals(result.player1Id()) ? result.player1Points() : result.player2Points();
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Причина завершения: <white>" + result.reason().getDescription()).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Длительность: <white>" + result.durationSeconds() + "с").decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize("<gray>Нанесённый урон: <red>" + String.format("%.1f", myDmg) + "❤").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Набранные очки: <yellow>" + myPts).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createOutcomeItem(boolean isWinner) {
        if (result.isDraw()) {
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize("<yellow><b>⌛ НИЧЬЯ</b></yellow>").decoration(TextDecoration.ITALIC, false));
                String moneyStr = (result.royal() && result.reason() == dev.lovelace.loveduels.match.MatchEndReason.TIMEOUT)
                        ? "<red>Ставки сгорели в казне"
                        : "<green>Ставки возвращены";
                meta.lore(List.of(
                        MiniMessage.miniMessage().deserialize("<gray>Исход: <yellow>" + result.reason().getDescription()).decoration(TextDecoration.ITALIC, false),
                        MiniMessage.miniMessage().deserialize("<gray>Деньги: " + moneyStr).decoration(TextDecoration.ITALIC, false),
                        MiniMessage.miniMessage().deserialize("<gray>Честь: <white>Без изменений").decoration(TextDecoration.ITALIC, false)
                ));
                item.setItemMeta(meta);
            }
            return item;
        }

        ItemStack item = new ItemStack(isWinner ? Material.EMERALD : Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String title = isWinner ? "<green><b>🏆 ВЫ ПОБЕДИЛИ!</b>" : "<red><b>💀 ПОРАЖЕНИЕ</b>";
            meta.displayName(MiniMessage.miniMessage().deserialize(title).decoration(TextDecoration.ITALIC, false));

            String moneyStr = isWinner ? "<green>+" + result.moneyPrizeWon() + " монет" : "<red>Потеряна ставка";
            String honorStr = isWinner ? "<gold>+" + result.honorWon() : "<gray>-" + result.honorLost();

            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Деньги: " + moneyStr).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Честь: " + honorStr).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRematchButton(Player opp) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<yellow><b>⚔ РЕВАНШ!</b></yellow>").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Бросить повторный вызов игроку").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>на тех же условиях.").decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize(opp != null && opp.isOnline() ? "<green>➤ Нажмите для предложения реванша" : "<red>Соперник офлайн").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
