package dev.lovelace.loveduels.arena;

import dev.lovelace.loveduels.core.DuelType;

import java.util.Collection;
import java.util.Optional;

public interface ArenaManager {

    Optional<Arena> findAvailableArena(DuelType type);

    /** True when at least one enabled, fully configured arena supports the type (busy or not). */
    boolean hasArenaFor(DuelType type);

    Optional<Arena> getArena(String id);

    Collection<Arena> getAllArenas();

    void registerArena(Arena arena);

    boolean deleteArena(String id);

    void loadArenas();

    void saveArenas();
}
