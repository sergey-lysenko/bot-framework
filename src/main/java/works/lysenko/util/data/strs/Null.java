package works.lysenko.util.data.strs;

import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * The Nulls interface provides several static methods for handling null values and returning default values in Java.
 * It includes methods for checking if a value is null and returning a default value if it is.
 */
@SuppressWarnings({"InterfaceNeverImplemented", "ClassIndependentOfModule"})
public interface Null {

    /**
     * Represents a method to return a value or a provided default value if the value is null.
     *
     * @param <N>    the type of the input values
     * @param ifNull the default value to be returned if the value is null
     * @param value  the value to be checked for being null
     * @return the value if not null, otherwise the provided default value
     */
    static <N> N n(final N ifNull, final N value) {

        return (null == value) ? ifNull : value;
    }

    /**
     * Returns the string representation of the provided value, or the string representation of the default value if the
     * value is null.
     *
     * @param <N>   the type of the input values
     * @param value the value to be checked for being null
     * @return the string representation of the value if not null, otherwise the string representation of the provided
     * default value
     */
    static <N> String sn(final N value) {

        return (null == value) ? NULL : s(value);
    }

    /**
     * Returns the string representation of the provided value, or the string representation of the default value if the
     * value is null.
     *
     * @param <N>    the type of the input values
     * @param ifNull the default value to be returned if the value is null
     * @param value  the value to be checked for being null
     * @return the string representation of the value if not null, otherwise the string representation of the provided
     * default value
     */
    static <N> String sn(final N ifNull, final N value) {

        return (null == value) ? s(ifNull) : s(value);
    }
}