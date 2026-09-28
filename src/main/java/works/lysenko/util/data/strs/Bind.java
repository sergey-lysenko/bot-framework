package works.lysenko.util.data.strs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.join;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.core.Assertions.еггог;
import static works.lysenko.util.lang.U.UNEXPECTED_NULL_VALUE_AMONG_;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * The Binder interface provides utility methods for binding strings with characters.
 * It includes methods for binding strings with space, long values with a character, int values with a character,
 * and strings with a character. It also includes methods for filtering out null or empty strings from an array
 * and joining the remaining strings with a binder character. Finally, it provides methods for binding elements
 * with space, a character, and the DASH symbol.
 */
@SuppressWarnings({"InterfaceNeverImplemented", "ClassIndependentOfModule", "StandardVariableNames"})
public interface Bind {

    /**
     * Bind strings with SPACE character
     *
     * @param s strings to bind
     * @return result string
     */
    static String b(final String... s) {

        return b(_SPACE_, s);
    }

    /**
     * Binds the given values to a single string using the specified binder character.
     *
     * @param <T>    the type of the elements to bind
     * @param binder the character to use as a delimiter between the values
     * @param values the values to bind into a single string
     * @return a string that consists of the string representations of the supplied values, separated by the binder character
     */
    @SafeVarargs
    static <T> String b(final char binder, final T... values) {

        final Collection<String> list = new ArrayList<>(1);
        for (final T v : values) {
            list.add(s(v));
        }
        return join(list.toArray(), binder);
    }

    /**
     * Filters out null or empty strings (conditionally) from the given array and joins the remaining strings with a space
     * character.
     *
     * @param includeEmpty whether to include empty strings in the result
     * @param s            the array of strings to filter and join
     * @return a string containing the non-null and non-empty strings from the input array joined with a space character
     */
    static String b(final boolean includeEmpty, final String... s) {

        return b(includeEmpty, _SPACE_, s);
    }

    /**
     * Filters out null or empty strings from the given array and joins the remaining strings with a binder character.
     *
     * @param includeEmpty whether to include empty strings in the result
     * @param binder       the character to use as a binder between the filtered strings
     * @param s            the array of strings to filter and join
     * @return a string containing the non-null and non-empty strings from the input array joined with the binder character
     */
    static String b(final boolean includeEmpty, final char binder, final String... s) {

        final Collection<String> list = new ArrayList<>(1);
        for (final String str : s) {
            if (isNull(str))
                еггог(s(UNEXPECTED_NULL_VALUE_AMONG_, Arrays.toString(s), IN,
                        Arrays.toString(Thread.currentThread().getStackTrace())));
            else if (includeEmpty || !str.isEmpty()) list.add(str);
        }
        return join(list.toArray(), binder);
    }

    /**
     * Bind elements with space
     * Nulls allowed, replaced by "null" String
     *
     * @param s strings to bind
     * @return result string
     */
    static String bn(final String... s) {

        return bn(_SPACE_, s);
    }

    /**
     * Bind elements with a character
     * Nulls allowed, replaced by "null" String
     *
     * @param binder character to use
     * @param s      strings to bind
     * @return result string
     */
    @SuppressWarnings("SameParameterValue")
    private static String bn(final char binder, final String... s) {

        final Collection<String> list = new ArrayList<>(1);
        for (final String str : s) {
            list.add(n(NULL, str)); // possible to use another null substitute
        }
        return join(list.toArray(), binder);
    }

    /**
     * Bind elements with DASH symbol
     *
     * @param s strings to bind
     * @return result string
     */
    static String d(final String... s) {

        return b(_DASH_, s);
    }
}
