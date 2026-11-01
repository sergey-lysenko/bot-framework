package works.lysenko.base;

import works.lysenko.base.test.ExceptionHandler;
import works.lysenko.base.test.Patience;
import works.lysenko.base.test.Repeater;
import works.lysenko.base.test.Routines;
import works.lysenko.base.test.Stat;
import works.lysenko.tree.Ctrl;
import works.lysenko.util.apis.core._Status;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.test._ExceptionsHandler;
import works.lysenko.util.apis.test._Exec;
import works.lysenko.util.apis.test._Patience;
import works.lysenko.util.apis.test._Repeater;
import works.lysenko.util.apis.test._Stat;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.data.records.test.Workflow;
import works.lysenko.util.prop.core.Mode;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.prop.tree.Traverse;

import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.log;
import static works.lysenko.Base.logEmptyLine;
import static works.lysenko.Base.logEvent;
import static works.lysenko.Base.parameters;
import static works.lysenko.Base.test;
import static works.lysenko.Base.timer;
import static works.lysenko.base.test.Routines.build;
import static works.lysenko.base.test.Routines.haltService;
import static works.lysenko.base.test.Routines.isAllLeafsMode;
import static works.lysenko.base.test.Routines.verifyProperties;
import static works.lysenko.base.test.Stat.reports;
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
import static works.lysenko.util.lang.A.ALL_LEAFS_MODE_ABORTED_WANDERING_PATIENCE_EXCEEDED;
import static works.lysenko.util.lang.I.INITIAL_SETBACK_MESSAGE;
import static works.lysenko.util.lang.M.MANUALLY_REQUESTED_TEST_STOP_PERFORMED;
import static works.lysenko.util.lang.T.TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY;
import static works.lysenko.util.lang.T.TEST_SESSION_COMPLETED;
import static works.lysenko.util.lang.word.S.STATUS;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.spec.Level.none;

/**
 * Orchestrates the lifecycle and repeated execution of test scenarios within a test session.
 * <p>
 * Responsibilities include instantiating workflow hooks (preflight, postflight, status provider, and
 * custom exception handling), preparing the root scenario tree, managing test repetition across fixed-count
 * or all-leafs execution modes (delegating wandering patience tracking to {@link Patience}), handling dashboard
 * or programmatic stop requests, and generating session completion reports.
 */
@SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ThisEscapedInObjectConstruction", "MethodWithMultipleReturnPoints"})
public class Test implements _Test {

    private final String root;
    private final _ExceptionsHandler handler = new ExceptionHandler();
    private final _Exec executor = new works.lysenko.base.test.Exec();
    private final _Stat statistics = new Stat();
    private final _Repeater repeater = new Repeater(handler, executor, statistics);
    private _Patience patience = new Patience(this::repeater, this::statistics);
    private Runnable postflight = null;
    private Runnable preflight = null;
    private _Status status = null;

    /**
     * Constructs a new {@code Test} session configured with the components defined in the given {@link Workflow}.
     * <p>
     * Instantiates any non-null workflow hooks, initialises the global {@link Exec} context with additional
     * property definitions, captures the root scenario package from {@link Scenario#root}, and registers
     * this instance as the active global {@code test}.
     *
     * @param workflow the {@link Workflow} record containing optional lifecycle classes and additional property enums
     */
    @SuppressWarnings({"FeatureEnvy", "StaticImportCanBeUsed"})
    public Test(final Workflow workflow) {

        instantiateGiven(workflow);
        exec = new Exec(this, workflow.additional(), null);
        root = Scenario.root;
        test = this;
    }

    private static boolean isLostPatience() {

        logEvent(S3, b(ALL_LEAFS_MODE_ABORTED_WANDERING_PATIENCE_EXCEEDED));
        return false;
    }

    /**
     * Finalises the test session by generating statistical reports, logging session completion,
     * executing the optional postflight callback, and conditionally closing core resources.
     * <p>
     * Any {@link RuntimeException} raised during completion is routed to
     * {@link #processUncheckedException(Exception, works.lysenko.util.data.enums.ExitCode)}, and
     * {@link Traverse#unfreeze()} is always invoked before returning.
     */
    @Override
    public final void complete() {

        try {
            reports();
            logEmptyLine();
            log(none, bb(TEST_SESSION_COMPLETED), false);
            if (isNotNull(postflight)) postflight.run();
            core.conditionalClose();
        } catch (final RuntimeException e) {
            processUncheckedException(e, TESTS_UNHANDLED_EXCEPTION);
        } finally {
            Traverse.unfreeze();
        }
    }

