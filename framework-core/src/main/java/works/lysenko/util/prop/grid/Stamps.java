package works.lysenko.util.prop.grid;

import static works.lysenko.util.prop.PropEnum._GRID_STAMPS_COMPRESS;
import static works.lysenko.util.prop.PropEnum._GRID_STAMPS_DISPLAY;
import static works.lysenko.util.prop.PropEnum._GRID_STAMPS_PROCESS;

/**
 * Configuration for grid calculation stamp processing and display.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Stamps() {

    /**
     * Indicates whether grid calculation stamps should be processed.
     */
    public static final boolean process = _GRID_STAMPS_PROCESS.get();

    /**
     * Indicates whether range result stamps should be displayed in outputs.
     */
    public static final boolean display = _GRID_STAMPS_DISPLAY.get();

    /**
     * Indicates whether grid stamps should be compressed when compression yields smaller size.
     */
    public static final boolean compress = _GRID_STAMPS_COMPRESS.get();

}
