package dev.lovelace.loveduels.match;

import dev.lovelace.loveduels.core.DuelBet;
import dev.lovelace.loveduels.core.DuelRequest;
import dev.lovelace.loveduels.util.CoinFormat;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Оба игрока подтверждают денежную ставку перед стартом (не для training).
 */
public final class StakeConfirmSession {

    private final DuelRequest request;
    private final UUID player1;
    private final UUID player2;
    private final AtomicLong moneyBet;
    private final int honorBet;
    private final AtomicBoolean ready1 = new AtomicBoolean(false);
    private final AtomicBoolean ready2 = new AtomicBoolean(false);
    private final AtomicBoolean terminated = new AtomicBoolean(false);

    private final BiConsumer<DuelRequest, DuelBet> onBothReady;
    private final Consumer<UUID> onCancel;
    private Consumer<StakeConfirmSession> stateChangeListener;

    public StakeConfirmSession(
            DuelRequest request,
            BiConsumer<DuelRequest, DuelBet> onBothReady,
            Consumer<UUID> onCancel
    ) {
        this.request = Objects.requireNonNull(request);
        this.player1 = request.senderId();
        this.player2 = request.targetId();
        long initial = Math.max(0L, request.bet().moneyBet());
        if (request.royal()) {
            long min = CoinFormat.goldUnit();
            if (initial < min) initial = min;
        }
        this.moneyBet = new AtomicLong(initial);
        this.honorBet = Math.max(0, request.bet().honorBet());
        this.onBothReady = Objects.requireNonNull(onBothReady);
        this.onCancel = Objects.requireNonNull(onCancel);
    }

    public DuelRequest getRequest() {
        return request;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public long getMoneyBet() {
        return moneyBet.get();
    }

    public int getHonorBet() {
        return honorBet;
    }

    public boolean isTraining() {
        return request.isTraining();
    }

    public void setMoneyBet(long amount) {
        if (terminated.get()) return;
        long v = Math.max(0L, amount);
        if (request.royal()) {
            long min = CoinFormat.goldUnit();
            if (v < min) v = min;
        }
        moneyBet.set(v);
        ready1.set(false);
        ready2.set(false);
        fireState();
    }

    public void setStateChangeListener(Consumer<StakeConfirmSession> listener) {
        this.stateChangeListener = listener;
    }

    public boolean isTerminated() {
        return terminated.get();
    }

    public boolean isReady(UUID uuid) {
        if (uuid.equals(player1)) return ready1.get();
        if (uuid.equals(player2)) return ready2.get();
        return false;
    }

    public void setReady(UUID uuid, boolean ready) {
        if (terminated.get()) return;
        if (uuid.equals(player1)) ready1.set(ready);
        else if (uuid.equals(player2)) ready2.set(ready);
        else return;
        fireState();
        if (ready1.get() && ready2.get()) {
            if (terminated.compareAndSet(false, true)) {
                onBothReady.accept(request, new DuelBet(moneyBet.get(), honorBet));
            }
        }
    }

    public void toggleReady(UUID uuid) {
        if (terminated.get()) return;
        setReady(uuid, !isReady(uuid));
    }

    public void cancel(UUID who) {
        if (!terminated.compareAndSet(false, true)) return;
        onCancel.accept(who);
    }

    private void fireState() {
        if (stateChangeListener != null) {
            stateChangeListener.accept(this);
        }
    }
}
