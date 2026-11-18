package works.lysenko.util.prop.grid;

import static works.lysenko.util.prop.PropEnum._GRID_RANGER_LOG_ABSENT_RANGE;
import static works.lysenko.util.prop.PropEnum._GRID_RANGER_LOG_BOUNDARIES;
import static works.lysenko.util.prop.PropEnum._GRID_RANGER_LOG_NON_FAILING_OUT_OF_BOUNDS;
import static works.lysenko.util.prop.PropEnum._GRID_RANGER_LOG_ORDER_CHANGE;
import static works.lysenko.util.prop.PropEnum._GRID_RANGER_LOG_OVERSTRETCH;
import static works.lysenko.util.prop.PropEnum._GRID_RANGER_LOG_SCALE_CHECK;
import static works.lysenko.util.prop.PropEnum._GRID_RANGER_LOG_SCALE_DEBUG;
import static works.lysenko.util.prop.PropEnum._GRID_RANGER_LOG_SORT_CHECK;
import static works.lysenko.util.prop.PropEnum._GRID_RANGER_LOG_WIDE_RANGE;
import static works.lysenko.util.prop.PropEnum._GRID_RANGER_LOG_WIDTH;

/**
 * Configuration for ranger logging behavior and formatting.
 * <p>
 * Controls logging flags and width for various range validation events such as boundaries,
 * order changes, scale checks, and overstretches.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Ranges() {

    /**
     * Indicates whether ranger should log when an expected range is absent.
     */
    public static final boolean logAbsentRange = _GRID_RANGER_LOG_ABSENT_RANGE.get();

    /**
     * Indicates whether range boundary evaluations should be logged.
     */
    public static final boolean logBoundaries = _GRID_RANGER_LOG_BOUNDARIES.get();

    /**
     * Indicates whether non-failing out-of-bounds occurrences should be logged.
     */
    public static final boolean logNonFailingOutOfBounds = _GRID_RANGER_LOG_NON_FAILING_OUT_OF_BOUNDS.get();

    /**
     * Indicates whether changes in range ordering should be logged.
     */
    public static final boolean logOrderChange = _GRID_RANGER_LOG_ORDER_CHANGE.get();

    /**
     * Indicates whether range overstretch events should be logged.
     */
    public static final boolean logOverstretch = _GRID_RANGER_LOG_OVERSTRETCH.get();

    /**
     * Indicates whether ranger scale checks should be logged.
     */
    public static final boolean logScaleCheck = _GRID_RANGER_LOG_SCALE_CHECK.get();

    /**
     * Indicates whether scale debug diagnostics should be logged.
     */
    public static final boolean logScaleDebug = _GRID_RANGER_LOG_SCALE_DEBUG.get();

    /**
     * Indicates whether ranger sort checks should be logged.
     */
    public static final boolean logSortCheck = _GRID_RANGER_LOG_SORT_CHECK.get();

    /**
     * Indicates whether wide range warnings or notices should be logged.
     */
    public static final boolean logWideRange = _GRID_RANGER_LOG_WIDE_RANGE.get();

    /**
     * Formatted width (in characters) allocated for ranger log messages.
     */
    public static final int width = _GRID_RANGER_LOG_WIDTH.get();
}
