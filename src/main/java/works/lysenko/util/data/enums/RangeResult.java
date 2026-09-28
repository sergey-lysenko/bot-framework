package works.lysenko.util.data.enums;

import works.lysenko.util.apis.grid.v._ValidationResult;

import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;

/**
 * Represents the possible outcomes of a range validation.
 */
@SuppressWarnings("MissingJavadoc")
public enum RangeResult implements _ValidationResult {

    OK,
    WITHIN_STATIC_MARGIN,
    WITHIN_DYNAMIC_MARGIN,
    NOT_OK;

    @Override
    public boolean isPassed() {

        return (FAILED > ordinal());
    }

    public String render() {

        return isPassed() ? gb(name()) : rb(name());
    }
}