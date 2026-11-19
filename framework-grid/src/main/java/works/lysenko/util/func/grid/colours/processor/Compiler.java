package works.lysenko.util.func.grid.colours.processor;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.FractionRange;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.func.logs.Trace;
import works.lysenko.util.prop.grid.Ranges;
import works.lysenko.util.prop.grid.Update;
import works.lysenko.util.spec.Symbols;

import java.util.Collection;
import java.util.List;

import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static org.apache.commons.math3.util.FastMath.abs;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Severity.S3;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.logs.trace.Data.traceable;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.B.BY_ACTUAL_VALUE;
import static works.lysenko.util.lang.H.HAS_NO_WIDTH;
import static works.lysenko.util.lang.I.IS_ALREADY_ON_EDGE_OF;
import static works.lysenko.util.lang.I.IS_TOO_CLOSE_TO___;
import static works.lysenko.util.lang.M.MERGING_ACTUAL_VALUE;
import static works.lysenko.util.lang.S.SHRINKING_WAS_DISABLED_BY_CONFIGURATION;
import static works.lysenko.util.lang.S.SHRINKING_WAS_DISABLED_BY_REQUEST;
import static works.lysenko.util.lang.W.WIDTH_IS_STILL;
import static works.lysenko.util.lang.W.WIDTH_WAS_REDUCED_BY___;
import static works.lysenko.util.lang.word.A.ACTUAL;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.R.RANGE;
import static works.lysenko.util.lang.word.S.SHRINKING;
import static works.lysenko.util.lang.word.U.USING;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * Represents a Compiler class for compiling and optimizing code.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "rawtypes"})
public record Compiler() {

    /**
     * Logs a debug message with the details of the Quota object and adds the Quota object to a collection.
     *
     * @param expected     The Quota object to be logged and added to the collection.
     * @param in           The collection where the Quota object will be added.
     * @param actualString The string representation of the Quota object.
     */
    private static void already(final _Quota expected, final Collection<? super _Quota> in, final String actualString) {

        logDebug(b(c(ACTUAL), VALUE, actualString, IS_ALREADY_ON_EDGE_OF, s(expected)));
        in.add(expected);
    }

    /**
     * Logs a debug message with the details of the Quota object and adds the Quota object to a collection.
     *
     * @param expected The Quota object to be added to the collection.
     * @param in       The collection where the Quota object will be added.
     */
    private static void disabledByConfig(final _Quota expected, final Collection<? super _Quota> in) {

        logDebug(SHRINKING_WAS_DISABLED_BY_CONFIGURATION);
        in.add(expected);
    }

    /**
     * Logs a debug message indicating that a Quota object was disabled by request and adds it to a collection.
     *
     * @param expected The Quota object that was disabled.
     * @param in       The collection where the disabled Quota object will be added.
     */
    private static void disabledByRequest(final _Quota expected, final Collection<? super _Quota> in) {

        logDebug(SHRINKING_WAS_DISABLED_BY_REQUEST);
        in.add(expected);
    }

    /**
     * Shrinks the range of a Quota object and adds the updated Quota object to a Collection.
     *
     * @param key      The key associated with the Quota object.
     * @param actual   The actual Fraction value to compare with the expected range.
     * @param expected The expected Quota object representing the range.
     * @param in       The Collection where the new Quota object will be added after shrinking.
     */
    private static void doShrinking(final Integer key, final Fraction actual, final _Quota expected, final Collection<?
            super Quota> in) {

        logDebug(b(c(SHRINKING), EXPECTED, s(expected), BY_ACTUAL_VALUE, s(ts(true, actual))));
        final String oldPercentage = expected.percentage();
        final double median = expected.median();
        final double radius = abs(median - actual.doubleValue());
        final String newPercentage = expected.percentage();
        final Fraction min = fr(median - radius);
        final Fraction max = fr(median + radius);
        final Quota<Integer> newExpected = new Quota<>(key, (Integer) expected.precision(), expected.stamp(), min, max);
        explain(actual, expected, oldPercentage, newPercentage);
        in.add(newExpected);
    }

