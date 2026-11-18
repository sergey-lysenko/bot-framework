package works.lysenko.util.apis.test;

/**
 * Defines the contract for tracking wandering patience usage, fruitless-iteration budgets,
 * leaf execution progress, and patience-based ETA estimation in all-leafs test execution mode.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Patience {

    /**
     * Estimates the remaining milliseconds until wandering patience aborts the test session
     * if no further leaf execution progress occurs.
     *
     * @param elapsedMs the elapsed session runtime in milliseconds
     * @return estimated milliseconds remaining until patience abort, or {@code -1L} if unavailable
     */
    long estimateEtaMs(long elapsedMs);

    /**
     * Retrieves the completed test count at which leaf execution progress last occurred.
     *
     * @return the 0-based completed test count of the most recent leaf execution progress
     */
    int getLastProgressTestNumber();

    /**
     * Retrieves the current wandering patience usage expressed as a percentage of the patience threshold.
     *
     * @return the current patience percentage ({@code 0.0} on progress or before iterations start)
     */
    double getPercentage();

    /**
     * Evaluates whether wandering patience has been reached or exceeded since the most recently
     * recorded leaf execution progress watermark.
     *
     * @return {@code true} if consecutive fruitless iterations have reached or exceeded the idle budget,
     *         {@code false} otherwise
     */
    default boolean isLost() {

        return isLost(getLastProgressTestNumber());
    }

    /**
     * Evaluates whether wandering patience has been reached or exceeded since {@code progressTestNumber},
     * updating the current patience percentage and emitting trace/debug telemetry.
     *
     * @param progressTestNumber the completed test count at which leaf execution progress last occurred
     * @return {@code true} if consecutive fruitless iterations have reached or exceeded the idle budget,
     *         {@code false} otherwise
     */
    boolean isLost(int progressTestNumber);

    /**
     * Records leaf execution progress after a test iteration completes, advancing the progress watermark
     * whenever total executed leaf executions increase.
     */
    void recordCycleProgress();

    /**
     * Initialises progress watermarks from the current execution state prior to starting repetition.
     */
    void resetProgress();

    /**
     * Synchronises wandering patience state at the completion of a test cycle so telemetry and ETA
     * reflect cycle progress immediately without a one-cycle phase lag.
     *
     * @param testNum     the 1-based test cycle number that just completed
     * @param reachedGoal {@code true} if the cycle achieved a new required leaf execution goal, {@code false} otherwise
     */
    void syncAtCycleEnd(int testNum, boolean reachedGoal);
}
