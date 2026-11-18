package works.lysenko.util.func.grid.ranger;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.data.enums.Brackets;
import works.lysenko.util.data.enums.RangeResult;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.range.FractionRange;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.grid.record.meta.ValidationMeta;
import works.lysenko.util.prop.grid.Ranges;
import works.lysenko.util.prop.grid.WideRange;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.___.AND;
import static works.lysenko.util.data.enums.ColoursIgnore.SHRINK;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.enums.Severity.S3;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.logs.OutOfBounds.getFractionConditionalResult;
import static works.lysenko.util.func.logs.OutOfBounds.getIntegerConditionalResult;
import static works.lysenko.util.func.type.Fractions.precision;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.A.ARE_TOO_SIMILAR;
import static works.lysenko.util.lang.D.DIFFERENCE_IS;
import static works.lysenko.util.lang.I.IS_WIDER_THAN;
import static works.lysenko.util.lang.T.THRESHOLD_DEFINED_IN;
import static works.lysenko.util.lang.word.R.RANGE;
import static works.lysenko.util.lang.word.V.VALUES;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Symbols._COLON_;

/**
 * The VerifyResource class provides methods for validation and verification of values.
 */
@SuppressWarnings({"MethodWithTooManyParameters", "NonBooleanMethodNameMayNotStartWithQuestion"})
record Verify() {

    /**
     * Retrieves the values based on the expected range and actual share.
     *
     * @param expectedRange The range within which the actual share is expected to fall.
     * @param actualShare   The actual share value to be compared.
     * @return Values object containing the maximum value of the range, minimum value of the range, and the actual share value.
     */
    private static Values getValues(final Quota<?> expectedRange, final _ValuedRangeResult actualShare) {

        final Fraction expectedMax = isNull(expectedRange) ? null : expectedRange.max();
        final Fraction expectedMin = isNull(expectedRange) ? null : expectedRange.min();
        return new Values(expectedMax, expectedMin, actualShare);
    }

    /**
     * Checks if the given fraction value is within the expected range based on the provided parameters.
     *
     * @param meta          The meta-information for the validation request.
     * @param actualValue   The actual fraction value to check.
     * @param expectedRange The expected range of the fraction value.
     * @param actualShare   The actual share of the fraction value within the range.
     * @return The result of the fraction value validation represented by RangeResult.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    static RangeResult isFractionValueInRange(final ValidationMeta meta, final Quota<Fraction> expectedRange,
                                              final Fraction actualValue, final _ValuedRangeResult actualShare) {

        final Values values = preChecks(meta, expectedRange, actualShare);
        if (isAnyNull(values.minimum(), values.maximum())) return null;
        RangeResult result = RangeResult.OK;
        if ((values.minimum().doubleValue() > values.actual().doubleValue()) || (values.maximum().doubleValue() < values.actual().doubleValue())) {

            result = getFractionConditionalResult(actualValue, actualShare, expectedRange.share(), meta);
        }
        return result;
    }

    /**
     * Checks if the given integer value is within the expected range based on the provided parameters.
     *
     * @param meta          The meta-information for the validation request.
     * @param actualValue   The actual integer value to check.
     * @param expectedShare The expected range of the integer value.
     * @param actualShare   The actual share of the integer value within the range.
     * @return The result of the integer value validation represented by RangeResult.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    static RangeResult isIntegerValueInRange(final ValidationMeta meta, final Quota<Integer> expectedShare,
                                             final int actualValue, final _ValuedRangeResult actualShare) {

        final Values values = preChecks(meta, expectedShare, actualShare);
        RangeResult result = RangeResult.OK;

        verifyPrecision(values.minimum(), values.actual(), S3);
        verifyPrecision(values.maximum(), values.actual(), S3);

        if (isAnyNull(values.minimum(), values.maximum())) return null;

        final _Range<Fraction> range = new FractionRange(values.minimum(), values.maximum());

        if (0.0 != range.margin(actualShare.value()).doubleValue()) {
            result = getIntegerConditionalResult(actualValue, actualShare, expectedShare.share(), meta);
        }
        return result;
    }

    /**
     * Checks if the wide share check is applicable based on the provided parameters.
     *
     * @param meta          The meta-information for validation request.
     * @param expectedShare The expected share of the value within the range.
     * @param max           The maximum value of the range.
     * @param min           The minimum value of the range.
     * @return true if the wide share check is applicable, false otherwise.
     */
    @SuppressWarnings("rawtypes")
    private static boolean isWideShareCheckApplicable(final ValidationMeta meta, final Quota expectedShare, final double max
            , final double min) {

        final boolean t = isNotNull(meta.vr().irq().colours()) && !meta.vr().irq().colours().ignore(SHRINK);
        final boolean w = isNotNull(WideRange.threshold);
        final boolean z = !isZeroSpan(expectedShare);
        final boolean a = (null != WideRange.threshold) && (max - min > WideRange.threshold.doubleValue());
        return t && w && z && a;
    }

    /**
     * Checks if the given range has no span, i.e., the minimum and maximum values are equal.
     *
     * @param theRange the range to check
     * @return true if the range has no span, false otherwise
     */
    @SuppressWarnings({"CallToSuspiciousStringMethod", "rawtypes"})
    private static boolean isZeroSpan(final Quota theRange) {

        return ts(theRange.min()).equals(ts(theRange.max()));
    }

    private static Values preChecks(final ValidationMeta meta, final Quota<?> expectedShare,
                                    final _ValuedRangeResult actualShare) {

        final Values values = getValues(expectedShare, actualShare);
        final Double max = isNull(values.maximum()) ? null : values.maximum().doubleValue();
        final Double min = isNull(values.minimum()) ? null : values.minimum().doubleValue();
        verifyWideRange(meta, expectedShare, max, min);
        return values;
    }

    @SuppressWarnings({"NumericCastThatLosesPrecision", "SameParameterValue"})
    private static void verifyPrecision(final Fraction value, final _ValuedRangeResult actual, final Severity severity) {

        if (!isAnyNull(value, actual)) if (value.doubleValue() != actual.doubleValue()) {
            final double less = FastMath.min(value.doubleValue(), actual.doubleValue());
            final double more = FastMath.max(value.doubleValue(), actual.doubleValue());
            final long multiplier = (long) FastMath.pow(10, precision());
            final double difference = more * multiplier - less * multiplier;
            if (difference < ONE)
                logEvent(severity, b(c(VALUES), ts(value), AND, ts(actual.value()), s(ARE_TOO_SIMILAR, _COLON_),
                        DIFFERENCE_IS, s(more - less)));
        }
    }

    /**
     * Verifies if the given range is wider than a specified threshold and logs an event accordingly.
     *
     * @param meta          The meta-information for validation request.
     * @param expectedShare The expected share of the value within the range.
     * @param max           The maximum value of the range.
     * @param min           The minimum value of the range.
     */
    @SuppressWarnings("rawtypes")
    private static void verifyWideRange(final ValidationMeta meta, final Quota expectedShare, final Double max,
                                        final Double min) {

        if (Ranges.logWideRange) if (!isAnyNull(max, min)) if (isWideShareCheckApplicable(meta, expectedShare, max, min))
            logEvent(S2, b(c(RANGE), expectedShare.toString(), e(Brackets.ROUND, expectedShare.percentage()), IS_WIDER_THAN,
                    ts(WideRange.threshold), THRESHOLD_DEFINED_IN, meta.subject().noun().singular(),
                    s(expectedShare.value()), IN, q(meta.vr().name())));
    }

    private record Values(Fraction maximum, Fraction minimum, _ValuedRangeResult actual) {

    }
}
