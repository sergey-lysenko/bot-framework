package works.lysenko.util.grid.reader;

import works.lysenko.util.apis.data._ScaledProperties;
import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.q._FractionQuotas;
import works.lysenko.util.apis.grid.r._ImageRequirements;
import works.lysenko.util.grid.expected.QuotasHSB;
import works.lysenko.util.prop.grid.Validation;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.grid.Fields.*;
import static works.lysenko.util.lang.word.B.BRIGHTNESS;
import static works.lysenko.util.lang.word.H.HUE;
import static works.lysenko.util.lang.word.S.SATURATION;


/**
 * Represents the HSB colour model and provides methods for retrieving brightness, hue, and saturation values.
 */
public record HSB() {

    /**
     * Retrieves the brightness shares based on the provided grid and predefined image requirements.
     *
     * @param grid       The grid containing properties for calculations.
     * @param predefined The predefined ImageRequirements object.
     * @return SharesOfBrightness object representing the brightness shares.
     */
    public static _FractionQuotas getBrightness(final _GridProperties grid, final _ImageRequirements predefined) {

        return getSharesOf((_ScaledProperties) grid, predefined, isNotNull(predefined) ? predefined.brightness() : null,
                BRIGHTNESS, BDF, BDT);
    }

    /**
     * Retrieves the hue shares based on the provided grid and predefined image requirements.
     *
     * @param grid       The grid containing properties for calculations.
     * @param predefined The predefined ImageRequirements object.
     * @return SharesOfHue object representing the hue shares.
     */
    public static _FractionQuotas getHue(final _GridProperties grid, final _ImageRequirements predefined) {

        return getSharesOf((_ScaledProperties) grid, predefined, isNotNull(predefined) ? predefined.hue() : null,
                HUE, HDF, HDT);
    }

    /**
     * Retrieves the saturation shares based on the provided grid and predefined image requirements.
     *
     * @param grid       The grid containing properties for calculations.
     * @param predefined The predefined ImageRequirements object.
     * @return SharesOfSaturation object representing the saturation shares.
     */
    public static _FractionQuotas getSaturation(final _GridProperties grid, final _ImageRequirements predefined) {

        return getSharesOf((_ScaledProperties) grid, predefined, isNotNull(predefined) ? predefined.saturation() : null,
                SATURATION, SDF, SDT);
    }

    /**
     * Retrieves the Hue, Saturation, or Brightness shares based on the provided grid, predefined image requirements,
     * and share values. The method checks if the predefined ImageRequirements or share values are null
     * and returns null if both or either is null. It then checks if the grid contains the specified key, and if it does,
     * it creates a new SharesOfHue object with the values retrieved from the grid based on the provided key, fences, and
     * threshold.
     *
     * @param grid       The grid containing properties for calculations.
     * @param predefined The predefined ImageRequirements object.
     * @param quotas     The current shares of HSB values.
     * @param key        The key to retrieve values from the grid.
     * @param fences     The fences value for quantisation.
     * @param threshold  The threshold value used in calculations.
     * @return The updated shares of HSB values based on the provided grid and predefined image requirements.
     */
    @SuppressWarnings("NestedConditionalExpression")
    private static _FractionQuotas getSharesOf(final _ScaledProperties grid, final _ImageRequirements predefined,
                                               final _FractionQuotas quotas, final String key, final String fences,
                                               final String threshold) {

        final String v = grid.getKV(key, true).v();
        return (isAnyNull(predefined, quotas)) ? (isNull(v)) ? null : new QuotasHSB(v, key,
                grid.getKV(s(fences), Validation.fences).v(),
                grid.getKV(s(threshold), Validation.hsbThreshold).v()) : quotas;
    }
}
