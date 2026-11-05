package dev.lovelace.loveduels.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MenuResourcesTest {

    private static YamlConfiguration resource(String name) {
        var stream = MenuResourcesTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @Test
    void everyMenuSectionHasANameAndADescriptionForSlotZero() {
        var lang = resource("lang.yml");
        for (String key : List.of("default", "main_menu", "challenge_mode", "player_select", "duel_setup",
                "kit_select", "stake_confirm", "confirm_bet", "readiness", "post_duel", "leaderboard", "royal",
                "spectate_list")) {
            assertNotNull(lang.getString("gui.section." + key + ".name"), key);
            assertTrue(lang.getStringList("gui.section." + key + ".lore").size() >= 3, key);
        }
    }

    @Test
    void newTexturesAreBundledAndEveryMigrationReplacesOnlyTheShippedValue() {
        var heads = resource("heads.yml");
        var manifest = resource("gui-style-followup.yml");
        for (var update : manifest.getMapList("updates")) {
            // Whole-section updates (maps) are covered by their own tests; texture swaps are single strings.
            if (!"heads.yml".equals(update.get("file")) || !(update.get("value") instanceof String)) continue;
            assertEquals(Boolean.TRUE, update.get("preserve-custom"), String.valueOf(update.get("path")));
            assertNotEquals(update.get("before"), update.get("value"));
            // The bundled file already carries the new value, so fresh installs and migrated ones agree.
            assertEquals(update.get("value"), heads.getString(String.valueOf(update.get("path"))),
                    String.valueOf(update.get("path")));
        }
    }

    @Test
    void slotZeroOfTheModeMenusUsesTheHeadOfTheChosenMode() {
        var heads = resource("heads.yml");
        var seen = new java.util.HashSet<String>();
        for (var mode : dev.lovelace.loveduels.core.ChallengeMode.values()) {
            String key = ChallengeModeGUI.textureOf(mode);
            assertTrue(seen.add(key), "one texture per mode: " + key);
            String path = key;
            assertNotNull(heads.getString(path), path);
        }
        assertEquals(3, seen.size());
    }

    @Test
    void theSetupKeysAreInTheConfig() {
        var config = resource("config.yml");
        assertTrue(config.getInt("settings.challenge_cooldown_seconds") > 0);
        assertTrue(config.getInt("settings.request_timeout_seconds") > 0);
        assertTrue(config.getInt("settings.base_honor_bet") > 0);
        assertTrue(config.getInt("royal_duel.ticket_cooldown_seconds") > 0);
    }
}
