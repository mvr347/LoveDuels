package dev.lovelace.loveduels.arena;

import dev.lovelace.loveduels.core.CombatCategory;
import dev.lovelace.loveduels.core.DuelType;
import org.bukkit.Location;
import org.bukkit.World;
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
    /** Arena type chosen at creation; null with a non-empty type set means "all modes". */
    private CombatCategory category;
    /** Empty until a type is chosen; an arena without types is never offered to duels. */
    private final Set<DuelType> supportedTypes = EnumSet.noneOf(DuelType.class);
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

    public CombatCategory getCategory() {
        return category;
    }

    /** Restricts the arena to the duel types of one category (melee / ranged / mounted). */
    public void setCategory(CombatCategory category) {
        this.category = category;
        supportedTypes.clear();
        if (category != null) {
            supportedTypes.addAll(category.getSubtypes());
        }
    }

    /** Makes the arena usable for every duel type (no category restriction). */
    public void setAllTypes() {
        this.category = null;
        supportedTypes.clear();
        supportedTypes.addAll(EnumSet.allOf(DuelType.class));
    }

    public boolean hasType() {
        return !supportedTypes.isEmpty();
    }

    /** Human readable arena type for admin output. */
    public String getTypeLabel() {
        if (category != null) return category.getDisplayName();
        return supportedTypes.isEmpty() ? "не задан" : "Все режимы";
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

    public World getWorld() {
        if (pos1 != null && pos1.getWorld() != null) return pos1.getWorld();
        if (pos2 != null && pos2.getWorld() != null) return pos2.getWorld();
        if (spectatorSpawn != null && spectatorSpawn.getWorld() != null) return spectatorSpawn.getWorld();
        return null;
    }

    public String getWorldName() {
        World w = getWorld();
        return w != null ? w.getName() : "не задан";
    }

    public java.util.List<String> getValidationErrors() {
        java.util.List<String> errors = new java.util.ArrayList<>();
        if (supportedTypes.isEmpty()) {
            errors.add("Тип арены не задан (/lda arena type <id> <melee|ranged|mounted|all>)");
        }
        if (pos1 == null) {
            errors.add("Точка спавна 1 (pos1) не установлена");
        } else if (pos1.getWorld() == null) {
            errors.add("Мир точки спавна 1 не загружен или не существует");
        }
        if (pos2 == null) {
            errors.add("Точка спавна 2 (pos2) не установлена");
        } else if (pos2.getWorld() == null) {
            errors.add("Мир точки спавна 2 не загружен или не существует");
        }
        if (pos1 != null && pos2 != null && pos1.getWorld() != null && pos2.getWorld() != null) {
            if (!pos1.getWorld().equals(pos2.getWorld())) {
                errors.add("Спавны 1 и 2 в разных мирах (" + pos1.getWorld().getName() + " и " + pos2.getWorld().getName() + ")");
            } else if (pos1.distanceSquared(pos2) < 4.0) {
                errors.add("Точки спавна расположены слишком близко (< 2 блоков)");
            }
        }
        if (bounds != null) {
            if (pos1 != null && !isInCombatBounds(pos1)) {
                errors.add("Спавн 1 находится ВНЕ боевых границ арены");
            }
            if (pos2 != null && !isInCombatBounds(pos2)) {
                errors.add("Спавн 2 находится ВНЕ боевых границ арены");
            }
        }
        if (spectatorSpawn != null && pos1 != null && spectatorSpawn.getWorld() != null && pos1.getWorld() != null) {
            if (!spectatorSpawn.getWorld().equals(pos1.getWorld())) {
                errors.add("Спавн зрителей находится в другом мире (" + spectatorSpawn.getWorld().getName() + ")");
            }
        }
        return errors;
    }

    public java.util.List<String> getValidationWarnings() {
        java.util.List<String> warnings = new java.util.ArrayList<>();
        if (bounds == null) {
            warnings.add("Боевые границы не заданы (бойцы могут убегать за пределы)");
        }
        if (spectatorSpawn == null) {
            warnings.add("Спавн зрителей не задан (будет точка над спавном 1)");
        }
        if (spectatorZone == null && bounds != null) {
            warnings.add("Зона трибун не задана (зрителям доступна зона вокруг арены)");
        }
        return warnings;
    }

    public boolean isConfigured() {
        return getValidationErrors().isEmpty();
    }

    public boolean isAvailableFor(DuelType type) {
        return enabled && state == ArenaState.FREE && isConfigured() && supportsType(type);
    }

    public boolean isInCombatBounds(Location loc) {
        if (bounds == null) return true;
        if (loc == null || loc.getWorld() == null) return false;
        World w = getWorld();
        if (w != null && !loc.getWorld().equals(w)) return false;
        return loc.getX() >= bounds.getMinX() - 0.05 && loc.getX() <= bounds.getMaxX() + 0.05
                && loc.getY() >= bounds.getMinY() - 0.20 && loc.getY() <= bounds.getMaxY() + 0.20
                && loc.getZ() >= bounds.getMinZ() - 0.05 && loc.getZ() <= bounds.getMaxZ() + 0.05;
    }

    public boolean isInSpectatorZone(Location loc) {
        if (spectatorZone == null) {
            if (bounds != null) {
                return loc.getX() >= bounds.getMinX() - 10.0 && loc.getX() <= bounds.getMaxX() + 10.0
                        && loc.getY() >= bounds.getMinY() - 5.0 && loc.getY() <= bounds.getMaxY() + 15.0
                        && loc.getZ() >= bounds.getMinZ() - 10.0 && loc.getZ() <= bounds.getMaxZ() + 10.0;
            }
            return true;
        }
        if (loc == null || loc.getWorld() == null) return false;
        World w = getWorld();
        if (w != null && !loc.getWorld().equals(w)) return false;
        return loc.getX() >= spectatorZone.getMinX() - 0.1 && loc.getX() <= spectatorZone.getMaxX() + 0.1
                && loc.getY() >= spectatorZone.getMinY() - 0.2 && loc.getY() <= spectatorZone.getMaxY() + 0.2
                && loc.getZ() >= spectatorZone.getMinZ() - 0.1 && loc.getZ() <= spectatorZone.getMaxZ() + 0.1;
    }

    public boolean setBoundsHeight(double height) {
        if (bounds == null || height < 2.0) return false;
        this.bounds = new BoundingBox(
                bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMinY() + height, bounds.getMaxZ()
        );
        return true;
    }

    /** A category arena may only fine-tune types inside its own category. */
    public boolean canToggleType(DuelType type) {
        return category == null || category.getSubtypes().contains(type);
    }

    public void toggleSupportedType(DuelType type) {
        if (!canToggleType(type)) return;
        if (supportedTypes.contains(type)) {
            if (supportedTypes.size() > 1) {
                supportedTypes.remove(type);
            }
        } else {
            supportedTypes.add(type);
        }
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
