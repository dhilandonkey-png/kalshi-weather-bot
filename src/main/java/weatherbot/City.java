package weatherbot;

/**
 * A city with Kalshi high-temperature markets. The coordinates are the weather station
 * the markets settle on, so the forecast is for the same spot the result is measured at.
 */
public record City(String name, String seriesTicker, double lat, double lon) {

    public static final java.util.List<City> ALL = java.util.List.of(
            new City("New York (Central Park)", "KXHIGHNY", 40.7789, -73.9692),
            new City("Chicago (Midway)", "KXHIGHCHI", 41.7842, -87.7553),
            new City("Miami (MIA)", "KXHIGHMIA", 25.7881, -80.3169),
            new City("Austin (Bergstrom)", "KXHIGHAUS", 30.1831, -97.6799));
}
