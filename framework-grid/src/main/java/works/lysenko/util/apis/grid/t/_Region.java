package works.lysenko.util.apis.grid.t;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.type.RelativePosition;

/**
 * Represents a region with defined aspect ratio, position, and scale.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Region {

    /**
     * Retrieves the aspect ratio of the region.
     *
     * @return the aspect ratio as a Fraction
     */
    Fraction aspect();

    /**
     * Retrieves the position relative to a reference area.
     *
     * @return an instance of RelativePosition representing the position relative to a reference area
     */
    RelativePosition position();

    /**
     * Retrieves the scale factor of the region.
     *
     * @return the scale factor as a Fraction
     */
    Fraction scale();

}
