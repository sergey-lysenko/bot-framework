package works.lysenko.util.prop.grid;

import org.apache.commons.math3.fraction.Fraction;

import static works.lysenko.util.prop.PropEnum._GRID_WIDE_RANGE_THRESHOLD;

/**
 * Threshold configuration for wide range detection.
 */
public record WideRange() {

    /**
     * Range width threshold above which a range is considered a wide range.
     */
    public static final Fraction threshold = _GRID_WIDE_RANGE_THRESHOLD.get();
}
