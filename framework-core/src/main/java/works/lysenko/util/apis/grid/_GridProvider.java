package works.lysenko.util.apis.grid;

import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import works.lysenko.util.data.enums.Severity;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import static works.lysenko.Base.exec;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Symbols.H;
import static works.lysenko.util.spec.Symbols.W;
import static works.lysenko.util.spec.Symbols.X;
import static works.lysenko.util.spec.Symbols.Y;
import static works.lysenko.util.spec.Symbols._AT_SGN;

/**
 * SPI bridging {@code framework-core} to screenshot capture, visual operation markers, and grid image validation
 * implemented in {@code framework-grid}.
 */
public interface _GridProvider {

    List<String> DEFAULT_MARKER_MODES = List.of("neutral", "military", "classic"); //NON-NLS

    /**
     * Returns the first registered {@link _GridProvider} on the classpath, if {@code framework-grid} is present.
     *
     * @return optional grid provider
     */
    static Optional<_GridProvider> find() {
        return Holder.PROVIDER;
    }

    /**
     * Generates a readable geometry description for a Selenium {@link Rectangle}.
     *
     * @param rectangle rectangle to describe
     * @return formatted geometry string
     */
    static String describeRectangle(final Rectangle rectangle) {
        return b(s(H, rectangle.height), s(W, rectangle.width),
                s(_AT_SGN), s(X, rectangle.x), s(Y, rectangle.y));
    }

    /**
     * Returns available visual operation marker mode names.
     *
     * @return list of supported marker mode names
     */
    static List<String> availableMarkerModes() {
        return Holder.PROVIDER
                .map(_GridProvider::getAvailableMarkerModes)
                .orElse(DEFAULT_MARKER_MODES);
    }

    /**
     * Captures a fullscreen screenshot without writing to disk unless configured.
     *
     * @return captured {@link BufferedImage}, or {@code null} if unavailable
     */
    static BufferedImage getBufferedImage() {
        return Holder.PROVIDER
                .map(_GridProvider::captureBufferedImage)
                .orElse(null);
    }

    /**
     * Captures a fullscreen screenshot with optional filename parts.
     *
     * @param name filename parts
     * @return captured {@link BufferedImage}, or {@code null}
     */
    static BufferedImage makeScreenshot(final String... name) {
        return Holder.PROVIDER
                .map(p -> p.captureScreenshot(name))
                .orElse(null);
    }

    /**
     * Captures a screenshot of a specific {@link WebElement} with optional filename parts.
     *
     * @param element element to capture
     * @param name    filename parts
     * @return captured {@link BufferedImage}, or {@code null}
     */
    static BufferedImage makeScreenshot(final WebElement element, final String... name) {
        return Holder.PROVIDER
                .map(p -> p.captureElementScreenshot(element, name))
                .orElse(null);
    }

    /**
     * Writes a {@link BufferedImage} screenshot to disk.
     *
     * @param image image to write
     * @param root  whether to write into the run root directory
     * @param name  filename parts
     * @return the written image, or {@code image} if no provider is present
     */
    static BufferedImage writeScreenshot(final BufferedImage image, final Boolean root, final String... name) {
        return Holder.PROVIDER
                .map(p -> p.storeScreenshot(image, root, name))
                .orElse(image);
    }

    /**
     * Marks an element operation on an image.
     *
     * @param image  source image
     * @param rect   element bounds
     * @param label  element label
     * @param action action name
     * @return annotated image
     */
    static BufferedImage markElement(final BufferedImage image, final Rectangle rect, final String label, final String action) {
        return Holder.PROVIDER
                .map(p -> p.annotateElement(image, rect, label, action))
                .orElse(image);
    }

    /**
     * Marks a point tap operation on an image.
     *
     * @param image  source image
     * @param point  tap coordinates
     * @param action action name
     * @return annotated image
     */
    static BufferedImage markPoint(final BufferedImage image, final Point point, final String action) {
        return Holder.PROVIDER
                .map(p -> p.annotatePoint(image, point, action))
                .orElse(image);
    }

    /**
     * Marks a swipe gesture on an image.
     *
     * @param image source image
     * @param start swipe start coordinates
     * @param end   swipe end coordinates
     * @return annotated image
     */
    static BufferedImage markSwipe(final BufferedImage image, final Point start, final Point end) {
        return Holder.PROVIDER
                .map(p -> p.annotateSwipe(image, start, end))
                .orElse(image);
    }

    /**
     * Validates an image against a named grid specification.
     * Fails closed (returns {@code false}) if no {@link _GridProvider} implementation is present on the classpath.
     *
     * @param image image to verify
     * @param name  grid specification name
     * @return {@code true} if grid verification passed, {@code false} if verification failed or no provider is registered
     */
    static boolean verifyImage(final BufferedImage image, final String name) {
        if (Holder.PROVIDER.isEmpty()) {
            if (isNotNull(exec)) {
                logEvent(Severity.S1, "Unable to verify image '" + name + "': no _GridProvider implementation on classpath (missing framework-grid)");
            }
            return false;
        }
        return Holder.PROVIDER.get().validateImage(image, name);
    }

    /**
     * Initializes background validation for the specified locator and grid name.
     *
     * @param locator         element locator
     * @param gridName        grid specification name
     * @param inImage         captured image
     * @param failureSeverity severity on failure
     */
    static void initBackgroundValidation(final String locator, final String gridName,
                                         final BufferedImage inImage, final Severity failureSeverity) {
        Holder.PROVIDER.ifPresent(p -> p.createBackgroundValidator(locator, gridName, inImage, failureSeverity));
    }

    /**
     * Lazy holder for classpath-discovered grid provider.
     */
    final class Holder {
        static final Optional<_GridProvider> PROVIDER =
                ServiceLoader.load(_GridProvider.class).findFirst();

        private Holder() {
        }
    }

    List<String> getAvailableMarkerModes();

    BufferedImage captureBufferedImage();

    BufferedImage captureScreenshot(String... name);

    BufferedImage captureElementScreenshot(WebElement element, String... name);

    BufferedImage storeScreenshot(BufferedImage image, Boolean root, String... name);

    BufferedImage annotateElement(BufferedImage image, Rectangle rect, String label, String action);

    BufferedImage annotatePoint(BufferedImage image, Point point, String action);

    BufferedImage annotateSwipe(BufferedImage image, Point start, Point end);

    boolean validateImage(BufferedImage image, String name);

    void createBackgroundValidator(String locator, String gridName, BufferedImage inImage, Severity failureSeverity);
}
