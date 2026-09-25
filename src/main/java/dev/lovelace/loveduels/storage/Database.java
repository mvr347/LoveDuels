package dev.lovelace.loveduels.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-performance asynchronous database layer using HikariCP and SQLite.
 */
public final class Database {

    private final HikariDataSource dataSource;
    private final ExecutorService asyncExecutor;
    private final Logger logger;
    private final Object writeLock = new Object();

    public Database(File databaseFile, Logger logger) {
        this.logger = logger;
        if (!databaseFile.getParentFile().exists()) {
            databaseFile.getParentFile().mkdirs();
        }

        // Java 25 Virtual Threads for ultra-lightweight async DB tasks
        this.asyncExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("LoveDuels-DB-", 1).factory()
        );

        HikariConfig config = new HikariConfig();
        config.setPoolName("LoveDuels-Pool");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        config.setMaximumPoolSize(6);
        config.setMinimumIdle(1);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(5));
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(15));
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));

        // SQLite pragmas for optimal speed & safety
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("foreign_keys", "ON");
        config.addDataSourceProperty("busy_timeout", "10000");

        this.dataSource = new HikariDataSource(config);

        initSchema();
    }

    private void initSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Players table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(32) NOT NULL,
                    honor INTEGER NOT NULL DEFAULT 1000,
                    wins INTEGER NOT NULL DEFAULT 0,
                    losses INTEGER NOT NULL DEFAULT 0,
                    current_streak INTEGER NOT NULL DEFAULT 0,
                    best_streak INTEGER NOT NULL DEFAULT 0,
                    money_won BIGINT NOT NULL DEFAULT 0,
                    money_lost BIGINT NOT NULL DEFAULT 0,
                    royal_wins INTEGER NOT NULL DEFAULT 0
                );
            """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_players_honor ON players(honor DESC);");

            // Arenas table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS arenas (
                    id VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(64) NOT NULL,
                    data_json TEXT NOT NULL,
                    enabled BOOLEAN NOT NULL DEFAULT 1
                );
            """);

            // Kits table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kits (
                    id VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(64) NOT NULL,
                    data_json TEXT NOT NULL
                );
            """);

            // History table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player1 VARCHAR(36) NOT NULL,
                    player2 VARCHAR(36) NOT NULL,
                    player1_name VARCHAR(32) NOT NULL,
                    player2_name VARCHAR(32) NOT NULL,
                    winner VARCHAR(36),
                    duel_type VARCHAR(32) NOT NULL,
                    money_bet BIGINT NOT NULL DEFAULT 0,
                    honor_bet INTEGER NOT NULL DEFAULT 0,
                    royal BOOLEAN NOT NULL DEFAULT 0,
                    timestamp BIGINT NOT NULL,
                    duration_seconds INTEGER NOT NULL DEFAULT 0
                );
            """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_history_player1 ON history(player1);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_history_player2 ON history(player2);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_history_timestamp ON history(timestamp DESC);");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize LoveDuels database schema", e);
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T t) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer<T> {
        void accept(T t) throws SQLException;
    }

    public <T> CompletableFuture<T> queryAsync(SqlFunction<Connection, T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                return action.apply(conn);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database query error", e);
                throw new CompletionException(e);
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Void> executeAsync(SqlConsumer<Connection> action) {
        return CompletableFuture.runAsync(() -> {
            synchronized (writeLock) {
                try (Connection conn = dataSource.getConnection()) {
                    action.accept(conn);
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Database execute error", e);
                    throw new CompletionException(e);
                }
            }
        }, asyncExecutor);
    }

    public void close() {
        try {
            asyncExecutor.shutdown();
            if (!asyncExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
