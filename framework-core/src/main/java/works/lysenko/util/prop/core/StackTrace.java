package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._LOGS_FULL_STACKTRACE;
import static works.lysenko.util.prop.PropEnum._LOGS_LINE_NUMBER_IN_SHORT_STACKTRACE;
import static works.lysenko.util.prop.PropEnum._LOGS_STACK_TRACE_REFERENCES;

/**
 * Stack trace formatting configuration for application logs.
 */
public record StackTrace() {

    /**
     * Indicates whether complete stack traces should be output instead of shortened ones.
     */
    public static final Boolean full = _LOGS_FULL_STACKTRACE.get();

    /**
     * Indicates whether line numbers should be included in shortened stack traces.
     */
    public static final Boolean numbers = _LOGS_LINE_NUMBER_IN_SHORT_STACKTRACE.get();

    /**
     * Indicates whether stack trace source references and links should be included.
     */
    public static final Boolean references = _LOGS_STACK_TRACE_REFERENCES.get();
}
