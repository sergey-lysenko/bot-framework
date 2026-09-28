package works.lysenko.util.prop.logs;

import static works.lysenko.util.spec.PropEnum._LOG_TIME_STAMP_FORMAT;

/**
 * Represents a logging entity with a maximum line length limit.
 * <p>
 * This record contains a single static final integer, `limit`, which defines the maximum length of a log line.
 * The value of `limit` is retrieved from the application properties, where it is defined as `_GRID_LOG_LINE_MAX_LENGTH`.
 */
public record Time() {

    /**
     *
     */
    public static final String stamp = _LOG_TIME_STAMP_FORMAT.get();
}
