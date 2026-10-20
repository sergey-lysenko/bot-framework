package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_BINDS;
import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_CLICKS;
import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_LONG_CLICKS;
import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_PIXELS;
import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_SWIPES;

/**
 * Configuration for automated screenshot capture during user interactions.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "StaticNonFinalField"})
public record Screenshot() {

    /**
     * Indicates whether screenshots should be captured for bind actions.
     */
    public static boolean binds = _SCREENSHOTS_OF_BINDS.get();

    /**
     * Indicates whether screenshots should be captured for click actions.
     */
    public static boolean clicks = _SCREENSHOTS_OF_CLICKS.get();

    /**
     * Indicates whether screenshots should be captured for long click actions.
     */
    public static boolean longClicks = _SCREENSHOTS_OF_LONG_CLICKS.get();

    /**
     * Indicates whether screenshots should be captured for pixel-level evaluations.
     */
    public static boolean pixels = _SCREENSHOTS_OF_PIXELS.get();

    /**
     * Indicates whether screenshots should be captured for swipe actions.
     */
    public static boolean swipes = _SCREENSHOTS_OF_SWIPES.get();

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {
        binds = _SCREENSHOTS_OF_BINDS.get();
        clicks = _SCREENSHOTS_OF_CLICKS.get();
        longClicks = _SCREENSHOTS_OF_LONG_CLICKS.get();
        pixels = _SCREENSHOTS_OF_PIXELS.get();
        swipes = _SCREENSHOTS_OF_SWIPES.get();
    }
}
