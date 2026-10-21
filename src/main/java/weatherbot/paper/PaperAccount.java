package weatherbot.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import weatherbot.model.Fees;
import weatherbot.model.Order;

/**
 * Fake money, real bookkeeping. Tracks cash in cents, every position, and every
 * prediction, and saves it all to a JSON file so results build up across daily runs.
 */
public class PaperAccount {

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** The saved form of an account. */
    record State(long startingCashCents, long cashCents, List<Position> positions, List<Prediction> predictions) {}

    private final long startingCashCents;
    private long cashCents;
    private final Map<String, Position> positions = new LinkedHashMap<>();
    private final Map<String, Prediction> predictions = new LinkedHashMap<>();

    public PaperAccount(long startingCashCents) {
        if (startingCashCents <= 0) {
            throw new IllegalArgumentException("starting cash must be positive");
        }
        this.startingCashCents = startingCashCents;
        this.cashCents = startingCashCents;
    }

    public long cashCents() {
        return cashCents;
    }

    public long startingCashCents() {
        return startingCashCents;
    }

    public List<Position> positions() {
        return List.copyOf(positions.values());
    }

    public List<Prediction> predictions() {
        return List.copyOf(predictions.values());
    }

    /** One position per market: the bot decides once and doesn't add to or trade out of it. */
    public boolean holds(String ticker) {
        return positions.containsKey(ticker);
    }

    /** Buys on paper at the ask, paying Kalshi's fee. Returns false, buying nothing, if cash is short. */
    public boolean buy(Order order, String eventTicker, LocalDate today) {
        if (holds(order.ticker())) {
            throw new IllegalStateException("already holds " + order.ticker());
        }
        long cost = (long) order.contracts() * order.priceCents() + Fees.takerFeeCents(order.contracts(), order.priceCents());
        if (cost > cashCents) {
            return false;
        }
        cashCents -= cost;
        positions.put(order.ticker(), new Position(order.ticker(), eventTicker, order.side(), order.contracts(),
                order.priceCents(), cost, order.modelProbability(), today.toString(), false, 0));
        return true;
    }

    /** Records the first prediction seen for a market. Later runs don't overwrite it. */
    public void recordPrediction(String ticker, double modelProbability, Double marketProbability) {
        predictions.putIfAbsent(ticker, new Prediction(ticker, modelProbability, marketProbability, null));
    }

    /** Applies a market's result: pays out a held position and grades the prediction. Safe to call twice. */
    public void settle(String ticker, boolean yesWon) {
        Position p = positions.get(ticker);
        if (p != null && !p.settled()) {
            Position settled = p.settle(yesWon);
            positions.put(ticker, settled);
            cashCents += settled.payoutCents();
        }
        Prediction pred = predictions.get(ticker);
        if (pred != null && pred.yesWon() == null) {
            predictions.put(ticker, pred.resolve(yesWon));
        }
    }

    public void save(Path file) throws IOException {
        JSON.writeValue(file.toFile(), new State(startingCashCents, cashCents, positions(), predictions()));
    }

    public static PaperAccount loadOrCreate(Path file, long startingCashCents) throws IOException {
        if (!Files.exists(file)) {
            return new PaperAccount(startingCashCents);
        }
        State s = JSON.readValue(file.toFile(), State.class);
        PaperAccount account = new PaperAccount(s.startingCashCents());
        account.cashCents = s.cashCents();
        for (Position p : new ArrayList<>(s.positions())) {
            account.positions.put(p.ticker(), p);
        }
        for (Prediction p : s.predictions()) {
            account.predictions.put(p.ticker(), p);
        }
        return account;
    }
}
