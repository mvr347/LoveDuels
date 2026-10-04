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
            "<gold>Свои вещи</gold>",
            Material.IRON_SWORD,
            "Сражение в собственной экипировке."
    ),
    KIT(
            "kit",
            "<aqua>Рыцарское снаряжение</aqua>",
            Material.IRON_CHESTPLATE,
            "Равный набор брони и оружия."
    ),
    SWORD(
            "sword",
            "<yellow>Мечи</yellow>",
            Material.DIAMOND_SWORD,
            "Классическая дуэль на мечах."
    ),
    BOW(
            "bow",
            "<green>Луки</green>",
            Material.BOW,
            "Стрелковая дуэль на луках."
    ),
    CROSSBOW(
            "crossbow",
            "<dark_aqua>Арбалеты</dark_aqua>",
            Material.CROSSBOW,
            "Дуэль на арбалетах."
    ),
    HORSE_SPEAR(
            "horse_spear",
            "<gold>Копьё</gold>",
            Material.TRIDENT,
            "Турнир верхом с зарядом копья."
    ),
    FISTS(
            "fists",
            "<red>Кулаки</red>",
            Material.LEATHER,
            "Голые руки, без оружия и брони."
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
        return MiniMessage.miniMessage().deserialize(getDisplayNameMiniMessage());
    }

    public String getDisplayNameMiniMessage() {
        return dev.lovelace.loveduels.util.Lang.getOrDefault("types." + id + ".name", displayNameMiniMessage);
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return dev.lovelace.loveduels.util.Lang.getOrDefault("types." + id + ".description", description);
    }

    public boolean requiresReadinessSession() {
        // Готовность/ставки теперь через StakeConfirmSession для всех режимов
        return false;
    }

    public boolean isMiniGame() {
        return this == SWORD || this == BOW || this == CROSSBOW || this == HORSE_SPEAR || this == FISTS;
    }

    public boolean hasSeparateRank() {
        return this == SWORD || this == BOW || this == CROSSBOW || this == HORSE_SPEAR || this == FISTS;
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
