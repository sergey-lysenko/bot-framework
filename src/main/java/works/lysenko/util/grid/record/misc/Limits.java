package works.lysenko.util.grid.record.misc;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.records.RangedMargin;

/**
 * Represents a class that represents an ignore range for HSB (SharesOfHue, SharesOfSaturation, SharesOfBrightness) values.
 */
public record Limits(Fraction border, RangedMargin margin) {

    /**
     * Creates a new Margins object with the specified border and allowed deviation.
     *
     * @param border the fraction representing the border for the margins
     * @param margin the allowed deviation for the margins, which can be relative or absolute
     * @return a new Margins object initialised with the given border and allowed deviation
     */
    public static Limits of(final Fraction border, final RangedMargin margin) {

        return new Limits(border, margin);
    }

    /**
     * Creates a new "zero" instance of the Margins class with default values for border and margin.
     * The border is initialised to Fraction.ZERO, and the margin is set to
     * a RelativeOrAbsoluteFraction object with a zero fraction and RelativeOrAbsolute.ABSOLUTE.
     *
     * @return a new instance of Margins with default border and margin values
     */
    public static Limits of() {

        return new Limits(Fraction.ZERO, null);
    }
}
