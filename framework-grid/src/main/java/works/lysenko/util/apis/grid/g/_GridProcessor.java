package works.lysenko.util.apis.grid.g;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._FractionQuotas;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.grid.record.misc.IgnoreHSB;

import java.util.Map;

/**
 * Represents a collection of methods for processing matrices related to pixel values, colours, hues, and saturations.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _GridProcessor {

    /**
     * Retrieves the brightness values by rates for a given SharesOfBrightness object.
     *
     * @param brightnesses the SharesOfBrightness object for which to retrieve the brightness values
     * @return a map containing the pixel values as keys and their corresponding brightness values as fractions
     */
    Map<Fraction, _ValuedRangeResult> getBrightnessesByRates(_FractionQuotas brightnesses);

    /**
     * Retrieves the pixel colours by rates.
     *
     * @return a LinkedHashMap containing the pixel values as keys and their corresponding rates as values
     */
    @SuppressWarnings("unused")
    Map<Integer, _ValuedRangeResult> getColoursByRates();

    /**
     * Retrieves the pixel colours by rates.
     *
     * @param threshold a double value representing rate threshold of pixels to be ignored
     * @return a map containing the pixel values as keys and their corresponding rates as values 0.0 ... 1.0
     */
    Map<Integer, _ValuedRangeResult> getColoursByRates(Fraction threshold);

    /**
     * Retrieves the pixel colours by rates.
     *
     * @param threshold a double value representing rate threshold of pixels to be ignored
     * @param ignoreHSB an instance of IgnoreHSB representing the range to ignore for HSB values
     * @return a map containing the pixel values as keys and their corresponding rates as values 0.0 ... 1.0
     */
    Map<Integer, _ValuedRangeResult> getColoursByRates(final Fraction threshold, final IgnoreHSB ignoreHSB);

    /**
     * Retrieves the hues by rates for a given SharesOfHue object.
     *
     * @param hues the SharesOfHue object for which to retrieve the hues by rates
     * @return a map containing the pixel values as keys and their corresponding hues as fractions
     */
    Map<Fraction, _ValuedRangeResult> getHuesByRates(_FractionQuotas hues);

    /**
     * Retrieves the saturations by rates for a given SharesOfSaturation object.
     *
     * @param saturations the SharesOfSaturation object for which to retrieve the saturation by rates
     * @return a map containing the pixel values as keys and their corresponding saturation as fractions
     */
    Map<Fraction, _ValuedRangeResult> getSaturationsByRates(_FractionQuotas saturations);
}
