package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.util.CoinFormat;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.match.Match;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SpectateListGUI extends CustomGUI {

    private final DuelManager duelManager;

    public SpectateListGUI(Player player, DuelManager duelManager) {
        super(player, 45, dev.lovelace.loveduels.util.Lang.component("gui.spectate_list.title"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        Collection<Match> matches = duelManager.getMatchManager().getActiveMatches();
        int[] slots = new int[]{20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

        if (matches.isEmpty()) {
            setItem(22, createNoMatchesItem(), null);
            return;
        }

        int idx = 0;
        for (Match match : matches) {
            if (idx >= slots.length) break;
            int slot = slots[idx++];

            setItem(slot, createMatchItem(match), e -> {
                player.closeInventory();
                duelManager.getSpectatorManager().addSpectator(player, match);
            });
        }
    }

    private ItemStack createMatchItem(Match match) {
        String p1 = match.getPlayer1().getName();
        String p2 = match.getPlayer2().getName();
        String prefix = match.isRoyal()
                ? dev.lovelace.loveduels.util.Lang.get("gui.spectate_list.match_card_prefix_royal")
                : dev.lovelace.loveduels.util.Lang.get("gui.spectate_list.match_card_prefix_normal");

        Component nameComp = dev.lovelace.loveduels.util.Lang.component(
                "gui.spectate_list.match_card_title",
                "prefix", prefix,
                "p1", p1,
                "p2", p2
        );

        long fee = duelManager.getSpectatorManager().calculateSpectatorFee(match);
        String feeStr = (fee <= 0)
                ? dev.lovelace.loveduels.util.Lang.get("gui.spectate_list.ticket_free")
                : CoinFormat.amount(fee);

        List<String> rawLore = dev.lovelace.loveduels.util.Lang.list(
                "gui.spectate_list.match_card_lore",
                "type", match.getType().getDisplayNameMiniMessage(),
                "arena", match.getArena().getName(),
                "spectators", String.valueOf(match.getSpectators().size()),
                "fee", feeStr
        );

        List<Component> lore = new ArrayList<>();
        for (String line : CoinFormat.resolveGlyphs(player, rawLore)) {
            if (line.isEmpty()) {
                lore.add(Component.empty());
            } else {
                lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(line)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
        }

        return HeadTextures.playerHead(match.getPlayer1().getUniqueId(), nameComp, lore);
    }

    private ItemStack createNoMatchesItem() {
        return HeadTextures.head(
                HeadTextures.EYE,
                dev.lovelace.loveduels.util.Lang.get("gui.spectate_list.no_matches_name"),
                dev.lovelace.loveduels.util.Lang.list("gui.spectate_list.no_matches_lore")
        );
    }
}
