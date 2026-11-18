package works.lysenko.util.apis.common;

/**
 * Routines for controlling of test tests
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "unused"})
public interface ControlsExecution {

    /**
     * Restart application in case of Android testing
     *
     * @param pause to make before activation of the app
     */
    void restartApp(long pause);

    /**
     * Do not start any consequent tests
     */
    void stopTests();

    /**
     * Do not start any consequent tests, recording a specific stop reason.
     *
     * @param reason description of why tests are stopped
     */
    default void stopTests(final String reason) {

        stopTests();
    }

    /**
     * Switch to other WebDriver window
     *
     * @param window handle to switch to
     */
    void switchTo(String window);
}
