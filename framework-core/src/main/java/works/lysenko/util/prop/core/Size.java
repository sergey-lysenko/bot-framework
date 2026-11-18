package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._SCREENSHOTS_OF_LONG_CLICKS_MARKER_DIAMETER;

/**
 * Size configurations for UI markers and overlays.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Size() {

    /**
     * Diameter (in pixels) of the marker rendered on screenshots for long click actions.
     */
    public static final int longClicksMarker = _SCREENSHOTS_OF_LONG_CLICKS_MARKER_DIAMETER.get();
}
