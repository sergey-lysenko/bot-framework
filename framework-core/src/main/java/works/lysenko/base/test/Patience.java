package works.lysenko.base.test;

import works.lysenko.util.apis.test._Patience;
import works.lysenko.util.apis.test._Repeater;
import works.lysenko.util.apis.test._Stat;
import works.lysenko.util.prop.tree.Wandering;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static works.lysenko.Base.core;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logTrace;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.P.PATIENCE_AT_S_D_OF_S;
import static works.lysenko.util.prop.tree.Wandering.applyCurve;
import static works.lysenko.util.prop.tree.Wandering.patience;
import static works.lysenko.util.spec.Numbers.HUNDRED;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.TWO;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Tracks wandering patience consumption, fruitless-cycle budgets, leaf execution progress watermarks,
 * and rolling-window ETA estimates for all-leafs test execution mode.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public class Patience implements _Patience {

    private static final int RECENT_TIMES_WINDOW = 20;
    private static final double DEFAULT_PATIENCE = TWO;
    private static final double FULL_PERCENTAGE = HUNDRED;
    private static final double INTEGER_TOLERANCE = 0.000_001;
    private static final String D1F = "%.1f"; //NON-NLS
    private static final String D0F = "%.0f"; //NON-NLS

    private final Supplier<? extends _Repeater> repeaterSupplier;
    private final Supplier<? extends _Stat> statisticsSupplier;
    private double percentage = 0.0;
    private int lastLeafExecutionCount = ZERO;
    private int lastProgressTestNumber = ZERO;
    private int lastSyncedCycle = ZERO;

    /**
     * Constructs a {@code Patience} tracker backed by fixed {@link _Repeater} and {@link _Stat} instances.
     *
     * @param repeater   the repeater providing completed history and active in-flight cycle timing
     * @param statistics the statistics collector providing recent cycle durations
     */
    public Patience(final _Repeater repeater, final _Stat statistics) {

        this(() -> repeater, () -> statistics);
    }

    /**
     * Constructs a {@code Patience} tracker backed by lazy suppliers for {@link _Repeater} and {@link _Stat}.
     *
     * @param repeaterSupplier   supplier resolving the active {@link _Repeater}
     * @param statisticsSupplier supplier resolving the active {@link _Stat}
     */
    public Patience(final Supplier<? extends _Repeater> repeaterSupplier,
                    final Supplier<? extends _Stat> statisticsSupplier) {

        this.repeaterSupplier = repeaterSupplier;
        this.statisticsSupplier = statisticsSupplier;
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
    static double computeIdleBudget(final int progressTestNumber) {

        final int totalLeafExecutions = Math.max(ONE, core.getTotalLeafExecutionsCount());
        final int baseScale = Math.max(totalLeafExecutions, progressTestNumber);
        final double patienceFactor = isNotNull(patience) ? Math.max(0.0, patience.doubleValue()) : DEFAULT_PATIENCE;
        return (0.0 < patienceFactor) ? Math.max(1.0, (patienceFactor / DEFAULT_PATIENCE) * baseScale) : 0.0;
    }

    /**
     * Maps consecutive fruitless iterations and the current idle-cycle budget onto a shaped percentage
     * in {@code [0, 100+]} using {@link Wandering#applyCurve(double)}.
     *
     * @param idleCycles consecutive fruitless cycles since the last leaf execution goal
     * @param idleBudget the idle-cycle budget computed by {@link #computeIdleBudget(int)}
     * @return the shaped wandering patience percentage
     */
    static double computePatiencePercentage(final int idleCycles, final double idleBudget) {

        if (ZERO >= idleCycles) return 0.0;
        if (0.0 >= idleBudget) return FULL_PERCENTAGE;
        return FULL_PERCENTAGE * applyCurve(idleCycles / idleBudget);
    }

    private static boolean isCountable(final Integer activeTest, final int completedCycles,
                                       final long inFlightMs, final int historySize) {

        return null != activeTest && activeTest == completedCycles && 0L < inFlightMs && historySize < completedCycles;
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
    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public final long estimateEtaMs(final long elapsedMs) {

        if (0L >= elapsedMs) return -1L;
        final double idleBudget = computeIdleBudget(lastProgressTestNumber);
        final int abortTotalTests = lastProgressTestNumber + (int) Math.ceil(idleBudget);
        final int historySize = getHistorySize();
        final int completedCycles = Math.max(historySize, lastSyncedCycle);
        if (ZERO == completedCycles) return -1L;
        if (completedCycles >= abortTotalTests) return 0L;

        final _Repeater repeater = repeaterSupplier.get();
        final Integer activeTest = isNotNull(repeater) ? repeater.getCurrent() : null;
        final long inFlightMs = isNotNull(repeater) ? repeater.getInFlightMs(elapsedMs) : 0L;

        final double avgCycleMs = computeAverageCycleMs(elapsedMs, completedCycles, historySize, activeTest, inFlightMs);
        if (null != activeTest && activeTest > completedCycles) {
            final double currentRemaining = Math.max(0.0, avgCycleMs - inFlightMs);
            final int futureCycles = Math.max(ZERO, abortTotalTests - activeTest);
            return Math.round(currentRemaining + futureCycles * avgCycleMs);
        }
        final int remainingCycles = Math.max(ZERO, abortTotalTests - completedCycles);
        return Math.round(remainingCycles * avgCycleMs);
    }

    @Override
    public final int getLastProgressTestNumber() {

        return lastProgressTestNumber;
    }

    /**
     * Retrieves the current wandering patience usage expressed as a percentage of the patience threshold,
     * shaped by {@link Wandering#steepness}.
     *
     * @return the current patience percentage ({@code 0.0} when leaf progress was just achieved or before iterations start)
     */
    @Override
    public final double getPercentage() {

        return percentage;
    }

    /**
     * Evaluates whether wandering patience has been reached or exceeded since the last leaf execution progress
     * and updates {@link #percentage}.
     * <p>
     * Measures consecutive fruitless iterations ({@code idleCycles = completedTests - progressTestNumber})
     * against {@link #computeIdleBudget(int)} and shapes {@link #percentage} via
     * {@link Wandering#applyCurve(double)}. Upon leaf progress ({@code idleCycles == 0}),
     * {@link #percentage} resets to {@code 0.0}. Once {@code idleCycles >= idleBudget}
     * ({@code 100%} patience), repetition terminates cleanly without executing an extra overshoot cycle.
     *
     * @param progressTestNumber the completed test count at which leaf execution progress last occurred
     * @return {@code true} if consecutive fruitless iterations have reached or exceeded the idle-cycle budget,
     *         {@code false} otherwise
     */
    @Override
    public final boolean isLost(final int progressTestNumber) {

        lastProgressTestNumber = progressTestNumber;
        final int completedTests = getHistorySize();
        final int idleCycles = Math.max(ZERO, completedTests - progressTestNumber);
        final double idleBudget = computeIdleBudget(progressTestNumber);
        logTrace(a(kv("lastProgressTestNumber", progressTestNumber))); //NON-NLS
        logTrace(a(kv("idleCycles", idleCycles))); //NON-NLS
        logTrace(a(kv("idleBudget", idleBudget))); //NON-NLS
        if (ZERO == completedTests) {
            percentage = 0.0;
            return false;
        }
        percentage = computePatiencePercentage(idleCycles, idleBudget);
        final String formattedPct = (INTEGER_TOLERANCE > Math.abs(percentage - Math.rint(percentage)))
                ? String.format(Locale.ROOT, D0F, percentage)
                : String.format(Locale.ROOT, D1F, percentage);
        final String formattedBudget = (idleBudget == Math.rint(idleBudget))
                ? String.format(Locale.ROOT, D0F, idleBudget)
                : String.format(Locale.ROOT, D1F, idleBudget);
        logDebug(String.format(Locale.ROOT, PATIENCE_AT_S_D_OF_S,
                formattedPct, idleCycles, formattedBudget));
        return idleCycles >= idleBudget;
    }

    @Override
    public final void recordCycleProgress() {

        final int executions = core.getExecutedLeafExecutionsCount();
        if (executions > lastLeafExecutionCount) {
            lastLeafExecutionCount = executions;
            lastProgressTestNumber = getHistorySize();
        }
    }

    @Override
    public final void resetProgress() {

        lastLeafExecutionCount = core.getExecutedLeafExecutionsCount();
        lastProgressTestNumber = getHistorySize();
        lastSyncedCycle = lastProgressTestNumber;
    }

    /**
     * Synchronises wandering patience state at the completion of a test cycle so telemetry and ETA
     * reflect cycle progress immediately without a one-cycle phase lag.
     *
     * @param testNum     the 1-based test cycle number that just completed
     * @param reachedGoal {@code true} if the cycle achieved a new required leaf execution goal, {@code false} otherwise
     */
    @Override
    public final void syncAtCycleEnd(final int testNum, final boolean reachedGoal) {

        if (ZERO >= testNum) return;
        lastSyncedCycle = Math.max(lastSyncedCycle, testNum);
        if (reachedGoal) {
            lastProgressTestNumber = testNum;
            lastLeafExecutionCount = Math.max(lastLeafExecutionCount, core.getExecutedLeafExecutionsCount());
            percentage = 0.0;
            return;
        }
        final int idleCycles = Math.max(ZERO, testNum - lastProgressTestNumber);
        final double idleBudget = computeIdleBudget(lastProgressTestNumber);
        percentage = computePatiencePercentage(idleCycles, idleBudget);
    }

    private double computeAverageCycleMs(final long elapsedMs, final int completedCycles,
                                         final int historySize, final Integer activeTest, final long inFlightMs) {

        final _Stat statistics = statisticsSupplier.get();
        final List<Long> times = isNotNull(statistics) ? statistics.times() : null;
        long sum = 0L;
        int count = ZERO;
        if (isNotNull(times) && !times.isEmpty()) {
            final int size = times.size();
            final int start = Math.max(ZERO, size - RECENT_TIMES_WINDOW);
            for (int i = start; i < size; i++) {
                final Long duration = times.get(i);
                if (null != duration && 0L < duration) {
                    sum += duration;
                    count++;
                }
            }
        }
        if (isCountable(activeTest, completedCycles, inFlightMs, historySize)) {
            sum += inFlightMs;
            count++;
        }
        return (ZERO < count) ? (double) sum / count : (double) elapsedMs / completedCycles;
    }

    private int getHistorySize() {

        final _Repeater repeater = repeaterSupplier.get();
        return (isNotNull(repeater) && isNotNull(repeater.getHistory())) ? repeater.getHistory().size() : ZERO;
    }
}
