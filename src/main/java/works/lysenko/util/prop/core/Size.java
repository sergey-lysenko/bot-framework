package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._SCREENSHOTS_OF_LONG_CLICKS_MARKER_DIAMETER;

/**
 * Represents the size of an object or element.
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
public record Size() {

    /**
     * Represents a marker for long clicks.
     */
    public static final int longClicksMarker = _SCREENSHOTS_OF_LONG_CLICKS_MARKER_DIAMETER.get();
}
