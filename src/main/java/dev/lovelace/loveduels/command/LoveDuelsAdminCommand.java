package dev.lovelace.loveduels.command;

import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.kit.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ У вас нет прав администратора LoveDuels."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help", "помощь", "?" -> sendHelp(sender);
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
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<yellow>Использование: <white>/lda arena <create|setpos1|setpos2|setspectator|setbound1|setbound2|setspecbound1|setspecbound2|delete|list></white>"
            ));
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "create", "создать" -> {
                if (args.length < 3) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Укажите ID арены: <yellow>/lda arena create <id> [название]</yellow>"));
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
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Укажите ID: <yellow>/lda arena setpos1 <id></yellow>"));
                    return;
                }
                Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
                if (opt.isEmpty()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Арена не найдена."));
                    return;
                }
                opt.get().setPos1(p.getLocation());
                duelManager.getArenaManager().saveArenas();
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Точка спавна 1 для арены <gold>" + args[2] + "</gold> установлена!"));
            }
            case "setpos2" -> {
                if (!(sender instanceof Player p)) return;
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Укажите ID: <yellow>/lda arena setpos2 <id></yellow>"));
                    return;
                }
                Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
                if (opt.isEmpty()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Арена не найдена."));
                    return;
                }
                opt.get().setPos2(p.getLocation());
                duelManager.getArenaManager().saveArenas();
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Точка спавна 2 для арены <gold>" + args[2] + "</gold> установлена!"));
            }
            case "setspectator" -> {
                if (!(sender instanceof Player p)) return;
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Укажите ID: <yellow>/lda arena setspectator <id></yellow>"));
                    return;
                }
                Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
                if (opt.isEmpty()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Арена не найдена."));
                    return;
                }
                opt.get().setSpectatorSpawn(p.getLocation());
                duelManager.getArenaManager().saveArenas();
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Точка наблюдения (спавн зрителей) для арены <gold>" + args[2] + "</gold> установлена!"));
            }
            case "setbound1" -> {
                if (!(sender instanceof Player p)) return;
                bound1Points.put(p.getUniqueId(), p.getLocation());
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Первая угловая точка боевой зоны сохранена. Встаньте во вторую точку и введите: <yellow>/lda arena setbound2 <id></yellow>"));
            }
            case "setbound2" -> {
                if (!(sender instanceof Player p)) return;
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Укажите ID: <yellow>/lda arena setbound2 <id></yellow>"));
                    return;
                }
                Location b1 = bound1Points.get(p.getUniqueId());
                if (b1 == null) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Сначала задайте первую точку командой: <yellow>/lda arena setbound1</yellow>"));
                    return;
                }
                Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
                if (opt.isEmpty()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Арена не найдена."));
                    return;
                }
                BoundingBox box = BoundingBox.of(b1, p.getLocation());
                opt.get().setBounds(box);
                duelManager.getArenaManager().saveArenas();
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Границы боевой зоны для арены <gold>" + args[2] + "</gold> успешно сохранены!"));
            }
            case "setspecbound1" -> {
                if (!(sender instanceof Player p)) return;
                bound1Points.put(p.getUniqueId(), p.getLocation());
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Первая точка зоны трибун сохранена. Встаньте во вторую точку и введите: <yellow>/lda arena setspecbound2 <id></yellow>"));
            }
            case "setspecbound2" -> {
                if (!(sender instanceof Player p)) return;
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Укажите ID: <yellow>/lda arena setspecbound2 <id></yellow>"));
                    return;
                }
                Location b1 = bound1Points.get(p.getUniqueId());
                if (b1 == null) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Сначала задайте первую точку трибун: <yellow>/lda arena setspecbound1</yellow>"));
                    return;
                }
                Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
                if (opt.isEmpty()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Арена не найдена."));
                    return;
                }
                BoundingBox box = BoundingBox.of(b1, p.getLocation());
                opt.get().setSpectatorZone(box);
                duelManager.getArenaManager().saveArenas();
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Границы трибун (зоны зрителей) для арены <gold>" + args[2] + "</gold> успешно сохранены!"));
            }
            case "delete", "удалить" -> {
                if (args.length < 3) return;
                if (duelManager.getArenaManager().deleteArena(args[2])) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Арена <gold>" + args[2] + "</gold> удалена."));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Арена не найдена."));
                }
            }
            case "list", "список" -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("\n<gradient:#FFD700:#FFA500><b>🏟 Список зарегистрированных арен:</b></gradient>"));
                Collection<Arena> arenas = duelManager.getArenaManager().getAllArenas();
                if (arenas.isEmpty()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Арены пока не созданы. Создайте первую: /lda arena create</gray>"));
                }
                for (Arena a : arenas) {
                    String cfg = a.isConfigured() ? "<green>[Полностью настроена]</green>" : "<red>[Требует настройки]</red>";
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<yellow>▪ <white><b>" + a.getId() + "</b> <gray>(«" + a.getName() + "»)</gray> " + cfg
                    ));
                }
            }
            default -> sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Неизвестное поддействие арены."));
        }
    }

    private void handleKit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Использование: <white>/lda kit <create|delete|list></white>"));
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "create", "создать" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Только игрок в игре может сохранить кит из своего инвентаря."));
                    return;
                }
                if (args.length < 3) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Укажите ID: <yellow>/lda kit create <id> [название]</yellow>"));
                    return;
                }
                String id = args[2].toLowerCase();
                String displayName = (args.length > 3) ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : id;
                Kit kit = Kit.fromPlayer(id, displayName, p);
                duelManager.getKitManager().registerKit(kit);
                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Кит <gold>" + id + "</gold> («" + displayName + "») успешно сохранён из вашего инвентаря!"));
            }
            case "delete", "удалить" -> {
                if (args.length < 3) return;
                if (duelManager.getKitManager().deleteKit(args[2])) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Кит <gold>" + args[2] + "</gold> удалён."));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Кит не найден."));
                }
            }
            case "list", "список" -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("\n<gradient:#55FFFF:#00AAAA><b>🛡 Список наборов снаряжения (китов):</b></gradient>"));
                Collection<Kit> kits = duelManager.getKitManager().getAllKits();
                if (kits.isEmpty()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Киты пока не созданы. Создайте первый: /lda kit create</gray>"));
                }
                for (Kit k : kits) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<yellow>▪ <white><b>" + k.id() + "</b> <gray>(«" + k.displayName() + "»)</gray>"
                    ));
                }
            }
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("ticket")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Использование: <white>/lda give ticket <игрок> [количество]</white>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Игрок не найден или офлайн."));
            return;
        }

        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Некорректное число количества: " + args[3]));
                return;
            }
        }

        ItemStack ticket = duelManager.getRoyalManager().getTicketItem().create(amount);
        target.getInventory().addItem(ticket);
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Выдано <gold>" + amount + " шт.</gold> Билетов Королевской Дуэли игроку <white>" + target.getName() + "</white>"));
    }

    private void handleForceEnd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Использование: <white>/lda forceend <игрок></white>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Игрок не найден или офлайн."));
            return;
        }

        if (duelManager.getMatchManager().forceEnd(target)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Дуэль с участием <gold>" + target.getName() + "</gold> принудительно остановлена."));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Игрок не находится в активной дуэли."));
        }
    }

    private void handleSetHonor(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Использование: <white>/lda sethonor <игрок> <значение></white>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Игрок не найден или офлайн."));
            return;
        }

        int val;
        try {
            val = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Некорректное число очков Чести."));
            return;
        }

        duelManager.getPlayerStorage().getOrCreatePlayer(target.getUniqueId(), target.getName()).thenAccept(data -> {
            var updated = data.withHonor(val);
            duelManager.getPlayerStorage().savePlayer(updated);
            duelManager.getLeaderboardsBridge().syncPlayerData(updated);
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Честь игрока <gold>" + target.getName() + "</gold> установлена на <yellow>" + val + "</yellow>"));
        });
    }

    private void handleReload(CommandSender sender) {
        duelManager.getPlugin().reloadConfig();
        duelManager.getArenaManager().loadArenas();
        duelManager.getKitManager().loadKits();
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>✔ Конфигурация, арены и наборы китов LoveDuels успешно перезагружены!"));
    }

    private void sendHelp(CommandSender sender) {
        MiniMessage mm = MiniMessage.miniMessage();
        Component header = mm.deserialize("\n<gradient:#FF5555:#FFAA00><b>🛡 LoveDuels — Панель Администратора</b></gradient>");
        Component divider = mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");

        Component msg = Component.empty()
                .append(header).append(Component.newline())
                .append(divider).append(Component.newline())
                .append(mm.deserialize("<gold><b>🏟 Арены:</b></gold>")).append(Component.newline())
                .append(formatAdminCmd("/lda arena list", "Список всех арен и их статус готовности")).append(Component.newline())
                .append(formatAdminCmd("/lda arena create <id> [имя]", "Создать новую арену")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setpos1 <id>", "Установить точку спавна первого дуэлянта")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setpos2 <id>", "Установить точку спавна второго дуэлянта")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setspectator <id>", "Установить точку наблюдения трибун")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setbound1 / setbound2", "Задать границы боевой зоны арены")).append(Component.newline())
                .append(formatAdminCmd("/lda arena delete <id>", "Удалить арену")).append(Component.newline())
                .append(mm.deserialize("<aqua><b>🛡 Киты снаряжения:</b></aqua>")).append(Component.newline())
                .append(formatAdminCmd("/lda kit list", "Список всех сохранённых китов")).append(Component.newline())
                .append(formatAdminCmd("/lda kit create <id> [имя]", "Сохранить текущий инвентарь как кит")).append(Component.newline())
                .append(formatAdminCmd("/lda kit delete <id>", "Удалить кит")).append(Component.newline())
                .append(mm.deserialize("<green><b>⚙ Управление и Система:</b></green>")).append(Component.newline())
                .append(formatAdminCmd("/lda give ticket <игрок> [кол-во]", "Выдать Билеты Королевской Дуэли")).append(Component.newline())
                .append(formatAdminCmd("/lda forceend <игрок>", "Принудительно остановить текущую дуэль")).append(Component.newline())
                .append(formatAdminCmd("/lda sethonor <игрок> <очки>", "Установить очки Чести игроку")).append(Component.newline())
                .append(formatAdminCmd("/lda reload", "Перезагрузить config.yml, арены и киты")).append(Component.newline())
                .append(divider).append(Component.newline());

        sender.sendMessage(msg);
    }

    private Component formatAdminCmd(String cmd, String desc) {
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize("  <yellow><b>" + cmd + "</b></yellow> <dark_gray>—</dark_gray> <gray>" + desc + "</gray>")
                .clickEvent(ClickEvent.suggestCommand(cmd.split(" <")[0] + " "))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<yellow>Нажмите для автозаполнения: <white>" + cmd + "</white></yellow>")));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("help", "arena", "kit", "give", "forceend", "sethonor", "reload", "помощь", "арена", "кит", "выдать"), args[0]);
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
