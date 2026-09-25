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

public final class MainMenuGUI extends CustomGUI {

    private final DuelManager duelManager;

    public MainMenuGUI(Player player, DuelManager duelManager) {
        super(player, 45, MiniMessage.miniMessage().deserialize("<gradient:#FFD700:#FFA500><b>⚔ Турниры и Дуэли LoveDuels</b></gradient>"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(false, null);

        // Work area buttons (Row 2, slots 19 to 25)
        // 1. Challenge Player
        setItem(20, createItem(
                Material.IRON_SWORD,
                "<gold><b>⚔ Бросить вызов игроку</b></gold>",
                List.of(
                        "<gray>Выберите соперника из списка",
                        "<gray>онлайн-игроков и настройте дуэль.",
                        "",
                        "<yellow>➤ Нажмите для выбора оппонента"
                )
        ), e -> new PlayerSelectGUI(player, duelManager, false).open());

        // 2. Kits
        setItem(21, createItem(
                Material.CHEST,
                "<aqua><b>🛡 Наборы снаряжения (Киты)</b></aqua>",
                List.of(
                        "<gray>Просмотр заготовленных наборов",
                        "<gray>для честных состязаний на равных.",
                        "",
                        "<yellow>➤ Нажмите для просмотра китов"
                )
        ), e -> new KitSelectGUI(player, duelManager, null).open());

        // 3. Royal Duel
        setItem(22, createItem(
                Material.NETHER_STAR,
                "<gradient:#FFD700:#FFA500><b>👑 Королевская Дуэль</b></gradient>",
                List.of(
                        "<gray>Эпический турнирный бой за казну,",
                        "<gray>с оповещением Глашатая и бонусами.",
                        "",
                        "<gold>Требуется: <yellow>Билет Королевской Дуэли",
                        "",
                        "<yellow>➤ Нажмите для открытия меню"
                )
        ), e -> new RoyalDuelGUI(player, duelManager).open());

        // 4. Leaderboard & Stats
        setItem(23, createItem(
                Material.GOLDEN_HELMET,
                "<yellow><b>🏆 Зал Славы и Топ Чести</b></yellow>",
                List.of(
                        "<gray>Рейтинг лучших дуэлянтов королевства,",
                        "<gray>ваша статистика, серии побед и Честь.",
                        "",
                        "<yellow>➤ Нажмите для просмотра топа"
                )
        ), e -> new LeaderboardGUI(player, duelManager).open());

        // 5. Spectate
        setItem(24, createItem(
                Material.ENDER_EYE,
                "<light_purple><b>👁 Наблюдение за боями</b></light_purple>",
                List.of(
                        "<gray>Смотрите за идущими поединками",
                        "<gray>с трибун в режиме зрителя.",
                        "",
                        "<yellow>➤ Нажмите для выбора арены"
                )
        ), e -> new SpectateListGUI(player, duelManager).open());
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(l -> MiniMessage.miniMessage().deserialize(l).decoration(TextDecoration.ITALIC, false))
                    .toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
