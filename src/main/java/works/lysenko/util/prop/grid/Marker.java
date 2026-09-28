package works.lysenko.util.prop.grid;

import java.awt.Color;

import static works.lysenko.util.spec.PropEnum._ACTION_MARKER_COLOUR;

/**
 * Represents a marker with a specific colour.
 * <p>
 * The colour of the marker is defined by the application properties, and defaults to a semi-transparent green if not
 * specified.
 */
@SuppressWarnings({"MissingJavadoc", "NonFinalStaticVariableUsedInClassInitialization"})
public record Marker() {

    public static final Color colour = _ACTION_MARKER_COLOUR.get();
}
