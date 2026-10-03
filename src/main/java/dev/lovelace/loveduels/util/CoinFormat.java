package dev.lovelace.loveduels.util;

import dev.lovelace.lovecore.api.economy.Denomination;
import dev.lovelace.lovecore.api.economy.LoveEconomy;
import dev.lovelace.loveduels.integration.LoveEconomyBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Формат монет как в LoveShop BankerGui: глифы %img_&lt;tag&gt;% xN по номиналам LoveEconomy.
 * Для отображения иконок нужен PlaceholderAPI (и расширение ItemsAdder / font_images).
 */
public final class CoinFormat {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    /** Used only if LoveCore is missing and the config value is unreadable: 1 gold coin in the standard scale. */
    private static final long DEFAULT_ROYAL_MIN_STAKE = 2_000L;

    private CoinFormat() {}

    public static String getCoinGlyph(Denomination den) {
        if (den == null || den.itemId() == null) return wrapGlyph("%img_copper_coin%");
        String id = den.itemId();
        int colon = id.indexOf(':');
        String tag = colon >= 0 ? id.substring(colon + 1) : id;
        return wrapGlyph("%img_" + tag + "%");
    }

    /** White colour before and after the glyph so it never inherits the neighbouring text colour. */
    static String wrapGlyph(String placeholder) {
        return "<white>" + placeholder + "</white>";
    }

    /**
     * Coin name by its id only. 2026-10-03: the old value thresholds (>= 1000 netherite, >= 100 diamond ...)
     * labelled every coin one tier too high after the denominations changed to 1/100/2000/20000.
     */
    public static String getCoinName(Denomination den) {
        if (den == null) return "Монета";
        String id = den.itemId() != null ? den.itemId().toLowerCase() : "";
        if (id.contains("netherite")) return "<gradient:#9B51E0:#BB6BD9>Незеритовая монета</gradient>";
        if (id.contains("diamond")) return "<gradient:#00C9FF:#92FE9D>Алмазная монета</gradient>";
        if (id.contains("gold")) return "<gradient:#FFE000:#799F0C>Золотая монета</gradient>";
        if (id.contains("iron")) return "<gradient:#E0E0E0:#F2F2F2>Железная монета</gradient>";
        if (id.contains("copper")) return "<gradient:#E67E22:#D35400>Медная монета</gradient>";
        return "Монета";
    }

    /** Minimum money stake of a royal duel ({@code royal_duel.min_stake}, default "1g"). */
    public static long royalMinStake() {
        var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("LoveDuels");
        if (plugin == null) return DEFAULT_ROYAL_MIN_STAKE;
        try {
            return Math.max(1L, dev.lovelace.lovecore.api.economy.MoneyConfig.get(
                    plugin.getConfig(), "royal_duel.min_stake", DEFAULT_ROYAL_MIN_STAKE));
        } catch (Throwable t) {
            return DEFAULT_ROYAL_MIN_STAKE;
        }
    }

    /** Amount as coin glyphs (MiniMessage, resolve with {@link #resolveGlyphs}); plain text without LoveCore. */
    public static String amount(long amount) {
        Optional<LoveEconomy> eco = tryEconomy();
        return eco.isPresent() ? formatGlyphs(eco.get(), amount) : amount + " монет";
    }

    /** MiniMessage text with glyphs → component for this player. */
    public static Component component(org.bukkit.entity.Player player, String mm) {
        return MM.deserialize(resolveGlyphs(player, mm));
    }

