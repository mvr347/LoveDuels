package dev.lovelace.loveduels.match;

import java.util.UUID;

/**
 * Immutable match outcome and statistics.
 */
public record MatchResult(
        UUID matchId,
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
}
