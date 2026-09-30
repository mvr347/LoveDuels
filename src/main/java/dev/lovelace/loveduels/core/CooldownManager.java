package dev.lovelace.loveduels.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.UUID;

/**
 * Manages challenge cooldowns between pairs of players and royal duel ticket cooldowns.
 */
public final class CooldownManager {

    private final Cache<String, Long> challengeCooldowns = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(50000)
            .build();

    private final Cache<UUID, Long> royalTicketCooldowns = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(2))
            .maximumSize(10000)
            .build();

    private final Cache<UUID, BloodRevengeOffer> bloodRevengeOffers = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .maximumSize(5000)
            .build();

    private final Cache<String, Integer> rematchChainCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(20000)
            .build();

    public record BloodRevengeOffer(
            UUID challengerId,
            UUID targetId,
            long moneyBet,
            int honorBet,
            DuelType type,
            String kitId,
            boolean royal,
            long expiresAtMs
    ) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }
    }

    private static String pairKey(UUID a, UUID b) {
        if (a == null || b == null) return "null";
        String sa = a.toString();
        String sb = b.toString();
        return sa.compareTo(sb) <= 0 ? sa + ":" + sb : sb + ":" + sa;
    }

    public int getRematchCount(UUID a, UUID b) {
        Integer c = rematchChainCounts.getIfPresent(pairKey(a, b));
        return c == null ? 0 : c;
    }

    public boolean canRematch(UUID a, UUID b, int maxRematches) {
        if (maxRematches <= 0) return false;
        return getRematchCount(a, b) < maxRematches;
    }

    public void incrementRematch(UUID a, UUID b) {
        String key = pairKey(a, b);
        Integer cur = rematchChainCounts.getIfPresent(key);
        rematchChainCounts.put(key, (cur == null ? 0 : cur) + 1);
    }

    public void clearRematchChain(UUID a, UUID b) {
        rematchChainCounts.invalidate(pairKey(a, b));
    }

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

    public void putBloodRevenge(BloodRevengeOffer offer) {
        if (offer == null || offer.challengerId() == null) return;
        bloodRevengeOffers.put(offer.challengerId(), offer);
    }

    public BloodRevengeOffer getBloodRevenge(UUID challengerId) {
        BloodRevengeOffer o = bloodRevengeOffers.getIfPresent(challengerId);
        if (o == null) return null;
        if (o.isExpired()) {
            bloodRevengeOffers.invalidate(challengerId);
            return null;
        }
        return o;
    }

    public void clearBloodRevenge(UUID challengerId) {
        if (challengerId != null) bloodRevengeOffers.invalidate(challengerId);
    }

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
