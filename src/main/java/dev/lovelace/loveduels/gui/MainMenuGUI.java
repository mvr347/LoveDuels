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
        super(player, 45, dev.lovelace.loveduels.util.Lang.component("gui.main_menu.title"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(false, null);

        // Рабочая зона: два пункта по центру (слоты 21 и 23)
        setItem(21, HeadTextures.head(HeadTextures.SWORD,
                dev.lovelace.loveduels.util.Lang.get("gui.main_menu.challenge.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.main_menu.challenge.lore")
        ), e -> new ChallengeModeGUI(player, duelManager).open());

        setItem(23, HeadTextures.head(HeadTextures.EYE,
                dev.lovelace.loveduels.util.Lang.get("gui.main_menu.spectate.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.main_menu.spectate.lore")
        ), e -> new SpectateListGUI(player, duelManager).open());
    }
}
