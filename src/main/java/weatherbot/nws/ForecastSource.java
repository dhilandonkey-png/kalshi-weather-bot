package weatherbot.nws;

import java.io.IOException;
import java.time.LocalDate;
import java.util.OptionalInt;

/** Where forecast highs come from. The real one is {@link NwsClient}; tests use a fixed one. */
public interface ForecastSource {

    /** The forecast high in °F for {@code date}, or empty if that day isn't in the forecast. */
    OptionalInt forecastHighF(double lat, double lon, LocalDate date) throws IOException, InterruptedException;
}
