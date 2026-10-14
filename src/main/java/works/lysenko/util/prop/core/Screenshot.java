package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_BINDS;
import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_CLICKS;
import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_LONG_CLICKS;
import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_PIXELS;
import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_SWIPES;

/**
 * Configuration for automated screenshot capture during user interactions.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Screenshot() {

    /**
     * Indicates whether screenshots should be captured for bind actions.
     */
    public static final boolean binds = _SCREENSHOTS_OF_BINDS.get();

    /**
     * Indicates whether screenshots should be captured for click actions.
     */
    public static final boolean clicks = _SCREENSHOTS_OF_CLICKS.get();

    /**
     * Indicates whether screenshots should be captured for long click actions.
     */
    public static final boolean longClicks = _SCREENSHOTS_OF_LONG_CLICKS.get();

    /**
     * Indicates whether screenshots should be captured for pixel-level evaluations.
     */
    public static final boolean pixels = _SCREENSHOTS_OF_PIXELS.get();

    /**
     * Indicates whether screenshots should be captured for swipe actions.
     */
    public static final boolean swipes = _SCREENSHOTS_OF_SWIPES.get();
}
