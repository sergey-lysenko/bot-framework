package works.lysenko.util.prop.core;

import works.lysenko.util.spec.PropEnum;

import java.awt.Color;

import static works.lysenko.util.spec.PropEnum._SWIPE_LINE_MARKER_COLOUR;
import static works.lysenko.util.spec.PropEnum._SWIPE_START_MARKER_COLOUR;
import static works.lysenko.util.spec.PropEnum._SWIPE_STOP_MARKER_COLOUR;

/**
 * Represents a class to manage swipe properties for the application.
 * <p>
 * This class provides the colours for the start, stop, and line markers used in a swipe action.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "NonFinalStaticVariableUsedInClassInitialization"})
public record Swipe() {

    /**
     * Represents the colour used for the start marker in swipe gesture interactions.
     * The value is dynamically retrieved from the application properties, providing
     * flexibility and ensuring a consistent appearance across the application.
     */
    public static final Color start = _SWIPE_START_MARKER_COLOUR.get();

    /**
     * Represents the colour used for the stop marker in swipe gesture interactions.
     * The value is dynamically retrieved from the application properties, allowing customisation
     * and ensuring a consistent appearance across the application.
     */
    public static final Color stop = _SWIPE_STOP_MARKER_COLOUR.get();

    /**
     * Represents the colour used for drawing or highlighting the swipe line marker.
     * This constant provides a customisable value dynamically fetched from the application
     * properties, ensuring consistent design and flexibility in swipe gesture interactions.
     */
    public static final Color line = _SWIPE_LINE_MARKER_COLOUR.get();

    /**
     * Represents the default reduction value applied in swipe gesture interactions.
     * This constant is typically used to define the standard reduction factor that should be
     * applied when a swipe gesture occurs.
     * A1 negative value indicates the swipe is in the opposite direction.
     */
    public static final float defaultReduction = PropEnum._SWIPES_DEFAULT_REDUCTION.get();

    /**
     * Represents a medium reduction value used during swipe gesture interactions.
     * The constant defines an intermediate reduction factor between the default and large reduction
     * factors, typically applied in scenarios where a moderate reduction effect is needed.
     * This value is dynamically retrieved from a property configuration, allowing flexibility
     * in its definition and adjustment.
     */
    public static final float mediumReduction = PropEnum._SWIPES_MEDIUM_REDUCTION.get();

    /**
     * Represents a significant reduction value applied in swipe gesture interactions.
     * This constant is typically used to define a larger than usual reduction factor
     * that should be applied under specific conditions or gestures where a more
     * pronounced reduction is desired.
     * A1 negative value indicates the swipe is in the opposite direction.
     */
    public static final float bigReduction = PropEnum._SWIPES_BIG_REDUCTION.get();


    /**
     * Represents the default weight value used in swipe gesture interactions.
     * This constant defines the standard weight factor applied during the evaluation
     * or processing of swipe gestures where a neutral weight is desired.
     */
    public static final float defaultWeight = PropEnum._SWIPES_DEFAULT_WEIGHT.get();

}
