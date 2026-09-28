package works.lysenko.util.func.type.fractions;

import org.apache.commons.lang3.Range;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.enums.Brackets;
import works.lysenko.util.data.range.FractionRange;
import works.lysenko.util.func.type.Fractions;
import works.lysenko.util.spec.Symbols;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Doubles.doubleToString;
import static works.lysenko.util.prop.data.Fractions.accuracy;
import static works.lysenko.util.prop.data.Fractions.output;
import static works.lysenko.util.spec.Symbols.FRC_SLS;
import static works.lysenko.util.spec.Symbols._0_;
import static works.lysenko.util.spec.Symbols._SLASH_;

/**
 * The Render class provides methods to convert fractions and ranges of fractions
 * to their string representations. It supports various formatting options
 * including the use of subscript and superscript characters.
 */
public record Render() {

    /**
     * Converts a fraction to a string representation.
     *
     * @param fraction the fraction to convert
     * @return the string representation of the fraction
     */
    public static String toString(final Fraction fraction) {

        return toString(false, fraction, true);
    }

    /**
     * Converts a given fraction to a string representation, optionally using scripts and
     * including its double value representation.
     *
     * @param full     a boolean indicating if the full string representation should include the fraction's double value
     * @param fraction the Fraction object to convert to a string representation
     * @param scripts  a boolean indicating whether to use subscript and superscript characters for the representation
     * @return the string representation of the fraction, possibly including its double value and using scripts
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static String toString(final boolean full, final Fraction fraction, final boolean scripts) {

        String result;
        if (isNull(fraction))
            result = s(Symbols.EMP_SET);
        else {
            if (output()) {
                if (0 == fraction.getNumerator()) return s(_0_);
                if (1 == fraction.getDenominator()) return s(fraction.getNumerator());
                final String numerator = s(fraction.getNumerator());
                final String denominator = s(fraction.getDenominator());
                result = s(Fractions.toSuperscript(numerator, scripts), scripts ? FRC_SLS : _SLASH_,
                        Fractions.toSubscript(denominator, scripts));
                if (full) result = s(result, e(Brackets.ROUND, doubleToString(fraction, accuracy())));
            } else result = s(fraction.doubleValue());
        }
        return result;
    }

    /**
     * Converts a given fraction to a string representation, optionally including its double value.
     *
     * @param full     indicates if the full string representation should include the fraction's double value
     * @param fraction the fraction to convert to a string representation
     * @return the string representation of the fraction, possibly including its double value
     */
    public static String ts(final boolean full, final Fraction fraction) {

        return toString(full, fraction, true);
    }

    /**
     * Converts a fraction to a string representation.
     *
     * @param fraction the fraction to convert
     * @return the string representation of the fraction
     */
    public static String ts(final Fraction fraction) {

        return ts(false, fraction);
    }

    /**
     * Converts a range of fractions to a string representation.
     *
     * @param range the range of fractions to convert
     * @return the string representation of the range of fractions
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String ts(final Range<Fraction> range) {

        return new FractionRange(range).toString();
    }

    /**
     * Converts a fraction to a string representation.
     *
     * @param fraction the fraction to convert
     * @param scripts  whether to use subscript and superscript characters
     * @return the string representation of the fraction
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String ts(final Fraction fraction, final boolean scripts) {

        return toString(false, fraction, scripts);
    }

    /**
     * Converts a given fraction to a string representation, optionally using scripts
     * and including its double value representation.
     *
     * @param full     a boolean indicating if the full string representation should
     *                 include the fraction's double value
     * @param fraction the Fraction object to convert to a string representation
     * @param scripts  a boolean indicating whether to use subscript and
     *                 superscript characters for the representation
     * @return the string representation of the fraction, possibly including its
     * double value and using scripts
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String ts(final boolean full, final Fraction fraction, final boolean scripts) {

        return toString(full, fraction, scripts);
    }
}
