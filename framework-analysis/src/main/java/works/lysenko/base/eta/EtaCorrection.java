package works.lysenko.base.eta;

import works.lysenko.base.eta.correction.CompletionSidecarReader;
import works.lysenko.base.eta.correction.CorrectionInterpolator;
import works.lysenko.base.eta.correction.CorrectionStore;
import works.lysenko.base.eta.correction.MetadataResolver;
import works.lysenko.base.eta.model.CorrectionRecord;
import works.lysenko.base.eta.model.EtaMetadata;
import works.lysenko.base.eta.model.TestError;
import works.lysenko.base.eta.model.TestSample;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade managing historical ETA calibration and runtime sample collection backed by {@code var/eta_correction.json}.
 */
@SuppressWarnings("StaticNonFinalField")
public record EtaCorrection() {

    private static long currentInitialPredictedTotalMs = 0L;
    private static final List<TestSample> currentTestSamples = new ArrayList<>();
    private static List<CorrectionRecord> cachedMatchingRecords = null;
    private static boolean recordsSearched = false;

    /**
     * Resets in-memory session state and cached historical lookup results.
     */
    public static synchronized void reset() {
        currentInitialPredictedTotalMs = 0L;
        currentTestSamples.clear();
        cachedMatchingRecords = null;
        recordsSearched = false;
    }

    /**
     * Records a runtime per-cycle ETA observation during active execution.
     *
     * @param testNum  1-based test cycle number
     * @param passedMs elapsed run duration in milliseconds
     * @param etaMs    estimated remaining duration in milliseconds
     */
    public static synchronized void recordTestSample(final int testNum, final long passedMs, final long etaMs) {
        if (passedMs > 0L && etaMs >= 0L) {
            currentTestSamples.add(new TestSample(testNum, passedMs, etaMs));
        }
    }

    /**
     * Records the first positive predicted total duration observed during the run.
     *
     * @param rawPredictedTotalMs initial predicted total run duration in milliseconds
     */
    public static synchronized void recordInitialPrediction(final long rawPredictedTotalMs) {
        if (currentInitialPredictedTotalMs == 0L && rawPredictedTotalMs > 0L) {
            currentInitialPredictedTotalMs = rawPredictedTotalMs;
        }
    }

    /**
     * Captures the active run's configuration metadata fingerprint.
     *
     * @return current {@link EtaMetadata}
     */
    public static EtaMetadata currentMetadata() {
        return MetadataResolver.resolveCurrent();
    }

    /**
     * Returns cached historical calibration records matching the current run metadata.
     *
     * @return matching {@link CorrectionRecord} entries
     */
    public static synchronized List<CorrectionRecord> getMatchingRecords() {
        if (!recordsSearched) {
            recordsSearched = true;
            cachedMatchingRecords = CorrectionStore.loadMatchingRecords(currentMetadata());
        }
        return cachedMatchingRecords;
    }

    /**
     * Returns the most recent historical calibration record matching the current run metadata, or {@code null}.
     *
     * @return latest matching {@link CorrectionRecord}, or {@code null}
     */
    public static synchronized CorrectionRecord getMatchingRecord() {
        final List<CorrectionRecord> list = getMatchingRecords();
        return (null != list && !list.isEmpty()) ? list.get(list.size() - 1) : null;
    }

    /**
     * Loads historical calibration records matching the specified metadata directly from disk.
     *
     * @param metadata configuration metadata to match
     * @return matching {@link CorrectionRecord} entries
     */
    public static synchronized List<CorrectionRecord> loadMatchingRecords(final EtaMetadata metadata) {
        return CorrectionStore.loadMatchingRecords(metadata);
    }

    /**
     * Loads the most recent historical calibration record matching the specified metadata, or {@code null}.
     *
     * @param metadata configuration metadata to match
     * @return latest matching {@link CorrectionRecord}, or {@code null}
     */
    public static synchronized CorrectionRecord loadMatchingRecord(final EtaMetadata metadata) {
        final List<CorrectionRecord> list = CorrectionStore.loadMatchingRecords(metadata);
        return (!list.isEmpty()) ? list.get(list.size() - 1) : null;
    }

    /**
     * Calculates the interpolated estimation correction (in milliseconds) for a given elapsed time.
     *
     * @param timeMs elapsed run duration in milliseconds
     * @return interpolated estimation error (positive for overestimation, negative for underestimation)
     */
    public static synchronized long getEstimationCorrection(final long timeMs) {
        return getEstimationCorrection(timeMs, currentMetadata());
    }

    /**
     * Calculates the interpolated estimation correction (in milliseconds) for a given elapsed time and metadata.
     *
     * @param timeMs   elapsed run duration in milliseconds
     * @param metadata configuration metadata to match
     * @return interpolated estimation error (positive for overestimation, negative for underestimation)
     */
    public static synchronized long getEstimationCorrection(final long timeMs, final EtaMetadata metadata) {
        final List<CorrectionRecord> matchingRecords = (null != metadata && metadata.matches(currentMetadata()))
                ? getMatchingRecords()
                : CorrectionStore.loadMatchingRecords(metadata);
        return CorrectionInterpolator.computeWeightedCorrection(matchingRecords, timeMs);
    }

    /**
     * Computes post-run estimation errors and appends a calibration record to {@code var/eta_correction.json}.
     *
     * @param actualTotalMs actual completed run duration in milliseconds
     */
    public static synchronized void recordRunCompletion(final long actualTotalMs) {
        if (actualTotalMs <= 0L) return;

        if (currentTestSamples.isEmpty()) {
            currentTestSamples.addAll(CompletionSidecarReader.readCurrentRunSamples());
        }

        final List<TestError> testErrors = CorrectionInterpolator.buildTestErrors(currentTestSamples, actualTotalMs);
        final long initialPredicted = (currentInitialPredictedTotalMs > 0L)
                ? currentInitialPredictedTotalMs
                : (!testErrors.isEmpty() ? testErrors.get(0).projectedTotalMs() : actualTotalMs);
        final long overEstimationMs = Math.max(0L, initialPredicted - actualTotalMs);

        final CorrectionRecord newRecord = new CorrectionRecord(
                currentMetadata(),
                initialPredicted,
                actualTotalMs,
                overEstimationMs,
                testErrors,
                System.currentTimeMillis());

        CorrectionStore.appendRecord(newRecord);
    }
}
