package dev.lovelace.loveduels.match;

import dev.lovelace.loveduels.core.DuelType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically sends a Russian ActionBar to participants and spectators.
 */
public final class DuelActionBarTask extends BukkitRunnable {

    private final Match match;

    public DuelActionBarTask(Match match) {
        this.match = match;
    }

    @Override
    public void run() {
        if (match.isEnded()) {
            cancel();
            return;
        }

        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();
        if (p1 == null || p2 == null || !p1.isOnline() || !p2.isOnline()) {
            return;
        }

        String p1Name = p1.getName();
        String p2Name = p2.getName();

        String mainBarContent;
        if (match.getType() == DuelType.HORSE_SPEAR) {
            int pts1 = match.getPoints(p1.getUniqueId());
            int pts2 = match.getPoints(p2.getUniqueId());
            mainBarContent = String.format(
                    "<gold>🐎 <white>%s <yellow>[%d] <gray>vs <white>%s <yellow>[%d] <dark_gray>| <yellow>Честь: <white>%d <dark_gray>| <green>Ставка: <white>%d",
                    p1Name, pts1, p2Name, pts2, match.getBet().honorBet(), match.getBet().moneyBet()
            );
        } else {
            int hp1 = (int) Math.ceil(p1.getHealth());
            int hp2 = (int) Math.ceil(p2.getHealth());
            mainBarContent = String.format(
                    "<gold>⚔ <white>%s <red>(%d❤) <gray>vs <white>%s <red>(%d❤) <dark_gray>| <yellow>Честь: <white>%d <dark_gray>| <green>Ставка: <white>%d",
                    p1Name, hp1, p2Name, hp2, match.getBet().honorBet(), match.getBet().moneyBet()
            );
        }

        if (match.isRoyal()) {
            mainBarContent = "<gradient:#FFD700:#FFA500>👑 [КОРОЛЕВСКАЯ ДУЭЛЬ] </gradient>" + mainBarContent;
        }

        Component component = MiniMessage.miniMessage().deserialize(mainBarContent);

        p1.sendActionBar(component);
        p2.sendActionBar(component);
        for (java.util.UUID specId : match.getSpectators()) {
            Player spec = org.bukkit.Bukkit.getPlayer(specId);
            if (spec != null && spec.isOnline()) {
                spec.sendActionBar(component);
            }
        }
    }
}
