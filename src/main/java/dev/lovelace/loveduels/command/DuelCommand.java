package dev.lovelace.loveduels.command;

import dev.lovelace.loveduels.core.ChallengeMode;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.core.DuelRequest;
import dev.lovelace.loveduels.gui.DuelSetupGUI;
import dev.lovelace.loveduels.gui.MainMenuGUI;
import dev.lovelace.loveduels.gui.ReadinessGUI;
import dev.lovelace.loveduels.gui.SpectateListGUI;
import dev.lovelace.loveduels.match.Match;
import dev.lovelace.loveduels.match.MatchResult;
import dev.lovelace.loveduels.match.ReadinessSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DuelCommand implements CommandExecutor, TabCompleter {

    private final DuelManager duelManager;

    public DuelCommand(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Команда только для игроков."));
            return true;
        }

        if (args.length == 0) {
            new MainMenuGUI(player, duelManager).open();
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help", "помощь", "?", "commands", "команды" -> sendHelp(player);
            case "accept", "принять" -> handleAccept(player, args);
            case "deny", "отклонить", "decline" -> handleDeny(player, args);
            case "leave", "выйти", "покинуть" -> handleLeave(player);
            case "spectate", "наблюдать", "spec" -> handleSpectate(player, args);
            case "top", "топ", "leaderboard" -> {
                // Зал славы убран из главного меню; ранги по видам — в разработке
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<gray>Отдельные ранги по видам дуэлей (мечи, луки, кони, кулаки) — в разработке." +
                        " Статистика: <yellow>/duel stats</yellow>"
                ));
            }
            case "stats", "статистика" -> handleStats(player, args);
            case "rematch", "реванш", "revenge", "месть" -> handleRematch(player);
            case "forfeit", "surrender", "сдаться" -> handleForfeit(player);
            default -> {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target != null && !target.equals(player)) {
                    new DuelSetupGUI(player, target, duelManager, ChallengeMode.NORMAL).open();
                } else if (target != null) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Нельзя вызвать самого себя."));
                } else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<red>Игрок <gold>" + args[0] + "</gold> не найден. <yellow>/duel help</yellow>"
                    ));
                }
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        MiniMessage mm = MiniMessage.miniMessage();
        Component header = mm.deserialize("\n<gradient:#C9A227:#E8D48B>LoveDuels — команды</gradient>");
        Component divider = mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");

        Component help = Component.empty()
                .append(header).append(Component.newline())
                .append(divider).append(Component.newline())
                .append(formatCmd("/duel", "Главное меню")).append(Component.newline())
                .append(formatCmd("/duel <игрок>", "Вызов (обычная дуэль)")).append(Component.newline())
                .append(formatCmd("/duel accept", "Принять вызов")).append(Component.newline())
                .append(formatCmd("/duel deny", "Отклонить вызов")).append(Component.newline())
                .append(formatCmd("/duel spectate", "Наблюдение")).append(Component.newline())
                .append(formatCmd("/duel stats [игрок]", "Статистика")).append(Component.newline())
                .append(formatCmd("/duel rematch", "Реванш")).append(Component.newline())
                .append(formatCmd("/duel surrender", "Сдаться")).append(Component.newline())
                .append(formatCmd("/duel leave", "Выйти с трибун")).append(Component.newline())
                .append(divider).append(Component.newline());

        player.sendMessage(help);
    }

    private Component formatCmd(String cmd, String description) {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize("<yellow>" + cmd + "</yellow> <dark_gray>—</dark_gray> <gray>" + description + "</gray>")
                .clickEvent(ClickEvent.suggestCommand(cmd + " "))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<yellow>" + cmd + "</yellow>")));
    }

    private void handleAccept(Player player, String[] args) {
        Optional<DuelRequest> optReq = duelManager.getMatchManager().getPendingRequest(player.getUniqueId());
        if (optReq.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Нет входящих вызовов."));
            return;
        }

        DuelRequest req = optReq.get();
        Player challenger = Bukkit.getPlayer(req.senderId());
        if (challenger == null || !challenger.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Вызвавший игрок офлайн."));
            duelManager.getMatchManager().removePendingRequest(player.getUniqueId());
            return;
        }

        if (duelManager.getMatchManager().isInMatch(challenger.getUniqueId()) || duelManager.getMatchManager().isInMatch(player.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Один из бойцов уже в поединке."));
            return;
        }

        if (req.bet().hasMoney() && !req.isTraining()) {
            long money = req.bet().moneyBet();
            if (!duelManager.getEconomyBridge().has(player, money)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Недостаточно средств для ставки."));
                return;
            }
            if (!duelManager.getEconomyBridge().has(challenger, money)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>У соперника недостаточно средств."));
                return;
            }
            if (!duelManager.getEconomyBridge().charge(player, money)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Не удалось списать ставку."));
                return;
            }
            if (!duelManager.getEconomyBridge().charge(challenger, money)) {
                duelManager.getEconomyBridge().give(player, money);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>У соперника не списалась ставка. Ваша возвращена."));
                return;
            }
            duelManager.getEconomyBridge().holdEscrow(challenger.getUniqueId(), player.getUniqueId(), money);
        }

        duelManager.getMatchManager().removePendingRequest(player.getUniqueId());

        if (req.type().requiresReadinessSession()) {
            ReadinessSession session = new ReadinessSession(
                    challenger.getUniqueId(),
                    player.getUniqueId(),
                    () -> Bukkit.getScheduler().runTask(duelManager.getPlugin(), () -> {
                        if (challenger.isOnline()) challenger.closeInventory();
                        if (player.isOnline()) player.closeInventory();
                        duelManager.getMatchManager().createAndStartMatch(
                                challenger, player, req.type(), req.kitId(), req.bet(), req.royal()
                        );
                    }),
                    (cancelledBy) -> {
                        Bukkit.getScheduler().runTask(duelManager.getPlugin(), () -> {
                            if (challenger.isOnline() && challenger.getOpenInventory().getTopInventory().getHolder() instanceof ReadinessGUI) {
                                challenger.closeInventory();
                            }
                            if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof ReadinessGUI) {
                                player.closeInventory();
                            }
                        });
                        if (req.bet().hasMoney() && !req.isTraining()) {
                            duelManager.getEconomyBridge().give(challenger, req.bet().moneyBet());
                            duelManager.getEconomyBridge().give(player, req.bet().moneyBet());
                            duelManager.getEconomyBridge().releaseEscrow(challenger.getUniqueId());
                        }
                        Player canceller = cancelledBy != null ? Bukkit.getPlayer(cancelledBy) : null;
                        String cName = (canceller != null) ? canceller.getName()
                                : (cancelledBy == null ? "время истекло" : "Один из бойцов");
                        challenger.sendMessage(MiniMessage.miniMessage().deserialize("<red>Дуэль отменена (" + cName + ")."));
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Дуэль отменена (" + cName + ")."));
                    }
            );

            session.setStateChangeListener(s -> {
                if (s.isTerminated()) return;
                if (challenger.getOpenInventory().getTopInventory().getHolder() instanceof ReadinessGUI r1) r1.refresh();
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof ReadinessGUI r2) r2.refresh();
            });

            new ReadinessGUI(challenger, session).open();
            new ReadinessGUI(player, session).open();
            return;
        }

        duelManager.getMatchManager().createAndStartMatch(
                challenger, player, req.type(), req.kitId(), req.bet(), req.royal()
        );
    }

    private void handleDeny(Player player, String[] args) {
        Optional<DuelRequest> optReq = duelManager.getMatchManager().getPendingRequest(player.getUniqueId());
        if (optReq.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Нет входящих вызовов."));
            return;
        }
        DuelRequest req = optReq.get();
        duelManager.getMatchManager().removePendingRequest(player.getUniqueId());
        Player challenger = Bukkit.getPlayer(req.senderId());
        if (challenger != null && challenger.isOnline()) {
            challenger.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Игрок <gold>" + player.getName() + "</gold> отклонил вызов."
            ));
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Вызов отклонён."));
    }

    private void handleLeave(Player player) {
        if (duelManager.getSpectatorManager().isSpectating(player.getUniqueId())) {
            duelManager.getSpectatorManager().removeSpectator(player);
            return;
        }
        if (duelManager.getMatchManager().isInMatch(player.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Вы в бою. Сдаться: <yellow>/duel surrender</yellow>."
            ));
            return;
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Вы не в дуэли и не среди зрителей."));
    }

    private void handleSpectate(Player player, String[] args) {
        if (args.length > 1) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                Optional<Match> m = duelManager.getMatchManager().getMatch(target.getUniqueId());
                if (m.isPresent()) {
                    duelManager.getSpectatorManager().addSpectator(player, m.get());
                    return;
                }
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Игрок не найден или не в бою."));
            return;
        }
        new SpectateListGUI(player, duelManager).open();
    }

    private void handleStats(Player player, String[] args) {
        Player target = (args.length > 1) ? Bukkit.getPlayerExact(args[1]) : player;
        if (target == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Игрок не найден."));
            return;
        }

        duelManager.getPlayerStorage().getOrCreatePlayer(target.getUniqueId(), target.getName()).thenAccept(data -> {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "\n<gradient:#C9A227:#E8D48B>Профиль: " + data.name() + "</gradient>\n" +
                    "<dark_gray>━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                    "<gray>Честь: <gold>" + data.honor() + "</gold>\n" +
                    "<gray>Побед: <green>" + data.wins() + "</green> · Поражений: <red>" + data.losses() + "</red>\n" +
                    "<gray>WR: <yellow>" + String.format("%.1f%%", data.winRate()) + "</yellow>\n" +
                    "<gray>Серия: <white>" + data.currentStreak() + "</white> (лучшая " + data.bestStreak() + ")\n" +
                    "<gray>Королевских побед: <gold>" + data.royalWins() + "</gold>\n" +
                    "<dark_gray>━━━━━━━━━━━━━━━━━━━━</dark_gray>\n"
            ));
        });
    }

    private void handleRematch(Player player) {
        Optional<MatchResult> optRes = duelManager.getMatchManager().getLastResult(player.getUniqueId());
        if (optRes.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Нет недавних дуэлей для реванша."));
            return;
        }
        MatchResult res = optRes.get();
        UUID oppId = res.getOpponentId(player.getUniqueId());
        Player opp = (oppId != null) ? Bukkit.getPlayer(oppId) : null;
        if (opp == null || !opp.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Соперник офлайн."));
            return;
        }
        ChallengeMode mode = res.royal() ? ChallengeMode.ROYAL : ChallengeMode.NORMAL;
        new DuelSetupGUI(player, opp, duelManager, mode).open();
    }

    private void handleForfeit(Player player) {
        Optional<Match> matchOpt = duelManager.getMatchManager().getMatch(player.getUniqueId());
        if (matchOpt.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Вы не в дуэли."));
            return;
        }
        Match match = matchOpt.get();
        match.forfeit(player);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String p = args[0].toLowerCase();
            for (String s : List.of("accept", "deny", "leave", "spectate", "stats", "rematch", "surrender", "help")) {
                if (s.startsWith(p)) out.add(s);
            }
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("spectate"))) {
            String p = args[1].toLowerCase();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
            }
        }
        return out;
    }
}
