package works.lysenko.util.apis.grid.g;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._FractionQuotas;
import works.lysenko.util.grid.record.misc.IgnoreHSB;

import java.util.List;

/**
 * An interface providing methods to retrieve pixel grid information as string representations.
 * Implementing classes should provide concrete implementations for these methods.
 */
@SuppressWarnings({"unused", "InterfaceWithOnlyOneDirectInheritor"})
public interface _GridToString {

    /**
     * Retrieves the brightnesses by rational rates as a string representation.
     *
     * @param brightness  the SharesOfBrightness object representing the brightness range for colour values
     * @param brightnessQ an integer representing the fences value for brightness range
     * @return a string representing the brightnesses by rational rates
     */
    String byShares_Brightnesses(_FractionQuotas brightness, int brightnessQ);

    /**
     * Retrieves the pixel colours by rational rates as a string representation.
     *
     * @param threshold a double value representing the rate threshold of pixels to be ignored
     * @return a string representing the pixel colours by rational rates
     */
    String byShares_Colors(Fraction threshold);

    /**
     * Retrieves the pixel colours by rational rates as a string representation.
     *
     * @param threshold a Double value representing the rate threshold of pixels to be ignored
     * @param ignoreHSB an object containing minimum and maximum values for SharesOfHue, SharesOfSaturation, and
     *                  SharesOfBrightness to be ignored
     * @return a string representing the pixel colours by rational rates
     */
    String byShares_Colours(Fraction threshold, IgnoreHSB ignoreHSB);

    /**
     * Retrieves the hues by rational rates as a string representation based on the provided SharesOfHue object and hue
     * fences value.
     *
     * @param hue  The SharesOfHue object representing the hue range for colour values
     * @param hueQ An integer representing the fences value for hue range
     * @return A1 string representing the hues by rational rates
     */
    String byShares_Hues(_FractionQuotas hue, int hueQ);

    /**
     * Retrieves the saturations by rational rates as a string representation based on the provided SharesOfSaturation
     * object and saturation fences value.
     *
     * @param saturation  The SharesOfSaturation object representing the saturation range for colour values
     * @param saturationQ An integer representing the fences value for saturation range
     * @return A1 string representing the saturations by rational rates
     */
    String byShares_Saturations(_FractionQuotas saturation, int saturationQ);

    /**
     * Retrieves the pixel colours by rational rates as a string representation.
     *
     * @param threshold a double value representing the rate threshold of pixels to be ignored
     * @param ignoreHSB an object containing minimum and maximum values for hue, saturation, and brightness
     * @return a string representing the pixel colours by rational rates
     */
    String colorsPropertyValue(Fraction threshold, IgnoreHSB ignoreHSB);

    /**
     * Retrieves the pixel count by colour as a string representation.
     *
     * @return a string representing the pixel count by colour
     */
    String pixelsCountByColor();

    /**
     * Retrieves a list of strings representing the pixel grid.
     *
     * @return a list of strings representing the pixel grid
     */
    List<String> toListString();
}
