package works.lysenko.util.data.range;

import org.apache.commons.lang3.Range;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Range;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.___.OUT;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.data.Regexes.l0;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.word.B.BOUNDS;
import static works.lysenko.util.lang.word.F.FRACTION;
import static works.lysenko.util.lang.word.M.MAXIMUM;
import static works.lysenko.util.lang.word.M.MINIMUM;
import static works.lysenko.util.lang.word.R.RANGE;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._PLUS_;

/**
 * Represents a range of Fraction values with a minimum and maximum value.
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "DataFlowIssue"})
public class FractionRange implements _Range<Fraction> {

    @SuppressWarnings("FieldNotUsedInToString")
    private final Range<Fraction> range;

    /**
     * Constructs a FractionMinMax object.
     *
     * @param source    the source string from which to extract values
     * @param globalMin the global minimum value of the range
     * @param globalMax the global maximum value of the range
     */
    @SuppressWarnings({"OverlyComplexMethod", "QuestionableName", "CallToSuspiciousStringMethod"})
    public FractionRange(final String source, final Fraction globalMin, final Fraction globalMax) {

        final String[] values = source.trim().split(l0);
        if (ZERO < values.length) {
            final String stringOne = values[ZERO].replace(s(_PLUS_), EMPTY).replace(s(_DASH_), EMPTY);
            final Fraction one = fr(stringOne);
            if (isNotNull(one)) {
                if ((one.doubleValue() < globalMin.doubleValue()) || (one.doubleValue() > globalMax.doubleValue())) {
                    logEvent(S0, b(VALUE, s(one), IS, OUT, OF, BOUNDS, s(globalMin, L0, globalMax)));
                    range = null;
                } else range = getFractionRange(globalMin, globalMax, values, one);
            } else range = null;
        } else range = null;
    }

    /**
     * Represents a range of Fractions with a minimum and maximum value.
     *
     * @param range the range of Fractions
     */
    public FractionRange(final Range<Fraction> range) {

        this.range = range;
    }

    /**
     * Represents a range of Fractions with a minimum and maximum value.
     *
     * @param min value
     * @param max value
     */
    @SuppressWarnings("unused")
    public FractionRange(final Fraction min, final Fraction max) {

        range = Range.of(min, max);
    }


    /**
     * Represents a range of Fractions with same single minimum and maximum value.
     *
     * @param i value
     */
    @SuppressWarnings("StandardVariableNames")
    public FractionRange(final Fraction i) {

        range = Range.of(i, i);
    }

    /**
     * Constructs a new FractionRange object based on a given range and an included fraction.
     *
     * @param range   the range to base the FractionRange on
     * @param include the Fraction to include in the range
     */
    public FractionRange(final FractionRange range, final Fraction include) {

        final Fraction min = range.min();
        final Fraction max = range.max();

        if (isNull(min) || isNull(max)) this.range = null;
        else {
            if (include.doubleValue() < min.doubleValue()) this.range = Range.of(include, max);
            else if (include.doubleValue() > max.doubleValue()) this.range = Range.of(min, include);
            else this.range = Range.of(min, max);
        }
    }

    /**
     * Constructs a FractionRange object.
     *
     * @param source    the source string from which to extract values
     * @param globalMin the global minimum value of the range
     * @param globalMax the global maximum value of the range
     * @return the FractionRange object constructed based on the given parameters
     */
    public static FractionRange frr(final String source, final Fraction globalMin, final Fraction globalMax) {

        return new FractionRange(source, globalMin, globalMax);
    }

    /**
     * Creates a new FractionRange object with the specified minimum and maximum Fraction values.
     *
     * @param min the minimum Fraction value for the range
     * @param max the maximum Fraction value for the range
     * @return a new FractionRange object representing the range between the specified minimum and maximum values
     */
    public static FractionRange frr(final Fraction min, final Fraction max) {

        return new FractionRange(min, max);
    }


    /**
     * Determines a range of fractions based on the given parameters.
     * The range is ensured to be within the bounds of globalMin and globalMax.
     * If the provided fractions are not within the bounds, it throws an IllegalArgumentException.
     *
     * @param globalMin the global minimum value allowed for the range
     * @param globalMax the global maximum value allowed for the range
     * @param one       the first fraction value to consider for the range
     * @param two       the second fraction value to consider for the range
     * @return a Range<Fraction> object that represents the determined range of fractions
     * @throws IllegalArgumentException if the calculated minimum or maximum fractions are not within the global bounds
     */
    @SuppressWarnings("QuestionableName")
    private static Range<Fraction> getFractionRange(final Fraction globalMin, final Fraction globalMax, final Fraction one,
                                                    final Fraction two) {

        final Range<Fraction> range;
        final Fraction min = (0 >= one.compareTo(two)) ? one : two;
        final Fraction max = (0 <= one.compareTo(two)) ? one : two;
        if (min.doubleValue() < globalMin.doubleValue() || min.doubleValue() > globalMax.doubleValue()) {
            throw new IllegalArgumentException(b(c(MINIMUM), FRACTION, ts(min), IS, NOT, IN, s(globalMin), DOTS,
                    s(globalMax), RANGE));
        }
        if (max.doubleValue() < globalMin.doubleValue() || max.doubleValue() > globalMax.doubleValue()) {
            throw new IllegalArgumentException(b(c(MAXIMUM), FRACTION, ts(min), IS, NOT, IN, s(globalMin), DOTS,
                    s(globalMax), RANGE));
        }
        range = Range.of(min, max);
        return range;
    }

    /**
     * Determines a range of fractions based on the given parameters.
     * The range is defined within the bounds of globalMin and globalMax.
     * It derives the second boundary of the range based on the provided values or defaults to a predefined fraction.
     *
     * @param globalMin the global minimum value allowed for the range
     * @param globalMax the global maximum value allowed for the range
     * @param values    an array of strings from which to derive the second boundary of the range
     * @param one       the default fraction to use as the second boundary if not specified in values
     * @return a Range<Fraction> object that represents the determined range of fractions, or null if the
     * derived second boundary is out of the specified global bounds
     */
    @SuppressWarnings({"QuestionableName", "IfStatementWithTooManyBranches"})
    private static Range<Fraction> getFractionRange(final Fraction globalMin, final Fraction globalMax,
                                                    final String[] values, final Fraction one) {

        final Fraction two; // value defined later
        final Range<Fraction> range;
        if (values[ZERO].endsWith(s(_PLUS_))) two = globalMax;
        else if (values[ZERO].endsWith(s(_DASH_))) two = globalMin;
        else if (ONE < values.length) two = fr(values[ONE]);
        else two = one;
        if ((two.doubleValue() < globalMin.doubleValue()) || (two.doubleValue() > globalMax.doubleValue())) {
            logEvent(S0, b(VALUE, s(two), IS, OUT, OF, BOUNDS, s(globalMin, L0, globalMax)));
            range = null;
        } else range = getFractionRange(globalMin, globalMax, one, two);
        return range;
    }

    @Override
    public final Range<Fraction> get() {

        return range;
    }

    /**
     * Checks if the given fraction is included in the range defined by the minimum and maximum fraction values.
     *
     * @param fraction the fraction to be checked
     * @return true if the fraction is included in the range, false otherwise
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public final Boolean includes(final Fraction fraction) {

        if (range.getMinimum().equals(range.getMaximum())) return range.getMinimum().equals(fraction);
        return 0 <= fraction.compareTo(range.getMinimum()) && 0 >= fraction.compareTo(range.getMaximum());
    }

    @Override
    public final Fraction margin(final Fraction value) {

        if (value.doubleValue() > max().doubleValue()) return fr(value.doubleValue() - max().doubleValue());
        if (value.doubleValue() < min().doubleValue()) return fr(value.doubleValue() - min().doubleValue());
        return Fraction.ZERO;
    }

    /**
     * Returns the maximum Fraction value of the range.
     *
     * @return the maximum Fraction value of the range
     */
    public final Fraction max() {

        if (isNull(range)) return null;
        return range.getMaximum();
    }

    /**
     * Returns the minimum Fraction value of the range.
     *
     * @return the minimum Fraction value of the range
     */
    public final Fraction min() {

        if (isNull(range)) return null;
        return range.getMinimum();
    }

    public final String toString() {

        return toString(false);
    }

    @Override
    public final String toVerboseString() {

        return toString(true);
    }

    private String toString(final boolean full) {

        if (isNull(range)) return null;
        String s = ts(full, range.getMinimum());
        if (!range.getMinimum().equals(range.getMaximum())) s = s(s, L0, ts(full, range.getMaximum()));
        return s;
    }
}