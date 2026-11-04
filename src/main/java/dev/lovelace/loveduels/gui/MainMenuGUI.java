package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Главное меню: только вызов и наблюдение (gui-gen-6, 27 слотов).
 */
public final class MainMenuGUI extends CustomGUI {

    private final DuelManager duelManager;

    public MainMenuGUI(Player player, DuelManager duelManager) {
        super(player, 27, dev.lovelace.loveduels.util.Lang.component("gui.main_menu.title"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(false, null);
        var cached = duelManager.getPlayerStorage().getCachedPlayer(player.getUniqueId());
        var info = cached.map(data -> dev.lovelace.loveduels.util.Lang.list("gui.main_menu.info.lore",
                "honor", String.valueOf(data.honor()), "wins", String.valueOf(data.wins()),
                "losses", String.valueOf(data.losses()), "streak", String.valueOf(data.currentStreak()),
                "best", String.valueOf(data.bestStreak()), "royal", String.valueOf(data.royalWins()),
                "active", String.valueOf(duelManager.getMatchManager().getActiveMatches().size())))
                .orElse(List.of("<gray>Статистика загружается…</gray>"));
        var mini = MiniMessage.miniMessage();
        setItem(0, HeadTextures.playerHead(player,
                dev.lovelace.loveduels.util.Lang.component("gui.main_menu.info.name", "player", player.getName()),
                info.stream().map(mini::deserialize).toList()), null);

        // Рабочая зона: два пункта по центру (слоты 12 и 14)
        setItem(12, HeadTextures.head("actions.challenge",
                dev.lovelace.loveduels.util.Lang.get("gui.main_menu.challenge.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.main_menu.challenge.lore")
        ), e -> new ChallengeModeGUI(player, duelManager).open());

        setItem(14, HeadTextures.head(duelManager.getMatchManager().getActiveMatches().isEmpty() ? "actions.spectate_empty" : "actions.spectate",
                dev.lovelace.loveduels.util.Lang.get("gui.main_menu.spectate.name"),
                dev.lovelace.loveduels.util.Lang.list("gui.main_menu.spectate.lore")
        ), e -> new SpectateListGUI(player, duelManager).open());
    }
}
