package dev.lovelace.loveduels.match;

import dev.lovelace.loveduels.arena.Arena;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * После конца матча на короткое время блокирует PvP в зоне арены
 * (пока бойцы ещё на арене / до телепорта).
 */
public final class PostMatchGuard {

    private final Plugin plugin;
    private final Map<UUID, Arena> locked = new ConcurrentHashMap<>();

    public PostMatchGuard(Plugin plugin) {
        this.plugin = plugin;
    }

    public void lock(UUID p1, UUID p2, Arena arena) {
        if (arena == null) return;
        int seconds = Math.max(0, plugin.getConfig().getInt("settings.post_match_pvp_disable_seconds", 20));
        if (seconds <= 0) return;

        locked.put(p1, arena);
        locked.put(p2, arena);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            locked.remove(p1);
            locked.remove(p2);
        }, seconds * 20L);
    }

    public boolean isProtected(UUID uuid) {
        return locked.containsKey(uuid);
    }

    public boolean blocksDamage(Player attacker, Player victim) {
        Arena a1 = locked.get(attacker.getUniqueId());
        Arena a2 = locked.get(victim.getUniqueId());
        if (a1 == null && a2 == null) return false;

        Arena arena = a1 != null ? a1 : a2;
        Location la = attacker.getLocation();
        Location lv = victim.getLocation();
        // Блокируем, если хотя бы один ещё в зоне арены
        boolean inArena = arena.isInCombatBounds(la) || arena.isInCombatBounds(lv)
                || (arena.getBounds() != null && (arena.getBounds().contains(la.getX(), la.getY(), la.getZ())
                || arena.getBounds().contains(lv.getX(), lv.getY(), lv.getZ())));
        return inArena;
    }

    public void clear(UUID uuid) {
        locked.remove(uuid);
    }
}
