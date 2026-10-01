package dev.lovelace.loveduels.core;

/**
 * Режим вызова из главного меню.
 */
public enum ChallengeMode {
    TRAINING,
    NORMAL,
    ROYAL;

    public boolean isTraining() {
        return this == TRAINING;
    }

    public boolean isRoyal() {
        return this == ROYAL;
    }
}
