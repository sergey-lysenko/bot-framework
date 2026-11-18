package works.lysenko.util.func.type.fractions;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.func.type.Fractions;

import static works.lysenko.Base.logDebug;
import static works.lysenko.util.chrs.__.FR;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Swap.inn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._SLASH_;

/**
 * Utility class for converting various string representations into Fraction objects and normal
 * numeric strings. This class provides methods for converting strings that represent
 * fractional values, as well as strings containing subscript and superscript numerals
 * into their standard numerical character equivalents.
 */
@SuppressWarnings({"WeakerAccess", "StaticMethodOnlyUsedInOneClass"})
public record Convertor() {

    /**
     * Converts a string representation of a double to a Fraction object.
     *
     * @param s the string representation of a double value to convert
     * @return a Fraction object representing the value of the parsed double, or null if parsing fails
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static Fraction fromDouble(final String s) {

        try {
            return new Fraction(Double.parseDouble(s));
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Creates a Fraction object from a given numerator and denominator that are parsed from a source string.
     * The method splits the source string using a given delimiter, parses the resulting parts into integers,
     * and constructs a Fraction object.
     * If the denominator is zero or if the numeric conversion fails, the method returns null.
     *
     * @param s      the delimiter used to split the source string into numerator and denominator parts
     * @param source the string containing the fractional representation to be split and parsed
     * @param dbS    an optional string for verification purposes, can be null
     * @return a Fraction object representing the numerator and denominator from the source string;
     * returns null if the numeric conversion fails or if verification fails
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static Fraction fromNumeratorDenominator(final String s, final String source, final String dbS) {

        final String[] array = source.split(s(_SLASH_));
        final int numerator;
        final int denominator;
        try {
            numerator = inn(array[ZERO]);
            denominator = inn(array[ONE]);
        } catch (final NumberFormatException e) {
            return null;
        }
        final Fraction fr = new Fraction(numerator, denominator);
        if (isNotNull(dbS)) {
            if (Fractions.verify(dbS, fr)) return null;
        }
        if (s.contains(s(L0))) logDebug(a(kv(FR, fr)));
        return fr;
    }

    /**
     * Converts a string containing subscript characters into a string with normal numerical characters.
     * This method replaces subscripted numeric characters (e.g., '₀', '₁', '₂') in the given input string
     * with their corresponding standard numeric character equivalents ('0', '1', '2', etc.).
     *
     * @param s the input string that possibly contains subscript characters
     * @return a string with subscript characters replaced by standard numeric characters
     */
    public static String fromSubscript(final String s) {

        return Fractions.replaceDigitsWithMappedChars(s, Fractions.subscripts, false);
    }

    /**
     * Converts a string that contains superscript characters into a string with normal numerical characters.
     * This method replaces superscripted numeric characters (e.g., '⁰', '¹', '²') in the given input string
     * with their corresponding standard numeric character equivalents ('0', '1', '2', etc.).
     *
     * @param s the input string that possibly contains superscript characters
     * @return a string with superscript characters replaced by standard numeric characters
     */
    public static String fromSuperscript(final String s) {

        return Fractions.replaceDigitsWithMappedChars(s, Fractions.superscripts, false);
    }
}
