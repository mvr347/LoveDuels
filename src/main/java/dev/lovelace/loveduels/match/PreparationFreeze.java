package dev.lovelace.loveduels.match;

/**
 * Position rule for the preparation countdown: the fighters stand still on their spawns and may only look around.
 * Falling is still allowed, so a player who was placed slightly above the floor does not hang in the air.
 */
final class PreparationFreeze {

    private PreparationFreeze() {
    }

    /** Whether a step from one position to another moves the player in a way the freeze forbids. */
    static boolean forbids(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        return fromX != toX || fromZ != toZ || toY > fromY;
    }

    /** The y coordinate the player ends up on: they never go up, but keep falling. */
    static double allowedY(double fromY, double toY) {
        return Math.min(fromY, toY);
    }
}
