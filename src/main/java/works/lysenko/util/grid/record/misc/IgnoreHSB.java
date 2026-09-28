package works.lysenko.util.grid.record.misc;

import works.lysenko.util.data.range.FractionRange;

/**
 * Represents a class that represents an ignore range for HSB (SharesOfHue, SharesOfSaturation, SharesOfBrightness) values.
 */
public record IgnoreHSB(FractionRange hue, FractionRange saturation, FractionRange brightness) {

    /**
     * Creates an instance of IgnoreHSB with all HSB ranges set to null.
     *
     * @return a new IgnoreHSB instance with null HSB ranges
     */
    public static IgnoreHSB of() {

        return new IgnoreHSB(null, null, null);
    }

    /**
     * Creates an instance of IgnoreHSB with specified HSB (SharesOfHue, SharesOfSaturation, SharesOfBrightness) ranges.
     *
     * @param hue        the range for the hue component
     * @param saturation the range for the saturation component
     * @param brightness the range for the brightness component
     * @return a new IgnoreHSB instance with the specified hue, saturation, and brightness ranges
     */
    public static IgnoreHSB of(final FractionRange hue, final FractionRange saturation, final FractionRange brightness) {

        return new IgnoreHSB(hue, saturation, brightness);
    }
}
