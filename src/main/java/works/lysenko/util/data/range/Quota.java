package works.lysenko.util.data.range;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.t._Value;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;

import java.util.Map;

import static org.apache.commons.math3.util.FastMath.abs;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.v;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.word.F.FRACTION;
import static works.lysenko.util.lang.word.M.MAXIMUM;
import static works.lysenko.util.lang.word.M.MINIMUM;
import static works.lysenko.util.lang.word.R.RANGE;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Represents a quota object with a value, precision, and a range defined by minimum and maximum fractions.
 * The Quota class allows for validation, calculation, and conversion related to quota values.
 *
 * @param <T> The type of the value held by the quota object.
 */
@SuppressWarnings({"CallToSuspiciousStringMethod", "MissingJavadoc"})
public record Quota<T>(T value, T precision, String stamp, Fraction min, Fraction max) implements _Quota<T> {

    /**
     * Represents the maximum share value.
     */
    public static final double MAX = 1.0;
    /**
     * Represents the minimum share value.
     */
    private static final double MIN = 0.0;

    /**
     * Constructs a new Quota object and validates the provided minimum and maximum fractional values.
     * Throws an IllegalArgumentException if the provided values are out of the acceptable range.
     *
     * @param min The minimum fractional value for the Quota. Must be within the range defined by MIN and MAX.
     * @param max The maximum fractional value for the Quota. Must be within the range defined by MIN and MAX.
     * @throws IllegalArgumentException if the value of min or max is not within the specified range.
     */
    public Quota {

        if (MIN > min.doubleValue() || MAX < min.doubleValue()) {
            throw new IllegalArgumentException(b(c(MINIMUM), FRACTION, ts(min), IS, NOT, IN, s(MIN), DOTS, s(MAX), RANGE));
        }
        if (MIN > max.doubleValue() || MAX < max.doubleValue()) {
            throw new IllegalArgumentException(b(c(MAXIMUM), FRACTION, ts(min), IS, NOT, IN, s(MIN), DOTS, s(MAX), RANGE));
        }
    }

    /**
     * Calculates the median value of the given Quota object by averaging the minimum and maximum fractions.
     *
     * @param value The Quota object to calculate the median value for.
     * @return The median value as a double.
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    static double median(final Quota<?> value) {

        return (value.min().doubleValue() + value.max().doubleValue()) / 2;
    }

    /**
     * Creates a Quota object of type Fraction using the specified Map.Entry, precision, and stamp.
     * The Quota is initialized with the key from the Map.Entry, the precision, the stamp, and the exact share
     * derived from the value of the Map.Entry.
     *
     * @param value     A Map.Entry where the key is a Fraction representing the identifier for the quota
     *                  and the value is a ValuedRangeResult containing the fractional share.
     * @param precision A Fraction specifying the precision level, which defines the granularity of the resulting Quota.
     * @param stamp     A string identifier or marker for the resulting Quota.
     * @return A Quota object of type Fraction initialised with the specified key, precision, stamp, and fractional share.
     */
    public static Quota<Fraction> shareOfFraction(final Map.Entry<? extends Fraction, ? extends _ValuedRangeResult> value,
                                                  final Fraction precision, final String stamp) {

        final Fraction exactShare = value.getValue().value();
        return new Quota<>(value.getKey(), precision, stamp, exactShare, exactShare);
    }

    /**
     * Creates a Quota object of type Integer using the specified integer value and exact fractional share.
     *
     * @param value      The integer value for the Quota object.
     * @param exactShare A Fraction representing the exact fractional share associated with the Quota.
     * @return A Quota object of type Integer initialised with the specified value and exact fractional share.
     */
    public static Quota<Integer> shareOfInteger(final int value, final Fraction exactShare) {

        return new Quota<>(value, null, null, exactShare, exactShare);
    }

