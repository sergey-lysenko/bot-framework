package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._TEST_PAUSE_LENGTH;

/**
 * Timing configuration for test execution pacing.
 */
public record Time() {

    /**
     * Default duration (in milliseconds) used for intentional pauses between test actions.
     */
    public static final int pauseLength = _TEST_PAUSE_LENGTH.get();
}
