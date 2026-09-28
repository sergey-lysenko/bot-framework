package works.lysenko.util.data.records;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._RelativeOrAbsoluteFraction;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.data.enums.RelativeOrAbsolute;
import works.lysenko.util.spec.Symbols;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.math3.fraction.Fraction.ZERO;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.OR;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.H.HOW_DID_WE_GET_HERE;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.word.A.ABSOLUTE;
import static works.lysenko.util.lang.word.F.FRACTION;
import static works.lysenko.util.lang.word.P.PARSE;
import static works.lysenko.util.lang.word.R.RELATIVE;

/**
 * Represents a rational number and its association type (absolute or relative).
 * Implements the _RelativeOrAbsoluteFraction interface.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public record RelativeOrAbsoluteFraction(Fraction fraction, RelativeOrAbsolute type) implements _RelativeOrAbsoluteFraction {


    private static final RelativeOrAbsolute R = RelativeOrAbsolute.RELATIVE;
    private static final RelativeOrAbsolute A = RelativeOrAbsolute.ABSOLUTE;
    private static final String NAME = s(c(RELATIVE), c(OR), c(ABSOLUTE), c(FRACTION));

    /**
     * Parses the given string to create a `RelativeOrAbsoluteFraction` object.
     * The first character of the string specifies whether the fraction is relative or absolute.
     *
     * @param origin the origin string used for error messaging when parsing fails
     * @param source the string representation of the `RelativeOrAbsoluteFraction` where the first character
     *               indicates the type ('a' for absolute, 'r' for relative)
     * @return a `RelativeOrAbsoluteFraction` object parsed from the string, or null if the string cannot be parsed
     */
    @SuppressWarnings("HardCodedStringLiteral") // false positive
    public static RelativeOrAbsoluteFraction raf(final String origin, final String source) {

        final char marker = source.charAt(0);
        final String raw = source.substring(1);
        return switch (marker) {
            case '0' -> new RelativeOrAbsoluteFraction(ZERO, A);
            case 'a' -> new RelativeOrAbsoluteFraction(fr(raw), A);
            case 'r' -> new RelativeOrAbsoluteFraction(fr(raw), R);
            default -> {
                final String in = isNull(origin) ? EMPTY : b(IN, q(origin));
                fail(b(UNABLE_TO, PARSE, NAME, FROM, q(source), in));
                yield null;
            }
        };
    }

    /**
     * Parses the given string to create a `RelativeOrAbsoluteFraction` object. The first character of the string
     * indicates the type of `RelativeOrAbsoluteFraction` to create ('a' for absolute, 'r' for relative).
     *
     * @param source the string representation of the `RelativeOrAbsoluteFraction`, where the first character
     *               specifies the type, followed by the fraction representation
     * @return a `RelativeOrAbsoluteFraction` object parsed from the string, or null if the string cannot be parsed
     */
    public static RelativeOrAbsoluteFraction raf(final String source) {

        return raf(null, source);
    }

    /**
     * Creates a new `RelativeOrAbsoluteFraction` object with the specified `Fraction` and `RelativeOrAbsolute`.
     *
     * @param fraction the `Fraction` to be associated with the created `RelativeOrAbsoluteFraction` object
     * @param type     the `RelativeOrAbsolute` indicating whether the created `RelativeOrAbsoluteFraction` is absolute or
     *                 relative
     * @return a new `RelativeOrAbsoluteFraction` object initialised with the specified `Fraction` and `RelativeOrAbsolute`
     */
    public static RelativeOrAbsoluteFraction raf(final Fraction fraction, final RelativeOrAbsolute type) {

        return new RelativeOrAbsoluteFraction(fraction, type);
    }

    /**
     * Creates a `RelativeOrAbsoluteFraction` object with a zero `Fraction` and `RelativeOrAbsolute.ABSOLUTE`.
     *
     * @return a new `RelativeOrAbsoluteFraction` object with `Fraction.ZERO` and `RelativeOrAbsolute.ABSOLUTE`
     */
    public static RelativeOrAbsoluteFraction raf() {

        return new RelativeOrAbsoluteFraction(ZERO, A);
    }

    @Override
    public String data() {

        if (R == type) return s(Symbols.R, ts(false, fraction, false));
        if (A == type) return s(Symbols.A, ts(false, fraction, false));
        else return null;
    }

    /**
     * Computes a double representation based on a reference Fraction and a specified range.
     *
     * @param base      the range of Fraction values within which the reference value is evaluated
     * @param reference the Fraction reference value for comparison and computation
     * @return a Double representing the processed value of the reference within the specified range,
     * or null if any input is null
     * @throws IllegalStateException if the reference value is outside the provided range and reaches an undefined state
     */
    @Override
    public Double doubleValue(final _Range<Fraction> base, final Fraction reference) {

        if (isAnyNull(base, reference)) return null;
        if (base.includes(reference)) return doubleValue(reference);
        if (reference.doubleValue() > base.max().doubleValue()) return doubleValue(base.max());
        if (reference.doubleValue() < base.min().doubleValue()) return doubleValue(base.min());
        throw new IllegalStateException(HOW_DID_WE_GET_HERE);
    }

    public Double doubleValue(final Fraction base) {

        if (R == type) return fraction.doubleValue() * base.doubleValue();
        if (A == type) return fraction.doubleValue();
        else return null;
    }

    @Override
    public String render(final boolean full, final boolean scripts) {

        if (R == type) return b(RELATIVE, ts(full, fraction, scripts));
        if (A == type) return b(ABSOLUTE, ts(full, fraction, scripts));
        else return null;
    }

    @Override
    public String render() {

        return render(false, true);
    }
}