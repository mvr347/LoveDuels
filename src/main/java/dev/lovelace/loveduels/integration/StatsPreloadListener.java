package dev.lovelace.loveduels.integration;

import dev.lovelace.loveduels.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Warms the player-statistics cache on join. The main menu reads only the cache, so without this a player
 * who had not yet been in a duel saw "statistics are loading" until some other code happened to load them.
 */
public final class StatsPreloadListener implements Listener {

    private final PlayerStorage storage;

    public StatsPreloadListener(PlayerStorage storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        preload(event.getPlayer());
    }

    /** For players already online when the plugin (re)loads. */
    public void preloadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            preload(player);
        }
    }

    private void preload(Player player) {
        storage.getOrCreatePlayer(player.getUniqueId(), player.getName());
    }
}
