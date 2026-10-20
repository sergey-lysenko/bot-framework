package works.lysenko.util.prop.logs;

import static works.lysenko.util.prop.PropEnum._LOGS_ADEBUG;
import static works.lysenko.util.prop.PropEnum._LOGS_DEBUG;
import static works.lysenko.util.prop.PropEnum._LOGS_TRACE;

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
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "NonFinalStaticVariableUsedInClassInitialization", "MissingJavadoc", "StaticNonFinalField"})
public record Debug() {

    public static Boolean adebug = _LOGS_ADEBUG.get();
    public static Boolean debug = _LOGS_DEBUG.get();
    public static Boolean trace = _LOGS_TRACE.get();

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {
        adebug = _LOGS_ADEBUG.get();
        debug = _LOGS_DEBUG.get();
        trace = _LOGS_TRACE.get();
    }
}
