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

    @Override
    protected String sectionKey() {
        return "challenge_mode";
    }

    private final DuelManager duelManager;

    public ChallengeModeGUI(Player player, DuelManager duelManager) {
        super(player, 27, dev.lovelace.loveduels.util.Lang.component("gui.challenge_mode.title"));
        this.duelManager = duelManager;
    }

    /** The texture key of a mode's head: used by its button here and by slot 0 of the menus opened for that mode. */
    static String textureOf(ChallengeMode mode) {
        return switch (mode) {
            case TRAINING -> "modes.training";
            case NORMAL -> "modes.normal";
            case ROYAL -> "modes.royal";
        };
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        setItem(11, HeadTextures.head(textureOf(ChallengeMode.TRAINING),
                dev.lovelace.loveduels.util.Lang.get("gui.challenge_mode.training.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.challenge_mode.training.lore")
        ), e -> new PlayerSelectGUI(player, duelManager, ChallengeMode.TRAINING).open());

        setItem(13, HeadTextures.head(textureOf(ChallengeMode.NORMAL),
                dev.lovelace.loveduels.util.Lang.get("gui.challenge_mode.normal.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.challenge_mode.normal.lore")
        ), e -> new PlayerSelectGUI(player, duelManager, ChallengeMode.NORMAL).open());

        setItem(15, HeadTextures.head(textureOf(ChallengeMode.ROYAL),
                dev.lovelace.loveduels.util.Lang.get("gui.challenge_mode.royal.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.challenge_mode.royal.lore")
        ), e -> new PlayerSelectGUI(player, duelManager, ChallengeMode.ROYAL).open());
    }
}
