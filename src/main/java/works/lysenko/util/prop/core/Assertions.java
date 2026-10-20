package works.lysenko.util.prop.core;

import works.lysenko.util.data.enums.Severity;

import static works.lysenko.util.prop.PropEnum._TEST_ASSERTIONS_PRODUCE_EXCEPTION;
import static works.lysenko.util.prop.PropEnum._TEST_ASSERTIONS_SEVERITY;

/**
 * Configuration for assertion behavior and error reporting during test execution.
 */
@SuppressWarnings("StaticNonFinalField")
public record Assertions() {

    /**
     * Indicates whether failed assertions should throw exceptions.
     */
    public static Boolean exception = _TEST_ASSERTIONS_PRODUCE_EXCEPTION.get();

    /**
     * Severity level assigned to assertion failures when logged.
     */
    public static Severity severity = _TEST_ASSERTIONS_SEVERITY.get();

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {
        exception = _TEST_ASSERTIONS_PRODUCE_EXCEPTION.get();
        severity = _TEST_ASSERTIONS_SEVERITY.get();
    }
}
