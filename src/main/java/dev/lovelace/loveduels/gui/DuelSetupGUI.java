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
 * Настройка параметров дуэли:
 * 1) категория (ближний / дальний / всадники)
 * 2) подтип внутри категории
 * 3) ставка монетами (глифы, Shift — номинал, ЛКМ + / ПКМ −)
 * 4) ставка Честью (база, ЛКМ ×2, ПКМ ÷2)
 * 5) отправить запрос
 */
public final class DuelSetupGUI extends CustomGUI {

    private static final int BASE_HONOR = 10;

    private final Player opponent;
    private final DuelManager duelManager;
    private final ChallengeMode mode;

    private CombatCategory category = CombatCategory.MELEE;
    private DuelType selectedType = CombatCategory.MELEE.firstSubtype();
    private long moneyBet = 0L;
    private int honorBet = BASE_HONOR;
    private int selectedDenomIndex = 0;

    public DuelSetupGUI(Player player, Player opponent, DuelManager duelManager, ChallengeMode mode) {
        super(player, 45, MiniMessage.miniMessage().deserialize(titleFor(mode)));
        this.opponent = opponent;
        this.duelManager = duelManager;
        this.mode = mode;
        if (mode.isRoyal()) {
            this.moneyBet = 500L;
        }
        if (mode.isTraining()) {
            this.honorBet = 0;
            this.moneyBet = 0L;
        }
    }

    @Deprecated
    public DuelSetupGUI(Player player, Player opponent, DuelManager duelManager, boolean royal) {
        this(player, opponent, duelManager, royal ? ChallengeMode.ROYAL : ChallengeMode.NORMAL);
    }

    private static String titleFor(ChallengeMode mode) {
        return switch (mode) {
            case TRAINING -> "<aqua>Настройка · тренировка</aqua>";
            case ROYAL -> "<gradient:#C9A227:#E8D48B>Настройка · королевская</gradient>";
            default -> "<gold>Настройка · дуэль</gold>";
        };
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new PlayerSelectGUI(player, duelManager, mode).open());

        setItem(4, createOpponentInfoItem(), null);

        // Категория
        setItem(20, createCategoryItem(), e -> {
            category = e.isRightClick() ? category.prev() : category.next();
            selectedType = category.firstSubtype();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            build();
        });

