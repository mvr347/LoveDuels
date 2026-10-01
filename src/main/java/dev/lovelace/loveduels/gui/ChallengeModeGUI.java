package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.ChallengeMode;
import dev.lovelace.loveduels.core.DuelManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Выбор режима вызова: тренировка / дуэль / королевская.
 */
public final class ChallengeModeGUI extends CustomGUI {

    private final DuelManager duelManager;

    public ChallengeModeGUI(Player player, DuelManager duelManager) {
        super(player, 27, MiniMessage.miniMessage().deserialize(
                "<gold>Выбор режима вызова</gold>"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        setItem(11, HeadTextures.head(HeadTextures.READY,
                "<aqua>Тренировочная дуэль</aqua>",
                List.of(
                        "<gray>Без ставок и без изменения Чести.",
                        "<gray>Только практика и разминка.",
                        "",
                        "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>выбрать соперника</white>"
                )
        ), e -> new PlayerSelectGUI(player, duelManager, ChallengeMode.TRAINING).open());

        setItem(13, HeadTextures.head(HeadTextures.SWORD,
                "<gold>Дуэль</gold>",
                List.of(
                        "<gray>Обычный поединок со ставками",
                        "<gray>и рейтингом Чести.",
                        "",
                        "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>выбрать соперника</white>"
                )
        ), e -> new PlayerSelectGUI(player, duelManager, ChallengeMode.NORMAL).open());

        setItem(15, HeadTextures.head(HeadTextures.CROWN,
                "<gradient:#C9A227:#E8D48B>Королевская дуэль</gradient>",
                List.of(
                        "<gray>Нужен билет. Глашатай, бонусы,",
                        "<gray>Discord и повышенная Честь.",
                        "",
                        "<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>выбрать соперника</white>"
                )
        ), e -> new PlayerSelectGUI(player, duelManager, ChallengeMode.ROYAL).open());
    }
}
