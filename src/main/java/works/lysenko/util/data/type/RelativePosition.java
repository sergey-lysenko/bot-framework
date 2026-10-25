package works.lysenko.util.data.type;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._RelativePosition;

import static org.apache.commons.math3.fraction.Fraction.ONE_HALF;
import static org.apache.commons.math3.fraction.Fraction.getReducedFraction;
import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * Represents a position relative to a reference area using horizontal and vertical fractions.
 * Implements the {@_RelativePosition} interface.
 */
public class RelativePosition implements _RelativePosition {

    /**
     * A predefined constant representing the center position relative to a reference area.
     * The horizontal and vertical values are both set to one-half, indicating a position
     * exactly in the middle of the reference area.
     */
    public final static RelativePosition CENTRE = rp(ONE_HALF, ONE_HALF);

    /** The horizontal relative position as a Fraction. */
    private final Fraction horizontal;
    /** The vertical relative position as a Fraction. */
    private final Fraction vertical;

    /**
     * Constructs a RelativePosition with horizontal and vertical rational components given by numerators and denominators.
     *
     * @param hNum the numerator of the horizontal value
     * @param hDen the denominator of the horizontal value
     * @param vNum the numerator of the vertical value
     * @param vDen the denominator of the vertical value
     */
    @SuppressWarnings("WeakerAccess")
    public RelativePosition(final int hNum, final int hDen, final int vNum, final int vDen) {

        horizontal = getReducedFraction(hNum, hDen);
        vertical = getReducedFraction(vNum, vDen);
    }

    /**
     * Constructs a RelativePosition with horizontal and vertical Fraction values.
     *
     * @param horizontal the horizontal value represented as a Fraction
     * @param vertical   the vertical value represented as a Fraction
     */
    public RelativePosition(final Fraction horizontal, final Fraction vertical) {

        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    /**
     * Returns a new RelativePosition object with the given horizontal and vertical fraction components.
     *
     * @param hNum the numerator of the horizontal value
     * @param hDen the denominator of the horizontal value
     * @param vNum the numerator of the vertical value
     * @param vDen the denominator of the vertical value
     * @return a new RelativePosition object
     */
    @SuppressWarnings("UseOfConcreteClass")
    public static RelativePosition rp(final int hNum, final int hDen, final int vNum, final int vDen) {

        return new RelativePosition(hNum, hDen, vNum, vDen);
    }

    /**
     * Returns a new RelativePosition object with the given horizontal and vertical string representations.
     *
     * @param horizontal the horizontal value represented as a string
     * @param vertical   the vertical value represented as a string
     * @return a new RelativePosition object
     */
    @SuppressWarnings("UseOfConcreteClass")
    public static RelativePosition rp(final String horizontal, final String vertical) {

        return new RelativePosition(fr(horizontal), fr(vertical));
    }

    /**
     * Creates a new RelativePosition object with the given horizontal and vertical double values and epsilon.
     *
     * @param horizontal the horizontal value
     * @param vertical   the vertical value
     * @param epsilon    the epsilon value for fraction conversion
     * @return a new RelativePosition object
     */
    @SuppressWarnings("UseOfConcreteClass")
    public static RelativePosition rp(final double horizontal, final double vertical, final double epsilon) {

        return new RelativePosition(new Fraction(horizontal, epsilon, 100), new Fraction(vertical, epsilon, 100));
    }

    /**
     * Creates a RelativePosition object with the given horizontal and vertical double values.
     *
     * @param horizontal the horizontal double value
     * @param vertical   the vertical double value
     * @return a new RelativePosition object
     */
    public static RelativePosition rp(final double horizontal, final double vertical) {

        return new RelativePosition(fr(horizontal), fr(vertical));
    }

    /**
     * Creates a new RelativePosition object with the provided horizontal and vertical fractions.
     *
     * @param horizontal the horizontal Fraction
     * @param vertical   the vertical Fraction
     * @return a new RelativePosition object
     */
    public static RelativePosition rp(final Fraction horizontal, final Fraction vertical) {

        return new RelativePosition(horizontal, vertical);
    }

    @Override
    public final double h() {

        return horizontal.doubleValue();
    }

    @Override
    public final double hor() {

        return horizontal.doubleValue();
    }

    @Override
    public final Fraction horizontal() {

        return horizontal;
    }

    @Override
    public final double v() {

        return vertical.doubleValue();
    }

    @Override
    public final double ver() {

        return vertical.doubleValue();
    }

    @Override
    public final Fraction vertical() {

        return vertical;
    }
}
