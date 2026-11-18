package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._TEST_EXCEPTION_RETRIES;

/**
 * Configuration for exception retry handling in tests.
 */
public record Exceptions() {

    /**
     * Number of allowed retries when an exception occurs during test operations.
     */
    public static final Integer retries = _TEST_EXCEPTION_RETRIES.get();
}
