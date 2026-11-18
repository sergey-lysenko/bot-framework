package works.lysenko.util.apis.grid.t;

import java.awt.Point;

/**
 * The _BitBuffer interface represents a buffer of bits in a two-dimensional space.
 * It allows setting and retrieving individual bits at specific coordinates.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _BitPlane {

    /**
     * Retrieves the coordinates of the farthest set bit in the _BitBuffer.
     *
     * @return a Point object representing the coordinates of the farthest set bit
     */
    Point farBound();

    /**
     * Retrieves the value of the bit at the specified coordinates in the BitBuffer.
     *
     * @param x the x-coordinate of the bit
     * @param y the y-coordinate of the bit
     * @return the value of the bit at the specified coordinates
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    Boolean get(int x, int y);

    /**
     * Retrieves the maximum x-coordinate where a bit is set in the _BitBuffer.
     *
     * @return the maximum x-coordinate where a bit is set
     */
    Integer getMaxSetX();

    /**
     * Retrieves the maximum y-coordinate where a bit is set in the _BitBuffer.
     *
     * @return the maximum y-coordinate where a bit is set
     */
    Integer getMaxSetY();

    /**
     * Retrieves the minimum x-coordinate where a bit is set in the _BitBuffer.
     *
     * @return the minimum x-coordinate where a bit is set
     */
    Integer getMinSetX();

    /**
     * Retrieves the minimum y-coordinate where a bit is set in the _BitBuffer.
     *
     * @return the minimum y-coordinate where a bit is set
     */
    Integer getMinSetY();

    /**
     * Retrieves the coordinates of the nearest set bit to the origin (0, 0) in the _BitBuffer.
     *
     * @return a Point object representing the coordinates of the nearest set bit
     */
    Point nearBound();

    /**
     * Sets the value of a bit at the specified coordinates in the BitBuffer.
     *
     * @param x     the x-coordinate of the bit
     * @param y     the y-coordinate of the bit
     * @param value the value to set the bit to
     */
    void set(int x, int y, boolean value);

    /**
     * Sets a bit at the specified coordinates in the BitBuffer.
     *
     * @param x the x-coordinate of the bit
     * @param y the y-coordinate of the bit
     */
    void set(int x, int y);

}
