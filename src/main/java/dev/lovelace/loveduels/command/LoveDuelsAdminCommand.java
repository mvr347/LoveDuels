package dev.lovelace.loveduels.command;

import dev.lovelace.loveduels.arena.Arena;
import dev.lovelace.loveduels.arena.ArenaParticlePreview;
import dev.lovelace.loveduels.core.DuelManager;
import dev.lovelace.loveduels.core.DuelType;
import dev.lovelace.loveduels.kit.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
import java.util.concurrent.ConcurrentHashMap;

public final class LoveDuelsAdminCommand implements CommandExecutor, TabCompleter {

    private final DuelManager duelManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Setup wizard sessions & temporary corner coordinates
    private final Map<UUID, ArenaSetupSession> setupSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Location> combatBound1Points = new ConcurrentHashMap<>();
    private final Map<UUID, Location> specBound1Points = new ConcurrentHashMap<>();

    public LoveDuelsAdminCommand(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("loveduels.admin")) {
            sender.sendMessage(mm.deserialize("<red>❌ У вас нет прав администратора LoveDuels."));
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

    // =========================================================================
    // ARENA MANAGEMENT & SEQUENTIAL WIZARD
    // =========================================================================

    private void handleArena(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendArenaHelp(sender);
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "help", "помощь", "?" -> sendArenaHelp(sender);
            case "create", "создать" -> handleArenaCreate(sender, args);
            case "setup", "wizard", "мастер" -> handleArenaWizard(sender, args);
            case "setpos1", "спавн1" -> handleArenaSetPos1(sender, args);
            case "setpos2", "спавн2" -> handleArenaSetPos2(sender, args);
            case "setspectator", "зрители" -> handleArenaSetSpectator(sender, args);
            case "setbound1", "граница1" -> handleArenaSetBound1(sender, args);
            case "setbound2", "граница2" -> handleArenaSetBound2(sender, args);
            case "setheight", "высота" -> handleArenaSetHeight(sender, args);
            case "setspecbound1", "трибуны1" -> handleArenaSetSpecBound1(sender, args);
            case "setspecbound2", "трибуны2" -> handleArenaSetSpecBound2(sender, args);
            case "finish", "завершить" -> handleArenaFinish(sender, args);
            case "cancel", "отмена" -> handleArenaCancel(sender);
            case "info", "инфо", "status", "статус" -> handleArenaInfo(sender, args);
            case "preview", "частицы", "подсветка" -> handleArenaPreview(sender, args);
            case "tp", "телепорт" -> handleArenaTp(sender, args);
            case "toggle", "переключить" -> handleArenaToggle(sender, args);
            case "mode", "режим", "type" -> handleArenaMode(sender, args);
            case "list", "список" -> handleArenaList(sender);
            case "delete", "удалить" -> handleArenaDelete(sender, args);
            default -> sender.sendMessage(mm.deserialize("<red>❌ Неизвестная команда арены. Введите <yellow>/lda arena</yellow> для справки."));
        }
    }

