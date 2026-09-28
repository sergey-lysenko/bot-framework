package works.lysenko.util.apis.grid.t;

import works.lysenko.util.data.records.Location;

/**
 * Represents an interface for defining an absolute region with specific characteristics.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _AbsoluteRegion {

    /**
     * Expands the region by adding a new point at the specified coordinates and updating the bounds accordingly.
     *
     * @param x The x-coordinate of the point to add
     * @param y The y-coordinate of the point to add
     */
    void expand(int x, int y);

    /**
     * Checks if a point with coordinates (x, y) is included in a region based on a normal range.
     *
     * @param x      The x-coordinate of the point to check
     * @param y      The y-coordinate of the point to check
     * @param normal The normal range for inclusion
     * @return true if the point is included in the region, false otherwise
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean includes(int x, int y, Integer normal);

    /**
     * Returns the point with the maximum coordinates within the region.
     *
     * @return the point with the maximum coordinates within the region
     */
    Location max();

    /**
     * Returns the point with the minimum coordinates within the region.
     *
     * @return the point with the minimum coordinates within the region
     */
    Location min();
}
