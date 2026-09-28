package works.lysenko.util.prop.grid;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.records.RangedMargin;

import static works.lysenko.util.data.records.RangedMargin.rm;
import static works.lysenko.util.spec.PropEnum.*;

/**
 * Representation of the allowed deviation for colour values during grid image validation
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "NonFinalStaticVariableUsedInClassInitialization"})
public record Allowed() {

    /**
     * Represents the allowed margin for deviations in colour values during grid image validation.
     * This static final field is retrieved from the application properties as a String object.
     * It defines the permissible tolerance for colour variations across the grid, ensuring
     * that validation processes accommodate acceptable discrepancies in colour rendering.
     */
    public static final RangedMargin defaultRangedMargin = rm((String) _DEFAULT_COLOUR_RANGED_MARGIN.get());

    /**
     * Represents the allowed margin for variations in polychromy during grid image validation.
     * This static final field is retrieved from application properties as a Fraction object.
     * It defines the tolerance level for polychromatic variations, allowing for discrepancies
     * in colour composition across the grid, to ensure the validation process accounts for
     * permissible deviations.
     */
    public static final Fraction defaultPolychromyMargin = _DEFAULT_POLYCHROMY_MARGIN.get();

    /**
     * Represents the allowed deviation for hue, saturation, and value (HSV) during grid image validation.
     * This static final field retrieves its value from application properties.
     * It defines the tolerance level for HSV colour components to ensure accurate validation,
     * allowing slight discrepancies between expected and actual colours within the grid.
     */
    public static final Fraction defaultHSVmargin = _DEFAULT_HSV_MARGIN.get();

    /**
     * A1 boolean flag indicating whether empty grid validation is allowed.
     * <p>
     * This static final field retrieves its value from the application properties
     * and is used to determine if grid validation should be bypassed. When set
     * to true, grid validation for empty grids is not performed; otherwise, grid
     * validation is executed based on predefined criteria.
     */
    public static final Boolean noValidation = _EMPTY_GRID_VALIDATION_ALLOWED.get();

    /**
     * Represents the amount by which the margin is to be reduced during grid image validation.
     * This constant is defined as an integer value, initialised to 'TWO'. It is used to adjust
     * the allowable deviation for various margin calculations within the validation process.
     */
    public static final int marginSeverityReduction = _ALLOWED_MARGIN_SEVERITY_REDUCTION.get();

    /**
     * Represents the reduction factor applied to secondary or non-critical severity margins
     * during validation processes.
     * <p>
     * This static final field retrieves its value from application properties
     * and defines the permissible adjustment for other severity settings, enabling
     * customization of tolerance levels based on predefined configuration.
     */
    public static final int otherSeverityReduction = _ALLOWED_OTHER_SEVERITY_REDUCTION.get();
}
