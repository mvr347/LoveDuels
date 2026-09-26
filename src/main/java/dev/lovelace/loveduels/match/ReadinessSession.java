package dev.lovelace.loveduels.match;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Coordination session for own-inventory duel readiness.
 * Both players must explicitly click "Ready" before the match can begin.
 */
public final class ReadinessSession {

    private final UUID player1;
    private final UUID player2;
    private final AtomicBoolean ready1 = new AtomicBoolean(false);
    private final AtomicBoolean ready2 = new AtomicBoolean(false);
    private final AtomicBoolean terminated = new AtomicBoolean(false);
    private final Runnable onBothReady;
    private final Consumer<UUID> onCancel;
    private Consumer<ReadinessSession> stateChangeListener;

    public ReadinessSession(UUID player1, UUID player2, Runnable onBothReady, Consumer<UUID> onCancel) {
        this.player1 = Objects.requireNonNull(player1, "player1");
        this.player2 = Objects.requireNonNull(player2, "player2");
        this.onBothReady = Objects.requireNonNull(onBothReady, "onBothReady");
        this.onCancel = Objects.requireNonNull(onCancel, "onCancel");
    }

    public void setStateChangeListener(Consumer<ReadinessSession> listener) {
        this.stateChangeListener = listener;
    }

    public boolean isTerminated() {
        return terminated.get();
    }

    public void setReady(UUID uuid, boolean ready) {
        if (terminated.get()) {
            return;
        }

        if (uuid.equals(player1)) {
            ready1.set(ready);
        } else if (uuid.equals(player2)) {
            ready2.set(ready);
        }

        if (stateChangeListener != null) {
            stateChangeListener.accept(this);
        }

        if (ready1.get() && ready2.get()) {
            if (terminated.compareAndSet(false, true)) {
                onBothReady.run();
            }
        }
    }

    public void toggleReady(UUID uuid) {
        if (terminated.get()) {
            return;
        }
        if (uuid.equals(player1)) {
            setReady(uuid, !ready1.get());
        } else if (uuid.equals(player2)) {
            setReady(uuid, !ready2.get());
        }
    }

    public boolean isReady(UUID uuid) {
        if (uuid.equals(player1)) return ready1.get();
        if (uuid.equals(player2)) return ready2.get();
        return false;
    }

    public boolean isPlayer1Ready() {
        return ready1.get();
    }

    public boolean isPlayer2Ready() {
        return ready2.get();
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public void cancel(UUID who) {
        if (!terminated.compareAndSet(false, true)) {
            return;
        }
        onCancel.accept(who);
    }
}
