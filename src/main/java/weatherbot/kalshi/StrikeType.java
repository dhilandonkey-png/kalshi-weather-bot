package weatherbot.kalshi;

/** How a market's temperature range is defined. */
public enum StrikeType {
    /** YES if the high is above {@code floorStrike}. */
    GREATER,
    /** YES if the high is below {@code capStrike}. */
    LESS,
    /** YES if the high is from {@code floorStrike} to {@code capStrike}, inclusive. */
    BETWEEN
}
