package works.lysenko.util.data.enums;

import works.lysenko.util.apis.grid.v._ValidationResult;

import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;

/**
 * Enum representing the result of a grid operation or validation.
 */
public enum GridValidationResult implements _ValidationResult {

    /**
     * Grid validation passed successfully.
     */
    OK,

    /**
     * Grid validation within static margin.
     */
    WITHIN_STATIC_MARGIN,

    /**
     * Grid validation within dynamic margin.
     */
    WITHIN_DYNAMIC_MARGIN,

    /**
     * Grid validation failed.
     */
    NOT_OK,

    /**
     * General validation issue occurred.
     */
    VALIDATION_ISSUE,

    /**
     * Invalid grid configuration.
     */
    INVALID_GRID,

    /**
     * Invalid geometry configuration.
     */
    INVALID_GEOMETRY,

    /**
     * Invalid grid location specified.
     */
    INVALID_GRID_LOCATION,

    /**
     * Invalid request parameters.
     */
    INVALID_REQUEST,

    /**
     * Colours input is null.
     */
    COLOURS_ARE_NULL,

    /**
     * Colours validation issue encountered.
     */
    COLOURS_VALIDATION_ISSUE,

    /**
     * Palette comparison failed.
     */
    PALETTE_FAILED,

    /**
     * Polychromy comparison failed.
     */
    POLYCHROMY_FAILED,

    /**
     * Hue check failed.
     */
    HUE_FAILED,

    /**
     * Saturation check failed.
     */
    SATURATION_FAILED,

    /**
     * Brightness check failed.
     */
    BRIGHTNESS_FAILED;

    /**
     * Copies the ordinal value of the provided Enum and returns a corresponding GridValidationResult.
     *
     * @param coloursResult the Enum value to copy the ordinal from
     * @return the GridValidationResult corresponding to the ordinal value of the Enum
     */
    public static GridValidationResult copyOrdinalFrom(final Enum<?> coloursResult) {

        return values()[coloursResult.ordinal()];
    }

    @Override
    public boolean isPassed() {

        return (FAILED > ordinal());
    }

    @Override
    public String render() {

        return isPassed() ? gb(name()) : rb(name());
    }
}
