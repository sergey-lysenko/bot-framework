package works.lysenko.util.data.strs;

import org.apache.commons.math3.fraction.Fraction;
import org.openqa.selenium.Keys;
import works.lysenko.util.apis.grid.t._Value;
import works.lysenko.util.data.enums.Marker;
import works.lysenko.util.data.range.FractionRange;
import works.lysenko.util.func.logs.poster.Value;

import java.util.Arrays;
import java.util.Collection;

import static java.lang.Math.round;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.core.Assertions.еггог;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.N.NULL_IS_NOT_ACCEPTABLE;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.U.UNEXPECTED_NULL_VALUE_AMONG_;
import static works.lysenko.util.lang.word.C.CONVERT;
import static works.lysenko.util.lang.word.I.INTEGER;
import static works.lysenko.util.spec.Symbols.*;

/**
 * The Transform interface provides utility methods for string transformations.
 */
@SuppressWarnings({"InterfaceNeverImplemented", "CallToSuspiciousStringMethod"})
public interface Swap {

    /**
     * @param d        The double value to be formatted.
     * @param decimals The number of decimal places to round the double value to.
     * @return The concatenated string representation of the formatted double value.
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    static String d(final double d, final int decimals) {

        final String format = s(_PRCNT_, _DOT_, decimals, F);
        return String.format(format, d);
    }

    /**
     * Shortcut for {@link String#format(String, Object...)}
     *
     * @param template template
     * @param value    value
     * @return filled in string template
     */
    static String f(final String template, final Object... value) {

        return String.format(template, value);
    }

    /**
     * Calculates the integer value of a given double after rounding.
     *
     * @param d The double value to convert to an integer.
     * @return The integer value calculated by rounding the double value.
     */
    static Integer i(final Double d) {

        int result = 0;
        try {
            result = Math.toIntExact(round(d));
        } catch (final ArithmeticException e) {
            fail(b(UNABLE_TO, CONVERT, q(s(d)), TO, INTEGER));
        }
        return result;
    }

    /**
     * Calculates the integer value of a given double after rounding.
     *
     * @param f The double value to convert to an integer.
     * @return The integer value calculated by rounding the double value.
     */
    static Integer i(final Float f) {

        int result = 0;
        try {
            result = Math.toIntExact(round(f));
        } catch (final ArithmeticException e) {
            fail(b(UNABLE_TO, CONVERT, q(s(f)), TO, INTEGER));
        }
        return result;
    }

    /**
     * Get Integer value from String
     *
     * @param s string to parse
     * @return result string
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    static Integer i(final String s) {

        if (isNull(s)) return null;
        return Integer.parseInt(s.trim());
    }

    /**
     * Parses the given string into an integer, trimming any leading or trailing whitespace.
     * Throws an IllegalArgumentException if the string is null.
     *
     * @param s the string to be parsed
     * @return the integer value represented by the string after trimming
     */
    static int inn(final String s) {

        if (isNull(s)) throw new IllegalArgumentException(NULL_IS_NOT_ACCEPTABLE);
        return Integer.parseInt(s.trim());
    }


    /**
     * Envelope a text with quotes or double quotes if value is not null
     *
     * @param s               string to envelope
     * @param nullReplacement optional replacement of null value (non-quoted)
     * @return string enclosed in quotes or double quotes depending on the contents of the string
     */
    static String qinn(final String s, final String nullReplacement) {

        return q(isNotNull(s), n(n(EMPTY, nullReplacement), s));
    }

    /**
     * Repeat defined key given amount times
     *
     * @param keys        key to repeat
     * @param repetitions times to repeat
     * @return requested sequence
     */
    static String r(final Keys keys, final int repetitions) {

        String sequence = EMPTY;
        for (int i = 0; i < repetitions; i++)
            sequence = s(sequence, keys);
        return sequence;
    }

    /**
     * Fast concatenation of objects text representations into single string
     * Can also be used as a short alias for String.valueOf()
     * Null values not allowed, Assertion will fail
     *
     * @param s strings to join
     * @return concatenated string
     */
    @SuppressWarnings({"StandardVariableNames", "OverloadedVarargsMethod"})
    static String s(final Object... s) {

        final StringBuilder sb = new StringBuilder(0);
        for (final Object o : s) {
            if (isNull(o)) еггог(sn(UNEXPECTED_NULL_VALUE_AMONG_, Arrays.toString(s), IN,
                    Arrays.toString(Thread.currentThread().getStackTrace())));
            sb.append(o);
        }
        return sb.toString();
    }

    /**
     * Converts the given collection of strings into a string representation of the array.
     *
     * @param result A collection of strings to be converted into an array and then to a string.
     * @return A string representation of the array containing the elements of the given collection.
     */
    static String s(final Collection<String> result) {

        return Arrays.toString(result.toArray());
    }

    /**
     * @param amount amount
     * @return 's' if amount is not 1
     */
    static String s1(final int amount) {

        return s((1 == amount) ? EMPTY : S);
    }

    /**
     * @param amount amount
     * @param noun   to use
     * @return string
     */
    static String s1(final long amount, final String noun) {

        return b(s(amount), s(noun, (1L == amount) ? EMPTY : S));
    }

    /**
     * Concatenates the first and second boolean values with the first and second descriptors after converting them
     * into strings.
     *
     * @param first  a first boolean value indicating if the first marker should be used
     * @param second a second boolean value indicating if the second marker should be used
     * @param marker the Marker object containing the first and second descriptors
     * @return a concatenated string of the first and second markers with the first and second strings
     * @throws NullPointerException if the marker parameter is null
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    static String s2(final Marker marker, final boolean first, final boolean second) {

        return b(marker.getFirst(), q(s(first)), marker.getSecond(), q(s(second)));
    }

    /**
     * Fast concatenation of object text representations into single string
     * Can also be used as a short alias for String.valueOf()
     * Null values allowed, replaced by "null" String
     *
     * @param s strings to join
     * @return concatenated string
     */
    @SuppressWarnings("StandardVariableNames")
    static String sn(final Object... s) {

        final StringBuilder sb = new StringBuilder(0);
        for (final Object o : s) {
            if (isNull(o)) sb.append(NULL);
            else sb.append(o);
        }
        return sb.toString();
    }

    /**
     * Supply visual representation for non-printable character
     *
     * @param keys key to visualise
     * @return visual representation for non-printable character
     */
    static String v(final Keys keys) {

        return switch (keys) {
            case ARROW_DOWN -> s(DWN_ARR);
            case ARROW_UP -> s(_UP_ARR);
            default -> keys.toString();
        };
    }

    /**
     * Returns a new _Value object with the specified Fraction value.
     *
     * @param value The Fraction value to be wrapped in the _Value object.
     * @return A1 new _Value object containing the specified Fraction value.
     */
    static _Value<?> v(final Fraction value) {

        return new Value<>(value);
    }

    /**
     * Returns a new _Value object with the specified FractionRange value.
     *
     * @param value The FractionRange value to be wrapped in the _Value object.
     * @return A1 new _Value object containing the specified FractionRange value.
     */
    static _Value<?> v(final FractionRange value) {

        return new Value<>(value);
    }

}
