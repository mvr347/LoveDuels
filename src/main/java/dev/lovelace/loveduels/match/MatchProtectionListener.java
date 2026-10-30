package dev.lovelace.loveduels.match;

import dev.lovelace.loveduels.util.Notify;
import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.spectator.SpectatorManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Strict anti-abuse and event listener during active duel matches.
 */
public final class MatchProtectionListener implements Listener {

    private final Plugin plugin;
    private final MatchManager matchManager;
    private final SpectatorManager spectatorManager;
    private final PostMatchGuard postMatchGuard;
    private final Set<String> allowedCommands;
    /** Easter egg: which fighter the lethal blow that ended each match landed on, and in which tick. */
    private final java.util.Map<UUID, LethalEnd> lethalEnds = new java.util.HashMap<>();

    private record LethalEnd(UUID victim, long tick) {}

    public MatchProtectionListener(Plugin plugin, MatchManager matchManager, SpectatorManager spectatorManager, PostMatchGuard postMatchGuard) {
        this.plugin = plugin;
        this.matchManager = matchManager;
        this.spectatorManager = spectatorManager;
        this.postMatchGuard = postMatchGuard;
        this.allowedCommands = new HashSet<>(plugin.getConfig().getStringList("protection.command_whitelist"));
        if (this.allowedCommands.isEmpty()) {
            this.allowedCommands.addAll(Set.of("duel", "дуэль", "msg", "tell", "w", "r"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (matchManager.isInMatch(player.getUniqueId())) {
            String message = event.getMessage().substring(1).trim().toLowerCase();
            String root = message.split("\\s+")[0];

            if (!allowedCommands.contains(root) && !player.hasPermission("loveduels.admin")) {
                event.setCancelled(true);
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Во время дуэли эта команда недоступна."
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        var matchOpt = matchManager.getMatch(player.getUniqueId());
        if (matchOpt.isEmpty()) return;

        Match match = matchOpt.get();
        Arena arena = match.getArena();
        Location to = event.getTo();
        if (to == null) return;

        if (!arena.isInCombatBounds(to)) {
            event.setCancelled(true);
            Location from = event.getFrom();
            if (!arena.isInCombatBounds(from)) {
                Location spawn = match.getPlayer1Id().equals(player.getUniqueId()) ? arena.getPos1() : arena.getPos2();
                if (spawn != null) {
                    player.teleportAsync(spawn);
                }
            }
            Notify.actionBar(player, MiniMessage.miniMessage().deserialize(
                    "<red>Не выходите за границы боевой арены."
            ));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        if (spectatorManager.isSpectating(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Player atk
                && spectatorManager.isSpectating(atk.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (event instanceof EntityDamageByEntityEvent by2) {
            Player attacker = null;
            if (by2.getDamager() instanceof Player p) attacker = p;
            else if (by2.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) attacker = p;
            if (attacker != null && postMatchGuard != null && postMatchGuard.blocksDamage(attacker, victim)) {
                event.setCancelled(true);
                return;
            }
        }

        var matchOpt = matchManager.getMatch(victim.getUniqueId());
        if (matchOpt.isEmpty()) return;

        Match match = matchOpt.get();
        if (match.isEnded()) {
            event.setCancelled(true);
            checkWestern(match, victim, event.getFinalDamage());
            return;
        }

        if (match.getState() == MatchState.PREPARATION || match.getState() == MatchState.ARMISTICE) {
            event.setCancelled(true);
            return;
        }

        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Player attacker = null;
            if (byEntity.getDamager() instanceof Player p) {
                attacker = p;
            } else if (byEntity.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                attacker = p;
            }

            if (attacker != null && !match.containsPlayer(attacker.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        if (victim.getHealth() - event.getFinalDamage() <= 0.0) {
            event.setCancelled(true);

            var maxHealthAttr = victim.getAttribute(Attribute.MAX_HEALTH);
            double maxHp = (maxHealthAttr != null) ? maxHealthAttr.getValue() : 20.0;
            victim.setHealth(maxHp);
            victim.setFoodLevel(20);
            victim.setFireTicks(0);
            victim.setVelocity(new Vector(0, 0, 0));

            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);

            Player killer = null;
            if (event instanceof EntityDamageByEntityEvent byEntity) {
                if (byEntity.getDamager() instanceof Player p) {
                    killer = p;
                } else if (byEntity.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                    killer = p;
                }
            }

            UUID winnerId;
            if (killer != null && match.containsPlayer(killer.getUniqueId())) {
                winnerId = killer.getUniqueId();
                match.registerDamage(killer, victim, event.getFinalDamage());
            } else {
                Player opp = match.getOpponent(victim.getUniqueId());
                winnerId = (opp != null) ? opp.getUniqueId() : null;
            }

            long now = Bukkit.getCurrentTick();
            lethalEnds.values().removeIf(end -> now - end.tick() > 200);
            lethalEnds.put(match.getPlayer1Id(), new LethalEnd(victim.getUniqueId(), now));
            match.end(winnerId, MatchEndReason.KILL);
        } else {
            if (event instanceof EntityDamageByEntityEvent byEntity) {
                Player attacker = null;
                if (byEntity.getDamager() instanceof Player p) {
                    attacker = p;
                } else if (byEntity.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                    attacker = p;
                }
                if (attacker != null && match.containsPlayer(attacker.getUniqueId())) {
                    match.registerDamage(attacker, victim, event.getFinalDamage());
                }
            }
        }
    }

    /**
     * Easter egg: a second lethal blow on the other fighter right after the one that ended the match means both
     * fell at once. The result stays as it is (stakes and rating are untouched); both only get a remark.
     */
    private void checkWestern(Match match, Player victim, double damage) {
        if (!plugin.getConfig().getBoolean("easter-eggs.western.enabled", true)) return;
        LethalEnd first = lethalEnds.get(match.getPlayer1Id());
        if (first == null || victim.getHealth() - damage > 0.0) return;
        int window = plugin.getConfig().getInt("easter-eggs.western.window-ticks", 2);
        if (!WesternRules.simultaneous(first.victim(), victim.getUniqueId(), first.tick(), Bukkit.getCurrentTick(), window)) {
            return;
        }
        lethalEnds.remove(match.getPlayer1Id());
        for (Player fighter : new Player[] {match.getPlayer1(), match.getPlayer2()}) {
            if (fighter == null || !fighter.isOnline()) continue;
            Notify.actionBar(fighter, dev.lovelace.loveduels.util.Lang.component("easter.western"));
            dev.lovelace.loveduels.integration.SecretsHook.discover(fighter.getUniqueId(), "western");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        var matchOpt = matchManager.getMatch(victim.getUniqueId());
        if (matchOpt.isEmpty()) return;

        Match match = matchOpt.get();
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.deathMessage(null);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (victim.isOnline()) {
                victim.spigot().respawn();
            }
        });

        if (!match.isEnded()) {
            Player killer = victim.getKiller();
            UUID winnerId;
            if (killer != null && match.containsPlayer(killer.getUniqueId())) {
                winnerId = killer.getUniqueId();
            } else {
                Player opp = match.getOpponent(victim.getUniqueId());
                winnerId = (opp != null) ? opp.getUniqueId() : null;
            }

            match.end(winnerId, MatchEndReason.KILL);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (spectatorManager.isSpectating(player.getUniqueId())) {
            spectatorManager.removeSpectator(player);
        }
        if (matchManager.isInMatch(player.getUniqueId())) {
            matchManager.handleDisconnect(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        if (spectatorManager.isSpectating(player.getUniqueId())) {
            spectatorManager.removeSpectator(player);
        }
        if (matchManager.isInMatch(player.getUniqueId())) {
            matchManager.handleDisconnect(player);
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (matchManager.isInMatch(player.getUniqueId()) && event.getNewGameMode() != GameMode.SURVIVAL) {
            if (!player.hasPermission("loveduels.admin")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (matchManager.isInMatch(player.getUniqueId())) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND ||
                event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                var matchOpt = matchManager.getMatch(player.getUniqueId());
                if (matchOpt.isPresent() && !matchOpt.get().getArena().isInCombatBounds(event.getTo())) {
                    event.setCancelled(true);
                    Notify.actionBar(player, MiniMessage.miniMessage().deserialize("<red>Телепортация за пределы арены запрещена."));
                }
            }
        }
    }

    // Fighters get their full snapshot back on restore: anything dropped or stashed in a
    // container during the duel would be duplicated, so block it for everyone in a match.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFighterDrop(PlayerDropItemEvent event) {
        if (matchManager.isInMatch(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFighterPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p && matchManager.isInMatch(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFighterOpenContainer(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player p) || !matchManager.isInMatch(p.getUniqueId())) return;
        switch (event.getInventory().getType()) {
            case CHEST, ENDER_CHEST, SHULKER_BOX, BARREL, HOPPER, DROPPER, DISPENSER -> event.setCancelled(true);
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfig().getBoolean("spectators.block_drop", true)) return;
        if (spectatorManager.isSpectating(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorPickup(EntityPickupItemEvent event) {
        if (!plugin.getConfig().getBoolean("spectators.block_pickup", true)) return;
        if (event.getEntity() instanceof Player p && spectatorManager.isSpectating(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.getConfig().getBoolean("spectators.block_combat", true)
                && !plugin.getConfig().getBoolean("spectators.block_interact", true)) return;
        if (spectatorManager.isSpectating(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
