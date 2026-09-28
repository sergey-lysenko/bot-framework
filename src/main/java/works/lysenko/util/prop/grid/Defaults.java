package works.lysenko.util.prop.grid;

import static works.lysenko.util.spec.PropEnum.*;

/**
 * A1 class that defines default values for grid configurations.
 */
@SuppressWarnings("MissingJavadoc")
public record Defaults() {

    public static final String horizontal = _GRID_DEFAULT_HORISONTAL.get();
    public static final String vertical = _GRID_DEFAULT_VERTICAL.get();
    public static final String scale = _GRID_DEFAULT_SCALE.get();
    public static final String resolution = _GRID_DEFAULT_RESOLUTION.get();
}
