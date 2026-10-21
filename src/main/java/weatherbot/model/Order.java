package weatherbot.model;

/**
 * A trade the strategy wants to make.
 *
 * @param modelProbability the model's probability that this side wins
 * @param edge             model probability minus the price, after fees, per contract
 */
public record Order(String ticker, Side side, int contracts, int priceCents, double modelProbability, double edge) {}
