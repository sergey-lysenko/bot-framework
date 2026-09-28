package works.lysenko.util.func.imgs;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.func.logs.Trace;
import works.lysenko.util.prop.grid.Marker;

import java.awt.image.BufferedImage;

import static org.apache.commons.math3.util.FastMath.ceil;
import static works.lysenko.Base.logDebug;
import static works.lysenko.util.chrs.__.AT;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.imgs.BufferedImages.getBufferedImage;
import static works.lysenko.util.func.imgs.Painter.drawLineOn;
import static works.lysenko.util.func.imgs.Screenshot.writeScreenshot;
import static works.lysenko.util.func.logs.trace.Data.traceable;
import static works.lysenko.util.lang.word.A.ABSOLUTE;
import static works.lysenko.util.lang.word.P.PIXEL;
import static works.lysenko.util.lang.word.R.RELATIVE;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.spec.Symbols.*;

/**
 * The Pixels class provides methods to retrieve the RGB color of a pixel from a BufferedImage or a web element.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public record Pixels() {

    /**
     * Get RGB color of a pixel of an element addressed by locator and absolute coordinates
     *
     * @param locator string locator of requested element
     * @param x1      coordinate
     * @param y1      coordinate
     * @return RGB color of defined pixel
     */
    public static int getPixel(final String locator, final int x1, final int y1) {

        return getPixel(getBufferedImage(locator), x1, y1);
    }

    /**
     * Retrieves the pixel color at the specified coordinates from the given BufferedImage.
     *
     * @param img the BufferedImage from which to retrieve the pixel color
     * @param x1  the x-coordinate of the pixel
     * @param y1  the y-coordinate of the pixel
     * @return the pixel color at the specified coordinates
     */
    public static int getPixel(final BufferedImage img, final int x1, final int y1) {

        final int w = img.getWidth();
        final int h = img.getHeight();
        final int result = img.getRGB(x1, y1);
        logDebug(b(s(W, w), s(H, h), s(X, x1), s(Y, y1), a(RESULT, result)));
        pixelScreenshot(img, ABSOLUTE, x1, y1);
        return result;
    }

    /**
     * Get RGB color of a pixel of an element addressed by locator and absolute coordinates
     *
     * @param locator   string locator of requested element
     * @param relativeX coordinate
     * @param relativeY coordinate
     * @return RGB color of defined pixel
     */
    public static int getPixel(final String locator, final Fraction relativeX, final Fraction relativeY) {

        return getPixel(getBufferedImage(locator), relativeX, relativeY);
    }

    /**
     * Retrieves the color pixel value at the specified relative coordinates within a given BufferedImage.
     *
     * @param img       the BufferedImage to extract the pixel from
     * @param relativeX the relative x-coordinate in the range of [0.0, 1.0] representing the position along the width of
     *                  the image
     * @param relativeY the relative y-coordinate in the range of [0.0, 1.0] representing the position along the height of
     *                  the image
     * @return the color pixel value as an integer at the specified relative coordinates
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    public static int getPixel(final BufferedImage img, final Fraction relativeX, final Fraction relativeY) {

        final double epsilon = 0.001;
        final int w = img.getWidth();
        final int h = img.getHeight();
        final int x1 = (epsilon >= Math.abs(relativeX.doubleValue() - 0)) ? 1 : (int) ceil(w * relativeX.doubleValue());
        final int y1 = (epsilon >= Math.abs(relativeY.doubleValue() - 0)) ? 1 : (int) ceil(h * relativeY.doubleValue());
        final int result = img.getRGB(x1, y1);
        Trace.log(traceable(w, h, relativeX, relativeY, x1, y1, result));
        pixelScreenshot(img, RELATIVE, x1, y1);
        return result;
    }

    /**
     * Takes a screenshot and marks a point on the image if a certain condition is met.
     *
     * @param name the name of the screenshot
     * @param x1   the x-coordinate of the point to long click
     * @param y1   the y-coordinate of the point to long click
     */
    private static void pixelScreenshot(final BufferedImage image, final String name, final int x1, final int y1) {

        if (works.lysenko.util.prop.core.Screenshot.pixels) {
            drawLineOn(image, Marker.colour, new Location(x1, y1 - 1), new Location(x1, 0)); // up
            drawLineOn(image, Marker.colour, new Location(x1 + 1, y1), new Location(image.getWidth(), y1)); // right
            drawLineOn(image, Marker.colour, new Location(x1, y1 + 1), new Location(x1, image.getHeight())); // down
            drawLineOn(image, Marker.colour, new Location(x1 - 1, y1), new Location(0, y1)); // left
            writeScreenshot(image, false, b(name, PIXEL, AT, s(X, x1), s(Y, y1)));
        }
    }
}
