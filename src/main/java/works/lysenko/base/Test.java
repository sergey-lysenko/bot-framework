package works.lysenko.base;

import works.lysenko.base.test.*;
import works.lysenko.tree.Ctrl;
import works.lysenko.util.apis.core._Status;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.test.*;
import works.lysenko.util.data.records.test.Workflow;
import works.lysenko.util.func.core.ClassLoader;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.spec.Level;

import static works.lysenko.Base.*;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.____.DONE;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.data.enums.Ansi.bb;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.ExitCode.TESTS_UNHANDLED_EXCEPTION;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.enums.Severity.S3;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Time.t;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.core.ClassLoader.instantiate;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.I.INITIAL_SETBACK_MESSAGE;
import static works.lysenko.util.lang.M.MANUALLY_REQUESTED_TEST_STOP_PERFORMED;
import static works.lysenko.util.lang.T.TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY;
import static works.lysenko.util.lang.T.TEST_SESSION_COMPLETED;
import static works.lysenko.util.lang.word.S.STATUS;
import static works.lysenko.util.lang.word.U.UNKNOWN;

/**
 * Represents the Test class that governs repetition of testing actions.
 */
@SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ThisEscapedInObjectConstruction", "MethodWithMultipleReturnPoints"})
public class Test implements _Test {

    private final String root;
    private final _ExceptionsHandler handler = new ExceptionHandler();
    private final _Exec executor = new works.lysenko.base.test.Exec();
    private final _Stat statistics = new Stat();
    private final _Repeater repeater = new Repeater(handler, executor, statistics);
    private java.lang.Runnable postflight = null;
    private java.lang.Runnable preflight = null;
    private _Status status = null;

    /**
     * Constructs a new Test instance.
     *
     * @param workflow the Workflow object containing the classes needed for instantiation
     */
    @SuppressWarnings("FeatureEnvy")
    public Test(final Workflow workflow) {

        instantiateGiven(workflow);
        exec = new Exec(this, workflow.additional(), null);
        root = Scenario.root;
        test = this;
    }

    @Override
    public final void complete() {

        try {
            Stat.reports();
            logEmptyLine();
            log(Level.none, bb(TEST_SESSION_COMPLETED), false);
            if (isNotNull(postflight)) postflight.run();
            core.conditionalClose();
        } catch (final RuntimeException e) {
            processUncheckedException(e, TESTS_UNHANDLED_EXCEPTION);
        }
    }

    @SuppressWarnings("ChainedMethodCall")
    public final void execute() throws SafeguardException {

        if (exec.createDriver() && exec.createScaler()) {
            if (isNotNull(preflight)) preflight.run();
            Routines.verifyProperties();
            prepareRoot();
            Routines.build();
            repeat();
            if (core.getStopFlag()) logEvent(S3, TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY);
            Routines.haltService();
            log(Level.none, b(yb(s1(repeater.getHistory().size(), TEST)), OF, parameters.testDescription(), DONE, IN,
                    yb(t(timer.msSinceStart()))), true);
        } else fail(INITIAL_SETBACK_MESSAGE); //NON-NLS
    }

    public final _Exec executor() {

        return executor;
    }

    @Override
    public final String getStatus() {

        return (null == status) ? b(c(STATUS), UNKNOWN) : status.get();
    }

    public final _ExceptionsHandler handler() {

        return handler;
    }

    public final _Stat statistics() {
        return statistics;
    }

    public final boolean isStopRequestedByDashboard() {

        return (isNotNull(core.getDashboard())) && core.getDashboard().isStop();
    }

    @Override
    public final Runnable postflight() {

        return postflight;
    }

    @Override
    public final Runnable preflight() {

        return preflight;
    }

    public final _Repeater repeater() {

        return repeater;
    }

    public final void stop() {

        core.setStopFlag(true);
    }

    /**
     * Instantiates and assigns objects for the given classes if they are not null.
     *
     * @param classes The Workflow object containing the specific classes needed for instantiation.
     */
    private void instantiateGiven(final Workflow classes) {

        if (isNotNull(classes.exceptional())) handler.setExceptional(instantiate(classes.exceptional()));
        if (isNotNull(classes.preflight())) preflight = instantiate(classes.preflight());
        if (isNotNull(classes.status())) status = instantiate(classes.status());
        if (isNotNull(classes.postflight())) postflight = instantiate(classes.postflight());
    }

    /**
     * Checks if the tests execution is not exhausted.
     *
     * @return true if the tests execution is not exhausted, false otherwise.
     */
    private boolean isNotExhausted() {

        return null == core.getTotalTests() || repeater.getHistory().size() < core.getTotalTests();
    }

    /**
     * Performs the necessary actions to stop the test execution.
     * <p>
     * This method includes the following steps:
     * - Logs the event indicating that a manual test stop has been performed.
     * - Deactivates the stop button on the dashboard.
     * - Sets the stop flag in the handler to true, signaling that test execution should halt.
     * - Closes the repeater to finalize the test process.
     */
    private void performStop() {

        logEvent(S2, MANUALLY_REQUESTED_TEST_STOP_PERFORMED);
        core.getDashboard().setStop(false);
        core.setStopFlag(true);
        repeater.close();
    }

    /**
     * Creates executor for root scenarios.
     */
    private void prepareRoot() {

        executor.ctrl(new Ctrl(null, ClassLoader.readFrom(root, false)));
    }

    /**
     * Repeats the execution of the repeater logic while certain conditions are met.
     * <p>
     * This method performs the following steps:
     * - Checks if there are no failing events using core's results.
     * - If no failing events are detected, it executes the repeater's run method in a loop.
     * - The loop continues as long as the execution is not exhausted and a stop request is not made.
     * - Catches any {@code Error} occurrences during the loop execution without rethrowing them,
     * and logs the error information using the _Test utility.
     *
     * @throws SafeguardException if any exception occurs during the execution of the repeater's run method
     */
    @SuppressWarnings({"MethodCallInLoopCondition", "ErrorNotRethrown"})
    private void repeat() throws SafeguardException {

        if (!core.getResults().areFailingEvents()) {
            try {
                while (!core.getStopFlag() && isNotExhausted() && !wasStopRequestedAndPerformed())
                    repeater.run();
            } catch (final Error e) { // Failed assertions, no rethrow
                _Test.printThrowable(e);
            }
        }
    }

    /**
     * Checks if a stop request was made and, if so, performs the necessary actions to stop the test execution.
     * <p>
     * This method:
     * - Evaluates if the stop was requested using the dashboard.
     * - If the stop request is detected, it triggers the stop procedure by calling {@code performStop()}.
     *
     * @return true if a stop request was made, false otherwise.
     */
    private boolean wasStopRequestedAndPerformed() {

        if (isStopRequestedByDashboard()) {
            performStop();
            return true;
        }
        return false;
    }
}
