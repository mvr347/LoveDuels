package dev.lovelace.loveduels.gui;

import dev.lovelace.lovecore.api.economy.Denomination;
import dev.lovelace.lovecore.api.economy.LoveEconomy;
import dev.lovelace.loveduels.core.ChallengeMode;
import dev.lovelace.loveduels.core.CombatCategory;
import dev.lovelace.loveduels.core.DuelBet;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.core.DuelRequest;
import dev.lovelace.loveduels.core.DuelType;
import dev.lovelace.loveduels.util.CoinFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Настройка дуэли.
 * Тренировка — только категория/режим, без ставок.
 * Королевская — минимум 1 золотая монета, торжественное оформление.
 */
public final class DuelSetupGUI extends CustomGUI {

    private static final int BASE_HONOR = 10;

    private final Player opponent;
    private final DuelManager duelManager;
    private final ChallengeMode mode;

    private CombatCategory category = CombatCategory.MELEE;
    private DuelType selectedType = CombatCategory.MELEE.firstSubtype();
    private long moneyBet;
    private int honorBet;
    private int selectedDenomIndex = 0;

    public DuelSetupGUI(Player player, Player opponent, DuelManager duelManager, ChallengeMode mode) {
        super(player, 45, MiniMessage.miniMessage().deserialize(titleFor(mode)));
        this.opponent = opponent;
        this.duelManager = duelManager;
        this.mode = mode;
        if (mode.isTraining()) {
            this.moneyBet = 0L;
            this.honorBet = 0;
        } else if (mode.isRoyal()) {
            this.moneyBet = minRoyalBet();
            this.honorBet = BASE_HONOR;
        } else {
            this.moneyBet = 0L;
            this.honorBet = BASE_HONOR;
        }
    }

    @Deprecated
    public DuelSetupGUI(Player player, Player opponent, DuelManager duelManager, boolean royal) {
        this(player, opponent, duelManager, royal ? ChallengeMode.ROYAL : ChallengeMode.NORMAL);
    }

    private static String titleFor(ChallengeMode mode) {
        return switch (mode) {
            case TRAINING -> "<aqua>Тренировка</aqua>";
            case ROYAL -> "<gradient:#FFD700:#F5E6A3:#C9A227>Королевская дуэль</gradient>";
            default -> "<gold>Настройка дуэли</gold>";
        };
    }

    private long minRoyalBet() {
        return CoinFormat.goldUnit();
    }

    private void clampRoyalMoney() {
        if (mode.isRoyal()) {
            long min = minRoyalBet();
            if (moneyBet < min) moneyBet = min;
        }
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new PlayerSelectGUI(player, duelManager, mode).open());

        setItem(4, createOpponentInfoItem(), null);

        setItem(20, createCategoryItem(), e -> {
            category = e.isRightClick() ? category.prev() : category.next();
            selectedType = category.firstSubtype();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            build();
        });

