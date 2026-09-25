package dev.lovelace.loveduels.spectator;

import dev.lovelace.loveduels.match.Match;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Manages spectator sessions, slot pricing, and boundaries.
 */
public interface SpectatorManager {

    /**
     * Calculates ticket price for the next spectator in this match.
     * Returns 0 for the first 15 spectators.
     */
    long calculateSpectatorFee(Match match);

    /**
     * Attempts to add a spectator to the match.
     */
    boolean addSpectator(Player player, Match match);

    /**
     * Removes a spectator and restores their original state and location.
     */
    void removeSpectator(Player player);

    /**
     * Checks if player is currently spectating any duel.
     */
    boolean isSpectating(UUID player);

    /**
     * Gets the match the player is currently spectating.
     */
    Optional<Match> getSpectatedMatch(UUID player);
}
