package dev.lovelace.loveduels.storage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage contract for player profiles, leaderboards, and match logs.
 */
public interface PlayerStorage {

    /**
     * Loads a player from cache or database, creating a default profile if none exists.
     */
    CompletableFuture<PlayerData> getOrCreatePlayer(UUID uuid, String name);

    /**
     * Gets a player if cached or stored.
     */
    CompletableFuture<Optional<PlayerData>> getPlayer(UUID uuid);

    /**
     * Synchronously returns cached player data if loaded in memory.
     */
    Optional<PlayerData> getCachedPlayer(UUID uuid);

    /**
     * Updates player profile asynchronously in cache and DB.
     */
    CompletableFuture<Void> savePlayer(PlayerData player);

    /**
     * Returns top players sorted by honor in descending order.
     */
    CompletableFuture<List<PlayerData>> getTopPlayersByHonor(int limit);

    /**
     * Logs match results in history table.
     */
    CompletableFuture<Void> logHistory(DuelHistoryEntry entry);

    /**
     * Gets recent match history for a given player.
     */
    CompletableFuture<List<DuelHistoryEntry>> getRecentHistory(UUID player, int limit);

    /**
     * Evicts player from memory cache.
     */
    void evict(UUID uuid);
}
