package works.lysenko.util.prop.grid;

import static works.lysenko.util.prop.PropEnum._GRID_SHRINK_EXPECTATIONS;
import static works.lysenko.util.prop.PropEnum._GRID_UPDATE_COLOUR_EXPECTATIONS;
import static works.lysenko.util.prop.PropEnum._GRID_UPDATE_HSV_EXPECTATIONS;
import static works.lysenko.util.prop.PropEnum._GRID_UPDATE_PASSED_EXPECTATIONS;
import static works.lysenko.util.prop.PropEnum._GRID_UPDATE_POLYCHROMY_EXPECTATIONS;

/**
 * Configuration for updating grid expectations.
 * <p>
 * Controls automatic updating of expected validation values, including colour,
 * HSV (hue, saturation, value), passed expectations, polychromy, and expectation shrinking.
 */
public record Update() {

    /**
     * Indicates whether colour expectations should be automatically updated.
     */
    public static final boolean colour = _GRID_UPDATE_COLOUR_EXPECTATIONS.get();

    /**
     * Indicates whether HSV (hue, saturation, value) expectations should be automatically updated.
     */
    @SuppressWarnings("unused")
    public static final boolean hsv = _GRID_UPDATE_HSV_EXPECTATIONS.get();

    /**
     * Indicates whether passed validation expectations should be updated.
     */
    public static final boolean passed = _GRID_UPDATE_PASSED_EXPECTATIONS.get();

    /**
     * Indicates whether polychromy expectations should be automatically updated.
     */
    public static final boolean polychromy = _GRID_UPDATE_POLYCHROMY_EXPECTATIONS.get();

    /**
     * Indicates whether expectations should be shrunk to tighter bounds when updated.
     */
    public static final boolean shrink = _GRID_SHRINK_EXPECTATIONS.get();
}
