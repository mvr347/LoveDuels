package dev.lovelace.loveduels.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.UUID;

/**
 * Manages challenge cooldowns between pairs of players and royal duel ticket cooldowns.
 */
public final class CooldownManager {

    // Key: sender UUID + ":" + target UUID -> timestamp when cooldown expires
    private final Cache<String, Long> challengeCooldowns = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(50000)
            .build();

    // Key: player UUID -> timestamp when royal ticket cooldown expires
    private final Cache<UUID, Long> royalTicketCooldowns = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(2))
            .maximumSize(10000)
            .build();

    /**
     * Checks if sender has an active cooldown to challenge the target player.
     *
     * @return remaining seconds, or 0 if no cooldown
     */
    public long getChallengeRemainingSeconds(UUID sender, UUID target) {
        String key = sender.toString() + ":" + target.toString();
        Long expiry = challengeCooldowns.getIfPresent(key);
        if (expiry == null) return 0;
        long diff = expiry - System.currentTimeMillis();
        if (diff <= 0) {
            challengeCooldowns.invalidate(key);
            return 0;
        }
        return (diff + 999) / 1000;
    }

    public void setChallengeCooldown(UUID sender, UUID target, long durationSeconds) {
        if (durationSeconds <= 0) return;
        String key = sender.toString() + ":" + target.toString();
        challengeCooldowns.put(key, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    /**
     * Checks if player has an active cooldown on royal duel tickets.
     *
     * @return remaining seconds, or 0 if no cooldown
     */
    public long getRoyalTicketRemainingSeconds(UUID player) {
        Long expiry = royalTicketCooldowns.getIfPresent(player);
        if (expiry == null) return 0;
        long diff = expiry - System.currentTimeMillis();
        if (diff <= 0) {
            royalTicketCooldowns.invalidate(player);
            return 0;
        }
        return (diff + 999) / 1000;
    }

    public void setRoyalTicketCooldown(UUID player, long durationSeconds) {
        if (durationSeconds <= 0) return;
        royalTicketCooldowns.put(player, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    /**
     * Formats remaining seconds into readable Russian string (e.g., "9м 45с" or "55с").
     */
    public static String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) return "0с";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("ч ");
        if (minutes > 0 || hours > 0) sb.append(minutes).append("м ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("с");
        return sb.toString().trim();
    }
}
