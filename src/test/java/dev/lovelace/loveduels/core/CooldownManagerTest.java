package dev.lovelace.loveduels.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CooldownManagerTest {

    @Test
    void aLongConfiguredTicketCooldownIsKeptWholeNotCutAtTwoHours() {
        var cooldowns = new CooldownManager();
        UUID player = UUID.randomUUID();
        cooldowns.setRoyalTicketCooldown(player, 24 * 3600L);
        long left = cooldowns.getRoyalTicketRemainingSeconds(player);
        assertTrue(left > 23 * 3600L && left <= 24 * 3600L, "left=" + left);
    }

    @Test
    void aLongChallengeCooldownIsKeptWholeNotCutAtFifteenMinutes() {
        var cooldowns = new CooldownManager();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        cooldowns.setChallengeCooldown(a, b, 3600L);
        long left = cooldowns.getChallengeRemainingSeconds(a, b);
        assertTrue(left > 3500L && left <= 3600L, "left=" + left);
        assertEquals(0L, cooldowns.getChallengeRemainingSeconds(b, a), "the cooldown is per direction");
    }

    @Test
    void anExpiredOrZeroCooldownReadsAsZero() {
        var cooldowns = new CooldownManager();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        cooldowns.setChallengeCooldown(a, b, 0L);
        assertEquals(0L, cooldowns.getChallengeRemainingSeconds(a, b));
    }
}
