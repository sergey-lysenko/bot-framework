package works.lysenko.util.func.type;

import java.util.Arrays;

import static java.util.Objects.isNull;

/**
 * The Objects class provides static methods for performing operations on objects.
 */
@SuppressWarnings("ClassIndependentOfModule")
public record Objects() {

    /**
     * Checks if any of the given objects is null.
     *
     * @param objects The objects to check for null.
     * @return true if any of the objects is null, false otherwise.
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static boolean isAnyNull(final Object... objects) {

        return Arrays.stream(objects).anyMatch(java.util.Objects::isNull);
    }

    /**
     * Checks if an object is not null.
     *
     * @param object The object to check.
     * @return true if the object is not null, false otherwise.
     */
    public static boolean isNotNull(final Object object) {

        return !isNull(object);
    }


    /**
     * Checks if the given Boolean object is not null and is true.
     *
     * @param b The Boolean object to check.
     * @return true if the object is not null and is true, false otherwise.
     */
    public static boolean isNotNullAndTrue(final Boolean b) {

        return !isNull(b) && b;
    }
}
