package weatherbot.paper;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Grades the bot. Profit alone is noisy over a few weeks, so it also compares Brier
 * scores: the average of (probability − outcome)², where 0 is perfect and 0.25 is what
 * always guessing 50% gets. If the model's score isn't lower than the market's on the
 * same markets, the model has no real edge, whatever the profit says.
 */
public record Scorecard(long startingCashCents, long cashCents, long openCostCents, long realizedProfitCents,
        int settledPositions, int winningPositions, int gradedPredictions, OptionalDouble modelBrier,
        OptionalDouble marketBrier) {

    public static Scorecard of(PaperAccount account) {
        long openCost = 0;
        long realized = 0;
        int settled = 0;
        int wins = 0;
        for (Position p : account.positions()) {
            if (p.settled()) {
                settled++;
                realized += p.profitCents();
                if (p.payoutCents() > 0) {
                    wins++;
                }
            } else {
                openCost += p.costCents();
            }
        }
        // Compare model and market on exactly the same markets: those graded and with a market price.
        List<Prediction> graded = account.predictions().stream()
                .filter(p -> p.yesWon() != null && p.marketProbability() != null)
                .toList();
        return new Scorecard(account.startingCashCents(), account.cashCents(), openCost, realized, settled, wins,
                graded.size(),
                brier(graded, true),
                brier(graded, false));
    }

    private static OptionalDouble brier(List<Prediction> graded, boolean model) {
        return graded.stream()
                .mapToDouble(p -> {
                    double prob = model ? p.modelProbability() : p.marketProbability();
                    double outcome = p.yesWon() ? 1 : 0;
                    return (prob - outcome) * (prob - outcome);
                })
                .average();
    }
}
