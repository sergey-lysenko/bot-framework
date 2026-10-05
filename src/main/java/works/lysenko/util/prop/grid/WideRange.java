package works.lysenko.util.prop.grid;

import org.apache.commons.math3.fraction.Fraction;

import static works.lysenko.util.spec.PropEnum._GRID_WIDE_RANGE_THRESHOLD;

@SuppressWarnings("MissingJavadoc")
public record WideRange() {

    public static final Fraction threshold = _GRID_WIDE_RANGE_THRESHOLD.get();
}
