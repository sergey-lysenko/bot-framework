package works.lysenko.util.prop.data;

import static works.lysenko.util.spec.PropEnum._PERSIST;

/**
 * Represents a persistence setting for the application.
 * <p>
 * This class provides a static boolean property which indicates if data persistence is enabled.
 * <p>
 * The static property is retrieved from the application properties using the predefined key.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MissingJavadoc", "NonFinalStaticVariableUsedInClassInitialization"})
public record Persist() {

    public static final Boolean data = _PERSIST.get();
}
