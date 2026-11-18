package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._LOGS_SILENT_SLEEPING_THRESHOLD;
import static works.lysenko.util.prop.PropEnum._TEST_MAX_TYPING_DELAY;

/**
 * Configuration settings related to pauses and sleeping mechanisms in the application.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Sleeping() {

    /**
     * Threshold (in milliseconds) below which pauses/sleeps are suppressed from logging.
     */
    public static final int threshold = _LOGS_SILENT_SLEEPING_THRESHOLD.get();

    /**
     * Maximum delay allowed between simulated keystrokes (in milliseconds).
     */
    public static final int maxTypingDelay = _TEST_MAX_TYPING_DELAY.get();
}
