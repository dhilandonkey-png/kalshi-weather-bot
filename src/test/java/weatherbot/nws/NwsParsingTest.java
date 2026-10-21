package weatherbot.nws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Hand-written from the NWS API docs' forecast format; check against the first live run. */
class NwsParsingTest {

    private static JsonNode fixture() throws Exception {
        try (var in = NwsParsingTest.class.getResourceAsStream("/nws-forecast.json")) {
            return new ObjectMapper().readTree(in);
        }
    }

    @Test
    void picksTheDaytimePeriodForTheDate() throws Exception {
        // Not "Tonight" (55°) or "Wednesday Night" (58°): the daytime high.
        assertEquals(72, NwsClient.highFor(fixture(), LocalDate.of(2026, 10, 22)).getAsInt());
    }

    @Test
    void convertsCelsius() throws Exception {
        assertEquals(68, NwsClient.highFor(fixture(), LocalDate.of(2026, 10, 23)).getAsInt());
    }

    @Test
    void emptyWhenTheDayIsntInTheForecast() throws Exception {
        assertTrue(NwsClient.highFor(fixture(), LocalDate.of(2026, 11, 30)).isEmpty());
    }
}
