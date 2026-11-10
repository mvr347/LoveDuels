package dev.lovelace.loveduels.match;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PreparationFreezeTest {

    @Test
    void turningTheHeadIsNotMovement() {
        assertFalse(PreparationFreeze.forbids(1, 64, 1, 1, 64, 1));
    }

    @Test
    void walkingAndJumpingAreForbidden() {
        assertTrue(PreparationFreeze.forbids(1, 64, 1, 1.2, 64, 1));
        assertTrue(PreparationFreeze.forbids(1, 64, 1, 1, 64, 1.2));
        assertTrue(PreparationFreeze.forbids(1, 64, 1, 1, 64.4, 1));
    }

    @Test
    void fallingIsStillAllowed() {
        assertFalse(PreparationFreeze.forbids(1, 64, 1, 1, 63.5, 1));
        assertEquals(63.5, PreparationFreeze.allowedY(64, 63.5));
        assertEquals(64, PreparationFreeze.allowedY(64, 64.4));
    }
}
