package works.lysenko.util.data.strs;

import works.lysenko.util.data.enums.Brackets;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Symbols.DBL_QUO;
import static works.lysenko.util.spec.Symbols._QUOTE_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * The Envelope class provides methods to enclose text or numbers with different types of characters and brackets.
 */
@SuppressWarnings({"InterfaceNeverImplemented", "OverloadedMethodsWithSameNumberOfParameters"})
public interface Wrap {

    /**
     * Encloses the provided string with a leading and trailing space.
     *
     * @param s the string to be enclosed
     * @return the string prefixed and suffixed with a space
     */
    static String e(final String s) {

        return e(true, s, true);
    }

    /**
     * Encloses the provided string with optional leading and trailing spaces.
     *
     * @param leadingSpace whether to add a leading space
     * @param s            the string to be enclosed
     * @param trailingSpace whether to add a trailing space
     * @return the string optionally prefixed and/or suffixed with a space
     */
    static String e(final boolean leadingSpace, final String s, final boolean trailingSpace) {

        return s(leadingSpace ? _SPACE_ : EMPTY, s, trailingSpace ? _SPACE_ : EMPTY);
    }

    /**
     * Encloses the string representation of a double value within the specified type of brackets.
     *
     * @param brackets The type of brackets to use for enclosing the double value.
     * @param d        The double value to be enclosed within the brackets.
     * @return A string representation of the double value enclosed by the opening and closing characters of the specified brackets.
     */
    static String e(final Brackets brackets, final double d) {

        return s(brackets.opening(), s(d), brackets.closing());
    }

    /**
     * Encloses the string representation of a long value within the specified type of brackets.
     *
     * @param brackets The type of brackets to use for enclosing the long value.
     * @param l        The long value to be enclosed within the brackets.
     * @return A string with the long value enclosed by the opening and closing characters of the specified brackets.
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    static String e(final Brackets brackets, final long l) {

        return s(brackets.opening(), s(l), brackets.closing());
    }

    /**
     * Encloses the string representation of an integer with the specified type of brackets.
     *
     * @param brackets The type of brackets to use for enclosing the integer.
     * @param i        The integer to be enclosed within the brackets.
     * @return A string with the integer enclosed by the opening and closing characters of the specified brackets.
     */
    static String e(final Brackets brackets, final int i) {

        return s(brackets.opening(), s(i), brackets.closing());
    }

    /**
     * Encloses a character within the specified type of brackets.
     *
     * @param brackets The type of brackets to use for enclosing the character.
     * @param c        The character to be enclosed within the brackets.
     * @return A string with the character enclosed in the opening and closing characters of the specified brackets.
     */
    static String e(final Brackets brackets, final char c) {

        return s(brackets.opening(), s(c), brackets.closing());
    }

    /**
     * Encloses the provided string within the specified type of brackets.
     *
     * @param brackets The type of brackets to use for enclosing the string.
     * @param s        The string to be enclosed.
     * @return The string enclosed within the specified brackets.
     */
    static String e(final Brackets brackets, final String s) {

        return s(brackets.opening(), s, brackets.closing());
    }

    /**
     * Encloses the specified character with the given fence character as a prefix and suffix.
     *
     * @param fence the character used to surround the specified character
     * @param c     the character to be enclosed
     * @return a string where the specified character is surrounded by the fence character
     */
    static String e(final char fence, final char c) {

        return s(fence, s(c), fence);
    }

    /**
     * Encloses a string with the specified character as a prefix and suffix.
     *
     * @param fence the character to surround the string with
     * @param s     the string to be enclosed
     * @return the string surrounded by the specified character
     */
    static String e(final char fence, final String s) {

        return s(fence, s, fence);
    }

    /**
     * Encloses a string with an optional leading space.
     *
     * @param leadingSpace whether to add a leading space
     * @param s            the string to be enclosed
     * @return the string optionally prefixed with a space
     */
    static String e(final boolean leadingSpace, final String s) {

        return e(leadingSpace, s, false);
    }

    /**
     * Envelope a text with optional trailing space.
     *
     * @param s             string to enclose
     * @param trailingSpace add trailing space?
     * @return string encircled by optional trailing space
     */
    static String e(final String s, final boolean trailingSpace) {

        return e(false, s, trailingSpace);
    }

    /**
     * Encloses the string representation of the given integer with single quotes.
     *
     * @param i the integer whose string representation will be enclosed in quotes
     * @return the string representation of the integer enclosed in single quotes
     */
    static String q(final int i) {

        return e(_QUOTE_, s(i));
    }

    /**
     * Encloses a given string with either single quotes or double quotes depending
     * on the contents of the string. If the string contains a single quote, it will
     * be enclosed with double quotes. Otherwise, it will be enclosed with single quotes.
     *
     * @param s the string to be enclosed
     * @return the string enclosed with quotes or double quotes
     */
    static String q(final String s) {

        final char q = (isNotNull(s)) && s.contains(s(_QUOTE_)) ? DBL_QUO : _QUOTE_;
        return e(q, s);
    }

    /**
     * Encloses the string representation of an object with quotes or double quotes
     * depending on its contents. If the object's string representation contains a
     * double quote character, it will be enclosed with single quotes. Otherwise, it
     * will be enclosed with double quotes. Assumes the object is not null.
     *
     * @param o Object whose string representation is to be enclosed.
     * @return The string representation of the object enclosed with quotes or double quotes.
     */
    static String q(final Object o) {

        final char q = (isNotNull(o)) && o.toString().contains(s(_QUOTE_)) ? DBL_QUO : _QUOTE_;
        return e(q, o.toString());
    }

    /**
     * Encloses a string with either single quotes or double quotes depending on its contents
     * and a specified condition. If the `inCase` parameter is true, the method adds enclosing
     * quotes to the string. The type of quotes chosen depends on whether the string contains
     * single quotes. If the string contains a single quote, double quotes are used; otherwise,
     * single quotes are used. If `inCase` is false, the string is returned without additional
     * enclosing quotes.
     *
     * @param inCase a boolean indicating whether to add enclosing quotes to the string
     * @param s the string to be enclosed, if applicable
     * @return the optionally enclosed string based on the input parameters
     */
    static String q(final boolean inCase, final String s) {

        final char q = (isNotNull(s)) && s.contains(s(_QUOTE_)) ? DBL_QUO : _QUOTE_;
        return inCase ? sn(q, s, q) : sn(s);
    }
}
