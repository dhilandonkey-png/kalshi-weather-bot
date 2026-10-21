package weatherbot.paper;

/**
 * What the model and the market each thought, before the outcome was known. Kept for
 * every market the bot looks at, traded or not, so the model can be graded fairly.
 *
 * @param marketProbability the market's midpoint price as a probability; null if there wasn't one
 * @param yesWon            null until the market settles
 */
public record Prediction(String ticker, double modelProbability, Double marketProbability, Boolean yesWon) {

    Prediction resolve(boolean outcome) {
        return new Prediction(ticker, modelProbability, marketProbability, outcome);
    }
}
