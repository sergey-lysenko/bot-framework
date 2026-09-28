package works.lysenko.util.prop.logs;

import static works.lysenko.util.spec.PropEnum._ADEBUG;
import static works.lysenko.util.spec.PropEnum._DEBUG;
import static works.lysenko.util.spec.PropEnum._TRACE;

/**
 * Represents a class for debugging purposes.
 * Provides static boolean properties for enabling debug, trace, and additional debug flags.
 * <p>
 * The class includes the following static properties:
 * - adebug: Indicates whether appium debug information is enabled.
 * - debug: Indicates whether regular debugging is enabled.
 * - trace: Indicates whether tracing is enabled.
 * <p>
 * This class interacts with a properties instance to retrieve boolean values based on specific keys.
 * The values for adebug, debug, and trace are obtained using the properties instance.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "NonFinalStaticVariableUsedInClassInitialization", "MissingJavadoc"})
public record Debug() {

    public static final Boolean adebug = _ADEBUG.get();
    public static final Boolean debug = _DEBUG.get();
    public static final Boolean trace = _TRACE.get();
}
