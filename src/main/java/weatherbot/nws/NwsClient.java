package weatherbot.nws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.OptionalInt;

/**
 * Gets the National Weather Service's forecast high for a place and day. Free and
 * keyless; the NWS only asks for a User-Agent that says who is calling.
 */
public class NwsClient implements ForecastSource {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final String userAgent;

    public NwsClient(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public OptionalInt forecastHighF(double lat, double lon, LocalDate date) throws IOException, InterruptedException {
        // Step 1: find which forecast grid covers this point. Step 2: read that grid's forecast.
        JsonNode point = get(String.format(java.util.Locale.ROOT, "https://api.weather.gov/points/%.4f,%.4f", lat, lon));
        String forecastUrl = point.path("properties").path("forecast").asText();
        if (forecastUrl.isEmpty()) {
            throw new IOException("NWS has no forecast for " + lat + "," + lon);
        }
        return highFor(get(forecastUrl), date);
    }

    /** Picks the daytime period on {@code date} from a forecast response and returns its temperature in °F. */
    static OptionalInt highFor(JsonNode forecast, LocalDate date) {
        for (JsonNode period : forecast.path("properties").path("periods")) {
            if (!period.path("isDaytime").asBoolean(false)) {
                continue;
            }
            LocalDate day = OffsetDateTime.parse(period.path("startTime").asText()).toLocalDate();
            if (day.equals(date)) {
                int t = period.path("temperature").asInt();
                return OptionalInt.of(period.path("temperatureUnit").asText("F").equals("C")
                        ? (int) Math.round(t * 9 / 5.0 + 32)
                        : t);
            }
        }
        return OptionalInt.empty();
    }

    private JsonNode get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .header("Accept", "application/geo+json")
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("NWS returned " + response.statusCode() + " for " + url);
        }
        return JSON.readTree(response.body());
    }
}
