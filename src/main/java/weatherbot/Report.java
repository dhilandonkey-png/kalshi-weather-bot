package weatherbot;

import java.util.Comparator;
import java.util.OptionalDouble;
import weatherbot.paper.PaperAccount;
import weatherbot.paper.Position;
import weatherbot.paper.Scorecard;

/** Writes REPORT.md: the running scorecard and every position, newest first. */
final class Report {

    private Report() {}

    static String markdown(PaperAccount account, String updatedAt) {
        Scorecard s = Scorecard.of(account);
        StringBuilder md = new StringBuilder();
        md.append("# Paper trading report\n\n");
        md.append("Updated ").append(updatedAt).append(". Fake money only; see the README.\n\n");
        md.append("| | |\n|---|---|\n");
        md.append("| Starting cash | ").append(dollars(s.startingCashCents())).append(" |\n");
        md.append("| Cash now | ").append(dollars(s.cashCents())).append(" |\n");
        md.append("| In open positions (cost) | ").append(dollars(s.openCostCents())).append(" |\n");
        md.append("| Realized profit | ").append(dollars(s.realizedProfitCents())).append(" |\n");
        md.append("| Settled positions | ").append(s.settledPositions()).append(" (")
                .append(s.winningPositions()).append(" won) |\n");
        md.append("| Predictions graded | ").append(s.gradedPredictions()).append(" |\n");
        md.append("| Brier score, model | ").append(score(s.modelBrier())).append(" |\n");
        md.append("| Brier score, market | ").append(score(s.marketBrier())).append(" |\n\n");
        md.append("Lower Brier is better. The model only has a real edge if its score beats the market's.\n\n");

        md.append("## Positions\n\n");
        md.append("| Opened | Market | Side | Contracts | Price | Model | Cost | Result |\n");
        md.append("|---|---|---|---|---|---|---|---|\n");
        account.positions().stream()
                .sorted(Comparator.comparing(Position::openedOn).reversed())
                .forEach(p -> md.append("| ").append(p.openedOn())
                        .append(" | `").append(p.ticker()).append('`')
                        .append(" | ").append(p.side())
                        .append(" | ").append(p.contracts())
                        .append(" | ").append(p.priceCents()).append("¢")
                        .append(" | ").append(Math.round(p.modelProbability() * 100)).append('%')
                        .append(" | ").append(dollars(p.costCents()))
                        .append(" | ").append(p.settled() ? (p.payoutCents() > 0 ? "won " : "lost ")
                                + dollars(p.profitCents()) : "open")
                        .append(" |\n"));
        return md.toString();
    }

    static String dollars(long cents) {
        long abs = Math.abs(cents);
        return String.format("%s$%d.%02d", cents < 0 ? "-" : "", abs / 100, abs % 100);
    }

    private static String score(OptionalDouble d) {
        return d.isPresent() ? String.format("%.4f", d.getAsDouble()) : "not enough data yet";
    }
}
