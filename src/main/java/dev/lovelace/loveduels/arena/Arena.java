package dev.lovelace.loveduels.arena;

import dev.lovelace.loveduels.core.DuelType;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Representation of a duel arena with combat spawns, fighting boundaries,
 * and a strictly bounded spectator zone.
 */
public final class Arena {

    private final String id;
    private String name;
    private Location pos1;
    private Location pos2;
    private Location spectatorSpawn;
    private BoundingBox bounds;
    private BoundingBox spectatorZone;
    private final Set<DuelType> supportedTypes = EnumSet.allOf(DuelType.class);
    private boolean enabled = true;
    private ArenaState state = ArenaState.FREE;

    public Arena(String id, String name) {
        this.id = id.toLowerCase();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getPos1() {
        return pos1 != null ? pos1.clone() : null;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1 != null ? pos1.clone() : null;
    }

    public Location getPos2() {
        return pos2 != null ? pos2.clone() : null;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2 != null ? pos2.clone() : null;
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn != null ? spectatorSpawn.clone() : null;
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn != null ? spectatorSpawn.clone() : null;
    }

    public BoundingBox getBounds() {
        return bounds != null ? bounds.clone() : null;
    }

    public void setBounds(BoundingBox bounds) {
        this.bounds = bounds != null ? bounds.clone() : null;
    }

    public BoundingBox getSpectatorZone() {
        return spectatorZone != null ? spectatorZone.clone() : null;
    }

    public void setSpectatorZone(BoundingBox spectatorZone) {
        this.spectatorZone = spectatorZone != null ? spectatorZone.clone() : null;
    }

    public Set<DuelType> getSupportedTypes() {
        return supportedTypes;
    }

    public boolean supportsType(DuelType type) {
        return supportedTypes.contains(type);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            this.state = ArenaState.DISABLED;
        } else if (this.state == ArenaState.DISABLED) {
            this.state = ArenaState.FREE;
        }
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public boolean isConfigured() {
        return pos1 != null && pos2 != null
                && pos1.getWorld() != null
                && pos2.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld());
    }

    public boolean isAvailableFor(DuelType type) {
        return enabled && state == ArenaState.FREE && isConfigured() && supportsType(type);
    }

    public boolean isInCombatBounds(Location loc) {
        if (bounds == null) return true;
        if (loc == null || loc.getWorld() == null) return false;
        if (pos1 != null && !loc.getWorld().equals(pos1.getWorld())) return false;
        return bounds.contains(loc.getX(), loc.getY(), loc.getZ());
    }

    public boolean isInSpectatorZone(Location loc) {
        if (spectatorZone == null) {
            // Fallback: if no custom spectator zone is defined, allow bounds expanded by 5 blocks
            if (bounds != null) {
                return bounds.clone().expand(5.0).contains(loc.getX(), loc.getY(), loc.getZ());
            }
            return true;
        }
        if (loc == null || loc.getWorld() == null) return false;
        if (spectatorSpawn != null && !loc.getWorld().equals(spectatorSpawn.getWorld())) return false;
        return spectatorZone.contains(loc.getX(), loc.getY(), loc.getZ());
    }

    public Location getSafeSpectatorPoint() {
        if (spectatorSpawn != null) {
            return spectatorSpawn.clone();
        }
        if (pos1 != null) {
            return pos1.clone().add(0, 5, 0);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Arena arena)) return false;
        return Objects.equals(id, arena.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
