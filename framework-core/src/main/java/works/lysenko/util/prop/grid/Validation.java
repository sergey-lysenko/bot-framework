package works.lysenko.util.prop.grid;

import static works.lysenko.util.prop.PropEnum._GRID_VALIDATION_DEFAULT_BORDER;
import static works.lysenko.util.prop.PropEnum._GRID_VALIDATION_FENCES;
import static works.lysenko.util.prop.PropEnum._GRID_VALIDATION_HSB_THRESHOLD;
import static works.lysenko.util.prop.PropEnum._GRID_VALIDATION_SCALE_EPSILON;

/**
 * Configuration parameters for grid image validation.
 * <p>
 * Defines tolerance and threshold properties used during validation of grid scale,
 * colour borders, and HSB (hue, saturation, brightness) levels.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Validation() {

    /**
     * Allowable deviation or tolerance level when comparing scale factors during grid validation.
     */
    public static final double scaleEpsilon = _GRID_VALIDATION_SCALE_EPSILON.get();

    /**
     * Fence pattern string used to delineate boundaries in grid validation.
     */
    public static final String fences = _GRID_VALIDATION_FENCES.get();

    /**
     * Default colour border tolerance fraction string used for grid colour validation.
     */
    public static final String defaultColoursBorder = _GRID_VALIDATION_DEFAULT_BORDER.get();

    /**
     * Default colour cutoff tolerance fraction string (synonym for {@link #defaultColoursBorder}).
     */
    public static final String defaultColoursCutoff = defaultColoursBorder;

    /**
     * Threshold fraction string for hue, saturation, and brightness (HSB) used in grid colour validation.
     */
    public static final String hsbThreshold = _GRID_VALIDATION_HSB_THRESHOLD.get();

}
