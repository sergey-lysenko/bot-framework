package works.lysenko.util.grid.record.misc;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.records.RangedMargin;

/**
 * Represents limits with a border fraction and a ranged margin.
 *
 * @param border the border fraction limit
 * @param margin the ranged margin object
 */
public record Limits(Fraction border, RangedMargin margin) {

    /**
     * Returns the cutoff border fraction.
     *
     * @return the border fraction
     */
    public Fraction cutoff() {

        return border;
    }

    /**
     * Creates a new Limits object with the specified border and allowed deviation.
     *
     * @param border the fraction representing the border for the margins
     * @param margin the allowed deviation for the margins, which can be relative or absolute
     * @return a new Limits object initialised with the given border and allowed deviation
     */
    public static Limits of(final Fraction border, final RangedMargin margin) {

        return new Limits(border, margin);
    }

    /**
     * Creates a new "zero" instance of the Limits class with default values for border and margin.
     * The border is initialised to Fraction.ZERO, and the margin is set to null.
     *
     * @return a new instance of Limits with default border and margin values
     */
    public static Limits of() {

        return new Limits(Fraction.ZERO, null);
    }
}
