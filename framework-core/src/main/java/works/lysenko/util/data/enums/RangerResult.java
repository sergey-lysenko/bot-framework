package works.lysenko.util.data.enums;

import works.lysenko.util.apis.grid.v._ValidationResult;

import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;

/**
 * Enum representing the possible results of a Ranger validation.
 */
public enum RangerResult implements _ValidationResult {

    /**
     * Ranger validation passed.
     */
    OK,

    /**
     * Ranger validation within static margin.
     */
    WITHIN_STATIC_MARGIN,

    /**
     * Ranger validation within dynamic margin.
     */
    WITHIN_DYNAMIC_MARGIN,

    /**
     * Ranger validation failed.
     */
    NOT_OK,

    /**
     * Expectations are absent.
     */
    ABSENT_EXPECTATIONS,

    /**
     * Expectations are empty.
     */
    EMPTY_EXPECTATIONS,

    /**
     * No corresponding share found.
     */
    NO_CORRESPONDING_SHARE,

    /**
     * Maximum amount is invalid.
     */
    INVALID_MAXIMUM_AMOUNT,

    /**
     * Minimum amount is invalid.
     */
    INVALID_MINIMUM_AMOUNT,

    /**
     * Expectations are less than minimum.
     */
    LESS_THAN_MINIMUM_EXPECTATIONS,

    /**
     * Threshold setting is too high.
     */
    THRESHOLD_TOO_HIGH,

    /**
     * Too few actual values.
     */
    TOO_FEW_ACTUAL,

    /**
     * Too few expectations defined.
     */
    TOO_FEW_EXPECTATIONS,

    /**
     * Too much actual values.
     */
    TOO_MUCH_ACTUAL,

    /**
     * Actual and expected amounts are unequal.
     */
    UNEQUAL_ACTUAL_AND_EXPECTED,

    /**
     * Unexpected actual value encountered.
     */
    UNEXPECTED_ACTUAL,

    /**
     * Wrong actual amount.
     */
    WRONG_ACTUAL_AMOUNT;

    @Override
    public boolean isPassed() {

        return (FAILED > ordinal());
    }

    @Override
    public String render() {

        return isPassed() ? gb(name()) : rb(name());
    }
}
