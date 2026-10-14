package dev.lovelace.loveduels.util;

import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.notify.LoveNotify;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Set;

/**
 * Thin wrapper over LoveCore's {@link LoveNotify} for event-style player notices.
 *
 * <p>Rules (shared by every Love* plugin):</p>
 * <ul>
 *   <li>Always call {@code notify}, never gate a direct {@code sendActionBar}/{@code showTitle}
 *       on {@code isChannelEnabled}: LoveCore only enables CHAT by default, so a gate silently
 *       drops the message for everybody who never touched {@code /lovenotify}.</li>
 *   <li>The service is looked up on every call and never cached (LoveCore can be reloaded).</li>
 *   <li>If LoveCore (or the notify API classes) is missing, the message is sent directly to the
 *       channel it was meant for, exactly as before the integration.</li>
 * </ul>
 *
 * <p>Not for command replies, help, lists or clickable messages (those stay plain chat), and not
 * for game HUD such as countdowns or ticking action bars.</p>
 */
public final class Notify {

    /** What kind of notice this is; decides the requested channels and the importance flag. */
    public enum Kind {
        /** Short status line: action bar, falls back to chat when disabled or too long. */
        ACTION_BAR(Set.of(LoveNotify.Channel.ACTION_BAR), false),
        /**
         * Headline: title, with the action bar as the second choice. Without ACTION_BAR in the
         * request a player who only enabled the action bar would get the chat fallback instead.
         */
        TITLE(Set.of(LoveNotify.Channel.TITLE, LoveNotify.Channel.ACTION_BAR), false),
        /** Ordinary event notice: action bar, plus chat for players who enabled the chat duplicate. */
        EVENT(Set.of(LoveNotify.Channel.ACTION_BAR, LoveNotify.Channel.CHAT), false),
        /** Critical notice (money, bans, losses): always duplicated to chat. */
        IMPORTANT(Set.of(LoveNotify.Channel.ACTION_BAR, LoveNotify.Channel.CHAT), true);

        private final Set<LoveNotify.Channel> channels;
        private final boolean important;

        Kind(Set<LoveNotify.Channel> channels, boolean important) {
            this.channels = channels;
            this.important = important;
        }

        public Set<LoveNotify.Channel> channels() {
            return channels;
        }

        public boolean important() {
            return important;
        }
    }

    /** Service lookup, replaceable in tests. */
    @FunctionalInterface
    interface Lookup {
        Optional<LoveNotify> get();
    }

    /** Direct delivery used when LoveNotify is unavailable. */
    @FunctionalInterface
    interface Direct {
        void send(Player player, Component message, Kind kind);
    }

    private Notify() {}

    public static void actionBar(Player player, Component message) {
        send(player, message, Kind.ACTION_BAR);
    }

    public static void title(Player player, Component message) {
        send(player, message, Kind.TITLE);
    }

    public static void event(Player player, Component message) {
        send(player, message, Kind.EVENT);
    }

    public static void important(Player player, Component message) {
        send(player, message, Kind.IMPORTANT);
    }

    /** Message with clickable parts: always chat, ignores the player's channel toggles. */
    public static void interactive(Player player, Component message) {
        if (player == null || message == null) {
            return;
        }
        try {
            Optional<LoveNotify> service = LoveCore.service(LoveNotify.class);
            if (service.isPresent()) {
                service.get().notifyInteractive(player, message);
                return;
            }
        } catch (NoClassDefFoundError ignored) {
            // LoveCore API not on the classpath: plain chat below.
        }
        player.sendMessage(message);
    }

    static void send(Player player, Component message, Kind kind) {
        dispatch(player, message, kind, () -> LoveCore.service(LoveNotify.class), Notify::sendDirect);
    }

    /** Core routing, free of Bukkit statics so it can be unit-tested. */
    static void dispatch(Player player, Component message, Kind kind, Lookup lookup, Direct direct) {
        if (player == null || message == null) {
            return;
        }
        try {
            Optional<LoveNotify> service = lookup.get();
            if (service.isPresent()) {
                service.get().notify(player, message, kind.channels(), kind.important());
                return;
            }
        } catch (NoClassDefFoundError ignored) {
            // LoveCore API not on the classpath: fall through to the direct send.
        }
        direct.send(player, message, kind);
    }

    private static void sendDirect(Player player, Component message, Kind kind) {
        switch (kind) {
            case ACTION_BAR -> player.sendActionBar(message);
            case TITLE -> player.showTitle(Title.title(message, Component.empty()));
            case EVENT, IMPORTANT -> player.sendMessage(message);
        }
    }
}
