package works.lysenko.util.data.enums;

import works.lysenko.util.apis.action.ProvidesCode;

/**
 * The ExitCode class contains constants that represent different exit code values for a program.
 * These exit codes can be used to communicate the status of the program to the calling environment.
 */
@SuppressWarnings({"MissingJavadoc", "EnumClass"})
public enum ExitCode implements ProvidesCode {


    SUCCESS(0),
    UNHANDLED_EXCEPTION(10),
    TESTS_UNHANDLED_EXCEPTION(11),
    FAILED_ASSERTION(12),
    SAFEGUARD_FAILED(13),
    SCENARIO_FAILED(14),
    EXECUTION_FAILURE(20),
    UNDEFINED_BROWSER(30),
    CLOSED_THROUGH_GUI(31),
    PLATFORMS_RESET(32);

    private final int code;

    ExitCode(final int code) {

        this.code = code;
    }

    public int getCode() {

        return code;
    }
}
