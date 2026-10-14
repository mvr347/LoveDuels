package dev.lovelace.loveduels.util;

import dev.lovelace.lovecore.api.notify.LoveNotify;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotifyTest {

    private static final Component MSG = Component.text("hello");

    /** Records notify() calls; the Player argument is never touched by the recorder. */
    private static final class Recorder implements LoveNotify {
        final List<String> calls = new ArrayList<>();

        @Override public Set<Channel> getEnabledChannels(UUID uuid) { return Set.of(); }
        @Override public boolean isChannelEnabled(UUID uuid, Channel channel) { return false; }
        @Override public boolean toggleChannel(UUID uuid, Channel channel) { return false; }
        @Override public boolean setChannelEnabled(UUID uuid, Channel channel, boolean enabled) { return false; }
        @Override public void notify(org.bukkit.entity.Player p, Component m, Set<Channel> ch, boolean important) {
            calls.add("notify:" + new java.util.TreeSet<>(ch) + ":" + important);
        }
        @Override public void notifyInteractive(org.bukkit.entity.Player p, Component m) { calls.add("interactive"); }
    }

    private static org.bukkit.entity.Player fakePlayer() {
        return (org.bukkit.entity.Player) java.lang.reflect.Proxy.newProxyInstance(
                NotifyTest.class.getClassLoader(), new Class<?>[]{org.bukkit.entity.Player.class},
                (proxy, method, args) -> { throw new UnsupportedOperationException(method.getName()); });
    }

    @Test
    void routesThroughServiceWithKindChannels() {
        Recorder rec = new Recorder();
        List<Notify.Kind> direct = new ArrayList<>();
        for (Notify.Kind kind : Notify.Kind.values()) {
            Notify.dispatch(fakePlayer(), MSG, kind, () -> Optional.of(rec), (p, m, k) -> direct.add(k));
        }
        assertTrue(direct.isEmpty(), "service present: nothing may be sent directly");
        assertEquals(List.of(
                "notify:[ACTION_BAR]:false",
                "notify:[ACTION_BAR, TITLE]:false",
                "notify:[CHAT, ACTION_BAR]:false",
                "notify:[CHAT, ACTION_BAR]:true"), rec.calls);
    }

    @Test
    void importantIsTheOnlyKindThatForcesChat() {
        for (Notify.Kind kind : Notify.Kind.values()) {
            assertEquals(kind == Notify.Kind.IMPORTANT, kind.important(), kind.name());
        }
    }

    @Test
    void titleAlsoAsksForActionBarSoItDoesNotFallBackToChat() {
        assertTrue(Notify.Kind.TITLE.channels().contains(LoveNotify.Channel.ACTION_BAR));
        assertTrue(Notify.Kind.TITLE.channels().contains(LoveNotify.Channel.TITLE));
        assertFalse(Notify.Kind.TITLE.channels().contains(LoveNotify.Channel.CHAT));
    }

    @Test
    void fallsBackToDirectSendWhenServiceMissing() {
        List<Notify.Kind> direct = new ArrayList<>();
        Notify.dispatch(fakePlayer(), MSG, Notify.Kind.EVENT, Optional::empty, (p, m, k) -> direct.add(k));
        assertEquals(List.of(Notify.Kind.EVENT), direct);
    }

    @Test
    void fallsBackToDirectSendOnNoClassDefFoundError() {
        List<Notify.Kind> direct = new ArrayList<>();
        Notify.dispatch(fakePlayer(), MSG, Notify.Kind.IMPORTANT,
                () -> { throw new NoClassDefFoundError("dev/lovelace/lovecore/api/notify/LoveNotify"); },
                (p, m, k) -> direct.add(k));
        assertEquals(List.of(Notify.Kind.IMPORTANT), direct);
    }

    @Test
    void nullPlayerOrMessageIsIgnored() {
        List<Notify.Kind> direct = new ArrayList<>();
        Notify.dispatch(null, MSG, Notify.Kind.EVENT, Optional::empty, (p, m, k) -> direct.add(k));
        Notify.dispatch(fakePlayer(), null, Notify.Kind.EVENT, Optional::empty, (p, m, k) -> direct.add(k));
        assertTrue(direct.isEmpty());
    }
}
