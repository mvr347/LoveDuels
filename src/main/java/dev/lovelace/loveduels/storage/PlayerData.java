package dev.lovelace.loveduels.storage;

import java.util.UUID;

/**
 * Immutable player profile with duel statistics and honor rating.
 */
public record PlayerData(
        UUID uuid,
        String name,
        int honor,
        int wins,
        int losses,
        int currentStreak,
        int bestStreak,
        long moneyWon,
        long moneyLost,
        int royalWins
) {
    public static final int DEFAULT_HONOR = 1000;

    public static PlayerData initial(UUID uuid, String name) {
        return new PlayerData(uuid, name, DEFAULT_HONOR, 0, 0, 0, 0, 0L, 0L, 0);
    }

    public int totalMatches() {
        return wins + losses;
    }

    public double winRate() {
        int total = totalMatches();
        if (total == 0) return 0.0;
        return (wins * 100.0) / total;
    }

    public PlayerData withWin(int honorDelta, long moneyEarned, boolean royal) {
        int newStreak = currentStreak + 1;
        int newBest = Math.max(bestStreak, newStreak);
        return new PlayerData(
                uuid,
                name,
                Math.max(0, honor + honorDelta),
                wins + 1,
                losses,
                newStreak,
                newBest,
                moneyWon + moneyEarned,
                moneyLost,
                royal ? royalWins + 1 : royalWins
        );
    }

    public PlayerData withLoss(int honorLost, long moneyLostAmount) {
        return new PlayerData(
                uuid,
                name,
                Math.max(0, honor - honorLost),
                wins,
                losses + 1,
                0,
                bestStreak,
                moneyWon,
                moneyLost + moneyLostAmount,
                royalWins
        );
    }

    public PlayerData withHonor(int newHonor) {
        return new PlayerData(
                uuid,
                name,
                Math.max(0, newHonor),
                wins,
                losses,
                currentStreak,
                bestStreak,
                moneyWon,
                moneyLost,
                royalWins
        );
    }

    public PlayerData withName(String newName) {
        return new PlayerData(
                uuid,
                newName,
                honor,
                wins,
                losses,
                currentStreak,
                bestStreak,
                moneyWon,
                moneyLost,
                royalWins
        );
    }
}
