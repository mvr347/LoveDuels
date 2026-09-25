package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelBet;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.core.DuelRequest;
import dev.lovelace.loveduels.core.DuelType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class DuelSetupGUI extends CustomGUI {

    private final Player opponent;
    private final DuelManager duelManager;
    private final boolean royal;

    private DuelType selectedType = DuelType.OWN_INVENTORY;
    private String selectedKitId = null;
    private long moneyBet = 0L;
    private int honorBet = 0;

    public DuelSetupGUI(Player player, Player opponent, DuelManager duelManager, boolean royal) {
        super(player, 45, MiniMessage.miniMessage().deserialize(
                royal ? "<gradient:#FFD700:#FFA500><b>👑 Настройка Королевской Дуэли</b></gradient>"
                      : "<gold><b>⚔ Настройка параметров дуэли</b></gold>"
        ));
        this.opponent = opponent;
        this.duelManager = duelManager;
        this.royal = royal;
        if (royal) {
            this.moneyBet = 500L; // Minimum required money stake for royal duel
        }
    }

    public void setKit(String kitId) {
        this.selectedKitId = kitId;
        this.selectedType = DuelType.KIT;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new PlayerSelectGUI(player, duelManager, royal).open());

        // Header indicator of opponent
        setItem(4, createOpponentInfoItem(), null);

        // Row 2 (slots 19 to 25): Duel Type Selectors
        DuelType[] types = DuelType.values();
        int[] typeSlots = new int[]{19, 20, 21, 22, 23};
        for (int i = 0; i < types.length && i < typeSlots.length; i++) {
            DuelType dt = types[i];
            int slot = typeSlots[i];
            boolean selected = (selectedType == dt);

            setItem(slot, createTypeItem(dt, selected), e -> {
                if (dt == DuelType.KIT && selectedKitId == null) {
                    new KitSelectGUI(player, duelManager, kit -> {
                        this.selectedKitId = kit.id();
                        this.selectedType = DuelType.KIT;
                        this.open();
                    }).open();
                } else {
                    this.selectedType = dt;
                    build();
                }
            });
        }

        // Row 3 (slots 28 to 34): Stakes and Send Challenge
        // Money bet adjuster
        setItem(29, createMoneyBetItem(), e -> {
            if (e.isRightClick()) {
                moneyBet = royal ? 500L : 0L;
            } else if (e.isShiftClick()) {
                moneyBet += 1000L;
            } else {
                moneyBet += 250L;
            }
            build();
        });

        // Honor bet adjuster
        setItem(30, createHonorBetItem(), e -> {
            if (e.isRightClick()) {
                honorBet = 0;
            } else if (e.isShiftClick()) {
                honorBet += 50;
            } else {
                honorBet += 10;
            }
            build();
        });

        // Send challenge button (Slot 31)
        setItem(31, createSendButton(), e -> sendChallenge());

        // Reset bets button (Slot 32)
        setItem(32, createResetBetsItem(), e -> {
            moneyBet = royal ? 500L : 0L;
            honorBet = 0;
            build();
        });
    }

    private void sendChallenge() {
        if (!opponent.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Игрок покинул сервер."));
            player.closeInventory();
            return;
        }

        if (duelManager.getMatchManager().isInMatch(opponent.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Этот игрок уже находится в бою!"));
            player.closeInventory();
            return;
        }

        // Check challenge cooldown
        long cd = duelManager.getCooldownManager().getChallengeRemainingSeconds(player.getUniqueId(), opponent.getUniqueId());
        if (cd > 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>⏳ Вы недавно вызывали этого игрока! Подождите ещё <gold>" + cd + "с</gold>."
            ));
            return;
        }

        // Check Royal duel prerequisites
        if (royal) {
            if (duelManager.getMatchManager().hasActiveRoyalDuel()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ На сервере уже идёт Королевская Дуэль!"));
                return;
            }

            long royalCd = duelManager.getCooldownManager().getRoyalTicketRemainingSeconds(player.getUniqueId());
            if (royalCd > 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>⏳ Кулдаун Королевского Билета! Подождите ещё <gold>" +
                        dev.lovelace.loveduels.core.CooldownManager.formatDuration(royalCd) + "</gold>."
                ));
                return;
            }

            // Check ticket in inventory
            if (!hasRoyalTicket(player)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ У вас нет Билета Королевской Дуэли в инвентаре!"));
                return;
            }

            if (moneyBet <= 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ В Королевской Дуэли обязательна денежная ставка!"));
                return;
            }
        }

        // Check economy balance
        if (moneyBet > 0) {
            if (!duelManager.getEconomyBridge().has(player, moneyBet)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>❌ У вас недостаточно денег! Требуется: <gold>" + moneyBet + " монет."
                ));
                return;
            }
            if (!duelManager.getEconomyBridge().has(opponent, moneyBet)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>❌ У противника недостаточно денег для такой ставки!"
                ));
                return;
            }
        }

        // Deduct ticket if royal
        if (royal) {
            consumeRoyalTicket(player);
            duelManager.getCooldownManager().setRoyalTicketCooldown(player.getUniqueId(), 3600); // 1 hour cooldown
        }

        // Set challenge cooldown (10 minutes = 600 seconds)
        duelManager.getCooldownManager().setChallengeCooldown(player.getUniqueId(), opponent.getUniqueId(), 600);

        DuelBet bet = new DuelBet(moneyBet, honorBet);
        DuelRequest req = DuelRequest.of(
                player.getUniqueId(),
                opponent.getUniqueId(),
                selectedType,
                selectedKitId,
                bet,
                royal,
                60_000L // 60 seconds timeout
        );

        duelManager.getMatchManager().sendRequest(req);
        player.closeInventory();

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>✔ Вы бросили вызов игроку <gold>" + opponent.getName() + "</gold>!"
        ));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        // Notify opponent with clickable MiniMessage
        Component acceptBtn = MiniMessage.miniMessage().deserialize("<green><b>[ПРИНЯТЬ]</b></green>")
                .clickEvent(ClickEvent.runCommand("/duel accept " + player.getName()))
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<green>Нажмите, чтобы принять дуэль")));

        Component denyBtn = MiniMessage.miniMessage().deserialize("<red><b>[ОТКЛОНИТЬ]</b></red>")
                .clickEvent(ClickEvent.runCommand("/duel deny " + player.getName()))
                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<red>Нажмите, чтобы отклонить дуэль")));

        String prefix = royal ? "<gradient:#FFD700:#FFA500>👑 [КОРОЛЕВСКИЙ ВЫЗОВ]</gradient>\n" : "<gold>⚔ [ВЫЗОВ НА ДУЭЛЬ]</gold>\n";
        String betInfo = "";
        if (moneyBet > 0) betInfo += "<green>Монеты: " + moneyBet + " </green>";
        if (honorBet > 0) betInfo += "<yellow>Честь: " + honorBet + "</yellow>";
        if (betInfo.isBlank()) betInfo = "<gray>Без ставок</gray>";

        Component invite = MiniMessage.miniMessage().deserialize(
                prefix +
                "<white>Игрок <gold>" + player.getName() + "</gold> вызывает вас на поединок!\n" +
                "<gray>Режим: <white>" + selectedType.getDisplayNameMiniMessage() + "\n" +
                "<gray>Ставки: " + betInfo + "\n"
        ).append(acceptBtn).append(Component.text("  ")).append(denyBtn);

        opponent.sendMessage(invite);
        opponent.playSound(opponent.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

        if (royal) {
            duelManager.getRoyalManager().broadcastCreation(player, opponent, moneyBet, honorBet);
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
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(opponent);
            meta.displayName(MiniMessage.miniMessage().deserialize(
                    "<gold><b>Соперник: " + opponent.getName() + "</b>"
            ).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTypeItem(DuelType dt, boolean selected) {
        ItemStack item = new ItemStack(dt.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = (selected ? "<green>✔ " : "<gray>") + dt.getDisplayNameMiniMessage();
            meta.displayName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>" + dt.getDescription()).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize(selected ? "<green><b>ВЫБРАНО</b>" : "<yellow>➤ Нажмите для выбора").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMoneyBetItem() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(
                    "<gold><b>Ставка деньгами: <green>" + moneyBet + " монет</b></gold>"
            ).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Ваш баланс: <white>" + duelManager.getEconomyBridge().getBalance(player) + " монет").decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize("<yellow>ЛКМ: <white>+250 монет").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<yellow>Shift+ЛКМ: <white>+1000 монет").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<red>ПКМ: <white>Сбросить ставку").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHonorBetItem() {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(
                    "<yellow><b>Ставка Честью: <gold>" + honorBet + " очков</b></yellow>"
            ).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Рейтинг победителя вырастет,").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>а проигравший потеряет Честь.").decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize("<yellow>ЛКМ: <white>+10 Чести").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<yellow>Shift+ЛКМ: <white>+50 Чести").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<red>ПКМ: <white>Сбросить ставку").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSendButton() {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(
                    "<green><b>⚔ ОТПРАВИТЬ ВЫЗОВ</b></green>"
            ).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Нажмите, чтобы отправить приглашение.").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createResetBetsItem() {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<red>Сбросить все ставки</red>").decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
