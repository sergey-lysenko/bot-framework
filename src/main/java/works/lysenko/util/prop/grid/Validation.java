package works.lysenko.util.prop.grid;

import static works.lysenko.util.spec.PropEnum.*;

/**
 * Represents a configuration validation record with specific properties.
 * <p>
 * This class manages several static final fields that store configuration values
 * related to grid validation aspects within the application. The properties
 * include a scale epsilon value, border colour defaults, and a threshold for hue,
 * saturation, and brightness (HSB).
 * <p>
 * The `scaleEpsilon` is a Double retrieved from the application properties, used
 * to define the allowable deviation or tolerance level when scaling elements within
 * the grid. The specifics of the value are tailored to the application's needs.
 * <p>
 * The `fences` represents a predefined character pattern, used as a static string,
 * possibly describing constraints or delimiters in grid configurations.
 * <p>
 * The `defaultColoursBorder` is a String representing default border colours,
 * potentially used for defining default colour schemes for grid elements. The
 * format and interpretation are subject to application requirements.
 * <p>
 * The `hsbThreshold` is a String value that defines a threshold level for hue,
 * saturation, and brightness, critical for operations pertaining to colour
 * validation and rendering processes within the grid.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MissingJavadoc"})
public record Validation() {

    public static final double scaleEpsilon = _GRID_VALIDATION_SCALE_EPSILON.get();
    public static final String fences = _GRID_VALIDATION_FENCES.get(); // "9"
    public static final String defaultColoursBorder = _GRID_VALIDATION_DEFAULT_BORDER.get(); // "1/999"
    public static final String hsbThreshold = _GRID_VALIDATION_HSB_THRESHOLD.get(); // "1/99";

}
