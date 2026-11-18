package works.lysenko.util.func;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.g._Grid;
import works.lysenko.util.apis.grid.q._FractionQuotas;
import works.lysenko.util.data.enums.Brackets;
import works.lysenko.util.data.range.FractionRange;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.prop.grid.Validation;

import static java.util.Objects.isNull;
import static org.apache.commons.math3.util.FastMath.ceil;
import static works.lysenko.Base.*;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.word.A.*;
import static works.lysenko.util.lang.word.B.BRIGHTNESS;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.D.DEBUG;
import static works.lysenko.util.lang.word.D.DISTRIBUTION;
import static works.lysenko.util.lang.word.E.EXACT;
import static works.lysenko.util.lang.word.F.FINAL;
import static works.lysenko.util.lang.word.H.HUE;
import static works.lysenko.util.lang.word.I.IGNORE;
import static works.lysenko.util.lang.word.I.IGNORED;
import static works.lysenko.util.lang.word.O.OCCURRENCE;
import static works.lysenko.util.lang.word.P.PIXELS;
import static works.lysenko.util.lang.word.P.PROPERTY;
import static works.lysenko.util.lang.word.R.REQUEST;
import static works.lysenko.util.lang.word.S.SATURATION;
import static works.lysenko.util.lang.word.T.THRESHOLD;
import static works.lysenko.util.lang.word.V.VALIDATION;
import static works.lysenko.util.spec.Numbers.SEVEN;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * Simple logger class for logging grid data and related statistics for validation purposes.
 * Contains methods to log grid information, including colours distribution, pixels threshold, and HSB data.
 */
public record Logs() {

    // TODO: move logging logic to respective objects ?

    /**
     * Logs the grid data and related statistics for validation purposes.
     *
     * @param grid the pixel grid to log
     * @param vr   the validation request containing threshold information
     * @return true if the grid was logged successfully
     */
    @SuppressWarnings({"NumericCastThatLosesPrecision", "BooleanMethodNameMustStartWithQuestion",
            "MethodWithMultipleReturnPoints"})
    public static boolean logGrid(final _Grid grid, final Request vr) {

        section(b(c(GRID), DEBUG, INFO));
        if (isNull(grid)) return failAndFalse(b(c(GRID), IS, NULL));
        if (isNull(vr)) return failAndFalse(b(c(VALIDATION), REQUEST, IS, NULL));

        final Fraction threshold = (null == vr.irq().colours()) ? fr(Validation.defaultColoursBorder) :
                vr.irq().colours().border();
        final int pixels = (int) ceil((grid.getSamplePoints().size() * threshold.doubleValue()));
        logTrace(b(b(c(COLOURS), s(OCCURRENCE, _COLON_), grid.render().pixelsCountByColor())));
        logDebug(b(b(c(COLOURS), s(AMOUNT, _COLON_), yb(grid.calculator().countPixelsByColor().size()))));
        logDebug(b(b(c(WITH), yb(ts(threshold)), OCCURRENCE, s(THRESHOLD, _COMMA_), COLOURS, APPEARING, IN, LESS, THAN,
                yb(pixels)), PIXELS, WILL, BE, IGNORED, IN, COLOURS, DISTRIBUTION));
        final int accounted = grid.processor().getColoursByRates(threshold).size();
        logDebug(b(b(c(COLOURS), s(ACCOUNTED, _COLON_), yb(accounted))));
        logDebug(b(c(ARRANGED), COLOURS, s(DISTRIBUTION, _COLON_), grid.render().byShares_Colors(threshold)));

        logGridHSB(grid, vr);
        logGridColourIgnore(grid, vr, accounted, threshold);

        return true;
    }

    private static void logGridColourIgnore(final _Grid grid, final Request vr, final int accounted,
                                            final Fraction threshold) {

        if (isNotNull(vr.irq().colours())) {
            final FractionRange hue = vr.irq().colours().ignoreHSB().hue();
            final FractionRange saturation = vr.irq().colours().ignoreHSB().saturation();
            final FractionRange brightness = vr.irq().colours().ignoreHSB().brightness();
            if (isNotNull(hue)) logIgnored(HUE, hue);
            if (isNotNull(saturation)) logIgnored(SATURATION, saturation);
            if (isNotNull(brightness)) logIgnored(BRIGHTNESS, brightness);
            final int ignored =
                    accounted - grid.processor().getColoursByRates(threshold, vr.irq().colours().ignoreHSB()).size();
            if (0 < ignored)
                logDebug(b(yb(s1(ignored, COLOUR)), s(IGNORED, _COMMA_), FINAL, LIST, OF, ACTUAL, s1(accounted - ignored,
                        COLOUR), IS, grid.render().byShares_Colours(threshold, vr.irq().colours().ignoreHSB())));
            logDebug(b(c(EXACT), COLOURS, PROPERTY, VALUE, FOR, THIS, s(GRID, _COLON_),
                    yb(grid.render().colorsPropertyValue(threshold, vr.irq().colours().ignoreHSB()))));
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private static void logGridHSB(final _Grid grid, final Request vr) {

        final int def = i(Validation.fences);

        final _FractionQuotas hue = vr.irq().hue();
        final _FractionQuotas saturation = vr.irq().saturation();
        final _FractionQuotas brightness = vr.irq().brightness();

        final int hueQ = isNull(hue) ? def : hue.fences();
        final int saturationQ = isNull(saturation) ? def : saturation.fences();
        final int brightnessQ = isNull(brightness) ? def : brightness.fences();

        final String hueN = s(e(Brackets.ROUND, b(s(grid.processor().getHuesByRates(hue).size()), OF, s(hueQ + 1))), _COLON_);
        final String saturationN = s(e(Brackets.ROUND, b(s(grid.processor().getSaturationsByRates(saturation).size()), OF,
                s(saturationQ + 1))), _COLON_);
        final String brightnessN = s(e(Brackets.ROUND, b(s(grid.processor().getBrightnessesByRates(brightness).size()), OF,
                s(brightnessQ + 1))), _COLON_);

        final String hueS = grid.render().byShares_Hues(hue, hueQ);
        final String saturationS = grid.render().byShares_Saturations(saturation, saturationQ);
        final String brightnessS = grid.render().byShares_Brightnesses(brightness, brightnessQ);

        final String hueC = isNull(hue) || isNull(hue.threshold()) ? StringUtils.EMPTY : b(WITH, yb(ts(hue.threshold())),
                THRESHOLD);
        final String saturationC = isNull(saturation) || isNull(saturation.threshold()) ? StringUtils.EMPTY : b(WITH,
                yb(ts(saturation.threshold())), THRESHOLD);
        final String brightnessC = isNull(brightness) || isNull(brightness.threshold()) ? StringUtils.EMPTY : b(WITH,
                yb(ts(brightness.threshold())), THRESHOLD);

        logDebug(b(s(c(HUE), s(_SPACE_).repeat(SEVEN)), hueN, hueS, hueC));
        logDebug(b(c(SATURATION), saturationN, saturationS, saturationC));
        logDebug(b(c(BRIGHTNESS), brightnessN, brightnessS, brightnessC));
    }

    private static void logIgnored(final String s, final FractionRange fractionRange) {

        logDebug(b(c(s), TO, s(IGNORE, _COLON_), fractionRange.toString()));
    }
}