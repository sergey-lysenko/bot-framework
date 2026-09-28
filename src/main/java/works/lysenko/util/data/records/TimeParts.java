package works.lysenko.util.data.records;

/**
 * Represents a breakdown of a time duration into its constituent parts:
 * milliseconds, days, hours, minutes, and seconds.
 */
public record TimeParts(long ms, long days, long hours, long minutes, long seconds) {

    /**
     * Represents the number of milliseconds in one day.
     * It is a constant value and cannot be modified.
     */
    private static final long MS_IN_DAY = 86400000;

    /**
     * Represents the number of milliseconds in one hour.
     * It is a constant value and cannot be modified.
     */

    private static final long MS_IN_HOUR = 3600000;
    /**
     * Represents the number of milliseconds in one minute.
     * It is a constant value and cannot be modified.
     */

    private static final long MS_IN_MINUTE = 60000;

    /**
     * Converts a duration given in milliseconds into its constituent parts: days,
     * hours, minutes, seconds, and milliseconds.
     *
     * @param milliseconds the duration in milliseconds to be converted
     * @return a TimeParts object containing the breakdown of the duration into days,
     * hours, minutes, seconds, and milliseconds
     */
    public static TimeParts get(final long milliseconds) {

        long ms = milliseconds;
        final long days = ms / MS_IN_DAY;
        ms = ms % MS_IN_DAY;
        final long hours = ms / MS_IN_HOUR;
        ms = ms % MS_IN_HOUR;
        final long minutes = ms / MS_IN_MINUTE;
        ms = ms % MS_IN_MINUTE;
        final long seconds = ms / 1000;
        ms = ms % 1000;
        return new TimeParts(ms, days, hours, minutes, seconds);
    }
}
