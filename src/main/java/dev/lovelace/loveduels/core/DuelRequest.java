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
        long createdAt,
        long expiresAt
) {
    public DuelRequest {
        Objects.requireNonNull(senderId, "senderId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(bet, "bet");
    }

    public static DuelRequest of(
            UUID senderId,
            UUID targetId,
            DuelType type,
            String kitId,
            DuelBet bet,
            boolean royal,
            long timeoutMillis
    ) {
        long now = System.currentTimeMillis();
        return new DuelRequest(senderId, targetId, type, kitId, bet, royal, now, now + timeoutMillis);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
