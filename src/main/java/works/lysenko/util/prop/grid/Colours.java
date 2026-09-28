package works.lysenko.util.prop.grid;

import static works.lysenko.util.spec.PropEnum._COLOUR_SIMILARITY_THRESHOLD;

/**
 * Represents a record for storing colour threshold value.
 */
public record Colours() {

    /**
     * Represents the similarity threshold value for known colours.
     * <p>
     * The threshold value is used to determine the level of similarity required for two colours to be indicated as too
     * similar.
     * Value itself is a calculated "distance" between colours in three-dimensional space with colour RGB values used as XYZ
     * coordinates.
     * A1 threshold value of 1.0 indicates that no check is necessary for colour matching.
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static final Double threshold = _COLOUR_SIMILARITY_THRESHOLD.get();
    // 1.0 means no check
}