    public static String formatGlyphs(LoveEconomy eco, long amount) {
        if (eco == null) return wrapGlyph("%img_copper_coin%") + " x0";
        List<Denomination> dens = new ArrayList<>(eco.denominations());
        dens.sort(Comparator.comparingLong(Denomination::value).reversed());

        if (amount <= 0) {
            Denomination smallest = dens.isEmpty() ? null : dens.get(dens.size() - 1);
            return (smallest != null ? getCoinGlyph(smallest) : wrapGlyph("%img_copper_coin%")) + " x0";
        }

        StringBuilder sb = new StringBuilder();
        long remaining = amount;
        for (Denomination den : dens) {
            if (den.value() <= 0) continue;
            long count = remaining / den.value();
            if (count > 0) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(getCoinGlyph(den)).append(" x").append(count);
                remaining %= den.value();
            }
        }
        if (sb.length() == 0) {
            Denomination smallest = dens.isEmpty() ? null : dens.get(dens.size() - 1);
            return (smallest != null ? getCoinGlyph(smallest) : wrapGlyph("%img_copper_coin%")) + " x0";
        }
        return sb.toString();
    }

    public static List<Component> formatGlyphLines(LoveEconomy eco, long amount) {
        List<Component> lines = new ArrayList<>();
        if (eco == null) {
            lines.add(MM.deserialize(wrapGlyph("%img_copper_coin%") + " <yellow>x0</yellow>"));
            return lines;
        }
        List<Denomination> dens = new ArrayList<>(eco.denominations());
        dens.sort(Comparator.comparingLong(Denomination::value).reversed());

        if (amount <= 0) {
            Denomination smallest = dens.isEmpty() ? null : dens.get(dens.size() - 1);
            String glyph = smallest != null ? getCoinGlyph(smallest) : wrapGlyph("%img_copper_coin%");
            lines.add(MM.deserialize(glyph + " <yellow>x0</yellow>"));
            return lines;
        }

        long remaining = amount;
        for (Denomination den : dens) {
            if (den.value() <= 0) continue;
            long count = remaining / den.value();
            if (count > 0) {
                lines.add(MM.deserialize(getCoinGlyph(den) + " <yellow>x" + count + "</yellow>"));
                remaining %= den.value();
            }
        }
        if (lines.isEmpty()) {
            Denomination smallest = dens.isEmpty() ? null : dens.get(dens.size() - 1);
            String glyph = smallest != null ? getCoinGlyph(smallest) : wrapGlyph("%img_copper_coin%");
            lines.add(MM.deserialize(glyph + " <yellow>x0</yellow>"));
        }
        return lines;
    }

    /** Прогон через PlaceholderAPI для %img_iron_coin% и т.п. */
    public static String applyPlaceholders(org.bukkit.entity.Player player, String text) {
        if (text == null || text.isEmpty() || player == null) return text;
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return text;
        try {
            var pl = org.bukkit.Bukkit.getPluginManager().getPlugin("LoveDuels");
            if (pl != null && !pl.getConfig().getBoolean("coins.use_placeholderapi", true)) {
                return text;
            }
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable ignored) {
            return text;
        }
    }

    private static final java.util.regex.Pattern GLYPH = java.util.regex.Pattern.compile("%img_[A-Za-z0-9_]+%");
    private static final java.util.regex.Pattern LEGACY = java.util.regex.Pattern.compile("[\u00A7&][0-9a-fk-orx]");

    /**
     * Resolves only the %img_*% glyph placeholders of a MiniMessage string through PlaceholderAPI.
     * Legacy colour codes in the result are stripped: they are not understood by MiniMessage and the
     * surrounding <white> tag already colours the glyph.
     */
    public static String resolveGlyphs(org.bukkit.entity.Player player, String mm) {
        if (mm == null || mm.indexOf("%img_") < 0) return mm;
        return GLYPH.matcher(mm).replaceAll(r -> {
            String raw = r.group();
            String out = applyPlaceholders(player, raw);
            if (out == null || out.equals(raw)) return java.util.regex.Matcher.quoteReplacement(raw);
            return java.util.regex.Matcher.quoteReplacement(LEGACY.matcher(out).replaceAll(""));
        });
    }

    public static java.util.List<String> resolveGlyphs(org.bukkit.entity.Player player, java.util.List<String> lines) {
        java.util.List<String> res = new ArrayList<>(lines.size());
        for (String l : lines) res.add(resolveGlyphs(player, l));
        return res;
    }

    public static String formatBalanceLine(LoveEconomyBridge bridge, long amount) {
        Optional<LoveEconomy> eco = tryEconomy();
        if (eco.isPresent()) {
            return formatGlyphs(eco.get(), amount);
        }
        return "<yellow>" + amount + "</yellow> <gray>" + (bridge != null ? bridge.currencyName() : "монет") + "</gray>";
    }

    public static Optional<LoveEconomy> tryEconomy() {
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LoveCore")) {
            return Optional.empty();
        }
        try {
            return dev.lovelace.lovecore.api.LoveCore.service(LoveEconomy.class);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }
}
