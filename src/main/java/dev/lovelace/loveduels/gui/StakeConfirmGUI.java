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
        super(viewer, 27, MiniMessage.miniMessage().deserialize(
                session.getRequest().royal()
                        ? "<gradient:#FFD700:#C9A227>Королевское подтверждение</gradient>"
                        : "<gold>Подтверждение дуэли</gold>"
        ));
        this.duelManager = duelManager;
        this.session = session;
    }

    private long minMoney() {
        return session.getRequest().royal() ? CoinFormat.goldUnit() : 0L;
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

        List<String> infoLore = new ArrayList<>();
        infoLore.add("<gray>Режим: </gray>" + type.getDisplayNameMiniMessage());
        infoLore.add("<gray>Честь: <yellow>" + session.getHonorBet() + "</yellow>");
        if (royal) {
            infoLore.add("<gradient:#FFD700:#C9A227>Королевская дуэль</gradient>");
            infoLore.add("<gray>Минимум ставки: 1 золотая</gray>");
        }
        setItem(4, HeadTextures.head(royal ? HeadTextures.CROWN : HeadTextures.SCROLL,
                royal ? "<gradient:#FFD700:#C9A227>Параметры</gradient>" : "<gold>Параметры</gold>",
                infoLore
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
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Недостаточно средств для ставки."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            if (royal && need < minMoney()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Минимум: 1 золотая монета."));
                return;
            }
            session.toggleReady(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        });

        setItem(size - 3, HeadTextures.head(HeadTextures.CANCEL,
                "<red>Отменить</red>",
                List.of("<gray>Отказаться от дуэли")
        ), e -> session.cancel(player.getUniqueId()));
    }

    private ItemStack createStatusItem(Player target, boolean ready) {
        String name = target != null ? target.getName() : "Игрок";
        String status = ready ? "<green>Готов</green>" : "<red>Не готов</red>";
        return HeadTextures.playerHead(target,
                MiniMessage.miniMessage().deserialize("<gold>" + name + "</gold> · " + status),
                List.of(MiniMessage.miniMessage().deserialize("<gray>Оба должны подтвердить"))
        );
    }

    private ItemStack createMoneyItem(boolean royal) {
        List<String> lore = new ArrayList<>();
        if (royal) {
            lore.add("<gradient:#FFD700:#C9A227>Королевская ставка</gradient>");
            lore.add("<gray>Не ниже 1 золотой монеты</gray>");
            lore.add("");
        }
        lore.add("<gray>Ставка (на каждого):</gray>");
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
            lore.add("<gray>Номинал: </gray>" + CoinFormat.getCoinGlyph(sel));
            lore.add("<yellow>Shift</yellow> <dark_gray>—</dark_gray> <white>сменить номинал</white>");
            lore.add("<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>+ номинал</white>");
            lore.add("<red>ПКМ</red> <dark_gray>—</dark_gray> <white>− номинал</white>");
        }
        lore.add("");
        lore.add("<dark_gray>Смена ставки сбрасывает готовность</dark_gray>");
        return HeadTextures.head(royal ? HeadTextures.CROWN : HeadTextures.COIN,
                royal ? "<gradient:#FFD700:#C9A227>Ставка монетами</gradient>" : "<gold>Ставка монетами</gold>",
                CoinFormat.resolveGlyphs(player, lore));
    }

    private ItemStack createReadyButton(boolean ready, boolean royal) {
        String tex = ready ? HeadTextures.READY : HeadTextures.NOT_READY;
        String name = ready
                ? "<green>Вы готовы</green> <dark_gray>(клик — отменить)</dark_gray>"
                : (royal
                    ? "<gradient:#FFD700:#C9A227>Готов к королевскому поединку</gradient>"
                    : "<yellow>Нажмите: готов</yellow>");
        return HeadTextures.head(tex, name, List.of("<gray>Когда оба готовы — старт"));
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
