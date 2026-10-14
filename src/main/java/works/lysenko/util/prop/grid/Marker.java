package works.lysenko.util.prop.grid;

import java.awt.Color;

import static works.lysenko.util.prop.PropEnum._GRID_ACTION_MARKER_COLOUR;

/**
 * Represents a marker with a specific colour.
 * <p>
 * The colour of the marker is defined by the application properties, and defaults to a semi-transparent green if not
 * specified.
 */
public record Marker() {

    /**
     * Colour used to highlight actions on grid images or screenshots.
     */
    public static final Color colour = _GRID_ACTION_MARKER_COLOUR.get();
}
