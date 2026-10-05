package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._TEST_EWAIT;
import static works.lysenko.util.spec.PropEnum._WEBD_IWAIT;

/**
 * A1 class to manage wait properties for the application.
 * <p>
 * This class provides static properties for implicit and explicit wait times.
 * The wait times are retrieved from a properties instance based on specific keys.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "NonFinalStaticVariableUsedInClassInitialization", "MissingJavadoc"})
public record Waits() {

    public static final int implicit = _WEBD_IWAIT.get();
    public static final int explicit = _TEST_EWAIT.get();
}
