package dev.lovelace.loveduels.command;

import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.core.DuelRequest;
import dev.lovelace.loveduels.core.DuelType;
import dev.lovelace.loveduels.gui.*;
import dev.lovelace.loveduels.match.Match;
import dev.lovelace.loveduels.match.MatchResult;
import dev.lovelace.loveduels.match.ReadinessSession;
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
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Команда доступна только для игроков."));
            return true;
        }

        if (args.length == 0) {
            new MainMenuGUI(player, duelManager).open();
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "accept", "принять" -> handleAccept(player, args);
            case "deny", "отклонить", "decline" -> handleDeny(player, args);
            case "leave", "выйти", "покинуть" -> handleLeave(player);
            case "spectate", "наблюдать", "spec" -> handleSpectate(player, args);
            case "top", "топ", "leaderboard" -> new LeaderboardGUI(player, duelManager).open();
            case "stats", "статистика" -> handleStats(player, args);
            case "rematch", "реванш" -> handleRematch(player);
            default -> {
                // Check if args[0] is an online player name for fast challenge
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target != null && !target.equals(player)) {
                    new DuelSetupGUI(player, target, duelManager, false).open();
                } else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<red>Игрок <gold>" + args[0] + "</gold> не найден или офлайн."
                    ));
                }
            }
        }

        return true;
    }

    private void handleAccept(Player player, String[] args) {
        Optional<DuelRequest> optReq = duelManager.getMatchManager().getPendingRequest(player.getUniqueId());
        if (optReq.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>У вас нет активных входящих вызовов на дуэль."));
            return;
        }

        DuelRequest req = optReq.get();
        Player challenger = Bukkit.getPlayer(req.senderId());
        if (challenger == null || !challenger.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Вызвавший вас игрок покинул сервер."));
            duelManager.getMatchManager().removePendingRequest(player.getUniqueId());
            return;
        }

        if (duelManager.getMatchManager().isInMatch(challenger.getUniqueId()) || duelManager.getMatchManager().isInMatch(player.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Один из игроков уже находится в бою!"));
            return;
        }

        // Verify balances for betting
        if (req.bet().hasMoney()) {
            long money = req.bet().moneyBet();
            if (!duelManager.getEconomyBridge().has(player, money)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>У вас недостаточно денег для этой ставки (" + money + " монет)!"));
                return;
            }
            if (!duelManager.getEconomyBridge().has(challenger, money)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>У соперника не хватает денег для ставки!"));
                return;
            }

            // Charge both players
            duelManager.getEconomyBridge().charge(player, money);
            duelManager.getEconomyBridge().charge(challenger, money);
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
                        }
                        Player canceller = Bukkit.getPlayer(cancelledBy);
                        String cName = (canceller != null) ? canceller.getName() : "Один из игроков";
                        challenger.sendMessage(MiniMessage.miniMessage().deserialize("<red>Дуэль отменена (" + cName + ")."));
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Дуэль отменена (" + cName + ")."));
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
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>У вас нет активных вызовов."));
            return;
        }

        DuelRequest req = optReq.get();
        duelManager.getMatchManager().removePendingRequest(player.getUniqueId());

        Player challenger = Bukkit.getPlayer(req.senderId());
        if (challenger != null && challenger.isOnline()) {
            challenger.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Игрок <gold>" + player.getName() + "</gold> отклонил ваш вызов на дуэль."
            ));
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Вы отклонили вызов на дуэль."));
    }

    private void handleLeave(Player player) {
        if (duelManager.getSpectatorManager().isSpectating(player.getUniqueId())) {
            duelManager.getSpectatorManager().removeSpectator(player);
            return;
        }

        if (duelManager.getMatchManager().isInMatch(player.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Вы находитесь в бою! Сдаться: /duel forfeit (будет засчитано бегство)."
            ));
            return;
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Вы не находитесь в дуэли или режиме зрителя."));
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
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Игрок не найден или не находится в бою."));
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
                    "<gold><b>⚔ Статистика игрока " + data.name() + ":</b>\n" +
                    "<gray>Честь: <gold>" + data.honor() + "\n" +
                    "<gray>Побед: <green>" + data.wins() + " <gray>| Поражений: <red>" + data.losses() + " <gray>(Винрейт: <yellow>" + String.format("%.1f%%", data.winRate()) + "<gray>)\n" +
                    "<gray>Серия: <white>" + data.currentStreak() + " <gray>(Лучшая: " + data.bestStreak() + ")\n" +
                    "<gray>Королевских побед: <gold>" + data.royalWins()
            ));
        });
    }

    private void handleRematch(Player player) {
        Optional<MatchResult> optRes = duelManager.getMatchManager().getLastResult(player.getUniqueId());
        if (optRes.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>У вас нет недавних завершённых дуэлей для реванша."));
            return;
        }

        MatchResult res = optRes.get();
        UUID oppId = res.getOpponentId(player.getUniqueId());
        Player opp = (oppId != null) ? Bukkit.getPlayer(oppId) : null;

        if (opp == null || !opp.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Соперник офлайн."));
            return;
        }

        new DuelSetupGUI(player, opp, duelManager, res.royal()).open();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = List.of("accept", "deny", "leave", "spectate", "top", "stats", "rematch");
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
