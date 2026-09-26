package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

public final class MainMenuGUI extends CustomGUI {

    private final DuelManager duelManager;

    public MainMenuGUI(Player player, DuelManager duelManager) {
        super(player, 45, MiniMessage.miniMessage().deserialize(
                "<gradient:#FFD700:#FFA500><b>⚔ Турниры и Дуэли</b></gradient>"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(false, null);

        setItem(20, HeadTextures.head(HeadTextures.SWORD,
                "<gold><b>⚔ Бросить вызов</b></gold>",
                List.of(
                        "<gray>Выберите соперника и настройте дуэль.",
                        "",
                        "<yellow>➤ ЛКМ — список игроков"
                )
        ), e -> new PlayerSelectGUI(player, duelManager, false).open());

        setItem(21, HeadTextures.head(HeadTextures.CHEST,
                "<aqua><b>🛡 Киты</b></aqua>",
                List.of(
                        "<gray>Заготовленные наборы снаряжения.",
                        "",
                        "<yellow>➤ ЛКМ — просмотр"
                )
        ), e -> new KitSelectGUI(player, duelManager, null).open());

        setItem(22, HeadTextures.head(HeadTextures.CROWN,
                "<gradient:#FFD700:#FFA500><b>👑 Королевская Дуэль</b></gradient>",
                List.of(
                        "<gray>Эпический бой с глашатаем и бонусами.",
                        "<gold>Нужен Билет Королевской Дуэли",
                        "",
                        "<yellow>➤ ЛКМ — открыть"
                )
        ), e -> new RoyalDuelGUI(player, duelManager).open());

        setItem(23, HeadTextures.head(HeadTextures.TROPHY,
                "<yellow><b>🏆 Зал Славы</b></yellow>",
                List.of(
                        "<gray>Топ Чести, серии побед, статистика.",
                        "",
                        "<yellow>➤ ЛКМ — топ"
                )
        ), e -> new LeaderboardGUI(player, duelManager).open());

        setItem(24, HeadTextures.head(HeadTextures.EYE,
                "<light_purple><b>👁 Наблюдение</b></light_purple>",
                List.of(
                        "<gray>Смотрите идущие поединки с трибун.",
                        "",
                        "<yellow>➤ ЛКМ — список боёв"
                )
        ), e -> new SpectateListGUI(player, duelManager).open());
    }
}
