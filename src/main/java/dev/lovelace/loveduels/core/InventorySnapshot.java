package dev.lovelace.loveduels.core;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Complete immutable snapshot of a player's state before entering a duel or spectating.
 * Prevents item duplication and fully restores everything upon exit.
 */
public final class InventorySnapshot {

    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private final int level;
    private final float exp;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int fireTicks;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean isFlying;
    private final Location returnLocation;
    private final List<PotionEffect> activePotionEffects;

    private InventorySnapshot(
            ItemStack[] storage,
            ItemStack[] armor,
            ItemStack offhand,
            int level,
            float exp,
            double health,
            int foodLevel,
            float saturation,
            int fireTicks,
            GameMode gameMode,
            boolean allowFlight,
            boolean isFlying,
            Location returnLocation,
            Collection<PotionEffect> effects
    ) {
        this.storage = storage;
        this.armor = armor;
        this.offhand = offhand;
        this.level = level;
        this.exp = exp;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.fireTicks = fireTicks;
        this.gameMode = gameMode;
        this.allowFlight = allowFlight;
        this.isFlying = isFlying;
        this.returnLocation = returnLocation != null ? returnLocation.clone() : null;
        this.activePotionEffects = new ArrayList<>(effects);
    }

    public static InventorySnapshot of(Player player) {
        ItemStack[] originalStorage = player.getInventory().getStorageContents();
        ItemStack[] clonedStorage = new ItemStack[originalStorage.length];
        for (int i = 0; i < originalStorage.length; i++) {
            clonedStorage[i] = originalStorage[i] != null ? originalStorage[i].clone() : null;
        }

        ItemStack[] originalArmor = player.getInventory().getArmorContents();
        ItemStack[] clonedArmor = new ItemStack[originalArmor.length];
        for (int i = 0; i < originalArmor.length; i++) {
            clonedArmor[i] = originalArmor[i] != null ? originalArmor[i].clone() : null;
        }

        ItemStack originalOffhand = player.getInventory().getItemInOffHand();
        ItemStack clonedOffhand = originalOffhand != null ? originalOffhand.clone() : null;

        return new InventorySnapshot(
                clonedStorage,
                clonedArmor,
                clonedOffhand,
                player.getLevel(),
                player.getExp(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getFireTicks(),
                player.getGameMode(),
                player.getAllowFlight(),
                player.isFlying(),
                player.getLocation(),
                player.getActivePotionEffects()
        );
    }

    /**
     * Fully resets and restores the player to their previous state.
     */
    public void restore(Player player) {
        // Clean current state first
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        // Remove active effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Restore items
        player.getInventory().setStorageContents(storage);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setItemInOffHand(offhand);

        // Restore level and exp
        player.setLevel(level);
        player.setExp(exp);

        // Restore attributes and stats
        var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
        player.setHealth(Math.min(health, maxHealth));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setFireTicks(fireTicks);

        // Restore gamemode and flight
        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(isFlying);

        // Restore potion effects
        for (PotionEffect effect : activePotionEffects) {
            player.addPotionEffect(effect);
        }

        // Teleport back if valid
        if (returnLocation != null && returnLocation.getWorld() != null) {
            player.teleportAsync(returnLocation);
        }
    }

    public Location getReturnLocation() {
        return returnLocation != null ? returnLocation.clone() : null;
    }
}
