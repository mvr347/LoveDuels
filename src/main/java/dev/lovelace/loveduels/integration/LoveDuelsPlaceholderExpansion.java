package dev.lovelace.loveduels.integration;

import dev.lovelace.loveduels.match.MatchManager;
import dev.lovelace.loveduels.storage.PlayerData;
import dev.lovelace.loveduels.storage.PlayerStorage;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class LoveDuelsPlaceholderExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final PlayerStorage playerStorage;
    private final MatchManager matchManager;

    public LoveDuelsPlaceholderExpansion(Plugin plugin, PlayerStorage playerStorage, MatchManager matchManager) {
        this.plugin = plugin;
        this.playerStorage = playerStorage;
        this.matchManager = matchManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "loveduels";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Lovelace";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("in_duel")) {
            return (player != null && matchManager.isInMatch(player.getUniqueId())) ? "true" : "false";
        }

        if (player == null) return "";

        Optional<PlayerData> opt = playerStorage.getCachedPlayer(player.getUniqueId());
        if (opt.isEmpty()) {
            return switch (params.toLowerCase()) {
                case "honor" -> "1000";
                case "wins", "losses", "streak", "best_streak", "royal_wins" -> "0";
                case "winrate" -> "0.0%";
                default -> "";
            };
        }

        PlayerData data = opt.get();
        return switch (params.toLowerCase()) {
            case "honor" -> String.valueOf(data.honor());
            case "wins" -> String.valueOf(data.wins());
            case "losses" -> String.valueOf(data.losses());
            case "total" -> String.valueOf(data.totalMatches());
            case "streak" -> String.valueOf(data.currentStreak());
            case "best_streak" -> String.valueOf(data.bestStreak());
            case "money_won" -> String.valueOf(data.moneyWon());
            case "royal_wins" -> String.valueOf(data.royalWins());
            case "winrate" -> String.format("%.1f%%", data.winRate());
            default -> null;
        };
    }
}
