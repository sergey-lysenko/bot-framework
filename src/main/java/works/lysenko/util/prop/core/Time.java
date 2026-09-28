package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._PAUSE_LENGTH;

/**
 * Represents a class to manage time properties for the application.
 * It provides static properties related to pause length and timestamp format.
 */
@SuppressWarnings({"MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "NonFinalStaticVariableUsedInClassInitialization"})
public record Time() {

    public static final int pauseLength = _PAUSE_LENGTH.get();
}
