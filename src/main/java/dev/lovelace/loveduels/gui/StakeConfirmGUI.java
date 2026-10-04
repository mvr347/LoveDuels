package dev.lovelace.loveduels.gui;

import dev.lovelace.lovecore.api.economy.Denomination;
import dev.lovelace.lovecore.api.economy.LoveEconomy;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.core.DuelType;
import dev.lovelace.loveduels.match.StakeConfirmSession;
import dev.lovelace.loveduels.util.CoinFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Подтверждение ставок (не для тренировки).
 * Королевская: минимум 1 золотая монета.
 */
public final class StakeConfirmGUI extends CustomGUI {

    private final DuelManager duelManager;
    private final StakeConfirmSession session;
    private int selectedDenomIndex = 0;

    public StakeConfirmGUI(Player viewer, DuelManager duelManager, StakeConfirmSession session) {
        super(viewer, 27, dev.lovelace.loveduels.util.Lang.component(
                session.getRequest().royal()
                        ? "gui.stake_confirm.title_royal"
                        : "gui.stake_confirm.title_normal"
        ));
        this.duelManager = duelManager;
        this.session = session;
    }

    private long minMoney() {
        return session.getRequest().royal() ? CoinFormat.royalMinStake() : 0L;
    }

    @Override
    protected void build() {
        applyStandardBorders(false, null);

        Player p1 = Bukkit.getPlayer(session.getPlayer1());
        Player p2 = Bukkit.getPlayer(session.getPlayer2());
        DuelType type = session.getRequest().type();
        boolean royal = session.getRequest().royal();

        setItem(11, createStatusItem(p1, session.isReady(session.getPlayer1())), null);
        setItem(15, createStatusItem(p2, session.isReady(session.getPlayer2())), null);

        List<String> infoLore = dev.lovelace.loveduels.util.Lang.list(
                "gui.stake_confirm.params_lore",
                "type", type.getDisplayNameMiniMessage(),
                "honor", String.valueOf(session.getHonorBet())
        );
        if (royal) {
            infoLore.add(dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.params_royal_header"));
            infoLore.add(dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.params_royal_min", "min", CoinFormat.amount(minMoney())));
        }
        String paramsName = royal
                ? dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.params_name_royal")
                : dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.params_name_normal");
        setItem(4, HeadTextures.head(royal ? HeadTextures.CROWN : HeadTextures.SCROLL,
                paramsName,
                CoinFormat.resolveGlyphs(player, infoLore)
        ), null);

        setItem(13, createMoneyItem(royal), e -> {
            long min = minMoney();
            List<Denomination> dens = denominations();
            if (dens.isEmpty()) {
                long m = session.getMoneyBet();
                if (e.isRightClick()) session.setMoneyBet(Math.max(min, m - 1));
                else if (!e.isShiftClick()) session.setMoneyBet(m + 1);
            } else {
                if (selectedDenomIndex >= dens.size()) selectedDenomIndex = 0;
                if (e.isShiftClick()) {
                    selectedDenomIndex = (selectedDenomIndex + 1) % dens.size();
                    build();
                    return;
                }
                long unit = dens.get(selectedDenomIndex).value();
                long m = session.getMoneyBet();
                if (e.isRightClick()) session.setMoneyBet(Math.max(min, m - unit));
                else session.setMoneyBet(m + unit);
            }
            // clamp royal
            if (session.getMoneyBet() < min) session.setMoneyBet(min);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.1f);
        });

        boolean myReady = session.isReady(player.getUniqueId());
        setItem(22, createReadyButton(myReady, royal), e -> {
            long need = session.getMoneyBet();
            if (need > 0 && !duelManager.getEconomyBridge().has(player, need)) {
                dev.lovelace.loveduels.util.Lang.send(player, "gui.stake_confirm.err_no_money");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            if (royal && need < minMoney()) {
                dev.lovelace.loveduels.util.Lang.send(player, "gui.stake_confirm.err_min_royal", "min", CoinFormat.amount(minMoney()));
                return;
            }
            session.toggleReady(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        });

        setItem(size - 3, HeadTextures.head(HeadTextures.CANCEL,
                dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.cancel_name"),
                dev.lovelace.loveduels.util.Lang.list("gui.stake_confirm.cancel_lore")
        ), e -> session.cancel(player.getUniqueId()));
    }

    private ItemStack createStatusItem(Player target, boolean ready) {
        String name = target != null ? target.getName() : "Игрок";
        String status = ready
                ? dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.status_ready")
                : dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.status_not_ready");
        return HeadTextures.playerHead(target,
                dev.lovelace.loveduels.util.Lang.component("gui.stake_confirm.status_header", "name", name, "status", status),
                dev.lovelace.loveduels.util.Lang.componentList("gui.stake_confirm.status_lore")
        );
    }

    private ItemStack createMoneyItem(boolean royal) {
        List<String> lore = new ArrayList<>();
        if (royal) {
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.money_royal_header"));
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.money_royal_min", "min", CoinFormat.amount(minMoney())));
            lore.add("");
        }
        lore.add(dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.money_current"));
        Optional<LoveEconomy> eco = CoinFormat.tryEconomy();
        if (eco.isPresent()) {
            for (Component line : CoinFormat.formatGlyphLines(eco.get(), session.getMoneyBet())) {
                lore.add(MiniMessage.miniMessage().serialize(line));
            }
        } else {
            lore.add("<yellow>" + session.getMoneyBet() + "</yellow>");
        }
        lore.add("");
        List<Denomination> dens = denominations();
        if (!dens.isEmpty()) {
            if (selectedDenomIndex >= dens.size()) selectedDenomIndex = 0;
            Denomination sel = dens.get(selectedDenomIndex);
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.denom_line", "glyph", CoinFormat.getCoinGlyph(sel)));
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.controls_shift"));
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.controls_plus"));
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.controls_minus"));
        }
        lore.add("");
        lore.add(dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.reset_hint"));
        String name = royal
                ? dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.money_item_name_royal")
                : dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.money_item_name_normal");
        return HeadTextures.head(royal ? HeadTextures.CROWN : HeadTextures.COIN,
                name,
                CoinFormat.resolveGlyphs(player, lore));
    }

    private ItemStack createReadyButton(boolean ready, boolean royal) {
        String tex = ready ? HeadTextures.READY : HeadTextures.NOT_READY;
        String name = ready
                ? dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.ready_btn_done")
                : (royal
                    ? dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.ready_btn_royal")
                    : dev.lovelace.loveduels.util.Lang.get("gui.stake_confirm.ready_btn_normal"));
        return HeadTextures.head(tex, name, dev.lovelace.loveduels.util.Lang.list("gui.stake_confirm.ready_btn_lore"));
    }

    private List<Denomination> denominations() {
        Optional<LoveEconomy> eco = CoinFormat.tryEconomy();
        if (eco.isEmpty()) return List.of();
        List<Denomination> dens = new ArrayList<>(eco.get().denominations());
        dens.sort(Comparator.comparingLong(Denomination::value));
        dens.removeIf(d -> d.value() <= 0);
        return dens;
    }

    @Override
    public void handleClose() {
        if (session.isTerminated()) return;
        if (!session.isReady(session.getPlayer1()) || !session.isReady(session.getPlayer2())) {
            session.cancel(player.getUniqueId());
        }
    }
}
