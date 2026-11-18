package works.lysenko.util.func.imgs;

import org.openqa.selenium.Rectangle;
import works.lysenko.util.prop.*;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

/**
 * Registry and selector for operation marker variants (similar to TreeProgressionAudio / Sonifier).
 * Controls which visual annotation style is used for screenshot marking ("neutral", "military", "classic").
 */
public record OperationMarkers() {

    private static final List<OperationMarker> ALL = List.of(
            new NeutralMarker(),
            new MilitaryMarker(),
            new ClassicMarker()
    );

    private static final OperationMarker DEFAULT_MARKER = ALL.get(0); // Neutral Marker default

    /**
     * @return list of modes for all registered operation markers
     */
    public static List<String> availableModes() {
        return ALL.stream().map(OperationMarker::mode).toList();
    }

    /**
     * Finds a registered OperationMarker by mode name (case-insensitive).
     *
     * @param mode the mode name (e.g. "military", "neutral", "classic")
     * @return Optional containing the matching marker if found
     */
    public static Optional<OperationMarker> find(final String mode) {
        if (null == mode || mode.isBlank()) return Optional.empty();
        final String normalized = mode.trim().toLowerCase();
        return ALL.stream().filter(m -> m.mode().equalsIgnoreCase(normalized)).findFirst();
    }

    /**
     * Returns the currently active OperationMarker.
     * Looks up configured property or parameter; falls back to DEFAULT_MARKER ("neutral").
     *
     * @return the active OperationMarker instance
     */
    public static OperationMarker current() {
        final String mode = PropEnum._SCREENSHOTS_MARKER_VARIANT.get();
        return find(mode).orElse(DEFAULT_MARKER);
    }

    /**
     * Delegates element marking to the currently active OperationMarker.
     */
    public static BufferedImage markElement(final BufferedImage image, final Rectangle rect,
                                            final String label, final String action) {
        return current().markElement(image, rect, label, action);
    }

    /**
     * Delegates point tap marking to the currently active OperationMarker.
     */
    public static BufferedImage markPoint(final BufferedImage image, final Point point, final String action) {
        return current().markPoint(image, point, action);
    }

    /**
     * Delegates swipe gesture marking to the currently active OperationMarker.
     */
    public static BufferedImage markSwipe(final BufferedImage image, final Point start, final Point end) {
        return current().markSwipe(image, start, end);
    }

    /**
     * Delegates status stamping to the currently active OperationMarker.
     */
    public static BufferedImage stampStatus(final BufferedImage image, final String status) {
        return current().stampStatus(image, status);
    }
}
