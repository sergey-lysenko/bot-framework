package works.lysenko.util.data.records;

import works.lysenko.util.apis.grid.t._Location;

import static org.apache.commons.math3.fraction.Fraction.ONE_HALF;
import static works.lysenko.util.data.strs.Swap.i;

/**
 * Represents a point in a two-dimensional space with float coordinates.
 */
@SuppressWarnings({"MethodNamesDifferingOnlyByCase", "EqualsAndHashcode"})
public record Location(float x, float y) implements _Location {

    /**
     * Creates a Location object representing a point in a two-dimensional space with integer coordinates.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return a Location object representing the specified point
     */
    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    public static Location point(final int x, final int y) {

        return new Location(x, y);
    }

    /**
     * Creates a Location object representing a point in a two-dimensional space with float coordinates.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return a Location object representing the specified point
     */
    @SuppressWarnings({"OverloadedMethodsWithSameNumberOfParameters", "NumericCastThatLosesPrecision"})
    public static Location point(final double x, final double y) {

        return new Location((float) x, (float) y);
    }

    /**
     * Creates a Location object representing a point in a two-dimensional space with float coordinates.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return a Location object representing the specified point
     */
    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    public static Location point(final float x, final float y) {

        return new Location(x, y);
    }

    /**
     * Round the value of x to the nearest integer and return it.
     *
     * @return the rounded value of x as an integer
     */
    public int X() {

        return i((double) Math.round(x() - ONE_HALF.doubleValue()));
    }

    /**
     * Rounds the y-coordinate of the current Location object to the nearest integer value and returns it.
     *
     * @return the rounded y-coordinate as an integer
     */
    public int Y() {

        return i((double) Math.round(y() - ONE_HALF.doubleValue()));
    }

    @SuppressWarnings({"MethodWithMultipleReturnPoints", "ParameterNameDiffersFromOverriddenParameter"})
    @Override
    public boolean equals(final Object o) {

        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;

        final Location point = (Location) o;
        return 0 == Float.compare(x, point.x()) && 0 == Float.compare(y, point.y());
    }
}
