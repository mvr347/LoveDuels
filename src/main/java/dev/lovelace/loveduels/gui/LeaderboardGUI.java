package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.storage.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public final class LeaderboardGUI extends CustomGUI {

    private final DuelManager duelManager;

    public LeaderboardGUI(Player player, DuelManager duelManager) {
        super(player, 54, dev.lovelace.loveduels.util.Lang.component("gui.leaderboard.title"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        // Player's personal stats at slot 4
        duelManager.getPlayerStorage().getOrCreatePlayer(player.getUniqueId(), player.getName()).thenAccept(myData -> {
            Bukkit.getScheduler().runTask(duelManager.getPlugin(), () -> {
                setItem(4, createMyStatsItem(myData), null);
            });
        });

        // Top 10 duelists
        duelManager.getPlayerStorage().getTopPlayersByHonor(10).thenAccept(topList -> {
            Bukkit.getScheduler().runTask(duelManager.getPlugin(), () -> {
                int[] slots = new int[]{
                        20, 21, 22, 23, 24,
                        29, 30, 31, 32, 33
                };

                for (int i = 0; i < Math.min(topList.size(), slots.length); i++) {
                    PlayerData data = topList.get(i);
                    int slot = slots[i];
                    int rank = i + 1;
                    setItem(slot, createTopItem(data, rank), null);
                }
            });
        });
    }

    private ItemStack createMyStatsItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.displayName(dev.lovelace.loveduels.util.Lang.component("gui.leaderboard.my_stats_name", "player", player.getName()));
            meta.lore(dev.lovelace.loveduels.util.Lang.componentList(
                    "gui.leaderboard.my_stats_lore",
                    "honor", String.valueOf(data.honor()),
                    "wins", String.valueOf(data.wins()),
                    "losses", String.valueOf(data.losses()),
                    "winrate", String.format("%.1f", data.winRate()),
                    "streak", String.valueOf(data.currentStreak()),
                    "best_streak", String.valueOf(data.bestStreak()),
                    "royal_wins", String.valueOf(data.royalWins())
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTopItem(PlayerData data, int rank) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(data.uuid()));
            String rankKey = switch (rank) {
                case 1 -> "gui.leaderboard.top_rank_1";
                case 2 -> "gui.leaderboard.top_rank_2";
                case 3 -> "gui.leaderboard.top_rank_3";
                default -> "gui.leaderboard.top_rank_other";
            };

            meta.displayName(dev.lovelace.loveduels.util.Lang.component(rankKey, "name", data.name(), "rank", String.valueOf(rank)));
            meta.lore(dev.lovelace.loveduels.util.Lang.componentList(
                    "gui.leaderboard.top_stats_lore",
                    "honor", String.valueOf(data.honor()),
                    "wins", String.valueOf(data.wins()),
                    "losses", String.valueOf(data.losses()),
                    "winrate", String.format("%.1f", data.winRate()),
                    "streak", String.valueOf(data.currentStreak()),
                    "royal_wins", String.valueOf(data.royalWins())
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
