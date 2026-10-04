package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class RoyalDuelGUI extends CustomGUI {

    private final DuelManager duelManager;

    public RoyalDuelGUI(Player player, DuelManager duelManager) {
        super(player, 45, dev.lovelace.loveduels.util.Lang.component("gui.royal_duel.title"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        boolean hasTicket = checkTicket(player);
        boolean serverActive = duelManager.getMatchManager().hasActiveRoyalDuel();
        long cd = duelManager.getCooldownManager().getRoyalTicketRemainingSeconds(player.getUniqueId());

        // Overview Scroll (Slot 20)
        setItem(20, createOverviewItem(), null);

        // Status Item (Slot 22)
        setItem(22, createStatusItem(hasTicket, serverActive, cd), null);

        // Challenge Button (Slot 24)
        setItem(24, createChallengeButtonItem(hasTicket, serverActive, cd), e -> {
            if (!hasTicket) {
                dev.lovelace.loveduels.util.Lang.send(player, "gui.royal_duel.err_no_ticket");
                return;
            }
            if (serverActive) {
                dev.lovelace.loveduels.util.Lang.send(player, "gui.royal_duel.err_active_duel");
                return;
            }
            if (cd > 0) {
                dev.lovelace.loveduels.util.Lang.send(player, "gui.royal_duel.err_cd", "cd", String.valueOf(cd));
                return;
            }
            new PlayerSelectGUI(player, duelManager, true).open();
        });
    }

    private boolean checkTicket(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (duelManager.getRoyalManager().getTicketItem().isTicket(item)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack createOverviewItem() {
        String name = dev.lovelace.loveduels.util.Lang.get("gui.royal_duel.rules_name");
        List<String> lore = dev.lovelace.loveduels.util.Lang.list("gui.royal_duel.rules_lore");
        return HeadTextures.head(HeadTextures.SCROLL, name, lore);
    }

    private ItemStack createStatusItem(boolean hasTicket, boolean serverActive, long cd) {
        boolean can = hasTicket && !serverActive && cd <= 0;
        String ticketStr = hasTicket
                ? dev.lovelace.loveduels.util.Lang.get("gui.royal_duel.ticket_yes")
                : dev.lovelace.loveduels.util.Lang.get("gui.royal_duel.ticket_no");
        String arenaStr = serverActive
                ? dev.lovelace.loveduels.util.Lang.get("gui.royal_duel.arena_busy")
                : dev.lovelace.loveduels.util.Lang.get("gui.royal_duel.arena_free");
        String cdStr = cd <= 0
                ? dev.lovelace.loveduels.util.Lang.get("gui.royal_duel.cd_ready")
                : dev.lovelace.loveduels.util.Lang.get("gui.royal_duel.cd_wait", "cd", String.valueOf(cd));

        String name = dev.lovelace.loveduels.util.Lang.get("gui.royal_duel.status_name");
        List<String> lore = dev.lovelace.loveduels.util.Lang.list(
                "gui.royal_duel.status_lore",
                "ticket", ticketStr,
                "arena", arenaStr,
                "cd", cdStr
        );

        return HeadTextures.head(can ? HeadTextures.READY : HeadTextures.NOT_READY, name, lore);
    }

    private ItemStack createChallengeButtonItem(boolean hasTicket, boolean serverActive, long cd) {
        boolean can = hasTicket && !serverActive && cd <= 0;
        String name = can
                ? dev.lovelace.loveduels.util.Lang.get("gui.royal_duel.btn_available")
                : dev.lovelace.loveduels.util.Lang.get("gui.royal_duel.btn_unavailable");
        List<String> lore = can
                ? dev.lovelace.loveduels.util.Lang.list("gui.royal_duel.btn_lore_available")
                : dev.lovelace.loveduels.util.Lang.list("gui.royal_duel.btn_lore_unavailable");
        return HeadTextures.head(can ? HeadTextures.CROWN : HeadTextures.CANCEL, name, lore);
    }
}
