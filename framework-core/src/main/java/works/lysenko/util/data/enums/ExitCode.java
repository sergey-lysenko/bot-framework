package works.lysenko.util.data.enums;

import works.lysenko.util.apis.action.ProvidesCode;

/**
 * The ExitCode enum contains constants that represent different exit code values for a program.
 * These exit codes can be used to communicate the status of the program to the calling environment.
 */
@SuppressWarnings("EnumClass")
public enum ExitCode implements ProvidesCode {

    /**
     * Program exited successfully (0).
     */
    SUCCESS(0),

    /**
     * Program encountered an unhandled exception (10).
     */
    UNHANDLED_EXCEPTION(10),

    /**
     * Tests encountered an unhandled exception (11).
     */
    TESTS_UNHANDLED_EXCEPTION(11),

    /**
     * Assertion failure occurred (12).
     */
    FAILED_ASSERTION(12),

    /**
     * Safeguard check failed (13).
     */
    SAFEGUARD_FAILED(13),

    /**
     * Scenario execution failed (14).
     */
    SCENARIO_FAILED(14),

    /**
     * General execution failure (20).
     */
    EXECUTION_FAILURE(20),

    /**
     * Undefined browser configuration (30).
     */
    UNDEFINED_BROWSER(30),

    /**
     * Program was closed through the GUI (31).
     */
    CLOSED_THROUGH_GUI(31),

    /**
     * Platforms configuration was reset (32).
     */
    PLATFORMS_RESET(32);

    private final int code;

    ExitCode(final int code) {

        this.code = code;
    }

    @Override
    public int getCode() {

        return code;
    }
}
