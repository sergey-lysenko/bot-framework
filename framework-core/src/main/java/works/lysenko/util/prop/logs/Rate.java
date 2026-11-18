package works.lysenko.util.prop.logs;

import static works.lysenko.util.prop.PropEnum._LOGS_RATE_1;
import static works.lysenko.util.prop.PropEnum._LOGS_RATE_2;
import static works.lysenko.util.prop.PropEnum._LOGS_RATE_3;
import static works.lysenko.util.prop.PropEnum._LOGS_RATE_4;

/**
 * Time span thresholds used for color-coding log record intervals.
 * <p>
 * Defines duration boundaries (in milliseconds) for rendering the elapsed time
 * between consecutive log records with distinct ANSI colors (blue, green, yellow, red).
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Rate() {

    /**
     * Duration threshold (in milliseconds) above which the elapsed time span is formatted in red.
     */
    public static final int red = _LOGS_RATE_4.get();

    /**
     * Duration threshold (in milliseconds) above which the elapsed time span is formatted in yellow.
     */
    public static final int yellow = _LOGS_RATE_3.get();

    /**
     * Duration threshold (in milliseconds) above which the elapsed time span is formatted in green.
     */
    public static final int green = _LOGS_RATE_2.get();

    /**
     * Duration threshold (in milliseconds) above which the elapsed time span is formatted in blue.
     */
    public static final int blue = _LOGS_RATE_1.get();
}
