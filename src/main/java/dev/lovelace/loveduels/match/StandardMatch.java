package dev.lovelace.loveduels.match;

import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.core.DuelBet;
import dev.lovelace.loveduels.core.DuelType;
import dev.lovelace.loveduels.integration.LoveBehaviorBridge;
import dev.lovelace.loveduels.integration.LoveEconomyBridge;
import dev.lovelace.loveduels.integration.LoveLeaderboardsBridge;
import dev.lovelace.loveduels.kit.Kit;
import dev.lovelace.loveduels.royal.RoyalDuelManager;
import dev.lovelace.loveduels.storage.PlayerStorage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class StandardMatch extends AbstractMatch {

    private final Kit kit;

    public StandardMatch(
            Plugin plugin,
            Player player1,
            Player player2,
            Arena arena,
            DuelType type,
            Kit kit,
            DuelBet bet,
            boolean royal,
            PlayerStorage playerStorage,
            LoveEconomyBridge economyBridge,
            LoveBehaviorBridge behaviorBridge,
            LoveLeaderboardsBridge leaderboardsBridge,
            RoyalDuelManager royalManager,
            MatchManager matchManager
    ) {
        super(plugin, player1, player2, arena, type, bet, royal, playerStorage, economyBridge, behaviorBridge, leaderboardsBridge, royalManager, matchManager);
        this.kit = kit;
    }

    @Override
    protected void setupEquipment() {
        switch (type) {
            case KIT -> {
                if (kit != null) {
                    kit.apply(player1);
                    kit.apply(player2);
                }
            }
            case SWORD -> {
                equipClassicSword(player1);
                equipClassicSword(player2);
            }
            case BOW -> {
                equipClassicBow(player1);
                equipClassicBow(player2);
            }
            case OWN_INVENTORY -> {
                // Players fight in their own gear (already snapped)
            }
            default -> {}
        }
    }

    private void equipClassicSword(Player player) {
        player.getInventory().clear();
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack steak = new ItemStack(Material.COOKED_BEEF, 16);
        player.getInventory().setItem(0, sword);
        player.getInventory().setItem(1, steak);
    }

    private void equipClassicBow(Player player) {
        player.getInventory().clear();
        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));

        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.POWER, 1);
        bow.addEnchantment(Enchantment.INFINITY, 1);

        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemStack arrow = new ItemStack(Material.ARROW, 1);
        ItemStack steak = new ItemStack(Material.COOKED_BEEF, 16);

        player.getInventory().setItem(0, bow);
        player.getInventory().setItem(1, sword);
        player.getInventory().setItem(2, steak);
        player.getInventory().setItem(9, arrow);
    }

    @Override
    protected void cleanupCustomEntities() {
        // No custom summoned entities in standard match
    }
}
