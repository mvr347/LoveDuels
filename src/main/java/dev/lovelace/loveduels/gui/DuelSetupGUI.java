package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.ChallengeMode;
import dev.lovelace.loveduels.core.DuelBet;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.core.DuelRequest;
import dev.lovelace.loveduels.core.DuelType;
import dev.lovelace.loveduels.kit.Kit;
import dev.lovelace.loveduels.util.CoinFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class DuelSetupGUI extends CustomGUI {

    private final Player opponent;
    private final DuelManager duelManager;
    private final ChallengeMode mode;

    private DuelType selectedType = DuelType.OWN_INVENTORY;
    private String selectedKitId = null;
    private long moneyBet = 0L;
    private int honorBet = 0;

    public DuelSetupGUI(Player player, Player opponent, DuelManager duelManager, ChallengeMode mode) {
        super(player, 45, MiniMessage.miniMessage().deserialize(titleFor(mode)));
        this.opponent = opponent;
        this.duelManager = duelManager;
        this.mode = mode;
        if (mode.isRoyal()) {
            this.moneyBet = 500L;
        }
    }

    /** @deprecated use ChallengeMode overload */
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

    public void setKit(String kitId) {
        this.selectedKitId = kitId;
        this.selectedType = DuelType.KIT;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new PlayerSelectGUI(player, duelManager, mode).open());

        setItem(4, createOpponentInfoItem(), null);

        DuelType[] types = DuelType.values();
        int[] typeSlots = new int[]{19, 20, 21, 22, 23, 24};
        List<Kit> kits = new ArrayList<>(duelManager.getKitManager().getAllKits());

        if (selectedKitId == null && !kits.isEmpty()) {
            selectedKitId = kits.get(0).id();
        }

        for (int i = 0; i < types.length && i < typeSlots.length; i++) {
            DuelType dt = types[i];
            int slot = typeSlots[i];
            boolean selected = (selectedType == dt);

            if (dt == DuelType.KIT) {
                setItem(slot, createKitTypeItem(selected, kits), e -> {
                    if (selectedType != DuelType.KIT) {
                        selectedType = DuelType.KIT;
                        if (selectedKitId == null && !kits.isEmpty()) {
                            selectedKitId = kits.get(0).id();
                        }
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                        build();
                        return;
                    }
                    if (kits.isEmpty()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Нет настроенных китов на сервере."));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }
                    int curIdx = 0;
                    for (int k = 0; k < kits.size(); k++) {
                        if (kits.get(k).id().equalsIgnoreCase(selectedKitId)) {
                            curIdx = k;
                            break;
                        }
                    }
                    if (e.isRightClick()) {
                        selectedKitId = kits.get((curIdx - 1 + kits.size()) % kits.size()).id();
                    } else {
                        selectedKitId = kits.get((curIdx + 1) % kits.size()).id();
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.3f);
                    build();
                });
            } else {
                setItem(slot, createTypeItem(dt, selected), e -> {
                    this.selectedType = dt;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    build();
                });
            }
        }

        boolean canBet = !mode.isTraining();

        if (canBet) {
            setItem(29, createMoneyBetItem(), e -> {
                if (e.isRightClick()) {
                    moneyBet = mode.isRoyal() ? 500L : 0L;
                } else if (e.isShiftClick()) {
                    moneyBet += 1000L;
                } else {
                    moneyBet += 250L;
                }
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                build();
            });

            setItem(30, createHonorBetItem(), e -> {
                if (e.isRightClick()) {
                    honorBet = 0;
                } else if (e.isShiftClick()) {
                    honorBet += 50;
                } else {
                    honorBet += 10;
                }
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
                build();
            });

            setItem(32, createResetBetsItem(), e -> {
                moneyBet = mode.isRoyal() ? 500L : 0L;
                honorBet = 0;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                build();
            });
        } else {
            setItem(30, HeadTextures.head(HeadTextures.READY,
                    "<aqua>Тренировка</aqua>",
                    List.of(
                            "<gray>Ставки и Честь не меняются.",
                            "<gray>Только практика."
                    )
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
            player.closeInventory();
            return;
        }

        long cd = duelManager.getCooldownManager().getChallengeRemainingSeconds(player.getUniqueId(), opponent.getUniqueId());
        if (cd > 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Недавно вызывали этого игрока. Подождите ещё <gold>" + cd + "с</gold>."
            ));
            return;
        }

        boolean royal = mode.isRoyal();
        boolean training = mode.isTraining();

        if (royal) {
            if (duelManager.getMatchManager().hasActiveRoyalDuel()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>На сервере уже идёт королевская дуэль."));
                return;
            }
            long royalCd = duelManager.getCooldownManager().getRoyalTicketRemainingSeconds(player.getUniqueId());
            if (royalCd > 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Кулдаун королевского билета: <gold>" +
                        dev.lovelace.loveduels.core.CooldownManager.formatDuration(royalCd) + "</gold>."
                ));
                return;
            }
            if (!hasRoyalTicket(player)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Нет билета королевской дуэли в инвентаре."));
                return;
            }
            if (moneyBet <= 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>В королевской дуэли обязательна денежная ставка."));
                return;
            }
        }

        long effectiveMoney = training ? 0L : moneyBet;
        int effectiveHonor = training ? 0 : honorBet;

        if (effectiveMoney > 0) {
            if (!duelManager.getEconomyBridge().has(player, effectiveMoney)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Недостаточно средств. Нужно: <gold>" +
                        CoinFormat.formatBalanceLine(duelManager.getEconomyBridge(), effectiveMoney) + "</gold>."
                ));
                return;
            }
            if (!duelManager.getEconomyBridge().has(opponent, effectiveMoney)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>У противника недостаточно средств для такой ставки."
                ));
                return;
            }
        }

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
                selectedKitId,
                bet,
                royal,
                training,
                60_000L
        );

        duelManager.getMatchManager().sendRequest(req);
        player.closeInventory();

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>Вызов отправлен игроку <gold>" + opponent.getName() + "</gold>."
        ));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        Component acceptBtn = MiniMessage.miniMessage().deserialize("<green>[Принять]</green>")
                .clickEvent(ClickEvent.runCommand("/duel accept " + player.getName()))
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<green>Принять дуэль</green>")));

        Component denyBtn = MiniMessage.miniMessage().deserialize("<red>[Отклонить]</red>")
                .clickEvent(ClickEvent.runCommand("/duel deny " + player.getName()))
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<red>Отклонить дуэль</red>")));

        String prefix = royal
                ? "<gradient:#C9A227:#E8D48B>[Королевский вызов]</gradient>\n"
                : training
                    ? "<aqua>[Тренировочный вызов]</aqua>\n"
                    : "<gold>[Вызов на дуэль]</gold>\n";

        String betInfo;
        if (training) {
            betInfo = "<gray>Без ставок (тренировка)</gray>";
        } else {
            betInfo = "";
            if (effectiveMoney > 0) {
                betInfo += "<green>Монеты: " + CoinFormat.formatBalanceLine(duelManager.getEconomyBridge(), effectiveMoney) + " </green>";
            }
            if (effectiveHonor > 0) betInfo += "<yellow>Честь: " + effectiveHonor + "</yellow>";
            if (betInfo.isBlank()) betInfo = "<gray>Без ставок</gray>";
        }

        Component invite = MiniMessage.miniMessage().deserialize(
                prefix +
                "<white>Игрок <gold>" + player.getName() + "</gold> вызывает вас.\n" +
                "<gray>Режим: </gray>" + selectedType.getDisplayNameMiniMessage() + "\n" +
                "<gray>Ставки: </gray>" + betInfo + "\n"
        ).append(acceptBtn).append(Component.text("  ")).append(denyBtn);

        opponent.sendMessage(invite);
        opponent.playSound(opponent.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

        if (royal) {
            duelManager.getRoyalManager().broadcastCreation(player, opponent, effectiveMoney, effectiveHonor);
        }
    }

    private boolean hasRoyalTicket(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (duelManager.getRoyalManager().getTicketItem().isTicket(item)) {
                return true;
            }
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
                MiniMessage.miniMessage().deserialize("<gold>Соперник: " + opponent.getName() + "</gold>"),
                List.of(
                        MiniMessage.miniMessage().deserialize("<gray>Здоровье: <red>" + (int) opponent.getHealth() + "</red>"),
                        MiniMessage.miniMessage().deserialize("<gray>Пинг: <white>" + opponent.getPing() + " ms</white>")
                )
        );
    }

    private ItemStack createTypeItem(DuelType dt, boolean selected) {
        String headTexture = switch (dt) {
            case SWORD -> HeadTextures.SWORD;
            case BOW -> HeadTextures.BOW;
            case HORSE_SPEAR -> HeadTextures.HORSE;
            case OWN_INVENTORY -> HeadTextures.BACKPACK;
            case FISTS -> HeadTextures.SKULL;
            default -> HeadTextures.SWORD;
        };

        String name = (selected ? "<green>● </green>" : "<dark_gray>○ </dark_gray>") + dt.getDisplayNameMiniMessage();
        List<String> lore = List.of(
                "<gray>" + dt.getDescription(),
                "",
                selected ? "<green>Выбрано</green>" : "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>выбрать</white>"
        );
        return HeadTextures.head(headTexture, name, lore);
    }

    private ItemStack createKitTypeItem(boolean selected, List<Kit> kits) {
        String name = (selected ? "<green>● </green>" : "<dark_gray>○ </dark_gray>") + DuelType.KIT.getDisplayNameMiniMessage();

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Сражение с готовым набором предметов.");
        lore.add("");

        if (kits.isEmpty()) {
            lore.add("<red>Нет настроенных китов");
        } else {
            int curIdx = 0;
            for (int k = 0; k < kits.size(); k++) {
                if (kits.get(k).id().equalsIgnoreCase(selectedKitId)) {
                    curIdx = k;
                    break;
                }
            }
            Kit curKit = kits.get(curIdx);
            lore.add("<aqua>Кит: <yellow>«" + curKit.displayName() + "»</yellow> <dark_gray>(" + (curIdx + 1) + "/" + kits.size() + ")</dark_gray></aqua>");
            lore.add("");
            lore.add("<gray>ЛКМ — следующий · ПКМ — предыдущий");
        }

        lore.add("");
        lore.add(selected ? "<green>Выбрано</green>" : "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>выбрать режим</white>");

        return HeadTextures.head(HeadTextures.CHEST, name, lore);
    }

    private ItemStack createMoneyBetItem() {
        String glyphs = CoinFormat.formatBalanceLine(duelManager.getEconomyBridge(), moneyBet);
        String balance = CoinFormat.formatBalanceLine(duelManager.getEconomyBridge(),
                duelManager.getEconomyBridge().getBalance(player));
        String name = "<gold>Ставка: </gold>" + glyphs;
        List<String> lore = List.of(
                "<gray>Баланс: </gray>" + balance,
                "",
                "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>+250</white>",
                "<yellow>Shift+ЛКМ</yellow> <dark_gray>—</dark_gray> <white>+1000</white>",
                "<red>ПКМ</red> <dark_gray>—</dark_gray> <white>сбросить</white>"
        );
        return HeadTextures.head(HeadTextures.COIN, name, lore);
    }

    private ItemStack createHonorBetItem() {
        String name = "<yellow>Ставка Честью: <white>" + honorBet + "</white></yellow>";
        List<String> lore = List.of(
                "<gray>Победитель получает, проигравший теряет.",
                "",
                "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>+10</white>",
                "<yellow>Shift+ЛКМ</yellow> <dark_gray>—</dark_gray> <white>+50</white>",
                "<red>ПКМ</red> <dark_gray>—</dark_gray> <white>сбросить</white>"
        );
        return HeadTextures.head(HeadTextures.STAR, name, lore);
    }

    private ItemStack createSendButton() {
        String name = mode.isRoyal()
                ? "<gradient:#C9A227:#E8D48B>Отправить королевский вызов</gradient>"
                : mode.isTraining()
                    ? "<aqua>Отправить тренировочный вызов</aqua>"
                    : "<green>Отправить вызов</green>";
        List<String> lore = List.of(
                "<gray>Приглашение уйдёт сопернику."
        );
        return HeadTextures.head(HeadTextures.SWORD, name, lore);
    }

    private ItemStack createResetBetsItem() {
        return HeadTextures.head(HeadTextures.RESET,
                "<red>Сбросить ставки</red>",
                List.of("<gray>Обнулить деньги и Честь"));
    }
}
