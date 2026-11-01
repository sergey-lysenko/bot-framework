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
 * Orchestrates the lifecycle and repeated execution of test scenarios within a test session.
 * <p>
 * Responsibilities include instantiating workflow hooks (preflight, postflight, status provider, and
 * custom exception handling), preparing the root scenario tree, managing test repetition across fixed-count
 * or all-leafs execution modes (with wandering patience tracking), handling dashboard or programmatic stop
 * requests, and generating session completion reports.
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
     * Constructs a new {@code Test} session configured with the components defined in the given {@link Workflow}.
     * <p>
     * Instantiates any non-null workflow hooks, initializes the global {@link Exec} context with additional
     * property definitions, captures the root scenario package from {@link Scenario#root}, and registers
     * this instance as the active global {@code test}.
     *
     * @param workflow the {@link Workflow} record containing optional lifecycle classes and additional property enums
     */
    @SuppressWarnings("FeatureEnvy")
    public Test(final Workflow workflow) {

        instantiateGiven(workflow);
        exec = new Exec(this, workflow.additional(), null);
        root = Scenario.root;
        test = this;
    }

    /**
     * Finalizes the test session by generating statistical reports, logging session completion,
     * executing the optional postflight callback, and conditionally closing core resources.
     * <p>
     * Any {@link RuntimeException} raised during completion is routed to
     * {@link #processUncheckedException(Exception, works.lysenko.util.data.enums.ExitCode)}, and
     * {@link Traverse#unfreeze()} is always invoked before returning.
     */
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

    /**
     * Executes the full test session workflow.
     * <p>
     * Freezes {@link Traverse} settings, initializes the driver and scaler, runs the optional preflight
     * callback, verifies properties, prepares the root scenario tree, executes build routines, and runs
     * repeated test iterations via {@link #repeat()}. Once repetition finishes, logs a preemptive stop
     * event if the stop flag was set, halts the underlying service, and logs the total executed tests
     * and elapsed duration. Fails with {@code INITIAL_SETBACK_MESSAGE} if driver or scaler creation fails.
     *
     * @throws SafeguardException if a safeguard violation occurs during test execution
     */
    @SuppressWarnings("ChainedMethodCall")
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
     * Retrieves the scenario executor wrapper that manages and runs the root scenario controller.
     *
     * @return the {@link _Exec} instance associated with this test session
     */
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
    public final _ExceptionsHandler handler() {

        return handler;
    }

    /**
     * Retrieves the statistics collector tracking execution history and timing metrics for this test session.
     *
     * @return the {@link _Stat} instance associated with this test session
     */
    public final _Stat statistics() {
        return statistics;
    }

    /**
     * Checks whether a manual stop has been requested via the UI dashboard.
     *
     * @return {@code true} if the dashboard is present and its stop flag is active, {@code false} otherwise
     */
    public final boolean isStopRequestedByDashboard() {

        return (isNotNull(core.getDashboard())) && core.getDashboard().isStop();
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
     * Retrieves the repeater responsible for executing individual test iterations and recording their history.
     *
     * @return the {@link _Repeater} instance associated with this test session
     */
    public final _Repeater repeater() {

        return repeater;
    }

    /**
     * Requests test execution to stop without specifying a reason.
     */
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
    public final void stop(final String reason) {

        if (null != reason) core.setStopReason(reason);
        core.setStopFlag(true);
        if (null == core.getStopScenario() && isNotNull(exec) && isNotNull(exec.scenarios())) {
            core.setStopScenario(exec.scenarios().current());
        }
    }

    /**
     * Instantiates and assigns optional workflow components ({@code exceptional}, {@code preflight},
     * {@code status}, and {@code postflight}) from the given {@link Workflow} when their class references
     * are non-null.
     *
     * @param classes the {@link Workflow} record containing the component classes to instantiate
     */
    private void instantiateGiven(final Workflow classes) {

        if (isNotNull(classes.exceptional())) handler.setExceptional(instantiate(classes.exceptional()));
        if (isNotNull(classes.preflight())) preflight = instantiate(classes.preflight());
        if (isNotNull(classes.status())) status = instantiate(classes.status());
        if (isNotNull(classes.postflight())) postflight = instantiate(classes.postflight());
    }

    /**
     * Checks whether multi-execution all-leafs mode is configured via {@link PropEnum#_TEST_ALL_LEAFS_EXECUTIONS}.
     *
     * @return {@code true} if the configured number of executions per leaf is greater than {@code 1},
     *         {@code false} otherwise
     */
    private static boolean isCountAllLeafs() {
        final Integer count = PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get();
        return isNotNull(count) && count > 1;
    }

    /**
     * Determines whether test execution has remaining iterations to run.
     * <p>
     * In all-leafs mode (enabled via {@code parameters.isAllLeafs()}, {@link PropEnum#_TEST_ALL_LEAFS},
     * or {@link #isCountAllLeafs()}), checks whether required leaf executions remain and wandering patience
     * has not been lost. Otherwise, checks whether the number of completed iterations is below
     * {@code core.getTotalTests()} (or unbounded if {@code core.getTotalTests()} is {@code null}).
     *
     * @param lastProgressTestNumber the completed test count at which leaf execution progress last occurred
     * @return {@code true} if test repetition should continue, {@code false} if execution is exhausted
     */
    private boolean isNotExhausted(final int lastProgressTestNumber) {

        final boolean allLeafsMode = (isNotNull(parameters))
                ? parameters.isAllLeafs()
                : (Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get()) || isCountAllLeafs());
        if (allLeafsMode) return isAllLeafs(lastProgressTestNumber);
        return null == core.getTotalTests() || repeater.getHistory().size() < core.getTotalTests();
    }

    private static final int RECENT_TIMES_WINDOW = 20;

    private double patiencePercentage = 0.0;
    private int lastLeafExecutionCount = 0;
    private int lastProgressTestNumber = 0;
    private int lastSyncedCycle = 0;

    /**
     * Retrieves the current wandering patience usage expressed as a percentage of the patience threshold,
     * shaped by {@link Wandering#steepness}.
     *
     * @return the current patience percentage ({@code 0.0} when leaf progress was just achieved or before iterations start)
     */
    @Override
    public double getPatiencePercentage() {
        return patiencePercentage;
    }

    /**
     * Computes the consecutive fruitless-iteration budget before wandering patience is exhausted.
     * <p>
     * Scales by {@link Wandering#patience} (default {@code 2}) and floors the reference scale by the total
     * required leaf executions in the scenario tree:
     * {@code idleBudget = max(1.0, (patience / 2.0) * max(totalLeafExecutions, progressTestNumber))}.
     * When {@code patience <= 0}, returns {@code 0.0}.
     *
     * @param progressTestNumber the completed test count at which leaf execution progress last occurred
     * @return the maximum allowed consecutive fruitless cycles
     */
    private static double computeIdleBudget(final int progressTestNumber) {

        final int totalLeafExecutions = Math.max(1, core.getTotalLeafExecutionsCount());
        final int baseScale = Math.max(totalLeafExecutions, progressTestNumber);
        final double patienceFactor = isNotNull(patience) ? Math.max(0.0, patience.doubleValue()) : 2.0;
        return (patienceFactor > 0.0) ? Math.max(1.0, (patienceFactor / 2.0) * baseScale) : 0.0;
    }

    /**
     * Maps consecutive fruitless iterations and the current idle-cycle budget onto a shaped percentage
     * in {@code [0, 100+]} using {@link Wandering#applyCurve(double)}.
     *
     * @param idleCycles consecutive fruitless cycles since the last leaf execution goal
     * @param idleBudget the idle-cycle budget computed by {@link #computeIdleBudget(int)}
     * @return the shaped wandering patience percentage
     */
    private static double computePatiencePercentage(final int idleCycles, final double idleBudget) {

        if (idleCycles <= 0) return 0.0;
        if (idleBudget <= 0.0) return 100.0;
        return 100.0 * applyCurve(idleCycles / idleBudget);
    }

    /**
     * Synchronizes wandering patience state at the completion of a test cycle so telemetry and ETA
     * reflect cycle progress immediately without a one-cycle phase lag.
     *
     * @param testNum     the 1-based test cycle number that just completed
     * @param reachedGoal {@code true} if the cycle achieved a new required leaf execution goal, {@code false} otherwise
     */
    @Override
    public void syncPatienceAtCycleEnd(final int testNum, final boolean reachedGoal) {

        if (testNum <= 0) return;
        lastSyncedCycle = Math.max(lastSyncedCycle, testNum);
        if (reachedGoal) {
            lastProgressTestNumber = testNum;
            lastLeafExecutionCount = Math.max(lastLeafExecutionCount, core.getExecutedLeafExecutionsCount());
            patiencePercentage = 0.0;
            return;
        }
        final int idleCycles = Math.max(0, testNum - lastProgressTestNumber);
        final double idleBudget = computeIdleBudget(lastProgressTestNumber);
        patiencePercentage = computePatiencePercentage(idleCycles, idleBudget);
    }

    /**
     * Estimates the remaining milliseconds until wandering patience aborts the test session if no further
     * leaf progress occurs.
     * <p>
     * Uses a rolling average of up to {@link #RECENT_TIMES_WINDOW} recent cycle durations (falling back to
     * {@code elapsedMs / completedCycles} when timing history is empty) and subtracts elapsed in-flight
     * time when an active cycle is currently running ahead of {@link #lastSyncedCycle}.
     *
     * @param elapsedMs the elapsed session runtime in milliseconds
     * @return estimated milliseconds remaining until patience abort, or {@code -1L} if no cycle has completed yet
     */
    @Override
    public long estimatePatienceEtaMs(final long elapsedMs) {

        if (elapsedMs <= 0L) return -1L;
        final double idleBudget = computeIdleBudget(lastProgressTestNumber);
        final int abortTotalTests = lastProgressTestNumber + (int) Math.ceil(idleBudget);
        final int historySize = (isNotNull(repeater) && isNotNull(repeater.getHistory()))
                ? repeater.getHistory().size()
                : 0;
        final int completedCycles = Math.max(historySize, lastSyncedCycle);
        if (completedCycles <= 0) return -1L;
        if (completedCycles >= abortTotalTests) return 0L;

        final Integer activeTest = isNotNull(repeater) ? repeater.getCurrent() : null;
        final long inFlightMs = isNotNull(repeater) ? repeater.getInFlightMs(elapsedMs) : 0L;

        double avgCycleMs = 0.0;
        final java.util.List<Long> times = isNotNull(statistics) ? statistics.times() : null;
        long sum = 0L;
        int count = 0;
        if (isNotNull(times) && !times.isEmpty()) {
            final int size = times.size();
            final int start = Math.max(0, size - RECENT_TIMES_WINDOW);
            for (int i = start; i < size; i++) {
                final Long d = times.get(i);
                if (null != d && d > 0L) {
                    sum += d;
                    count++;
                }
            }
        }
        if (null != activeTest && activeTest == completedCycles && inFlightMs > 0L && historySize < completedCycles) {
            sum += inFlightMs;
            count++;
        }
        if (count > 0) {
            avgCycleMs = (double) sum / count;
        } else {
            avgCycleMs = (double) elapsedMs / completedCycles;
        }

        if (null != activeTest && activeTest > completedCycles) {
            final double currentRemaining = Math.max(0.0, avgCycleMs - inFlightMs);
            final int futureCycles = Math.max(0, abortTotalTests - activeTest);
            return Math.round(currentRemaining + futureCycles * avgCycleMs);
        }
        final int remainingCycles = Math.max(0, abortTotalTests - completedCycles);
        return Math.round(remainingCycles * avgCycleMs);
    }

    /**
     * Evaluates whether wandering patience has been reached or exceeded since the last leaf execution progress
     * and updates {@link #patiencePercentage}.
     * <p>
     * Measures consecutive fruitless iterations ({@code idleCycles = completedTests - progressTestNumber})
     * against {@link #computeIdleBudget(int)} and shapes {@link #patiencePercentage} via
     * {@link Wandering#applyCurve(double)}. Upon leaf progress ({@code idleCycles == 0}),
     * {@link #patiencePercentage} resets to {@code 0.0}. Once {@code idleCycles >= idleBudget}
     * ({@code 100%} patience), repetition terminates cleanly without executing an extra overshoot cycle.
     *
     * @param progressTestNumber the completed test count at which leaf execution progress last occurred
     * @return {@code true} if consecutive fruitless iterations have reached or exceeded the idle-cycle budget,
     *         {@code false} otherwise
     */
    boolean isPatienceLost(final int progressTestNumber) {

        this.lastProgressTestNumber = progressTestNumber;
        final int completedTests = repeater.getHistory().size();
        final int idleCycles = Math.max(0, completedTests - progressTestNumber);
        final double idleBudget = computeIdleBudget(progressTestNumber);
        logTrace(a(kv("lastProgressTestNumber", progressTestNumber)));
        logTrace(a(kv("idleCycles", idleCycles)));
        logTrace(a(kv("idleBudget", idleBudget)));
        if (completedTests == 0) {
            this.patiencePercentage = 0.0;
            return false;
        }
        this.patiencePercentage = computePatiencePercentage(idleCycles, idleBudget);
        final String formattedPct = (Math.abs(patiencePercentage - Math.rint(patiencePercentage)) < 1e-6)
                ? String.format(java.util.Locale.ROOT, "%.0f", patiencePercentage)
                : String.format(java.util.Locale.ROOT, "%.1f", patiencePercentage);
        final String formattedBudget = (idleBudget == Math.rint(idleBudget))
                ? String.format(java.util.Locale.ROOT, "%.0f", idleBudget)
                : String.format(java.util.Locale.ROOT, "%.1f", idleBudget);
        logDebug(String.format(java.util.Locale.ROOT, "Patience at %s%% (%d / %s)",
                formattedPct, idleCycles, formattedBudget));
        return idleCycles >= idleBudget;
    }

    /**
     * Determines whether the test run should continue in all-leafs mode.
     * <p>
     * Terminates cleanly once {@code core.areAllLeafsExecuted()} is {@code true}. Otherwise evaluates
     * wandering patience via {@link #isPatienceLost(int)} and logs an abort event via {@link #isLostPatience()}
     * when patience is exceeded.
     *
     * @param progressTestNumber the completed test count at which leaf execution progress last occurred
     * @return {@code true} if required leaf executions remain and patience has not run out,
     *         {@code false} if all leafs have been executed or patience was exceeded
     */
    private boolean isAllLeafs(final int progressTestNumber) {

        if (core.areAllLeafsExecuted()) return false;
        if (isPatienceLost(progressTestNumber)) return isLostPatience();
        return true;
    }

    /**
     * Logs that all-leafs mode was aborted due to exceeding wandering patience.
     *
     * @return always {@code false}, signalling that test repetition must not continue
     */
    private static boolean isLostPatience() {

        logEvent(S3, b(ALL_LEAFS_MODE_ABORTED_WANDERING_PATIENCE_EXCEEDED));
        return false;
    }

    /**
     * Executes a manual stop requested from the dashboard.
     * <p>
     * Logs the manual stop event at severity {@code S2}, resets the dashboard stop toggle, and invokes
     * {@link #stop(String)} with {@code MANUALLY_REQUESTED_TEST_STOP_PERFORMED}.
     */
    private void performStop() {

        logEvent(S2, MANUALLY_REQUESTED_TEST_STOP_PERFORMED);
        core.getDashboard().setStop(false);
        stop(MANUALLY_REQUESTED_TEST_STOP_PERFORMED);
    }

    /**
     * Initializes the root scenario {@link works.lysenko.tree.Tree} and assigns a {@link Ctrl} controller
     * to the {@link #executor} if one has not already been configured.
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
     * Repeatedly executes test iterations via {@link #repeater} while continuation conditions hold.
     * <p>
     * Repetition starts and continues only when {@link Mode#resilient} is enabled or no failing events
     * have been recorded, execution is not exhausted, the core stop flag is unset, and no manual stop
     * has been requested via the dashboard. Tracks leaf execution progress after each iteration to update
     * {@link #lastProgressTestNumber} whenever new leaf executions are recorded.
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
                lastLeafExecutionCount = core.getExecutedLeafExecutionsCount();
                lastProgressTestNumber = repeater.getHistory().size();
                lastSyncedCycle = lastProgressTestNumber;

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
     * Checks whether a manual stop was requested via the dashboard and, if so, performs the stop sequence.
     *
     * @return {@code true} if a dashboard stop request was detected and processed via {@link #performStop()},
     *         {@code false} otherwise
     */
    private boolean wasStopRequestedAndPerformed() {

        if (isStopRequestedByDashboard()) {
            performStop();
            return true;
        }
        return false;
    }
}
