package dev.lovelace.loveduels.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistent bookkeeping for duel money that must never be lost:
 * <ul>
 *     <li>{@code pending_payouts} - coins owed to a player who was offline at payout time;</li>
 *     <li>{@code pending_escrow} - stakes charged for a duel that has not finished yet, refunded on
 *         the next start if the server died before the duel ended.</li>
 * </ul>
 * All calls are synchronous on purpose: the row must exist before the caller moves on.
 */
public final class PayoutStore {

    private final Database database;
    private final Logger logger;

    public PayoutStore(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public void queue(UUID player, long amount) {
        if (player == null || amount <= 0) return;
        try {
            database.querySync(conn -> {
                insertPayout(conn, player, amount);
                return null;
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to queue payout of " + amount + " for " + player, e);
        }
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
    }

    public void releaseEscrow(UUID challenger) {
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
