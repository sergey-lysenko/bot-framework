package works.lysenko.util.prop.grid;

import works.lysenko.util.data.enums.Severity;

import static works.lysenko.util.spec.PropEnum._EMPTY_RESULT_SEVERITY;
import static works.lysenko.util.spec.PropEnum._NULL_RESULT_SEVERITY;

/**
 * A1 class representing a long operation with threshold and severity properties.
 * <p>
 * Provides a static integer `threshold` representing the limit or threshold
 * for the long operation and a static Severity object `severity` representing
 * the severity level of the long operation.
 * The values for these properties are retrieved using a properties instance.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticMethodOnlyUsedInOneClass", "MissingJavadoc"
})
public record Result() {

    public static final Severity nullSeverity = Severity.values()[(int) _NULL_RESULT_SEVERITY.get()];
    public static final Severity emptySeverity = Severity.values()[(int) _EMPTY_RESULT_SEVERITY.get()];
}
