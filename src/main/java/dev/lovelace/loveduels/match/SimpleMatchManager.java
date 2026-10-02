package dev.lovelace.loveduels.match;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.arena.ArenaManager;
import dev.lovelace.loveduels.core.CooldownManager;
import dev.lovelace.loveduels.core.DuelBet;
import dev.lovelace.loveduels.core.DuelRequest;
import dev.lovelace.loveduels.core.DuelType;
import dev.lovelace.loveduels.integration.LoveBehaviorBridge;
import dev.lovelace.loveduels.integration.LoveEconomyBridge;
import dev.lovelace.loveduels.integration.LoveLeaderboardsBridge;
import dev.lovelace.loveduels.kit.Kit;
import dev.lovelace.loveduels.kit.KitManager;
import dev.lovelace.loveduels.match.spear.HorseStaminaHandler;
import dev.lovelace.loveduels.match.spear.SpearChargeHandler;
import dev.lovelace.loveduels.match.spear.SpearItem;
import dev.lovelace.loveduels.royal.RoyalDuelManager;
import dev.lovelace.loveduels.spectator.SpectatorManager;
import dev.lovelace.loveduels.storage.PlayerStorage;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class SimpleMatchManager implements MatchManager {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final PlayerStorage playerStorage;
    private final CooldownManager cooldownManager;
    private final LoveEconomyBridge economyBridge;
    private final LoveBehaviorBridge behaviorBridge;
    private final LoveLeaderboardsBridge leaderboardsBridge;
    private final RoyalDuelManager royalManager;
    private final SpearItem spearItem;
    private final SpearChargeHandler chargeHandler;
    private final HorseStaminaHandler staminaHandler;

    private final Map<UUID, Match> activeMatches = new ConcurrentHashMap<>();
    private final Cache<UUID, DuelRequest> pendingRequests = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(2000)
            .build();
    private final Cache<UUID, ReadinessSession> pendingReadiness;
    private final Cache<UUID, MatchResult> lastMatchResults = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(2000)
            .build();

    private SpectatorManager spectatorManager;
    private final PostMatchGuard postMatchGuard;
    private Consumer<MatchResult> postDuelSummaryOpener;

    public SimpleMatchManager(
            Plugin plugin,
            ArenaManager arenaManager,
            KitManager kitManager,
            PlayerStorage playerStorage,
            CooldownManager cooldownManager,
            LoveEconomyBridge economyBridge,
            LoveBehaviorBridge behaviorBridge,
            LoveLeaderboardsBridge leaderboardsBridge,
            RoyalDuelManager royalManager
    ) {
        this.plugin = plugin;
        this.pendingReadiness = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(2))
                .maximumSize(1000)
                .removalListener((UUID key, ReadinessSession session, com.github.benmanes.caffeine.cache.RemovalCause cause) -> {
                    if (session != null && cause.wasEvicted() && plugin.isEnabled()) {
                        Bukkit.getScheduler().runTask(plugin, () -> session.cancel(null));
                    }
                })
                .build();
        Bukkit.getScheduler().runTaskTimer(plugin, pendingReadiness::cleanUp, 400L, 400L);
        this.arenaManager = arenaManager;
        this.kitManager = kitManager;
        this.playerStorage = playerStorage;
        this.cooldownManager = cooldownManager;
        this.economyBridge = economyBridge;
        this.behaviorBridge = behaviorBridge;
        this.leaderboardsBridge = leaderboardsBridge;
        this.royalManager = royalManager;
        this.spearItem = new SpearItem(plugin);
        this.chargeHandler = new SpearChargeHandler();
        this.staminaHandler = new HorseStaminaHandler();
        this.postMatchGuard = new PostMatchGuard(plugin);
    }

    public PostMatchGuard getPostMatchGuard() {
        return postMatchGuard;
    }

    public void setPostDuelSummaryOpener(Consumer<MatchResult> opener) {
        this.postDuelSummaryOpener = opener;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public void setSpectatorManager(SpectatorManager spectatorManager) {
        this.spectatorManager = spectatorManager;
    }

    public SpearItem getSpearItem() {
        return spearItem;
    }

    public SpearChargeHandler getChargeHandler() {
        return chargeHandler;
    }

    public HorseStaminaHandler getStaminaHandler() {
        return staminaHandler;
    }

    @Override
    public boolean isInMatch(UUID uuid) {
        return activeMatches.containsKey(uuid);
    }

    @Override
    public Optional<Match> getMatch(UUID uuid) {
        return Optional.ofNullable(activeMatches.get(uuid));
    }

    @Override
    public Collection<Match> getActiveMatches() {
        Set<Match> unique = new HashSet<>(activeMatches.values());
        return Collections.unmodifiableCollection(unique);
    }

    public void sendRequest(DuelRequest request) {
        pendingRequests.put(request.targetId(), request);
    }

    public Optional<DuelRequest> getPendingRequest(UUID targetId) {
        DuelRequest req = pendingRequests.getIfPresent(targetId);
        if (req != null && req.isExpired()) {
            pendingRequests.invalidate(targetId);
            return Optional.empty();
        }
        return Optional.ofNullable(req);
    }

    public void removePendingRequest(UUID targetId) {
        pendingRequests.invalidate(targetId);
    }

    public void setPendingReadiness(UUID p1, UUID p2, ReadinessSession session) {
        pendingReadiness.put(p1, session);
        pendingReadiness.put(p2, session);
    }

    public Optional<ReadinessSession> getReadinessSession(UUID player) {
        return Optional.ofNullable(pendingReadiness.getIfPresent(player));
    }

    public void removeReadinessSession(UUID p1, UUID p2) {
        pendingReadiness.invalidate(p1);
        pendingReadiness.invalidate(p2);
    }

    public Optional<MatchResult> getLastResult(UUID player) {
        return Optional.ofNullable(lastMatchResults.getIfPresent(player));
    }

    @Override
    public void createAndStartMatch(Player p1, Player p2, DuelType type, String kitId, DuelBet bet, boolean royal) {
        Optional<Arena> arenaOpt = arenaManager.findAvailableArena(type);
        if (arenaOpt.isEmpty()) {
            p1.sendMessage(MiniMessage.miniMessage().deserialize("<red>Нет свободных арен для этого режима."));
            p2.sendMessage(MiniMessage.miniMessage().deserialize("<red>Нет свободных арен для этого режима."));
            if (bet.hasMoney()) {
                economyBridge.give(p1, bet.moneyBet());
                economyBridge.give(p2, bet.moneyBet());
                economyBridge.releaseEscrow(p1.getUniqueId());
            }
            return;
        }

        Arena arena = arenaOpt.get();

        Match match;
        if (type == DuelType.HORSE_SPEAR) {
            match = new HorseSpearMatch(
                    plugin, p1, p2, arena, bet, royal,
                    playerStorage, economyBridge, behaviorBridge, leaderboardsBridge, royalManager, this,
                    spearItem, chargeHandler, staminaHandler
            );
        } else {
            Kit kit = (kitId != null) ? kitManager.getKit(kitId).orElse(null) : null;
            match = new StandardMatch(
                    plugin, p1, p2, arena, type, kit, bet, royal,
                    playerStorage, economyBridge, behaviorBridge, leaderboardsBridge, royalManager, this
            );
        }

        if (royal) {
            if (!royalManager.registerRoyalDuel(match)) {
                p1.sendMessage(MiniMessage.miniMessage().deserialize("<red>На сервере уже идёт королевская дуэль."));
                p2.sendMessage(MiniMessage.miniMessage().deserialize("<red>На сервере уже идёт королевская дуэль."));
                if (bet.hasMoney()) {
                    economyBridge.give(p1, bet.moneyBet());
                    economyBridge.give(p2, bet.moneyBet());
                    economyBridge.releaseEscrow(p1.getUniqueId());
                }
                return;
            }
        }

        activeMatches.put(p1.getUniqueId(), match);
        activeMatches.put(p2.getUniqueId(), match);

        match.start();
    }

    @Override
    public void endMatch(Match match, UUID winnerId, MatchEndReason reason) {
        postMatchGuard.lock(match.getPlayer1Id(), match.getPlayer2Id(), match.getArena());
        activeMatches.remove(match.getPlayer1Id());
        activeMatches.remove(match.getPlayer2Id());

        if (spectatorManager != null) {
            for (UUID specId : match.getSpectators()) {
                Player spec = Bukkit.getPlayer(specId);
                if (spec != null && spec.isOnline()) {
                    spectatorManager.removeSpectator(spec);
                }
            }
        }

        MatchResult res = match.getResult();
        if (res != null) {
            lastMatchResults.put(match.getPlayer1Id(), res);
            lastMatchResults.put(match.getPlayer2Id(), res);

            if (postDuelSummaryOpener != null) {
                postDuelSummaryOpener.accept(res);
            }
        }
    }

    @Override
    public void handleDisconnect(Player player) {
        Match match = activeMatches.get(player.getUniqueId());
        if (match != null) {
            match.handleDisconnect(player);
            return;
        }

        Optional<ReadinessSession> readinessOpt = getReadinessSession(player.getUniqueId());
        if (readinessOpt.isPresent()) {
            readinessOpt.get().cancel(player.getUniqueId());
            removeReadinessSession(
                    readinessOpt.get().getPlayer1(),
                    readinessOpt.get().getPlayer2()
            );
        }
    }

    @Override
    public boolean forceEnd(Player player) {
        Match match = activeMatches.get(player.getUniqueId());
        if (match != null && !match.isEnded()) {
            match.end(null, MatchEndReason.ADMIN_FORCE);
            return true;
        }
        return false;
    }

    @Override
    public boolean hasActiveRoyalDuel() {
        return royalManager.hasActiveRoyalDuel();
    }
}
