package works.lysenko.util.func.grid.hsb;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.g._Grid;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.data.records.Noun;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.func.grid.ranger.OfFractions;
import works.lysenko.util.grid.expected.QuotasHSB;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.meta.Method;
import works.lysenko.util.grid.record.meta.Subject;
import works.lysenko.util.grid.record.meta.ValidationMeta;
import works.lysenko.util.grid.record.rash.Binner;
import works.lysenko.util.grid.record.rash.SharesData;

import java.util.Map;

import static works.lysenko.util.chrs.__.ES;
import static works.lysenko.util.data.enums.Ansi.byBrightness;
import static works.lysenko.util.data.enums.Ansi.byHue;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.d;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.func.grid.Renderers.r;
import static works.lysenko.util.func.type.Strings.stripExcessZeros;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.word.B.BRIGHTNESS;
import static works.lysenko.util.lang.word.H.HUE;
import static works.lysenko.util.lang.word.S.SATURATION;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Numbers.SEVEN;
import static works.lysenko.util.spec.Numbers.SIX;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Class containing routines for visual representation and validation of data based on brightness, hue, and saturation.
 */
public record Routines() {

    /**
     * Determines the visual representation of data based on the brightness and range.
     *
     * @param data  the SharesData object containing the value, index, maximum value, and width
     * @param range the Quota object representing the range
     * @return the visual representation of the data based on the brightness and range
     */
    @SuppressWarnings("unchecked")
    private static String brightnessDataVisual(final SharesData<?> data, final _Quota<?> range, final Binner binner) {

        final Result r = getMinMax((SharesData<? extends Fraction>) data, (Quota<Fraction>) range);
        final String dataPoint = byBrightness(DATA_POINT, (Fraction) data.value());
        return (data.i() >= r.min() && data.i() <= r.max()) ? dataPoint : s(_BULLT_);
    }

    /**
     * Determines the minimum and maximum indices based on the given SharesData and Quota range.
     *
     * @param data  the SharesData object containing the value, index, maximum value, and width
     * @param range the Quota object representing the range
     * @return a Result object with the calculated maximum and minimum indices
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static Result getMinMax(final SharesData<? extends Fraction> data, final Quota<Fraction> range) {

        final Fraction maxR = range.max();
        final Fraction minR = range.min();
        final int max = (int) (data.width() * (maxR.doubleValue() / data.max()));
        final int min = (int) (data.width() * (minR.doubleValue() / data.max()));
        return new Result(max, min);
    }

    /**
     * Determines the visual representation of data based on the hue and range.
     *
     * @param data  the SharesData object containing the value, index, maximum, and width
     * @param range the Quota object representing the range
     * @return the visual representation of the data based on the hue and range
     */
    @SuppressWarnings("unchecked")
    private static String hueDataVisual(final SharesData<?> data, final _Quota<?> range, final Binner binner) {

        final Result r = getMinMax((SharesData<? extends Fraction>) data, (Quota<Fraction>) range);
        final String dataPoint = byHue(DATA_POINT, (Fraction) data.value());
        return (data.i() >= r.min() && data.i() <= r.max()) ? dataPoint : s(_BULLT_);
    }

    /**
     * Determines if the brightness values are within the acceptable range.
     *
     * @param vr   the Request object containing validation parameters
     * @param grid the _Grid object to process brightness values
     * @param max  the Severity object defining the maximum severity level
     * @return true if brightness values are within the acceptable range, false otherwise
     */
    @SuppressWarnings("unchecked")
    public static boolean isBrightnessOk(final Request vr, final _Grid grid, final Severity max) {

        final Renderers renderers = r(Routines::noDataIgnore, Routines::brightnessDataVisual, Routines::plainDataLegend);
        final Map<Fraction, _ValuedRangeResult> actual = grid.processor().getBrightnessesByRates(vr.irq().brightness());
        final QuotasHSB expected = (QuotasHSB) vr.irq().brightness();
        final Noun noun = new Noun(BRIGHTNESS, s(BRIGHTNESS, ES));
        final Subject subject = new Subject(noun, Method.OPTIONAL);
        final ValidationMeta meta = new ValidationMeta(max, subject, null, vr.irq().brightness().fences(), vr);
        return new OfFractions(meta, actual, expected, null, false, false, renderers, max).validate().isPassed();
    }

