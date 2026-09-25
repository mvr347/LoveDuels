package dev.lovelace.loveduels.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;

/**
 * Supported duel types in LoveDuels.
 */
public enum DuelType {
    OWN_INVENTORY(
            "own_inventory",
            "<gold>⚔ Свои вещи",
            Material.IRON_SWORD,
            "Сражение в собственной экипировке с экраном готовности."
    ),
    KIT(
            "kit",
            "<aqua>🛡 Киты",
            Material.CHEST,
            "Битва с равными заготовленными наборами брони и оружия."
    ),
    SWORD(
            "sword",
            "<yellow>🗡 Мечи",
            Material.DIAMOND_SWORD,
            "Классическая дуэль на мечах без сторонних предметов."
    ),
    BOW(
            "bow",
            "<green>🏹 Луки",
            Material.BOW,
            "Стрелковая дуэль на луках со стрелами."
    ),
    HORSE_SPEAR(
            "horse_spear",
            "<gold>🐎 Копьё и Конь",
            Material.TRIDENT,
            "Турнирный рыцарский поединок верхом с зарядом копья и очками."
    );

    private final String id;
    private final String displayNameMiniMessage;
    private final Material icon;
    private final String description;

    DuelType(String id, String displayNameMiniMessage, Material icon, String description) {
        this.id = id;
        this.displayNameMiniMessage = displayNameMiniMessage;
        this.icon = icon;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return MiniMessage.miniMessage().deserialize(displayNameMiniMessage);
    }

    public String getDisplayNameMiniMessage() {
        return displayNameMiniMessage;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresReadinessSession() {
        return this == OWN_INVENTORY;
    }

    public boolean isMiniGame() {
        return this == SWORD || this == BOW || this == HORSE_SPEAR;
    }

    public static DuelType fromString(String name) {
        if (name == null || name.isBlank()) return null;
        for (DuelType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.id.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
