package dev.lovelace.loveduels.command;

import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.kit.Kit;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class LoveDuelsAdminCommand implements CommandExecutor, TabCompleter {

    private final DuelManager duelManager;
    private final Map<UUID, Location> bound1Points = new HashMap<>();

    public LoveDuelsAdminCommand(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("loveduels.admin")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>У вас нет прав администратора LoveDuels."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "arena", "арена" -> handleArena(sender, args);
            case "kit", "кит" -> handleKit(sender, args);
            case "give", "выдать" -> handleGive(sender, args);
            case "forceend", "завершить" -> handleForceEnd(sender, args);
            case "sethonor", "честь" -> handleSetHonor(sender, args);
            case "reload", "перезагрузить" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleArena(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Использование: /lda arena <create|setpos1|setpos2|setspectator|setbound1|setbound2|setspecbound1|setspecbound2|delete|list>"));
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "create", "создать" -> {
                if (args.length < 3) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Укажите ID арены: /lda arena create <id> [название]"));
                    return;
                }
                String id = args[2].toLowerCase();
                String name = (args.length > 3) ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : id;
                Arena arena = new Arena(id, name);
                duelManager.getArenaManager().registerArena(arena);
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Арена <gold>" + id + "</gold> («" + name + "») успешно создана!"));
            }
            case "setpos1" -> {
                if (!(sender instanceof Player p)) return;
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Укажите ID: /lda arena setpos1 <id>"));
                    return;
                }
                Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
                if (opt.isEmpty()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Арена не найдена."));
                    return;
                }
                opt.get().setPos1(p.getLocation());
                duelManager.getArenaManager().saveArenas();
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Точка спавна 1 для арены " + args[2] + " установлена!"));
            }
            case "setpos2" -> {
                if (!(sender instanceof Player p)) return;
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Укажите ID: /lda arena setpos2 <id>"));
                    return;
                }
                Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
                if (opt.isEmpty()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Арена не найдена."));
                    return;
                }
                opt.get().setPos2(p.getLocation());
                duelManager.getArenaManager().saveArenas();
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Точка спавна 2 для арены " + args[2] + " установлена!"));
            }
            case "setspectator" -> {
                if (!(sender instanceof Player p)) return;
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Укажите ID: /lda arena setspectator <id>"));
                    return;
                }
                Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
                if (opt.isEmpty()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Арена не найдена."));
                    return;
                }
                opt.get().setSpectatorSpawn(p.getLocation());
                duelManager.getArenaManager().saveArenas();
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Точка наблюдения для арены " + args[2] + " установлена!"));
            }
            case "setbound1" -> {
                if (!(sender instanceof Player p)) return;
                bound1Points.put(p.getUniqueId(), p.getLocation());
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Точка 1 боевой зоны сохранена. Встаньте в точку 2 и введите: /lda arena setbound2 <id>"));
            }
            case "setbound2" -> {
                if (!(sender instanceof Player p)) return;
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Укажите ID: /lda arena setbound2 <id>"));
                    return;
                }
                Location b1 = bound1Points.get(p.getUniqueId());
                if (b1 == null) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Сначала установите первую точку: /lda arena setbound1"));
                    return;
                }
                Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
                if (opt.isEmpty()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Арена не найдена."));
                    return;
                }
                BoundingBox box = BoundingBox.of(b1, p.getLocation());
                opt.get().setBounds(box);
                duelManager.getArenaManager().saveArenas();
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Границы боевой зоны для арены " + args[2] + " успешно сохранены!"));
            }
            case "setspecbound1" -> {
                if (!(sender instanceof Player p)) return;
                bound1Points.put(p.getUniqueId(), p.getLocation());
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Точка 1 зоны зрителей сохранена. Встаньте в точку 2 и введите: /lda arena setspecbound2 <id>"));
            }
            case "setspecbound2" -> {
                if (!(sender instanceof Player p)) return;
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Укажите ID: /lda arena setspecbound2 <id>"));
                    return;
                }
                Location b1 = bound1Points.get(p.getUniqueId());
                if (b1 == null) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Сначала установите первую точку: /lda arena setspecbound1"));
                    return;
                }
                Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
                if (opt.isEmpty()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Арена не найдена."));
                    return;
                }
                BoundingBox box = BoundingBox.of(b1, p.getLocation());
                opt.get().setSpectatorZone(box);
                duelManager.getArenaManager().saveArenas();
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Границы трибун (зоны зрителей) для арены " + args[2] + " успешно сохранены!"));
            }
            case "delete", "удалить" -> {
                if (args.length < 3) return;
                if (duelManager.getArenaManager().deleteArena(args[2])) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Арена " + args[2] + " удалена."));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Арена не найдена."));
                }
            }
            case "list", "список" -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<gold><b>Список арен:</b></gold>"));
                for (Arena a : duelManager.getArenaManager().getAllArenas()) {
                    String cfg = a.isConfigured() ? "<green>[Готова]" : "<red>[Не настроена]";
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<yellow>▪ <white>" + a.getId() + " <gray>(«" + a.getName() + "») " + cfg
                    ));
                }
            }
            default -> sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Неизвестное действие арены."));
        }
    }

    private void handleKit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Использование: /lda kit <create|delete|list>"));
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "create", "создать" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Только игрок может создать кит из инвентаря."));
                    return;
                }
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Укажите ID: /lda kit create <id> [название]"));
                    return;
                }
                String id = args[2].toLowerCase();
                String displayName = (args.length > 3) ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : id;
                Kit kit = Kit.fromPlayer(id, displayName, p);
                duelManager.getKitManager().registerKit(kit);
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Кит <gold>" + id + "</gold> («" + displayName + "») создан из вашего инвентаря!"));
            }
            case "delete", "удалить" -> {
                if (args.length < 3) return;
                if (duelManager.getKitManager().deleteKit(args[2])) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Кит " + args[2] + " удалён."));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Кит не найден."));
                }
            }
            case "list", "список" -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<aqua><b>Список китов:</b></aqua>"));
                for (Kit k : duelManager.getKitManager().getAllKits()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<yellow>▪ <white>" + k.id() + " <gray>(«" + k.displayName() + "»)"
                    ));
                }
            }
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        // /lda give ticket <player> [amount]
        if (args.length < 3 || !args[1].equalsIgnoreCase("ticket")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Использование: /lda give ticket <игрок> [количество]"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Игрок не найден."));
            return;
        }

        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Некорректное число количества билетов: " + args[3]));
                return;
            }
        }

        ItemStack ticket = duelManager.getRoyalManager().getTicketItem().create(amount);
        target.getInventory().addItem(ticket);
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Выдано " + amount + " шт. Билетов Королевской Дуэли игроку " + target.getName()));
    }

    private void handleForceEnd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Использование: /lda forceend <игрок>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Игрок не найден."));
            return;
        }

        if (duelManager.getMatchManager().forceEnd(target)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Дуэль с участием " + target.getName() + " принудительно завершена."));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Игрок не находится в активной дуэли."));
        }
    }

    private void handleSetHonor(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Использование: /lda sethonor <игрок> <значение>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Игрок не найден."));
            return;
        }

        int val;
        try {
            val = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Некорректное число."));
            return;
        }

        duelManager.getPlayerStorage().getOrCreatePlayer(target.getUniqueId(), target.getName()).thenAccept(data -> {
            var updated = data.withHonor(val);
            duelManager.getPlayerStorage().savePlayer(updated);
            duelManager.getLeaderboardsBridge().syncPlayerData(updated);
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Честь игрока " + target.getName() + " установлена на " + val));
        });
    }

    private void handleReload(CommandSender sender) {
        duelManager.getPlugin().reloadConfig();
        duelManager.getArenaManager().loadArenas();
        duelManager.getKitManager().loadKits();
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Конфигурация, арены и киты LoveDuels перезагружены!"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold><b>⚔ LoveDuels Административные команды:</b>\n" +
                "<yellow>/lda arena <create|setpos1|setpos2|setspectator|setbound1|setbound2|list>\n" +
                "<yellow>/lda kit <create|delete|list>\n" +
                "<yellow>/lda give ticket <игрок> [кол-во]\n" +
                "<yellow>/lda forceend <игрок>\n" +
                "<yellow>/lda sethonor <игрок> <значение>\n" +
                "<yellow>/lda reload"
        ));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("arena", "kit", "give", "forceend", "sethonor", "reload"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("арена"))) {
            return filter(List.of("create", "setpos1", "setpos2", "setspectator", "setbound1", "setbound2", "setspecbound1", "setspecbound2", "delete", "list"), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("kit") || args[0].equalsIgnoreCase("кит"))) {
            return filter(List.of("create", "delete", "list"), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("выдать"))) {
            return filter(List.of("ticket"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> src, String prefix) {
        List<String> res = new ArrayList<>();
        for (String s : src) {
            if (s.startsWith(prefix.toLowerCase())) res.add(s);
        }
        return res;
    }
}
