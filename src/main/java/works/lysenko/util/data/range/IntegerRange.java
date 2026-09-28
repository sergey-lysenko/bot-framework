package works.lysenko.util.data.range;

import org.apache.commons.lang3.Range;
import works.lysenko.util.apis.grid.t._Range;

import java.util.Objects;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.ANY;
import static works.lysenko.util.chrs.___.OUT;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.data.Regexes.l0;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.lang.word.B.BOUNDS;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols.FOR_ALL;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._PLUS_;

/**
 * Represents a class that stores minimum and maximum integer values.
 */
@SuppressWarnings({"DataFlowIssue", "FieldNotUsedInToString"})
public class IntegerRange implements _Range<Integer> {

    private final Range<Integer> range;

    /**
     * Parses ExpectedDistribution from string.
     * <p>
     * {element},{element},...
     * <p>
     * {element} can have several forms:
     * - {min}|{max},...
     * - {min}+... more then, short for {min}|{globalMin}
     * - {max}-... less then, short for {max}|{globalMax}
     * - {value}... (very strict requirement of single value)
     *
     * @param source    string in format described above
     * @param globalMin the global minimum value
     * @param globalMax the global maximum value
     */
    @SuppressWarnings({"OverlyComplexMethod", "CallToSuspiciousStringMethod", "QuestionableName",
            "IfStatementWithTooManyBranches"})
    public IntegerRange(final String source, final Integer globalMin, final Integer globalMax) {

        final String[] values = source.trim().split(l0);
        if (ZERO < values.length) {
            final Integer two; // value defined later
            final String rawOne = values[ZERO];
            final String stringOne = rawOne.replace(s(_PLUS_), EMPTY).replace(s(_DASH_), EMPTY);

            Integer one;
            try {
                one = i(stringOne);
            } catch (final NumberFormatException e) {
                one = null;
            }
            if (isNull(one)) range = null;
            else {
                if ((one < globalMin) || (one > globalMax)) {
                    logEvent(S0, b(VALUE, s(one), IS, OUT, OF, BOUNDS, s(globalMin, L0, globalMax)));
                    range = null;
                } else {
                    if (values[ZERO].endsWith(s(_PLUS_))) two = globalMax;
                    else if (values[ZERO].endsWith(s(_DASH_))) two = globalMin;
                    else if (ONE < values.length) two = i(values[ONE]);
                    else two = one;
                    if ((two.doubleValue() < globalMin) || (two.doubleValue() > globalMax)) {
                        logEvent(S0, b(VALUE, s(two), IS, OUT, OF, BOUNDS, s(globalMin, L0, globalMax)));
                        range = null;
                    } else {
                        final int min = (0 >= one.compareTo(two)) ? one : two;
                        final int max = (0 <= one.compareTo(two)) ? one : two;
                        range = Range.of(min, max);
                    }
                }
            }
        } else
            range = null;
    }

    /**
     * Parses a string representation of IntMinMax from the given source string.
     * <p>
     * The source string should be in the following format:
     * - {element},{element},...
     * <p>
     * Each element can have several forms:
     * - {min}|{max},...
     * - {min}+... (representing a value greater than {min}, equivalent to {min}|{globalMin})
     * - {max}-... (representing a value less than {max}, equivalent to {max}|{globalMax})
     * - {value}... (a strict requirement of a single value)
     *
     * @param source The string to parse in the format described above.
     */
    public IntegerRange(final String source) {

        this(source, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Represents a range of integers with a minimum and maximum value.
     *
     * @param min value
     * @param max value
     */
    @SuppressWarnings("unused")
    public IntegerRange(final int min, final int max) {

        range = Range.of(min, max);
    }

    /**
     * Represents a range of integers with same single minimum and maximum value.
     *
     * @param i value
     */
    @SuppressWarnings("unused")
    public IntegerRange(final int i) {

        range = Range.of(i, i);
    }

    @Override
    public final Range<Integer> get() {

        return range;

    }

    public final Boolean includes(final Integer i) {

        return range.contains(i);
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public final Integer margin(final Integer value) {

        if (isAnyNull(min(), max())) return null;
        if (value > max()) return value - max();
        if (value < min()) return value - min();
        return ZERO;
    }

    /**
     * Retrieves the maximum value of type Integer.
     *
     * @return the maximum value
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public final Integer max() {

        if (isNull(range)) return null;
        return range.getMaximum();
    }

    /**
     * Retrieves the minimum value of type Integer.
     *
     * @return the minimum value
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public final Integer min() {

        if (isNull(range)) return null;
        return range.getMinimum();
    }

    public final String toString() {

        return toString(false);
    }

    public final String toVerboseString() {

        return toString(true);
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private String toString(final boolean verbose) {

        final boolean noMin = Objects.equals(min(), Integer.MIN_VALUE);
        final boolean noMax = Objects.equals(max(), Integer.MAX_VALUE);
        final boolean equal = Objects.equals(min(), max());

        if (noMin && noMax) return verbose ? ANY : s(FOR_ALL);
        if (noMin) return verbose ? b(s(max()), OR, LESS) : s(max(), L0, s(_DASH_));
        if (noMax) return verbose ? b(s(min()), OR, MORE) : s(min(), L0, s(_PLUS_));
        if (equal) return s(min());
        return verbose ? b(FROM, s(min()), TO, s(max())) : s(min(), L0, max());
    }
}