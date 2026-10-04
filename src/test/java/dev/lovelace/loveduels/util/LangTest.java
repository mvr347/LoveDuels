package dev.lovelace.loveduels.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LangTest {

    @Test
    void testBundledLangYmlLoadsProperly() throws Exception {
        YamlConfiguration cfg;
        try (InputStream in = getClass().getResourceAsStream("/lang.yml")) {
            assertNotNull(in, "lang.yml should be present in resources");
            cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        assertNotNull(cfg.getString("prefix"), "prefix should exist");
        assertNotNull(cfg.getString("gui.main_menu.title"), "main menu title should exist");
        assertNotNull(cfg.getString("gui.challenge_mode.title"), "challenge mode title should exist");
        assertNotNull(cfg.getString("gui.player_select.title_normal"), "player select title should exist");
        assertNotNull(cfg.getString("gui.duel_setup.title_normal"), "duel setup title should exist");
        assertNotNull(cfg.getString("gui.confirm_bet.title"), "confirm bet title should exist");
        assertNotNull(cfg.getString("gui.kit_select.title"), "kit select title should exist");
        assertNotNull(cfg.getString("gui.leaderboard.title"), "leaderboard title should exist");
        assertNotNull(cfg.getString("gui.post_duel.title"), "post duel title should exist");
        assertNotNull(cfg.getString("gui.readiness.title"), "readiness title should exist");
        assertNotNull(cfg.getString("gui.royal_duel.title"), "royal duel title should exist");
        assertNotNull(cfg.getString("gui.spectate_list.title"), "spectate list title should exist");
        assertNotNull(cfg.getString("gui.stake_confirm.title_normal"), "stake confirm title should exist");

        List<String> lore = cfg.getStringList("gui.main_menu.challenge.lore");
        assertFalse(lore.isEmpty(), "lore list should not be empty");
    }

    @Test
    void testFallbackWhenPluginNotInitialized() {
        // Without plugin instance, get() returns fallback [path] without crashing
        String missing = Lang.get("some.nonexistent.key");
        assertEquals("<red>[some.nonexistent.key]</red>", missing);

        String def = Lang.getOrDefault("some.nonexistent.key", "Default value");
        assertEquals("Default value", def);
    }
}
