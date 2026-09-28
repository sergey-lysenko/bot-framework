package works.lysenko.util.apis.grid.t;

/**
 * Represents a location with x and y coordinates in a two-dimensional space.
 * This interface provides default methods to access these coordinates.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "MethodNamesDifferingOnlyByCase"})
public interface _Location {

    /**
     * Returns the x-coordinate of the location.
     *
     * @return the x-coordinate as an integer
     */
    int X();

    /**
     * Returns the y-coordinate of the location.
     *
     * @return the y-coordinate as an integer
     */
    int Y();

    /**
     * Returns the x-coordinate value of the location.
     *
     * @return the x-coordinate as a float
     */
    default float x() {

        return 0;
    }

    /**
     * Returns the y-coordinate value of the location.
     *
     * @return the y-coordinate as a float
     */
    default float y() {

        return 0;
    }

}
