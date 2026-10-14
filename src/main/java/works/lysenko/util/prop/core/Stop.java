package works.lysenko.util.prop.core;

import works.lysenko.util.data.enums.Severity;

import static works.lysenko.util.prop.PropEnum._TEST_FAIL_BY_EVENT_OF_SEVERITY;

/**
 * Execution termination threshold configuration.
 */
public record Stop() {

    /**
     * Minimum event severity level that will trigger immediate termination of the test run.
     */
    public static final Severity atSeverity = _TEST_FAIL_BY_EVENT_OF_SEVERITY.get();
}
