package dev.lovelace.loveduels.gui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

class GuiLoreTest {
    final Component subtitle = Component.text("Турниры и дуэли", NamedTextColor.DARK_GRAY);
    final Component separator = Component.text("─────────────────────", NamedTextColor.DARK_GRAY);

    @Test
    void clicksAlwaysFollowTheSeparatorAndKeepTheirFormatting() {
        var value = Component.text("Цена: 12", NamedTextColor.WHITE);
        var left = Component.text("ЛКМ — увеличить", NamedTextColor.GREEN);
        var shift = Component.text("Shift+ЛКМ — сменить номинал", NamedTextColor.YELLOW);
        assertEquals(
                List.of(subtitle, Component.empty(), value, separator, left, shift),
                GuiLore.normalize(List.of(value, left, shift), subtitle, separator));
    }

    @Test
    void anInformativeCardGetsNoInventedClickAndKeepsItsSubtitle() {
        var value = Component.text("Побед: 12");
        assertEquals(
                List.of(subtitle, Component.empty(), value),
                GuiLore.normalize(
                        List.of(subtitle, Component.empty(), value), subtitle, separator));
        assertEquals(List.of(), GuiLore.normalize(List.of(), subtitle, separator));
    }
}
