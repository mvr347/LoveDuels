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
            Plugin plugin, Player player1, Player player2, Arena arena, DuelType type, DuelBet bet, boolean royal,
            PlayerStorage playerStorage, LoveEconomyBridge economyBridge, LoveBehaviorBridge behaviorBridge,
            LoveLeaderboardsBridge leaderboardsBridge, RoyalDuelManager royalManager, MatchManager matchManager) {
        this.plugin = plugin; this.player1 = player1; this.player2 = player2;
        this.player1Id = player1.getUniqueId(); this.player2Id = player2.getUniqueId();
        this.arena = arena; this.type = type; this.bet = bet; this.royal = royal;
        this.playerStorage = playerStorage; this.economyBridge = economyBridge;
        this.behaviorBridge = behaviorBridge; this.leaderboardsBridge = leaderboardsBridge;
        this.royalManager = royalManager; this.matchManager = matchManager;
    }

    @Override public UUID getMatchId() { return matchId; }
    @Override public UUID getPlayer1Id() { return player1Id; }
    @Override public UUID getPlayer2Id() { return player2Id; }
    @Override public Player getPlayer1() { return player1; }
    @Override public Player getPlayer2() { return player2; }
    @Override public Arena getArena() { return arena; }
    @Override public DuelType getType() { return type; }
    @Override public DuelBet getBet() { return bet; }
    @Override public boolean isRoyal() { return royal; }
    @Override public MatchState getState() { return state; }
    @Override public boolean isEnded() { return ended.get(); }
    @Override public long getStartTime() { return startTime; }
    @Override public long getDurationSeconds() { return startTime <= 0 ? 0 : (System.currentTimeMillis() - startTime) / 1000L; }
    @Override public Set<UUID> getSpectators() { return spectators; }
    @Override public void addSpectator(Player player) {
        if (player != null) { spectators.add(player.getUniqueId()); if (timerBossBar != null) player.showBossBar(timerBossBar); }
    }
    @Override public void removeSpectator(Player player) {
        if (player != null) { spectators.remove(player.getUniqueId()); if (timerBossBar != null) player.hideBossBar(timerBossBar); }
    }
    @Override public int getSpectatorCount() { return spectators.size(); }
    @Override public boolean containsPlayer(UUID uuid) { return player1Id.equals(uuid) || player2Id.equals(uuid); }
    @Override public Player getOpponent(UUID uuid) {
        if (player1Id.equals(uuid)) return player2; if (player2Id.equals(uuid)) return player1; return null;
    }
    @Override public double getDamageDealt(UUID player) { return damageDealt.getOrDefault(player, 0.0); }
    @Override public void registerDamage(Player attacker, Player victim, double damage) { damageDealt.merge(attacker.getUniqueId(), damage, Double::sum); }
    @Override public int getPoints(UUID player) { return points.getOrDefault(player, 0); }
    @Override public void addPoints(UUID player, int pts) { points.merge(player, pts, Integer::sum); }
    @Override public InventorySnapshot getSnapshot(UUID player) { return snapshots.get(player); }
    @Override public MatchResult getResult() { return result; }

    @Override
    public void start() {
        arena.setState(ArenaState.BUSY);
        snapshots.put(player1Id, InventorySnapshot.of(player1));
        snapshots.put(player2Id, InventorySnapshot.of(player2));
        prepareFighter(player1);
        prepareFighter(player2);
        player1.teleportAsync(arena.getPos1());
        player2.teleportAsync(arena.getPos2());
        setupEquipment();

        int prepSeconds = royal
                ? Math.max(1, plugin.getConfig().getInt("settings.preparation_countdown_royal_seconds",
                        plugin.getConfig().getInt("settings.preparation_countdown_seconds", 15)))
                : Math.max(1, plugin.getConfig().getInt("settings.preparation_countdown_seconds", 10));
        new BukkitRunnable() {
            int countdown = prepSeconds;
            @Override public void run() {
                if (isEnded()) { cancel(); return; }
                if (countdown > 0) {
                    Title title = Title.title(
                            MiniMessage.miniMessage().deserialize("<gold>" + countdown + "</gold>"),
                            MiniMessage.miniMessage().deserialize("<gray>Приготовьтесь к бою!"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(300)));
                    if (player1.isOnline()) { player1.showTitle(title); player1.playSound(player1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f); }
                    if (player2.isOnline()) { player2.showTitle(title); player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f); }
                    countdown--;
                } else {
                    cancel();
                    if (isEnded()) return;
                    state = MatchState.FIGHTING;
                    startTime = System.currentTimeMillis();
                    Title fightTitle = Title.title(
                            MiniMessage.miniMessage().deserialize("<green>В БОЙ!</green>"),
                            MiniMessage.miniMessage().deserialize("<yellow>Да победит сильнейший!"),
                            Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofMillis(400)));
                    if (player1.isOnline()) { player1.showTitle(fightTitle); player1.playSound(player1.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f); }
                    if (player2.isOnline()) { player2.showTitle(fightTitle); player2.playSound(player2.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f); }
                    if (royal) royalManager.broadcastStart(player1, player2);
                    actionBarTask = new DuelActionBarTask(AbstractMatch.this).runTaskTimer(plugin, 0L, 10L);
                    startTimerTask();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @Override public boolean hasRounds() { return false; }
    @Override public int getTimeLimitSeconds() {
        int seconds = hasRounds()
                ? plugin.getConfig().getInt("settings.round_time_limit_seconds", 600)
                : plugin.getConfig().getInt("settings.match_time_limit_seconds", 720);
        return Math.max(10, seconds);
    }
    @Override public int getRemainingSeconds() {
        if (startTime <= 0) return getTimeLimitSeconds();
        long elapsed = (System.currentTimeMillis() - startTime) / 1000L;
        return (int) Math.max(0, getTimeLimitSeconds() - elapsed);
    }

    private void startTimerTask() {
        if (isEnded()) return;
        int totalSeconds = getTimeLimitSeconds();
        this.timerBossBar = BossBar.bossBar(createBossBarTitle(totalSeconds), 1.0f,
                royal ? BossBar.Color.PURPLE : BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        if (player1.isOnline()) player1.showBossBar(timerBossBar);
        if (player2.isOnline()) player2.showBossBar(timerBossBar);
        for (UUID specId : spectators) {
            Player s = Bukkit.getPlayer(specId);
            if (s != null && s.isOnline()) s.showBossBar(timerBossBar);
        }
        timerTask = new BukkitRunnable() {
            private int lastSoundPlayedSecond = -1;
            @Override public void run() {
                if (isEnded()) { cancel(); return; }
                int remaining = getRemainingSeconds();
                float progress = Math.max(0f, Math.min(1f, (float) remaining / totalSeconds));
                timerBossBar.progress(progress);
                timerBossBar.name(createBossBarTitle(remaining));
                timerBossBar.color(getBossBarColor(remaining));
                if ((remaining == 60 || remaining == 30) && lastSoundPlayedSecond != remaining) {
                    playTimerSound(Sound.BLOCK_NOTE_BLOCK_BELL, remaining == 60 ? 1.0f : 1.2f);
                    lastSoundPlayedSecond = remaining;
                } else if (remaining <= 10 && remaining > 0 && lastSoundPlayedSecond != remaining) {
                    playTimerSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f + (10 - remaining) * 0.1f);
                    lastSoundPlayedSecond = remaining;
                }
                if (remaining <= 0) {
                    cancel();
                    if (isArmisticeApplicable()) startArmistice();
                    else end(null, MatchEndReason.TIMEOUT);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private Component createBossBarTitle(int remainingSeconds) {
        int safeSec = Math.max(0, remainingSeconds);
        String timeStr = String.format("%02d:%02d", safeSec / 60, safeSec % 60);
        if (royal) {
            return MiniMessage.miniMessage().deserialize(
                    safeSec <= 30
                            ? "<red>КОРОЛЕВСКАЯ ДУЭЛЬ · " + timeStr + "</red>"
                            : "<gradient:#FFD700:#FFA500>КОРОЛЕВСКАЯ ДУЭЛЬ</gradient> · <yellow>" + timeStr + "</yellow>");
        }
        return MiniMessage.miniMessage().deserialize(
                safeSec <= 30 ? "<red>До конца: " + timeStr + "</red>" : "<gold>Бой</gold> · <yellow>" + timeStr + "</yellow>");
    }

    private BossBar.Color getBossBarColor(int remainingSeconds) {
        if (royal) return remainingSeconds <= 60 ? BossBar.Color.RED : BossBar.Color.PURPLE;
        if (remainingSeconds <= 30) return BossBar.Color.RED;
        if (remainingSeconds <= 120) return BossBar.Color.YELLOW;
        return BossBar.Color.GREEN;
    }

    protected boolean isArmisticeApplicable() {
        if (!plugin.getConfig().getBoolean("armistice.enabled", true)) return false;
        if (royal && plugin.getConfig().getBoolean("armistice.skip_royal", true)) return false;
        return state == MatchState.FIGHTING && !isEnded();
    }

    protected void startArmistice() {
        if (isEnded()) return;
        state = MatchState.ARMISTICE;
        int duration = Math.max(3, plugin.getConfig().getInt("armistice.duration_seconds", 15));
        int surrenderPercent = Math.max(1, Math.min(100, plugin.getConfig().getInt("armistice.surrender_stake_percent", 25)));
        Title title = Title.title(
                MiniMessage.miniMessage().deserialize("<gold>ПЕРЕМИРИЕ</gold>"),
                MiniMessage.miniMessage().deserialize("<yellow>Урон заморожен. /duel forfeit — скидка " + surrenderPercent + "%</yellow>"),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400)));
        for (Player p : new Player[]{player1, player2}) {
            if (p != null && p.isOnline()) {
                p.showTitle(title);
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
            }
        }
        if (timerBossBar != null) {
            timerBossBar.color(BossBar.Color.WHITE);
            timerBossBar.progress(1f);
            timerBossBar.name(MiniMessage.miniMessage().deserialize("<gold>ПЕРЕМИРИЕ · " + duration + "с</gold>"));
        }
        timerTask = new BukkitRunnable() {
            private int remaining = duration;
            @Override public void run() {
                if (isEnded()) { cancel(); return; }
                remaining--;
                if (remaining <= 0) { cancel(); end(null, MatchEndReason.TIMEOUT); return; }
                if (timerBossBar != null) {
                    timerBossBar.progress(Math.max(0f, (float) remaining / duration));
                    timerBossBar.name(MiniMessage.miniMessage().deserialize("<gold>ПЕРЕМИРИЕ · " + remaining + "с</gold>"));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void playTimerSound(Sound sound, float pitch) {
        if (player1 != null && player1.isOnline()) player1.playSound(player1.getLocation(), sound, 1f, pitch);
        if (player2 != null && player2.isOnline()) player2.playSound(player2.getLocation(), sound, 1f, pitch);
    }

    protected void prepareFighter(Player player) {
        if (player == null || !player.isOnline()) return;
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setExhaustion(0f);
    }

    protected abstract void setupEquipment();
    protected void cleanupCustomEntities() {}

    @Override
    public void end(UUID winnerId, MatchEndReason reason) {
        if (!ended.compareAndSet(false, true)) return;
        state = MatchState.ENDING;
        if (actionBarTask != null) actionBarTask.cancel();
        if (timerTask != null) timerTask.cancel();
        if (timerBossBar != null) {
            if (player1 != null && player1.isOnline()) player1.hideBossBar(timerBossBar);
            if (player2 != null && player2.isOnline()) player2.hideBossBar(timerBossBar);
            for (UUID specId : spectators) {
                Player s = Bukkit.getPlayer(specId);
                if (s != null && s.isOnline()) s.hideBossBar(timerBossBar);
            }
        }
        cleanupCustomEntities();

        // Delegate full payout/stats logic: restore from main branch end() body is complex;
        // minimal safe path: refund if no winner, else pay prize pool once.
        long duration = getDurationSeconds();
        UUID loserId = winnerId != null ? (winnerId.equals(player1Id) ? player2Id : player1Id) : null;
        long prize = bet.totalMoneyPrizePool();
        int honor = bet.hasHonor() ? bet.honorBet() : 25;
        if (royal && prize > 0) prize += prize / 4;
        if (royal) honor = (int) Math.round(honor * plugin.getConfig().getDouble("royal_duel.honor_multiplier", 1.5));

        if (winnerId != null && prize > 0) economyBridge.giveTo(winnerId, prize);
        else if (winnerId == null && bet.hasMoney() && reason != MatchEndReason.TIMEOUT || (reason == MatchEndReason.TIMEOUT && !royal)) {
            // draw refund both (non-royal timeout)
            if (bet.hasMoney()) {
                economyBridge.giveTo(player1Id, bet.moneyBet());
                economyBridge.giveTo(player2Id, bet.moneyBet());
            }
        }
        economyBridge.releaseEscrow(player1Id);

        Player winner = winnerId != null ? Bukkit.getPlayer(winnerId) : null;
        Player loser = loserId != null ? Bukkit.getPlayer(loserId) : null;
        if (winner != null && winner.isOnline()) {
            final int h = honor; final long pr = prize;
            playerStorage.getOrCreatePlayer(winnerId, winner.getName()).thenAccept(data -> {
                playerStorage.savePlayer(data.withWin(h, pr, royal));
                leaderboardsBridge.syncPlayerData(data.withWin(h, pr, royal));
                leaderboardsBridge.recordDuelWin(winnerId, royal);
            });
            winner.sendMessage(MiniMessage.miniMessage().deserialize("<green>Победа!</green>"));
        }
        if (loser != null && loser.isOnline()) {
            final int h = honor;
            playerStorage.getOrCreatePlayer(loserId, loser.getName()).thenAccept(data -> {
                playerStorage.savePlayer(data.withLoss(h, bet.moneyBet()));
                leaderboardsBridge.syncPlayerData(data.withLoss(h, bet.moneyBet()));
            });
            loser.sendMessage(MiniMessage.miniMessage().deserialize("<red>Поражение.</red>"));
        }

        result = new MatchResult(matchId, player1Id, player2Id, winnerId, type, royal, bet,
                damageDealt.getOrDefault(player1Id, 0.0), damageDealt.getOrDefault(player2Id, 0.0),
                duration, reason);

        Runnable cleanup = () -> {
            restoreFighter(player1, snapshots.get(player1Id));
            restoreFighter(player2, snapshots.get(player2Id));
            arena.setState(ArenaState.FREE);
            state = MatchState.ENDED;
            matchManager.endMatch(this, winnerId, reason);
        };
        if (!plugin.isEnabled()) cleanup.run();
        else Bukkit.getScheduler().runTaskLater(plugin, cleanup, 40L);
    }

    protected void restoreFighter(Player player, InventorySnapshot snapshot) {
        if (player != null && player.isOnline() && snapshot != null) snapshot.restore(player);
    }

    @Override
    public void forfeit(Player player) {
        if (isEnded() || player == null) return;
        Player opp = getOpponent(player.getUniqueId());
        end(opp != null ? opp.getUniqueId() : null, MatchEndReason.FORFEIT);
    }

    @Override
    public void handleDisconnect(Player player) {
        if (isEnded() || player == null) return;
        if (plugin.getConfig().getBoolean("behavior.broadcast_fleeing", true)) {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                    "<red>" + player.getName() + " покинул дуэль."));
        }
        behaviorBridge.penalizeFleeing(player.getUniqueId(),
                plugin.getConfig().getInt("behavior.fleeing_politeness_penalty", 150));
        Player opp = getOpponent(player.getUniqueId());
        end(opp != null ? opp.getUniqueId() : null, MatchEndReason.DISCONNECT);
    }
}
