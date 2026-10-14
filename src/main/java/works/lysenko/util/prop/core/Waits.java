package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._TEST_EWAIT;
import static works.lysenko.util.prop.PropEnum._WEBD_IWAIT;

/**
 * Manages wait timeout properties for the application.
 * <p>
 * Provides implicit and explicit wait timeout durations (in seconds).
 */
public record Waits() {

    /**
     * Implicit wait timeout duration (in seconds) applied to WebDriver lookups.
     */
    public static final int implicit = _WEBD_IWAIT.get();

    /**
     * Explicit wait timeout duration (in seconds) used for expected condition evaluations.
     */
    public static final int explicit = _TEST_EWAIT.get();
}
