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

    public static final boolean logAbsentRange = _RANGER_LOG_ABSENT_RANGE.get();
    public static final boolean logBoundaries = _RANGER_LOG_BOUNDARIES.get();
    public static final boolean logNonFailingOutOfBounds = _RANGER_LOG_NON_FAILING_OUT_OF_BOUNDS.get();
    public static final boolean logOrderChange = _RANGER_LOG_ORDER_CHANGE.get();
    public static final boolean logOverstretch = _RANGER_LOG_OVERSTRETCH.get();
    public static final boolean logScaleCheck = _RANGER_LOG_SCALE_CHECK.get();
    public static final boolean logScaleDebug = _RANGER_LOG_SCALE_DEBUG.get();
    public static final boolean logSortCheck = _RANGER_LOG_SORT_CHECK.get();
    public static final boolean logWideRange = _RANGER_LOG_WIDE_RANGE.get();
    public static final int width = _RANGER_LOG_WIDTH.get();
}
