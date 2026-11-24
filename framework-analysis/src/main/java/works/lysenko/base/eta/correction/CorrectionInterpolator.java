package works.lysenko.base.eta.correction;

import works.lysenko.base.eta.model.CorrectionRecord;
import works.lysenko.base.eta.model.EtaMetadata;
import works.lysenko.base.eta.model.TestError;
import works.lysenko.base.eta.model.TestSample;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Gatherers;

/**
 * Pure stateless interpolation and error-trajectory calculations for historical ETA calibration.
 */
public record CorrectionInterpolator() {

    /**
     * Exponential decay factor weighting more recent runs higher than older runs.
     */
    public static final double GAMMA = 0.8;

    /**
     * Minimum multiplicative calibration scale applied to raw remaining ETA (caps extreme overestimation damping).
     */
    public static final double MIN_SCALE = 0.05;

    /**
     * Maximum multiplicative calibration scale applied to raw remaining ETA (prevents runaway inflation).
     */
    public static final double MAX_SCALE = 3.0;

    private record Point(long x, long y) {
    }

    private record ScalePoint(double x, double y) {
    }

    /**
     * Checks whether a persisted {@link CorrectionRecord} represents a valid completed run rather than
     * an aborted/truncated session that would taint historical ETA calibration.
     *
     * @param record calibration record to validate
     * @return {@code true} if the record is valid for calibration
     */
    public static boolean isValidRecord(final CorrectionRecord record) {
        if (null == record || record.actualTotalMs() <= 0L) return false;

        final List<TestError> errors = record.testErrors();
        if (null == errors || errors.isEmpty()) {
            return true;
        }

        final TestError last = errors.getLast();
        final EtaMetadata meta = record.metadata();
        final long actualTotal = record.actualTotalMs();
        final int targetTests = parsePositiveInt((null != meta) ? meta.tests() : null);

        if (targetTests > 0) {
            return last.testNum() >= targetTests;
        }
        if (null != meta && meta.allLeafs()) {
            if (last.etaMs() > 0L
                    && (last.passedMs() >= actualTotal * 0.5 || last.errorMs() > actualTotal * 0.15)) {
                return false;
            }
            return true;
        }
        return !(last.passedMs() >= actualTotal * 0.8 && last.etaMs() > actualTotal * 0.25);
    }

