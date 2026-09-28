package works.lysenko.util.prop.logs;

import works.lysenko.util.data.enums.Severity;

import static works.lysenko.util.spec.PropEnum._LONG_OPERATION_SEVERITY;
import static works.lysenko.util.spec.PropEnum._LONG_OPERATION_THRESHOLD;

/**
 * Represents a class for handling configurations related to long operations.
 * This class provides static constants for threshold and severity configurations.
 * <p>
 * The `threshold` constant defines the numerical threshold to determine if an operation
 * qualifies as a long operation. The value is defined in the application properties
 * and retrieved via `_LONG_OPERATION_THRESHOLD.get()`.
 * <p>
 * The `severity` constant specifies the severity level associated with long operations.
 * It utilizes the `Severity` enum to represent the level and is resolved using the application
 * configuration `_LONG_OPERATION_SEVERITY.get()`.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticMethodOnlyUsedInOneClass", "MissingJavadoc"
})
public record LongOperation() {

    public static final int threshold = _LONG_OPERATION_THRESHOLD.get();
    public static final Severity severity = Severity.values()[(int) _LONG_OPERATION_SEVERITY.get()];
}
