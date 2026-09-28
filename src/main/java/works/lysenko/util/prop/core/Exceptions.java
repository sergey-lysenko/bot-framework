package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._EXCEPTION_RETRIES;

/**
 * Record class for handling exceptions and assertions.
 */
public record Exceptions() {

    /**
     * Represents the number of retries for a specific operation.
     */
    public static final Integer retries = _EXCEPTION_RETRIES.get();
}
