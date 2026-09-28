package works.lysenko.util.grid.validation;

import org.apache.commons.math3.fraction.Fraction;
import org.openqa.selenium.WebElement;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.q._ColoursQuotas;
import works.lysenko.util.apis.grid.r._ImageRequirements;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.apis.grid.t._RelativePosition;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.data.type.RelativePosition;
import works.lysenko.util.grid.record.ImageRequirements;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.gsrc.Resolution;

import java.awt.image.BufferedImage;

import static works.lysenko.Base.find;
import static works.lysenko.util.apis.grid.g._GridProperties.getGridPropertiesByName;
import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenAndCodeSnapshot;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenshot;
import static works.lysenko.util.lang.word.D.DEFAULT;
import static works.lysenko.util.lang.word.F.FULLSCREEN;
import static works.lysenko.util.lang.word.R.REQUESTED;
import static works.lysenko.util.lang.word.V.VALIDATION;

/**
 * The Probe class is responsible for generating and managing validation requests
 * for various scenarios, including grid-based validation, image analysis,
 * location-based validation, and others.
 */
public record Check() {

    /**
     * Creates a Request object with the given name.
     *
     * @param name the name of the validation request
     * @return a new Request object with the given name
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static Request byGrid(final String name) { // 15 usages

        return image(null, name);
    }

    /**
     * Creates a Request object with the given name, bufferedImage, and requirements.
     *
     * @param name          the name of the validation request
     * @param bufferedImage the BufferedImage object can be null
     * @param predefined    the ImageRequirements object representing the expectations for image analysis
     * @return a new Request object with the given name, BufferedImage, RelativePosition, Fraction, Density, and
     * ImageRequirements
     */
    @SuppressWarnings("WeakerAccess")
    public static Request byImageRequirements( // 2 usages
                                               final String name,
                                               final BufferedImage bufferedImage,
                                               final _ImageRequirements predefined) {

        final _GridProperties gridProperties = getGridPropertiesByName(name);
        final BufferedImage image = (null == bufferedImage) ? makeScreenAndCodeSnapshot(d(DEFAULT, FULLSCREEN, BY,
                VALIDATION, REQUESTED)) : bufferedImage;
        final _RelativePosition position = works.lysenko.util.grid.validation.Routines.getRelativePosition(gridProperties);
        final Fraction scale = Routines.getScale(gridProperties);
        final _Resolution resolution = works.lysenko.util.grid.validation.Routines.getResolution(gridProperties);

        return new Request(name, image, position, scale, resolution, null, _ImageRequirements.read(name,
                gridProperties, predefined, false));
    }

    /**
     * Creates a Request object with the given name, point, and dimensions.
     *
     * @param name       the name of the validation request
     * @param location   the Point object representing the position
     * @param dimensions the dimensions value
     * @return a new Request object with the given name, point, and dimensions
     */
    @SuppressWarnings("unused")
    public static Request byLocationDimensions( // 2 usages
                                                final String name,
                                                final Location location,
                                                final int dimensions) {

        return Factory.vr(name, null, location, dimensions);
    }

    /**
     * Creates a Request object with the provided parameters and default values if necessary.
     *
     * @param name       the name of the validation request
     * @param image      the BufferedImage object representing the image to be validated
     * @param scale      the Fraction object representing the scale of the image
     * @param resolution the Resolution object representing the resolution of the image
     * @param exclude    the Region object representing the region to be excluded from validation
     * @return a new Request object with the provided parameters or default values
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static Request byScaleResolutionExclude( // one usage
                                                    final BufferedImage image,
                                                    final String name,
                                                    final Fraction scale,
                                                    final _Resolution resolution,
                                                    final Iterable<_Region> exclude) {

        return Factory.vr(name, image, null, scale, resolution, exclude);
    }

    /**
     * Generates a new Request object based on the provided parameters with default values if necessary.
     *
     * @param name          the name of the validation request
     * @param bufferedImage the BufferedImage object representing the image to be validated
     * @return a new Request object with the provided parameters or default values
     */
    public static Request image(final BufferedImage bufferedImage, final String name) { // 14 usages

        return byImageRequirements(name, bufferedImage, null);
    }

    /**
     * Creates a Request object based on the specified locator and name.
     *
     * @param locator the locator used to identify the web element
     * @param name    the name of the validation request
     * @return a new Request object created using a screenshot of the identified web element and the specified name
     */
    public static Request element(final String locator, final String name) { // 14 usages

        return byImageRequirements(name, makeScreenshot(find(locator)), null);
    }

    /**
     * Creates a Request object using the provided WebElement and name.
     *
     * @param element the WebElement from which a screenshot is taken to create the validation request
     * @param name    the name of the validation request
     * @return a new Request object created using the screenshot of the provided WebElement and the specified name
     */
    public static Request element(final WebElement element, final String name) { // 14 usages

        return byImageRequirements(name, makeScreenshot(element), null);
    }

    /**
     * Creates a Request object with the provided parameters.
     *
     * @param colours    The ColoursQuotas object representing the colors of the image
     * @param image      The BufferedImage object representing the image to be validated
     * @param position   The RelativePosition object representing the position of the image
     * @param scale      The Fraction object representing the scale of the image
     * @param resolution The Resolution object representing the resolution of the image
     * @param exclude    The Iterable of Region objects representing the regions to be excluded from validation
     * @return A1 new Request object with the provided parameters
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static Request withoutGrid(
            final BufferedImage image,
            final _ColoursQuotas colours,
            final RelativePosition position,
            final Fraction scale,
            final Resolution resolution,
            final Iterable<_Region> exclude) {

        return new Request(null, image, position, scale, resolution, exclude, new ImageRequirements(colours));
    }
}