        setItem(21, createSubtypeItem(), e -> {
            selectedType = e.isRightClick()
                    ? category.prevSubtype(selectedType)
                    : category.nextSubtype(selectedType);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.15f);
            build();
        });

        if (mode.isTraining()) {
            // только режим + отправить — без чести и монет
            setItem(31, createSendButton(), e -> sendChallenge());
            return;
        }

        setItem(23, createMoneyBetItem(), e -> {
            List<Denomination> dens = denominations();
            long min = mode.isRoyal() ? minRoyalBet() : 0L;
            if (dens.isEmpty()) {
                if (e.isRightClick()) {
                    moneyBet = Math.max(min, moneyBet - 1);
                } else if (!e.isShiftClick()) {
                    moneyBet += 1;
                }
            } else {
                if (selectedDenomIndex >= dens.size()) selectedDenomIndex = 0;
                if (e.isShiftClick()) {
                    selectedDenomIndex = (selectedDenomIndex + 1) % dens.size();
                } else {
                    long unit = dens.get(selectedDenomIndex).value();
                    if (e.isRightClick()) {
                        moneyBet = Math.max(min, moneyBet - unit);
                    } else {
                        moneyBet += unit;
                    }
                }
            }
            clampRoyalMoney();
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
            build();
        });

        setItem(24, createHonorBetItem(), e -> {
            if (e.isRightClick()) {
                if (honorBet > BASE_HONOR && honorBet % 2 == 0) {
                    honorBet = honorBet / 2;
                } else {
                    honorBet = BASE_HONOR;
                }
            } else {
                honorBet = Math.max(BASE_HONOR, honorBet * 2);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
            build();
        });

        setItem(31, createSendButton(), e -> sendChallenge());
    }

    private void sendChallenge() {
        if (!opponent.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Игрок покинул сервер."));
            player.closeInventory();
            return;
        }
        if (duelManager.getMatchManager().isInMatch(opponent.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Этот игрок уже в бою."));
            return;
        }

        long cd = duelManager.getCooldownManager().getChallengeRemainingSeconds(player.getUniqueId(), opponent.getUniqueId());
        if (cd > 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Кулдаун вызова: <gold>" + cd + "с</gold>."
            ));
            return;
        }

        boolean royal = mode.isRoyal();
        boolean training = mode.isTraining();

        if (royal) {
            if (duelManager.getMatchManager().hasActiveRoyalDuel()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Уже идёт королевская дуэль."));
                return;
            }
            long royalCd = duelManager.getCooldownManager().getRoyalTicketRemainingSeconds(player.getUniqueId());
            if (royalCd > 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Кулдаун билета: <gold>" +
                        dev.lovelace.loveduels.core.CooldownManager.formatDuration(royalCd) + "</gold>."
                ));
                return;
            }
            if (!hasRoyalTicket(player)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Нет билета королевской дуэли."));
                return;
            }
            clampRoyalMoney();
            if (moneyBet < minRoyalBet()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Минимум для королевской: 1 золотая монета."
                ));
                return;
            }
        }

        long effectiveMoney = training ? 0L : moneyBet;
        int effectiveHonor = training ? 0 : honorBet;

        if (royal) {
            consumeRoyalTicket(player);
            duelManager.getCooldownManager().setRoyalTicketCooldown(player.getUniqueId(), 3600);
        }

        duelManager.getCooldownManager().setChallengeCooldown(player.getUniqueId(), opponent.getUniqueId(), 600);

        DuelBet bet = new DuelBet(effectiveMoney, effectiveHonor);
        DuelRequest req = DuelRequest.of(
                player.getUniqueId(),
                opponent.getUniqueId(),
                selectedType,
                selectedType == DuelType.KIT ? "default" : null,
                bet,
                royal,
                training,
                90_000L
        );

        duelManager.getMatchManager().sendRequest(req);
        player.closeInventory();

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>Вызов отправлен <gold>" + opponent.getName() + "</gold>."
        ));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        Component acceptBtn = MiniMessage.miniMessage().deserialize(
                        royal ? "<gradient:#FFD700:#C9A227>[Принять]</gradient>" : "<green>[Принять]</green>")
                .clickEvent(ClickEvent.runCommand("/duel accept " + player.getName()))
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<green>Принять</green>")));
        Component denyBtn = MiniMessage.miniMessage().deserialize("<red>[Отклонить]</red>")
                .clickEvent(ClickEvent.runCommand("/duel deny " + player.getName()))
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<red>Отклонить</red>")));

        String prefix = royal
                ? "<gradient:#FFD700:#F5E6A3:#C9A227>══ Королевский вызов ══</gradient>\n"
                : training
                    ? "<aqua>[Тренировочный вызов]</aqua>\n"
                    : "<gold>[Вызов на дуэль]</gold>\n";

        String moneyLine;
        String honorLine;
        if (training) {
            moneyLine = "<gray>Без ставок</gray>";
            honorLine = "";
        } else {
            moneyLine = "<gray>Монеты: </gray>" + CoinFormat.formatBalanceLine(duelManager.getEconomyBridge(), effectiveMoney);
            honorLine = "\n<gray>Честь: <yellow>" + effectiveHonor + "</yellow>";
        }

        moneyLine = CoinFormat.resolveGlyphs(opponent, moneyLine);
        Component invite = MiniMessage.miniMessage().deserialize(
                prefix +
                "<white><gold>" + player.getName() + "</gold> вызывает вас.\n" +
                "<gray>Категория: <white>" + category.getDisplayName() + "</white>\n" +
                "<gray>Режим: </gray>" + selectedType.getDisplayNameMiniMessage() + "\n" +
                moneyLine + honorLine + "\n"
        ).append(acceptBtn).append(Component.text("  ")).append(denyBtn);

        opponent.sendMessage(invite);
        opponent.playSound(opponent.getLocation(),
                royal ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

        if (royal) {
            duelManager.getRoyalManager().broadcastCreation(player, opponent, effectiveMoney, effectiveHonor);
        }
    }

    private List<Denomination> denominations() {
        Optional<LoveEconomy> eco = CoinFormat.tryEconomy();
        if (eco.isEmpty()) return List.of();
        List<Denomination> dens = new ArrayList<>(eco.get().denominations());
        dens.sort(Comparator.comparingLong(Denomination::value));
        dens.removeIf(d -> d.value() <= 0);
        return dens;
    }

    private boolean hasRoyalTicket(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (duelManager.getRoyalManager().getTicketItem().isTicket(item)) return true;
        }
        return false;
    }

    private void consumeRoyalTicket(Player p) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (duelManager.getRoyalManager().getTicketItem().isTicket(item)) {
                item.setAmount(item.getAmount() - 1);
                p.getInventory().setItem(i, item.getAmount() > 0 ? item : null);
                return;
            }
        }
    }

    private ItemStack createOpponentInfoItem() {
        String name = mode.isRoyal()
                ? "<gradient:#FFD700:#C9A227>" + opponent.getName() + "</gradient>"
                : "<gold>" + opponent.getName() + "</gold>";
        return HeadTextures.playerHead(opponent,
                MiniMessage.miniMessage().deserialize(name),
                List.of(
                        MiniMessage.miniMessage().deserialize(
                                "<gray>HP <red>" + (int) opponent.getHealth() + "</red> · пинг <white>" + opponent.getPing() + "</white>")
                )
        );
    }

    private ItemStack createCategoryItem() {
        String name = mode.isRoyal()
                ? "<gradient:#FFD700:#C9A227>Категория: " + category.getDisplayName() + "</gradient>"
                : "<gold>Категория: <white>" + category.getDisplayName() + "</white></gold>";
        List<String> lore = List.of(
                "<gray>" + category.getDescription(),
                "",
                "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>следующая</white>",
                "<yellow>ПКМ</yellow> <dark_gray>—</dark_gray> <white>предыдущая</white>"
        );
        String tex = switch (category) {
            case MELEE -> HeadTextures.SWORD;
            case RANGED -> HeadTextures.BOW;
            case MOUNTED -> HeadTextures.HORSE;
        };
        return HeadTextures.head(tex, name, lore);
    }

    private ItemStack createSubtypeItem() {
        String name = "<aqua>Режим: </aqua>" + selectedType.getDisplayNameMiniMessage();
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + selectedType.getDescription());
        lore.add("");
        if (category.getSubtypes().size() > 1) {
            lore.add("<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>следующий</white>");
            lore.add("<yellow>ПКМ</yellow> <dark_gray>—</dark_gray> <white>предыдущий</white>");
            lore.add("");
            for (DuelType t : category.getSubtypes()) {
                String mark = t == selectedType ? "<green>●</green> " : "<dark_gray>○</dark_gray> ";
                lore.add(mark + t.getDisplayNameMiniMessage());
            }
        } else {
            lore.add("<dark_gray>Единственный вариант</dark_gray>");
        }
        String tex = switch (selectedType) {
            case FISTS -> HeadTextures.SKULL;
            case KIT, SWORD -> HeadTextures.SWORD;
            case OWN_INVENTORY -> HeadTextures.BACKPACK;
            case BOW, CROSSBOW -> HeadTextures.BOW;
            case HORSE_SPEAR -> HeadTextures.HORSE;
            default -> HeadTextures.SWORD;
        };
        return HeadTextures.head(tex, name, lore);
    }

    private ItemStack createMoneyBetItem() {
        List<String> lore = new ArrayList<>();
        if (mode.isRoyal()) {
            lore.add("<gradient:#FFD700:#C9A227>Королевская ставка</gradient>");
            lore.add("<gray>Минимум: 1 золотая монета</gray>");
            lore.add("");
        }
        lore.add("<gray>Текущая ставка:</gray>");

        Optional<LoveEconomy> eco = CoinFormat.tryEconomy();
        if (eco.isPresent()) {
            for (Component line : CoinFormat.formatGlyphLines(eco.get(), moneyBet)) {
                lore.add(MiniMessage.miniMessage().serialize(line));
            }
        } else {
            lore.add("<yellow>" + moneyBet + "</yellow>");
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

        String balance = CoinFormat.formatBalanceLine(duelManager.getEconomyBridge(),
                duelManager.getEconomyBridge().getBalance(player));
        lore.add("");
        lore.add("<gray>Баланс: </gray>" + balance);

        String name = mode.isRoyal()
                ? "<gradient:#FFD700:#C9A227>Ставка монетами</gradient>"
                : "<gold>Ставка монетами</gold>";
        return HeadTextures.head(mode.isRoyal() ? HeadTextures.CROWN : HeadTextures.COIN, name,
                CoinFormat.resolveGlyphs(player, lore));
    }

    private ItemStack createHonorBetItem() {
        String name = mode.isRoyal()
                ? "<gradient:#FFD700:#C9A227>Честь: " + honorBet + "</gradient>"
                : "<yellow>Честь: <white>" + honorBet + "</white></yellow>";
        List<String> lore = List.of(
                "<gray>База: " + BASE_HONOR + ". Победитель получает, проигравший теряет.",
                mode.isRoyal() ? "<gray>В королевской дуэли Честь выше ценится.</gray>" : "",
                "",
                "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>×2</white>",
                "<red>ПКМ</red> <dark_gray>—</dark_gray> <white>÷2 (или база)</white>"
        );
        return HeadTextures.head(HeadTextures.STAR, name, lore);
    }

    private ItemStack createSendButton() {
        String name = mode.isRoyal()
                ? "<gradient:#FFD700:#F5E6A3:#C9A227>Отправить королевский вызов</gradient>"
                : mode.isTraining()
                    ? "<aqua>Отправить вызов</aqua>"
                    : "<green>Отправить вызов</green>";
        List<String> lore = mode.isTraining()
                ? List.of("<gray>Без ставок. Соперник примет — и сразу бой.")
                : List.of("<gray>Соперник увидит параметры и сможет принять.");
        return HeadTextures.head(mode.isRoyal() ? HeadTextures.CROWN : HeadTextures.SWORD, name, lore);
    }
}
