package dev.lovelace.loveduels.royal;

import dev.lovelace.loveduels.integration.DiscordWebhookSender;
import dev.lovelace.loveduels.match.Match;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles server-wide Royal Duel lifecycle, herald announcements,
 * and single-active-match constraints.
 */
public final class RoyalDuelManager {

    private final Plugin plugin;
    private final RoyalTicketItem ticketItem;
    private final DiscordWebhookSender discordSender;
    private final AtomicReference<Match> activeRoyalMatch = new AtomicReference<>(null);

    public RoyalDuelManager(Plugin plugin, DiscordWebhookSender discordSender) {
        this.plugin = plugin;
        this.ticketItem = new RoyalTicketItem(plugin);
        this.discordSender = discordSender;
    }

    public RoyalTicketItem getTicketItem() {
        return ticketItem;
    }

    public boolean hasActiveRoyalDuel() {
        Match m = activeRoyalMatch.get();
        return m != null && !m.isEnded();
    }

    public Optional<Match> getActiveRoyalDuel() {
        Match m = activeRoyalMatch.get();
        if (m != null && !m.isEnded()) {
            return Optional.of(m);
        }
        return Optional.empty();
    }

    public boolean registerRoyalDuel(Match match) {
        if (hasActiveRoyalDuel()) {
            return false;
        }
        activeRoyalMatch.set(match);
        return true;
    }

    public void clearActiveRoyalDuel(Match match) {
        activeRoyalMatch.compareAndSet(match, null);
    }

    /**
     * Herald Announcement 1: Royal Duel challenged / created
     */
    public void broadcastCreation(Player challenger, Player target, long moneyBet, int honorBet) {
        String msg = String.format(
                "<gradient:#FFD700:#FFA500><b>👑 [КОРОЛЕВСКИЙ ГЛАШАТАЙ]</b></gradient>\n" +
                "<gold>Внимание всем жителям королевства! Рыцарь <white>%s</white> бросил вызов рыцарю <white>%s</white>!\n" +
                "<yellow>Ставка деньгами: <green>%d монет <dark_gray>| <yellow>Ставка Честью: <gold>%d",
                challenger.getName(), target.getName(), moneyBet, honorBet
        );
        Component comp = MiniMessage.miniMessage().deserialize(msg);
        Bukkit.broadcast(comp);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
        }

        if (discordSender != null) {
            discordSender.sendRoyalDuelCreated(challenger.getName(), target.getName(), moneyBet, honorBet);
        }
    }

    /**
     * Herald Announcement 2: Royal Duel starts
     */
    public void broadcastStart(Player p1, Player p2) {
        String msg = String.format(
                "<gradient:#FFD700:#FFA500><b>👑 [КОРОЛЕВСКИЙ ГЛАШАТАЙ]</b></gradient>\n" +
                "<gold>Королевский турнирный поединок между <white>%s</white> и <white>%s</white> начался на арене!\n" +
                "<yellow>Желающие могут наблюдать за боем: <aqua>/duel spectate %s",
                p1.getName(), p2.getName(), p1.getName()
        );
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg));

        Title title = Title.title(
                MiniMessage.miniMessage().deserialize("<gradient:#FFD700:#FFA500><b>КОРОЛЕВСКАЯ ДУЭЛЬ</b></gradient>"),
                MiniMessage.miniMessage().deserialize("<white>" + p1.getName() + " <gold>VS <white>" + p2.getName()),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))
        );

        p1.showTitle(title);
        p2.showTitle(title);
    }

    /**
     * Herald Announcement 3: Royal Duel ended with winner
     */
    public void broadcastWinner(Player winner, Player loser, long totalMoney, int honorWon) {
        String msg = String.format(
                "<gradient:#FFD700:#FFA500><b>👑 [КОРОЛЕВСКИЙ ГЛАШАТАЙ]</b></gradient>\n" +
                "<gold>Славная победа! Рыцарь <green><b>%s</b></green> сокрушил <red>%s</red> в Королевской Дуэли!\n" +
                "<yellow>Награда победителя: <green>+%d монет <dark_gray>| <yellow>Честь: <gold>+%d",
                winner.getName(), loser.getName(), totalMoney, honorWon
        );
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        if (discordSender != null) {
            discordSender.sendRoyalDuelEnded(winner.getName(), loser.getName(), totalMoney, honorWon);
        }
    }

    /**
     * Herald Announcement 4: Royal Duel ended with draw / timeout
     */
    public void broadcastTimeout(Player p1, Player p2, long burnedMoney) {
        String msg = String.format(
                "<gradient:#FFD700:#FFA500><b>👑 [КОРОЛЕВСКИЙ ГЛАШАТАЙ]</b></gradient>\n" +
                "<gold>Время поединка между <white>%s</white> и <white>%s</white> истекло!\n" +
                "<red><b>НИЧЬЯ!</b> Победитель не определён. Все ставки (%d монет) аннулированы и сгорели в казне!",
                p1.getName(), p2.getName(), burnedMoney
        );
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        }

        if (discordSender != null) {
            discordSender.sendRoyalDuelDraw(p1.getName(), p2.getName(), burnedMoney);
        }
    }
}
