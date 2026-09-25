package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.match.Match;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        ItemStack item = new ItemStack(match.getType().getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String p1 = match.getPlayer1().getName();
            String p2 = match.getPlayer2().getName();
            String prefix = match.isRoyal() ? "<gradient:#FFD700:#FFA500>👑 " : "<gold>⚔ ";

            meta.displayName(MiniMessage.miniMessage().deserialize(
                    prefix + "<b>" + p1 + " <gray>vs <white>" + p2 + "</b>"
            ).decoration(TextDecoration.ITALIC, false));

            long fee = duelManager.getSpectatorManager().calculateSpectatorFee(match);
            String feeStr = (fee <= 0) ? "<green>БЕСПЛАТНО" : "<gold>" + fee + " монет";

            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Режим: <white>" + match.getType().getDisplayNameMiniMessage()).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Арена: <yellow>" + match.getArena().getName()).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Зрителей: <aqua>" + match.getSpectators().size()).decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Цена места: " + feeStr).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize("<yellow>➤ Нажмите, чтобы перейти на трибуны").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNoMatchesItem() {
        ItemStack item = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gray>В данный момент нет активных дуэлей").decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
