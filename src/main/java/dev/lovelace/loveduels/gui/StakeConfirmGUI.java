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
 * Оба бойца видят параметры, правят денежную ставку (если не тренировка),
 * нажимают «Готов» или «Отмена». При готовности обоих — старт.
 */
public final class StakeConfirmGUI extends CustomGUI {

    private final DuelManager duelManager;
    private final StakeConfirmSession session;
    private int selectedDenomIndex = 0;

    public StakeConfirmGUI(Player viewer, DuelManager duelManager, StakeConfirmSession session) {
        super(viewer, 27, MiniMessage.miniMessage().deserialize("<gold>Подтверждение дуэли</gold>"));
        this.duelManager = duelManager;
        this.session = session;
    }

    @Override
    protected void build() {
        applyStandardBorders(false, null);

        Player p1 = Bukkit.getPlayer(session.getPlayer1());
        Player p2 = Bukkit.getPlayer(session.getPlayer2());
        DuelType type = session.getRequest().type();

        setItem(11, createStatusItem(p1, session.isReady(session.getPlayer1())), null);
        setItem(15, createStatusItem(p2, session.isReady(session.getPlayer2())), null);

        // Инфо о режиме
        setItem(4, HeadTextures.head(HeadTextures.SCROLL,
                "<gold>Параметры</gold>",
                List.of(
                        "<gray>Режим: </gray>" + type.getDisplayNameMiniMessage(),
                        session.isTraining()
                                ? "<aqua>Тренировка — без ставок</aqua>"
                                : "<gray>Честь: <yellow>" + session.getHonorBet() + "</yellow>",
                        session.getRequest().royal()
                                ? "<gradient:#C9A227:#E8D48B>Королевская дуэль</gradient>"
                                : ""
                )
        ), null);

        if (!session.isTraining()) {
            setItem(13, createMoneyItem(), e -> {
                List<Denomination> dens = denominations();
                if (dens.isEmpty()) {
                    long m = session.getMoneyBet();
                    if (e.isRightClick()) session.setMoneyBet(Math.max(0, m - 1));
                    else session.setMoneyBet(m + 1);
                } else {
                    if (selectedDenomIndex >= dens.size()) selectedDenomIndex = 0;
                    if (e.isShiftClick()) {
                        selectedDenomIndex = (selectedDenomIndex + 1) % dens.size();
                        build();
                        return;
                    }
                    long unit = dens.get(selectedDenomIndex).value();
                    long m = session.getMoneyBet();
                    if (e.isRightClick()) session.setMoneyBet(Math.max(0, m - unit));
                    else session.setMoneyBet(m + unit);
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.1f);
            });
        } else {
            setItem(13, HeadTextures.head(HeadTextures.READY,
                    "<aqua>Без ставок</aqua>",
                    List.of("<gray>Тренировочный бой")
            ), null);
        }

        boolean myReady = session.isReady(player.getUniqueId());
        setItem(22, createReadyButton(myReady), e -> {
            if (!session.isTraining() && session.getMoneyBet() > 0) {
                if (!duelManager.getEconomyBridge().has(player, session.getMoneyBet())) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<red>Недостаточно средств для ставки."
                    ));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
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

    private ItemStack createMoneyItem() {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Общая ставка (на каждого):</gray>");
        Optional<LoveEconomy> eco = tryEconomy();
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
            lore.add("<yellow>Shift</yellow> — сменить · <yellow>ЛКМ</yellow> + · <red>ПКМ</red> −");
        }
        lore.add("");
        lore.add("<dark_gray>Смена ставки сбрасывает готовность</dark_gray>");
        return HeadTextures.head(HeadTextures.COIN, "<gold>Ставка монетами</gold>", lore);
    }

    private ItemStack createReadyButton(boolean ready) {
        String tex = ready ? HeadTextures.READY : HeadTextures.NOT_READY;
        String name = ready
                ? "<green>Вы готовы</green> <dark_gray>(клик — отменить)</dark_gray>"
                : "<yellow>Нажмите: готов</yellow>";
        return HeadTextures.head(tex, name, List.of("<gray>Когда оба готовы — старт"));
    }

    private List<Denomination> denominations() {
        Optional<LoveEconomy> eco = tryEconomy();
        if (eco.isEmpty()) return List.of();
        List<Denomination> dens = new ArrayList<>(eco.get().denominations());
        dens.sort(Comparator.comparingLong(Denomination::value));
        dens.removeIf(d -> d.value() <= 0);
        return dens;
    }

    private static Optional<LoveEconomy> tryEconomy() {
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LoveCore")) return Optional.empty();
        try {
            return dev.lovelace.lovecore.api.LoveCore.service(LoveEconomy.class);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    @Override
    public void handleClose() {
        if (session.isTerminated()) return;
        // закрытие окна = отмена, если ещё не оба готовы
        if (!session.isReady(session.getPlayer1()) || !session.isReady(session.getPlayer2())) {
            session.cancel(player.getUniqueId());
        }
    }
}
