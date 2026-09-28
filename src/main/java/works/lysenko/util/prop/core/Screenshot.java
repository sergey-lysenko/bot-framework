package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum.*;

/**
 * The Screenshot class represents the configuration for taking screenshots in different scenarios.
 * It provides static constants for determining whether to capture screenshots of clicks, long clicks, and swipes.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticMethodOnlyUsedInOneClass"})
public record Screenshot() {

    /**
     * The variable "binds" represents whether the application should capture screenshots of binds.
     */
    public static final boolean binds = _SCREENSHOTS_OF_BINDS.get();

    /**
     * The Screenshot class represents the configuration for taking screenshots in different scenarios.
     * It provides static constants for determining whether to capture screenshots of clicks, long clicks, and swipes.
     */
    public static final boolean clicks = _SCREENSHOTS_OF_CLICKS.get();

    /**
     * The boolean variable "longClicks" represents whether to capture screenshots of long clicks.
     */
    public static final boolean longClicks = _SCREENSHOTS_OF_LONG_CLICKS.get();

    /**
     * The variable "swipes" represents whether to capture screenshots of swipes.
     */
    public static final boolean pixels = _SCREENSHOTS_OF_PIXELS.get();

    /**
     * The variable "swipes" represents whether to capture screenshots of swipes.
     */
    public static final boolean swipes = _SCREENSHOTS_OF_SWIPES.get();
}
