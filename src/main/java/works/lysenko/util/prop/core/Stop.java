package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._FAIL_BY_EVENT_OF_SEVERITY;

/**
 * Represents a class to manage stop properties for the application.
 * <p>
 * Provides the severity level at which the application should stop.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record Stop() {

    public static final Integer atSeverity = _FAIL_BY_EVENT_OF_SEVERITY.get();
}