        // Подтип
        setItem(21, createSubtypeItem(), e -> {
            selectedType = e.isRightClick()
                    ? category.prevSubtype(selectedType)
                    : category.nextSubtype(selectedType);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.15f);
            build();
        });

        boolean canBet = !mode.isTraining();

        if (canBet) {
            setItem(23, createMoneyBetItem(), e -> {
                List<Denomination> dens = denominations();
                if (dens.isEmpty()) {
                    // fallback без экономики
                    if (e.isShiftClick()) {
                        // no-op
                    } else if (e.isRightClick()) {
                        moneyBet = Math.max(mode.isRoyal() ? 500L : 0L, moneyBet - 1);
                    } else {
                        moneyBet += 1;
                    }
                } else {
                    if (selectedDenomIndex >= dens.size()) selectedDenomIndex = 0;
                    if (e.isShiftClick()) {
                        selectedDenomIndex = (selectedDenomIndex + 1) % dens.size();
                    } else {
                        long unit = dens.get(selectedDenomIndex).value();
                        if (e.isRightClick()) {
                            moneyBet = Math.max(0L, moneyBet - unit);
                            if (mode.isRoyal() && moneyBet < 500L) moneyBet = 500L;
                        } else {
                            moneyBet += unit;
                        }
                    }
                }
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
        } else {
            setItem(23, HeadTextures.head(HeadTextures.READY,
                    "<aqua>Тренировка</aqua>",
                    List.of("<gray>Без ставок и Чести.")
            ), null);
        }

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
            if (moneyBet <= 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Нужна денежная ставка."));
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

        Component acceptBtn = MiniMessage.miniMessage().deserialize("<green>[Принять]</green>")
                .clickEvent(ClickEvent.runCommand("/duel accept " + player.getName()))
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<green>Принять</green>")));
        Component denyBtn = MiniMessage.miniMessage().deserialize("<red>[Отклонить]</red>")
                .clickEvent(ClickEvent.runCommand("/duel deny " + player.getName()))
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<red>Отклонить</red>")));

        String prefix = royal
                ? "<gradient:#C9A227:#E8D48B>[Королевский вызов]</gradient>\n"
                : training
                    ? "<aqua>[Тренировочный вызов]</aqua>\n"
                    : "<gold>[Вызов на дуэль]</gold>\n";

        String moneyLine = training
                ? "<gray>Монеты: без ставок</gray>"
                : "<gray>Монеты: </gray>" + CoinFormat.formatBalanceLine(duelManager.getEconomyBridge(), effectiveMoney);
        String honorLine = training
                ? ""
                : "\n<gray>Честь: <yellow>" + effectiveHonor + "</yellow>";

        Component invite = MiniMessage.miniMessage().deserialize(
                prefix +
                "<white><gold>" + player.getName() + "</gold> вызывает вас.\n" +
                "<gray>Категория: <white>" + category.getDisplayName() + "</white>\n" +
                "<gray>Режим: </gray>" + selectedType.getDisplayNameMiniMessage() + "\n" +
                moneyLine + honorLine + "\n"
        ).append(acceptBtn).append(Component.text("  ")).append(denyBtn);

        opponent.sendMessage(invite);
        opponent.playSound(opponent.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

        if (royal) {
            duelManager.getRoyalManager().broadcastCreation(player, opponent, effectiveMoney, effectiveHonor);
        }
    }

    private List<Denomination> denominations() {
        Optional<LoveEconomy> eco = tryEconomy();
        if (eco.isEmpty()) return List.of();
        List<Denomination> dens = new ArrayList<>(eco.get().denominations());
        dens.sort(Comparator.comparingLong(Denomination::value)); // от мелкого к крупному для переключения
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
        return HeadTextures.playerHead(opponent,
                MiniMessage.miniMessage().deserialize("<gold>" + opponent.getName() + "</gold>"),
                List.of(
                        MiniMessage.miniMessage().deserialize("<gray>HP <red>" + (int) opponent.getHealth() + "</red> · пинг <white>" + opponent.getPing() + "</white>")
                )
        );
    }

    private ItemStack createCategoryItem() {
        String name = "<gold>Категория: <white>" + category.getDisplayName() + "</white></gold>";
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
            lore.add("<dark_gray>Варианты:</dark_gray>");
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
        lore.add("<gray>Текущая ставка:</gray>");

        Optional<LoveEconomy> eco = tryEconomy();
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
            lore.add("<gray>Номинал: </gray>" + CoinFormat.getCoinGlyph(sel) + " <white>×" + sel.value() + "</white>");
            lore.add("");
            lore.add("<yellow>Shift+клик</yellow> <dark_gray>—</dark_gray> <white>сменить номинал</white>");
            lore.add("<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>+1 номинал</white>");
            lore.add("<red>ПКМ</red> <dark_gray>—</dark_gray> <white>−1 номинал</white>");
        } else {
            lore.add("<yellow>ЛКМ</yellow> +1 · <red>ПКМ</red> −1");
        }

        String balance = CoinFormat.formatBalanceLine(duelManager.getEconomyBridge(),
                duelManager.getEconomyBridge().getBalance(player));
        lore.add("");
        lore.add("<gray>Баланс: </gray>" + balance);

        String name = "<gold>Ставка монетами</gold>";
        return HeadTextures.head(HeadTextures.COIN, name, lore);
    }

    private ItemStack createHonorBetItem() {
        String name = "<yellow>Честь: <white>" + honorBet + "</white></yellow>";
        List<String> lore = List.of(
                "<gray>База: " + BASE_HONOR + ". Победитель получает, проигравший теряет.",
                "",
                "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>×2</white>",
                "<red>ПКМ</red> <dark_gray>—</dark_gray> <white>÷2 (или база)</white>"
        );
        return HeadTextures.head(HeadTextures.STAR, name, lore);
    }

    private ItemStack createSendButton() {
        String name = mode.isRoyal()
                ? "<gradient:#C9A227:#E8D48B>Отправить вызов</gradient>"
                : mode.isTraining()
                    ? "<aqua>Отправить вызов</aqua>"
                    : "<green>Отправить вызов</green>";
        return HeadTextures.head(HeadTextures.SWORD, name, List.of(
                "<gray>Соперник увидит параметры и сможет принять."
        ));
    }
}
