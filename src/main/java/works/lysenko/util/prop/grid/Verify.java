package works.lysenko.util.prop.grid;

import static works.lysenko.util.prop.PropEnum._GRID_VERIFY_COLOURS;

/**
 * Verification flags for grid validation operations.
 */
public record Verify() {

    /**
     * Indicates whether colours should be verified during grid validation.
     */
    public static final boolean colours = _GRID_VERIFY_COLOURS.get();
}