    /**
     * Logs a debug message based on the comparison between actual and expected values of a Quota object.
     *
     * @param actual        The actual Fraction value to compare.
     * @param expected      The expected Quota object to be compared to.
     * @param oldPercentage The old percentage value.
     * @param newPercentage The new percentage value.
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    private static void explain(final Fraction actual, final _Quota expected, final String oldPercentage,
                                final String newPercentage) {

        if (Ranges.logBoundaries) {
            if (oldPercentage.equals(newPercentage))
                logEvent(S3, b(c(ACTUAL), VALUE, s(ts(true, actual)), IS_TOO_CLOSE_TO___, s(expected, _COMMA_),
                        WIDTH_IS_STILL, oldPercentage));
            else
                logEvent(S3, b(c(EXPECTED), s(expected), WIDTH_WAS_REDUCED_BY___, s(ts(true, actual)), FROM, oldPercentage,
                        TO, newPercentage));
        }
    }

    /**
     * Merges the given actual Fraction value with the expected Quota object and adds the result to the provided List.
     *
     * @param key      The key associated with the Quota object.
     * @param actual   The actual Fraction value to merge.
     * @param expected The expected Quota object representing the range for merging.
     * @param in       The List where the merged Quota object will be added.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static void merge(final Integer key, final _ValuedRangeResult actual, final _Quota expected, final List<?
            super Quota> in) {

        logDebug(b(MERGING_ACTUAL_VALUE, ts(true, actual.value()), WITH, s(expected), FOR, s(key)));
        final Fraction min = fr(min(actual.doubleValue(), expected.min().doubleValue()));
        final Fraction max = fr(max(actual.doubleValue(), expected.max().doubleValue()));
        final FractionRange fractionRange = new FractionRange(min, max);
        Trace.log(traceable(fractionRange));
        in.add(new Quota<>(key, expected.precision(), expected.stamp(), min, max));
    }

    /**
     * Logs a debug message with the details of the Quota object and adds the Quota object to a collection.
     *
     * @param expected The Quota object to be logged and added to the collection.
     * @param in       The collection where the Quota object will be added.
     */
    private static void noWidth(final _Quota expected, final Collection<? super _Quota> in) {

        logDebug(b(c(EXPECTED), VALUE, s(expected), HAS_NO_WIDTH));
        in.add(expected);
    }

    /**
     * Reuses a Quota object by adding it to a list.
     *
     * @param title The title of the reuse operation.
     * @param range The Quota object to be reused.
     * @param in    The list where the Quota object will be added.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static void reuse(final String title, final _Quota<Integer> range, final List<? super _Quota<Integer>> in) {

        logDebug(b(c(USING), title, RANGE, s(range)));
        in.add(range);
    }

    /**
     * Shrinks the range of a Quota object based on the actual value and expected range.
     *
     * @param key          The key associated with the Quota object.
     * @param actual       The actual Fraction value to compare with the expected range.
     * @param expected     The expected Quota object representing the range.
     * @param in           The Collection where the new Quota object will be added after shrinking.
     * @param ignoreShrink Flag indicating whether shrinking should be ignored.
     */
    @SuppressWarnings({"CallToSuspiciousStringMethod", "rawtypes"})
    public static void shrink(final Integer key, final _ValuedRangeResult actual, final _Quota expected, final Collection<?
            super _Quota> in, final boolean ignoreShrink) {

        if (Update.shrink) {
            if (ignoreShrink) disabledByRequest(expected, in);
            else {
                if (ZERO < expected.width()) {
                    final String s = ts(actual.value());
                    logDebug(b(Symbols.LSS_THN, e(ts(true, expected.min())), e(ts(true, actual.value())), e(ts(true,
                            expected.max()))));
                    if (ts(expected.max()).equals(s) || ts(true, expected.min()).equals(s)) already(expected, in, s);
                    else doShrinking(key, actual.value(), expected, in);
                } else noWidth(expected, in);
            }
        } else disabledByConfig(expected, in);
    }
}