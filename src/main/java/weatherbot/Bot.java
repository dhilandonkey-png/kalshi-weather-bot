package weatherbot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import weatherbot.kalshi.EventDates;
import weatherbot.kalshi.KalshiClient;
import weatherbot.kalshi.Market;
import weatherbot.model.Order;
import weatherbot.model.Strategy;
import weatherbot.model.TemperatureModel;
import weatherbot.nws.ForecastSource;
import weatherbot.nws.NwsClient;
import weatherbot.paper.PaperAccount;

/**
 * One daily run: settle markets that finished, then look at open markets, compare the
 * model's probability to the price, and paper-trade where the edge is big enough.
 * Run with {@code ./gradlew run}; state lives in {@code state.json}.
 */
public final class Bot {

    static final long STARTING_CASH_CENTS = 1_000_00; // $1,000 of fake money
    static final double SIGMA_F = 3.0;                 // typical forecast miss, °F; to be fit from results
    static final double BIAS_F = 0.0;
    static final double KELLY_MULTIPLIER = 0.25;       // quarter Kelly
    static final double MIN_EDGE = 0.05;               // 5¢ per contract after fees
    static final int MAX_CONTRACTS = 50;

    private Bot() {}

    public static void main(String[] args) throws IOException, InterruptedException {
        Path stateFile = Path.of("state.json");
        PaperAccount account = PaperAccount.loadOrCreate(stateFile, STARTING_CASH_CENTS);
        KalshiClient kalshi = new KalshiClient();
        NwsClient nws = new NwsClient("kalshi-weather-bot (github.com/dhilandonkey-png)");
        TemperatureModel model = new TemperatureModel(SIGMA_F, BIAS_F);
        Strategy strategy = new Strategy(KELLY_MULTIPLIER, MIN_EDGE, MAX_CONTRACTS);
        LocalDate today = LocalDate.now(Clock.system(ZoneId.of("America/New_York")));

        for (City city : City.ALL) {
            System.out.println("\n== " + city.name());
            try {
                settle(account, kalshi.settledSince(city.seriesTicker(), Instant.now().minus(Duration.ofDays(10))));
                trade(account, city, kalshi.openMarkets(city.seriesTicker()), nws, model, strategy, today);
            } catch (IOException e) {
                // One city's outage shouldn't stop the others.
                System.out.println("  skipped: " + e.getMessage());
            }
        }

        account.save(stateFile);
        Files.writeString(Path.of("REPORT.md"), Report.markdown(account, Instant.now().toString()));
        System.out.println("\ncash " + Report.dollars(account.cashCents()) + ", report written to REPORT.md");
    }

    static void settle(PaperAccount account, List<Market> settledMarkets) {
        for (Market m : settledMarkets) {
            if (m.settled()) {
                account.settle(m.ticker(), m.result().equals("yes"));
            }
        }
    }

    static void trade(PaperAccount account, City city, List<Market> open, ForecastSource nws, TemperatureModel model,
            Strategy strategy, LocalDate today) throws IOException, InterruptedException {
        Map<LocalDate, OptionalInt> forecasts = new HashMap<>();
        for (Market m : open) {
            var date = EventDates.dateOf(m.eventTicker());
            if (date.isEmpty() || date.get().isBefore(today)) {
                continue;
            }
            OptionalInt high = forecasts.get(date.get());
            if (high == null) {
                high = nws.forecastHighF(city.lat(), city.lon(), date.get());
                forecasts.put(date.get(), high);
            }
            if (high.isEmpty()) {
                continue;
            }
            double p = model.probabilityYes(m, high.getAsInt());
            account.recordPrediction(m.ticker(), p, m.impliedProbability().isPresent()
                    ? m.impliedProbability().getAsDouble() : null);
            if (account.holds(m.ticker())) {
                continue;
            }
            var order = strategy.decide(m, p, account.cashCents());
            if (order.isPresent() && account.buy(order.get(), m.eventTicker(), today)) {
                Order o = order.get();
                System.out.printf("  %s: forecast %d°F, model %.0f%%, bought %d %s at %d¢ (edge %.0f¢)%n",
                        m.ticker(), high.getAsInt(), p * 100, o.contracts(), o.side(), o.priceCents(), o.edge() * 100);
            }
        }
    }
}
