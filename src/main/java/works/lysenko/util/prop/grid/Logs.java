package works.lysenko.util.prop.grid;

import static works.lysenko.util.spec.PropEnum._GRID_LOG_LINE_MAX_LENGTH;

/**
 * Represents a logging entity with a maximum line length limit.
 * <p>
 * This record contains a single static final integer, `limit`, which defines the maximum length of a log line.
 * The value of `limit` is retrieved from the application properties, where it is defined as `_GRID_LOG_LINE_MAX_LENGTH`.
 */
public record Logs() {

    /**
     * Defines the maximum allowable length for a log line.
     * <p>
     * This static final integer represents a limit on the number of characters
     * that a single log line can contain. Its value is dynamically retrieved
     * from the application properties through the key `_GRID_LOG_LINE_MAX_LENGTH`.
     */
    public static final int limit = _GRID_LOG_LINE_MAX_LENGTH.get();
}
