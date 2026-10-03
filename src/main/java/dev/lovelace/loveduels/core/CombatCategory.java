package dev.lovelace.loveduels.core;

import java.util.List;

/**
 * Верхний уровень выбора режима в DuelSetupGUI.
 */
public enum CombatCategory {
    MELEE(
            "melee", "Ближний бой",
            "Кулаки, рыцарское снаряжение или свои вещи.",
            List.of(DuelType.FISTS, DuelType.KIT, DuelType.OWN_INVENTORY)
    ),
    RANGED(
            "ranged", "Дальний бой",
            "Луки и арбалеты.",
            List.of(DuelType.BOW, DuelType.CROSSBOW)
    ),
    MOUNTED(
            "mounted", "Всадники",
            "Турнир на копьях верхом.",
            List.of(DuelType.HORSE_SPEAR)
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final List<DuelType> subtypes;

    CombatCategory(String id, String displayName, String description, List<DuelType> subtypes) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.subtypes = List.copyOf(subtypes);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<DuelType> getSubtypes() {
        return subtypes;
    }

    public DuelType firstSubtype() {
        return subtypes.get(0);
    }

    public DuelType nextSubtype(DuelType current) {
        int idx = subtypes.indexOf(current);
        if (idx < 0) return firstSubtype();
        return subtypes.get((idx + 1) % subtypes.size());
    }

    public DuelType prevSubtype(DuelType current) {
        int idx = subtypes.indexOf(current);
        if (idx < 0) return firstSubtype();
        return subtypes.get((idx - 1 + subtypes.size()) % subtypes.size());
    }

    public CombatCategory next() {
        CombatCategory[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public CombatCategory prev() {
        CombatCategory[] all = values();
        return all[(ordinal() - 1 + all.length) % all.length];
    }

    public static CombatCategory of(DuelType type) {
        if (type == null) return MELEE;
        for (CombatCategory c : values()) {
            if (c.subtypes.contains(type)) return c;
        }
        return MELEE;
    }

    /** Parses an English id/enum name or a Russian alias; null when unknown. */
    public static CombatCategory fromString(String name) {
        if (name == null || name.isBlank()) return null;
        String n = name.trim().toLowerCase(java.util.Locale.ROOT);
        for (CombatCategory c : values()) {
            if (c.id.equals(n) || c.name().equalsIgnoreCase(n)) return c;
        }
        return switch (n) {
            case "ближний", "ближний_бой", "ближнийбой" -> MELEE;
            case "дальний", "дальний_бой", "дальнийбой" -> RANGED;
            case "всадники", "конный", "конная", "верхом" -> MOUNTED;
            default -> null;
        };
    }
}
