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
        super(player, 27, dev.lovelace.loveduels.util.Lang.component("gui.challenge_mode.title"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        setItem(11, HeadTextures.head(HeadTextures.READY,
                dev.lovelace.loveduels.util.Lang.get("gui.challenge_mode.training.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.challenge_mode.training.lore")
        ), e -> new PlayerSelectGUI(player, duelManager, ChallengeMode.TRAINING).open());

        setItem(13, HeadTextures.head(HeadTextures.SWORD,
                dev.lovelace.loveduels.util.Lang.get("gui.challenge_mode.normal.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.challenge_mode.normal.lore")
        ), e -> new PlayerSelectGUI(player, duelManager, ChallengeMode.NORMAL).open());

        setItem(15, HeadTextures.head(HeadTextures.CROWN,
                dev.lovelace.loveduels.util.Lang.get("gui.challenge_mode.royal.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.challenge_mode.royal.lore")
        ), e -> new PlayerSelectGUI(player, duelManager, ChallengeMode.ROYAL).open());
    }
}
