package dev.lovelace.loveduels.match;

import dev.lovelace.loveduels.core.DuelBet;
import dev.lovelace.loveduels.core.DuelType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * High level manager for all active matches and player match sessions.
 */
public interface MatchManager {

    boolean isInMatch(UUID uuid);

    Optional<Match> getMatch(UUID uuid);

    Collection<Match> getActiveMatches();

    void createAndStartMatch(Player p1, Player p2, DuelType type, String kitId, DuelBet bet, boolean royal);

    void endMatch(Match match, UUID winnerId, MatchEndReason reason);

    void handleDisconnect(Player player);

    boolean forceEnd(Player player);

    boolean hasActiveRoyalDuel();
}
