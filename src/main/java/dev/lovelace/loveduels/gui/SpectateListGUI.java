package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.util.CoinFormat;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.match.Match;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;

public final class SpectateListGUI extends CustomGUI {

    private final DuelManager duelManager;

    public SpectateListGUI(Player player, DuelManager duelManager) {
        super(player, 45, MiniMessage.miniMessage().deserialize("<light_purple><b>👁 Наблюдение за активными дуэлями</b></light_purple>"));
        this.duelManager = duelManager;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        Collection<Match> matches = duelManager.getMatchManager().getActiveMatches();
        int[] slots = new int[]{20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

        if (matches.isEmpty()) {
            setItem(22, createNoMatchesItem(), null);
            return;
        }

        int idx = 0;
        for (Match match : matches) {
            if (idx >= slots.length) break;
            int slot = slots[idx++];

            setItem(slot, createMatchItem(match), e -> {
                player.closeInventory();
                duelManager.getSpectatorManager().addSpectator(player, match);
            });
        }
    }

    private ItemStack createMatchItem(Match match) {
        String p1 = match.getPlayer1().getName();
        String p2 = match.getPlayer2().getName();
        String prefix = match.isRoyal() ? "<gradient:#FFD700:#FFA500>👑 " : "<gold>⚔ ";

        Component nameComp = MiniMessage.miniMessage().deserialize(
                prefix + "<b>" + p1 + " <gray>vs <white>" + p2 + "</b>"
        );

        long fee = duelManager.getSpectatorManager().calculateSpectatorFee(match);
        String feeStr = (fee <= 0) ? "<green>БЕСПЛАТНО" : CoinFormat.amount(fee);

        List<Component> lore = List.of(
                MiniMessage.miniMessage().deserialize("<gray>Режим: <white>" + match.getType().getDisplayNameMiniMessage()),
                MiniMessage.miniMessage().deserialize("<gray>Арена: <yellow>" + match.getArena().getName()),
                MiniMessage.miniMessage().deserialize("<gray>Зрителей: <aqua>" + match.getSpectators().size()),
                MiniMessage.miniMessage().deserialize(CoinFormat.resolveGlyphs(player, "<gray>Цена билета: " + feeStr)),
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<yellow>➤ Нажмите, чтобы перейти на трибуны")
        );

        return HeadTextures.playerHead(match.getPlayer1().getUniqueId(), nameComp, lore);
    }

    private ItemStack createNoMatchesItem() {
        return HeadTextures.head(
                HeadTextures.EYE,
                "<gray>В данный момент нет активных дуэлей",
                List.of(
                        "<gray>Сейчас все арены свободны.",
                        "<yellow>Вы можете бросить вызов сами!"
                )
        );
    }
}
