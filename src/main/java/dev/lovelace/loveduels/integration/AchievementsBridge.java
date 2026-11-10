package dev.lovelace.loveduels.integration;

import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.achievements.Achievements;

import java.util.UUID;

/**
 * Feeds LoveCore's shared achievements with duel counters (the wins already travel through the StatBus). Looked up on
 * every call; no LoveCore, an older one without the contract ({@link LinkageError}) or no service means nothing is
 * recorded. Main thread only.
 */
public final class AchievementsBridge {

    private AchievementsBridge() {
    }

    public static final String PLAYED = "duels.played";
    public static final String LOST = "duels.lost";

    /** One finished match: both fighters played it, the loser (if there is one) lost it. */
    public static void matchFinished(UUID player1, UUID player2, UUID loser) {
        try {
            LoveCore.service(Achievements.class).ifPresent(service -> Gateway.record(service, player1, player2, loser));
        } catch (LinkageError missingContract) {
            // an older LoveCore: nothing to record
        }
    }

    /** Isolated so the contract class is only resolved inside the try/catch above. */
    static final class Gateway {
        private Gateway() {
        }

        static void record(Achievements service, UUID player1, UUID player2, UUID loser) {
            if (player1 != null) service.progress(player1, PLAYED, 1);
            if (player2 != null) service.progress(player2, PLAYED, 1);
            if (loser != null) service.progress(loser, LOST, 1);
        }
    }
}
