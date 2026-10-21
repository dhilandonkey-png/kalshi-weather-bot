package weatherbot.model;

import weatherbot.kalshi.Market;

/**
 * Turns a forecast high into the probability that a market's temperature range happens.
 *
 * <p>The idea: forecasts are usually close, and their misses are roughly bell-shaped.
 * So the actual high is modeled as normal, centered on the forecast plus a bias, with a
 * spread {@code sigmaF}. Recorded highs are whole degrees, so a range like 71–72° covers
 * every true temperature from 70.5° up to 72.5° (a continuity correction).
 */
public class TemperatureModel {

    private final double sigmaF;
    private final double biasF;

    /**
     * @param sigmaF how far off the forecast typically is, in °F (one standard deviation)
     * @param biasF  added to the forecast, for a forecast that runs consistently warm or cold
     */
    public TemperatureModel(double sigmaF, double biasF) {
        if (!(sigmaF > 0)) {
            throw new IllegalArgumentException("sigmaF must be positive, got " + sigmaF);
        }
        this.sigmaF = sigmaF;
        this.biasF = biasF;
    }

    /** Probability that the market resolves YES, given the forecast high in °F. */
    public double probabilityYes(Market market, double forecastHighF) {
        double mean = forecastHighF + biasF;
        return switch (market.strikeType()) {
            // "above 75°" pays if the recorded high is 76° or more.
            case GREATER -> 1 - cdf(market.floorStrike() + 0.5, mean);
            // "below 68°" pays if the recorded high is 67° or less.
            case LESS -> cdf(market.capStrike() - 0.5, mean);
            // "71° to 72°" pays if the recorded high is 71° or 72°.
            case BETWEEN -> cdf(market.capStrike() + 0.5, mean) - cdf(market.floorStrike() - 0.5, mean);
        };
    }

    private double cdf(double x, double mean) {
        return Normal.cdf((x - mean) / sigmaF);
    }
}
