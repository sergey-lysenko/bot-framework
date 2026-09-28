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
@SuppressWarnings("MissingJavadoc")
public enum ColoursValidationResult implements _ValidationResult {

    OK,
    WITHIN_STATIC_MARGIN,
    WITHIN_DYNAMIC_MARGIN,
    NOT_OK,

    RANGER_FAILED;

    @Override
    public boolean isPassed() {

        return (FAILED > ordinal());
    }

    public String render() {

        return isPassed() ? gb(name()) : rb(name());
    }
}