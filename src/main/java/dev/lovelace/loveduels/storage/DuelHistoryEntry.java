package dev.lovelace.loveduels.storage;

import dev.lovelace.loveduels.core.DuelType;

import java.util.UUID;

/**
 * Historical record of a completed duel.
 */
public record DuelHistoryEntry(
        long id,
        UUID player1,
        UUID player2,
        String player1Name,
        String player2Name,
        UUID winner,
        DuelType type,
        long moneyBet,
        int honorBet,
        boolean royal,
        long timestamp,
        int durationSeconds
) {
    public boolean isWinner(UUID player) {
        return winner != null && winner.equals(player);
    }
}
