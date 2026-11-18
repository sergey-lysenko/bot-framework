package works.lysenko.util.apis.grid.v;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.r._ImageRequirements;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.apis.grid.t._RelativePosition;

import java.awt.image.BufferedImage;

/**
 * The _ValidationRequest interface defines a contract for creating validation requests,
 * allowing for the configuration and retrieval of various properties and settings related
 * to image validation processes.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _ValidationRequest {

    /**
     * Returns an iterable collection of _Region objects that should be excluded from the validation request.
     *
     * @return an Iterable containing the _Region objects to be excluded
     */
    Iterable<_Region> exclude();

    /**
     * Returns the image associated with the validation request.
     *
     * @return the BufferedImage object representing the image for validation
     */
    BufferedImage image();

    /**
     * Returns the image requirements associated with the validation request.
     *
     * @return the _ImageRequirements object representing the image-related requirements for validation
     */
    _ImageRequirements irq();

    /**
     * Returns the name associated with the validation request.
     *
     * @return the name of the validation request
     */
    String name();

    /**
     * Returns the relative position associated with the validation request.
     *
     * @return the _RelativePosition object representing the position for validation
     */
    _RelativePosition position();

    /**
     * Returns the resolution associated with the validation request.
     *
     * @return the _Resolution object representing the resolution settings for validation
     */
    _Resolution resolution();

    /**
     * Returns the scaling factor associated with the validation request.
     *
     * @return the Fraction representing the scale factor
     */
    Fraction scale();

}
