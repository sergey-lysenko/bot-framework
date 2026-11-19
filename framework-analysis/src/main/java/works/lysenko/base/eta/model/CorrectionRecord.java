package works.lysenko.base.eta.model;

import java.util.List;

/**
 * Persisted historical run calibration entry stored in {@code var/eta_correction.json}.
 *
 * @param metadata                configuration fingerprint of the run
 * @param initialPredictedTotalMs initial predicted total run duration in milliseconds
 * @param actualTotalMs           actual completed run duration in milliseconds
 * @param overEstimationMs        non-negative initial overestimation in milliseconds
 * @param testErrors              per-cycle estimation error trajectory
 * @param timestamp               completion epoch timestamp in milliseconds
 */
public record CorrectionRecord(
        EtaMetadata metadata,
        long initialPredictedTotalMs,
        long actualTotalMs,
        long overEstimationMs,
        List<TestError> testErrors,
        long timestamp) {
}
