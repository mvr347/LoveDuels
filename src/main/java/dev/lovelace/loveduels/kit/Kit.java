package dev.lovelace.loveduels.kit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Duel kit containing fixed inventory, armor, offhand, and potion effects.
 */
public record Kit(
        String id,
        String displayName,
        ItemStack icon,
        ItemStack[] storageContents,
        ItemStack[] armorContents,
        ItemStack offhand,
        List<PotionEffect> effects
) {
    public static Kit fromPlayer(String id, String displayName, Player player) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        ItemStack[] clonedStorage = new ItemStack[storage.length];
        for (int i = 0; i < storage.length; i++) {
            clonedStorage[i] = storage[i] != null ? storage[i].clone() : null;
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack[] clonedArmor = new ItemStack[armor.length];
        for (int i = 0; i < armor.length; i++) {
            clonedArmor[i] = armor[i] != null ? armor[i].clone() : null;
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        ItemStack clonedOffhand = offhand != null ? offhand.clone() : null;

        ItemStack icon = player.getInventory().getItemInMainHand();
        if (icon == null || icon.getType().isAir()) {
            icon = new ItemStack(Material.IRON_SWORD);
        } else {
            icon = icon.clone();
        }

        return new Kit(
                id.toLowerCase(),
                displayName,
                icon,
                clonedStorage,
                clonedArmor,
                clonedOffhand,
                new ArrayList<>(player.getActivePotionEffects())
        );
    }

    public void apply(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        ItemStack[] clonedStorage = new ItemStack[storageContents.length];
        for (int i = 0; i < storageContents.length; i++) {
            clonedStorage[i] = storageContents[i] != null ? storageContents[i].clone() : null;
        }
        player.getInventory().setStorageContents(clonedStorage);

        ItemStack[] clonedArmor = new ItemStack[armorContents.length];
        for (int i = 0; i < armorContents.length; i++) {
            clonedArmor[i] = armorContents[i] != null ? armorContents[i].clone() : null;
        }
        player.getInventory().setArmorContents(clonedArmor);

        if (offhand != null) {
            player.getInventory().setItemInOffHand(offhand.clone());
        }

        if (effects != null) {
            for (PotionEffect effect : effects) {
                player.addPotionEffect(effect);
            }
        }
    }
}
