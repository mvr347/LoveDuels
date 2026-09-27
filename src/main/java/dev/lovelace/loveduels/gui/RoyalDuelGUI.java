package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class RoyalDuelGUI extends CustomGUI {

    private final DuelManager duelManager;

    public RoyalDuelGUI(Player player, DuelManager duelManager) {
        super(player, 45, MiniMessage.miniMessage().deserialize("<gradient:#FFD700:#FFA500><b>👑 Королевские Дуэли Королевства</b></gradient>"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        boolean hasTicket = checkTicket(player);
        boolean serverActive = duelManager.getMatchManager().hasActiveRoyalDuel();
        long cd = duelManager.getCooldownManager().getRoyalTicketRemainingSeconds(player.getUniqueId());

        // Overview Scroll (Slot 20)
        setItem(20, createOverviewItem(), null);

        // Status Item (Slot 22)
        setItem(22, createStatusItem(hasTicket, serverActive, cd), null);

        // Challenge Button (Slot 24)
        setItem(24, createChallengeButtonItem(hasTicket, serverActive, cd), e -> {
            if (!hasTicket) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ У вас нет Билета Королевской Дуэли!"));
                return;
            }
            if (serverActive) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ На сервере уже идёт Королевская Дуэль!"));
                return;
            }
            if (cd > 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>⏳ Подождите кулдаун билета: " + cd + "с."));
                return;
            }
            new PlayerSelectGUI(player, duelManager, true).open();
        });
    }

    private boolean checkTicket(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (duelManager.getRoyalManager().getTicketItem().isTicket(item)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack createOverviewItem() {
        String name = "<gradient:#FFD700:#FFA500><b>Правила Королевской Дуэли</b></gradient>";
        List<String> lore = List.of(
                "<gray>Самый престижный турнирный бой.",
                "",
                "<yellow>▪ Обязательная ставка деньгами",
                "<yellow>▪ Серверные оповещения Глашатая",
                "<yellow>▪ Победитель забирает банк + <gold>25% бонус",
                "<yellow>▪ Увеличенный прирост Чести (<green>+150%</green>)",
                "<yellow>▪ Оповещение в канале Discord",
                "<yellow>▪ Ограничение: 1 дуэль на сервер одновременно"
        );
        return HeadTextures.head(HeadTextures.SCROLL, name, lore);
    }

    private ItemStack createStatusItem(boolean hasTicket, boolean serverActive, long cd) {
        boolean can = hasTicket && !serverActive && cd <= 0;
        String ticketStr = hasTicket ? "<green>✔ В наличии" : "<red>✖ Отсутствует";
        String arenaStr = serverActive ? "<red>✖ Занято (идёт бой)" : "<green>✔ Свободно";
        String cdStr = cd <= 0 ? "<green>✔ Готов" : "<red>⏳ " + cd + "с";

        String name = "<gold><b>Статус готовности:</b></gold>";
        List<String> lore = List.of(
                "<gray>Наличие билета: " + ticketStr,
                "<gray>Статус турнира: " + arenaStr,
                "<gray>Кулдаун билета: " + cdStr
        );

        return HeadTextures.head(can ? HeadTextures.READY : HeadTextures.NOT_READY, name, lore);
    }

    private ItemStack createChallengeButtonItem(boolean hasTicket, boolean serverActive, long cd) {
        boolean can = hasTicket && !serverActive && cd <= 0;
        String name = can ? "<gradient:#FFD700:#FFA500><b>⚔ БРОСИТЬ КОРОЛЕВСКИЙ ВЫЗОВ</b></gradient>"
                          : "<red><b>Недоступно для вызова</b></red>";
        List<String> lore = List.of(
                can ? "<yellow>➤ Нажмите для выбора оппонента" : "<gray>Проверьте условия готовности"
        );
        return HeadTextures.head(can ? HeadTextures.CROWN : HeadTextures.CANCEL, name, lore);
    }
}
