package works.lysenko.util.func.type;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.func.type.fractions.Convertor;
import works.lysenko.util.func.type.fractions.Factory;
import works.lysenko.util.func.type.fractions.Render;

import static java.lang.StrictMath.abs;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.D.DIFFERENCE;
import static works.lysenko.util.prop.data.Fractions.accuracy;
import static works.lysenko.util.spec.Numbers.*;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Bruchrechnung class is a subclass of the Fraction class.
 * It provides additional functionality for performing arithmetic operations on fractions.
 */
@SuppressWarnings({"unused", "MethodWithMultipleReturnPoints", "StaticMethodOnlyUsedInOneClass"})
public record Fractions() {

    /**
     * A constant representing the fraction 1/7.
     * This is a pre-defined Fraction object initialized with a numerator of one (1)
     * and a denominator of seven (7), commonly used in precise fractional calculations.
     */
    public static final Fraction ONE_SEVENTH = Factory.fr(ONE, SEVEN);
    /**
     * Represents the constant fraction 1/8.
     * This is a predefined Fraction object created using a numerator of 1 and
     * a denominator of 8, intended for consistent representation and reuse
     * of the fractional value one-eighth throughout the application.
     */
    public static final Fraction ONE_EIGHTH = Factory.fr(ONE, EIGHT);
    /**
     * Represents the fraction 1/9 as a constant Fraction object.
     * This constant is useful for calculations and comparisons involving the specific
     * fractional value of one ninth.
     */
    public static final Fraction ONE_NINTH = Factory.fr(ONE, NINE);

    /**
     * Represents the fraction value of one-tenth.
     * Initialised as a Fraction object with the value 1 as the numerator and 10 as the denominator.
     */
    public static final Fraction ONE_TENTH = Factory.fr(ONE, TEN);

    /**
     * Represents one hundredth as a fraction, which is calculated by multiplying one tenth by itself.
     */
    public static final Fraction ONE_HUNDREDTH = ONE_TENTH.multiply(ONE_TENTH);
    /**
     * Represents a string consisting of subscript characters from 0 to 9.
     * This string is used for converting numeric digits to their corresponding subscript form.
     */
    public static final String subscripts = "₀₁₂₃₄₅₆₇₈₉";
    /**
     * A1 string containing Unicode superscript characters
     * representing digits 0 through 9.
     */
    public static final String superscripts = "⁰¹²³⁴⁵⁶⁷⁸⁹";
    /*
     * As soon as there are two fractions involved, maximal drift is two epsilons
     */
    private static final double ALLOWED_DRIFT = 2.0;

    /**
     * Calculates the deviation between the expected and actual fractions.
     *
     * @param expected the expected fraction
     * @param actual   the actual fraction
     * @return the deviation between the expected and actual fractions
     */
    public static Fraction dV(final Fraction expected, final Fraction actual) {

        return actual.subtract(expected);
    }

    /**
     * Processes a given string by replacing certain characters and converting
     * Unicode subscript and superscript characters to standard numerical characters.
     *
     * @param s the input string that may contain fractions with subscript or
     *          superscript numerals
     * @return a new string with fractions represented using standard numerical
     * characters instead of Unicode subscripts or superscripts
     */
    public static String deUnicode(final String s) {

        return Convertor.fromSubscript(Convertor.fromSuperscript(s.replace(FRC_SLS, _SLASH_)));
    }

    /**
     * Calculates and returns a small constant value, epsilon, that is used for
     * precise fractional numerical operations. The epsilon value is derived
     * based on an adjustable accuracy level.
     *
     * @return a double representing the small epsilon value used for precision
     * calculations in fractional numerical operations.
     */
    public static double epsilon() {

        return StrictMath.pow(10, -accuracy());
    }

    /**
     * Calculate the precision of fractional numerical values based on the EPSILON v.
     *
     * @return the precision of the fractional numerical values as an integer
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    public static int precision() {

        final long e = (long) (1 / epsilon());
        return s(e).length() - 1;
    }

    /**
     * Replaces characters in a given string based on a mapping of digits or specific characters.
     *
     * @param s           the input string where digits or specified characters will be replaced with mapped characters
     * @param mappedChars the string containing mapped characters to be used for replacement, corresponding to each digit or
     *                   specified character
     * @param fromDigits  a boolean flag indicating whether to replace digits with mapped characters (if true) or to replace
     *                   characters with digits (if false), based on the specified
     *                    characters in mappedChars
     * @return a new string with specified replacements applied, replacing either digits with mapped characters or specified
     * characters with digits
     */
    @SuppressWarnings({"OverlyComplexBooleanExpression", "CharUsedInArithmeticContext", "NumericCastThatLosesPrecision"})
    public static String replaceDigitsWithMappedChars(final String s, final String mappedChars, final boolean fromDigits) {

        final StringBuilder sb = new StringBuilder(s.length());
        for (final char c : s.toCharArray())
            if ((fromDigits && Character.isDigit(c)) || (!fromDigits && -1 != mappedChars.indexOf(c)))
                sb.append(fromDigits ? mappedChars.charAt(c - _0_) : (char) (_0_ + mappedChars.indexOf(c)));
            else sb.append(c);
        return sb.toString();
    }

    /**
     * Converts a given string to its subscript representation if the apply flag is true.
     *
     * @param s     the input string to be converted
     * @param apply a boolean flag indicating whether to apply subscript conversion
     * @return the subscript representation of the input string if apply is true, otherwise returns the input string unchanged
     */
    public static String toSubscript(final String s, final boolean apply) {

        if (apply) return replaceDigitsWithMappedChars(s, subscripts, true);
        else return s;
    }

    /**
     * Converts a given string to its superscript representation if the apply flag is true.
     *
     * @param s     the input string to be converted
     * @param apply a boolean flag indicating whether to apply superscript conversion
     * @return the superscript representation of the input string if apply is true, otherwise returns the input string
     * unchanged
     */
    public static String toSuperscript(final String s, final boolean apply) {

        if (apply) return replaceDigitsWithMappedChars(s, superscripts, true);
        else return s;
    }

    /**
     * Verifies the precision of the given fraction against a double value represented
     * as a string. It checks whether the difference between the fractions is within
     * an allowed drift range.
     *
     * @param dbS the string representing the double value, which may contain commas
     *            as decimal separators
     * @param fr  the fraction to verify against the parsed double value
     * @return true if the string does not represent a valid double value; false if the
     * difference between the fractions exceeds the allowed drift
     * @throws IllegalArgumentException if the difference between the parsed fraction
     *                                  and the given fraction is greater than the
     *                                  allowed drift
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public static boolean verify(final String dbS, final Fraction fr) {

        final double db;
        try {
            db = Double.parseDouble(dbS.replace(_COMMA_, _DOT_));
            final Fraction co = Factory.fr(db);
            final double diff = abs(fr.doubleValue() - co.doubleValue());
            final double mult = diff / epsilon();
            if (ALLOWED_DRIFT < mult) {
                throw new IllegalArgumentException(b(s(db), s(_EXCL_, _EQUAL_), s(Render.ts(false, fr), _COMMA_), DIFFERENCE
                        , IS, s(diff)));
            }
        } catch (final NumberFormatException e) {
            return true;
        }
        return false;
    }
}
