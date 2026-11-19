package works.lysenko.base.eta.model;

/**
 * Aggregated ETA telemetry statistics for a single scenario execution cycle.
 *
 * @param meanEtaMs      arithmetic mean of sampled ETA values in milliseconds
 * @param stdDevEtaMs    standard deviation of sampled ETA values in milliseconds
 * @param predictedMs    cycle-level predicted duration in milliseconds
 */
public record CycleStats(long meanEtaMs, long stdDevEtaMs, long predictedMs) {
}
