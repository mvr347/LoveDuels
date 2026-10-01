package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Главное меню: только вызов и наблюдение (gui-gen-5, 45 слотов).
 */
public final class MainMenuGUI extends CustomGUI {

    private final DuelManager duelManager;

    public MainMenuGUI(Player player, DuelManager duelManager) {
        super(player, 45, MiniMessage.miniMessage().deserialize(
                "<gradient:#C9A227:#E8D48B>Турниры и дуэли</gradient>"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(false, null);

        // Рабочая зона: два пункта по центру (слоты 21 и 23)
        setItem(21, HeadTextures.head(HeadTextures.SWORD,
                "<gold>Бросить вызов</gold>",
                List.of(
                        "<gray>Тренировка, обычная или королевская дуэль.",
                        "",
                        "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>выбрать режим</white>"
                )
        ), e -> new ChallengeModeGUI(player, duelManager).open());

        setItem(23, HeadTextures.head(HeadTextures.EYE,
                "<light_purple>Наблюдение</light_purple>",
                List.of(
                        "<gray>Смотрите идущие поединки с трибун.",
                        "",
                        "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>список боёв</white>"
                )
        ), e -> new SpectateListGUI(player, duelManager).open());
    }
}
