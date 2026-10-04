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

    private static Component titleFor(ChallengeMode mode) {
        return switch (mode) {
            case TRAINING -> dev.lovelace.loveduels.util.Lang.component("gui.duel_setup.title_training");
            case ROYAL -> dev.lovelace.loveduels.util.Lang.component("gui.duel_setup.title_royal");
            default -> dev.lovelace.loveduels.util.Lang.component("gui.duel_setup.title_normal");
        };
    }

    public DuelSetupGUI(Player player, Player opponent, DuelManager duelManager, ChallengeMode mode) {
        super(player, 45, titleFor(mode));
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

    private long minRoyalBet() {
        return CoinFormat.royalMinStake();
    }

    private void clampRoyalMoney() {
        if (mode.isRoyal()) {
            long min = minRoyalBet();
            if (moneyBet < min)
                moneyBet = min;
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
                if (selectedDenomIndex >= dens.size())
                    selectedDenomIndex = 0;
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
            dev.lovelace.loveduels.util.Lang.send(player, "gui.duel_setup.errors.offline");
            player.closeInventory();
            return;
        }
        if (duelManager.getMatchManager().isInMatch(opponent.getUniqueId())) {
            dev.lovelace.loveduels.util.Lang.send(player, "gui.duel_setup.errors.in_match");
            return;
        }

        long cd = duelManager.getCooldownManager().getChallengeRemainingSeconds(player.getUniqueId(),
                opponent.getUniqueId());
        if (cd > 0) {
            dev.lovelace.loveduels.util.Lang.send(player, "gui.duel_setup.errors.cooldown", "cd", String.valueOf(cd));
            return;
        }

        // Checked before any ticket/cooldown is spent: arenas are typed (melee / ranged / mounted).
        if (!duelManager.getArenaManager().hasArenaFor(selectedType)) {
            dev.lovelace.loveduels.util.Lang.send(player, "gui.duel_setup.errors.no_arena", "type", selectedType.getDisplayNameMiniMessage());
            return;
        }

        boolean royal = mode.isRoyal();
        boolean training = mode.isTraining();

        if (royal) {
            if (duelManager.getMatchManager().hasActiveRoyalDuel()) {
                dev.lovelace.loveduels.util.Lang.send(player, "gui.duel_setup.errors.royal_active");
                return;
            }
            long royalCd = duelManager.getCooldownManager().getRoyalTicketRemainingSeconds(player.getUniqueId());
            if (royalCd > 0) {
                dev.lovelace.loveduels.util.Lang.send(player, "gui.duel_setup.errors.royal_ticket_cd",
                        "cd", dev.lovelace.loveduels.core.CooldownManager.formatDuration(royalCd));
                return;
            }
            if (!hasRoyalTicket(player)) {
                dev.lovelace.loveduels.util.Lang.send(player, "gui.duel_setup.errors.no_royal_ticket");
                return;
            }
            clampRoyalMoney();
            if (moneyBet < minRoyalBet()) {
                dev.lovelace.loveduels.util.Lang.send(player, "gui.duel_setup.errors.min_royal_bet", "min", CoinFormat.amount(minRoyalBet()));
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
                90_000L);

        duelManager.getMatchManager().sendRequest(req);
        player.closeInventory();

        dev.lovelace.loveduels.util.Lang.send(player, "gui.duel_setup.invite.sent", "opponent", opponent.getName());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        Component acceptBtn = MiniMessage.miniMessage().deserialize(
                royal ? dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.invite.accept_btn_royal")
                      : dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.invite.accept_btn"))
                .clickEvent(ClickEvent.runCommand("/duel accept " + player.getName()))
                .hoverEvent(HoverEvent.showText(dev.lovelace.loveduels.util.Lang.component("gui.duel_setup.invite.hover_accept")));
        Component denyBtn = MiniMessage.miniMessage().deserialize(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.invite.deny_btn"))
                .clickEvent(ClickEvent.runCommand("/duel deny " + player.getName()))
                .hoverEvent(HoverEvent.showText(dev.lovelace.loveduels.util.Lang.component("gui.duel_setup.invite.hover_deny")));

        String prefix = royal
                ? dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.invite.prefix_royal")
                : training
                        ? dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.invite.prefix_training")
                        : dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.invite.prefix_normal");

        String moneyLine;
        String honorLine;
        if (training) {
            moneyLine = dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.invite.money_free");
            honorLine = "";
        } else {
            moneyLine = dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.invite.money_line", "money",
                    CoinFormat.formatBalanceLine(duelManager.getEconomyBridge(), effectiveMoney));
            honorLine = dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.invite.honor_line", "honor", String.valueOf(effectiveHonor));
        }

        moneyLine = CoinFormat.resolveGlyphs(opponent, moneyLine);
        String body = dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.invite.body",
                "player", player.getName(),
                "category", category.getDisplayName(),
                "type", selectedType.getDisplayNameMiniMessage(),
                "money", moneyLine,
                "honor", honorLine
        );

        Component invite = MiniMessage.miniMessage().deserialize(prefix + body)
                .append(acceptBtn).append(Component.text("  ")).append(denyBtn);

        opponent.sendMessage(invite);
        opponent.playSound(opponent.getLocation(),
                royal ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

        if (royal) {
            duelManager.getRoyalManager().broadcastCreation(player, opponent, effectiveMoney, effectiveHonor);
        }
    }

    private List<Denomination> denominations() {
        Optional<LoveEconomy> eco = CoinFormat.tryEconomy();
        if (eco.isEmpty())
            return List.of();
        List<Denomination> dens = new ArrayList<>(eco.get().denominations());
        dens.sort(Comparator.comparingLong(Denomination::value));
        dens.removeIf(d -> d.value() <= 0);
        return dens;
    }

    private boolean hasRoyalTicket(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (duelManager.getRoyalManager().getTicketItem().isTicket(item))
                return true;
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
        Component name = dev.lovelace.loveduels.util.Lang.component("gui.duel_setup.opponent_info.name", "name", opponent.getName());
        List<Component> lore = dev.lovelace.loveduels.util.Lang.componentList(
                "gui.duel_setup.opponent_info.lore",
                "health", String.valueOf((int) opponent.getHealth()),
                "ping", String.valueOf(opponent.getPing())
        );
        return HeadTextures.playerHead(opponent, name, lore);
    }

    private ItemStack createCategoryItem() {
        String name = dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.category.name", "category", category.getDisplayName());
        List<String> lore = List.of(
                "<gray>" + category.getDescription(),
                "",
                dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.category.hint_next"),
                dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.category.hint_prev"));
        String tex = switch (category) {
            case MELEE -> HeadTextures.SWORD;
            case RANGED -> HeadTextures.BOW;
            case MOUNTED -> HeadTextures.HORSE;
        };
        return HeadTextures.head(tex, name, lore);
    }

    private ItemStack createSubtypeItem() {
        String name = dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.subtype.name", "type", selectedType.getDisplayNameMiniMessage());
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + selectedType.getDescription());
        lore.add("");
        if (category.getSubtypes().size() > 1) {
            for (DuelType t : category.getSubtypes()) {
                String mark = t == selectedType ? "<green>●</green> " : "<dark_gray>○</dark_gray> ";
                lore.add(mark + t.getDisplayNameMiniMessage());
            }
            lore.add("");
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.subtype.hint_next"));
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.subtype.hint_prev"));
        } else {
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.subtype.only_one"));
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
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.money_bet.royal_header"));
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.money_bet.royal_min", "min", CoinFormat.amount(minRoyalBet())));
            lore.add("");
        }
        lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.money_bet.current"));

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
            if (selectedDenomIndex >= dens.size())
                selectedDenomIndex = 0;
            Denomination sel = dens.get(selectedDenomIndex);
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.money_bet.denom", "glyph", CoinFormat.getCoinGlyph(sel)));
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.money_bet.controls"));
        }

        String balance = CoinFormat.formatBalanceLine(duelManager.getEconomyBridge(),
                duelManager.getEconomyBridge().getBalance(player));
        lore.add("");
        lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.money_bet.balance", "balance", balance));

        String name = mode.isRoyal()
                ? dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.money_bet.name_royal")
                : dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.money_bet.name_normal");
        return HeadTextures.head(mode.isRoyal() ? HeadTextures.CROWN : HeadTextures.COIN, name,
                CoinFormat.resolveGlyphs(player, lore));
    }

    private ItemStack createHonorBetItem() {
        String name = dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.honor_bet.name");
        List<String> lore = new ArrayList<>();
        lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.honor_bet.current", "honor", String.valueOf(honorBet)));
        if (mode.isRoyal()) {
            lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.honor_bet.royal_note"));
        }
        lore.add("");
        lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.honor_bet.hint_multiply"));
        lore.add(dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.honor_bet.hint_divide"));
        return HeadTextures.head(HeadTextures.STAR, name, lore);
    }

    private ItemStack createSendButton() {
        String name = mode.isRoyal()
                ? dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.send_button.name_royal")
                : mode.isTraining()
                        ? dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.send_button.name_training")
                        : dev.lovelace.loveduels.util.Lang.get("gui.duel_setup.send_button.name_normal");
        List<String> lore = mode.isTraining()
                ? dev.lovelace.loveduels.util.Lang.list("gui.duel_setup.send_button.lore_training")
                : dev.lovelace.loveduels.util.Lang.list("gui.duel_setup.send_button.lore_normal");
        return HeadTextures.head(mode.isRoyal() ? HeadTextures.CROWN : HeadTextures.SWORD, name, lore);
    }
}
