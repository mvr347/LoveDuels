package dev.lovelace.loveduels.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Эффекты появления на арене при старте дуэли.
 * Конфиг и разблокировка по рангам — в следующих итерациях.
 */
public enum AppearanceEffect {
    NONE(null, null, 0),
    SPARK(Particle.CRIT, Sound.ENTITY_PLAYER_ATTACK_CRIT, 24),
    FLAME(Particle.FLAME, Sound.ITEM_FIRECHARGE_USE, 20),
    SOUL(Particle.SOUL_FIRE_FLAME, Sound.PARTICLE_SOUL_ESCAPE, 18),
    ROYAL(Particle.END_ROD, Sound.UI_TOAST_CHALLENGE_COMPLETE, 32);

    private final Particle particle;
    private final Sound sound;
    private final int count;

    AppearanceEffect(Particle particle, Sound sound, int count) {
        this.particle = particle;
        this.sound = sound;
        this.count = count;
    }

    public void play(Player player) {
        if (this == NONE || player == null) return;
        Location loc = player.getLocation().add(0, 1, 0);
        if (particle != null) {
            player.getWorld().spawnParticle(particle, loc, count, 0.4, 0.6, 0.4, 0.02);
        }
        if (sound != null) {
            player.getWorld().playSound(loc, sound, 0.7f, 1.1f);
        }
    }

    public static AppearanceEffect defaultForRoyal(boolean royal) {
        return royal ? ROYAL : SPARK;
    }
}
