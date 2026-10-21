package weatherbot.model;

/**
 * Kalshi's trading fee: 7% of contracts × price × (1 − price), rounded up to the next cent.
 * It's largest for contracts near 50¢ and close to zero near 1¢ or 99¢.
 */
public final class Fees {

    private Fees() {}

    public static long takerFeeCents(long contracts, int priceCents) {
        checkPrice(priceCents);
        if (contracts < 0) {
            throw new IllegalArgumentException("contracts must not be negative");
        }
        // 0.07 × C × (p/100) × ((100−p)/100) dollars = 7·C·p·(100−p) / 10,000 cents.
        long numerator = 7L * contracts * priceCents * (100 - priceCents);
        return Math.ceilDiv(numerator, 10_000);
    }

    static void checkPrice(int priceCents) {
        if (priceCents < 1 || priceCents > 99) {
            throw new IllegalArgumentException("price must be 1–99 cents, got " + priceCents);
        }
    }
}
