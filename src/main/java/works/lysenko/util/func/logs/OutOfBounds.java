package works.lysenko.util.func.logs;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._RangedMargin;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.apis.grid.t._Value;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.enums.RangeResult;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.LogMeta;
import works.lysenko.util.func.logs.poster.Failure;
import works.lysenko.util.grid.record.meta.ValidationMeta;
import works.lysenko.util.grid.record.rgbc.Colour;
import works.lysenko.util.prop.grid.Allowed;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.logDebug;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.data.enums.RangeResult.NOT_OK;
import static works.lysenko.util.data.enums.RangeResult.WITHIN_DYNAMIC_MARGIN;
import static works.lysenko.util.data.enums.RangeResult.WITHIN_STATIC_MARGIN;
import static works.lysenko.util.data.enums.RangedMarginType.STATIC;
import static works.lysenko.util.data.records.RangedMargin.rm;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.v;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.logs.Poster.logFailure;
import static works.lysenko.util.func.logs.trace.Data.traceable;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.R.RANGE;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Represents a class for handling out of bounds validation events.
 */
@SuppressWarnings("NestedConditionalExpression")
public record OutOfBounds() {

    /**
     * Logs a failure event related to the fraction range validation and returns the result of the range validation.
     *
     * @param actualValue the actual value of the fraction
     * @param actual      the actual fraction value
     * @param expected    the expected fraction value
     * @param meta        the validation information for the validation request
     * @return the result of the fraction range validation
     */
    public static RangeResult getFractionConditionalResult(final Fraction actualValue, final _ValuedRangeResult actual,
                                                           final _Value<?> expected, final ValidationMeta meta) {

        final String description = b(q(ts(actualValue)));
        return logOutOfBounds(actual, description, expected, meta);
    }

    /**
     * Retrieves the result of an integer-based range validation process for the given actual value,
     * expected value, and validation metadata while logging the outcome if necessary.
     *
     * @param actualValue the actual integer value subjected to validation
     * @param actual      the actual fractional value, represented as an implementation of {@link _ValuedRangeResult},
     *                    associated with the range validation
     * @param expected    the expected value, represented as an implementation of {@link _Value},
     *                    to compare against the actual value in the validation process
     * @param meta        metadata containing additional parameters and settings for the validation process
     * @return the result of the range validation, represented as a {@link RangeResult}
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    public static RangeResult getIntegerConditionalResult(final int actualValue, final _ValuedRangeResult actual, final _Value<
            ?> expected, final ValidationMeta meta) {

        final String description = meta.subject().noun().singular().equals(COLOUR) ? b(q(Colour.shortOfValue(actualValue))) :
                b(q(actualValue));
        return logOutOfBounds(actual, description, expected, meta);
    }

    @SuppressWarnings({"LocalVariableNamingConvention", "unchecked"})
    private static LogMeta getLogMeta(final Fraction actual, final _ValuedRangeResult actualFraction, final _Value<?> expected,
                                      final ValidationMeta meta) {

        final Fraction af = isNull(actualFraction) ? actual : actualFraction.value();

        final _Range<Fraction> expectedRange = ((_Range<Fraction>) expected.value());
        final double absoluteMargin = Math.abs(expectedRange.margin(af).doubleValue());
        final _RangedMargin allowedMargin = (isNull(meta.margin())) ? rm() : meta.margin();
        final double allowedAbsoluteMargin = allowedMargin.doubleValue(expectedRange, af);
        final boolean withinMargin = absoluteMargin <= allowedAbsoluteMargin;
        final Severity severity = (withinMargin) ? Severity.byCode(meta.max().ordinal() + Allowed.marginSeverityReduction) :
                meta.max();
        final LogMeta logMeta = new LogMeta(expectedRange, absoluteMargin, allowedMargin, allowedAbsoluteMargin,
                withinMargin, severity);

        Trace.log(traceable(logMeta.expectedRange(),
                af,
                logMeta.absoluteMargin(),
                logMeta.allowedMargin(),
                logMeta.allowedAbsoluteMargin(),
                logMeta.withinMargin(),
                logMeta.severity()));

        return logMeta;
    }

    private static RangeResult getRangeResult(final Fraction actual, final _ValuedRangeResult actualFraction,
                                              final String description, final _Value<?> expected, final ValidationMeta meta) {

        final RangeResult result;
        final LogMeta logMeta = getLogMeta(actual, actualFraction, expected, meta);
        final Fraction af = isNull(actualFraction) ? actual : actualFraction.value();
        final Fraction actualValue = isNull(actual) ? af : actual;
        final String realDescription = (isNull(description)) ? EMPTY : description;
        final Failure failure = new Failure(logMeta.severity(),
                meta.subject().noun().singular(),
                b(false, realDescription, IN, q(meta.vr().name())),
                v(actualValue), expected, fr(logMeta.allowedAbsoluteMargin()));
        logFailure(failure);
        result = (logMeta.absoluteMargin() == ZERO) ? NOT_OK : (logMeta.withinMargin() ?
                STATIC == logMeta.allowedMargin().type() ? WITHIN_STATIC_MARGIN : WITHIN_DYNAMIC_MARGIN : NOT_OK); // TODO:
        // EXTEND TO RELATIVE, ABSOLUTE, DYNAMIC
        logDebug(b(c(RANGE), c(RESULT), IS, result.render()));
        return result;
    }

    /**
     * Retrieves the result of a range validation process for the given actual value, description, expected value,
     * and validation metadata while logging the outcome if necessary.
     *
     * @param actual      the actual fractional value subjected to validation
     * @param description a description of the validation context or purpose
     * @param expected    the expected value for comparison in the validation process
     * @param meta        metadata containing additional validation parameters and settings
     * @return the result of the range validation, represented as a {@link RangeResult}
     */
    private static RangeResult logOutOfBounds(final _ValuedRangeResult actual, final String description,
                                              final _Value<?> expected, final ValidationMeta meta) {

        return getRangeResult(null, actual, description, expected, meta);
    }

    /**
     * Logs a range validation failure event for the provided parameters and returns the result of the validation.
     *
     * @param actual      the actual fractional value subjected to the validation
     * @param description a description of the validation context or purpose
     * @param expected    the expected value for comparison during the validation process
     * @param meta        metadata containing additional validation parameters and settings
     * @return the result of the range validation, represented as a {@link RangeResult}
     */
    public static RangeResult logOutOfBounds(final Fraction actual, final String description, final _Value<?> expected,
                                             final ValidationMeta meta) {

        return getRangeResult(actual, null, description, expected, meta);
    }
}
