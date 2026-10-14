package works.lysenko.util.prop.core;

import works.lysenko.util.data.enums.Severity;

import static works.lysenko.util.prop.PropEnum._TEST_ASSERTIONS_PRODUCE_EXCEPTION;
import static works.lysenko.util.prop.PropEnum._TEST_ASSERTIONS_SEVERITY;

/**
 * Configuration for assertion behavior and error reporting during test execution.
 */
public record Assertions() {

    /**
     * Indicates whether failed assertions should throw exceptions.
     */
    public static final Boolean exception = _TEST_ASSERTIONS_PRODUCE_EXCEPTION.get();

    /**
     * Severity level assigned to assertion failures when logged.
     */
    public static final Severity severity = _TEST_ASSERTIONS_SEVERITY.get();
}
