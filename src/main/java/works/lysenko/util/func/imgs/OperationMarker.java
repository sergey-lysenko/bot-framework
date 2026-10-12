package works.lysenko.util.func.imgs;

import org.openqa.selenium.Rectangle;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Interface for screenshot operation marker variants.
 * Implementations define how UI element actions, taps, swipes, and failure statuses are visually rendered on screenshots.
 */
public interface OperationMarker {

    /**
     * @return the mode identifier used to select this marker variant (e.g. "military", "neutral", "classic")
     */
    String mode();

    /**
     * @return user-friendly description of this marker variant
     */
    String description();

    /**
     * Annotates a target UI element action on a screenshot.
     *
     * @param image  the screenshot image
     * @param rect   the bounding rectangle of the target element
     * @param label  the element label or description
     * @param action the action performed (e.g. "CLICK", "INPUT")
     * @return the annotated image
     */
    BufferedImage markElement(BufferedImage image, Rectangle rect, String label, String action);

    /**
     * Annotates a point tap / click action on a screenshot.
     *
     * @param image  the screenshot image
     * @param point  the target coordinates
     * @param action action description
     * @return the annotated image
     */
    BufferedImage markPoint(BufferedImage image, Point point, String action);

    /**
     * Annotates a swipe gesture on a screenshot.
     *
     * @param image the screenshot image
     * @param start start coordinates
     * @param end   end coordinates
     * @return the annotated image
     */
    BufferedImage markSwipe(BufferedImage image, Point start, Point end);

    /**
     * Stamps a status watermark on a screenshot (e.g. failure/bug report).
     *
     * @param image  the screenshot image
     * @param status the status or failure description
     * @return the stamped image
     */
    BufferedImage stampStatus(BufferedImage image, String status);
}
