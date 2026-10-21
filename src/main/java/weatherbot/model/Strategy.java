package weatherbot.model;

import java.util.Optional;
import weatherbot.kalshi.Market;

/**
 * Decides whether to trade a market and how much, from the model's probability and
 * the market's prices.
 *
 * <p>Buying YES at price P (in dollars) when the model says probability q gains q − P per
 * contract on average. The Kelly criterion turns that edge into a bet size: the fraction
 * of the bankroll to risk is (q − P) / (1 − P). Full Kelly is very aggressive when the
 * model is wrong, so only a fraction of it is used, and each market has a cap.
 */
public class Strategy {

    private final double kellyMultiplier;
    private final double minEdge;
    private final int maxContractsPerMarket;

    /**
     * @param kellyMultiplier       share of the full Kelly bet to place, e.g. 0.25
     * @param minEdge               smallest edge per contract worth trading, in dollars, e.g. 0.05
     * @param maxContractsPerMarket cap on contracts in one market
     */
    public Strategy(double kellyMultiplier, double minEdge, int maxContractsPerMarket) {
        if (!(kellyMultiplier > 0 && kellyMultiplier <= 1)) {
            throw new IllegalArgumentException("kellyMultiplier must be in (0, 1]");
        }
        if (minEdge < 0 || maxContractsPerMarket < 1) {
            throw new IllegalArgumentException("minEdge must be ≥ 0 and maxContractsPerMarket ≥ 1");
        }
        this.kellyMultiplier = kellyMultiplier;
        this.minEdge = minEdge;
        this.maxContractsPerMarket = maxContractsPerMarket;
    }

    /** The best order for this market, or empty if neither side has enough edge. */
    public Optional<Order> decide(Market market, double probabilityYes, long bankrollCents) {
        Optional<Order> yes = consider(market.ticker(), Side.YES, probabilityYes, market.yesAskCents(), bankrollCents);
        Optional<Order> no = consider(market.ticker(), Side.NO, 1 - probabilityYes, market.noAskCents(), bankrollCents);
        if (yes.isPresent() && no.isPresent()) {
            return yes.get().edge() >= no.get().edge() ? yes : no;
        }
        return yes.isPresent() ? yes : no;
    }

    private Optional<Order> consider(String ticker, Side side, double q, int askCents, long bankrollCents) {
        if (askCents < 1 || askCents > 99) {
            return Optional.empty(); // nobody is selling this side
        }
        double price = askCents / 100.0;
        double feePerContract = Fees.takerFeeCents(1, askCents) / 100.0;
        double edge = q - price - feePerContract;
        if (edge < minEdge) {
            return Optional.empty();
        }
        double kellyFraction = kelly(q, price + feePerContract) * kellyMultiplier;
        long budgetCents = (long) Math.floor(kellyFraction * bankrollCents);
        long costPerContract = askCents + Fees.takerFeeCents(1, askCents);
        int contracts = (int) Math.min(maxContractsPerMarket, budgetCents / costPerContract);
        if (contracts < 1) {
            return Optional.empty();
        }
        return Optional.of(new Order(ticker, side, contracts, askCents, q, edge));
    }

    /** Full-Kelly fraction of the bankroll for a contract that costs {@code price} and pays $1 with probability q. */
    static double kelly(double q, double price) {
        if (price >= 1) {
            return 0;
        }
        return Math.max(0, (q - price) / (1 - price));
    }
}