    /**
     * Executes the full test session workflow.
     * <p>
     * Freezes {@link Traverse} settings, initialises the driver and scaler, runs the optional preflight
     * callback, verifies properties, prepares the root scenario tree, executes build routines, and runs
     * repeated test iterations via {@link #repeat()}. Once repetition finishes, logs a preemptive stop
     * event if the stop flag was set, halts the underlying service, and logs the total executed tests
     * and elapsed duration. Fails with {@code INITIAL_SETBACK_MESSAGE} if driver or scaler creation fails.
     *
     * @throws SafeguardException if a safeguard violation occurs during test execution
     */
    @SuppressWarnings("ChainedMethodCall")
    @Override
    public final void execute() throws SafeguardException {

        Traverse.freeze();
        if (exec.createDriver() && exec.createScaler()) {
            if (isNotNull(preflight)) preflight.run();
            verifyProperties();
            prepareRoot();
            build();
            repeat();
            if (core.getStopFlag()) logEvent(S3, TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY);
            haltService();
            log(none, b(yb(s1(repeater.getHistory().size(), TEST)), OF, parameters.testDescription(), DONE, IN,
                    yb(t(timer.msSinceStart()))), true);
        } else fail(INITIAL_SETBACK_MESSAGE); //NON-NLS
    }

    /**
     * Retrieves the scenario executor wrapper that manages and runs the root scenario controller.
     *
     * @return the {@link _Exec} instance associated with this test session
     */
    @Override
    public final _Exec executor() {

        return executor;
    }

    /**
     * Retrieves the current human-readable status of the test session.
     *
     * @return the status string supplied by the configured {@link _Status} provider, or a default
     *         {@code "Status unknown"} message if no status provider is configured
     */
    @Override
    public final String getStatus() {

        return (null == status) ? b(c(STATUS), UNKNOWN) : status.get();
    }

    /**
     * Retrieves the exception handler responsible for managing exceptions encountered during test iterations.
     *
     * @return the {@link _ExceptionsHandler} instance associated with this test session
     */
    @Override
    public final _ExceptionsHandler handler() {

        return handler;
    }

    /**
     * Checks whether a manual stop has been requested via the UI dashboard.
     *
     * @return {@code true} if the dashboard is present and its stop flag is active, {@code false} otherwise
     */
    @Override
    public final boolean isStopRequestedByDashboard() {

        return (isNotNull(core.getDashboard())) && core.getDashboard().isStop();
    }

    /**
     * Retrieves the wandering patience tracker associated with this test session.
     *
     * @return the {@link _Patience} instance associated with this test session
     */
    @Override
    public final _Patience patience() {

        if (null == patience) patience = new Patience(this::repeater, this::statistics);
        return patience;
    }

    /**
     * Retrieves the optional postflight action executed during {@link #complete()} after reports are generated.
     *
     * @return the postflight {@link Runnable}, or {@code null} if none was configured
     */
    @Override
    public final Runnable postflight() {

        return postflight;
    }

    /**
     * Retrieves the optional preflight action executed during {@link #execute()} prior to property verification.
     *
     * @return the preflight {@link Runnable}, or {@code null} if none was configured
     */
    @Override
    public final Runnable preflight() {

        return preflight;
    }

    /**
     * Initialises the root scenario {@link works.lysenko.tree.Tree} and assigns a {@link Ctrl} controller
     * to the {@link #executor} if one has not already been configured.
     */
    @SuppressWarnings({"StaticImportCanBeUsed", "MethodWithMoreThanThreeNegations"})
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
     * Retrieves the repeater responsible for executing individual test iterations and recording their history.
     *
     * @return the {@link _Repeater} instance associated with this test session
     */
    @Override
    public final _Repeater repeater() {

        return repeater;
    }

    /**
     * Retrieves the statistics collector tracking execution history and timing metrics for this test session.
     *
     * @return the {@link _Stat} instance associated with this test session
     */
    @Override
    public final _Stat statistics() {

        return statistics;
    }

    /**
     * Requests test execution to stop without specifying a reason.
     */
    @Override
    public final void stop() {

        stop(null);
    }

