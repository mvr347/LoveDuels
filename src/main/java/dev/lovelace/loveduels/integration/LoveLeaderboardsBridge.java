package dev.lovelace.loveduels.integration;

import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.stats.Metrics;
import dev.lovelace.lovecore.api.stats.StatBus;
import dev.lovelace.loveduels.storage.PlayerData;

import java.util.UUID;

/**
 * Bridge for publishing duel statistics to LoveCore's StatBus
 * so LoveLeaderboards can automatically display top duelists.
 */
public final class LoveLeaderboardsBridge {

    public static final String METRIC_DUELS_WON = "duels.won";
    public static final String METRIC_DUELS_HONOR = "duels.honor";
    public static final String METRIC_DUELS_STREAK = "duels.streak";
    public static final String METRIC_DUELS_ROYAL = "duels.royal";

    public boolean isAvailable() {
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LoveCore")) {
            return false;
        }
        try {
            return LoveCore.service(StatBus.class).isPresent();
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().fine("[LoveDuels] StatBus unavailable: " + t.getMessage());
            return false;
        }
    }

    public void recordDuelWin(UUID player, boolean royal) {
        if (!isAvailable()) return;
        try {
            LoveCore.service(StatBus.class).ifPresent(bus -> {
                bus.record(player, METRIC_DUELS_WON, 1.0);
                bus.record(player, Metrics.KILLS, 1.0);
                if (royal) {
                    bus.record(player, METRIC_DUELS_ROYAL, 1.0);
                }
            });
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().fine("[LoveDuels] StatBus record error: " + t.getMessage());
        }
    }

    public void syncPlayerData(PlayerData data) {
        if (data == null || !isAvailable()) return;
        try {
            LoveCore.service(StatBus.class).ifPresent(bus -> {
                bus.set(data.uuid(), METRIC_DUELS_HONOR, data.honor());
                bus.set(data.uuid(), METRIC_DUELS_STREAK, data.currentStreak());
                bus.set(data.uuid(), METRIC_DUELS_WON, data.wins());
                bus.set(data.uuid(), METRIC_DUELS_ROYAL, data.royalWins());
            });
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().fine("[LoveDuels] StatBus sync error: " + t.getMessage());
        }
    }
}
