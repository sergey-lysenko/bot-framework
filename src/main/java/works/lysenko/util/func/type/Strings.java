package works.lysenko.util.func.type;

import works.lysenko.util.data.enums.Brackets;

import java.util.Collection;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._DOLLAR;

/**
 * The Strings class provides utility methods for manipulating strings.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Strings() {


    /**
     * Represents a constant string with a value of {@code null}.
     * It can be used as a placeholder or marker where a non-value string may be needed
     * in the context of string manipulations or comparisons.
     */
    public final static String NULL = null;

    /**
     * Retrieves the first element from the given collection of strings.
     *
     * @param values The collection of strings from which the first element will be retrieved.
     * @return The first element in the collection.
     * @throws java.util.NoSuchElementException If the collection is empty.
     */
    public static String firstOf(final Collection<String> values) {
        return values.stream().findFirst().orElseThrow();
    }

    /**
     * Strips excess zeros from the provided string, based on the specified maximum value.
     *
     * @param str The string to strip excess zeros from.
     * @param max The maximum value to consider when stripping excess zeros.
     * @return The string with excess zeros removed based on the specified maximum value.
     */
    public static String stripExcessZeros(final String str, final int max) {

        return str.replaceAll(s(ZERO, e(Brackets.CURLY, s(ONE, _COMMA_, s(max))), _DOLLAR), EMPTY);
    }
}
