package dev.lovelace.loveduels;

import dev.lovelace.loveduels.command.DuelCommand;
import dev.lovelace.loveduels.command.LoveDuelsAdminCommand;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.gui.GuiListener;
import dev.lovelace.loveduels.gui.PostDuelSummaryGUI;
import dev.lovelace.loveduels.integration.LoveDuelsPlaceholderExpansion;
import dev.lovelace.loveduels.match.Match;
import dev.lovelace.loveduels.match.MatchEndReason;
import dev.lovelace.loveduels.match.MatchProtectionListener;
import dev.lovelace.loveduels.match.spear.SpearDuelListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LoveDuels extends JavaPlugin {

    private DuelManager duelManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.duelManager = new DuelManager(this);

        // Configure post-duel summary GUI callback
        duelManager.getMatchManager().setPostDuelSummaryOpener(res -> {
            Bukkit.getScheduler().runTask(this, () -> {
                Player p1 = Bukkit.getPlayer(res.winnerId());
                Player p2 = Bukkit.getPlayer(res.loserId());
                if (p1 != null && p1.isOnline()) {
                    new PostDuelSummaryGUI(p1, res, duelManager).open();
                }
                if (p2 != null && p2.isOnline()) {
                    new PostDuelSummaryGUI(p2, res, duelManager).open();
                }
            });
        });

        // Register event listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new GuiListener(), this);
        pm.registerEvents(new MatchProtectionListener(this, duelManager.getMatchManager(), duelManager.getSpectatorManager()), this);
        pm.registerEvents(new SpearDuelListener(
                duelManager.getMatchManager(),
                duelManager.getMatchManager().getSpearItem(),
                duelManager.getMatchManager().getChargeHandler(),
                duelManager.getMatchManager().getStaminaHandler()
        ), this);
        pm.registerEvents(duelManager.getSpectatorManager().getZoneGuard(), this);

        // Register commands
        var duelCmd = getCommand("duel");
        if (duelCmd != null) {
            DuelCommand cmd = new DuelCommand(duelManager);
            duelCmd.setExecutor(cmd);
            duelCmd.setTabCompleter(cmd);
        }

        var adminCmd = getCommand("loveduelsadmin");
        if (adminCmd != null) {
            LoveDuelsAdminCommand cmd = new LoveDuelsAdminCommand(duelManager);
            adminCmd.setExecutor(cmd);
            adminCmd.setTabCompleter(cmd);
        }

        // Register PlaceholderAPI expansion if hooked
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new LoveDuelsPlaceholderExpansion(this, duelManager.getPlayerStorage(), duelManager.getMatchManager()).register();
            getLogger().info("PlaceholderAPI expansion successfully hooked!");
        }

        getLogger().info("LoveDuels (Paper 1.21.x / Java 25) successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (duelManager != null) {
            // Safely finish active matches to restore player items
            for (Match match : duelManager.getMatchManager().getActiveMatches()) {
                match.end(null, MatchEndReason.ADMIN_FORCE);
            }
            duelManager.shutdown();
        }
        getLogger().info("LoveDuels successfully disabled.");
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }
}
