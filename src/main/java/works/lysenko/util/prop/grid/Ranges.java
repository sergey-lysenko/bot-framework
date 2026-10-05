package works.lysenko.util.prop.grid;

import static works.lysenko.util.spec.PropEnum.*;

/**
 * A1 class for managing range properties within the application.
 * <p>
 * This class contains the following static property:
 * - width: The width of the ranger log.
 * <p>
 * The width value is obtained using the properties instance with the specified key.
 */
@SuppressWarnings({"MissingJavadoc", "NonFinalStaticVariableUsedInClassInitialization", "StaticMethodOnlyUsedInOneClass"})
public record Ranges() {

    public static final boolean logAbsentRange = _GRID_RANGER_LOG_ABSENT_RANGE.get();
    public static final boolean logBoundaries = _GRID_RANGER_LOG_BOUNDARIES.get();
    public static final boolean logNonFailingOutOfBounds = _GRID_RANGER_LOG_NON_FAILING_OUT_OF_BOUNDS.get();
    public static final boolean logOrderChange = _GRID_RANGER_LOG_ORDER_CHANGE.get();
    public static final boolean logOverstretch = _GRID_RANGER_LOG_OVERSTRETCH.get();
    public static final boolean logScaleCheck = _GRID_RANGER_LOG_SCALE_CHECK.get();
    public static final boolean logScaleDebug = _GRID_RANGER_LOG_SCALE_DEBUG.get();
    public static final boolean logSortCheck = _GRID_RANGER_LOG_SORT_CHECK.get();
    public static final boolean logWideRange = _GRID_RANGER_LOG_WIDE_RANGE.get();
    public static final int width = _GRID_RANGER_LOG_WIDTH.get();
}
