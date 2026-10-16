package dev.lovelace.loveduels.gui;

/** Pure pagination arithmetic, kept separate from Bukkit so it can be unit-tested. */
final class PageMath {

    private PageMath() {}

    static int pageCount(int total, int perPage) {
        return Math.max(1, (total + perPage - 1) / perPage);
    }

    /** Clamps into [0, pageCount-1]; handles a list that shrank while the GUI was open. */
    static int clampPage(int page, int total, int perPage) {
        return Math.max(0, Math.min(page, pageCount(total, perPage) - 1));
    }

    static int fromIndex(int page, int perPage) {
        return page * perPage;
    }

    static int toIndex(int page, int total, int perPage) {
        return Math.min(total, (page + 1) * perPage);
    }
}
