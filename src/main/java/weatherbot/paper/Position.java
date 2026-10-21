package weatherbot.paper;

import weatherbot.model.Side;

/**
 * Contracts the bot bought on paper.
 *
 * @param costCents   what was paid, fees included
 * @param payoutCents what it paid back at settlement ($1 per winning contract); 0 until then
 */
public record Position(String ticker, String eventTicker, Side side, int contracts, int priceCents, long costCents,
        double modelProbability, String openedOn, boolean settled, long payoutCents) {

    Position settle(boolean yesWon) {
        boolean won = (side == Side.YES) == yesWon;
        return new Position(ticker, eventTicker, side, contracts, priceCents, costCents, modelProbability, openedOn,
                true, won ? contracts * 100L : 0);
    }

    public long profitCents() {
        return settled ? payoutCents - costCents : 0;
    }
}
