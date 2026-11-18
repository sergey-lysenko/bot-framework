package works.lysenko.util.func.type;

import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;

/**
 * Points
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Points() {

    /**
     * Calculate a point placed in a center of a rectangle
     *
     * @param r Rectangle
     * @return Point located in a center of the given Rectangle
     */
    public static Point getCenter(final Rectangle r) {

        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }

    /**
     * Calculate a point based on a rectangle and relative coefficients of x and y
     * coordinates. Coordinate 0.0 corresponds to leftmost or upper part of the
     * rectangle, and 1.0 is for rightmost of the lowest part of an object.
     *
     * @param x horizontal location of the requested point relative to the given
     *          rectangle
     * @param y vertical location of the requested point relative to the given
     *          rectangle
     * @param r base rectangle
     * @return Point object based on given parameters
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    public static Point getPoint(final double x, final double y, final Rectangle r) {

        final int offsetX = (int) Math.round(r.width * x);
        final int offsetY = (int) Math.round(r.height * y);
        return new Point(r.x + offsetX, r.y + offsetY);
    }
}
