package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._LOGS_DEBUG;

/**
 * Diagnostic and execution mode flags.
 */
public record Is() {

    /**
     * Indicates whether debug logging mode is enabled.
     */
    public static final boolean debug = _LOGS_DEBUG.get();
}
