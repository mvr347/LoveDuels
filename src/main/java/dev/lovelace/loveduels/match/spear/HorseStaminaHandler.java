package dev.lovelace.loveduels.match.spear;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks horse stamina, sprint exhaustion, and dash ability during jousting duels.
 */
public final class HorseStaminaHandler {

    private final Map<UUID, Double> stamina = new ConcurrentHashMap<>();
    private final Map<UUID, Long> dashCooldown = new ConcurrentHashMap<>();

    public double getStamina(Player player) {
        return stamina.getOrDefault(player.getUniqueId(), 100.0);
    }

    public void setStamina(Player player, double value) {
        stamina.put(player.getUniqueId(), Math.max(0.0, Math.min(100.0, value)));
    }

    public void tick(Player player, Horse horse) {
        UUID uuid = player.getUniqueId();
        double current = stamina.getOrDefault(uuid, 100.0);

        if (player.isSprinting()) {
            // Drain stamina when sprinting
            current = Math.max(0.0, current - 1.5);
            if (current <= 10.0) {
                // Exhausted: slow down horse
                horse.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
                horse.getWorld().spawnParticle(Particle.SMOKE, horse.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
            }
        } else {
            // Regenerate stamina
            current = Math.min(100.0, current + 2.0);
        }

        stamina.put(uuid, current);
    }

    public boolean performDash(Player player, Horse horse) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long cd = dashCooldown.get(uuid);
        if (cd != null && now < cd) {
            return false;
        }

        double current = stamina.getOrDefault(uuid, 100.0);
        if (current < 30.0) {
            return false;
        }

        // Apply dash
        stamina.put(uuid, current - 30.0);
        dashCooldown.put(uuid, now + 4000); // 4 second cooldown

        Vector dir = player.getLocation().getDirection().setY(0.1).normalize().multiply(1.5);
        horse.setVelocity(dir);
        horse.getWorld().playSound(horse.getLocation(), Sound.ENTITY_HORSE_GALLOP, 1.2f, 1.4f);
        horse.getWorld().spawnParticle(Particle.CLOUD, horse.getLocation().add(0, 0.5, 0), 10, 0.3, 0.2, 0.3, 0.1);
        return true;
    }

    public void remove(Player player) {
        stamina.remove(player.getUniqueId());
        dashCooldown.remove(player.getUniqueId());
    }
}
