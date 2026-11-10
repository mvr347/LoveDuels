package dev.lovelace.loveduels.integration;

import dev.lovelace.lovecore.api.achievements.Achievements;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AchievementsBridgeTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    private static Achievements recorder(List<String> calls) {
        return (Achievements) Proxy.newProxyInstance(Achievements.class.getClassLoader(),
                new Class<?>[] {Achievements.class}, (proxy, method, args) -> {
                    calls.add(args[0] + " " + args[1] + " " + args[2]);
                    return null;
                });
    }

    @Test
    void bothPlayAndTheLoserLoses() {
        List<String> calls = new ArrayList<>();
        AchievementsBridge.Gateway.record(recorder(calls), a, b, b);
        assertEquals(List.of(a + " duels.played 1.0", b + " duels.played 1.0", b + " duels.lost 1.0"), calls);
    }

    @Test
    void aDrawCountsAsPlayedOnly() {
        List<String> calls = new ArrayList<>();
        AchievementsBridge.Gateway.record(recorder(calls), a, b, null);
        assertEquals(List.of(a + " duels.played 1.0", b + " duels.played 1.0"), calls);
    }
}