    /**
     * Creates a Quota object of type Integer using the given value, precision, stamp, and exact share.
     *
     * @param value      The integer value for the Quota object.
     * @param precision  An Integer specifying the precision level, which defines the granularity of the Quota.
     * @param stamp      A String identifier or marker for the resulting Quota.
     * @param exactShare A Fraction representing the exact fractional share associated with the Quota.
     * @return A Quota object of type Integer initialised with the specified value, precision, stamp, and exact fractional
     * share.
     */
    public static Quota<Integer> shareOfInteger(final int value, final Integer precision, final String stamp,
                                                final Fraction exactShare) {

        return new Quota<>(value, precision, stamp, exactShare, exactShare);
    }

    /**
     * Creates a Quota object of type Integer using the provided value, precision, stamp, and share.
     * The share parameter is used to determine the exact fractional share associated with the Quota.
     *
     * @param value     The integer value to assign to the Quota object.
     * @param precision An Integer specifying the precision level, defining the granularity of the Quota.
     * @param stamp     A String identifier or marker for the resulting Quota.
     * @param share     A _ValuedRangeResult containing the fractional value used to calculate the exact share.
     * @return A Quota object of type Integer initialised with the specified value, precision, stamp, and exact fractional
     * share.
     */
    public static Quota<Integer> shareOfInteger(final int value, final Integer precision, final String stamp,
                                                final _ValuedRangeResult share) {

        final Fraction exactShare = share.value();
        return new Quota<>(value, precision, stamp, exactShare, exactShare);
    }

    /**
     * Creates a Quota object of type Integer using the specified Map.Entry, precision, and stamp.
     * The Quota is initialised with the key from the Map.Entry, the precision, the stamp, and the exact
     * share derived from the value of the Map.Entry.
     *
     * @param value     A Map.Entry where the key is an Integer representing the identifier for the quota
     *                  and the value is a ValuedRangeResult containing the fractional share.
     * @param precision An integer specifying the precision level, which defines the granularity of the resulting Quota.
     * @param stamp     A string identifier or marker for the resulting Quota.
     * @return A Quota object of type Integer initialised with the specified key, precision, stamp, and fractional share.
     */
    public static Quota<Integer> shareOfInteger(final Map.Entry<Integer, ? extends _ValuedRangeResult> value,
                                                final int precision, final String stamp) {

        final Fraction exactShare = value.getValue().value();
        return new Quota<>(value.getKey(), precision, stamp, exactShare, exactShare);
    }

    /**
     * Creates a Quota object of type Integer using the provided value, precision, stamp, minimum, and maximum fractions.
     *
     * @param value     The integer value to initialise the Quota object.
     * @param precision An Integer specifying the precision level, which defines the granularity of the Quota.
     * @param stamp     A String identifier or marker for the resulting Quota.
     * @param min       A Fraction specifying the minimum allowable value for the Quota.
     * @param max       A Fraction specifying the maximum allowable value for the Quota.
     * @return A Quota object of type Integer initialised with the specified value, precision, stamp, minimum, and maximum
     * fractions.
     */
    public static Quota<Integer> shareOfInteger(final int value, final Integer precision, final String stamp,
                                                final Fraction min, final Fraction max) {

        return new Quota<>(value, precision, stamp, min, max);
    }

    @SuppressWarnings({"IfStatementWithTooManyBranches", "MethodWithMultipleReturnPoints"})
    public Fraction actualAbsoluteMargin(final Fraction actualValue) {

        if ((actualValue.doubleValue() >= min.doubleValue()) && (actualValue.doubleValue() <= max.doubleValue()))
            return fr(ZERO);
        else if (actualValue.doubleValue() > max.doubleValue()) return fr(abs(actualValue.doubleValue() - max.doubleValue()));
        else if (actualValue.doubleValue() < min.doubleValue()) return fr(abs(actualValue.doubleValue() - min.doubleValue()));
        else return null;
    }

    public double median() {

        return (min().doubleValue() + max().doubleValue()) / 2;
    }

    @Override
    public String percentage() {

        return percentString(width(), MAX - MIN);
    }

    public String render() {

        return toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public _Value<T> share() {

        return (_Value<T>) v(new FractionRange(min, max));
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {

        final String range = (ts(min).equals(ts(max))) ? ts(min) : b(L0, ts(min), ts(max));
        return b(L0, s(value), range);
    }

    @Override
    public double width() {

        return max.doubleValue() - min.doubleValue();
    }
}