package weatherbot.kalshi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The fixture follows the field names in Kalshi's API docs. It was written by hand, not
 * captured from a live response, so the first real run should be checked against it.
 */
class KalshiParsingTest {

    private static List<Market> fixture() throws Exception {
        try (var in = KalshiParsingTest.class.getResourceAsStream("/kalshi-markets.json")) {
            return KalshiClient.parseMarkets(new ObjectMapper().readTree(in));
        }
    }

    @Test
    void parsesTemperatureMarketsAndSkipsOthers() throws Exception {
        List<Market> markets = fixture();
        assertEquals(3, markets.size());

        Market range = markets.get(0);
        assertEquals(StrikeType.BETWEEN, range.strikeType());
        assertEquals(71.0, range.floorStrike());
        assertEquals(72.0, range.capStrike());
        assertEquals(31, range.yesAskCents());
        assertEquals(0.295, range.impliedProbability().getAsDouble(), 1e-12);
        assertFalse(range.settled());

        Market above = markets.get(1);
        assertEquals(StrikeType.GREATER, above.strikeType());
        assertNull(above.capStrike());
        assertTrue(above.impliedProbability().isEmpty(), "no bid, so no midpoint");

        Market settled = markets.get(2);
        assertTrue(settled.settled());
        assertEquals("no", settled.result());
    }

    @Test
    void readsTheDateFromTheEventTicker() {
        assertEquals(LocalDate.of(2026, 10, 22), EventDates.dateOf("KXHIGHNY-26OCT22").orElseThrow());
        assertEquals(LocalDate.of(2027, 1, 5), EventDates.dateOf("KXHIGHCHI-27JAN05").orElseThrow());
        assertTrue(EventDates.dateOf("KXHIGHNY").isEmpty());
        assertTrue(EventDates.dateOf("KXHIGHNY-NOTADATE").isEmpty());
    }
}
