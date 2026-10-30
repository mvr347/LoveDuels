package dev.lovelace.loveduels.match;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WesternRulesTest {
    private final UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void theOtherFighterInsideTheWindowIsSimultaneous() {
        assertTrue(WesternRules.simultaneous(a, b, 100, 100, 2));
        assertTrue(WesternRules.simultaneous(a, b, 100, 102, 2));
    }

    @Test
    void aLateBlowIsNot() {
        assertFalse(WesternRules.simultaneous(a, b, 100, 103, 2));
        assertFalse(WesternRules.simultaneous(a, b, 100, 100 + 400, 2));
    }

    @Test
    void theSameFighterTwiceIsNotAWestern() {
        assertFalse(WesternRules.simultaneous(a, a, 100, 100, 2));
    }

    @Test
    void aClockGoingBackOrMissingIdsNeverMatch() {
        assertFalse(WesternRules.simultaneous(a, b, 100, 99, 2));
        assertFalse(WesternRules.simultaneous(null, b, 100, 100, 2));
        assertFalse(WesternRules.simultaneous(a, null, 100, 100, 2));
    }

    @Test
    void aNegativeWindowMeansTheSameTickOnly() {
        assertTrue(WesternRules.simultaneous(a, b, 100, 100, -5));
        assertFalse(WesternRules.simultaneous(a, b, 100, 101, -5));
    }
}
