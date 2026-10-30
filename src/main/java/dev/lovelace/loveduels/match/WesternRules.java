package dev.lovelace.loveduels.match;

import java.util.UUID;

/** Pure rule of the "western" easter egg (no Bukkit types), so it can be unit-tested. */
public final class WesternRules {
    private WesternRules() {
    }

    /**
     * Both fighters took a lethal blow at the same moment: the match was ended by a lethal blow on {@code firstVictim}
     * at {@code endedTick}, and now, within {@code windowTicks}, a lethal blow lands on the other fighter.
     */
    public static boolean simultaneous(UUID firstVictim, UUID secondVictim, long endedTick, long nowTick, int windowTicks) {
        if (firstVictim == null || secondVictim == null || firstVictim.equals(secondVictim)) return false;
        long since = nowTick - endedTick;
        return since >= 0 && since <= Math.max(0, windowTicks);
    }
}
