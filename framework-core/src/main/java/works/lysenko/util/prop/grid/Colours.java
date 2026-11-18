package works.lysenko.util.prop.grid;

import static works.lysenko.util.prop.PropEnum._GRID_COLOUR_SIMILARITY_THRESHOLD;

/**
 * Configuration for grid colour similarity evaluation.
 */
public record Colours() {

    /**
     * Similarity threshold value for known colours.
     * <p>
     * Used to determine the level of similarity required for two colours to be indicated as too similar.
     * The value represents calculated distance between colours in 3D RGB colour space.
     * A threshold value of 1.0 indicates that no similarity check is performed.
     */
    public static final Double threshold = _GRID_COLOUR_SIMILARITY_THRESHOLD.get();
}
