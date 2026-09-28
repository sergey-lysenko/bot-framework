package works.lysenko.util.data.enums;

import works.lysenko.util.apis.grid.v._ValidationResult;

import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;

/**
 * Enum representing the possible results of a Ranger validation.
 */
@SuppressWarnings("MissingJavadoc")
public enum RangerResult implements _ValidationResult {

    OK,
    WITHIN_STATIC_MARGIN,
    WITHIN_DYNAMIC_MARGIN,
    NOT_OK,

    ABSENT_EXPECTATIONS,
    EMPTY_EXPECTATIONS,
    NO_CORRESPONDING_SHARE,
    INVALID_MAXIMUM_AMOUNT,
    INVALID_MINIMUM_AMOUNT,
    LESS_THAN_MINIMUM_EXPECTATIONS,
    THRESHOLD_TOO_HIGH,
    TOO_FEW_ACTUAL,
    TOO_FEW_EXPECTATIONS,
    TOO_MUCH_ACTUAL,
    UNEQUAL_ACTUAL_AND_EXPECTED,
    UNEXPECTED_ACTUAL,
    WRONG_ACTUAL_AMOUNT;

    public boolean isPassed() {

        return (FAILED > ordinal());
    }

    public String render() {

        return isPassed() ? gb(name()) : rb(name());
    }
}