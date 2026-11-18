package works.lysenko.util.data.enums;

import works.lysenko.util.apis.grid.v._ValidationResult;

import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;

/**
 * Represents the possible outcomes of a range validation.
 */
public enum RangeResult implements _ValidationResult {

    /**
     * Range check passed.
     */
    OK,

    /**
     * Range check within static margin.
     */
    WITHIN_STATIC_MARGIN,

    /**
     * Range check within dynamic margin.
     */
    WITHIN_DYNAMIC_MARGIN,

    /**
     * Range check failed.
     */
    NOT_OK;

    @Override
    public boolean isPassed() {

        return (FAILED > ordinal());
    }

    /**
     * Renders formatted status string.
     *
     * @return green bold if passed, red bold if failed
     */
    public String render() {

        return isPassed() ? gb(name()) : rb(name());
    }
}
