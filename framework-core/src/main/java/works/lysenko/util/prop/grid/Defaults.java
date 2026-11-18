package works.lysenko.util.prop.grid;

import static works.lysenko.util.prop.PropEnum._GRID_DEFAULT_HORISONTAL;
import static works.lysenko.util.prop.PropEnum._GRID_DEFAULT_RESOLUTION;
import static works.lysenko.util.prop.PropEnum._GRID_DEFAULT_SCALE;
import static works.lysenko.util.prop.PropEnum._GRID_DEFAULT_VERTICAL;

/**
 * Defines default geometry and resolution configuration for grids.
 */
public record Defaults() {

    /**
     * Default horizontal grid cell division/count.
     */
    public static final String horizontal = _GRID_DEFAULT_HORISONTAL.get();

    /**
     * Default vertical grid cell division/count.
     */
    public static final String vertical = _GRID_DEFAULT_VERTICAL.get();

    /**
     * Default scale factor applied to grid rendering and geometry.
     */
    public static final String scale = _GRID_DEFAULT_SCALE.get();

    /**
     * Default display/device resolution string for grid layouts.
     */
    public static final String resolution = _GRID_DEFAULT_RESOLUTION.get();
}
