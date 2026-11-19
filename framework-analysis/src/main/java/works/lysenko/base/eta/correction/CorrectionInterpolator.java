package works.lysenko.base.eta.correction;

import works.lysenko.base.eta.model.CorrectionRecord;
import works.lysenko.base.eta.model.TestError;
import works.lysenko.base.eta.model.TestSample;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure stateless interpolation and error-trajectory calculations for historical ETA calibration.
 */
public record CorrectionInterpolator() {

    /**
     * Exponential decay factor weighting more recent runs higher than older runs.
     */
    public static final double GAMMA = 0.8;

    private record Point(long x, long y) {
    }

    /**
     * Computes the exponentially-weighted moving average estimation correction across historical runs.
     *
     * @param matchingRecords chronological list of matching historical calibration records
     * @param timeMs          elapsed run duration in milliseconds
     * @return interpolated estimation error in milliseconds (positive = overestimation, negative = underestimation)
     */
    public static long computeWeightedCorrection(final List<CorrectionRecord> matchingRecords, final long timeMs) {
        if (null == matchingRecords || matchingRecords.isEmpty()) return 0L;

        final int count = matchingRecords.size();
        double sumWeightedY = 0.0;
        double sumWeights = 0.0;

        for (int m = 0; m < count; m++) {
            final CorrectionRecord rec = matchingRecords.get(m);
            final double weight = Math.pow(GAMMA, count - 1 - m);
            final long y = interpolateSingleRecord(rec, timeMs);
            sumWeightedY += weight * y;
            sumWeights += weight;
        }

        return (sumWeights > 0.0) ? Math.round(sumWeightedY / sumWeights) : 0L;
    }

    /**
     * Computes piecewise-linear interpolated estimation error at {@code timeMs} for a single historical record.
     *
     * @param record single calibration record
     * @param timeMs elapsed run duration in milliseconds
     * @return interpolated error in milliseconds
     */
    public static long interpolateSingleRecord(final CorrectionRecord record, final long timeMs) {
        if (null == record) return 0L;

        final List<TestError> errors = record.testErrors();
        if (null == errors || errors.isEmpty()) {
            return record.overEstimationMs();
        }

        final List<Point> points = new ArrayList<>();
        final long initialY = (record.overEstimationMs() != 0L)
                ? record.overEstimationMs()
                : errors.get(0).errorMs();
        points.add(new Point(0L, initialY));

        for (final TestError err : errors) {
            final Point p = new Point(err.passedMs(), err.errorMs());
            final Point prev = points.get(points.size() - 1);
            if (prev.x < p.x) {
                points.add(p);
            } else if (prev.x == p.x) {
                points.set(points.size() - 1, p);
            }
        }

        if (timeMs <= points.get(0).x) {
            return points.get(0).y;
        }

        final Point last = points.get(points.size() - 1);
        if (timeMs >= last.x) {
            return last.y;
        }

        for (int i = 0; i < points.size() - 1; i++) {
            final Point p1 = points.get(i);
            final Point p2 = points.get(i + 1);
            if (timeMs >= p1.x && timeMs <= p2.x) {
                if (p2.x == p1.x) return p1.y;
                final double ratio = (double) (timeMs - p1.x) / (double) (p2.x - p1.x);
                final double interpolated = p1.y + ratio * (p2.y - p1.y);
                return Math.round(interpolated);
            }
        }

        return last.y;
    }

    /**
     * Converts recorded runtime {@link TestSample} observations into post-run {@link TestError} metrics.
     *
     * @param samples       recorded per-cycle ETA samples
     * @param actualTotalMs actual completed run duration in milliseconds
     * @return list of computed {@link TestError} entries
     */
    public static List<TestError> buildTestErrors(final List<TestSample> samples, final long actualTotalMs) {
        if (null == samples || samples.isEmpty() || actualTotalMs <= 0L) return List.of();

        final List<TestError> testErrors = new ArrayList<>(samples.size());
        for (final TestSample sample : samples) {
            final long projectedTotalMs = sample.passedMs() + sample.etaMs();
            final long errorMs = projectedTotalMs - actualTotalMs;
            final double errorPercent = Math.round(((double) errorMs / actualTotalMs * 100.0) * 100.0) / 100.0;
            testErrors.add(new TestError(
                    sample.testNum(),
                    sample.passedMs(),
                    sample.etaMs(),
                    projectedTotalMs,
                    errorMs,
                    errorPercent));
        }
        return testErrors;
    }
}
