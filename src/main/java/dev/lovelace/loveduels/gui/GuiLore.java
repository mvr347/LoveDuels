package dev.lovelace.loveduels.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Retains formatted values while separating the description from the actual click hints. */
final class GuiLore {
    private GuiLore() {}

    static List<Component> normalize(
            List<Component> source, Component subtitle, Component separator) {
        if (source == null || source.isEmpty()) return List.of();
        var body = new ArrayList<Component>();
        var clicks = new ArrayList<Component>();
        for (var line : source) {
            String text = PlainTextComponentSerializer.plainText().serialize(line).strip();
            if (text.matches("[─━—_-]{3,}")) continue;
            if (text.matches("(?i)^(?:Shift\\s*\\+\\s*)?(?:ЛКМ|ПКМ)(?:\\s|$).*")) clicks.add(line);
            else body.add(line);
        }
        while (!body.isEmpty()
                && PlainTextComponentSerializer.plainText().serialize(body.getLast()).isBlank())
            body.removeLast();
        if (body.isEmpty() || !NamedTextColor.DARK_GRAY.equals(body.getFirst().color())) {
            body.addFirst(Component.empty());
            body.addFirst(subtitle);
        }
        if (!clicks.isEmpty()) {
            body.add(separator);
            body.addAll(clicks);
        }
        return List.copyOf(body);
    }
}
