package works.lysenko.util.grid.record.gsrc;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.data.enums.Shape;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.enums.Shape.ELLIPCE;
import static works.lysenko.util.data.enums.Shape.RECTANGLE;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.spec.Symbols.R;
import static works.lysenko.util.spec.Symbols._ASTRS_;
import static works.lysenko.util.spec.Symbols._AT_SGN;

/**
 * The Density class represents the density of an object in a two-dimensional space.
 * It stores the number of steps on the x-axis and y-axis.
 */
public record Resolution(int x, int y, Fraction fill, Shape shape) implements _Resolution {

    /**
     * Constructs a Resolution object with the given x and y values.
     *
     * @param x The value of x coordinate.
     * @param y The value of y coordinate.
     */
    public Resolution(final int x, final int y) {

        this(x, y, Fraction.ONE, RECTANGLE);
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public Fraction fill() {

        if ((null == fill) || (1 < fill.doubleValue())) return Fraction.ONE;
        return fill;
    }

    @Override
    public float ratio() {

        return ((float) y) / x;
    }

    @Override
    public Integer size() {

        final int x1 = _Resolution.getDimension(x);
        final int y1 = _Resolution.getDimension(y);
        return x1 * y1;
    }

    @Override
    public String text() {

        final String common = s((x << 1) + 1, s(_ASTRS_), (y << 1) + 1);
        String at = EMPTY;
        if ((1 < fill().doubleValue()) || (1 > fill().doubleValue())) at = ts(fill);
        if (ELLIPCE == shape) at = s(R, at);
        return at.isEmpty() ? common : s(common, _AT_SGN, at);

    }

}
