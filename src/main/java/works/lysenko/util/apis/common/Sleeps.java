package works.lysenko.util.apis.common;

/**
 * A1 class representing sleep-related functionality.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter"})
public interface Sleeps {

    /**
     * Sleep during defined amount of milliseconds
     *
     * @param duration amount of milliseconds to wait
     */
    void sleep(long duration);

    /**
     * Sleep during defined amount of milliseconds with optional custom message
     *
     * @param duration amount of milliseconds to wait
     * @param message  custom message to post into the execution log
     */
    void sleep(long duration, String message);

    /**
     * Sleep during defined amount of milliseconds with optional custom message and
     * optional bypassing of logging
     *
     * @param duration amount of milliseconds to pause for
     * @param message  text to display in the execution log, can be set to null for default
     *                 "Sleeping X ms" message
     * @param silent   whether to bypass output to test log. Useful for short delays
     */
    void sleep(long duration, String message, boolean silent);

    /**
     * Sleep during defined amount of milliseconds with optional bypassing of
     * logging
     *
     * @param duration amount of milliseconds to wait
     * @param silent   whether to bypass logging
     */
    void sleep(long duration, boolean silent);

    /**
     * Pauses the execution for a long duration.
     *
     * @param times the number of times to pause
     */
    void sleepLong(int times);

    /**
     * Pauses the execution for a long duration.
     */
    void sleepLong();

    /**
     * Medium sleep
     */
    void sleepMedium();

    /**
     * Short sleep
     */
    void sleepShort();
}
