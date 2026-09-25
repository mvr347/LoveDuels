package dev.lovelace.loveduels.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.lovelace.loveduels.core.DuelType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SQLite backed player storage with Caffeine memory caching.
 */
public final class SqlitePlayerStorage implements PlayerStorage {

    private final Database database;
    private final Cache<UUID, PlayerData> cache;

    public SqlitePlayerStorage(Database database) {
        this.database = database;
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(30))
                .maximumSize(5000)
                .build();
    }

    @Override
    public CompletableFuture<PlayerData> getOrCreatePlayer(UUID uuid, String name) {
        PlayerData cached = cache.getIfPresent(uuid);
        if (cached != null) {
            if (!cached.name().equals(name)) {
                PlayerData updated = cached.withName(name);
                cache.put(uuid, updated);
                savePlayer(updated);
                return CompletableFuture.completedFuture(updated);
            }
            return CompletableFuture.completedFuture(cached);
        }

        return database.queryAsync(conn -> {
            String selectSql = "SELECT * FROM players WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        PlayerData data = mapRow(rs);
                        if (!data.name().equals(name)) {
                            data = data.withName(name);
                            updatePlayerInDb(conn, data);
                        }
                        cache.put(uuid, data);
                        return data;
                    }
                }
            }

            // Insert new default player
            PlayerData initial = PlayerData.initial(uuid, name);
            String insertSql = """
                INSERT INTO players (uuid, name, honor, wins, losses, current_streak, best_streak, money_won, money_lost, royal_wins)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, initial.uuid().toString());
                ps.setString(2, initial.name());
                ps.setInt(3, initial.honor());
                ps.setInt(4, initial.wins());
                ps.setInt(5, initial.losses());
                ps.setInt(6, initial.currentStreak());
                ps.setInt(7, initial.bestStreak());
                ps.setLong(8, initial.moneyWon());
                ps.setLong(9, initial.moneyLost());
                ps.setInt(10, initial.royalWins());
                ps.executeUpdate();
            }

            cache.put(uuid, initial);
            return initial;
        });
    }

    @Override
    public CompletableFuture<Optional<PlayerData>> getPlayer(UUID uuid) {
        PlayerData cached = cache.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return database.queryAsync(conn -> {
            String sql = "SELECT * FROM players WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        PlayerData data = mapRow(rs);
                        cache.put(uuid, data);
                        return Optional.of(data);
                    }
                }
            }
            return Optional.empty();
        });
    }

    @Override
    public Optional<PlayerData> getCachedPlayer(UUID uuid) {
        return Optional.ofNullable(cache.getIfPresent(uuid));
    }

    @Override
    public CompletableFuture<Void> savePlayer(PlayerData player) {
        cache.put(player.uuid(), player);
        return database.executeAsync(conn -> updatePlayerInDb(conn, player));
    }

    private void updatePlayerInDb(java.sql.Connection conn, PlayerData player) {
        String sql = """
            INSERT INTO players (uuid, name, honor, wins, losses, current_streak, best_streak, money_won, money_lost, royal_wins)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                honor = excluded.honor,
                wins = excluded.wins,
                losses = excluded.losses,
                current_streak = excluded.current_streak,
                best_streak = excluded.best_streak,
                money_won = excluded.money_won,
                money_lost = excluded.money_lost,
                royal_wins = excluded.royal_wins;
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.uuid().toString());
            ps.setString(2, player.name());
            ps.setInt(3, player.honor());
            ps.setInt(4, player.wins());
            ps.setInt(5, player.losses());
            ps.setInt(6, player.currentStreak());
            ps.setInt(7, player.bestStreak());
            ps.setLong(8, player.moneyWon());
            ps.setLong(9, player.moneyLost());
            ps.setInt(10, player.royalWins());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save player " + player.uuid(), e);
        }
    }

    @Override
    public CompletableFuture<List<PlayerData>> getTopPlayersByHonor(int limit) {
        return database.queryAsync(conn -> {
            List<PlayerData> list = new ArrayList<>();
            String sql = "SELECT * FROM players ORDER BY honor DESC, wins DESC LIMIT ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return list;
        });
    }

    @Override
    public CompletableFuture<Void> logHistory(DuelHistoryEntry entry) {
        return database.executeAsync(conn -> {
            String sql = """
                INSERT INTO history (player1, player2, player1_name, player2_name, winner, duel_type, money_bet, honor_bet, royal, timestamp, duration_seconds)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, entry.player1().toString());
                ps.setString(2, entry.player2().toString());
                ps.setString(3, entry.player1Name());
                ps.setString(4, entry.player2Name());
                ps.setString(5, entry.winner() != null ? entry.winner().toString() : null);
                ps.setString(6, entry.type().name());
                ps.setLong(7, entry.moneyBet());
                ps.setInt(8, entry.honorBet());
                ps.setBoolean(9, entry.royal());
                ps.setLong(10, entry.timestamp());
                ps.setInt(11, entry.durationSeconds());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to log match history", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<DuelHistoryEntry>> getRecentHistory(UUID player, int limit) {
        return database.queryAsync(conn -> {
            List<DuelHistoryEntry> list = new ArrayList<>();
            String sql = """
                SELECT * FROM history
                WHERE player1 = ? OR player2 = ?
                ORDER BY timestamp DESC
                LIMIT ?
            """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.toString());
                ps.setString(2, player.toString());
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        UUID p1 = UUID.fromString(rs.getString("player1"));
                        UUID p2 = UUID.fromString(rs.getString("player2"));
                        String p1Name = rs.getString("player1_name");
                        String p2Name = rs.getString("player2_name");
                        String winnerStr = rs.getString("winner");
                        UUID winner = winnerStr != null ? UUID.fromString(winnerStr) : null;
                        DuelType type = DuelType.valueOf(rs.getString("duel_type"));
                        long moneyBet = rs.getLong("money_bet");
                        int honorBet = rs.getInt("honor_bet");
                        boolean royal = rs.getBoolean("royal");
                        long timestamp = rs.getLong("timestamp");
                        int duration = rs.getInt("duration_seconds");

                        list.add(new DuelHistoryEntry(id, p1, p2, p1Name, p2Name, winner, type, moneyBet, honorBet, royal, timestamp, duration));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return list;
        });
    }

    @Override
    public void evict(UUID uuid) {
        cache.invalidate(uuid);
    }

    private PlayerData mapRow(ResultSet rs) throws SQLException {
        return new PlayerData(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("name"),
                rs.getInt("honor"),
                rs.getInt("wins"),
                rs.getInt("losses"),
                rs.getInt("current_streak"),
                rs.getInt("best_streak"),
                rs.getLong("money_won"),
                rs.getLong("money_lost"),
                rs.getInt("royal_wins")
        );
    }
}
