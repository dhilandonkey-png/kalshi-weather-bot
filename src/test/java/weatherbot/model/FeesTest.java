package weatherbot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FeesTest {

    @Test
    void feeIsSevenPercentOfPriceTimesOneMinusPriceRoundedUp() {
        // 100 contracts at 50¢: 0.07 × 100 × 0.5 × 0.5 = $1.75
        assertEquals(175, Fees.takerFeeCents(100, 50));
        // 1 contract at 50¢: 1.75¢ rounds up to 2¢
        assertEquals(2, Fees.takerFeeCents(1, 50));
        // 10 contracts at 10¢: 0.07 × 10 × 0.1 × 0.9 = 6.3¢ → 7¢
        assertEquals(7, Fees.takerFeeCents(10, 10));
    }

    @Test
    void feeIsSmallestAtTheExtremes() {
        assertEquals(1, Fees.takerFeeCents(1, 1));
        assertEquals(1, Fees.takerFeeCents(1, 99));
        assertEquals(0, Fees.takerFeeCents(0, 40));
    }

    @Test
    void invalidPricesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Fees.takerFeeCents(1, 0));
        assertThrows(IllegalArgumentException.class, () -> Fees.takerFeeCents(1, 100));
        assertThrows(IllegalArgumentException.class, () -> Fees.takerFeeCents(-1, 50));
    }
}
