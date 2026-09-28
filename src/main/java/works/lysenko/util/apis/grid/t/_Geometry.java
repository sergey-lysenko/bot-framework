package works.lysenko.util.apis.grid.t;

/**
 * Interface representing geometric shapes, providing methods to compute
 * the centre, radius, and aspect ratio of the shape. Implementations are
 * expected to define the specifics of these geometric calculations.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Geometry {

    /**
     * Computes the centre point of the geometry.
     *
     * @return a Location object representing the centre of the geometry, or null if not defined.
     */
    default _Location center() {

        return null;
    }

    /**
     * Retrieves the radius of the geometry shape.
     *
     * @return the radius of the geometry, which by default is 0.
     */
    default float radius() {

        return 0;
    }

    /**
     * Calculates the ratio based on the specific implementation of the geometry.
     *
     * @return a float representing the ratio, typically calculated as a proportion
     * of some dimension such as width to height, with a default value of 0.
     */
    default float ratio() {

        return 0;
    }

}
