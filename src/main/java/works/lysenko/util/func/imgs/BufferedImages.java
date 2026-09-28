package works.lysenko.util.func.imgs;

import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

import static works.lysenko.Base.exec;
import static works.lysenko.Base.logDebug;
import static works.lysenko.util.chrs.___.PNG;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenshot;
import static works.lysenko.util.lang.word.A.AREA;
import static works.lysenko.util.lang.word.C.CROPPING;
import static works.lysenko.util.lang.word.F.FULLSCREEN;
import static works.lysenko.util.lang.word.I.IMAGE;
import static works.lysenko.util.lang.word.R.REQUESTED;

/**
 * This class provides utility methods for working with BufferedImages.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MethodWithMultipleReturnPoints", "unused", "ClassIndependentOfModule"})
public record BufferedImages() {

    /**
     * Converts a Base64 encoded string to a BufferedImage.
     *
     * @param base64String the Base64 encoded string to convert
     * @return the BufferedImage representation of the base64 string
     * @throws IOException if an I/O error occurs during the conversion
     */
    public static BufferedImage base64ToBufferedImage(final String base64String) throws IOException {

        final byte[] bytes = Base64.getDecoder().decode(base64String);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    /**
     * Converts a BufferedImage to Base64 encoded string.
     *
     * @param image the BufferedImage to convert
     * @return the Base64 encoded string representation of the image or null in case of IOException
     */
    public static String bufferedImageToBase64(final RenderedImage image) {

        try {
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, PNG, stream);
            return Base64.getEncoder().encodeToString(stream.toByteArray());
        } catch (final IOException e) {
            return null;
        }
    }

    /**
     * Returns a BufferedImage by calling the makeScreenshot method with the specified parameters.
     *
     * @return a BufferedImage object.
     */
    public static BufferedImage getBufferedImage() {

        return makeScreenshot(null, false, null, false);
    }

    /**
     * Retrieves a buffered image of a web element.
     *
     * @param locator the locator string used to find the web element
     * @return a BufferedImage representation of the web element
     */
    public static BufferedImage getBufferedImage(final String locator) {

        final WebElement element = exec.find(locator);
        return makeScreenshot(element, false, null, false, exec.describe(element));
    }

    /**
     * Retrieves a buffered image of the given web element.
     *
     * @param element the web element to capture as an image
     * @return a buffered image representing the captured web element
     */
    public static BufferedImage getBufferedImage(final WebElement element) {

        return makeScreenshot(element, false, null, false);
    }

    /**
     * Retrieves a BufferedImage of a given WebElement using the specified substituteFullscreen image and name.
     *
     * @param element              The WebElement to capture the screenshot from.
     * @param substituteFullscreen The BufferedImage to use as a substitute if capturing the entire screen is not possible.
     * @return The captured BufferedImage.
     */
    public static BufferedImage getBufferedImage(final WebElement element, final BufferedImage substituteFullscreen) {

        return makeScreenshot(element, false, substituteFullscreen, false);
    }

    /**
     * Retrieves a BufferedImage of the specified WebElement with the ability to handle logging and substitute images.
     *
     * @param element              The WebElement to capture as a BufferedImage.
     * @param silent               A flag to suppress additional logging during the image capture process.
     * @param substituteFullscreen A pre-captured fullscreen BufferedImage to use as a substitute if capturing from the WebElement is not possible.
     * @return A BufferedImage representation of the specified WebElement or the substitute image if applicable.
     */
    public static BufferedImage getBufferedImage(final WebElement element, final boolean silent, final BufferedImage substituteFullscreen) {

        return makeScreenshot(element, silent, false, substituteFullscreen, false);
    }

    /**
     * Creates a copy of a given BufferedImage.
     *
     * @param source The BufferedImage to copy.
     * @return The copy of the BufferedImage.
     */
    public static BufferedImage getCopyOf(final BufferedImage source) {

        final BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        final Graphics graphics = copy.getGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    /**
     * @param image to crop
     * @param rect  to crop
     * @return cropped image (copy)
     */
    static BufferedImage getCropped(final BufferedImage image, final Rectangle rect, final boolean silent) {

        if (!silent) logDebug(b(c(CROPPING), REQUESTED, AREA, FROM, FULLSCREEN, IMAGE));
        final BufferedImage cropped = new BufferedImage(rect.width, rect.height, image.getType());
        final Graphics2D g2d = cropped.createGraphics();
        g2d.drawImage(image.getSubimage(rect.x, rect.y, rect.width, rect.height), 0, 0, null);
        g2d.dispose();
        return cropped;
    }

    /**
     * Loads a BufferedImage from the specified file name.
     *
     * @param pathname the name of the file to load the BufferedImage from
     * @return the loaded BufferedImage, or null if an IOException occurs
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static BufferedImage loadBufferedImageFromFile(final String pathname) {

        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(pathname));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return image;
    }
}