    /**
     * Determines whether the hue values are within the acceptable range.
     *
     * @param vr   the Request object containing validation parameters
     * @param grid the _Grid object to process hue values
     * @param max  the Severity object defining the maximum severity level
     * @return true if hue values are within the acceptable range, false otherwise
     */
    @SuppressWarnings("unchecked")
    public static boolean isHueOk(final Request vr, final _Grid grid, final Severity max) {

        final Renderers renderers = r(Routines::noDataIgnore, Routines::hueDataVisual, Routines::plainDataLegend);
        final Map<Fraction, _ValuedRangeResult> actual = grid.processor().getHuesByRates(vr.irq().hue());
        final QuotasHSB expected = (QuotasHSB) vr.irq().hue();
        final Noun noun = new Noun(HUE, s(HUE, S));
        final Subject subject = new Subject(noun, Method.OPTIONAL);
        final ValidationMeta meta = new ValidationMeta(max, subject, null, vr.irq().hue().fences(), vr);
        return new OfFractions(meta, actual, expected, null, false, false, renderers, max).validate().isPassed();
    }

    /**
     * Determines whether the saturation values are within the acceptable range.
     *
     * @param vr   the Request object containing validation parameters
     * @param grid the _Grid object to process saturation values
     * @param max  the Severity object defining the maximum severity level
     * @return true if saturation values are within the acceptable range, false otherwise
     */
    @SuppressWarnings("unchecked")
    public static boolean isSaturationOk(final Request vr, final _Grid grid, final Severity max) {

        final Renderers renderers = r(Routines::noDataIgnore, Routines::plainDataVisual, Routines::plainDataLegend);
        final Map<Fraction, _ValuedRangeResult> actual = grid.processor().getSaturationsByRates(vr.irq().saturation());
        final QuotasHSB expected = (QuotasHSB) vr.irq().saturation();
        final Noun noun = new Noun(SATURATION, s(SATURATION, S));
        final Subject subject = new Subject(noun, Method.OPTIONAL);
        final ValidationMeta meta = new ValidationMeta(max, subject, null, vr.irq().saturation().fences(), vr);
        return new OfFractions(meta, actual, expected, null, false, false, renderers, max).validate().isPassed();
    }

    /**
     * Determines whether to ignore validation errors when no data is present for a given Quota range.
     *
     * @param vr    the Request object containing validation parameters
     * @param share the Quota range to validate against
     * @return Boolean indicating whether to ignore validation errors for no data (always returns false)
     */
    @SuppressWarnings({"rawtypes", "BooleanMethodNameMustStartWithQuestion"})
    private static Boolean noDataIgnore(final Request vr, final _Quota share) {

        return false;
    }

    /**
     * Generates a plain data legend based on the provided Quota range.
     *
     * @param range the Quota object representing the range
     * @return the generated plain data legend as a string
     */
    private static String plainDataLegend(final _Quota<?> range) {

        final double min = range.min().doubleValue();
        final double max = range.max().doubleValue();
        return (b(L0,
                s(range.value()),
                ts(range.min()),
                ts(range.max()),
                stripExcessZeros(d(min, SEVEN), SIX),
                stripExcessZeros(d(max, SEVEN), SIX),
                percentString(max - min, 1.0)));
    }

    /**
     * Visualizes the plain data based on the given SharesData and Quota range.
     *
     * @param data  the SharesData object containing value, index, maximum, and width
     * @param range the Quota object representing the range
     * @return the visual representation of the plain data
     */
    @SuppressWarnings("NestedConditionalExpression")
    private static String plainDataVisual(final SharesData<?> data, final _Quota<?> range, final Binner binner) {

        final String dataPoint = s(DATA_POINT);
        return (data.i() >= binner.min() && data.i() <= binner.max()) ? dataPoint :
                (data.i() >= binner.minS() && data.i() <= binner.maxS()) ? s(VRT_BAR) : s(_BULLT_);
    }

    private record Result(int max, int min) {

    }
}