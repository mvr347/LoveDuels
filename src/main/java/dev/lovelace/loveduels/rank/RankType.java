package dev.lovelace.loveduels.rank;

import dev.lovelace.loveduels.core.DuelType;

/**
 * Отдельные ранговые треки по видам дуэлей.
 */
public enum RankType {
    SWORD("sword", "Мечи", DuelType.SWORD),
    BOW("bow", "Луки", DuelType.BOW),
    HORSE("horse", "Кони", DuelType.HORSE_SPEAR),
    FISTS("fists", "Кулаки", DuelType.FISTS);

    private final String id;
    private final String displayName;
    private final DuelType linkedType;

    RankType(String id, String displayName, DuelType linkedType) {
        this.id = id;
        this.displayName = displayName;
        this.linkedType = linkedType;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public DuelType getLinkedType() {
        return linkedType;
    }

    public static RankType fromDuelType(DuelType type) {
        if (type == null) return null;
        for (RankType rt : values()) {
            if (rt.linkedType == type) return rt;
        }
        return null;
    }
}
