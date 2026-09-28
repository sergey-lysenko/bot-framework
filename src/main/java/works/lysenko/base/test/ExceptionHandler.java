package works.lysenko.base.test;

import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._ExceptionalExecution;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._ExceptionsHandler;
import works.lysenko.util.data.records.test.Triplet;
import works.lysenko.util.spec.Level;

import static java.util.Objects.isNull;
import static works.lysenko.Base.*;
import static works.lysenko.util.apis.exception.Routines.describeTriplet;
import static works.lysenko.util.apis.exception.Routines.extractTriplet;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.lang.A.ATTEMPTING_COUNTER_ACTION;
import static works.lysenko.util.lang.C.CAUGHT_DURING_EXECUTION_OF;
import static works.lysenko.util.lang.E.EXCEPTIONAL_SCENARION;
import static works.lysenko.util.lang.I.IS_TO_BE_EXECUTED;
import static works.lysenko.util.lang.N.NOTHING_TO_DO_NOW;
import static works.lysenko.util.lang.P.PROCESSING_EXCEPTIONAL_STATE;
import static works.lysenko.util.lang.U.UNCAUGHT_EXCEPTION;
import static works.lysenko.util.lang.word.E.EXCEPTION;
import static works.lysenko.util.lang.word.E.EXCEPTIONS;
import static works.lysenko.util.lang.word.M.MOST;
import static works.lysenko.util.lang.word.R.RELEVANT;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._LFD_;

/**
 * The ExceptionHandler class handles exceptions by setting them as the current exception, logging events, setting failure
 * messages, and throwing the exceptions. It also provides methods to determine if there are failing events and to process
 * scenarios.
 */
public class ExceptionHandler implements _ExceptionsHandler {

    private _Scenario exceptional = null;
    private Exception exception = null;
    private Triplet triplet = null;

    public final Exception getException() {

        return exception;
    }

    public final void handle(final RuntimeException exception) throws SafeguardException {

        section(PROCESSING_EXCEPTIONAL_STATE, () -> {
            this.exception = exception;
            triplet = extractTriplet(this.exception);
            printInfo();
        });
        section(ATTEMPTING_COUNTER_ACTION);
        action(exception, exec.scenarios().currentString(true));
    }

    public final void setExceptional(final _Scenario scenario) {

        exceptional = scenario;
    }

    /**
     * Performs an action in response to a provided exception and scenario title.
     * If the exception is null, it logs an event and rethrows the exception.
     * Otherwise, it delegates handling of the exceptional scenario.
     *
     * @param exception     the runtime exception that is the subject of the action
     * @param scenarioTitle the title of the scenario associated with the exception
     * @throws SafeguardException if an error occurs while handling the exceptional scenario
     */
    @SuppressWarnings("ProhibitedExceptionThrown")
    private void action(final RuntimeException exception, final String scenarioTitle) throws SafeguardException {

        if (isNull(exceptional)) {
            logEvent(S1, b(UNCAUGHT_EXCEPTION, exception.getMessage()));
            throw exception;
        } else handleExceptionalScenario(this.exception, scenarioTitle);
    }

    /**
     * Handles an exceptional scenario by performing necessary actions and logging events.
     *
     * @param exception     the exception that occurred
     * @param scenarioTitle the title of the exceptional scenario
     */
    private void handleExceptionalScenario(final Exception exception, final String scenarioTitle) throws SafeguardException {

        logEvent(S1, b(message(exception, scenarioTitle), EXCEPTIONAL_SCENARION, q(exceptional.getName()), IS_TO_BE_EXECUTED));
        try {
            ((_ExceptionalExecution) exceptional).isOk(exception, triplet);
        } catch (final RuntimeException e) {
            logEvent(S0, b(message(e, exceptional.getName()), NOTHING_TO_DO_NOW));
        }
    }

    /**
     * Generates a message describing an exception caught during the execution of a specified scenario.
     *
     * @param exception     the exception that was caught
     * @param scenarioTitle the title of the scenario during which the exception was caught
     * @return a formatted string describing the exception and the scenario context
     */
    private static String message(final Exception exception, final String scenarioTitle) {

        return b(c(EXCEPTION), exception.toString(), CAUGHT_DURING_EXECUTION_OF, s(q(scenarioTitle), _COMMA_));
    }

    /**
     * This method is responsible for printing information related to an exception.
     * It first checks if the debug mode is enabled. If it is enabled, it prints the exception stack trace to the standard
     * error stream.
     * Then, it prints the most relevant exceptions which are extracted from the given exception using the extractTriplet
     * (Throwable) and
     * describeTriplet(Triplet) methods.
     * Finally, it prints a section indicating the processing of the exceptional state.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void printInfo() {

        if (isDebug()) exception.printStackTrace(System.err);
        section(b(c(MOST), RELEVANT, EXCEPTIONS), () -> {
            logEmptyLine(true, true);
            log(Level.none, b(s(_LFD_, describeTriplet(triplet)), s(_LFD_)), false);
            logEmptyLine(true, true);
        });

    }
}
