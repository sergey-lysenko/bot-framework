package works.lysenko.util.prop.data;

import static java.util.Objects.isNull;
import static works.lysenko.util.spec.PropEnum._FRACTIONS_ACCURACY;
import static works.lysenko.util.spec.PropEnum._FRACTIONS_OUTPUT;

/**
 * Represents a utility class for working with fractions.
 */
public record Fractions() {

    private static Integer accuracy = null;
    private static Boolean output = null;

    /**
     * Returns the accuracy level for fraction operations. If the properties are not
     * initialized, it defaults to a predefined value. Otherwise, it retrieves the
     * accuracy setting from the properties.
     *
     * @return the accuracy level as an integer. Defaults to a predefined constant if properties are not set.
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static int accuracy() {

        if (isNull(accuracy)) accuracy = _FRACTIONS_ACCURACY.get();
        return accuracy;
    }

    /**
     * Determines if fractions should be output based on the application properties.
     *
     * @return true if the properties are null or if the property associated with
     * the key for fractions output is set to true; false otherwise.
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public static boolean output() {

        if (isNull(output)) output = _FRACTIONS_OUTPUT.get();
        return output;
    }
}
