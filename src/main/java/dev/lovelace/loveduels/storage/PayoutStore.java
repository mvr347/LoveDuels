package dev.lovelace.loveduels.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistent bookkeeping for duel money that must never be lost:
 * <ul>
 *     <li>{@code pending_payouts} - coins owed to a player who was offline at payout time;</li>
 *     <li>{@code pending_escrow} - stakes charged for a duel that has not finished yet, refunded on
 *         the next start if the server died before the duel ended.</li>
 * </ul>
 * Writes go through a single ordered worker thread so the main thread never blocks on SQLite
 * and {@code holdEscrow -> releaseEscrow} can never be reordered (a reordered release would leave
 * an orphaned escrow row that refunds the stake a second time on the next start). During shutdown
 * ({@link #enterSynchronousMode()}) the queue is drained and every call runs inline instead, because
 * scheduled work is not guaranteed to run once the plugin is disabling.
 */
public final class PayoutStore {

    private final Database database;
    private final Logger logger;

    private final ExecutorService ordered = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LoveDuels-Payouts");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean synchronous = false;

    public PayoutStore(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    /** Runs the task on the ordered worker, or inline once in synchronous (shutdown) mode. */
    private void run(Runnable task) {
        if (synchronous) {
            task.run();
            return;
        }
        try {
            ordered.execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        }
    }

    /** Drains already queued writes, then makes every later call synchronous. Call first thing in onDisable. */
    public void enterSynchronousMode() {
        synchronous = true;
        ordered.shutdown();
        try {
            if (!ordered.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.severe("Payout queue did not drain within 10s; some writes may be lost");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Async variant of {@link #takePending}, ordered after previously queued writes. */
    public CompletableFuture<Long> takePendingAsync(UUID player) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        run(() -> future.complete(takePending(player)));
        return future;
    }

    public void queue(UUID player, long amount) {
        if (player == null || amount <= 0) return;
        run(() -> {
            try {
                database.querySync(conn -> {
                    insertPayout(conn, player, amount);
                    return null;
                });
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to queue payout of " + amount + " for " + player, e);
            }
        });
    }

    /** Removes and returns everything owed to the player (0 if nothing). */
    public long takePending(UUID player) {
        try {
            return database.querySync(conn -> {
                boolean auto = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try {
                    long sum = 0;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT COALESCE(SUM(amount), 0) FROM pending_payouts WHERE uuid = ?")) {
                        ps.setString(1, player.toString());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) sum = rs.getLong(1);
                        }
                    }
                    if (sum > 0) {
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM pending_payouts WHERE uuid = ?")) {
                            ps.setString(1, player.toString());
                            ps.executeUpdate();
                        }
                    }
                    conn.commit();
                    return sum;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(auto);
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to read pending payouts for " + player, e);
            return 0L;
        }
    }

    public void holdEscrow(UUID challenger, UUID target, long amountEach) {
        if (amountEach <= 0) return;
        run(() -> {
            try {
                database.querySync(conn -> {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT OR REPLACE INTO pending_escrow (challenger, target, amount_each, created_at) VALUES (?, ?, ?, ?)")) {
                        ps.setString(1, challenger.toString());
                        ps.setString(2, target.toString());
                        ps.setLong(3, amountEach);
                        ps.setLong(4, System.currentTimeMillis());
                        ps.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to record escrow for " + challenger, e);
            }
        });
    }

    public void releaseEscrow(UUID challenger) {
        run(() -> {
            try {
                database.querySync(conn -> {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM pending_escrow WHERE challenger = ?")) {
                        ps.setString(1, challenger.toString());
                        ps.executeUpdate();
                    }
                    return null;
                });
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to release escrow for " + challenger, e);
            }
        });
    }

    /**
     * Turns every escrow row left over from a previous run into pending payouts for both fighters.
     * On a fresh start no duel can be running, so every remaining row is by definition orphaned.
     *
     * @return number of orphaned duels refunded
     */
    public int recoverEscrow() {
        try {
            return database.querySync(conn -> {
                boolean auto = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try {
                    int count = 0;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT challenger, target, amount_each FROM pending_escrow");
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long each = rs.getLong("amount_each");
                            insertPayout(conn, UUID.fromString(rs.getString("challenger")), each);
                            insertPayout(conn, UUID.fromString(rs.getString("target")), each);
                            count++;
                        }
                    }
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM pending_escrow")) {
                        ps.executeUpdate();
                    }
                    conn.commit();
                    return count;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(auto);
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to recover orphaned duel escrow", e);
            return 0;
        }
    }

    private static void insertPayout(Connection conn, UUID player, long amount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO pending_payouts (uuid, amount, created_at) VALUES (?, ?, ?)")) {
            ps.setString(1, player.toString());
            ps.setLong(2, amount);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }
}
