package works.lysenko.util.data.enums;

import works.lysenko.util.apis.grid.v._ValidationResult;

import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;

/**
 * Represents the result of a validation process for colours.
 * This enumerator defines various states that a colours validation
 * can result in and provides methods to determine the result status
 * and render it in a specific format.
 */
public enum ColoursValidationResult implements _ValidationResult {

    /**
     * Color validation passed successfully.
     */
    OK,

    /**
     * Color validation is within the static margin.
     */
    WITHIN_STATIC_MARGIN,

    /**
     * Color validation is within the dynamic margin.
     */
    WITHIN_DYNAMIC_MARGIN,

    /**
     * Color validation failed.
     */
    NOT_OK,

    /**
     * Ranger component validation failed.
     */
    RANGER_FAILED;

    @Override
    public boolean isPassed() {

        return (FAILED > ordinal());
    }

    /**
     * Renders the enum name wrapped in green bold if passed, or red bold if failed.
     *
     * @return colorized string representation of the result
     */
    public String render() {

        return isPassed() ? gb(name()) : rb(name());
    }
}
