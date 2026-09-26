package dev.lovelace.loveduels.match;

import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.arena.ArenaState;
import dev.lovelace.loveduels.core.DuelBet;
import dev.lovelace.loveduels.core.DuelType;
import dev.lovelace.loveduels.core.InventorySnapshot;
import dev.lovelace.loveduels.integration.LoveBehaviorBridge;
import dev.lovelace.loveduels.integration.LoveEconomyBridge;
import dev.lovelace.loveduels.integration.LoveLeaderboardsBridge;
import dev.lovelace.loveduels.royal.RoyalDuelManager;
import dev.lovelace.loveduels.storage.DuelHistoryEntry;
import dev.lovelace.loveduels.storage.PlayerStorage;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractMatch implements Match {

    protected final Plugin plugin;
    protected final UUID matchId = UUID.randomUUID();
    protected final Player player1;
    protected final Player player2;
    protected final UUID player1Id;
    protected final UUID player2Id;
    protected final Arena arena;
    protected final DuelType type;
    protected final DuelBet bet;
    protected final boolean royal;

    protected final PlayerStorage playerStorage;
    protected final LoveEconomyBridge economyBridge;
    protected final LoveBehaviorBridge behaviorBridge;
    protected final LoveLeaderboardsBridge leaderboardsBridge;
    protected final RoyalDuelManager royalManager;
    protected final MatchManager matchManager;

    protected final Map<UUID, InventorySnapshot> snapshots = new ConcurrentHashMap<>();
    protected final Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
    protected final Map<UUID, Integer> points = new ConcurrentHashMap<>();
    protected final Set<UUID> spectators = ConcurrentHashMap.newKeySet();

    protected volatile MatchState state = MatchState.PREPARATION;
    protected final AtomicBoolean ended = new AtomicBoolean(false);
    protected long startTime;
    protected MatchResult result;
    protected BukkitTask actionBarTask;
    protected BukkitTask timerTask;
    protected BossBar timerBossBar;

    public AbstractMatch(
            Plugin plugin,
            Player player1,
            Player player2,
            Arena arena,
            DuelType type,
            DuelBet bet,
            boolean royal,
            PlayerStorage playerStorage,
            LoveEconomyBridge economyBridge,
            LoveBehaviorBridge behaviorBridge,
            LoveLeaderboardsBridge leaderboardsBridge,
            RoyalDuelManager royalManager,
            MatchManager matchManager
    ) {
        this.plugin = plugin;
        this.player1 = player1;
        this.player2 = player2;
        this.player1Id = player1.getUniqueId();
        this.player2Id = player2.getUniqueId();
        this.arena = arena;
        this.type = type;
        this.bet = bet;
        this.royal = royal;
        this.playerStorage = playerStorage;
        this.economyBridge = economyBridge;
        this.behaviorBridge = behaviorBridge;
        this.leaderboardsBridge = leaderboardsBridge;
        this.royalManager = royalManager;
        this.matchManager = matchManager;
    }

    @Override
    public UUID getMatchId() {
        return matchId;
    }

    @Override
    public UUID getPlayer1Id() {
        return player1Id;
    }

    @Override
    public UUID getPlayer2Id() {
        return player2Id;
    }

    @Override
    public Player getPlayer1() {
        return player1;
    }

    @Override
    public Player getPlayer2() {
        return player2;
    }

    @Override
    public Arena getArena() {
        return arena;
    }

    @Override
    public DuelType getType() {
        return type;
    }

    @Override
    public DuelBet getBet() {
        return bet;
    }

    @Override
    public boolean isRoyal() {
        return royal;
    }

    @Override
    public MatchState getState() {
        return state;
    }

    @Override
    public boolean isEnded() {
        return ended.get();
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getDurationSeconds() {
        if (startTime <= 0) return 0;
        return (System.currentTimeMillis() - startTime) / 1000L;
    }

    @Override
    public Set<UUID> getSpectators() {
        return spectators;
    }

    @Override
    public void addSpectator(Player player) {
        if (player != null) {
            spectators.add(player.getUniqueId());
            if (timerBossBar != null) {
                player.showBossBar(timerBossBar);
            }
        }
    }

    @Override
    public void removeSpectator(Player player) {
        if (player != null) {
            spectators.remove(player.getUniqueId());
            if (timerBossBar != null) {
                player.hideBossBar(timerBossBar);
            }
        }
    }

    @Override
    public int getSpectatorCount() {
        return spectators.size();
    }

    @Override
    public boolean containsPlayer(UUID uuid) {
        return player1Id.equals(uuid) || player2Id.equals(uuid);
    }

    @Override
    public Player getOpponent(UUID uuid) {
        if (player1Id.equals(uuid)) return player2;
        if (player2Id.equals(uuid)) return player1;
        return null;
    }

    @Override
    public double getDamageDealt(UUID player) {
        return damageDealt.getOrDefault(player, 0.0);
    }

    @Override
    public void registerDamage(Player attacker, Player victim, double damage) {
        damageDealt.merge(attacker.getUniqueId(), damage, Double::sum);
    }

    @Override
    public int getPoints(UUID player) {
        return points.getOrDefault(player, 0);
    }

    @Override
    public void addPoints(UUID player, int pts) {
        points.merge(player, pts, Integer::sum);
    }

    @Override
    public InventorySnapshot getSnapshot(UUID player) {
        return snapshots.get(player);
    }

    @Override
    public MatchResult getResult() {
        return result;
    }

    @Override
    public void start() {
        arena.setState(ArenaState.BUSY);

        // Take snapshots
        snapshots.put(player1Id, InventorySnapshot.of(player1));
        snapshots.put(player2Id, InventorySnapshot.of(player2));

        // Setup players (gamemode, heal, saturation)
        prepareFighter(player1);
        prepareFighter(player2);

        // Teleport to spawns
        player1.teleportAsync(arena.getPos1());
        player2.teleportAsync(arena.getPos2());

        // Custom setup by subclass
        setupEquipment();

        // Preparation countdown
        int prepSeconds = Math.max(1, plugin.getConfig().getInt("settings.preparation_countdown_seconds", 5));
        new BukkitRunnable() {
            int countdown = prepSeconds;

            @Override
            public void run() {
                if (isEnded()) {
                    cancel();
                    return;
                }

                if (countdown > 0) {
                    Title title = Title.title(
                            MiniMessage.miniMessage().deserialize("<gold><b>" + countdown + "</b></gold>"),
                            MiniMessage.miniMessage().deserialize("<gray>Приготовьтесь к бою!"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(300))
                    );

                    if (player1.isOnline()) {
                        player1.showTitle(title);
                        player1.playSound(player1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                    if (player2.isOnline()) {
                        player2.showTitle(title);
                        player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                    countdown--;
                } else {
                    cancel();
                    if (isEnded()) {
                        return;
                    }
                    state = MatchState.FIGHTING;
                    startTime = System.currentTimeMillis();

                    Title fightTitle = Title.title(
                            MiniMessage.miniMessage().deserialize("<green><b>В БОЙ!</b></green>"),
                            MiniMessage.miniMessage().deserialize("<yellow>Да победит сильнейший!"),
                            Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofMillis(400))
                    );
                    if (player1.isOnline()) {
                        player1.showTitle(fightTitle);
                        player1.playSound(player1.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
                    }
                    if (player2.isOnline()) {
                        player2.showTitle(fightTitle);
                        player2.playSound(player2.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
                    }

                    if (royal) {
                        royalManager.broadcastStart(player1, player2);
                    }

                    // Start Action Bar Task
                    actionBarTask = new DuelActionBarTask(AbstractMatch.this).runTaskTimer(plugin, 0L, 10L);

                    // Start BossBar Countdown Task
                    startTimerTask();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public boolean hasRounds() {
        return false;
    }

    @Override
    public int getTimeLimitSeconds() {
        int seconds;
        if (hasRounds()) {
            seconds = plugin.getConfig().getInt("settings.round_time_limit_seconds", 600);
        } else {
            seconds = plugin.getConfig().getInt("settings.match_time_limit_seconds", 720);
        }
        return Math.max(10, seconds);
    }

    @Override
    public int getRemainingSeconds() {
        if (startTime <= 0) return getTimeLimitSeconds();
        long elapsed = (System.currentTimeMillis() - startTime) / 1000L;
        return (int) Math.max(0, getTimeLimitSeconds() - elapsed);
    }

    private void startTimerTask() {
        if (isEnded()) return;

        int totalSeconds = getTimeLimitSeconds();
        BossBar.Color initialColor = royal ? BossBar.Color.PURPLE : BossBar.Color.GREEN;
        this.timerBossBar = BossBar.bossBar(
                createBossBarTitle(totalSeconds),
                1.0f,
                initialColor,
                BossBar.Overlay.PROGRESS
        );

        if (player1.isOnline()) player1.showBossBar(timerBossBar);
        if (player2.isOnline()) player2.showBossBar(timerBossBar);
        for (UUID specId : spectators) {
            Player s = Bukkit.getPlayer(specId);
            if (s != null && s.isOnline()) {
                s.showBossBar(timerBossBar);
            }
        }

        timerTask = new BukkitRunnable() {
            private int lastSoundPlayedSecond = -1;

            @Override
            public void run() {
                if (isEnded()) {
                    cancel();
                    return;
                }

                int remaining = getRemainingSeconds();
                float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / totalSeconds));

                timerBossBar.progress(progress);
                timerBossBar.name(createBossBarTitle(remaining));
                timerBossBar.color(getBossBarColor(remaining));

                // Sound cues with duplicate/lag protection
                if ((remaining == 60 || remaining == 30) && lastSoundPlayedSecond != remaining) {
                    playTimerSound(Sound.BLOCK_NOTE_BLOCK_BELL, remaining == 60 ? 1.0f : 1.2f);
                    lastSoundPlayedSecond = remaining;
                } else if (remaining <= 10 && remaining > 0 && lastSoundPlayedSecond != remaining) {
                    float pitch = 1.0f + (10 - remaining) * 0.1f;
                    playTimerSound(Sound.BLOCK_NOTE_BLOCK_PLING, pitch);
                    lastSoundPlayedSecond = remaining;
                }

                if (remaining <= 0) {
                    cancel();
                    end(null, MatchEndReason.TIMEOUT);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private Component createBossBarTitle(int remainingSeconds) {
        int safeSec = Math.max(0, remainingSeconds);
        int min = safeSec / 60;
        int sec = safeSec % 60;
        String timeStr = String.format("%02d:%02d", min, sec);

        if (royal) {
            if (safeSec <= 30) {
                return MiniMessage.miniMessage().deserialize(
                        "<red>👑 <b>КОРОЛЕВСКАЯ ДУЭЛЬ</b> <dark_gray>•</dark_gray> До конца: <b>" + timeStr + "</b></red>"
                );
            }
            return MiniMessage.miniMessage().deserialize(
                    "<gradient:#FFD700:#FFA500>👑 <b>КОРОЛЕВСКАЯ ДУЭЛЬ</b></gradient> <dark_gray>•</dark_gray> <yellow>Время: <white><b>" + timeStr + "</b></white></yellow>"
            );
        } else {
            String prefix = hasRounds() ? "Раунд" : "Бой";
            if (safeSec <= 30) {
                return MiniMessage.miniMessage().deserialize(
                        "<red>⚠ <b>До завершения " + prefix.toLowerCase() + "а: " + timeStr + "</b></red>"
                );
            }
            return MiniMessage.miniMessage().deserialize(
                    "<gold>⚔ <b>" + prefix + "</b> <dark_gray>•</dark_gray> <yellow>Осталось: <white><b>" + timeStr + "</b></white></yellow>"
            );
        }
    }

    private BossBar.Color getBossBarColor(int remainingSeconds) {
        if (royal) {
            return (remainingSeconds <= 60) ? BossBar.Color.RED : BossBar.Color.PURPLE;
        }
        if (remainingSeconds <= 30) {
            return BossBar.Color.RED;
        } else if (remainingSeconds <= 120) {
            return BossBar.Color.YELLOW;
        }
        return BossBar.Color.GREEN;
    }

    private void playTimerSound(Sound sound, float pitch) {
        if (player1 != null && player1.isOnline()) player1.playSound(player1.getLocation(), sound, 0.8f, pitch);
        if (player2 != null && player2.isOnline()) player2.playSound(player2.getLocation(), sound, 0.8f, pitch);
        for (UUID specId : spectators) {
            Player spec = Bukkit.getPlayer(specId);
            if (spec != null && spec.isOnline()) {
                spec.playSound(spec.getLocation(), sound, 0.8f, pitch);
            }
        }
    }

    protected void prepareFighter(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        var maxHpAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double maxHp = (maxHpAttr != null) ? maxHpAttr.getValue() : 20.0;
        player.setHealth(maxHp);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    protected abstract void setupEquipment();

    protected abstract void cleanupCustomEntities();

    @Override
    public void end(UUID winnerId, MatchEndReason reason) {
        if (!ended.compareAndSet(false, true)) {
            return;
        }

        state = MatchState.ENDING;
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        if (timerTask != null) {
            timerTask.cancel();
        }

        if (timerBossBar != null) {
            if (player1 != null && player1.isOnline()) player1.hideBossBar(timerBossBar);
            if (player2 != null && player2.isOnline()) player2.hideBossBar(timerBossBar);
            for (UUID specId : spectators) {
                Player s = Bukkit.getPlayer(specId);
                if (s != null && s.isOnline()) s.hideBossBar(timerBossBar);
            }
        }

        cleanupCustomEntities();

        long duration = getDurationSeconds();
        UUID loserId = (winnerId != null) ? (winnerId.equals(player1Id) ? player2Id : player1Id) : null;

        Player winner = (winnerId != null) ? Bukkit.getPlayer(winnerId) : null;
        Player loser = (loserId != null) ? Bukkit.getPlayer(loserId) : null;

        // Calculate bets and rewards
        long calcPrizeMoney = 0L;
        int calcHonorDelta = 0;

        if (winnerId != null) {
            calcPrizeMoney = bet.totalMoneyPrizePool();
            if (royal && calcPrizeMoney > 0) {
                // Royal duel winner bonus: +25% bonus from royal treasury
                long bonus = calcPrizeMoney / 4;
                calcPrizeMoney += bonus;
            }

            calcHonorDelta = bet.hasHonor() ? bet.honorBet() : 25;
            if (royal) {
                calcHonorDelta = (int) (calcHonorDelta * 1.5);
            }
        }

        final long finalPrizeMoney = calcPrizeMoney;
        final int finalHonorDelta = calcHonorDelta;

        this.result = new MatchResult(
                matchId,
                player1Id,
                player2Id,
                winnerId,
                loserId,
                reason,
                duration,
                getDamageDealt(player1Id),
                getDamageDealt(player2Id),
                getPoints(player1Id),
                getPoints(player2Id),
                finalPrizeMoney,
                finalHonorDelta,
                finalHonorDelta,
                royal
        );

        // Process winner rewards & loser penalties
        if (winner != null && winner.isOnline()) {
            if (finalPrizeMoney > 0) {
                economyBridge.give(winner, finalPrizeMoney);
            }
            playerStorage.getOrCreatePlayer(winnerId, winner.getName()).thenAccept(data -> {
                var updated = data.withWin(finalHonorDelta, finalPrizeMoney, royal);
                playerStorage.savePlayer(updated);
                leaderboardsBridge.syncPlayerData(updated);
                leaderboardsBridge.recordDuelWin(winnerId, royal);
            });

            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            winner.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gold>⚔ <b>ПОБЕДА!</b> Вы победили в дуэли против <white>" + (loser != null ? loser.getName() : "противника") +
                    "</white>! <green>(+" + finalPrizeMoney + " монет, +" + finalHonorDelta + " Чести)"
            ));
        }

        if (loser != null && loser.isOnline()) {
            playerStorage.getOrCreatePlayer(loserId, loser.getName()).thenAccept(data -> {
                var updated = data.withLoss(finalHonorDelta, bet.moneyBet());
                playerStorage.savePlayer(updated);
                leaderboardsBridge.syncPlayerData(updated);
            });

            loser.playSound(loser.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            loser.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>⚔ <b>ПОРАЖЕНИЕ!</b> Вы проиграли дуэль против <white>" + (winner != null ? winner.getName() : "противника") +
                    "</white>. <gray>(-" + finalHonorDelta + " Чести)"
            ));
        }

        if (winner != null && loser != null && royal) {
            royalManager.broadcastWinner(winner, loser, finalPrizeMoney, finalHonorDelta);
            royalManager.clearActiveRoyalDuel(this);
        } else if (winnerId == null) {
            if (reason == MatchEndReason.TIMEOUT) {
                // DRAW (НИЧЬЯ ПО ИСТЕЧЕНИИ ВРЕМЕНИ)
                if (royal) {
                    // Royal duel draw: bets are burned and vanish ("ставки пропадают и больше ничего")
                    royalManager.broadcastTimeout(player1, player2, bet.totalMoneyPrizePool());
                    royalManager.clearActiveRoyalDuel(this);

                    Title drawTitle = Title.title(
                            MiniMessage.miniMessage().deserialize("<gradient:#FFD700:#FFA500><b>НИЧЬЯ!</b></gradient>"),
                            MiniMessage.miniMessage().deserialize("<red>Время вышло! Ставки сгорели."),
                            Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400))
                    );
                    if (player1.isOnline()) {
                        player1.showTitle(drawTitle);
                        player1.playSound(player1.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                        player1.sendMessage(MiniMessage.miniMessage().deserialize(
                                "<gold>👑 <b>КОРОЛЕВСКАЯ НИЧЬЯ!</b> Время вышло. Победитель не определён, ставки сгорели в казне."
                        ));
                    }
                    if (player2.isOnline()) {
                        player2.showTitle(drawTitle);
                        player2.playSound(player2.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                        player2.sendMessage(MiniMessage.miniMessage().deserialize(
                                "<gold>👑 <b>КОРОЛЕВСКАЯ НИЧЬЯ!</b> Время вышло. Победитель не определён, ставки сгорели в казне."
                        ));
                    }
                } else {
                    // Normal duel draw: "а если обычные бои то просто ничего не происходит"
                    // Refund staked money to both players
                    if (bet.hasMoney()) {
                        economyBridge.give(player1, bet.moneyBet());
                        economyBridge.give(player2, bet.moneyBet());
                    }

                    Title drawTitle = Title.title(
                            MiniMessage.miniMessage().deserialize("<yellow><b>НИЧЬЯ!</b></yellow>"),
                            MiniMessage.miniMessage().deserialize("<gray>Время поединка истекло"),
                            Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400))
                    );
                    String refundMsg = bet.hasMoney() ? " <green>(Ставка " + bet.moneyBet() + " монет возвращена)</green>" : "";

                    if (player1.isOnline()) {
                        player1.showTitle(drawTitle);
                        player1.playSound(player1.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                        player1.sendMessage(MiniMessage.miniMessage().deserialize(
                                "<yellow>⚔ <b>НИЧЬЯ!</b> Время поединка истекло." + refundMsg
                        ));
                    }
                    if (player2.isOnline()) {
                        player2.showTitle(drawTitle);
                        player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                        player2.sendMessage(MiniMessage.miniMessage().deserialize(
                                "<yellow>⚔ <b>НИЧЬЯ!</b> Время поединка истекло." + refundMsg
                        ));
                    }
                }
            } else {
                // Admin force cancellation or server shutdown
                if (royal) {
                    royalManager.clearActiveRoyalDuel(this);
                }
                if (bet.hasMoney()) {
                    economyBridge.give(player1, bet.moneyBet());
                    economyBridge.give(player2, bet.moneyBet());
                }
                Component stopMsg = MiniMessage.miniMessage().deserialize(
                        "<yellow>⚠ <b>Поединок остановлен администрацией/сервером.</b> Ставки возвращены."
                );
                if (player1.isOnline()) player1.sendMessage(stopMsg);
                if (player2.isOnline()) player2.sendMessage(stopMsg);
            }
        }

        // Log match to database history
        playerStorage.logHistory(new DuelHistoryEntry(
                0L,
                player1Id,
                player2Id,
                player1.getName(),
                player2.getName(),
                winnerId,
                type,
                bet.moneyBet(),
                bet.honorBet(),
                royal,
                System.currentTimeMillis(),
                (int) duration
        ));

        // Restore players after 2 seconds (or immediately if server is disabling)
        Runnable cleanupTask = () -> {
            restoreFighter(player1, snapshots.get(player1Id));
            restoreFighter(player2, snapshots.get(player2Id));

            // Return arena to free
            arena.setState(ArenaState.FREE);
            state = MatchState.ENDED;

            // Notify MatchManager
            matchManager.endMatch(this, winnerId, reason);
        };

        if (!plugin.isEnabled()) {
            cleanupTask.run();
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, cleanupTask, 40L);
        }
    }

    protected void restoreFighter(Player player, InventorySnapshot snapshot) {
        if (player != null && player.isOnline() && snapshot != null) {
            snapshot.restore(player);
        }
    }

    @Override
    public void handleDisconnect(Player player) {
        if (isEnded()) return;

        // Synchronously restore the disconnecting player before their session is saved and closed
        InventorySnapshot discSnap = snapshots.remove(player.getUniqueId());
        if (discSnap != null) {
            discSnap.restore(player);
            org.bukkit.Location ret = discSnap.getReturnLocation();
            if (ret != null && ret.getWorld() != null) {
                player.teleport(ret);
            }
        }

        Player opponent = getOpponent(player.getUniqueId());
        UUID winnerId = (opponent != null) ? opponent.getUniqueId() : null;

        // Shame message & behavior penalty
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                "<red>⚔ <b>ПОЗОР!</b> Игрок <gold>" + player.getName() + "</gold> сбежал с дуэли против <gold>" +
                (opponent != null ? opponent.getName() : "соперника") + "</gold>!"
        ));

        // LoveBehavior politeness deduction
        behaviorBridge.punishFleeing(player.getUniqueId(), 150);

        end(winnerId, MatchEndReason.DISCONNECT);
    }
}
