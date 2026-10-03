package dev.lovelace.loveduels.arena;

import dev.lovelace.loveduels.core.CombatCategory;
import dev.lovelace.loveduels.core.DuelType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArenaTypeTest {

    @Test
    void newArenaHasNoTypeAndReportsIt() {
        Arena a = new Arena("a1", "A");
        assertFalse(a.hasType());
        assertFalse(a.supportsType(DuelType.FISTS));
        assertTrue(a.getValidationErrors().stream().anyMatch(e -> e.contains("Тип арены")));
    }

    @Test
    void mountedArenaOnlyTakesHorseSpear() {
        Arena a = new Arena("horse", "H");
        a.setCategory(CombatCategory.MOUNTED);
        assertTrue(a.supportsType(DuelType.HORSE_SPEAR));
        assertFalse(a.supportsType(DuelType.FISTS));
        assertFalse(a.supportsType(DuelType.BOW));
    }

    @Test
    void meleeArenaTakesMeleeTypesOnly() {
        Arena a = new Arena("melee", "M");
        a.setCategory(CombatCategory.MELEE);
        for (DuelType t : CombatCategory.MELEE.getSubtypes()) assertTrue(a.supportsType(t));
        assertFalse(a.supportsType(DuelType.HORSE_SPEAR));
    }

    @Test
    void categoryArenaCannotToggleForeignTypes() {
        Arena a = new Arena("r", "R");
        a.setCategory(CombatCategory.RANGED);
        assertFalse(a.canToggleType(DuelType.HORSE_SPEAR));
        a.toggleSupportedType(DuelType.HORSE_SPEAR);
        assertFalse(a.supportsType(DuelType.HORSE_SPEAR));
        a.toggleSupportedType(DuelType.BOW);
        assertFalse(a.supportsType(DuelType.BOW));
        assertTrue(a.supportsType(DuelType.CROSSBOW));
    }

    @Test
    void lastTypeCannotBeToggledOff() {
        Arena a = new Arena("m", "M");
        a.setCategory(CombatCategory.MOUNTED);
        a.toggleSupportedType(DuelType.HORSE_SPEAR);
        assertTrue(a.supportsType(DuelType.HORSE_SPEAR));
    }

    @Test
    void allTypesArenaSupportsEverythingAndSwitchesBack() {
        Arena a = new Arena("all", "All");
        a.setAllTypes();
        assertNull(a.getCategory());
        for (DuelType t : DuelType.values()) assertTrue(a.supportsType(t));
        a.setCategory(CombatCategory.MOUNTED);
        assertEquals(1, a.getSupportedTypes().size());
    }

    @Test
    void categoryParsingAcceptsEnglishAndRussian() {
        assertEquals(CombatCategory.MELEE, CombatCategory.fromString("melee"));
        assertEquals(CombatCategory.MOUNTED, CombatCategory.fromString("MOUNTED"));
        assertEquals(CombatCategory.MOUNTED, CombatCategory.fromString("всадники"));
        assertEquals(CombatCategory.RANGED, CombatCategory.fromString("дальний"));
        assertNull(CombatCategory.fromString("all"));
        assertNull(CombatCategory.fromString(""));
        assertNull(CombatCategory.fromString(null));
    }
}
