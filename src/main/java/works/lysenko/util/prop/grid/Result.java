package works.lysenko.util.prop.grid;

import works.lysenko.util.data.enums.Severity;

import static works.lysenko.util.prop.PropEnum._GRID_EMPTY_RESULT_SEVERITY;
import static works.lysenko.util.prop.PropEnum._GRID_NULL_RESULT_SEVERITY;

/**
 * Severity configuration for empty or null results in grid operations.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Result() {

    /**
     * Severity level assigned when a grid operation yields a null result.
     */
    public static final Severity nullSeverity = _GRID_NULL_RESULT_SEVERITY.get();

    /**
     * Severity level assigned when a grid operation yields an empty result.
     */
    public static final Severity emptySeverity = _GRID_EMPTY_RESULT_SEVERITY.get();
}
