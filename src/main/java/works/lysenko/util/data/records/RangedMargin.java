package works.lysenko.util.data.records;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._RangedMargin;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.data.enums.RangedMarginType;
import works.lysenko.util.data.range.FractionRange;

import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.____.LESS;
import static works.lysenko.util.chrs.____.MORE;
import static works.lysenko.util.chrs.____.THEN;
import static works.lysenko.util.data.enums.RelativeOrAbsolute.ABSOLUTE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.data.records.RelativeOrAbsoluteFraction.raf;
import static works.lysenko.util.func.data.Regexes.l0;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.H.HOW_DID_WE_GET_HERE;
import static works.lysenko.util.lang.U.UNABLE_TO_PARCE_RANGEDMARGIN___;
import static works.lysenko.util.lang.word.R.RANGE;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.TWO;

/**
 * Encapsulates a range-aware margin composed of two {@code RelativeOrAbsoluteFraction} values
 * and an associated {@code FractionRange}. This class is primarily utilized in scenarios where
 * margins or bounds are defined as either relative or absolute, along with an applicable range.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public record RangedMargin(RelativeOrAbsoluteFraction at0, RelativeOrAbsoluteFraction at1,
                           FractionRange range) implements _RangedMargin {

    /**
     * Creates a {@code RangedMargin} object with the specified starting and ending
     * {@code RelativeOrAbsoluteFraction} values. The {@code RangedMargin} is initialized
     * with a default range from zero to one.
     *
     * @param at0 the starting {@code RelativeOrAbsoluteFraction} value
     * @param at1 the ending {@code RelativeOrAbsoluteFraction} value
     * @return a new {@code RangedMargin} object initialized with the given fractions and default range
     */
    public static RangedMargin rm(final RelativeOrAbsoluteFraction at0, final RelativeOrAbsoluteFraction at1) {

        return new RangedMargin(at0, at1, FractionRange.frr(Fraction.ZERO, Fraction.ONE));
    }

    /**
     * Parses a string representation of a ranged margin and returns a corresponding {@code RangedMargin} object.
     * The input string is expected to consist of one or two parts separated by a delimiter defined in {@code Regexes.l0}.
     * <p>
     * If the string contains only one part, it will be used for both the start and end fractions of the range.
     * If the string contains two parts, the first part will represent the start fraction, and the second part
     * will represent the end fraction. The method will throw an {@code IllegalArgumentException} if the input
     * string has an invalid format.
     *
     * @param source the string representation of the ranged margin to parse; expected to contain one or two parts
     *               separated by the {@code Regexes.l0} delimiter
     * @return a {@code RangedMargin} object created from the parsed fractions
     * @throws IllegalArgumentException if the input string has an invalid format or cannot be parsed into fractions
     */
    public static RangedMargin rm(final String source) {

        final String[] array = source.split(l0);
        if (array.length < ONE || array.length > TWO)
            throw new IllegalArgumentException(b(UNABLE_TO_PARCE_RANGEDMARGIN___, q(source)));
        final RelativeOrAbsoluteFraction at0 = RelativeOrAbsoluteFraction.raf(array[0]);
        final RelativeOrAbsoluteFraction at1 = array.length == TWO ? RelativeOrAbsoluteFraction.raf(array[1]) : at0;
        return rm(at0, at1);
    }

    /**
     * Creates a {@code RangedMargin} object using the provided {@code RelativeOrAbsoluteFraction}.
     * The resulting {@code RangedMargin} is initialized with the same start and end fractions
     * and a default range from zero to one.
     *
     * @param raf the {@code RelativeOrAbsoluteFraction} to be used for both the starting and
     *            ending fractions of the {@code RangedMargin}
     * @return a {@code RangedMargin} object with the specified fractions and a predefined range
     */
    public static RangedMargin rm(final RelativeOrAbsoluteFraction raf) {

        return new RangedMargin(raf, raf, FractionRange.frr(Fraction.ZERO, Fraction.ONE));
    }

    /**
     * Creates a {@code RangedMargin} object using the given {@code Fraction}. The resulting
     * {@code RangedMargin} will be initialized with a range from zero to one.
     *
     * @param f the {@code Fraction} to be used in creating the {@code RangedMargin}
     * @return a {@code RangedMargin} object created using the given {@code Fraction}
     */
    @SuppressWarnings("StandardVariableNames")
    public static RangedMargin rm(final Fraction f) {

        return rm(raf(f, ABSOLUTE));
    }

    /**
     * Creates a new {@code RangedMargin} object initialized with a default
     * range from zero to one.
     *
     * @return a {@code RangedMargin} object with default settings
     */
    public static RangedMargin rm() {

        return rm(Fraction.ZERO);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public double doubleValue(final Fraction base) {

        if (isAnyNull(range.min(), range.max())) throw new IllegalArgumentException(b(c(UNKNOWN), RANGE));
        if (base.doubleValue() < range.min().doubleValue())
            throw new IllegalArgumentException(b(ts(true, base), IS, LESS, THEN, ts(true, range.min())));
        if (base.doubleValue() > range.max().doubleValue())
            throw new IllegalArgumentException(b(ts(true, base), IS, MORE, THEN, ts(true, range.min())));

        final Double r0 = at0.doubleValue(base);
        final Double r1 = at1.doubleValue(base);

        final double doubleValue = base.doubleValue();
        return r0 + doubleValue * (r1 - r0);
    }

    @Override
    public Double doubleValue(final _Range<Fraction> base, final Fraction reference) {

        if (isAnyNull(base, reference)) return null;
        if (base.includes(reference)) return doubleValue(reference);
        if (reference.doubleValue() > base.max().doubleValue()) return doubleValue(base.max());
        if (reference.doubleValue() < base.min().doubleValue()) return doubleValue(base.min());
        throw new IllegalStateException(HOW_DID_WE_GET_HERE);
    }

    @Override
    public String render() {

        if (isEquals()) return at0.render();
        return (s(at0.render(), L0, at1().render()));
    }

    @Override
    public RangedMarginType type() {

        if (isEquals()) return RangedMarginType.STATIC;
        else return RangedMarginType.DYNAMIC;
    }

    @SuppressWarnings("CallToSuspiciousStringMethod")
    private boolean isEquals() {

        return at0.render().equals(at1().render());
    }
}
