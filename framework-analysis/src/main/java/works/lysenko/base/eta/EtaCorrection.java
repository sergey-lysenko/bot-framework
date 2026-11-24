package works.lysenko.base.eta;

import works.lysenko.Base;
import works.lysenko.base.eta.calc.EtaModeResolver;
import works.lysenko.base.eta.correction.CompletionSidecarReader;
import works.lysenko.base.eta.correction.CorrectionInterpolator;
import works.lysenko.base.eta.correction.CorrectionStore;
import works.lysenko.base.eta.correction.MetadataResolver;
import works.lysenko.base.eta.model.CorrectionRecord;
import works.lysenko.base.eta.model.EtaMetadata;
import works.lysenko.base.eta.model.TestError;
import works.lysenko.base.eta.model.TestSample;
import works.lysenko.util.prop.core.Mode;

import java.util.ArrayList;
import java.util.List;

import static works.lysenko.util.func.type.Objects.isNotNull;

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
        return (null != list && !list.isEmpty()) ? list.getLast() : null;
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
        return (!list.isEmpty()) ? list.getLast() : null;
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
     * Computes a speed-invariant, progress-indexed calibrated remaining duration for the active run metadata.
     *
     * @param elapsedMs elapsed run duration in milliseconds
     * @param rawEtaMs  uncalibrated remaining ETA in milliseconds
     * @return calibrated remaining duration in milliseconds (equals {@code rawEtaMs} when no valid history exists)
     */
    public static synchronized long estimateCorrectedRemainingMs(final long elapsedMs, final long rawEtaMs) {
        return estimateCorrectedRemainingMs(elapsedMs, rawEtaMs, currentMetadata());
    }

    /**
     * Computes a speed-invariant, progress-indexed calibrated remaining duration for the specified metadata.
     *
     * @param elapsedMs elapsed run duration in milliseconds
     * @param rawEtaMs  uncalibrated remaining ETA in milliseconds
     * @param metadata  configuration metadata to match
     * @return calibrated remaining duration in milliseconds (equals {@code rawEtaMs} when no valid history exists)
     */
    public static synchronized long estimateCorrectedRemainingMs(
            final long elapsedMs,
            final long rawEtaMs,
            final EtaMetadata metadata) {
        if (rawEtaMs <= 0L) return 0L;
        final List<CorrectionRecord> matchingRecords = (null != metadata && metadata.matches(currentMetadata()))
                ? getMatchingRecords()
                : CorrectionStore.loadMatchingRecords(metadata);
        return CorrectionInterpolator.applyRemainingScale(elapsedMs, rawEtaMs, matchingRecords);
    }

    /**
     * Checks whether the active runtime test session completed its configured target without being
     * stopped preemptively, aborted on failure, or cut short before finishing all leaves/tests.
     * When {@link Base#core} is {@code null} (standalone unit tests), returns {@code true}.
     *
     * @return {@code true} if the current run is eligible to be persisted for historical calibration
     */
    public static synchronized boolean isCurrentRunCompleted() {
        if (!isNotNull(Base.core)) return true;
        if (Base.core.getStopFlag()) return false;
        if (isNotNull(Base.core.getResults()) && !Mode.resilient && Base.core.getResults().areFailingEvents()) {
            return false;
        }
        final int completedTests = EtaModeResolver.resolveCompletedTests();
        final int testsCount = Base.core.getTestsCount();
        if (completedTests <= 0 && testsCount <= 0 && currentTestSamples.isEmpty()) {
            return false;
        }
        if (EtaModeResolver.isExhaustiveMode()) {
            final var leaves = Base.core.getAccessibleLeaves();
            return isNotNull(leaves) && !leaves.isEmpty() && Base.core.areAllLeavesExecuted();
        }
        final int totalTests = EtaModeResolver.resolveTotalTests();
        if (totalTests > 0) {
            return completedTests >= totalTests;
        }
        return true;
    }

    /**
     * Computes post-run estimation errors and appends a calibration record to {@code var/eta_correction.json}
     * if and only if the run completed legitimately without being stopped or aborted early.
     *
     * @param actualTotalMs actual completed run duration in milliseconds
     */
    public static synchronized void recordRunCompletion(final long actualTotalMs) {
        if (actualTotalMs <= 0L || !isCurrentRunCompleted()) return;

        if (currentTestSamples.isEmpty()) {
            currentTestSamples.addAll(CompletionSidecarReader.readCurrentRunSamples());
        }

        final List<TestError> testErrors = CorrectionInterpolator.buildTestErrors(currentTestSamples, actualTotalMs);
        final long initialPredicted = (currentInitialPredictedTotalMs > 0L)
                ? currentInitialPredictedTotalMs
                : (!testErrors.isEmpty() ? testErrors.getFirst().projectedTotalMs() : actualTotalMs);
        final long overEstimationMs = Math.max(0L, initialPredicted - actualTotalMs);

        final CorrectionRecord newRecord = new CorrectionRecord(
                currentMetadata(),
                initialPredicted,
                actualTotalMs,
                overEstimationMs,
                testErrors,
                System.currentTimeMillis());

        if (!CorrectionInterpolator.isValidRecord(newRecord)) return;

        CorrectionStore.appendRecord(newRecord);
    }
}
