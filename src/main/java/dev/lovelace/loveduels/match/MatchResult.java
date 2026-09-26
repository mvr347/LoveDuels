package dev.lovelace.loveduels.match;

import java.util.UUID;

/**
 * Immutable match outcome and statistics.
 */
public record MatchResult(
        UUID matchId,
        UUID player1Id,
        UUID player2Id,
        UUID winnerId,
        UUID loserId,
        MatchEndReason reason,
        long durationSeconds,
        double player1DamageDealt,
        double player2DamageDealt,
        int player1Points,
        int player2Points,
        long moneyPrizeWon,
        int honorWon,
        int honorLost,
        boolean royal
) {
    public boolean hasWinner() {
        return winnerId != null;
    }

    public boolean isWinner(UUID player) {
        return winnerId != null && winnerId.equals(player);
    }

    public boolean isDraw() {
        return winnerId == null;
    }

    public UUID getOpponentId(UUID player) {
        if (player1Id != null && player1Id.equals(player)) return player2Id;
        if (player2Id != null && player2Id.equals(player)) return player1Id;
        return null;
    }
}
