package dev.lovelace.loveduels.util;

import dev.lovelace.lovecore.api.economy.Denomination;
import dev.lovelace.lovecore.api.economy.MoneyParser;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CoinFormatTest {

    @Test
    void coinNameFollowsTheIdNotTheValue() {
        // With the new scale gold is worth 2000: the old ">= 1000 means netherite" rule mislabelled it.
        assertTrue(CoinFormat.getCoinName(new Denomination("gold_coin", 2_000)).contains("Золотая"));
        assertTrue(CoinFormat.getCoinName(new Denomination("iron_coin", 100)).contains("Железная"));
        assertTrue(CoinFormat.getCoinName(new Denomination("diamond_coin", 20_000)).contains("Алмазная"));
        assertTrue(CoinFormat.getCoinName(new Denomination("currency:copper_coin", 1)).contains("Медная"));
        assertEquals("Монета", CoinFormat.getCoinName(new Denomination("mystery", 7)));
    }

    @Test
    void configMoneyKeysParse() throws Exception {
        YamlConfiguration cfg;
        try (Reader r = new InputStreamReader(getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            cfg = YamlConfiguration.loadConfiguration(r);
        }
        for (String path : new String[]{"royal_duel.min_stake", "spectators.base_price", "spectators.step_price"}) {
            Object raw = cfg.get(path);
            assertNotNull(raw, path);
            long v = raw instanceof Number n ? n.longValue() : MoneyParser.parse(String.valueOf(raw), MoneyParser.STANDARD);
            assertTrue(v > 0, path);
        }
        assertEquals(2_000L, MoneyParser.parse(cfg.getString("royal_duel.min_stake"), MoneyParser.STANDARD));
    }
}
