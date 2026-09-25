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
        super(player, 54, MiniMessage.miniMessage().deserialize("<gold><b>🏆 Зал Славы — Топ Дуэлянтов</b></gold>"));
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
            meta.displayName(MiniMessage.miniMessage().deserialize(
                    "<yellow><b>Ваша статистика (" + player.getName() + ")</b></yellow>"
            ).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Рейтинг Чести: <gold>" + data.honor()).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Побед: <green>" + data.wins() + " <gray>| Поражений: <red>" + data.losses()).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Винрейт: <yellow>" + String.format("%.1f%%", data.winRate())).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Текущая серия: <white>" + data.currentStreak() + " <gray>(Лучшая: " + data.bestStreak() + ")").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Королевских побед: <gold>" + data.royalWins()).decoration(TextDecoration.ITALIC, false)
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
            String rankColor = switch (rank) {
                case 1 -> "<gold><b>#1 👑 ";
                case 2 -> "<white><b>#2 🥈 ";
                case 3 -> "<yellow><b>#3 🥉 ";
                default -> "<gray>#" + rank + " ";
            };

            meta.displayName(MiniMessage.miniMessage().deserialize(
                    rankColor + data.name() + "</b>"
            ).decoration(TextDecoration.ITALIC, false));

            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Рейтинг Чести: <gold><b>" + data.honor() + "</b>").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Побед: <green>" + data.wins() + " <gray>| Поражений: <red>" + data.losses()).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Винрейт: <yellow>" + String.format("%.1f%%", data.winRate())).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Серия побед: <white>" + data.currentStreak()).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Королевских побед: <gold>" + data.royalWins()).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
