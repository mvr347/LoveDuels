package dev.lovelace.loveduels.rank;

/**
 * Ступени ранга внутри каждого RankType.
 */
public enum RankTier {
    PAGE(0, "Паж", 0),
    SQUIRE(1, "Оруженосец", 50),
    KNIGHT(2, "Рыцарь", 150),
    BARON(3, "Барон", 350),
    COUNT(4, "Граф", 700),
    DUKE(5, "Герцог", 1200),
    KING(6, "Король", 2000);

    private final int order;
    private final String title;
    private final int minPoints;

    RankTier(int order, String title, int minPoints) {
        this.order = order;
        this.title = title;
        this.minPoints = minPoints;
    }

    public int getOrder() {
        return order;
    }

    public String getTitle() {
        return title;
    }

    public int getMinPoints() {
        return minPoints;
    }

    public static RankTier fromPoints(int points) {
        RankTier best = PAGE;
        for (RankTier t : values()) {
            if (points >= t.minPoints && t.order >= best.order) {
                best = t;
            }
        }
        return best;
    }
}
