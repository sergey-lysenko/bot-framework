package works.lysenko.util.prop.data;

import static java.util.Objects.isNull;
import static works.lysenko.util.prop.PropEnum._MATH_FRACTIONS_ACCURACY;
import static works.lysenko.util.prop.PropEnum._MATH_FRACTIONS_OUTPUT;

/**
 * Configuration and caching for fraction mathematical operations and output formatting.
 */
public record Fractions() {

    private static Integer accuracy = null;
    private static Boolean output = null;

    /**
     * Returns the accuracy level for fraction operations.
     * <p>
     * Lazily retrieves the accuracy setting from test properties, caching the result.
     *
     * @return the accuracy level as an integer
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static int accuracy() {

        if (isNull(accuracy)) accuracy = _MATH_FRACTIONS_ACCURACY.get();
        return accuracy;
    }

    /**
     * Determines if fractions should be output based on application properties.
     * <p>
     * Lazily retrieves the output setting from test properties, caching the result.
     *
     * @return true if fractions output is enabled, false otherwise
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public static boolean output() {

        if (isNull(output)) output = _MATH_FRACTIONS_OUTPUT.get();
        return output;
    }
}
