package works.lysenko.base.eta.model;

import works.lysenko.base.eta.calc.EtaFormatter;

/**
 * Immutable runtime ETA and progress snapshot suitable for GUI labels and {@code dash/eta} JSON export.
 *
 * @param state                       current ETA lifecycle state
 * @param mode                        active estimation mode
 * @param runId                       session start epoch in milliseconds
 * @param updatedAtEpochMs            snapshot capture epoch in milliseconds
 * @param elapsedMs                   elapsed execution duration in milliseconds
 * @param remainingMs                 raw estimated remaining duration in milliseconds
 * @param correctionMs                historical calibration adjustment in milliseconds
 * @param correctedRemainingMs        calibrated remaining duration in milliseconds
 * @param currentTest                 current 1-based test cycle number
 * @param completedTests              completed test cycles count
 * @param totalTests                  configured total test cycles (or {@code 0} if unbounded)
 * @param executed                    executed progress units in the active mode
 * @param total                       total target progress units in the active mode
 * @param progressPercent             completion percentage {@code [0..100]}
 * @param patiencePercent             wandering patience percentage {@code [0..100]}
 * @param formattedRemaining          human-readable raw remaining duration
 * @param formattedCorrectedRemaining human-readable calibrated remaining duration
 * @param label                       compact status label (e.g. {@code "(ETA 01:05)"})
 */
public record EtaSnapshot(
        EtaState state,
        EtaMode mode,
        long runId,
        long updatedAtEpochMs,
        long elapsedMs,
        long remainingMs,
        long correctionMs,
        long correctedRemainingMs,
        int currentTest,
        int completedTests,
        int totalTests,
        int executed,
        int total,
        double progressPercent,
        double patiencePercent,
        String formattedRemaining,
        String formattedCorrectedRemaining,
        String label) {

    /**
     * Creates an unavailable placeholder snapshot when core runtime services are not yet initialized.
     *
     * @param nowEpochMs current epoch timestamp in milliseconds
     * @return unavailable {@link EtaSnapshot}
     */
    public static EtaSnapshot unavailable(final long nowEpochMs) {
        return new EtaSnapshot(
                EtaState.UNAVAILABLE,
                EtaMode.LEAF_FALLBACK,
                0L,
                nowEpochMs,
                0L,
                0L,
                0L,
                0L,
                0,
                0,
                0,
                0,
                0,
                0.0,
                0.0,
                EtaFormatter.PLACEHOLDER_DURATION,
                EtaFormatter.PLACEHOLDER_DURATION,
                EtaFormatter.LABEL_UNAVAILABLE
        );
    }

    /**
     * Returns a terminal {@link EtaState#DONE} copy of this snapshot at run teardown.
     *
     * @param nowEpochMs     completion epoch timestamp in milliseconds
     * @param finalElapsedMs final elapsed milliseconds
     * @return terminal snapshot marked {@link EtaState#DONE}
     */
    public EtaSnapshot asDone(final long nowEpochMs, final long finalElapsedMs) {
        final int doneExecuted = (total > 0) ? Math.max(executed, total) : executed;
        final double donePct = (total > 0) ? 100.0 : progressPercent;
        return new EtaSnapshot(
                EtaState.DONE,
                mode,
                runId,
                nowEpochMs,
                Math.max(elapsedMs, finalElapsedMs),
                0L,
                0L,
                0L,
                currentTest,
                completedTests,
                totalTests,
                doneExecuted,
                total,
                donePct,
                patiencePercent,
                EtaFormatter.DONE_TEXT,
                EtaFormatter.DONE_TEXT,
                EtaFormatter.LABEL_DONE
        );
    }

    /**
     * Serializes this snapshot as a compact JSON object for {@code dash/eta}.
     *
     * @param title current dashboard title string (may be {@code null})
     * @return single-line UTF-8 JSON representation
     */
    public String toJson(final String title) {
        return EtaFormatter.toJson(this, title);
    }
}