    /**
     * Requests test execution to stop, optionally recording the stop reason and capturing the active scenario.
     * <p>
     * Sets the core stop flag to {@code true}, updates the core stop reason when {@code reason} is non-null,
     * and records the currently executing scenario as the stop scenario if one has not yet been set.
     *
     * @param reason a description of why test execution is being stopped, or {@code null} if unspecified
     */
    @Override
    public final void stop(final String reason) {

        if (null != reason) core.setStopReason(reason);
        core.setStopFlag(true);
        if (null == core.getStopScenario() && isNotNull(exec) && isNotNull(exec.scenarios())) {
            core.setStopScenario(exec.scenarios().current());
        }
    }

    /**
     * Evaluates whether wandering patience has been reached or exceeded since {@code progressTestNumber}.
     *
     * @param progressTestNumber the completed test count at which leaf execution progress last occurred
     * @return {@code true} if consecutive fruitless iterations have reached or exceeded the idle-cycle budget,
     *         {@code false} otherwise
     */
    final boolean isPatienceLost(final int progressTestNumber) {

        return patience().isLost(progressTestNumber);
    }

    private void instantiateGiven(final Workflow classes) {

        if (isNotNull(classes.exceptional())) handler.setExceptional(instantiate(classes.exceptional()));
        if (isNotNull(classes.preflight())) preflight = instantiate(classes.preflight());
        if (isNotNull(classes.status())) status = instantiate(classes.status());
        if (isNotNull(classes.postflight())) postflight = instantiate(classes.postflight());
    }

    private boolean isAllLeafs() {

        if (core.areAllLeafsExecuted()) return false;
        if (patience().isLost()) return isLostPatience();
        return true;
    }

    /**
     * Determines whether test execution has remaining iterations to run.
     * <p>
     * In all-leafs mode (see {@link Routines#isAllLeafsMode()}), checks whether required leaf executions
     * remain and wandering patience has not been lost. Otherwise, checks whether the number of completed
     * iterations is below {@code core.getTotalTests()} (or unbounded if {@code core.getTotalTests()} is {@code null}).
     *
     * @return {@code true} if test repetition should continue, {@code false} if execution is exhausted
     */
    private boolean isNotExhausted() {

        if (isAllLeafsMode()) return isAllLeafs();
        return null == core.getTotalTests() || repeater.getHistory().size() < core.getTotalTests();
    }

    private boolean isToBeRepeated() {

        return isNotExhausted()
                && !core.getStopFlag()
                && (Mode.resilient || !core.getResults().areFailingEvents())
                && !wasStopRequestedAndPerformed();
    }

    private void performStop() {

        logEvent(S2, MANUALLY_REQUESTED_TEST_STOP_PERFORMED);
        core.getDashboard().setStop(false);
        stop(MANUALLY_REQUESTED_TEST_STOP_PERFORMED);
    }

    /**
     * Repeatedly executes test iterations via {@link #repeater} while continuation conditions hold.
     * <p>
     * Repetition starts and continues only when {@link Mode#resilient} is enabled or no failing events
     * have been recorded, execution is not exhausted, the core stop flag is unset, and no manual stop
     * has been requested via the dashboard. Delegates leaf execution progress tracking to {@link #patience()}.
     * <p>
     * Any {@link Error} (such as a failed assertion) thrown inside the loop is caught without rethrowing,
     * printed via {@link _Test#printThrowable(Throwable)}, and used to trigger {@link #stop(String)}.
     *
     * @throws SafeguardException if a safeguard violation occurs during a repeater iteration
     */
    @SuppressWarnings({"MethodCallInLoopCondition", "ErrorNotRethrown"})
    private void repeat() throws SafeguardException {

        if (Mode.resilient || !core.getResults().areFailingEvents()) {
            try {
                patience().resetProgress();
                while (isToBeRepeated()) {
                    repeater.run();
                    patience().recordCycleProgress();
                }
            } catch (final Error e) { // Failed assertions, no rethrow
                _Test.printThrowable(e);
                stop(null != e.getMessage() ? e.getMessage() : e.toString());
            }
        }
    }

    private boolean wasStopRequestedAndPerformed() {

        if (isStopRequestedByDashboard()) {
            performStop();
            return true;
        }
        return false;
    }
}
