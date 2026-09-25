package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.DuelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public final class PlayerSelectGUI extends CustomGUI {

    private final DuelManager duelManager;
    private final boolean royal;

    public PlayerSelectGUI(Player player, DuelManager duelManager, boolean royal) {
        super(player, 54, MiniMessage.miniMessage().deserialize(
                royal ? "<gradient:#FFD700:#FFA500><b>👑 Выбор цели Королевской Дуэли</b></gradient>"
                      : "<gold><b>⚔ Выберите соперника для дуэли</b></gold>"
        ));
        this.duelManager = duelManager;
        this.royal = royal;
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new MainMenuGUI(player, duelManager).open());

        List<? extends Player> opponents = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(player))
                .filter(p -> !duelManager.getMatchManager().isInMatch(p.getUniqueId()))
                .limit(28)
                .toList();

        if (opponents.isEmpty()) {
            setItem(22, createNoPlayersItem(), null);
            return;
        }

        // Place opponents in work area (Row 2, 3, 4: slots 19-25, 28-34, 37-43)
        int[] validSlots = new int[]{
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < Math.min(opponents.size(), validSlots.length); i++) {
            Player opp = opponents.get(i);
            int slot = validSlots[i];

            setItem(slot, createOpponentHead(opp), e -> {
                // Open duel configuration GUI with chosen opponent
                new DuelSetupGUI(player, opp, duelManager, royal).open();
            });
        }
    }

    private ItemStack createOpponentHead(Player opp) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(opp);
            meta.displayName(MiniMessage.miniMessage().deserialize(
                    "<gold><b>" + opp.getName() + "</b>"
            ).decoration(TextDecoration.ITALIC, false));

            long cd = duelManager.getCooldownManager().getChallengeRemainingSeconds(player.getUniqueId(), opp.getUniqueId());
            String cdInfo = cd > 0 ? "<red>Кулдаун вызова: " + cd + "с" : "<green>Готов к вызову!";

            meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Здоровье: <red>" + (int) opp.getHealth() + "❤").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize("<gray>Пинг: <white>" + opp.getPing() + "ms").decoration(TextDecoration.ITALIC, false),
                    MiniMessage.miniMessage().deserialize(cdInfo).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize("<yellow>➤ Нажмите, чтобы настроить дуэль").decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNoPlayersItem() {
        ItemStack item = new ItemStack(Material.STRUCTURE_VOID);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gray>Нет доступных игроков онлайн").decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
