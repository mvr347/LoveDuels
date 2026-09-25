package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

        // Overview Book (Slot 20)
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
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(
                    "<gradient:#FFD700:#FFA500><b>Правила Королевской Дуэли</b></gradient>"
            ).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Самый престижный турнирный бой.").decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize("<yellow>▪ Обязательная ставка деньгами").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<yellow>▪ Серверные оповещения Глашатая").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<yellow>▪ Победитель забирает банк + <gold>25% бонус").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<yellow>▪ Увеличенный прирост Чести (<green>+150%</green>)").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<yellow>▪ Оповещение в канале Discord").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<yellow>▪ Ограничение: 1 дуэль на сервер одновременно").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createStatusItem(boolean hasTicket, boolean serverActive, long cd) {
        ItemStack item = new ItemStack(hasTicket && !serverActive && cd <= 0 ? Material.EMERALD : Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(
                    "<gold><b>Статус готовности:</b></gold>"
            ).decoration(TextDecoration.ITALIC, false));

            String ticketStr = hasTicket ? "<green>✔ В наличии" : "<red>✖ Отсутствует";
            String arenaStr = serverActive ? "<red>✖ Занято (идёт бой)" : "<green>✔ Свободно";
            String cdStr = cd <= 0 ? "<green>✔ Готов" : "<red>⏳ " + cd + "с";

            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Наличие билета: " + ticketStr).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Статус турнира: " + arenaStr).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Кулдаун билета: " + cdStr).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createChallengeButtonItem(boolean hasTicket, boolean serverActive, long cd) {
        boolean can = hasTicket && !serverActive && cd <= 0;
        ItemStack item = new ItemStack(can ? Material.NETHER_STAR : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = can ? "<gradient:#FFD700:#FFA500><b>⚔ БРОСИТЬ КОРОЛЕВСКИЙ ВЫЗОВ</b></gradient>"
                              : "<red><b>Недоступно для вызова</b></red>";
            meta.displayName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize(can ? "<yellow>➤ Нажмите для выбора оппонента" : "<gray>Проверьте условия готовности").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
