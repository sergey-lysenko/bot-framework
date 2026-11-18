package works.lysenko.util.func.type.fractions;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.fraction.FractionConversionException;
import works.lysenko.util.data.strs.Bind;
import works.lysenko.util.func.data.Regexes;
import works.lysenko.util.func.type.Fractions;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.V.VALUE_IS_TO_BIG___;
import static works.lysenko.util.lang.word.C.CONVERT;
import static works.lysenko.util.lang.word.F.FRACTION;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.prop.data.Delimeters.L2;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Symbols.CLS_BRK;
import static works.lysenko.util.spec.Symbols._SLASH_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * Factory class for creating Fraction objects from various data types.
 * Provides static methods to convert numbers and strings into Fraction objects
 * with different levels of detail and error handling.
 */
public record Factory() {

    /**
     * Returns a Fraction object created from a Double value.
     *
     * @param d the Double value to create the Fraction from
     * @return the fraction represented by the Double value
     */
    public static Fraction fr(final Double d) {

        return fr(d, true);
    }

    /**
     * Returns a Fraction object created from a Float value.
     *
     * @param f the Float value to create the Fraction from
     * @return the fraction represented by the Float value
     */
    public static Fraction fr(final float f) {

        return fr((double) f, true);
    }

    /**
     * Converts a Double value to a Fraction object.
     *
     * @param d      the Double value to create the Fraction from
     * @param silent whether to suppress logging if the value is converted to a capped Fraction
     * @return the Fraction object represented by the Double value
     */
    @SuppressWarnings("StandardVariableNames")
    public static Fraction fr(final Double d, final boolean silent) {

        final Fraction f;
        try {
            f = new Fraction(d, Fractions.epsilon(), 100);
        } catch (final FractionConversionException e) {
            logEvent(S1, b(VALUE_IS_TO_BIG___, e.toString()));
            throw e;
        }
        if (!silent) logDebug(Bind.b(s(d), s(IS), Render.ts(f)));
        return f;
    }

    /**
     * Creates a Fraction object from a string representation of a numeric value
     * without controlling the format validity. The string can represent either a
     * fraction or a double value.
     *
     * @param s the string representation of a numeric value, which can be a fraction or a double
     * @return a Fraction object created from the string
     */
    public static Fraction fr(final String s) {

        return fr(s, false);
    }

    /**
     * Creates a Fraction object from a string representation of a numeric value.
     * The string can represent either a fraction or a double value.
     * If the string is improperly formatted according to specific rules,
     * the method can return null or throw an exception based on the nullIfWrongFormat parameter.
     *
     * @param s                 the string representation of a numeric value, which can be a fraction or a double
     * @param nullIfWrongFormat if true, the method returns null for incorrectly formatted strings;
     *                          otherwise, it throws an IllegalArgumentException
     * @return a Fraction object created from the string, or null if the string is null or improperly formatted
     * and nullIfWrongFormat is true
     * @throws IllegalArgumentException if nullIfWrongFormat is false and the string is improperly formatted
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static Fraction fr(final String s, final boolean nullIfWrongFormat) {

        if (isNull(s)) return null;
        if (s.contains(s(L0)) || s.contains(s(L2)) || s.contains(s(_SPACE_)))
            if (nullIfWrongFormat) return null;
            else throw new IllegalArgumentException(b(UNABLE_TO, CONVERT, q(s), TO, FRACTION));
        final String[] parts = s.split(Regexes.opn_brk);
        final String frS = parts[0];
        final String dbS = parts.length > ONE ? parts[ONE].replace(s(CLS_BRK), EMPTY) : null;
        final String source = Fractions.deUnicode(frS);
        if (source.contains(s(_SLASH_))) return Convertor.fromNumeratorDenominator(s, source, dbS);
        else return Convertor.fromDouble(s);
    }

    /**
     * Creates a Fraction object with the given numerator and denominator.
     *
     * @param num the numerator of the fraction
     * @param den the denominator of the fraction
     * @return a new Fraction object with the specified numerator and denominator
     */
    public static Fraction fr(final int num, final int den) {

        return new Fraction(num, den);
    }
}
