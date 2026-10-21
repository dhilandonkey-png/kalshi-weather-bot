package weatherbot.kalshi;

import java.util.Objects;

/**
 * One Kalshi market, e.g. "Will the high in NYC on Oct 22 be 71–72°?". Prices are in
 * cents (1–99); 0 means nobody is offering that side right now.
 *
 * @param result "yes" or "no" once the market has settled, otherwise empty
 */
public record Market(String ticker, String eventTicker, String title, StrikeType strikeType, Double floorStrike,
        Double capStrike, int yesBidCents, int yesAskCents, int noBidCents, int noAskCents, String status,
        String result) {

    public Market {
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(eventTicker, "eventTicker");
        Objects.requireNonNull(strikeType, "strikeType");
        switch (strikeType) {
            case GREATER -> Objects.requireNonNull(floorStrike, "GREATER needs floorStrike");
            case LESS -> Objects.requireNonNull(capStrike, "LESS needs capStrike");
            case BETWEEN -> {
                Objects.requireNonNull(floorStrike, "BETWEEN needs floorStrike");
                Objects.requireNonNull(capStrike, "BETWEEN needs capStrike");
            }
        }
        result = result == null ? "" : result;
    }

    /** Midpoint of the YES bid and ask, as a probability: the market's own estimate. Empty if one side is missing. */
    public java.util.OptionalDouble impliedProbability() {
        if (yesBidCents <= 0 || yesAskCents <= 0 || yesAskCents > 99) {
            return java.util.OptionalDouble.empty();
        }
        return java.util.OptionalDouble.of((yesBidCents + yesAskCents) / 200.0);
    }

    public boolean settled() {
        return result.equals("yes") || result.equals("no");
    }
}