    private void handleArenaCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(mm.deserialize("<red>❌ Команды настройки арен доступны только игроку в игре."));
            return;
        }

        if (args.length < 3) {
            p.sendMessage(mm.deserialize("<red>❌ Укажите ID арены: <yellow>/lda arena create <id> [название]</yellow>"));
            return;
        }

        String id = args[2].toLowerCase();
        Optional<Arena> existing = duelManager.getArenaManager().getArena(id);
        Arena arena;
        if (existing.isPresent()) {
            arena = existing.get();
            p.sendMessage(mm.deserialize("<yellow>ℹ Арена с ID <gold>" + id + "</gold> уже существует. Открываю мастер настройки этой арены."));
        } else {
            String name = (args.length > 3) ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : id;
            arena = new Arena(id, name);
            duelManager.getArenaManager().registerArena(arena);
            p.sendMessage(mm.deserialize("<green>✔ Арена <gold>" + id + "</gold> («" + name + "») успешно зарегистрирована!"));
        }

        // Start wizard session
        setupSessions.put(p.getUniqueId(), new ArenaSetupSession(arena.getId()));

        p.sendMessage(Component.empty());
        p.sendMessage(mm.deserialize("<gradient:#FFD700:#FFA500><b>══════════════════════════════════════════════════</b></gradient>"));
        p.sendMessage(mm.deserialize(" <b>🏟 Мастер пошаговой настройки арены:</b> <yellow>" + arena.getId() + "</yellow> <gray>(«" + arena.getName() + "»)</gray>"));
        p.sendMessage(mm.deserialize(" <gray>Следуйте подсказкам мастера или нажимайте на кнопки в чате.</gray>"));
        p.sendMessage(mm.deserialize("<gradient:#FFD700:#FFA500><b>══════════════════════════════════════════════════</b></gradient>"));

        sendWizardStep1(p, arena);
    }

    private void handleArenaWizard(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(mm.deserialize("<red>❌ Только игрок в игре может использовать мастер настройки."));
            return;
        }

        String id = null;
        if (args.length >= 3) {
            id = args[2].toLowerCase();
        } else {
            ArenaSetupSession session = setupSessions.get(p.getUniqueId());
            if (session != null) {
                id = session.getArenaId();
            } else {
                Collection<Arena> all = duelManager.getArenaManager().getAllArenas();
                if (all.size() == 1) {
                    id = all.iterator().next().getId();
                }
            }
        }

        if (id == null) {
            p.sendMessage(mm.deserialize("<red>❌ Укажите ID арены: <yellow>/lda arena wizard <id></yellow>"));
            return;
        }

        Optional<Arena> opt = duelManager.getArenaManager().getArena(id);
        if (opt.isEmpty()) {
            p.sendMessage(mm.deserialize("<red>❌ Арена <gold>" + id + "</gold> не найдена."));
            return;
        }

        Arena arena = opt.get();
        setupSessions.put(p.getUniqueId(), new ArenaSetupSession(arena.getId()));

        p.sendMessage(Component.empty());
        p.sendMessage(mm.deserialize("<gradient:#FFD700:#FFA500><b>══════════════════════════════════════════════════</b></gradient>"));
        p.sendMessage(mm.deserialize(" <b>🏟 Мастер настройки арены:</b> <yellow>" + arena.getId() + "</yellow> <gray>(«" + arena.getName() + "»)</gray>"));
        p.sendMessage(mm.deserialize("<gradient:#FFD700:#FFA500><b>══════════════════════════════════════════════════</b></gradient>"));

        if (arena.getPos1() == null) {
            sendWizardStep1(p, arena);
        } else if (arena.getPos2() == null) {
            sendWizardStep2(p, arena);
        } else if (arena.getSpectatorSpawn() == null) {
            sendWizardStep3(p, arena);
        } else if (arena.getBounds() == null) {
            sendWizardStep4(p, arena);
        } else {
            sendWizardStep5(p, arena);
        }
    }

    private void handleArenaSetPos1(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        Arena arena = resolveArena(p, args, 2);
        if (arena == null) return;

        arena.setPos1(p.getLocation());
        duelManager.getArenaManager().saveArenas();
        ArenaParticlePreview.show(duelManager.getPlugin(), p, arena, 3);

        Location loc = p.getLocation();
        p.sendMessage(mm.deserialize(String.format(
                "<green>✔ Точка спавна 1 сохранена! <gray>[мир: <white>%s</white>, X: <yellow>%.1f</yellow>, Y: <yellow>%.1f</yellow>, Z: <yellow>%.1f</yellow>, взгляд: <yellow>%.0f°</yellow>]</gray>",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw()
        )));

        ArenaSetupSession session = setupSessions.get(p.getUniqueId());
        if (session != null && session.getArenaId().equalsIgnoreCase(arena.getId())) {
            sendWizardStep2(p, arena);
        }
    }

    private void handleArenaSetPos2(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        Arena arena = resolveArena(p, args, 2);
        if (arena == null) return;

        if (arena.getPos1() != null && !p.getWorld().equals(arena.getPos1().getWorld())) {
            p.sendMessage(mm.deserialize("<red>❌ Ошибка: спавн 2 должен находиться в том же мире, что и спавн 1 (<yellow>" + arena.getPos1().getWorld().getName() + "</yellow>)!"));
            return;
        }

        if (arena.getPos1() != null && arena.getPos1().distanceSquared(p.getLocation()) < 4.0) {
            p.sendMessage(mm.deserialize("<yellow>⚠ Предупреждение: спавны 1 и 2 находятся слишком близко (< 2 блоков друг от друга)."));
        }

        arena.setPos2(p.getLocation());
        duelManager.getArenaManager().saveArenas();
        ArenaParticlePreview.show(duelManager.getPlugin(), p, arena, 3);

        Location loc = p.getLocation();
        double dist = arena.getPos1() != null ? arena.getPos1().distance(loc) : 0.0;
        p.sendMessage(mm.deserialize(String.format(
                "<green>✔ Точка спавна 2 сохранена! <gray>[X: <yellow>%.1f</yellow>, Y: <yellow>%.1f</yellow>, Z: <yellow>%.1f</yellow>, взгляд: <yellow>%.0f°</yellow>] <aqua>(дистанция между бойцами: %.1f бл.)</aqua></gray>",
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), dist
        )));

        ArenaSetupSession session = setupSessions.get(p.getUniqueId());
        if (session != null && session.getArenaId().equalsIgnoreCase(arena.getId())) {
            sendWizardStep3(p, arena);
        }
    }

    private void handleArenaSetSpectator(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        Arena arena = resolveArena(p, args, 2);
        if (arena == null) return;

        if (arena.getPos1() != null && !p.getWorld().equals(arena.getPos1().getWorld())) {
            p.sendMessage(mm.deserialize("<red>❌ Спавн зрителей должен находиться в том же мире, что и боевая арена!"));
            return;
        }

        arena.setSpectatorSpawn(p.getLocation());
        duelManager.getArenaManager().saveArenas();
        ArenaParticlePreview.show(duelManager.getPlugin(), p, arena, 3);

        Location loc = p.getLocation();
        p.sendMessage(mm.deserialize(String.format(
                "<green>✔ Точка спавна зрителей (трибуна) сохранена! <gray>[X: <yellow>%.1f</yellow>, Y: <yellow>%.1f</yellow>, Z: <yellow>%.1f</yellow>]</gray>",
                loc.getX(), loc.getY(), loc.getZ()
        )));

        ArenaSetupSession session = setupSessions.get(p.getUniqueId());
        if (session != null && session.getArenaId().equalsIgnoreCase(arena.getId())) {
            sendWizardStep4(p, arena);
        }
    }

    private void handleArenaSetBound1(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        Arena arena = resolveArena(p, args, 2);
        if (arena == null) return;

        ArenaSetupSession session = setupSessions.get(p.getUniqueId());
        if (session != null) {
            session.setCombatBound1(p.getLocation());
        }
        combatBound1Points.put(p.getUniqueId(), p.getLocation());

        Location l = p.getLocation();
        p.sendMessage(mm.deserialize(String.format(
                "<green>✔ Точка границы 1 сохранена! <gray>[X: <yellow>%d</yellow>, Y: <yellow>%d</yellow>, Z: <yellow>%d</yellow>]</gray>",
                l.getBlockX(), l.getBlockY(), l.getBlockZ()
        )));

        p.sendMessage(mm.deserialize("<gray>Теперь перейдите в противоположный угол арены и нажмите:</gray>"));
        p.sendMessage(Component.text("  ")
                .append(button("📍 ТОЧКА ГРАНИЦЫ 2 И СОХРАНЕНИЕ", "/lda arena setbound2 " + arena.getId(), "Зафиксировать второй угол арены", "gold")));
    }

    private void handleArenaSetBound2(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        Arena arena = resolveArena(p, args, 2);
        if (arena == null) return;

        ArenaSetupSession session = setupSessions.get(p.getUniqueId());
        Location b1 = (session != null && session.getCombatBound1() != null)
                ? session.getCombatBound1()
                : combatBound1Points.get(p.getUniqueId());

        if (b1 == null) {
            p.sendMessage(mm.deserialize("<red>❌ Сначала задайте 1-ю угловую точку боевой зоны командой: <yellow>/lda arena setbound1 " + arena.getId() + "</yellow>"));
            return;
        }

        if (!b1.getWorld().equals(p.getWorld())) {
            p.sendMessage(mm.deserialize("<red>❌ Обе точки границы должны находиться в одном мире!"));
            return;
        }

        Location b2 = p.getLocation();
        double minX = Math.min(b1.getBlockX(), b2.getBlockX());
        double maxX = Math.max(b1.getBlockX(), b2.getBlockX()) + 1.0;
        double minZ = Math.min(b1.getBlockZ(), b2.getBlockZ());
        double maxZ = Math.max(b1.getBlockZ(), b2.getBlockZ()) + 1.0;
        double minY = Math.min(b1.getBlockY(), b2.getBlockY());
        double maxY = Math.max(b1.getBlockY(), b2.getBlockY()) + 1.0;

        boolean autoExpandedHeight = false;
        if (maxY - minY < 5.0) {
            maxY = minY + 20.0;
            minY = Math.max(-64.0, minY - 1.0);
            autoExpandedHeight = true;
        }

        BoundingBox box = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        arena.setBounds(box);
        duelManager.getArenaManager().saveArenas();

        int sizeX = (int) (maxX - minX);
        int sizeY = (int) (maxY - minY);
        int sizeZ = (int) (maxZ - minZ);

        p.sendMessage(mm.deserialize(String.format(
                "<green>✔ Боевые границы арены <gold>%s</gold> успешно сохранены! <gray>[Размер: <yellow>%dx%dx%d</yellow> бл., Y: <white>%d → %d</white>]</gray>",
                arena.getId(), sizeX, sizeY, sizeZ, (int) minY, (int) maxY
        )));

        if (autoExpandedHeight) {
            p.sendMessage(mm.deserialize("<yellow>💡 Высота арены автоматически установлена в 20 блоков вверх от пола, чтобы бойцы могли прыгать и сражаться верхом. Изменить высоту: <white>/lda arena setheight " + arena.getId() + " <высота></white></yellow>"));
        }

        // Validate spawns inside bounds
        if (arena.getPos1() != null && !arena.isInCombatBounds(arena.getPos1())) {
            p.sendMessage(mm.deserialize("<red>⚠ ВНИМАНИЕ: Точка спавна 1 оказалась ВНЕ созданных границ арены! Перезадайте спавн 1 или границы."));
        }
        if (arena.getPos2() != null && !arena.isInCombatBounds(arena.getPos2())) {
            p.sendMessage(mm.deserialize("<red>⚠ ВНИМАНИЕ: Точка спавна 2 оказалась ВНЕ созданных границ арены! Перезадайте спавн 2 или границы."));
        }

        ArenaParticlePreview.show(duelManager.getPlugin(), p, arena, 10);

        if (session != null && session.getArenaId().equalsIgnoreCase(arena.getId())) {
            sendWizardStep5(p, arena);
        }
    }

    private void handleArenaSetHeight(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        if (args.length < 4) {
            p.sendMessage(mm.deserialize("<red>❌ Укажите ID и высоту в блоках: <yellow>/lda arena setheight <id> <высота></yellow>"));
            return;
        }

        Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
        if (opt.isEmpty()) {
            p.sendMessage(mm.deserialize("<red>❌ Арена не найдена."));
            return;
        }

        Arena arena = opt.get();
        if (arena.getBounds() == null) {
            p.sendMessage(mm.deserialize("<red>❌ Сначала задайте границы арены: <yellow>/lda arena setbound1</yellow> и <yellow>setbound2</yellow>"));
            return;
        }

        double height;
        try {
            height = Double.parseDouble(args[3]);
            if (height < 2.0 || height > 256.0) {
                p.sendMessage(mm.deserialize("<red>❌ Высота должна быть от 2 до 256 блоков."));
                return;
            }
        } catch (NumberFormatException e) {
            p.sendMessage(mm.deserialize("<red>❌ Некорректное число высоты: " + args[3]));
            return;
        }

        arena.setBoundsHeight(height);
        duelManager.getArenaManager().saveArenas();
        ArenaParticlePreview.show(duelManager.getPlugin(), p, arena, 10);

        p.sendMessage(mm.deserialize(String.format(
                "<green>✔ Высота боевой зоны арены <gold>%s</gold> установлена на <yellow>%.0f</yellow> блоков! <gray>(Y: <white>%.0f → %.0f</white>)</gray>",
                arena.getId(), height, arena.getBounds().getMinY(), arena.getBounds().getMaxY()
        )));
    }

    private void handleArenaSetSpecBound1(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        Arena arena = resolveArena(p, args, 2);
        if (arena == null) return;

        ArenaSetupSession session = setupSessions.get(p.getUniqueId());
        if (session != null) {
            session.setSpecBound1(p.getLocation());
        }
        specBound1Points.put(p.getUniqueId(), p.getLocation());

        Location l = p.getLocation();
        p.sendMessage(mm.deserialize(String.format(
                "<green>✔ Первая точка зоны трибун сохранена! <gray>[X: <yellow>%d</yellow>, Y: <yellow>%d</yellow>, Z: <yellow>%d</yellow>]</gray>",
                l.getBlockX(), l.getBlockY(), l.getBlockZ()
        )));
        p.sendMessage(mm.deserialize("<gray>Перейдите в противоположный угол зоны зрителей и нажмите:</gray>"));
        p.sendMessage(Component.text("  ")
                .append(button("📍 ТРИБУНЫ: ТОЧКА 2", "/lda arena setspecbound2 " + arena.getId(), "Сохранить границы трибун", "aqua")));
    }

    private void handleArenaSetSpecBound2(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        Arena arena = resolveArena(p, args, 2);
        if (arena == null) return;

        ArenaSetupSession session = setupSessions.get(p.getUniqueId());
        Location b1 = (session != null && session.getSpecBound1() != null)
                ? session.getSpecBound1()
                : specBound1Points.get(p.getUniqueId());

        if (b1 == null) {
            p.sendMessage(mm.deserialize("<red>❌ Сначала задайте 1-ю точку трибун: <yellow>/lda arena setspecbound1 " + arena.getId() + "</yellow>"));
            return;
        }

        Location b2 = p.getLocation();
        double minX = Math.min(b1.getBlockX(), b2.getBlockX());
        double maxX = Math.max(b1.getBlockX(), b2.getBlockX()) + 1.0;
        double minZ = Math.min(b1.getBlockZ(), b2.getBlockZ());
        double maxZ = Math.max(b1.getBlockZ(), b2.getBlockZ()) + 1.0;
        double minY = Math.min(b1.getBlockY(), b2.getBlockY());
        double maxY = Math.max(b1.getBlockY(), b2.getBlockY()) + 1.0;

        if (maxY - minY < 5.0) {
            maxY = minY + 20.0;
        }

        BoundingBox box = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        arena.setSpectatorZone(box);
        duelManager.getArenaManager().saveArenas();
        ArenaParticlePreview.show(duelManager.getPlugin(), p, arena, 10);

        p.sendMessage(mm.deserialize("<green>✔ Границы трибун (зоны зрителей) для арены <gold>" + arena.getId() + "</gold> успешно сохранены!"));

        if (session != null && session.getArenaId().equalsIgnoreCase(arena.getId())) {
            sendWizardCompletion(p, arena);
        }
    }

    private void handleArenaFinish(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        Arena arena = resolveArena(p, args, 2);
        if (arena == null) return;

        sendWizardCompletion(p, arena);
    }

    private void handleArenaCancel(CommandSender sender) {
        if (!(sender instanceof Player p)) return;
        ArenaSetupSession session = setupSessions.remove(p.getUniqueId());
        if (session != null) {
            p.sendMessage(mm.deserialize("<yellow>✔ Вы вышли из мастера настройки арены <gold>" + session.getArenaId() + "</gold>."));
        } else {
            p.sendMessage(mm.deserialize("<gray>У вас нет активного мастера настройки арены.</gray>"));
        }
    }

    private void handleArenaInfo(CommandSender sender, String[] args) {
        String id;
        if (args.length >= 3) {
            id = args[2].toLowerCase();
        } else if (sender instanceof Player p && setupSessions.containsKey(p.getUniqueId())) {
            id = setupSessions.get(p.getUniqueId()).getArenaId();
        } else {
            sender.sendMessage(mm.deserialize("<red>❌ Укажите ID арены: <yellow>/lda arena info <id></yellow>"));
            return;
        }

        Optional<Arena> opt = duelManager.getArenaManager().getArena(id);
        if (opt.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>❌ Арена <gold>" + id + "</gold> не найдена."));
            return;
        }

        Arena a = opt.get();
        Component div = mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");

        sender.sendMessage(Component.empty());
        sender.sendMessage(mm.deserialize("<gradient:#FFD700:#FFA500><b>🏟 Диагностическая карточка арены: " + a.getId() + "</b></gradient>"));
        sender.sendMessage(div);
        sender.sendMessage(mm.deserialize("▪ <b>Название:</b> <white>" + a.getName() + "</white>"));
        sender.sendMessage(mm.deserialize("▪ <b>Мир:</b> <yellow>" + a.getWorldName() + "</yellow>"));
        sender.sendMessage(mm.deserialize("▪ <b>Активность:</b> " + (a.isEnabled() ? "<green>✔ Включена</green>" : "<red>⛔ Отключена</red>") + " <gray>| Состояние: <gold>" + a.getState().name() + "</gold></gray>"));

        sender.sendMessage(Component.newline().append(mm.deserialize("<gold><b>📍 Точки спавна:</b></gold>")));
        sender.sendMessage(formatPointLine(a.getId(), "Спавн 1 (pos1)", a.getPos1(), "setpos1"));
        sender.sendMessage(formatPointLine(a.getId(), "Спавн 2 (pos2)", a.getPos2(), "setpos2"));
        sender.sendMessage(formatPointLine(a.getId(), "Спавн зрителей", a.getSpectatorSpawn(), "setspectator"));

        if (a.getPos1() != null && a.getPos2() != null && a.getPos1().getWorld().equals(a.getPos2().getWorld())) {
            double dist = a.getPos1().distance(a.getPos2());
            sender.sendMessage(mm.deserialize("  <gray>Дистанция между дуэлянтами: <yellow>" + String.format("%.1f", dist) + "</yellow> блоков</gray>"));
        }

        sender.sendMessage(Component.newline().append(mm.deserialize("<aqua><b>📦 Зоны и границы:</b></aqua>")));
        if (a.getBounds() != null) {
            BoundingBox b = a.getBounds();
            int sx = (int) (b.getMaxX() - b.getMinX());
            int sy = (int) (b.getMaxY() - b.getMinY());
            int sz = (int) (b.getMaxZ() - b.getMinZ());
            sender.sendMessage(Component.text("  ")
                    .append(mm.deserialize("<yellow>▪ Боевая зона:</yellow> <white>" + sx + "x" + sy + "x" + sz + " бл.</white> <gray>(Y: " + (int) b.getMinY() + " → " + (int) b.getMaxY() + ")</gray> "))
                    .append(button("✨ Частицы", "/lda arena preview " + a.getId() + " 10", "Подсветить", "aqua"))
                    .append(Component.space())
                    .append(suggestButton("↕ Высота", "/lda arena setheight " + a.getId() + " ", "Изменить высоту", "yellow")));
        } else {
            sender.sendMessage(Component.text("  ")
                    .append(mm.deserialize("<red>▪ Боевая зона: не задана (открытый мир)</red> "))
                    .append(button("📍 Задать границы", "/lda arena setbound1 " + a.getId(), "Задать угол 1", "yellow")));
        }

        if (a.getSpectatorZone() != null) {
            BoundingBox sb = a.getSpectatorZone();
            int sx = (int) (sb.getMaxX() - sb.getMinX());
            int sy = (int) (sb.getMaxY() - sb.getMinY());
            int sz = (int) (sb.getMaxZ() - sb.getMinZ());
            sender.sendMessage(mm.deserialize("  <yellow>▪ Трибуны:</yellow> <white>" + sx + "x" + sy + "x" + sz + " бл.</white> <gray>(Y: " + (int) sb.getMinY() + " → " + (int) sb.getMaxY() + ")</gray>"));
        } else {
            sender.sendMessage(mm.deserialize("  <gray>▪ Трибуны: по умолчанию (вокруг боевой зоны)</gray>"));
        }

        sender.sendMessage(Component.newline().append(mm.deserialize("<green><b>⚔ Поддерживаемые режимы дуэлей:</b></green>")));
        Component typesLine = Component.text("  ");
        for (DuelType dt : DuelType.values()) {
            boolean sup = a.supportsType(dt);
            String col = sup ? "green" : "dark_gray";
            String sign = sup ? "✔ " : "✖ ";
            typesLine = typesLine.append(button(sign + dt.name(), "/lda arena mode " + a.getId() + " " + dt.name(), "Нажмите для переключения", col)).append(Component.space());
        }
        sender.sendMessage(typesLine);

        sender.sendMessage(Component.newline().append(mm.deserialize("<yellow><b>🩺 Проверка готовности арены:</b></yellow>")));
        List<String> errors = a.getValidationErrors();
        List<String> warnings = a.getValidationWarnings();

        if (errors.isEmpty()) {
            sender.sendMessage(mm.deserialize("  <green>✔ Все обязательные проверки пройдены! Арена готова к приёму дуэлей.</green>"));
        } else {
            for (String err : errors) {
                sender.sendMessage(mm.deserialize("  <red>✖ " + err + "</red>"));
            }
        }
        for (String w : warnings) {
            sender.sendMessage(mm.deserialize("  <yellow>⚠ " + w + "</yellow>"));
        }

        sender.sendMessage(Component.newline().append(mm.deserialize("<b>Быстрые действия:</b> ")));
        sender.sendMessage(Component.text("  ")
                .append(button("⚙ Мастер настройки", "/lda arena wizard " + a.getId(), "Открыть пошаговый мастер", "gold"))
                .append(Component.space())
                .append(button("✨ Подсветка (15с)", "/lda arena preview " + a.getId() + " 15", "Показать частицы", "aqua"))
                .append(Component.space())
                .append(button(a.isEnabled() ? "⛔ Отключить" : "✔ Включить", "/lda arena toggle " + a.getId(), "Вкл/выкл", a.isEnabled() ? "red" : "green"))
                .append(Component.space())
                .append(button("❌ Удалить", "/lda arena delete " + a.getId(), "Удалить арену", "dark_red")));
        sender.sendMessage(div);
    }

    private Component formatPointLine(String arenaId, String label, Location loc, String setSubCmd) {
        if (loc != null && loc.getWorld() != null) {
            String coords = String.format("%.1f, %.1f, %.1f (взгляд: %.0f°)", loc.getX(), loc.getY(), loc.getZ(), loc.getYaw());
            return Component.text("  ")
                    .append(mm.deserialize("<green>✔ " + label + ":</green> <white>" + coords + "</white> "))
                    .append(button("🚀 ТП", "/lda arena tp " + arenaId + " " + (setSubCmd.contains("1") ? "pos1" : setSubCmd.contains("2") ? "pos2" : "spectator"), "Телепортироваться", "gold"))
                    .append(Component.space())
                    .append(button("✏ Задать", "/lda arena " + setSubCmd + " " + arenaId, "Перезаписать", "yellow"));
        } else {
            return Component.text("  ")
                    .append(mm.deserialize("<red>✖ " + label + ": не установлена</red> "))
                    .append(button("✏ Установить", "/lda arena " + setSubCmd + " " + arenaId, "Задать позицию", "green"));
        }
    }

    private void handleArenaList(CommandSender sender) {
        Collection<Arena> arenas = duelManager.getArenaManager().getAllArenas();
        sender.sendMessage(Component.empty());
        sender.sendMessage(mm.deserialize("<gradient:#FFD700:#FFA500><b>🏟 Список дуэльных арен (" + arenas.size() + " шт.):</b></gradient>"));
        sender.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"));

        if (arenas.isEmpty()) {
            sender.sendMessage(mm.deserialize("  <gray>Арены пока не созданы. Создайте первую:</gray>"));
            sender.sendMessage(Component.text("  ").append(button("➕ СОЗДАТЬ ПЕРВУЮ АРЕНУ", "/lda arena create ", "Нажмите для создания", "green")));
        } else {
            int readyCount = 0;
            for (Arena a : arenas) {
                boolean ready = a.isConfigured() && a.isEnabled();
                if (ready) readyCount++;

                String statusBadge;
                if (!a.isEnabled()) {
                    statusBadge = "<dark_gray>[ОТКЛЮЧЕНА]</dark_gray>";
                } else if (a.getState() == dev.lovelace.loveduels.arena.ArenaState.BUSY) {
                    statusBadge = "<gold>[В БОЮ]</gold>";
                } else if (a.isConfigured()) {
                    statusBadge = "<green>[ГОТОВА]</green>";
                } else {
                    statusBadge = "<red>[ТРЕБУЕТ НАСТРОЙКИ]</red>";
                }

                Component line = Component.text("▪ ")
                        .append(mm.deserialize("<b><white>" + a.getId() + "</white></b> <gray>(«" + a.getName() + "»)</gray> " + statusBadge + " <yellow>" + a.getWorldName() + "</yellow> "))
                        .append(button("📋 Инфо", "/lda arena info " + a.getId(), "Диагностика", "yellow"))
                        .append(Component.space())
                        .append(button("🚀 ТП", "/lda arena tp " + a.getId() + " pos1", "Телепорт на спавн 1", "gold"))
                        .append(Component.space())
                        .append(button("⚙ Настройка", "/lda arena wizard " + a.getId(), "Мастер настройки", "aqua"));

                sender.sendMessage(line);
            }
            sender.sendMessage(mm.deserialize("<dark_gray>────────────────────────────────────────────────</dark_gray>"));
            sender.sendMessage(mm.deserialize("<gray>Готово к дуэлям: <green>" + readyCount + "</green> из <white>" + arenas.size() + "</white></gray> | ")
                    .append(suggestButton("➕ Создать ещё арену", "/lda arena create ", "Создать", "green")));
        }
        sender.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"));
    }

    private void handleArenaPreview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        Arena arena = resolveArena(p, args, 2);
        if (arena == null) return;

        int seconds = 10;
        if (args.length >= 4) {
            try {
                seconds = Math.max(3, Integer.parseInt(args[3]));
            } catch (NumberFormatException ignored) {}
        }

        ArenaParticlePreview.show(duelManager.getPlugin(), p, arena, seconds);
        p.sendMessage(mm.deserialize("<aqua>✨ Границы и точки арены <gold>" + arena.getId() + "</gold> подсвечены частицами на <yellow>" + seconds + " сек.</yellow>"));
    }

    private void handleArenaTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        if (args.length < 3) {
            p.sendMessage(mm.deserialize("<red>❌ Укажите ID арены: <yellow>/lda arena tp <id> [pos1|pos2|spectator]</yellow>"));
            return;
        }

        Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
        if (opt.isEmpty()) {
            p.sendMessage(mm.deserialize("<red>❌ Арена не найдена."));
            return;
        }

        Arena a = opt.get();
        String target = (args.length >= 4) ? args[3].toLowerCase() : "pos1";
        Location dest;
        String pointName;

        switch (target) {
            case "pos2", "спавн2", "2" -> {
                dest = a.getPos2();
                pointName = "Спавн 2";
            }
            case "spectator", "зрители", "spec" -> {
                dest = a.getSafeSpectatorPoint();
                pointName = "Трибуна зрителей";
            }
            default -> {
                dest = a.getPos1();
                pointName = "Спавн 1";
            }
        }

        if (dest == null) {
            p.sendMessage(mm.deserialize("<red>❌ Точка " + pointName + " для арены <gold>" + a.getId() + "</gold> ещё не установлена."));
            return;
        }

        p.teleportAsync(dest);
        p.sendMessage(mm.deserialize("<green>✔ Вы телепортированы на <yellow>" + pointName + "</yellow> арены <gold>" + a.getId() + "</gold>."));
    }

    private void handleArenaToggle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(mm.deserialize("<red>❌ Укажите ID: <yellow>/lda arena toggle <id></yellow>"));
            return;
        }
        Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
        if (opt.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>❌ Арена не найдена."));
            return;
        }
        Arena a = opt.get();
        a.setEnabled(!a.isEnabled());
        duelManager.getArenaManager().saveArenas();
        if (a.isEnabled()) {
            sender.sendMessage(mm.deserialize("<green>✔ Арена <gold>" + a.getId() + "</gold> теперь <white>ВКЛЮЧЕНА</white> для дуэлей."));
        } else {
            sender.sendMessage(mm.deserialize("<gold>⛔ Арена <gold>" + a.getId() + "</gold> теперь <white>ОТКЛЮЧЕНА</white> (не будет выдаваться игрокам)."));
        }
    }

    private void handleArenaMode(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(mm.deserialize("<red>❌ Использование: <yellow>/lda arena mode <id> <all|тип_дуэли></yellow>"));
            return;
        }
        Optional<Arena> opt = duelManager.getArenaManager().getArena(args[2]);
        if (opt.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>❌ Арена не найдена."));
            return;
        }
        Arena a = opt.get();
        String modeArg = args[3].toUpperCase();

        if (modeArg.equalsIgnoreCase("ALL") || modeArg.equalsIgnoreCase("ВСЕ")) {
            a.getSupportedTypes().addAll(EnumSet.allOf(DuelType.class));
            duelManager.getArenaManager().saveArenas();
            sender.sendMessage(mm.deserialize("<green>✔ Арена <gold>" + a.getId() + "</gold> теперь поддерживает <yellow>ВСЕ</yellow> режимы дуэлей!"));
            return;
        }

        DuelType type = DuelType.fromString(modeArg);
        if (type == null) {
            sender.sendMessage(mm.deserialize("<red>❌ Неизвестный режим: " + modeArg + ". Доступные: " + Arrays.toString(DuelType.values())));
            return;
        }

        a.toggleSupportedType(type);
        duelManager.getArenaManager().saveArenas();
        boolean nowSupported = a.supportsType(type);
        sender.sendMessage(mm.deserialize(String.format(
                "<green>✔ Режим <yellow>%s</yellow> для арены <gold>%s</gold> теперь: %s",
                type.name(), a.getId(), nowSupported ? "<green>[ВКЛЮЧЁН]</green>" : "<red>[ВЫКЛЮЧЕН]</red>"
        )));
    }

    private void handleArenaDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(mm.deserialize("<red>❌ Укажите ID: <yellow>/lda arena delete <id></yellow>"));
            return;
        }
        String id = args[2].toLowerCase();
        if (duelManager.getArenaManager().deleteArena(id)) {
            sender.sendMessage(mm.deserialize("<green>✔ Арена <gold>" + id + "</gold> успешно удалена из конфигурации."));
        } else {
            sender.sendMessage(mm.deserialize("<red>❌ Арена <gold>" + id + "</gold> не найдена."));
        }
    }

    // =========================================================================
    // STEP-BY-STEP WIZARD RENDERERS
    // =========================================================================

    private void sendWizardStep1(Player player, Arena arena) {
        player.sendMessage(Component.empty());
        player.sendMessage(mm.deserialize("<gold><b>👉 ШАГ 1 из 5: Спавн первого дуэлянта (Позиция 1)</b></gold>"));
        player.sendMessage(mm.deserialize("<gray>Встаньте на точку появления 1-го игрока лицом к центру арены и нажмите кнопку ниже:</gray>"));
        player.sendMessage(Component.text("  ")
                .append(button("✔ УСТАНОВИТЬ СПАВН 1", "/lda arena setpos1 " + arena.getId(), "Установить спавн 1 на вашей текущей позиции", "green"))
                .append(Component.space())
                .append(button("❌ Отмена", "/lda arena cancel", "Выйти из мастера", "dark_gray")));
    }

    private void sendWizardStep2(Player player, Arena arena) {
        player.sendMessage(Component.empty());
        player.sendMessage(mm.deserialize("<gold><b>👉 ШАГ 2 из 5: Спавн второго дуэлянта (Позиция 2)</b></gold>"));
        player.sendMessage(mm.deserialize("<gray>Перейдите на противоположную сторону арены лицом к первому бойцу и нажмите:</gray>"));
        player.sendMessage(Component.text("  ")
                .append(button("✔ УСТАНОВИТЬ СПАВН 2", "/lda arena setpos2 " + arena.getId(), "Установить спавн 2 на вашей текущей позиции", "green"))
                .append(Component.space())
                .append(button("❌ Отмена", "/lda arena cancel", "Выйти из мастера", "dark_gray")));
    }

    private void sendWizardStep3(Player player, Arena arena) {
        player.sendMessage(Component.empty());
        player.sendMessage(mm.deserialize("<gold><b>👉 ШАГ 3 из 5: Точка появления зрителей (Трибуна)</b></gold>"));
        player.sendMessage(mm.deserialize("<gray>Встаньте на трибуну или балкон, откуда зрители будут наблюдать за дуэлями:</gray>"));
        player.sendMessage(Component.text("  ")
                .append(button("✔ УСТАНОВИТЬ СПАВН ЗРИТЕЛЕЙ", "/lda arena setspectator " + arena.getId(), "Установить спавн зрителей здесь", "aqua"))
                .append(Component.space())
                .append(button("⏩ ПРОПУСТИТЬ ШАГ", "/lda arena setbound1 " + arena.getId(), "Использовать точку по умолчанию над спавном 1", "gray")));
    }

    private void sendWizardStep4(Player player, Arena arena) {
        player.sendMessage(Component.empty());
        player.sendMessage(mm.deserialize("<gold><b>👉 ШАГ 4 из 5: Боевые границы арены (Combat Bounds)</b></gold>"));
        player.sendMessage(mm.deserialize("<gray>Боевая зона не позволяет дуэлянтам выбегать за пределы арены.</gray>"));
        player.sendMessage(mm.deserialize(" 1) Встаньте в <yellow>ПЕРВЫЙ угол</yellow> площадки на уровне пола и нажмите:"));
        player.sendMessage(Component.text("    ")
                .append(button("📍 ТОЧКА ГРАНИЦЫ 1", "/lda arena setbound1 " + arena.getId(), "Задать угол 1 на вашей позиции", "yellow")));
        player.sendMessage(mm.deserialize(" 2) Перейдите в <yellow>ПРОТИВОПОЛОЖНЫЙ угол</yellow> арены и нажмите:"));
        player.sendMessage(Component.text("    ")
                .append(button("📍 ТОЧКА ГРАНИЦЫ 2", "/lda arena setbound2 " + arena.getId(), "Задать угол 2 и рассчитать зону арены", "gold")));
    }

    private void sendWizardStep5(Player player, Arena arena) {
        player.sendMessage(Component.empty());
        player.sendMessage(mm.deserialize("<gold><b>👉 ШАГ 5 из 5: Зона трибун для зрителей (Spectator Zone)</b></gold>"));
        player.sendMessage(mm.deserialize("<gray>Ограничивает перемещение зрителей, чтобы они не летали внутри боя (опционально).</gray>"));
        player.sendMessage(Component.text("  ")
                .append(button("📍 ТРИБУНЫ: ТОЧКА 1", "/lda arena setspecbound1 " + arena.getId(), "Задать угол 1 трибун", "aqua"))
                .append(Component.space())
                .append(button("📍 ТРИБУНЫ: ТОЧКА 2", "/lda arena setspecbound2 " + arena.getId(), "Задать угол 2 трибун", "blue")));
        player.sendMessage(Component.text("  ")
                .append(button("✔ ЗАВЕРШИТЬ НАСТРОЙКУ АРЕНЫ", "/lda arena finish " + arena.getId(), "Проверить арену и завершить мастер", "green"))
                .append(Component.space())
                .append(button("⏩ ПРОПУСТИТЬ ЗОНУ ТРИБУН", "/lda arena finish " + arena.getId(), "Трибуной будет считаться зона вокруг арены", "gray")));
    }

    private void sendWizardCompletion(Player player, Arena arena) {
        List<String> errors = arena.getValidationErrors();
        List<String> warnings = arena.getValidationWarnings();

        player.sendMessage(Component.empty());
        player.sendMessage(mm.deserialize("<gradient:#FFD700:#FFA500><b>══════════════════════════════════════════════════</b></gradient>"));
        if (errors.isEmpty()) {
            player.sendMessage(mm.deserialize(" <gradient:#55FF55:#00AA00><b>✔ НАСТРОЙКА АРЕНЫ УСПЕШНО ЗАВЕРШЕНА!</b></gradient>"));
            player.sendMessage(mm.deserialize(" Арена <gold>" + arena.getId() + "</gold> («" + arena.getName() + "») <green>полностью готова к бою</green>!"));
            setupSessions.remove(player.getUniqueId());
            ArenaParticlePreview.show(duelManager.getPlugin(), player, arena, 15);
        } else {
            player.sendMessage(mm.deserialize(" <red><b>⚠ АРЕНА НАСТРОЕНА С ОШИБКАМИ:</b></red>"));
            for (String err : errors) {
                player.sendMessage(mm.deserialize("  <red>✖ " + err + "</red>"));
            }
        }

        for (String w : warnings) {
            player.sendMessage(mm.deserialize("  <yellow>⚠ " + w + "</yellow>"));
        }

        player.sendMessage(Component.newline().append(mm.deserialize("<b>Действия:</b> ")));
        player.sendMessage(Component.text("  ")
                .append(button("✨ Частицы (15 сек)", "/lda arena preview " + arena.getId() + " 15", "Показать границы и спавны", "aqua"))
                .append(Component.space())
                .append(button("🚀 ТП 1", "/lda arena tp " + arena.getId() + " pos1", "Телепорт на спавн 1", "gold"))
                .append(Component.space())
                .append(button("🚀 ТП 2", "/lda arena tp " + arena.getId() + " pos2", "Телепорт на спавн 2", "gold"))
                .append(Component.space())
                .append(button("📋 Карточка арены", "/lda arena info " + arena.getId(), "Подробная информация", "yellow")));
        player.sendMessage(mm.deserialize("<gradient:#FFD700:#FFA500><b>══════════════════════════════════════════════════</b></gradient>"));
    }

    private @Nullable Arena resolveArena(Player player, String[] args, int idIndex) {
        String id = null;
        if (args.length > idIndex) {
            id = args[idIndex].toLowerCase();
        } else {
            ArenaSetupSession session = setupSessions.get(player.getUniqueId());
            if (session != null) {
                id = session.getArenaId();
            } else {
                Collection<Arena> all = duelManager.getArenaManager().getAllArenas();
                if (all.size() == 1) {
                    id = all.iterator().next().getId();
                }
            }
        }

        if (id == null) {
            player.sendMessage(mm.deserialize("<red>❌ Укажите ID арены: <yellow>" + args[0] + " " + args[1] + " <id></yellow>"));
            return null;
        }

        Optional<Arena> opt = duelManager.getArenaManager().getArena(id);
        if (opt.isEmpty()) {
            player.sendMessage(mm.deserialize("<red>❌ Арена с ID <gold>" + id + "</gold> не найдена."));
            return null;
        }
        return opt.get();
    }

    // =========================================================================
    // OTHER ADMIN COMMANDS (KIT, GIVE, FORCEEND, SETHONOR, RELOAD, HELP)
    // =========================================================================

    private void handleKit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<yellow>Использование: <white>/lda kit <create|delete|list></white>"));
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "create", "создать" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(mm.deserialize("<red>❌ Только игрок в игре может сохранить кит из своего инвентаря."));
                    return;
                }
                if (args.length < 3) {
                    p.sendMessage(mm.deserialize("<red>❌ Укажите ID: <yellow>/lda kit create <id> [название]</yellow>"));
                    return;
                }
                String id = args[2].toLowerCase();
                String displayName = (args.length > 3) ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : id;
                Kit kit = Kit.fromPlayer(id, displayName, p);
                duelManager.getKitManager().registerKit(kit);
                p.sendMessage(mm.deserialize("<green>✔ Кит <gold>" + id + "</gold> («" + displayName + "») успешно сохранён из вашего инвентаря!"));
            }
            case "delete", "удалить" -> {
                if (args.length < 3) return;
                if (duelManager.getKitManager().deleteKit(args[2])) {
                    sender.sendMessage(mm.deserialize("<green>✔ Кит <gold>" + args[2] + "</gold> удалён."));
                } else {
                    sender.sendMessage(mm.deserialize("<red>❌ Кит не найден."));
                }
            }
            case "list", "список" -> {
                sender.sendMessage(mm.deserialize("\n<gradient:#55FFFF:#00AAAA><b>🛡 Список наборов снаряжения (китов):</b></gradient>"));
                Collection<Kit> kits = duelManager.getKitManager().getAllKits();
                if (kits.isEmpty()) {
                    sender.sendMessage(mm.deserialize("<gray>Киты пока не созданы. Создайте первый: /lda kit create</gray>"));
                }
                for (Kit k : kits) {
                    sender.sendMessage(mm.deserialize(
                            "<yellow>▪ <white><b>" + k.id() + "</b> <gray>(«" + k.displayName() + "»)</gray>"
                    ));
                }
            }
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("ticket")) {
            sender.sendMessage(mm.deserialize("<yellow>Использование: <white>/lda give ticket <игрок> [количество]</white>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>❌ Игрок не найден или офлайн."));
            return;
        }

        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>❌ Некорректное число количества: " + args[3]));
                return;
            }
        }

        ItemStack ticket = duelManager.getRoyalManager().getTicketItem().create(amount);
        target.getInventory().addItem(ticket);
        sender.sendMessage(mm.deserialize("<green>✔ Выдано <gold>" + amount + " шт.</gold> Билетов Королевской Дуэли игроку <white>" + target.getName() + "</white>"));
    }

    private void handleForceEnd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<yellow>Использование: <white>/lda forceend <игрок></white>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>❌ Игрок не найден или офлайн."));
            return;
        }

        if (duelManager.getMatchManager().forceEnd(target)) {
            sender.sendMessage(mm.deserialize("<green>✔ Дуэль с участием <gold>" + target.getName() + "</gold> принудительно остановлена."));
        } else {
            sender.sendMessage(mm.deserialize("<red>❌ Игрок не находится в активной дуэли."));
        }
    }

    private void handleSetHonor(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(mm.deserialize("<yellow>Использование: <white>/lda sethonor <игрок> <значение></white>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>❌ Игрок не найден или офлайн."));
            return;
        }

        int val;
        try {
            val = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(mm.deserialize("<red>❌ Некорректное число очков Чести."));
            return;
        }

        duelManager.getPlayerStorage().getOrCreatePlayer(target.getUniqueId(), target.getName()).thenAccept(data -> {
            var updated = data.withHonor(val);
            duelManager.getPlayerStorage().savePlayer(updated);
            duelManager.getLeaderboardsBridge().syncPlayerData(updated);
            sender.sendMessage(mm.deserialize("<green>✔ Честь игрока <gold>" + target.getName() + "</gold> установлена на <yellow>" + val + "</yellow>"));
        });
    }

    private void handleReload(CommandSender sender) {
        duelManager.getPlugin().reloadConfig();
        dev.lovelace.loveduels.gui.HeadsConfig.load(duelManager.getPlugin());
        duelManager.getArenaManager().loadArenas();
        duelManager.getKitManager().loadKits();
        sender.sendMessage(mm.deserialize("<green>✔ Конфигурация, арены, киты и скины голов (heads.yml) LoveDuels успешно перезагружены!"));
    }

    private void sendHelp(CommandSender sender) {
        Component header = mm.deserialize("\n<gradient:#FF5555:#FFAA00><b>🛡 LoveDuels — Панель Администратора</b></gradient>");
        Component divider = mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");

        Component msg = Component.empty()
                .append(header).append(Component.newline())
                .append(divider).append(Component.newline())
                .append(mm.deserialize("<gold><b>🏟 Арены:</b></gold>")).append(Component.newline())
                .append(formatAdminCmd("/lda arena list", "Список всех арен со статусом готовности")).append(Component.newline())
                .append(formatAdminCmd("/lda arena create <id> [имя]", "Создать арену и запустить пошаговый мастер")).append(Component.newline())
                .append(formatAdminCmd("/lda arena wizard [id]", "Пошаговый мастер настройки арены")).append(Component.newline())
                .append(formatAdminCmd("/lda arena info <id>", "Диагностическая карточка и проверка арены")).append(Component.newline())
                .append(formatAdminCmd("/lda arena preview <id> [сек]", "Подсветить границы арены частицами в мире")).append(Component.newline())
                .append(formatAdminCmd("/lda arena tp <id> [точка]", "Телепорт на арену (pos1, pos2, spectator)")).append(Component.newline())
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

    private void sendArenaHelp(CommandSender sender) {
        Component header = mm.deserialize("\n<gradient:#FFD700:#FFA500><b>🏟 LoveDuels — Команды Управления Аренами</b></gradient>");
        Component div = mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");

        Component msg = Component.empty()
                .append(header).append(Component.newline())
                .append(div).append(Component.newline())
                .append(mm.deserialize("<yellow><b>Пошаговый мастер настройки:</b></yellow>")).append(Component.newline())
                .append(formatAdminCmd("/lda arena create <id> [имя]", "Создать арену и запустить пошаговый мастер")).append(Component.newline())
                .append(formatAdminCmd("/lda arena wizard [id]", "Открыть пошаговый мастер для существующей арены")).append(Component.newline())
                .append(formatAdminCmd("/lda arena cancel", "Выйти из пошагового мастера настройки")).append(Component.newline())
                .append(mm.deserialize("<gold><b>Быстрая установка точек и границ:</b></gold>")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setpos1 [id]", "Спавн 1-го дуэлянта (лицом к центру)")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setpos2 [id]", "Спавн 2-го дуэлянта (напротив первого)")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setspectator [id]", "Точка наблюдения трибун для зрителей")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setbound1 [id]", "1-я угловая точка боевых границ")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setbound2 [id]", "2-я точка и расчёт границ боевой зоны")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setheight <id> <блок>", "Изменить высоту боевой зоны (потолок)")).append(Component.newline())
                .append(formatAdminCmd("/lda arena setspecbound1/2 [id]", "Задать границы зоны трибун (опционально)")).append(Component.newline())
                .append(mm.deserialize("<aqua><b>Управление и диагностика:</b></aqua>")).append(Component.newline())
                .append(formatAdminCmd("/lda arena list", "Список всех зарегистрированных арен")).append(Component.newline())
                .append(formatAdminCmd("/lda arena info <id>", "Полная диагностическая карточка арены")).append(Component.newline())
                .append(formatAdminCmd("/lda arena preview <id> [сек]", "Подсветить границы арены частицами в мире")).append(Component.newline())
                .append(formatAdminCmd("/lda arena tp <id> [точка]", "Телепорт на арену (pos1, pos2, spectator)")).append(Component.newline())
                .append(formatAdminCmd("/lda arena toggle <id>", "Включить / отключить арену для дуэлей")).append(Component.newline())
                .append(formatAdminCmd("/lda arena mode <id> <тип|all>", "Переключить поддерживаемый тип дуэли")).append(Component.newline())
                .append(formatAdminCmd("/lda arena delete <id>", "Удалить арену из конфигурации")).append(Component.newline())
                .append(div).append(Component.newline());

        sender.sendMessage(msg);
    }

    private Component formatAdminCmd(String cmd, String desc) {
        return mm.deserialize("  <yellow><b>" + cmd + "</b></yellow> <dark_gray>—</dark_gray> <gray>" + desc + "</gray>")
                .clickEvent(ClickEvent.suggestCommand(cmd.split(" <|\\[")[0] + " "))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<yellow>Нажмите для автозаполнения: <white>" + cmd + "</white></yellow>")));
    }

    private Component button(String text, String command, String hoverText, String color) {
        return mm.deserialize("<" + color + "><b>[" + text + "]</b></" + color + ">")
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<gray>" + hoverText + "<newline><yellow>Нажмите для выполнения: <white>" + command + "</white></yellow></gray>")));
    }

    private Component suggestButton(String text, String command, String hoverText, String color) {
        return mm.deserialize("<" + color + "><b>[" + text + "]</b></" + color + ">")
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<gray>" + hoverText + "<newline><yellow>Вставить в чат: <white>" + command + "</white></yellow></gray>")));
    }

    // =========================================================================
    // TAB COMPLETION
    // =========================================================================

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("help", "arena", "kit", "give", "forceend", "sethonor", "reload", "помощь", "арена", "кит", "выдать"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("арена"))) {
            return filter(List.of(
                    "list", "create", "setup", "wizard", "info", "setpos1", "setpos2",
                    "setspectator", "setbound1", "setbound2", "setheight", "setspecbound1",
                    "setspecbound2", "finish", "cancel", "preview", "tp", "toggle", "mode", "delete"
            ), args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("арена"))) {
            String sub = args[1].toLowerCase();
            if (!sub.equals("create") && !sub.equals("list") && !sub.equals("cancel")) {
                List<String> ids = duelManager.getArenaManager().getAllArenas().stream().map(Arena::getId).toList();
                return filter(ids, args[2]);
            }
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("арена"))) {
            if (args[1].equalsIgnoreCase("tp")) {
                return filter(List.of("pos1", "pos2", "spectator"), args[3]);
            }
            if (args[1].equalsIgnoreCase("mode")) {
                List<String> modes = new ArrayList<>();
                modes.add("all");
                for (DuelType dt : DuelType.values()) {
                    modes.add(dt.name());
                }
                return filter(modes, args[3]);
            }
            if (args[1].equalsIgnoreCase("setheight")) {
                return filter(List.of("15", "20", "25", "30"), args[3]);
            }
            if (args[1].equalsIgnoreCase("preview")) {
                return filter(List.of("5", "10", "15", "20", "30"), args[3]);
            }
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("kit") || args[0].equalsIgnoreCase("кит"))) {
            return filter(List.of("create", "delete", "list"), args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("kit") || args[0].equalsIgnoreCase("кит"))) {
            if (args[1].equalsIgnoreCase("delete")) {
                List<String> ids = duelManager.getKitManager().getAllKits().stream().map(Kit::id).toList();
                return filter(ids, args[2]);
            }
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("выдать"))) {
            return filter(List.of("ticket"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> src, String prefix) {
        List<String> res = new ArrayList<>();
        for (String s : src) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) res.add(s);
        }
        return res;
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    public static final class ArenaSetupSession {
        private final String arenaId;
        private Location combatBound1;
        private Location specBound1;

        public ArenaSetupSession(String arenaId) {
            this.arenaId = arenaId;
        }

        public String getArenaId() {
            return arenaId;
        }

        public Location getCombatBound1() {
            return combatBound1;
        }

        public void setCombatBound1(Location combatBound1) {
            this.combatBound1 = combatBound1;
        }

        public Location getSpecBound1() {
            return specBound1;
        }

        public void setSpecBound1(Location specBound1) {
            this.specBound1 = specBound1;
        }
    }
}
