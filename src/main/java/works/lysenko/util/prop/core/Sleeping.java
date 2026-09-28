package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._MAX_TYPING_DELAY;
import static works.lysenko.util.spec.PropEnum._SILENT_SLEEPING_THRESHOLD;

/**
 * Represents configuration settings related to the sleeping mechanism in the application.
 * <p>
 * This class contains static properties used to manage thresholds and delays for sleeping states.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Sleeping() {

    /**
     * Represents the threshold for activating silent sleeping modes.
     *
     * <p>
     * This value is retrieved from the application properties using the
     * class type {@code Integer} and the key defined by the constant
     * {@code _SILENT_SLEEPING_THRESHOLD}.
     * </p>
     *
     * <p>
     * The {@code threshold} specifies the point at which the application
     * decides to enter a silent sleeping state, helping to manage resource
     * usage and performance.
     * </p>
     */

    public static final int threshold = _SILENT_SLEEPING_THRESHOLD.get();
    /**
     * Represents the maximum delay allowed between typing actions in milliseconds.
     * <p>
     * This variable helps manage the typing speed by setting an upper limit on the delay
     * between keystrokes, ensuring consistency and preventing excessive delay that could
     * degrade user experience.
     */
    public static final int maxTypingDelay = _MAX_TYPING_DELAY.get();
}
