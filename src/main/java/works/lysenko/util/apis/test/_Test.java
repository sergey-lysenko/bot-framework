package works.lysenko.util.apis.test;

import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.exception.unchecked.ScenarioRuntimeException;
import works.lysenko.util.data.enums.ExitCode;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.enums.ExitCode.SUCCESS;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.core.Assertions.еггог;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Symbols._DASH_;

/**
 * The _Test interface represents a class that performs test tests and manages execution status and scenarios.
 * It provides methods to configure and execute tests, retrieve status and associated objects, and handle exceptions.
 * The specific implementation details should be documented in the class that implements this interface.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "unused"})
public interface _Test {

    /**
     * Exits the system with a specified exit code.
     *
     * @param status The exit code status.
     */
    @SuppressWarnings("CallToSystemExit")
    private static void exit(final ExitCode status) {

        System.exit(status.getCode());
    }

    private static void printError(final ExitCode status) {

        final String dashes = s(_DASH_).repeat(status.name().length());
        еггог(EMPTY);
        еггог(dashes);
        еггог(status.name());
        еггог(dashes);
    }

    /**
     * Prints the stack trace and error message of a Throwable object and exits the system with a specified exit code.
     *
     * @param e      The Throwable object to be printed.
     * @param status The exit code status to exit the system with.
     */
    private static void printErrorAndExit(final Throwable e, final ExitCode status) {

        printThrowable(e);
        printExitCode(status);
        exit(status);
    }

    /**
     * Prints the exit code if it is not successful.
     *
     * @param status The exit code status.
     */
    static void printExitCode(final ExitCode status) {

        if (SUCCESS != status) {
            printError(status);
        }
    }

    /**
     * Prints the stack trace and error message of a Throwable object.
     *
     * @param e The Throwable object to be printed.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    static void printThrowable(final Throwable e) {

        if (isNotNull(e)) {
            e.printStackTrace();
            еггог(EMPTY);
            еггог(e.getMessage());
        }
    }

    /**
     * Processes the code based on the given exit code status.
     *
     * @param status The exit code status.
     */
    static void processCode(final ExitCode status) {

        printErrorAndExit(null, status);
    }

    /**
     * Performs the following routines before ending test execution: - stats() - write
     * execution summary to the console - jsonStats() - write test execution summary
     * in JSON format (for following import into ELK) - conditionalClose() - close
     * browser window if not in CI
     */
    void complete();

    /**
     * Executes the current tests.
     * This method is responsible for executing the tests and handling any exceptions that may occur during execution.
     *
     * @throws SafeguardException       if a safeguard exception occurs during execution
     * @throws ScenarioRuntimeException if an exception occurs during the execution of a scenario
     */
    @SuppressWarnings("ProhibitedExceptionDeclared")
    void execute() throws Exception;

    /**
     * Retrieves the scenarios executor object associated with the Test object.
     *
     * @return The scenarios executor object.
     */
    _Exec executor();

    /**
     * Retrieves the exception handler for handling exceptions.
     *
     * @return The exception handler.
     */
    _ExceptionsHandler handler();

    /**
     * Retrieves the statistics object associated with the current test execution.
     *
     * @return The statistics object that provides methods for collecting and retrieving test-related statistics.
     */
    _Stat statistics();

    /**
     * Retrieves the postflight action associated with the Test object.
     *
     * @return The postflight action.
     */
    Runnable postflight();

    /**
     * Retrieves the preflight action associated with the Test object.
     *
     * @return The postflight action.
     */
    Runnable preflight();

    /**
     * Retrieves the Repeater object associated with the Test object.
     *
     * @return The Repeater object.
     */
    _Repeater repeater();

    /**
     * Retrieves the status of the current execution.
     *
     * @return The status of the current execution as a String.
     */
    String getStatus();

    /**
     * Checks if the execution is stopping.
     *
     * @return true if the execution is stopping, false otherwise.
     */
    boolean isStopRequestedByDashboard();

    /**
     * Processes a checked exception by printing the exception stack trace and message,
     * and exiting the system with the specified exit code.
     *
     * @param e      The checked exception to be processed.
     * @param status The exit code status to exit the system with.
     */
    default void processCheckedException(final Exception e, final ExitCode status) {

        printErrorAndExit(e, status);
    }

    /**
     * Processes an error by printing the stack trace to the console, outputting the error message,
     * and exiting the system with a specified status code.
     *
     * @param e      The error to be processed.
     * @param status The exit code status to exit the system with.
     */
    default void processError(final Error e, final ExitCode status) {

        printErrorAndExit(e, status);
    }

    /**
     * Performs processing for a runtime exception and exits the system with a specified status code.
     * The exception stack trace is printed to the console before exiting.
     *
     * @param e      The runtime exception to process.
     * @param status The status code to exit the system with.
     */
    default void processUncheckedException(final Exception e, final ExitCode status) {

        printErrorAndExit(e, status);
    }

    /**
     * Stop tests
     */
    void stop();
}
