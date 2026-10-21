package weatherbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import weatherbot.kalshi.Market;
import weatherbot.kalshi.StrikeType;
import weatherbot.model.Side;
import weatherbot.model.Strategy;
import weatherbot.model.TemperatureModel;
import weatherbot.paper.PaperAccount;

/** One full pass of the bot with a fixed forecast instead of the network. */
class BotTest {

    private static final City NYC = City.ALL.get(0);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    private static Market market(String event, String suffix, double floor, double cap, int yesAsk, int noAsk) {
        return new Market(event + "-" + suffix, event, "", StrikeType.BETWEEN, floor, cap, yesAsk - 2, yesAsk,
                noAsk - 2, noAsk, "active", "");
    }

    private final TemperatureModel model = new TemperatureModel(3.0, 0);
    private final Strategy strategy = new Strategy(0.25, 0.05, 50);

    @Test
    void tradesTheMispricedRangeAndRecordsEveryPrediction() throws Exception {
        var account = new PaperAccount(100_000);
        // Forecast 72°: 71–72 is very likely (model ≈ 26%), but priced at 10¢. 79–80 is far off, priced at 40¢.
        List<Market> open = List.of(
                market("KXHIGHNY-26OCT22", "B71.5", 71, 72, 10, 92),
                market("KXHIGHNY-26OCT22", "B79.5", 79, 80, 40, 62),
                market("KXHIGHNY-26OCT22", "B67.5", 67, 68, 12, 90));

        Bot.trade(account, NYC, open, (lat, lon, date) -> OptionalInt.of(72), model, strategy, TODAY);

        assertEquals(3, account.predictions().size());
        var held = account.positions();
        assertTrue(held.stream().anyMatch(p -> p.ticker().endsWith("B71.5") && p.side() == Side.YES));
        assertTrue(held.stream().anyMatch(p -> p.ticker().endsWith("B79.5") && p.side() == Side.NO));
    }

    @Test
    void runningTwiceDoesNotBuyTwice() throws Exception {
        var account = new PaperAccount(100_000);
        List<Market> open = List.of(market("KXHIGHNY-26OCT22", "B71.5", 71, 72, 10, 92));

        Bot.trade(account, NYC, open, (lat, lon, date) -> OptionalInt.of(72), model, strategy, TODAY);
        long cash = account.cashCents();
        Bot.trade(account, NYC, open, (lat, lon, date) -> OptionalInt.of(72), model, strategy, TODAY);

        assertEquals(1, account.positions().size());
        assertEquals(cash, account.cashCents());
    }

    @Test
    void skipsPastDaysAndDaysWithoutAForecast() throws Exception {
        var account = new PaperAccount(100_000);
        List<Market> open = List.of(
                market("KXHIGHNY-26OCT20", "B71.5", 71, 72, 10, 92),   // yesterday
                market("KXHIGHNY-26OCT30", "B71.5", 71, 72, 10, 92));  // beyond the forecast

        Bot.trade(account, NYC, open, (lat, lon, date) -> date.equals(LocalDate.of(2026, 10, 30))
                ? OptionalInt.empty() : OptionalInt.of(72), model, strategy, TODAY);

        assertTrue(account.positions().isEmpty());
        assertTrue(account.predictions().isEmpty());
    }

    @Test
    void settlingPaysOutAndGrades() throws Exception {
        var account = new PaperAccount(100_000);
        Bot.trade(account, NYC, List.of(market("KXHIGHNY-26OCT22", "B71.5", 71, 72, 10, 92)),
                (lat, lon, date) -> OptionalInt.of(72), model, strategy, TODAY);

        Bot.settle(account, List.of(new Market("KXHIGHNY-26OCT22-B71.5", "KXHIGHNY-26OCT22", "", StrikeType.BETWEEN,
                71.0, 72.0, 0, 0, 0, 0, "finalized", "yes")));

        assertTrue(account.positions().get(0).settled());
        assertTrue(account.positions().get(0).profitCents() > 0);
        assertEquals(Boolean.TRUE, account.predictions().get(0).yesWon());
    }
}
