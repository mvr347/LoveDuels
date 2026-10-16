package dev.lovelace.loveduels.integration;

import dev.lovelace.loveduels.util.Notify;
import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.economy.LoveEconomy;
import dev.lovelace.loveduels.storage.PayoutStore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Bridge to LoveEconomy (LoveCore's physical coin economy).
 *
 * <p>Payouts never disappear: if the recipient is offline (or the economy is unavailable) at the
 * moment of payout, the amount is written to {@code pending_payouts} and delivered on their next
 * join. Stakes charged for a running duel are tracked in {@code pending_escrow} so that a hard
 * server crash refunds them on the next start.</p>
 */
public final class LoveEconomyBridge {

    private final org.bukkit.plugin.Plugin plugin;
    private final PayoutStore payouts;

    public LoveEconomyBridge(org.bukkit.plugin.Plugin plugin, PayoutStore payouts) {
        this.plugin = plugin;
        this.payouts = payouts;
    }

    public boolean isAvailable() {
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LoveCore")) {
            return false;
        }
        try {
            return LoveCore.service(LoveEconomy.class).isPresent();
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().fine("[LoveDuels] LoveEconomy not available: " + t.getMessage());
            return false;
        }
    }

    public long getBalance(Player player) {
        if (player == null) return 0L;
        return economy().map(eco -> eco.balance(player)).orElse(0L);
    }

    public boolean has(Player player, long amount) {
        if (player == null) return false;
        if (amount <= 0) return true;
        return economy().map(eco -> eco.has(player, amount)).orElse(false);
    }

    public boolean charge(Player player, long amount) {
        if (player == null) return false;
        if (amount <= 0) return true;
        return economy().map(eco -> eco.charge(player, amount)).orElse(false);
    }

    /** Pays the player now if online, otherwise queues the amount for their next join. */
    public void give(Player player, long amount) {
        if (player == null) return;
        giveTo(player.getUniqueId(), amount);
    }

    /** Same as {@link #give(Player, long)} but by UUID, for callers whose {@code Player} lookup came back null. */
    public void giveTo(UUID playerId, long amount) {
        if (playerId == null || amount <= 0) return;
        Player online = Bukkit.getPlayer(playerId);
        Optional<LoveEconomy> eco = economy();
        if (online != null && online.isOnline() && eco.isPresent()) {
            eco.get().give(online, amount);
        } else {
            payouts.queue(playerId, amount);
        }
    }

    /** Switch DB bookkeeping to synchronous mode; call first thing in onDisable. */
    public void enterShutdownMode() {
        payouts.enterSynchronousMode();
    }

    /** Delivers everything queued for the player; called on join. The DB read is off the main thread. */
    public void deliverPending(Player player) {
        if (player == null || !player.isOnline()) return;
        if (economy().isEmpty()) return;
        UUID id = player.getUniqueId();
        payouts.takePendingAsync(id).thenAccept(owed -> {
            if (owed <= 0) return;
            Runnable pay = () -> {
                Player p = Bukkit.getPlayer(id);
                Optional<LoveEconomy> eco = economy();
                if (p == null || !p.isOnline() || eco.isEmpty()) {
                    // Row is already removed: put the coins back instead of losing them
                    payouts.queue(id, owed);
                    return;
                }
                eco.get().give(p, owed);
                Notify.important(p, dev.lovelace.loveduels.util.CoinFormat.component(p,
                        "<green>💰 Вам выплачено</green> " + dev.lovelace.loveduels.util.CoinFormat.amount(owed)
                                + " <green>по завершённым дуэлям, пока вы были не в сети.</green>"));
            };
            try {
                Bukkit.getScheduler().runTask(plugin, pay);
            } catch (RuntimeException e) {
                // Plugin disabled between read and callback
                payouts.queue(id, owed);
            }
        });
    }

    /** Records the stakes just charged from both fighters so a crash cannot swallow them. */
    public void holdEscrow(UUID challenger, UUID target, long amountEach) {
        payouts.holdEscrow(challenger, target, amountEach);
    }

    /** Duel finished (or was refunded) - the stakes are accounted for. */
    public void releaseEscrow(UUID challenger) {
        payouts.releaseEscrow(challenger);
    }

    public String currencyName() {
        return economy().map(LoveEconomy::currencyName).orElse("монет");
    }

    private Optional<LoveEconomy> economy() {
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LoveCore")) {
            return Optional.empty();
        }
        try {
            return LoveCore.service(LoveEconomy.class);
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().fine("[LoveDuels] Error loading LoveEconomy service: " + t.getMessage());
            return Optional.empty();
        }
    }
}
