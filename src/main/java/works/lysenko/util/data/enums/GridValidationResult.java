package works.lysenko.util.data.enums;

import works.lysenko.util.apis.grid.v._ValidationResult;

import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;

/**
 * Enum representing the result of a grid operation or validation.
 */
@SuppressWarnings("MissingJavadoc")
public enum GridValidationResult implements _ValidationResult {

    OK,
    WITHIN_STATIC_MARGIN,
    WITHIN_DYNAMIC_MARGIN,
    NOT_OK,

    VALIDATION_ISSUE,
    INVALID_GRID,
    INVALID_GEOMETRY,
    INVALID_GRID_LOCATION,
    INVALID_REQUEST,
    COLOURS_ARE_NULL,
    COLOURS_VALIDATION_ISSUE,
    PALETTE_FAILED,
    POLYCHROMY_FAILED,
    HUE_FAILED,
    SATURATION_FAILED,
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

    public boolean isPassed() {

        return (FAILED > ordinal());
    }

    @Override
    public String render() {

        return isPassed() ? gb(name()) : rb(name());
    }
}