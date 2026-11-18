package works.lysenko.util.spec;

import works.lysenko.util.data.enums.Brackets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static works.lysenko.util.spec.Symbols.DBL_QUO;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._QUOTE_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * Self-contained string synthesis primitives for the lexicon layer, free of runtime engine dependencies.
 */
@SuppressWarnings({"ClassWithoutLogger", "OverloadedVarargsMethod", "OverloadedMethodsWithSameNumberOfParameters",
        "StandardVariableNames", "UseOfSystemOutOrSystemErr", "unused"})
public record Synth() {

    private static final String EMPTY = "";
    private static final String NULL_LITERAL = "null";

    /**
     * Concatenates object representations into a single string.
     *
     * @param parts parts to concatenate
     * @return concatenated string
     */
    public static String s(final Object... parts) {

        final StringBuilder sb = new StringBuilder(32);
        for (final Object o : parts) {
            if (null == o) {
                System.err.println("Unexpected null value among " + Arrays.toString(parts) + " in "
                        + Arrays.toString(Thread.currentThread().getStackTrace()));
            }
            sb.append(o);
        }
        return sb.toString();
    }

    /**
     * Concatenates object representations into a single string, replacing null with "null".
     *
     * @param parts parts to concatenate
     * @return concatenated string
     */
    public static String sn(final Object... parts) {

        final StringBuilder sb = new StringBuilder(32);
        for (final Object o : parts) {
            sb.append(null == o ? NULL_LITERAL : o);
        }
        return sb.toString();
    }

    /**
     * Joins strings with a space delimiter.
     *
     * @param parts strings to join
     * @return space-joined string
     */
    public static String b(final String... parts) {

        return b(_SPACE_, parts);
    }

