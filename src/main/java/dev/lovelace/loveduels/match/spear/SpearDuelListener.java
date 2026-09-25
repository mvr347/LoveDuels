package dev.lovelace.loveduels.match.spear;

import dev.lovelace.loveduels.match.HorseSpearMatch;
import dev.lovelace.loveduels.match.Match;
import dev.lovelace.loveduels.match.MatchManager;
import dev.lovelace.loveduels.match.MatchState;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityDismountEvent;

public final class SpearDuelListener implements Listener {

    private final MatchManager matchManager;
    private final SpearItem spearItem;
    private final SpearChargeHandler chargeHandler;
    private final HorseStaminaHandler staminaHandler;

    public SpearDuelListener(
            MatchManager matchManager,
            SpearItem spearItem,
            SpearChargeHandler chargeHandler,
            HorseStaminaHandler staminaHandler
    ) {
        this.matchManager = matchManager;
        this.spearItem = spearItem;
        this.chargeHandler = chargeHandler;
        this.staminaHandler = staminaHandler;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        var matchOpt = matchManager.getMatch(player.getUniqueId());
        if (matchOpt.isEmpty()) return;

        Match m = matchOpt.get();
        if (!(m instanceof HorseSpearMatch match)) return;

        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!spearItem.isSpear(item)) return;

        if (match.getState() == MatchState.PREPARATION) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (!chargeHandler.isCharging(player)) {
                chargeHandler.startCharge(player);
                player.sendActionBar(MiniMessage.miniMessage().deserialize(
                        "<gold>🗡 Зарядка копья... <gray>(Нажмите ещё раз или ударьте для выпада)"
                ));
            } else {
                chargeHandler.stopCharge(player, match);
            }
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (chargeHandler.isCharging(player)) {
                event.setCancelled(true);
                chargeHandler.stopCharge(player, match);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Trident && event.getEntity().getShooter() instanceof Player player) {
            if (matchManager.isInMatch(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            var matchOpt = matchManager.getMatch(player.getUniqueId());
            if (matchOpt.isPresent() && matchOpt.get() instanceof HorseSpearMatch match) {
                event.setCancelled(true);
                match.handleDismount(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof Horse horse) {
            var matchOpt = matchManager.getMatch(player.getUniqueId());
            if (matchOpt.isPresent() && matchOpt.get() instanceof HorseSpearMatch match) {
                Horse myHorse = match.getHorse(player.getUniqueId());
                if (myHorse != null && myHorse.equals(horse)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onHorseDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Horse horse) {
            for (Match match : matchManager.getActiveMatches()) {
                if (match instanceof HorseSpearMatch hsm && hsm.isTournamentHorse(horse)) {
                    event.getDrops().clear();
                    event.setDroppedExp(0);
                    hsm.handleHorseDeath(horse);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && player.getVehicle() instanceof Horse horse) {
            var matchOpt = matchManager.getMatch(player.getUniqueId());
            if (matchOpt.isPresent() && matchOpt.get() instanceof HorseSpearMatch) {
                staminaHandler.performDash(player, horse);
            }
        }
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        var matchOpt = matchManager.getMatch(player.getUniqueId());
        if (matchOpt.isPresent() && matchOpt.get() instanceof HorseSpearMatch) {
            // Keep spear held
            if (event.getNewSlot() != 0) {
                event.setCancelled(true);
            }
        }
    }
}
