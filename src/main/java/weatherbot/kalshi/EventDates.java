package weatherbot.kalshi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.Optional;

/** Reads the date out of an event ticker like {@code KXHIGHNY-26OCT22} (Oct 22, 2026). */
public final class EventDates {

    private static final DateTimeFormatter FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyMMMdd")
            .toFormatter(Locale.US);

    private EventDates() {}

    public static Optional<LocalDate> dateOf(String eventTicker) {
        int dash = eventTicker.lastIndexOf('-');
        if (dash < 0) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(eventTicker.substring(dash + 1), FORMAT));
        } catch (java.time.format.DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