    /**
     * Joins values with a custom character delimiter.
     *
     * @param binder delimiter character
     * @param values values to join
     * @param <T>    element type
     * @return joined string
     */
    @SafeVarargs
    public static <T> String b(final char binder, final T... values) {

        if (null == values || 0 == values.length) return EMPTY;
        final StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < values.length; i++) {
            if (0 < i) sb.append(binder);
            final T v = values[i];
            if (null != v) sb.append(s(v));
        }
        return sb.toString();
    }

    /**
     * Filters empty strings optionally and joins with a space delimiter.
     *
     * @param includeEmpty true to keep empty strings
     * @param parts        strings to join
     * @return joined string
     */
    public static String b(final boolean includeEmpty, final String... parts) {

        return b(includeEmpty, _SPACE_, parts);
    }

    /**
     * Filters empty strings optionally and joins with a custom delimiter.
     *
     * @param includeEmpty true to keep empty strings
     * @param binder       delimiter character
     * @param parts        strings to join
     * @return joined string
     */
    public static String b(final boolean includeEmpty, final char binder, final String... parts) {

        final List<String> list = new ArrayList<>(parts.length);
        for (final String str : parts) {
            if (null == str) {
                System.err.println("Unexpected null value among " + Arrays.toString(parts) + " in "
                        + Arrays.toString(Thread.currentThread().getStackTrace()));
            } else if (includeEmpty || !str.isEmpty()) {
                list.add(str);
            }
        }
        return joinList(list, binder);
    }

    /**
     * Joins strings with a dash delimiter.
     *
     * @param parts strings to join
     * @return dash-joined string
     */
    public static String d(final String... parts) {

        return b(_DASH_, parts);
    }

    /**
     * Capitalizes the first character of the string representation of {@code value}.
     *
     * @param value object to capitalize
     * @return capitalized string
     */
    public static String c(final Object value) {

        final String original = s(value);
        if (original.isEmpty()) return original;
        final char first = original.charAt(0);
        if (Character.isDigit(first)) {
            throw new IllegalArgumentException("Cannot capitalize a string starting with a digit");
        }
        final char title = Character.toTitleCase(first);
        return (first == title) ? original : (title + original.substring(1));
    }

    /**
     * Lowercases the string representation of {@code value}.
     *
     * @param value object to lowercase
     * @return lowercased string
     */
    public static String l(final Object value) {

        return s(value).toLowerCase();
    }

    /**
     * Uppercases the string representation of {@code value}.
     *
     * @param value object to uppercase
     * @return uppercased string
     */
    public static String u(final Object value) {

        return s(value).toUpperCase();
    }

    /**
     * Surrounds a string with leading and trailing spaces.
     *
     * @param str string to enclose
     * @return space-enclosed string
     */
    public static String e(final String str) {

        return e(true, str, true);
    }

    /**
     * Surrounds a string with optional leading and trailing spaces.
     *
     * @param leadingSpace  true to prepend space
     * @param str           string to enclose
     * @param trailingSpace true to append space
     * @return enclosed string
     */
    public static String e(final boolean leadingSpace, final String str, final boolean trailingSpace) {

        return s(leadingSpace ? _SPACE_ : EMPTY, str, trailingSpace ? _SPACE_ : EMPTY);
    }

    /**
     * Encloses a string with optional leading space.
     *
     * @param leadingSpace true to prepend space
     * @param str          string to enclose
     * @return enclosed string
     */
    public static String e(final boolean leadingSpace, final String str) {

        return e(leadingSpace, str, false);
    }

    /**
     * Encloses a string with optional trailing space.
     *
     * @param str           string to enclose
     * @param trailingSpace true to append space
     * @return enclosed string
     */
    public static String e(final String str, final boolean trailingSpace) {

        return e(false, str, trailingSpace);
    }

    /**
     * Encloses a string within brackets.
     *
     * @param brackets bracket pair
     * @param str      string to enclose
     * @return bracketed string
     */
    public static String e(final Brackets brackets, final String str) {

        return s(brackets.opening(), str, brackets.closing());
    }

    /**
     * Encloses a character within brackets.
     *
     * @param brackets bracket pair
     * @param ch       character to enclose
     * @return bracketed string
     */
    public static String e(final Brackets brackets, final char ch) {

        return s(brackets.opening(), s(ch), brackets.closing());
    }

    /**
     * Encloses an int within brackets.
     *
     * @param brackets bracket pair
     * @param i        int to enclose
     * @return bracketed string
     */
    public static String e(final Brackets brackets, final int i) {

        return s(brackets.opening(), s(i), brackets.closing());
    }

    /**
     * Encloses a string with a fence character.
     *
     * @param fence fence character
     * @param str   string to enclose
     * @return fenced string
     */
    public static String e(final char fence, final String str) {

        return s(fence, str, fence);
    }

    /**
     * Encloses a character with a fence character.
     *
     * @param fence fence character
     * @param ch    character to enclose
     * @return fenced string
     */
    public static String e(final char fence, final char ch) {

        return s(fence, s(ch), fence);
    }

    /**
     * Quotes an integer with single quotes.
     *
     * @param i integer to quote
     * @return quoted integer string
     */
    public static String q(final int i) {

        return e(_QUOTE_, s(i));
    }

    /**
     * Quotes a string with single quotes (or double quotes if it contains a single quote).
     *
     * @param str string to quote
     * @return quoted string
     */
    public static String q(final String str) {

        final char quote = (null != str && str.contains(s(_QUOTE_))) ? DBL_QUO : _QUOTE_;
        return e(quote, str);
    }

    /**
     * Quotes an object's string representation.
     *
     * @param obj object to quote
     * @return quoted string
     */
    public static String q(final Object obj) {

        final String text = String.valueOf(obj);
        final char quote = (null != obj && text.contains(s(_QUOTE_))) ? DBL_QUO : _QUOTE_;
        return e(quote, text);
    }

    private static String joinList(final List<String> list, final char binder) {

        if (list.isEmpty()) return EMPTY;
        final StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < list.size(); i++) {
            if (0 < i) sb.append(binder);
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
