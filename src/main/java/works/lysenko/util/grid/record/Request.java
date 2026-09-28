package works.lysenko.util.grid.record;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.r._ImageRequirements;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.apis.grid.t._RelativePosition;
import works.lysenko.util.apis.grid.v._ValidationRequest;

import java.awt.image.BufferedImage;

/**
 * Represents a validation request with specific parameters.
 */
@SuppressWarnings("WeakerAccess")
public record Request(String name,
                      BufferedImage image,
                      _RelativePosition position,
                      Fraction scale,
                      _Resolution resolution,
                      Iterable<_Region> exclude,
                      _ImageRequirements irq) implements _ValidationRequest {
}