package weatherbot.kalshi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads markets from Kalshi's public API. Market data needs no account or key, and this
 * client never places orders: every trade in this project is on paper.
 */
public class KalshiClient {

    private static final String BASE = "https://api.elections.kalshi.com/trade-api/v2";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    /** Every open market in a series. */
    public List<Market> openMarkets(String seriesTicker) throws IOException, InterruptedException {
        return markets("series_ticker=" + enc(seriesTicker) + "&status=open");
    }

    /** Markets in a series that settled after {@code since}, so a daily run doesn't page through all history. */
    public List<Market> settledSince(String seriesTicker, Instant since) throws IOException, InterruptedException {
        return markets("series_ticker=" + enc(seriesTicker) + "&status=settled&min_close_ts=" + since.getEpochSecond());
    }

    private List<Market> markets(String query) throws IOException, InterruptedException {
        List<Market> all = new ArrayList<>();
        String cursor = "";
        do {
            String url = BASE + "/markets?limit=200&" + query + (cursor.isEmpty() ? "" : "&cursor=" + enc(cursor));
            JsonNode page = get(url);
            all.addAll(parseMarkets(page));
            cursor = page.path("cursor").asText("");
        } while (!cursor.isEmpty());
        return all;
    }

    /** Parses one page of {@code GET /markets}. Markets that aren't temperature ranges are skipped. */
    static List<Market> parseMarkets(JsonNode page) {
        List<Market> markets = new ArrayList<>();
        for (JsonNode m : page.path("markets")) {
            StrikeType type;
            try {
                type = StrikeType.valueOf(m.path("strike_type").asText("").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                continue;
            }
            markets.add(new Market(
                    m.path("ticker").asText(),
                    m.path("event_ticker").asText(),
                    m.path("title").asText(""),
                    type,
                    m.hasNonNull("floor_strike") ? m.get("floor_strike").asDouble() : null,
                    m.hasNonNull("cap_strike") ? m.get("cap_strike").asDouble() : null,
                    m.path("yes_bid").asInt(0),
                    m.path("yes_ask").asInt(0),
                    m.path("no_bid").asInt(0),
                    m.path("no_ask").asInt(0),
                    m.path("status").asText(""),
                    m.path("result").asText("")));
        }
        return markets;
    }

    private JsonNode get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Kalshi returned " + response.statusCode() + " for " + url);
        }
        return JSON.readTree(response.body());
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
