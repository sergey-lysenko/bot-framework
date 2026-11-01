package works.lysenko.base;

import works.lysenko.base.test.*;
import works.lysenko.tree.Ctrl;
import works.lysenko.util.apis.core._Status;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.test.*;
import works.lysenko.util.data.records.test.Workflow;
import works.lysenko.util.prop.core.Mode;
import works.lysenko.util.prop.tree.*;
import works.lysenko.util.spec.Level;
import works.lysenko.util.prop.PropEnum;

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
import static works.lysenko.util.data.records.KeyValue.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Time.t;
import static works.lysenko.util.data.strs.Vars.*;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.core.ClassLoader.instantiate;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.A.*;
import static works.lysenko.util.lang.I.INITIAL_SETBACK_MESSAGE;
import static works.lysenko.util.lang.M.MANUALLY_REQUESTED_TEST_STOP_PERFORMED;
import static works.lysenko.util.lang.T.TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY;
import static works.lysenko.util.lang.T.TEST_SESSION_COMPLETED;
import static works.lysenko.util.lang.word.S.STATUS;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.prop.tree.Wandering.*;

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
        } finally {
            Traverse.unfreeze();
        }
    }

    @SuppressWarnings("ChainedMethodCall")
    /**
     * Performs execute.
     */
    public final void execute() throws SafeguardException {

        Traverse.freeze();
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

    /**
     * Performs executor.
     * @return the executor result
     */
    public final _Exec executor() {

        return executor;
    }

    @Override
    public final String getStatus() {

        return (null == status) ? b(c(STATUS), UNKNOWN) : status.get();
    }

    /**
     * Performs handler.
     * @return the handler result
     */
    public final _ExceptionsHandler handler() {

        return handler;
    }

    /**
     * Performs statistics.
     * @return the statistics result
     */
    public final _Stat statistics() {
        return statistics;
    }

    /**
     * Performs is stop requested by dashboard.
     * @return the is stop requested by dashboard result
     */
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

    /**
     * Performs repeater.
     * @return the repeater result
     */
    public final _Repeater repeater() {

        return repeater;
    }

    /**
     * Performs stop.
     */
    public final void stop() {

        stop(null);
    }

    /**
     * Performs stop.
     * @param reason the reason
     */
    public final void stop(final String reason) {

        if (null != reason) core.setStopReason(reason);
        core.setStopFlag(true);
        if (null == core.getStopScenario() && isNotNull(exec) && isNotNull(exec.scenarios())) {
            core.setStopScenario(exec.scenarios().current());
        }
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

    private static boolean isCountAllLeafs() {
        final Integer count = PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get();
        return isNotNull(count) && count > 1;
    }

    /**
     * Checks if the tests execution is not exhausted.
     *
     * @param lastProgressTestNumber the test number when the last leaf execution occurred
     * @return true if the tests execution is not exhausted, false otherwise.
     */
    private boolean isNotExhausted(final int lastProgressTestNumber) {

        final boolean allLeafsMode = (isNotNull(parameters))
                ? parameters.isAllLeafs()
                : (Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get()) || isCountAllLeafs());
        if (allLeafsMode) return isAllLeafs(isPatienceLost(lastProgressTestNumber));
        return null == core.getTotalTests() || repeater.getHistory().size() < core.getTotalTests();
    }

    private boolean isPatienceLost(final int lastProgressTestNumber) {

        boolean patienceLost = false;
        logDebug(a(kv("lastProgressTestNumber", lastProgressTestNumber)));
        if (lastProgressTestNumber > 0) {
            final int currentTest = repeater.getHistory().size() + 1;
            final double multiplier = (2.0 + (patience.doubleValue() / lastProgressTestNumber));
            logDebug(a(kv("multiplier", multiplier)));
            final double patienceThreshold = lastProgressTestNumber * multiplier;
            logDebug(String.format(java.util.Locale.ROOT, "Patience at %.0f%% (%d / %.0f)",
                    100.0 * currentTest / patienceThreshold, currentTest, patienceThreshold), false);
            patienceLost = currentTest > patienceThreshold;
        }
        return patienceLost;
    }

    /**
     * Determines whether the test run should continue in "all-leafs" mode.
     * <p>
     * If the wandering patience has been lost, the run is aborted. Otherwise, the run continues for as long
     * as not every leaf of the test model has been executed.
     *
     * @param patienceLost {@code true} if the number of tests executed since the last progress exceeds the
     *                     patience threshold ({@code lastProgressTestNumber + patience}), {@code false} otherwise
     * @return {@code true} if execution should continue (not all leafs have been executed and patience
     * has not run out), {@code false} if all leafs have been executed or patience was exceeded
     */
    private static boolean isAllLeafs(final boolean patienceLost) {

        if (patienceLost) return isLostPatience();
        return !core.areAllLeafsExecuted();
    }

    /**
     * Handles the loss of patience in "all-leafs" mode by logging that the mode was aborted.
     * <p>
     * The returned value is meant to be passed straight back as the "continue" flag of the calling method.
     *
     * @return always {@code false}, signalling that the run should not continue
     */
    private static boolean isLostPatience() {

        logEvent(S3, b(ALL_LEAFS_MODE_ABORTED_WANDERING_PATIENCE_EXCEEDED));
        return false;
    }

    /**
     * Performs the necessary actions to stop the test execution.
     * <p>
     * This method includes the following steps:
     * - Logs the event indicating that a manual test stop has been performed.
     * - Deactivates the stop button on the dashboard.
     * - Sets the stop flag in the core to true, signaling that test execution should halt.
     */
    private void performStop() {

        logEvent(S2, MANUALLY_REQUESTED_TEST_STOP_PERFORMED);
        core.getDashboard().setStop(false);
        stop(MANUALLY_REQUESTED_TEST_STOP_PERFORMED);
    }

    /**
     * Creates executor for root scenarios.
     */
    @Override
    public final void prepareRoot() {

        if (null == executor.ctrl()) {
            final String pack = (null != root && !root.isEmpty()) ? root : Scenario.root;
            if (null != pack && !pack.isEmpty()) {
                works.lysenko.Base.tree = new works.lysenko.tree.Tree(pack);
                executor.ctrl(new Ctrl(null, works.lysenko.Base.tree.getBranches()));
            }
        }
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

        if (Mode.resilient || !core.getResults().areFailingEvents()) {
            try {
                int lastLeafExecutionCount = core.getExecutedLeafExecutionsCount();
                int lastProgressTestNumber = repeater.getHistory().size();

                while (isNotExhausted(lastProgressTestNumber) && !core.getStopFlag() && (Mode.resilient || !core.getResults().areFailingEvents()) && !wasStopRequestedAndPerformed()) {
                    repeater.run();
                    final int currentLeafExecutionCount = core.getExecutedLeafExecutionsCount();
                    if (currentLeafExecutionCount > lastLeafExecutionCount) {
                        lastLeafExecutionCount = currentLeafExecutionCount;
                        lastProgressTestNumber = repeater.getHistory().size();
                    }
                }
            } catch (final Error e) { // Failed assertions, no rethrow
                _Test.printThrowable(e);
                stop(e.getMessage() != null ? e.getMessage() : e.toString());
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
