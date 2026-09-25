package dev.lovelace.loveduels.match.spear;

import dev.lovelace.loveduels.match.Match;
import dev.lovelace.loveduels.match.MatchEndReason;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles charging mechanics, raytracing, impact angle, power calculations,
 * and point rewards for Horse + Spear tournament duels.
 */
public final class SpearChargeHandler {

    private final Map<UUID, ChargeData> charges = new ConcurrentHashMap<>();

    public void startCharge(Player player) {
        charges.put(player.getUniqueId(), new ChargeData(System.currentTimeMillis()));
        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, 0.8f, 1.2f);
    }

    public boolean isCharging(Player player) {
        return charges.containsKey(player.getUniqueId());
    }

    public double getChargeRatio(Player player) {
        ChargeData data = charges.get(player.getUniqueId());
        if (data == null) return 0.0;
        long elapsed = System.currentTimeMillis() - data.startTime();
        return Math.min(1.0, elapsed / 1500.0);
    }

    public String getChargeBar(Player player) {
        ChargeData data = charges.get(player.getUniqueId());
        if (data == null) return "<gray>[----------]";
        long elapsed = System.currentTimeMillis() - data.startTime();
        if (elapsed >= 1200 && elapsed <= 1800) {
            return "<green><b>[██████████] ИДЕАЛЬНО!</b></green>";
        } else if (elapsed < 1200) {
            int bars = (int) ((elapsed / 1200.0) * 10);
            return "<yellow>[" + "█".repeat(Math.max(1, bars)) + "-".repeat(Math.max(0, 10 - bars)) + "] Зарядка...";
        } else {
            return "<red>[██████████] ПЕРЕЗАРЯД!</red>";
        }
    }

    public void stopCharge(Player player, Match match) {
        ChargeData data = charges.remove(player.getUniqueId());
        if (data == null) return;

        long chargedMs = System.currentTimeMillis() - data.startTime();
        double power = calculatePower(chargedMs);

        performThrust(player, power, chargedMs, match);
    }

    public void cancelCharge(Player player) {
        charges.remove(player.getUniqueId());
    }

    private double calculatePower(long chargedMs) {
        // Optimal window 1200–1800 ms
        if (chargedMs >= 1200 && chargedMs <= 1800) {
            return 1.0; // Perfect
        }
        if (chargedMs < 1200) {
            return Math.max(0.3, chargedMs / 1200.0);
        }
        // Overcharged
        long over = chargedMs - 1800;
        return Math.max(0.4, 1.0 - (over / 1500.0));
    }

    private void performThrust(Player player, double power, long chargedMs, Match match) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        // Raytrace forward up to 4.5 blocks
        RayTraceResult hit = player.getWorld().rayTraceEntities(
                eye,
                dir,
                4.5,
                0.8,
                entity -> !entity.equals(player) && !entity.equals(player.getVehicle())
        );

        // Visual effects for thrust
        Location tip = eye.clone().add(dir.clone().multiply(2.5));
        player.getWorld().spawnParticle(Particle.CRIT, tip, 12, 0.2, 0.2, 0.2, 0.1);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, tip, 1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        if (hit == null || hit.getHitEntity() == null) {
            return;
        }

        Entity targetEntity = hit.getHitEntity();
        Player targetPlayer = null;
        Horse targetHorse = null;

        if (targetEntity instanceof Player p) {
            targetPlayer = p;
            if (p.getVehicle() instanceof Horse h) {
                targetHorse = h;
            }
        } else if (targetEntity instanceof Horse h) {
            targetHorse = h;
            if (h.getPassengers().size() > 0 && h.getPassengers().getFirst() instanceof Player p) {
                targetPlayer = p;
            }
        }

        if (targetPlayer == null || !match.containsPlayer(targetPlayer.getUniqueId())) {
            return;
        }

        // Check impact angle
        Vector toTarget = targetPlayer.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        double dot = dir.dot(toTarget);
        if (dot < 0.5) {
            // Glancing blow
            return;
        }

        boolean isPerfect = (power >= 0.95 && dot >= 0.80);
        int pointsAwarded = isPerfect ? 2 : 1;
        double damage = isPerfect ? 7.0 : 3.5;

        // Apply damage and points
        targetPlayer.damage(damage, player);
        match.registerDamage(player, targetPlayer, damage);
        match.addPoints(player.getUniqueId(), pointsAwarded);

        // Knockback vector
        Vector knockback = dir.clone().multiply(isPerfect ? 1.4 : 0.8).setY(0.35);
        if (targetHorse != null) {
            targetHorse.setVelocity(knockback);
            targetHorse.getWorld().playSound(targetHorse.getLocation(), Sound.ENTITY_HORSE_HURT, 1.0f, 0.9f);

            // Disorient opponent horse for 1.2s
            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 2));
        } else {
            targetPlayer.setVelocity(knockback);
        }

        if (isPerfect) {
            targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.1f);
            targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 0.8f);
            targetPlayer.getWorld().spawnParticle(Particle.EXPLOSION, hit.getHitPosition().toLocation(targetPlayer.getWorld()), 1);
            targetPlayer.getWorld().spawnParticle(Particle.FLASH, hit.getHitPosition().toLocation(targetPlayer.getWorld()), 2);

            Title perfectTitle = Title.title(
                    MiniMessage.miniMessage().deserialize("<gold>★ <b>ИДЕАЛЬНЫЙ УДАР!</b> ★</gold>"),
                    MiniMessage.miniMessage().deserialize("<green>+2 очка!"),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(300))
            );
            player.showTitle(perfectTitle);

            Title hitTitle = Title.title(
                    MiniMessage.miniMessage().deserialize("<red>💥 <b>СОКРУШИТЕЛЬНЫЙ УДАР!</b></red>"),
                    MiniMessage.miniMessage().deserialize("<gray>Противник выбил вас из равновесия"),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(300))
            );
            targetPlayer.showTitle(hitTitle);
        } else {
            targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.2f, 1.2f);
            targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 1.0f);

            player.sendActionBar(MiniMessage.miniMessage().deserialize("<green>⚔ Попадание! +1 очко"));
            targetPlayer.sendActionBar(MiniMessage.miniMessage().deserialize("<red>💥 Удар получен!"));
        }

        // Check win condition (first to 5 points)
        if (match.getPoints(player.getUniqueId()) >= 5) {
            match.end(player.getUniqueId(), MatchEndReason.POINTS_REACHED);
        }
    }

    public record ChargeData(long startTime) {}
}
