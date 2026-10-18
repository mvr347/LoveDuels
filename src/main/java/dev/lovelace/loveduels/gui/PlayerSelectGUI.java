package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.ChallengeMode;
import dev.lovelace.loveduels.core.DuelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class PlayerSelectGUI extends CustomGUI {

    private final DuelManager duelManager;
    private final ChallengeMode mode;
    private int page = 0;

    static final int PAGE_SIZE = 21;
    static final int PREV_SLOT = 36;
    static final int NEXT_SLOT = 44;

    private static Component titleFor(ChallengeMode mode) {
        return switch (mode) {
            case TRAINING -> dev.lovelace.loveduels.util.Lang.component("gui.player_select.title_training");
            case ROYAL -> dev.lovelace.loveduels.util.Lang.component("gui.player_select.title_royal");
            default -> dev.lovelace.loveduels.util.Lang.component("gui.player_select.title_normal");
        };
    }

    public PlayerSelectGUI(Player player, DuelManager duelManager, ChallengeMode mode) {
        super(player, 54, titleFor(mode));
        this.duelManager = duelManager;
        this.mode = mode;
    }

    /** @deprecated use ChallengeMode overload */
    @Deprecated
    public PlayerSelectGUI(Player player, DuelManager duelManager, boolean royal) {
        this(player, duelManager, royal ? ChallengeMode.ROYAL : ChallengeMode.NORMAL);
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new ChallengeModeGUI(player, duelManager).open());

        // Rebuilt on every page flip: drop stale content, handlers and arrows first
        int[] validSlots = new int[]{
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
        for (int slot : validSlots) setItem(slot, null, null);
        setItem(PREV_SLOT, null, null);
        setItem(NEXT_SLOT, null, null);

        List<? extends Player> opponents = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(player))
                .filter(p -> !duelManager.getMatchManager().isInMatch(p.getUniqueId()))
                .toList();

        if (opponents.isEmpty()) {
            setItem(22, createNoPlayersItem(), null);
            return;
        }

        page = PageMath.clampPage(page, opponents.size(), PAGE_SIZE);
        int from = PageMath.fromIndex(page, PAGE_SIZE);
        int to = PageMath.toIndex(page, opponents.size(), PAGE_SIZE);

        for (int i = from; i < to; i++) {
            Player opp = opponents.get(i);
            int slot = validSlots[i - from];
            setItem(slot, createOpponentHead(opp), e ->
                    new DuelSetupGUI(player, opp, duelManager, mode).open());
        }

        // Arrows exist only when there is somewhere to go; otherwise the slot stays empty
        if (page > 0) {
            setItem(PREV_SLOT, createArrow(HeadTextures.PREVIOUS, "gui.common.previous"), e -> {
                page--;
                refresh();
            });
        }
        if (page < PageMath.pageCount(opponents.size(), PAGE_SIZE) - 1) {
            setItem(NEXT_SLOT, createArrow(HeadTextures.NEXT, "gui.common.next"), e -> {
                page++;
                refresh();
            });
        }
    }

    private ItemStack createArrow(String texture, String langKey) {
        return HeadTextures.head(
                texture,
                dev.lovelace.loveduels.util.Lang.get(langKey + ".name"),
                dev.lovelace.loveduels.util.Lang.list(langKey + ".lore")
        );
    }

    private ItemStack createOpponentHead(Player opp) {
        Component name = dev.lovelace.loveduels.util.Lang.component("gui.player_select.player_card.name", "name", opp.getName());

        long cd = duelManager.getCooldownManager().getChallengeRemainingSeconds(player.getUniqueId(), opp.getUniqueId());
        Component cdComp = cd > 0
                ? dev.lovelace.loveduels.util.Lang.component("gui.player_select.player_card.cooldown", "cd", String.valueOf(cd))
                : dev.lovelace.loveduels.util.Lang.component("gui.player_select.player_card.ready");

        List<Component> lore = List.of(
                dev.lovelace.loveduels.util.Lang.component("gui.player_select.player_card.health", "health", String.valueOf((int) opp.getHealth())),
                dev.lovelace.loveduels.util.Lang.component("gui.player_select.player_card.ping", "ping", String.valueOf(opp.getPing())),
                MiniMessage.miniMessage().deserialize("<dark_gray><st>─────────────────────</st></dark_gray>").decoration(TextDecoration.ITALIC, false),
                dev.lovelace.loveduels.util.Lang.component("gui.player_select.player_card.click_hint")
        );

        return HeadTextures.playerHead(opp, name, lore);
    }

    private ItemStack createNoPlayersItem() {
        return HeadTextures.head(
                HeadTextures.EYE,
                dev.lovelace.loveduels.util.Lang.get("gui.player_select.no_players.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.player_select.no_players.lore")
        );
    }
}
