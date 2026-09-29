package dev.lovelace.loveduels.command;

import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.core.DuelRequest;
import dev.lovelace.loveduels.gui.DuelSetupGUI;
import dev.lovelace.loveduels.gui.LeaderboardGUI;
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
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Команда доступна только для игроков."));
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
            case "top", "топ", "leaderboard" -> new LeaderboardGUI(player, duelManager).open();
            case "stats", "статистика" -> handleStats(player, args);
            case "rematch", "реванш", "revenge", "месть" -> handleRematch(player);
            case "forfeit", "surrender", "сдаться" -> handleForfeit(player);
            default -> {
                // Check if args[0] is an online player name for fast challenge
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target != null && !target.equals(player)) {
                    new DuelSetupGUI(player, target, duelManager, false).open();
                } else if (target != null && target.equals(player)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Вы не можете вызвать на дуэль самого себя!"));
                } else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<red>❌ Игрок <gold>" + args[0] + "</gold> не найден или офлайн. Введите <yellow>/duel help</yellow> для списка команд."
                    ));
                }
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        MiniMessage mm = MiniMessage.miniMessage();
        Component header = mm.deserialize("\n<gradient:#FFD700:#FFA500><b>⚔ LoveDuels — Справка по командам поединков</b></gradient>");
        Component divider = mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");

        Component help = Component.empty()
                .append(header).append(Component.newline())
                .append(divider).append(Component.newline())
                .append(formatCmd("/duel", "Открыть главное интерактивное меню дуэлей и турниров")).append(Component.newline())
                .append(formatCmd("/duel <игрок>", "Настроить и бросить вызов конкретному игроку")).append(Component.newline())
                .append(formatCmd("/duel accept", "Принять входящий вызов на поединок (/принять)")).append(Component.newline())
                .append(formatCmd("/duel deny", "Отклонить входящий вызов на дуэль (/отклонить)")).append(Component.newline())
                .append(formatCmd("/duel spectate", "Открыть список активных дуэлей для наблюдения (/наблюдать)")).append(Component.newline())
                .append(formatCmd("/duel top", "Открыть Зал Славы — рейтинг лучших дуэлянтов (/топ)")).append(Component.newline())
                .append(formatCmd("/duel stats [игрок]", "Посмотреть статистику побед, поражений и Чести (/статистика)")).append(Component.newline())
                .append(formatCmd("/duel rematch", "Предложить мгновенный реванш после поединка (/реванш)")).append(Component.newline())
                .append(formatCmd("/duel surrender", "Сдаться сопернику во время боя или перемирия (/сдаться)")).append(Component.newline())
                .append(formatCmd("/duel leave", "Покинуть трибуны зрителей (/выйти)")).append(Component.newline())
                .append(divider).append(Component.newline())
                .append(mm.deserialize("<gray><i>💡 Нажмите на любую команду в чате для автозаполнения</i></gray>\n"));

        player.sendMessage(help);
    }

    private Component formatCmd(String cmd, String description) {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize("<yellow><b>" + cmd + "</b></yellow> <dark_gray>—</dark_gray> <gray>" + description + "</gray>")
                .clickEvent(ClickEvent.suggestCommand(cmd + " "))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<yellow>Нажмите, чтобы ввести: <white>" + cmd + "</white></yellow>")));
    }

    private void handleAccept(Player player, String[] args) {
        Optional<DuelRequest> optReq = duelManager.getMatchManager().getPendingRequest(player.getUniqueId());
        if (optReq.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ У вас нет активных входящих вызовов на дуэль."));
            return;
        }

        DuelRequest req = optReq.get();
        Player challenger = Bukkit.getPlayer(req.senderId());
        if (challenger == null || !challenger.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Вызвавший вас игрок покинул сервер."));
            duelManager.getMatchManager().removePendingRequest(player.getUniqueId());
            return;
        }

        if (duelManager.getMatchManager().isInMatch(challenger.getUniqueId()) || duelManager.getMatchManager().isInMatch(player.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Один из бойцов уже находится в поединке!"));
            return;
        }

        // Verify balances for betting
        if (req.bet().hasMoney()) {
            long money = req.bet().moneyBet();
            if (!duelManager.getEconomyBridge().has(player, money)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ У вас недостаточно монет для ставки (<gold>" + money + " монет</gold>)!"));
                return;
            }
            if (!duelManager.getEconomyBridge().has(challenger, money)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ У соперника недостаточно монет для этой ставки!"));
                return;
            }

            // Charge both players. has() was true a moment ago, but charge() is the authority: if the
            // second one fails, give the first one's stake back instead of starting an unfunded duel.
            if (!duelManager.getEconomyBridge().charge(player, money)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Не удалось списать ставку (<gold>" + money + " монет</gold>)!"));
                return;
            }
            if (!duelManager.getEconomyBridge().charge(challenger, money)) {
                duelManager.getEconomyBridge().give(player, money);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ У соперника не удалось списать ставку. Ваша ставка возвращена."));
                return;
            }
            duelManager.getEconomyBridge().holdEscrow(challenger.getUniqueId(), player.getUniqueId(), money);
        }

        duelManager.getMatchManager().removePendingRequest(player.getUniqueId());

        // If OWN_INVENTORY: open readiness GUI for both players
        if (req.type().requiresReadinessSession()) {
            ReadinessSession session = new ReadinessSession(
                    challenger.getUniqueId(),
                    player.getUniqueId(),
                    () -> {
                        // On both ready: close inventories on next tick and start match
                        Bukkit.getScheduler().runTask(duelManager.getPlugin(), () -> {
                            if (challenger.isOnline()) {
                                challenger.closeInventory();
                            }
                            if (player.isOnline()) {
                                player.closeInventory();
                            }
                            duelManager.getMatchManager().createAndStartMatch(
                                    challenger, player, req.type(), req.kitId(), req.bet(), req.royal()
                            );
                        });
                    },
                    (cancelledBy) -> {
                        // Close inventories on next tick to avoid recursive closeContainer loop
                        Bukkit.getScheduler().runTask(duelManager.getPlugin(), () -> {
                            if (challenger.isOnline() && challenger.getOpenInventory().getTopInventory().getHolder() instanceof ReadinessGUI) {
                                challenger.closeInventory();
                            }
                            if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof ReadinessGUI) {
                                player.closeInventory();
                            }
                        });
                        // Refund money
                        if (req.bet().hasMoney()) {
                            duelManager.getEconomyBridge().give(challenger, req.bet().moneyBet());
                            duelManager.getEconomyBridge().give(player, req.bet().moneyBet());
                            duelManager.getEconomyBridge().releaseEscrow(challenger.getUniqueId());
                        }
                        // cancelledBy is null when the session expired without both fighters confirming
                        Player canceller = cancelledBy != null ? Bukkit.getPlayer(cancelledBy) : null;
                        String cName = (canceller != null) ? canceller.getName()
                                : (cancelledBy == null ? "время ожидания подтверждения истекло" : "Один из бойцов");
                        challenger.sendMessage(MiniMessage.miniMessage().deserialize("<red>✖ Дуэль была отменена (" + cName + ")."));
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>✖ Дуэль была отменена (" + cName + ")."));
                    }
            );

            session.setStateChangeListener(s -> {
                if (s.isTerminated()) {
                    return;
                }
                // Refresh items without reopening inventory to prevent close events
                if (challenger.getOpenInventory().getTopInventory().getHolder() instanceof ReadinessGUI r1) {
                    r1.refresh();
                }
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof ReadinessGUI r2) {
                    r2.refresh();
                }
            });

            new ReadinessGUI(challenger, session).open();
            new ReadinessGUI(player, session).open();
            return;
        }

        // Other modes: start match immediately
        duelManager.getMatchManager().createAndStartMatch(
                challenger, player, req.type(), req.kitId(), req.bet(), req.royal()
        );
    }

    private void handleDeny(Player player, String[] args) {
        Optional<DuelRequest> optReq = duelManager.getMatchManager().getPendingRequest(player.getUniqueId());
        if (optReq.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ У вас нет активных входящих вызовов."));
            return;
        }

        DuelRequest req = optReq.get();
        duelManager.getMatchManager().removePendingRequest(player.getUniqueId());

        Player challenger = Bukkit.getPlayer(req.senderId());
        if (challenger != null && challenger.isOnline()) {
            challenger.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>✖ Игрок <gold>" + player.getName() + "</gold> отклонил ваш вызов на дуэль."
            ));
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>✖ Вы отклонили вызов на дуэль."));
    }

    private void handleLeave(Player player) {
        if (duelManager.getSpectatorManager().isSpectating(player.getUniqueId())) {
            duelManager.getSpectatorManager().removeSpectator(player);
            return;
        }

        if (duelManager.getMatchManager().isInMatch(player.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>❌ Вы находитесь в бою! Чтобы сдаться, используйте: <yellow>/duel surrender</yellow>."
            ));
            return;
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>❌ Вы не находитесь в дуэли или среди зрителей."));
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
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Игрок не найден или не участвует в бою."));
            return;
        }
        new SpectateListGUI(player, duelManager).open();
    }

    private void handleStats(Player player, String[] args) {
        Player target = (args.length > 1) ? Bukkit.getPlayerExact(args[1]) : player;
        if (target == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Игрок не найден или офлайн."));
            return;
        }

        duelManager.getPlayerStorage().getOrCreatePlayer(target.getUniqueId(), target.getName()).thenAccept(data -> {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "\n<gradient:#FFD700:#FFA500><b>⚔ Боевой профиль: " + data.name() + "</b></gradient>\n" +
                    "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                    "<gray>▪ Рейтинг Чести: <gold><b>" + data.honor() + "</b></gold>\n" +
                    "<gray>▪ Побед: <green><b>" + data.wins() + "</b></green> <dark_gray>|</dark_gray> Поражений: <red><b>" + data.losses() + "</b></red>\n" +
                    "<gray>▪ Процент побед (WR): <yellow><b>" + String.format("%.1f%%", data.winRate()) + "</b></yellow>\n" +
                    "<gray>▪ Текущая серия: <white><b>" + data.currentStreak() + "</b></white> <dark_gray>(Лучшая: " + data.bestStreak() + ")</dark_gray>\n" +
                    "<gray>▪ Королевских триумфов: <gold>👑 <b>" + data.royalWins() + "</b></gold>\n" +
                    "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n"
            ));
        });
    }

    private void handleRematch(Player player) {
        Optional<MatchResult> optRes = duelManager.getMatchManager().getLastResult(player.getUniqueId());
        if (optRes.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ У вас нет недавних завершённых дуэлей для предложения реванша."));
            return;
        }

        MatchResult res = optRes.get();
        UUID oppId = res.getOpponentId(player.getUniqueId());
        Player opp = (oppId != null) ? Bukkit.getPlayer(oppId) : null;

        if (opp == null || !opp.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Ваш бывший соперник уже вышел с сервера."));
            return;
        }

        new DuelSetupGUI(player, opp, duelManager, res.royal()).open();
    }

    private void handleForfeit(Player player) {
        Optional<Match> matchOpt = duelManager.getMatchManager().getMatch(player.getUniqueId());
        if (matchOpt.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Вы не участвуете в дуэли."));
            return;
        }

        Match match = matchOpt.get();
        if (match.isEnded()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Бой уже завершён."));
            return;
        }

        match.surrender(player);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = List.of(
                    "help", "accept", "deny", "leave", "spectate", "top", "stats", "rematch", "forfeit", "surrender",
                    "помощь", "принять", "отклонить", "выйти", "наблюдать", "топ", "статистика", "реванш", "сдаться", "месть"
            );
            List<String> res = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) res.add(s);
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) res.add(p.getName());
            }
            return res;
        }
        return List.of();
    }
}
