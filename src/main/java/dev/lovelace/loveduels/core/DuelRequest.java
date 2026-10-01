package dev.lovelace.loveduels.core;

import java.util.Objects;
import java.util.UUID;

/**
 * Duel request / invitation pending acceptance.
 */
public record DuelRequest(
        UUID senderId,
        UUID targetId,
        DuelType type,
        String kitId,
        DuelBet bet,
        boolean royal,
        boolean training,
        long createdAt,
        long expiresAt
) {
    public DuelRequest {
        Objects.requireNonNull(senderId, "senderId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(bet, "bet");
        if (training && royal) {
            throw new IllegalArgumentException("training and royal cannot both be true");
        }
    }

    public static DuelRequest of(
            UUID senderId,
            UUID targetId,
            DuelType type,
            String kitId,
            DuelBet bet,
            boolean royal,
            boolean training,
            long timeoutMillis
    ) {
        long now = System.currentTimeMillis();
        return new DuelRequest(senderId, targetId, type, kitId, bet, royal, training, now, now + timeoutMillis);
    }

    /** Backward-compatible factory (non-training). */
    public static DuelRequest of(
            UUID senderId,
            UUID targetId,
            DuelType type,
            String kitId,
            DuelBet bet,
            boolean royal,
            long timeoutMillis
    ) {
        return of(senderId, targetId, type, kitId, bet, royal, false, timeoutMillis);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isTraining() {
        return training;
    }
}
