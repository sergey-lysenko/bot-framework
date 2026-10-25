package works.lysenko.util.grid;

import org.openqa.selenium.Point;
import works.lysenko.util.apis.grid.t._AbsoluteRegion;
import works.lysenko.util.data.records.Location;

import static java.util.Objects.isNull;

/**
 * Represents a region defined by a collection of points with specific boundaries in a two-dimensional space.
 */
public final class AbsoluteRegion implements _AbsoluteRegion {

    private Location min;
    private Location max;

    private AbsoluteRegion(final int x, final int y) {

        min = new Location(x, y);
        max = new Location(x, y);
        expand(x, y);
    }

    /**
     * Creates a new AbsoluteRegion object with the specified x and y coordinates.
     *
     * @param x The x-coordinate for the new AbsoluteRegion
     * @param y The y-coordinate for the new AbsoluteRegion
     * @return a new AbsoluteRegion object with the specified coordinates
     */
    public static _AbsoluteRegion create(final int x, final int y) {

        return new AbsoluteRegion(x, y);
    }

    /**
     * Calculates the Euclidean distance between two points given their coordinates.
     *
     * @param x     The x-coordinate of the first point
     * @param y     The y-coordinate of the first point
     * @param point The second point with x and y coordinates
     * @return The Euclidean distance between the two points
     */
    public static double distance(final int x, final int y, final Point point) {

        final int xD = point.x - x;
        final int yD = point.y - y;

        return Math.sqrt(xD * xD + yD * yD);
    }

    /**
     * Expands the region to include the specified coordinates.
     *
     * @param x the x-coordinate to include
     * @param y the y-coordinate to include
     */
    @Override
    public void expand(final int x, final int y) {

        updateBounds(x, y);
    }

    /**
     * Checks if the specified coordinates are included within the region, optionally padded by a normal value.
     *
     * @param x      the x-coordinate to check
     * @param y      the y-coordinate to check
     * @param normal the normal padding value (can be null)
     * @return true if the coordinates are included, false otherwise
     */
    @SuppressWarnings({"OverlyComplexBooleanExpression", "MethodWithMultipleReturnPoints"})
    public boolean includes(final int x, final int y, final Integer normal) {

        if (isNull(normal)) return true;
        else return (min.x() - normal < x) &&
                (x < max.x() + normal) &&
                (min.y() - normal < y) &&
                (y < max.y() + normal);
    }

    /**
     * Returns the maximum boundary location of the region.
     *
     * @return the maximum location
     */
    @Override
    public Location max() {

        return max;
    }

    /**
     * Returns the minimum boundary location of the region.
     *
     * @return the minimum location
     */
    @Override
    public Location min() {

        return min;
    }

    private void updateBounds(final int x, final int y) {

        min = new Location(Math.min(min.x(), x), Math.min(min.y(), y));
        max = new Location(Math.max(max.x(), x + 1), Math.max(max.y(), y + 1));
    }
}
