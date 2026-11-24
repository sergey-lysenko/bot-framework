package works.lysenko.base.eta.calc;

import works.lysenko.base.eta.model.CycleStats;
import works.lysenko.base.eta.model.EtaMode;

import java.util.List;

/**
 * Pure stateless mathematical formulas for remaining ETA projection and cycle statistics.
 */
public record EtaFormulas() {

    /**
     * Pure stateless ETA calculation for {@link EtaMode#EXHAUSTIVE} with wandering-patience blending.
     *
     * @param elapsedMs          elapsed execution duration in milliseconds
     * @param executed           executed leaf goals count
     * @param total              total leaf goals count
     * @param patiencePercentage current wandering patience percentage {@code [0..100]}
     * @param patienceEtaMs      estimated remaining patience window in ms, or {@code < 0} if unavailable
     * @return estimated remaining duration in milliseconds, or {@code 0L}
     */
    public static long estimateExhaustiveEtaMs(
            final long elapsedMs,
            final int executed,
            final int total,
            final double patiencePercentage,
            final long patienceEtaMs) {

        if (elapsedMs < 1000L || total <= 0) return 0L;

        long leafEtaMs = -1L;
        if (executed > 0 && total > executed) {
            final double avgMsPerExecution = (double) elapsedMs / executed;
            final int remainingExecutions = total - executed;
            leafEtaMs = Math.round(remainingExecutions * avgMsPerExecution);
        }

        final double w = Math.clamp(patiencePercentage / 100.0, 0.0, 1.0);
        final long rawEtaMs;
        if (leafEtaMs >= 0L && patienceEtaMs >= 0L && w > 0.0) {
            rawEtaMs = Math.round((1.0 - w) * leafEtaMs + w * patienceEtaMs);
        } else if (leafEtaMs >= 0L) {
            rawEtaMs = leafEtaMs;
        } else if (patienceEtaMs >= 0L && w > 0.0) {
            rawEtaMs = patienceEtaMs;
        } else {
            rawEtaMs = 0L;
        }
        return Math.max(0L, rawEtaMs);
    }

    /**
     * Pure stateless ETA calculation for {@link EtaMode#FIXED_TESTS}.
     *
     * @param elapsedMs      elapsed execution duration in milliseconds
     * @param completedTests completed test cycles count
     * @param totalTests     target total test cycles count
     * @return estimated remaining duration in milliseconds, or {@code 0L}
     */
    public static long estimateFixedTestsEtaMs(
            final long elapsedMs,
            final int completedTests,
            final int totalTests) {

        if (elapsedMs < 1000L || totalTests <= 0 || completedTests <= 0 || completedTests >= totalTests) {
            return 0L;
        }
        final double avgMsPerTest = (double) elapsedMs / completedTests;
        final int remainingTests = totalTests - completedTests;
        return Math.max(0L, Math.round(remainingTests * avgMsPerTest));
    }

    /**
     * Pure stateless ETA calculation for {@link EtaMode#LEAF_FALLBACK}.
     *
     * @param elapsedMs      elapsed execution duration in milliseconds
     * @param executedLeaves executed accessible leaves count
     * @param totalLeaves    total accessible leaves count
     * @return estimated remaining duration in milliseconds, or {@code 0L}
     */
    public static long estimateLeafFallbackEtaMs(
            final long elapsedMs,
            final int executedLeaves,
            final int totalLeaves) {

        if (elapsedMs < 1000L || totalLeaves <= 0 || executedLeaves <= 0 || executedLeaves >= totalLeaves) {
            return 0L;
        }
        final double avgMsPerLeaf = (double) elapsedMs / executedLeaves;
        final int remainingLeaves = totalLeaves - executedLeaves;
        return Math.max(0L, Math.round(remainingLeaves * avgMsPerLeaf));
    }

    /**
     * Computes mean, standard deviation, and predicted duration across per-cycle ETA samples.
     *
     * @param samples      non-null list of sampled projected-total or ETA values in milliseconds
     * @param initialEtaMs remaining ETA recorded at cycle start
     * @param finalEtaMs   remaining ETA recorded at cycle completion
     * @param realMs       actual elapsed cycle duration in milliseconds
     * @return aggregated {@link CycleStats}
     */
    public static CycleStats computeCycleStats(
            final List<Long> samples,
            final long initialEtaMs,
            final long finalEtaMs,
            final long realMs) {
        return computeCycleStats(samples, initialEtaMs, finalEtaMs, realMs, 0L);
    }

    /**
     * Computes mean, standard deviation, and predicted duration across per-cycle ETA samples.
     *
     * @param samples          non-null list of sampled projected-total or ETA values in milliseconds
     * @param initialEtaMs     remaining ETA recorded at cycle start
     * @param finalEtaMs       remaining ETA recorded at cycle completion
     * @param realMs           actual elapsed cycle duration in milliseconds
     * @param predictedCycleMs optional model-derived single-cycle duration prediction at cycle start, or {@code <= 0}
     * @return aggregated {@link CycleStats}
     */
    public static CycleStats computeCycleStats(
            final List<Long> samples,
            final long initialEtaMs,
            final long finalEtaMs,
            final long realMs,
            final long predictedCycleMs) {

        long sum = 0L;
        for (final long s : samples) {
            sum += s;
        }
        final long count = Math.max(1L, samples.size());
        final long meanEtaMs = sum / count;

        double sumVariance = 0.0;
        for (final long s : samples) {
            final double diff = s - meanEtaMs;
            sumVariance += diff * diff;
        }
        final long stdDevEtaMs = Math.round(Math.sqrt(sumVariance / count));

        final long predictedMs;
        if (predictedCycleMs > 0L) {
            predictedMs = predictedCycleMs;
        } else if (initialEtaMs > 0L && finalEtaMs > 0L) {
            predictedMs = Math.max(1L, initialEtaMs - finalEtaMs);
        } else if (meanEtaMs > 0L) {
            predictedMs = meanEtaMs;
        } else {
            predictedMs = realMs;
        }

        return new CycleStats(meanEtaMs, stdDevEtaMs, predictedMs);
    }
}
