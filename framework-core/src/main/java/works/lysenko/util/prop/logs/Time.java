package works.lysenko.util.prop.logs;

import static works.lysenko.util.prop.PropEnum._LOGS_TIME_STAMP_FORMAT;

/**
 * Provides timestamp formatting configuration for log records.
 */
public record Time() {

    /**
     * Format pattern used to format log record timestamps.
     */
    public static final String stamp = _LOGS_TIME_STAMP_FORMAT.get();
}
