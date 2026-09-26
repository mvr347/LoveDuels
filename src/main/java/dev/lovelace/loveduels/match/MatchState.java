package dev.lovelace.loveduels.match;

public enum MatchState {
    PREPARATION,
    FIGHTING,
    /** Post-fight freeze: both can surrender for reduced stakes or wait for draw. */
    ARMISTICE,
    ENDING,
    ENDED
}