    /**
     * Computes the exponentially-weighted moving average estimation correction across valid historical runs.
     *
     * @param matchingRecords chronological list of matching historical calibration records
     * @param timeMs          elapsed run duration in milliseconds
     * @return interpolated estimation error in milliseconds (positive = overestimation, negative = underestimation)
     */
    public static long computeWeightedCorrection(final List<CorrectionRecord> matchingRecords, final long timeMs) {
        if (null == matchingRecords || matchingRecords.isEmpty()) return 0L;

        final List<CorrectionRecord> validRecords = new ArrayList<>(matchingRecords.size());
        for (final CorrectionRecord rec : matchingRecords) {
            if (isValidRecord(rec)) {
                validRecords.add(rec);
            }
        }
        if (validRecords.isEmpty()) return 0L;

        final int count = validRecords.size();
        double sumWeightedY = 0.0;
        double sumWeights = 0.0;

        for (int m = 0; m < count; m++) {
            final CorrectionRecord rec = validRecords.get(m);
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
        if (!isValidRecord(record)) return 0L;

        final List<TestError> errors = record.testErrors();
        if (null == errors || errors.isEmpty()) {
            return record.overEstimationMs();
        }
        if (timeMs >= record.actualTotalMs()) {
            return 0L;
        }

        final List<Point> points = new ArrayList<>();
        final long initialY = (record.overEstimationMs() != 0L)
                ? record.overEstimationMs()
                : errors.getFirst().errorMs();
        points.add(new Point(0L, initialY));

        for (final TestError err : errors) {
            final Point p = new Point(err.passedMs(), err.errorMs());
            final Point prev = points.getLast();
            if (prev.x < p.x) {
                points.add(p);
            } else if (prev.x == p.x) {
                points.set(points.size() - 1, p);
            }
        }

        if (timeMs <= points.getFirst().x) {
            return points.getFirst().y;
        }

        final Point last = points.getLast();
        if (timeMs >= last.x) {
            return last.y;
        }

        return points.stream()
                .gather(Gatherers.windowSliding(2))
                .filter(seg -> timeMs >= seg.getFirst().x && timeMs <= seg.getLast().x)
                .findFirst()
                .map(seg -> {
                    final Point p1 = seg.getFirst();
                    final Point p2 = seg.getLast();
                    if (p2.x == p1.x) return p1.y;
                    final double ratio = (double) (timeMs - p1.x) / (double) (p2.x - p1.x);
                    return Math.round(p1.y + ratio * (p2.y - p1.y));
                })
                .orElse(last.y);
    }

    /**
     * Computes a speed-invariant, progress-indexed multiplicative calibration factor
     * ({@code actualRemaining / rawEta}) across valid historical runs.
     *
     * @param matchingRecords chronological list of matching historical calibration records
     * @param rawProgress     current raw progress fraction {@code elapsed / (elapsed + rawEta)} in {@code [0..1]}
     * @return weighted multiplicative scale factor, or {@code 1.0} if no valid calibration signal exists
     */
    public static double computeWeightedRemainingScale(
            final List<CorrectionRecord> matchingRecords,
            final double rawProgress) {
        if (null == matchingRecords || matchingRecords.isEmpty()) return 1.0;

        final List<Double> scales = new ArrayList<>(matchingRecords.size());
        for (final CorrectionRecord rec : matchingRecords) {
            final Double scale = interpolateRemainingScale(rec, rawProgress);
            if (null != scale) {
                scales.add(scale);
            }
        }
        if (scales.isEmpty()) return 1.0;

        final int count = scales.size();
        double sumWeighted = 0.0;
        double sumWeights = 0.0;
        for (int m = 0; m < count; m++) {
            final double weight = Math.pow(GAMMA, count - 1 - m);
            sumWeighted += weight * scales.get(m);
            sumWeights += weight;
        }
        return (sumWeights > 0.0) ? clampScale(sumWeighted / sumWeights) : 1.0;
    }

    /**
     * Computes piecewise-linear interpolated remaining-ETA scale ({@code actualRemaining / rawEta})
     * at {@code rawProgress} for a single historical record.
     *
     * @param record      single calibration record
     * @param rawProgress current raw progress fraction in {@code [0..1]}
     * @return interpolated scale factor in {@code [MIN_SCALE..MAX_SCALE]}, or {@code null} if record is invalid or has no ETA samples
     */
    public static Double interpolateRemainingScale(final CorrectionRecord record, final double rawProgress) {
        if (!isValidRecord(record)) return null;

        final long actualTotal = record.actualTotalMs();
        final List<TestError> errors = record.testErrors();
        if (null == errors || errors.isEmpty()) {
            return (record.initialPredictedTotalMs() > 0L)
                    ? clampScale((double) actualTotal / (double) record.initialPredictedTotalMs())
                    : null;
        }

        final List<ScalePoint> rawPoints = new ArrayList<>(errors.size());
        for (final TestError err : errors) {
            if (err.passedMs() <= 0L || err.etaMs() <= 0L) continue;
            final long actualRemaining = actualTotal - err.passedMs();
            if (actualRemaining <= 0L) continue;
            final double p = (double) err.passedMs() / (double) (err.passedMs() + err.etaMs());
            final double s = clampScale((double) actualRemaining / (double) err.etaMs());
            rawPoints.add(new ScalePoint(p, s));
        }
        if (rawPoints.isEmpty()) return null;

        rawPoints.sort(Comparator.comparingDouble(ScalePoint::x));

        final List<ScalePoint> merged = new ArrayList<>(rawPoints.size());
        int idx = 0;
        while (idx < rawPoints.size()) {
            double sumX = rawPoints.get(idx).x;
            double sumY = rawPoints.get(idx).y;
            int n = 1;
            while (idx + n < rawPoints.size() && (rawPoints.get(idx + n).x - rawPoints.get(idx).x) <= 1e-6) {
                sumX += rawPoints.get(idx + n).x;
                sumY += rawPoints.get(idx + n).y;
                n++;
            }
            merged.add(new ScalePoint(sumX / n, sumY / n));
            idx += n;
        }

        final List<ScalePoint> points = new ArrayList<>(merged.size() + 1);
        if (record.initialPredictedTotalMs() > 0L && merged.getFirst().x > 1e-6) {
            points.add(new ScalePoint(0.0, clampScale((double) actualTotal / (double) record.initialPredictedTotalMs())));
        }
        points.addAll(merged);

        final double clampedProgress = Math.clamp(rawProgress, 0.0, 1.0);
        if (clampedProgress <= points.getFirst().x) {
            return points.getFirst().y;
        }
        final ScalePoint last = points.getLast();
        if (clampedProgress >= last.x) {
            return last.y;
        }

        return points.stream()
                .gather(Gatherers.windowSliding(2))
                .filter(seg -> clampedProgress >= seg.getFirst().x && clampedProgress <= seg.getLast().x)
                .findFirst()
                .map(seg -> {
                    final ScalePoint p1 = seg.getFirst();
                    final ScalePoint p2 = seg.getLast();
                    if (p2.x <= p1.x) return p1.y;
                    final double ratio = (clampedProgress - p1.x) / (p2.x - p1.x);
                    return clampScale(p1.y + ratio * (p2.y - p1.y));
                })
                .orElse(last.y);
    }

    /**
     * Applies progress-indexed multiplicative historical calibration to a raw remaining ETA estimate.
     * Always preserves a positive remaining estimate whenever {@code rawEtaMs > 0}, avoiding sign flips and fallback cliffs.
     *
     * @param elapsedMs       elapsed run duration in milliseconds
     * @param rawEtaMs        uncalibrated remaining ETA in milliseconds
     * @param matchingRecords chronological list of matching historical calibration records
     * @return calibrated remaining duration in milliseconds
     */
    public static long applyRemainingScale(
            final long elapsedMs,
            final long rawEtaMs,
            final List<CorrectionRecord> matchingRecords) {
        if (rawEtaMs <= 0L) return 0L;
        if (null == matchingRecords || matchingRecords.isEmpty()) return rawEtaMs;

        final double rawProgress = (elapsedMs > 0L)
                ? ((double) elapsedMs / (double) (elapsedMs + rawEtaMs))
                : 0.0;
        final double scale = computeWeightedRemainingScale(matchingRecords, rawProgress);
        return Math.max(1L, Math.round(rawEtaMs * scale));
    }

    private static double clampScale(final double scale) {
        if (Double.isNaN(scale) || Double.isInfinite(scale)) return 1.0;
        return Math.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    private static int parsePositiveInt(final String value) {
        if (null == value || value.isBlank()) return 0;
        try {
            final int parsed = Integer.parseInt(value.trim());
            return Math.max(0, parsed);
        } catch (final NumberFormatException _) {
            return 0;
        }
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
