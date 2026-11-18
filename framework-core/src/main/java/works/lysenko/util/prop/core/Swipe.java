package works.lysenko.util.prop.core;

import java.awt.Color;

import static works.lysenko.util.prop.PropEnum._SWIPE_LINE_MARKER_COLOUR;
import static works.lysenko.util.prop.PropEnum._SWIPE_START_MARKER_COLOUR;
import static works.lysenko.util.prop.PropEnum._SWIPE_STOP_MARKER_COLOUR;

/**
 * Configuration for swipe gesture rendering, reduction, and weighting.
 */
public record Swipe() {

    /**
     * Colour used for the start marker in swipe gesture overlays.
     */
    public static final Color start = _SWIPE_START_MARKER_COLOUR.get();

    /**
     * Colour used for the stop marker in swipe gesture overlays.
     */
    public static final Color stop = _SWIPE_STOP_MARKER_COLOUR.get();

    /**
     * Colour used for drawing the path line in swipe gesture overlays.
     */
    public static final Color line = _SWIPE_LINE_MARKER_COLOUR.get();
}
