package works.lysenko.util.func.grid.colours;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._RangedMargin;
import works.lysenko.util.apis.grid.g._Grid;
import works.lysenko.util.apis.grid.q._ColoursQuotas;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.enums.ColoursValidationResult;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.range.FractionRange;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.records.Noun;
import works.lysenko.util.data.records.RGB24;
import works.lysenko.util.data.records.same.Pair;
import works.lysenko.util.data.type.list.RangerResults;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.func.grid.ranger.OfIntegers;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.meta.Method;
import works.lysenko.util.grid.record.meta.Subject;
import works.lysenko.util.grid.record.meta.ValidationMeta;
import works.lysenko.util.grid.record.rash.Binner;
import works.lysenko.util.grid.record.rash.SharesData;
import works.lysenko.util.grid.record.rgbc.HSB;

import java.util.Map;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.BE;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.THE;
import static works.lysenko.util.chrs.____.LESS;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.chrs.____.THEN;
import static works.lysenko.util.data.enums.Ansi.byRGB;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.ColoursIgnore.ORDER;
import static works.lysenko.util.data.enums.ColoursIgnore.OTHER;
import static works.lysenko.util.data.enums.ColoursValidationResult.OK;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.records.same.Pair.p;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.d;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.func.grid.Renderers.r;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.Strings.stripExcessZeros;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.grid.record.rgbc.Colour.shortOfValue;
import static works.lysenko.util.lang.U.UNABLE_TO_TRANSFORM___;
import static works.lysenko.util.lang.word.C.CANNOT;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.D.DEFINED;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.M.MINIMUM;
import static works.lysenko.util.lang.word.Q.QUOTAS;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.lang.word.S.SHARE;
import static works.lysenko.util.lang.word.T.THRESHOLD;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Numbers.*;
import static works.lysenko.util.spec.Symbols.DATA_POINT;
import static works.lysenko.util.spec.Symbols.VRT_BAR;
import static works.lysenko.util.spec.Symbols._BULLT_;

/**
 * The ColoursRenderers class is responsible for validating and processing colour data in a grid.
 */
@SuppressWarnings({"NonBooleanMethodNameMayNotStartWithQuestion", "MethodWithMultipleReturnPoints",
        "BooleanMethodNameMustStartWithQuestion"})
public record Routines() {

    private static final String B1 = b(c(EXPECTED), MINIMUM, SHARE);
    private static final String B2 = b(IS, LESS, THEN, DEFINED, THRESHOLD);
    private static final String E1 = b(c(THE), EXPECTED, QUOTAS, CANNOT, BE, NULL);

    /**
     * Determines whether the colours in a given grid are valid, according to the expected range of colours.
     *
     * @param vr   the validation request object
     * @param grid the grid object containing the actual colours data
     * @param max  the maximum severity level for validation
     * @return the ColoursValidationResult indicating the validation result
     */
    @SuppressWarnings("UseOfConcreteClass")
    public static ColoursValidationResult areColoursOk(final Request vr, final _Grid grid, final Severity max) {

        // Mapping
        final boolean ignoreOrder = vr.irq().colours().ignore(ORDER);
        final boolean ignoreOther = vr.irq().colours().ignore(OTHER);
        final Map<Integer, _ValuedRangeResult> actual = grid.processor().getColoursByRates(vr.irq().colours().border(),
                vr.irq().colours().ignoreHSB());
        final _ColoursQuotas expected = vr.irq().colours();
        final IntegerRange amount = vr.irq().colours().amount();
        final _RangedMargin margin = vr.irq().colours().margin();

        // Processing
        final ColoursValidationResult result = getColoursResult(vr, max, actual, expected, margin, ignoreOrder, amount,
                ignoreOther);
        logDebug(b(c(COLOURS), RESULT, IS, result.render()));
        return result;
    }

    /**
     * Evaluates the validity of colours based on the provided metadata, actual values, expected quotas, and other parameters.
     *
     * @param meta        the metadata associated with the validation process, including severity and context
     * @param actual      a map containing the actual colour fractions, keyed by their respective identifiers
     * @param expected    the expected quotas defining the valid ranges of colours
     * @param ignoreOrder a flag indicating whether the order of colours should be ignored during validation
     * @param amount      the acceptable range for the number of colours
     * @param ignoreOther a flag indicating whether to ignore colours outside the defined expected range
     * @return a ColoursValidationResult object that represents the outcome of the colour validation process
     */
    @SuppressWarnings({"MethodWithTooManyParameters", "MethodWithMultipleReturnPoints", "unchecked"})
    private static ColoursValidationResult areColoursOk(final ValidationMeta meta,
                                                        final Map<Integer, _ValuedRangeResult> actual
            , final _ColoursQuotas expected, final boolean ignoreOrder, final IntegerRange amount, final boolean ignoreOther) {

        final Fraction threshold = meta.vr().irq().colours().border();
        verifyThreshold((_Quotas<Integer>) expected, threshold);
        final Renderers renderers = r(Routines::colorDataIgnore, Routines::colorDataVisual, Routines::colourDataLegend);
        final RangerResults result = new OfIntegers(meta, actual, (_Quotas<Integer>) expected, amount, ignoreOrder,
                ignoreOther, renderers, S0).validate();
        if (result.isPassed()) return result.basedOn();
        if (!result.isPassed()) return ColoursValidationResult.RANGER_FAILED;
        logEvent(S0, UNABLE_TO_TRANSFORM___);
        return null;
    }

