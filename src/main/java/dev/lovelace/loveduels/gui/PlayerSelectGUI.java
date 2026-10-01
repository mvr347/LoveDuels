package dev.lovelace.loveduels.gui;

import dev.lovelace.loveduels.core.ChallengeMode;
import dev.lovelace.loveduels.core.DuelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class PlayerSelectGUI extends CustomGUI {

    private final DuelManager duelManager;
    private final ChallengeMode mode;

    public PlayerSelectGUI(Player player, DuelManager duelManager, ChallengeMode mode) {
        super(player, 54, MiniMessage.miniMessage().deserialize(titleFor(mode)));
        this.duelManager = duelManager;
        this.mode = mode;
    }

    /** @deprecated use ChallengeMode overload */
    @Deprecated
    public PlayerSelectGUI(Player player, DuelManager duelManager, boolean royal) {
        this(player, duelManager, royal ? ChallengeMode.ROYAL : ChallengeMode.NORMAL);
    }

    private static String titleFor(ChallengeMode mode) {
        return switch (mode) {
            case TRAINING -> "<aqua>Соперник · тренировка</aqua>";
            case ROYAL -> "<gradient:#C9A227:#E8D48B>Соперник · королевская</gradient>";
            default -> "<gold>Соперник · дуэль</gold>";
        };
    }

    @Override
    protected void build() {
        applyStandardBorders(true, () -> new ChallengeModeGUI(player, duelManager).open());

        List<? extends Player> opponents = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(player))
                .filter(p -> !duelManager.getMatchManager().isInMatch(p.getUniqueId()))
                .limit(28)
                .toList();

        if (opponents.isEmpty()) {
            setItem(22, createNoPlayersItem(), null);
            return;
        }

        int[] validSlots = new int[]{
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < Math.min(opponents.size(), validSlots.length); i++) {
            Player opp = opponents.get(i);
            int slot = validSlots[i];
            setItem(slot, createOpponentHead(opp), e ->
                    new DuelSetupGUI(player, opp, duelManager, mode).open());
        }
    }

    private ItemStack createOpponentHead(Player opp) {
        Component name = MiniMessage.miniMessage().deserialize("<gold>" + opp.getName() + "</gold>");

        long cd = duelManager.getCooldownManager().getChallengeRemainingSeconds(player.getUniqueId(), opp.getUniqueId());
        String cdInfo = cd > 0
                ? "<red>Кулдаун вызова: " + cd + "с</red>"
                : "<green>Готов к вызову</green>";

        List<Component> lore = List.of(
                MiniMessage.miniMessage().deserialize("<gray>Здоровье: <red>" + (int) opp.getHealth() + "</red>"),
                MiniMessage.miniMessage().deserialize("<gray>Пинг: <white>" + opp.getPing() + " ms</white>"),
                MiniMessage.miniMessage().deserialize(cdInfo),
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<yellow>ЛКМ</yellow> <dark_gray>—</dark_gray> <white>настроить дуэль</white>")
        );

        return HeadTextures.playerHead(opp, name, lore);
    }

    private ItemStack createNoPlayersItem() {
        return HeadTextures.head(
                HeadTextures.EYE,
                "<gray>Нет доступных игроков</gray>",
                List.of(
                        "<gray>Все оффлайн или уже в бою.",
                        "<yellow>Пригласите кого-нибудь на сервер.</yellow>"
                )
        );
    }
}
