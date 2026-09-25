package dev.lovelace.loveduels.core;

/**
 * Immutable record representing the stake for a duel.
 *
 * @param moneyBet amount of money (LoveEconomy) staked by EACH player
 * @param honorBet amount of honor (rating) staked by EACH player
 */
public record DuelBet(long moneyBet, int honorBet) {

    public static final DuelBet ZERO = new DuelBet(0L, 0);

    public DuelBet {
        if (moneyBet < 0) {
            throw new IllegalArgumentException("Money bet cannot be negative: " + moneyBet);
        }
        if (honorBet < 0) {
            throw new IllegalArgumentException("Honor bet cannot be negative: " + honorBet);
        }
    }

    public boolean hasMoney() {
        return moneyBet > 0;
    }

    public boolean hasHonor() {
        return honorBet > 0;
    }

    public boolean isFree() {
        return moneyBet == 0 && honorBet == 0;
    }

    public long totalMoneyPrizePool() {
        return moneyBet * 2;
    }

    public int totalHonorPrizePool() {
        return honorBet * 2;
    }
}
