package dev.lovelace.loveduels.integration;

import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.economy.LoveEconomy;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Bridge to LoveEconomy (LoveCore's physical coin economy).
 */
public final class LoveEconomyBridge {

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

    public void give(Player player, long amount) {
        if (player == null || amount <= 0) return;
        economy().ifPresent(eco -> eco.give(player, amount));
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
