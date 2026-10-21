package weatherbot.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import weatherbot.model.Order;
import weatherbot.model.Side;

class PaperAccountTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);

    private static Order order(String ticker, Side side, int contracts, int price) {
        return new Order(ticker, side, contracts, price, 0.6, 0.1);
    }

    @Test
    void buyingPaysPriceAndFee() {
        var account = new PaperAccount(10_000);
        assertTrue(account.buy(order("A", Side.YES, 10, 40), "E", DAY));
        // 10 × 40¢ = $4.00, fee = ceil(0.07 × 10 × 0.4 × 0.6 × 100) = 17¢
        assertEquals(10_000 - 400 - 17, account.cashCents());
    }

    @Test
    void aWinningPositionPaysADollarPerContract() {
        var account = new PaperAccount(10_000);
        account.buy(order("A", Side.YES, 10, 40), "E", DAY);
        account.settle("A", true);

        Position p = account.positions().get(0);
        assertEquals(1_000, p.payoutCents());
        assertEquals(1_000 - 417, p.profitCents());
        assertEquals(10_000 - 417 + 1_000, account.cashCents());
    }

    @Test
    void aNoPositionWinsWhenTheEventDoesntHappen() {
        var account = new PaperAccount(10_000);
        account.buy(order("A", Side.NO, 5, 70), "E", DAY);
        account.settle("A", false);
        assertEquals(500, account.positions().get(0).payoutCents());
    }

    @Test
    void settlingTwiceDoesNotPayTwice() {
        var account = new PaperAccount(10_000);
        account.buy(order("A", Side.YES, 10, 40), "E", DAY);
        account.settle("A", true);
        long cash = account.cashCents();
        account.settle("A", true);
        assertEquals(cash, account.cashCents());
    }

    @Test
    void cantSpendMoreThanItHasOrBuyTheSameMarketTwice() {
        var account = new PaperAccount(1_000);
        assertFalse(account.buy(order("A", Side.YES, 50, 40), "E", DAY));
        assertEquals(1_000, account.cashCents());

        assertTrue(account.buy(order("B", Side.YES, 1, 40), "E", DAY));
        assertThrows(IllegalStateException.class, () -> account.buy(order("B", Side.YES, 1, 40), "E", DAY));
    }

    @Test
    void theFirstPredictionIsKeptAndGradedOnSettlement() {
        var account = new PaperAccount(1_000);
        account.recordPrediction("A", 0.7, 0.5);
        account.recordPrediction("A", 0.1, 0.9);
        assertNull(account.predictions().get(0).yesWon());

        account.settle("A", true);
        Prediction p = account.predictions().get(0);
        assertEquals(0.7, p.modelProbability());
        assertTrue(p.yesWon());
    }

    @Test
    void saveAndLoadRoundTrips(@TempDir Path dir) throws Exception {
        var account = new PaperAccount(10_000);
        account.buy(order("A", Side.YES, 10, 40), "E", DAY);
        account.recordPrediction("A", 0.6, null);
        account.settle("A", false);
        Path file = dir.resolve("state.json");
        account.save(file);

        var loaded = PaperAccount.loadOrCreate(file, 999);
        assertEquals(account.cashCents(), loaded.cashCents());
        assertEquals(account.startingCashCents(), loaded.startingCashCents());
        assertEquals(account.positions(), loaded.positions());
        assertEquals(account.predictions(), loaded.predictions());
    }

    @Test
    void scorecardComparesModelAndMarketOnTheSameMarkets() {
        var account = new PaperAccount(10_000);
        account.recordPrediction("A", 0.9, 0.6);  // happened: model 0.01, market 0.16
        account.recordPrediction("B", 0.2, 0.5);  // didn't:   model 0.04, market 0.25
        account.recordPrediction("C", 0.5, null); // no market price: left out of both
        account.settle("A", true);
        account.settle("B", false);
        account.settle("C", true);

        Scorecard s = Scorecard.of(account);
        assertEquals(2, s.gradedPredictions());
        assertEquals(0.025, s.modelBrier().getAsDouble(), 1e-12);
        assertEquals(0.205, s.marketBrier().getAsDouble(), 1e-12);
    }
}
