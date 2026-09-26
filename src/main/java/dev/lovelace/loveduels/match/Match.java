package dev.lovelace.loveduels.match;

import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.core.DuelBet;
import dev.lovelace.loveduels.core.DuelType;
import dev.lovelace.loveduels.core.InventorySnapshot;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Common contract for active 1v1 duel matches.
 */
public interface Match {

    UUID getMatchId();

    UUID getPlayer1Id();

    UUID getPlayer2Id();

    Player getPlayer1();

    Player getPlayer2();

    Arena getArena();

    DuelType getType();

    DuelBet getBet();

    boolean isRoyal();

    MatchState getState();

    boolean isEnded();

    long getStartTime();

    long getDurationSeconds();

    Set<UUID> getSpectators();

    void addSpectator(Player player);

    void removeSpectator(Player player);

    int getSpectatorCount();

    void start();

    void end(UUID winnerId, MatchEndReason reason);

    void handleDisconnect(Player player);

    void registerDamage(Player attacker, Player victim, double damage);

    double getDamageDealt(UUID player);

    int getPoints(UUID player);

    void addPoints(UUID player, int points);

    InventorySnapshot getSnapshot(UUID player);

    MatchResult getResult();

    boolean containsPlayer(UUID uuid);

    Player getOpponent(UUID uuid);

    boolean hasRounds();

    int getTimeLimitSeconds();

    int getRemainingSeconds();
}
