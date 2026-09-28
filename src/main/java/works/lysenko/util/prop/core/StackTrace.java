package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum.*;

/**
 * Represents a stack trace configuration for the application.
 * <p>
 * This class provides static Boolean properties to indicate various stack trace settings.
 * The class includes:
 * - full: Determines if the full stack trace should be used.
 * - numbers: Indicates if line numbers should be included in short stack traces.
 * - references: Indicates if stack trace references should be included.
 * - threshold: Determines the threshold for stack traces.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record StackTrace() {

    public static final Boolean full = _FULL_STACKTRACE.get();
    public static final Boolean numbers = _LINE_NUMBER_IN_SHORT_STACKTRACE.get();
    public static final Boolean references = _STACK_TRACE_REFERENCES.get();
}