    /**
     * Determines whether to ignore a colour based on its HSB (SharesOfHue, SharesOfSaturation, SharesOfBrightness) values.
     *
     * @param vr     the validation request object
     * @param colour the colour value to analyse
     * @return true if the colour should be ignored, false otherwise
     */
    @SuppressWarnings("RedundantIfStatement")
    private static Boolean colorDataIgnore(final Request vr, final _Quota<?> colour) {

        final int colorCode = (int) colour.value();
        final HSB thisColorHSB = HSB.convertRGBtoHSB(RGB24.rgb24(colorCode));
        final FractionRange hue = vr.irq().colours().ignoreHSB().hue();
        final FractionRange saturation = vr.irq().colours().ignoreHSB().saturation();
        final FractionRange brightness = vr.irq().colours().ignoreHSB().brightness();
        if (isNotNull(thisColorHSB)) {
            if (isNotNull(hue) && hue.includes(thisColorHSB.hue())) return true;
            if (isNotNull(saturation) && saturation.includes(thisColorHSB.saturation())) return true;
            if (isNotNull(brightness) && brightness.includes(thisColorHSB.brightness())) return true;
        }
        return false;
    }

    /**
     * Determines the colour of a data point based on the given data and range.
     *
     * @param data  The data object containing the value, i, max, and width.
     * @param range The range object containing the min and max fraction values.
     * @return The ANSI colour code representing the colour of the data point.
     */
    @SuppressWarnings("NestedConditionalExpression")
    private static String colorDataVisual(final SharesData<?> data, final _Quota<?> range, final Binner binner) {

        final String dataPoint = byRGB(DATA_POINT, RGB24.rgb24((Integer) data.value()));
        return (data.i() >= binner.min() && data.i() <= binner.max()) ? dataPoint :
                (data.i() >= binner.minS() && data.i() <= binner.maxS()) ? s(VRT_BAR) : s(_BULLT_);
    }

    /**
     * Determines the colour legend for a given range of share values.
     *
     * @param range The range object containing the min and max fraction values.
     * @return The colour legend represented as a string.
     */
    private static String colourDataLegend(final _Quota<?> range) {

        final double min = range.min().doubleValue();
        final double max = range.max().doubleValue();
        final Pair<String> fraction = p(ts(range.min()), ts(range.max()));
        final Pair<String> decimal = p(stripExcessZeros(d(min, SEVEN), SIX), stripExcessZeros(d(max, SEVEN), SIX));
        final String rngF = (min == max) ? yb(fraction.any()) : b(L0, fraction.l(), fraction.right());
        final String rngD = (min == max) ? yb(decimal.any()) : b(L0, decimal.left(), decimal.right());
        return (b(L0, shortOfValue((int) range.value()), rngF, rngD, percentString(max - min, ONE)));
    }

    /**
     * Evaluates the result of colours validation based on the provided parameters.
     *
     * @param vr          the validation request object containing validation context and metadata
     * @param max         the maximum severity level for validation
     * @param actual      a map containing actual colour fractions, keyed by their identifiers
     * @param expected    the quotas representing the expected ranges of colours
     * @param margin      the ranged margin to consider during validation
     * @param ignoreOrder a flag indicating whether the order of colours should be ignored during validation
     * @param amount      the acceptable range for the number of colours
     * @param ignoreOther a flag indicating whether colours beyond the defined range should be ignored
     * @return the ColoursValidationResult indicating whether the validation passed, failed, or fell within defined margins
     */
    private static ColoursValidationResult getColoursResult(final Request vr, final Severity max, final Map<Integer,
                                                                    _ValuedRangeResult> actual,
                                                            final _ColoursQuotas expected, final _RangedMargin margin,
                                                            final boolean ignoreOrder,
                                                            final IntegerRange amount, final boolean ignoreOther) {

        final Noun noun = new Noun(COLOUR, COLOURS);
        final Subject subject = new Subject(noun, Method.COMPULSIVE);
        final ValidationMeta meta = new ValidationMeta(max, subject, margin, ONE, vr);
        final ColoursValidationResult result = areColoursOk(meta, actual, expected, ignoreOrder, amount, ignoreOther);
        if (OK == result) new Processor(actual, (_Quotas<?>) expected).processPositiveResult(meta.vr());
        else new Processor(actual, (_Quotas<?>) expected).processNegativeResult(meta.vr());
        return result;
    }

    /**
     * Verifies that each minimum value in the expected quotas is greater than or equal to the defined threshold.
     * If a minimum share value is greater than zero but less than the threshold, an event is logged.
     *
     * @param expected  The collection of quotas to be validated.
     * @param threshold The threshold that each minimum value in the quotas must meet or exceed.
     */
    private static void verifyThreshold(final _Quotas<Integer> expected, final Fraction threshold) {

        if (isNull(expected.get()))
            throw new IllegalArgumentException(E1);
        for (final _Quota<?> share : expected.get()) {
            if (share.min().doubleValue() > ZERO && share.min().doubleValue() < threshold.doubleValue())
                logEvent(S2, b(B1, ts(share.min()), OF, shortOfValue((int) share.value()), B2, ts(threshold)));
        }
    }
}
