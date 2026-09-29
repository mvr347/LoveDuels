package dev.lovelace.loveduels.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/** Pays out duel money that was queued while the player was offline. */
public final class PayoutListener implements Listener {

    private final Plugin plugin;
    private final LoveEconomyBridge economyBridge;

    public PayoutListener(Plugin plugin, LoveEconomyBridge economyBridge) {
        this.plugin = plugin;
        this.economyBridge = economyBridge;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Give the inventory a moment to be fully loaded before coins are added to it.
        Bukkit.getScheduler().runTaskLater(plugin, () -> economyBridge.deliverPending(player), 40L);
    }

    /** For players already online when the plugin (re)loads. */
    public void deliverToOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            economyBridge.deliverPending(player);
        }
    }
}
