package dev.lovelace.loveduels.core;

import dev.lovelace.loveduels.arena.ArenaManager;
import dev.lovelace.loveduels.arena.SimpleArenaManager;
import dev.lovelace.loveduels.integration.DiscordWebhookSender;
import dev.lovelace.loveduels.integration.LoveBehaviorBridge;
import dev.lovelace.loveduels.integration.LoveEconomyBridge;
import dev.lovelace.loveduels.integration.LoveLeaderboardsBridge;
import dev.lovelace.loveduels.kit.KitManager;
import dev.lovelace.loveduels.kit.SimpleKitManager;
import dev.lovelace.loveduels.match.MatchManager;
import dev.lovelace.loveduels.match.SimpleMatchManager;
import dev.lovelace.loveduels.royal.RoyalDuelManager;
import dev.lovelace.loveduels.spectator.SimpleSpectatorManager;
import dev.lovelace.loveduels.spectator.SpectatorManager;
import dev.lovelace.loveduels.storage.Database;
import dev.lovelace.loveduels.storage.PlayerStorage;
import dev.lovelace.loveduels.storage.SqlitePlayerStorage;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * Main coordinator linking storage, matches, arenas, kits, spectators, and economy bridges.
 */
public final class DuelManager {

    private final Plugin plugin;
    private final Database database;
    private final PlayerStorage playerStorage;
    private final CooldownManager cooldownManager;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final LoveEconomyBridge economyBridge;
    private final LoveBehaviorBridge behaviorBridge;
    private final LoveLeaderboardsBridge leaderboardsBridge;
    private final DiscordWebhookSender discordSender;
    private final RoyalDuelManager royalManager;
    private final SimpleMatchManager matchManager;
    private final SimpleSpectatorManager spectatorManager;

    public DuelManager(Plugin plugin) {
        this.plugin = plugin;
        this.database = new Database(new File(plugin.getDataFolder(), "data.db"), plugin.getLogger());
        this.playerStorage = new SqlitePlayerStorage(database);
        this.cooldownManager = new CooldownManager();

        this.arenaManager = new SimpleArenaManager(plugin);
        this.arenaManager.loadArenas();

        this.kitManager = new SimpleKitManager(plugin);
        this.kitManager.loadKits();

        this.economyBridge = new LoveEconomyBridge();
        this.behaviorBridge = new LoveBehaviorBridge(plugin.getLogger());
        this.leaderboardsBridge = new LoveLeaderboardsBridge();

        String webhookUrl = plugin.getConfig().getString("discord.webhook_url", "");
        this.discordSender = new DiscordWebhookSender(webhookUrl, plugin.getLogger());

        this.royalManager = new RoyalDuelManager(plugin, discordSender);

        this.matchManager = new SimpleMatchManager(
                plugin, arenaManager, kitManager, playerStorage,
                cooldownManager, economyBridge, behaviorBridge, leaderboardsBridge, royalManager
        );

        this.spectatorManager = new SimpleSpectatorManager(plugin, economyBridge);
        this.matchManager.setSpectatorManager(this.spectatorManager);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Database getDatabase() {
        return database;
    }

    public PlayerStorage getPlayerStorage() {
        return playerStorage;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public LoveEconomyBridge getEconomyBridge() {
        return economyBridge;
    }

    public LoveBehaviorBridge getBehaviorBridge() {
        return behaviorBridge;
    }

    public LoveLeaderboardsBridge getLeaderboardsBridge() {
        return leaderboardsBridge;
    }

    public RoyalDuelManager getRoyalManager() {
        return royalManager;
    }

    public SimpleMatchManager getMatchManager() {
        return matchManager;
    }

    public SimpleSpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    public void shutdown() {
        spectatorManager.cleanupAll();
        arenaManager.saveArenas();
        kitManager.saveKits();
        database.close();
    }
}
