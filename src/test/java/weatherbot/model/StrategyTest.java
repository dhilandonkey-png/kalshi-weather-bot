package weatherbot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import weatherbot.kalshi.Market;
import weatherbot.kalshi.StrikeType;

class StrategyTest {

    private final Strategy strategy = new Strategy(0.25, 0.05, 50);

    private static Market priced(int yesAsk, int noAsk) {
        return new Market("KXHIGHNY-26OCT22-B71.5", "KXHIGHNY-26OCT22", "", StrikeType.BETWEEN, 71.0, 72.0,
                yesAsk - 1, yesAsk, noAsk - 1, noAsk, "open", "");
    }

    @Test
    void kellyMatchesTheFormula() {
        // q = 0.6 for a 40¢ contract: (0.6 − 0.4) / 0.6 = 1/3 of the bankroll.
        assertEquals(1.0 / 3, Strategy.kelly(0.6, 0.4), 1e-12);
        assertEquals(0, Strategy.kelly(0.3, 0.4));
    }

    @Test
    void buysYesWhenTheModelIsAboveThePrice() {
        var order = strategy.decide(priced(30, 72), 0.50, 100_000).orElseThrow();

        assertEquals(Side.YES, order.side());
        assertEquals(30, order.priceCents());
        // edge = 0.50 − 0.30 − 0.02 fee
        assertEquals(0.18, order.edge(), 1e-9);
    }

    @Test
    void buysNoWhenTheModelIsBelowThePrice() {
        var order = strategy.decide(priced(70, 32), 0.40, 100_000).orElseThrow();

        assertEquals(Side.NO, order.side());
        assertEquals(32, order.priceCents());
    }

    @Test
    void skipsWhenTheEdgeIsTooSmallAfterFees() {
        // Model 0.34 vs a 30¢ ask: 4¢ before fees, 2¢ after, below the 5¢ minimum.
        assertTrue(strategy.decide(priced(30, 72), 0.34, 100_000).isEmpty());
    }

    @Test
    void skipsSidesNobodyIsSelling() {
        assertTrue(strategy.decide(priced(1, 1), 0.5, 100_000).isPresent());
        var noAsks = new Market("X", "KXHIGHNY-26OCT22", "", StrikeType.GREATER, 75.0, null, 0, 0, 0, 0, "open", "");
        assertTrue(strategy.decide(noAsks, 0.99, 100_000).isEmpty());
    }

    @Test
    void sizeFollowsQuarterKellyAndTheCap() {
        // q = 0.5 at 30¢ + 2¢ fee: full Kelly = (0.5 − 0.32) / 0.68 ≈ 0.2647; quarter ≈ 0.0662.
        // On a $100 bankroll that's $6.61, and each contract costs 32¢ → 20 contracts.
        assertEquals(20, strategy.decide(priced(30, 72), 0.5, 10_000).orElseThrow().contracts());
        // On a huge bankroll the 50-contract cap applies.
        assertEquals(50, strategy.decide(priced(30, 72), 0.5, 10_000_000).orElseThrow().contracts());
        // Too small a bankroll to afford one contract: no order.
        assertTrue(strategy.decide(priced(30, 72), 0.5, 100).isEmpty());
    }
}
