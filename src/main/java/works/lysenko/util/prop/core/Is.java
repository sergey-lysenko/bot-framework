package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._DEBUG;

/**
 * Record class for handling exceptions and assertions.
 */
public record Is() {

    /**
     * Represents the number of retries for a specific operation.
     */
    public static final boolean debug = _DEBUG.get();
}
