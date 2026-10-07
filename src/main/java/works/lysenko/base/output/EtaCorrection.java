package works.lysenko.base.output;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import works.lysenko.Base;
import works.lysenko.util.spec.Layout;
import works.lysenko.util.spec.PropEnum;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Manages recording historical execution data stored in var/eta_correction.json.
 */
public final class EtaCorrection {

    private static final String FILE_PATH = Layout.Directories.VAR_ + "eta_correction.json";
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static long currentInitialPredictedTotalMs = 0L;
    private static final List<TestSample> currentTestSamples = new ArrayList<>();
    private static List<CorrectionRecord> cachedMatchingRecords = null;
    private static boolean recordsSearched = false;

    private EtaCorrection() {
    }

    public record TestSample(int testNum, long passedMs, long etaMs) {
    }

    public record TestError(
            int testNum,
            long passedMs,
            long etaMs,
            long projectedTotalMs,
            long errorMs,
            double errorPercent) {
    }

    public record Metadata(
            String test,
            String tests,
            boolean allLeafs,
            int allLeafsCount,
            String root,
            String app,
            int pauseLength,
            String platform) {

        public boolean matches(final Metadata other) {
            if (null == other) return false;
            return Objects.equals(test, other.test)
                    && Objects.equals(tests, other.tests)
                    && allLeafs == other.allLeafs
                    && allLeafsCount == other.allLeafsCount
                    && Objects.equals(root, other.root)
                    && Objects.equals(app, other.app)
                    && pauseLength == other.pauseLength
                    && Objects.equals(platform, other.platform);
        }
    }

    public record CorrectionRecord(
            Metadata metadata,
            long initialPredictedTotalMs,
            long actualTotalMs,
            long overEstimationMs,
            List<TestError> testErrors,
            long timestamp) {
    }

    private record Point(long x, long y) {
    }

    public static synchronized void reset() {
        currentInitialPredictedTotalMs = 0L;
        currentTestSamples.clear();
        cachedMatchingRecords = null;
        recordsSearched = false;
    }

    public static synchronized void recordTestSample(final int testNum, final long passedMs, final long etaMs) {
        if (passedMs > 0L && etaMs >= 0L) {
            currentTestSamples.add(new TestSample(testNum, passedMs, etaMs));
        }
    }

    public static Metadata currentMetadata() {
        final String test = (isNotNull(Base.parameters) && isNotNull(Base.parameters.getTest()))
                ? Base.parameters.getTest()
                : n(PropEnum._TEST_ROOT.get(), "default");
        final String tests = n(PropEnum._TEST_TESTS.get(), "");
        final boolean allLeafs = (isNotNull(Base.parameters) && Base.parameters.isAllLeafs())
                || Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get());
        final int allLeafsCount = (isNotNull(Base.parameters))
                ? Base.parameters.getAllLeafsCount()
                : Math.max(1, n(PropEnum._TEST_ALL_LEAFS_COUNT.get(), 1));
        final String root = n(PropEnum._TEST_ROOT.get(), "");
        final String app = n(PropEnum._TEST_APP.get(), "");
        final Integer pauseProp = PropEnum._TEST_PAUSE_LENGTH.get();
        final int pauseLength = (null != pauseProp) ? pauseProp : 5000;
        final String platform = n(PropEnum._WEBD_FORCED_PLATFORM.get(), "web");

        return new Metadata(test, tests, allLeafs, allLeafsCount, root, app, pauseLength, platform);
    }

    private static String n(final Object val, final String fallback) {
        return (null != val) ? val.toString() : fallback;
    }

    private static int n(final Integer val, final int fallback) {
        return (null != val) ? val : fallback;
    }

    public static synchronized void recordInitialPrediction(final long rawPredictedTotalMs) {
        if (currentInitialPredictedTotalMs == 0L && rawPredictedTotalMs > 0L) {
            currentInitialPredictedTotalMs = rawPredictedTotalMs;
        }
    }

    public static synchronized List<CorrectionRecord> getMatchingRecords() {
        if (!recordsSearched) {
            recordsSearched = true;
            cachedMatchingRecords = loadMatchingRecords(currentMetadata());
        }
        return cachedMatchingRecords;
    }

    public static synchronized CorrectionRecord getMatchingRecord() {
        final List<CorrectionRecord> list = getMatchingRecords();
        return (null != list && !list.isEmpty()) ? list.get(list.size() - 1) : null;
    }

    public static synchronized List<CorrectionRecord> loadMatchingRecords(final Metadata currentMeta) {
        final File file = new File(FILE_PATH);
        if (!file.isFile()) return List.of();

        try {
            final List<CorrectionRecord> records = MAPPER.readValue(file, new TypeReference<List<CorrectionRecord>>() {});
            if (null == records || records.isEmpty()) return List.of();

            final List<CorrectionRecord> matching = new ArrayList<>();
            for (final CorrectionRecord rec : records) {
                if (null != rec.metadata() && rec.metadata().matches(currentMeta)) {
                    matching.add(rec);
                }
            }
            return matching;
        } catch (final Exception e) {
            logEvent(S2, "Failed to load ETA correction file " + file.getAbsolutePath() + ": " + e.getMessage());
        }
        return List.of();
    }

    public static synchronized CorrectionRecord loadMatchingRecord(final Metadata currentMeta) {
        final List<CorrectionRecord> list = loadMatchingRecords(currentMeta);
        return (!list.isEmpty()) ? list.get(list.size() - 1) : null;
    }

    /**
     * Calculates the interpolated estimation correction (in milliseconds) for a given elapsed time.
     *
     * @param timeMs time moment in milliseconds (passed time)
     * @return interpolated estimation error (positive for overestimation, negative for underestimation)
     */
    public static synchronized long getEstimationCorrection(final long timeMs) {
        return getEstimationCorrection(timeMs, currentMetadata());
    }

    /**
     * Calculates the interpolated estimation correction (in milliseconds) for a given elapsed time and metadata.
     * Uses a weighted moving average across up to K recent historical runs.
     *
     * @param timeMs   time moment in milliseconds (passed time)
     * @param metadata configuration metadata to match
     * @return interpolated estimation error (positive for overestimation, negative for underestimation)
     */
    public static synchronized long getEstimationCorrection(final long timeMs, final Metadata metadata) {
        final List<CorrectionRecord> matchingRecords = (null != metadata && metadata.matches(currentMetadata()))
                ? getMatchingRecords()
                : loadMatchingRecords(metadata);

        if (null == matchingRecords || matchingRecords.isEmpty()) return 0L;

        final double GAMMA = 0.8;
        final int M = matchingRecords.size();
        double sumWeightedY = 0.0;
        double sumWeights = 0.0;

        for (int m = 0; m < M; m++) {
            final CorrectionRecord rec = matchingRecords.get(m);
            final double weight = Math.pow(GAMMA, M - 1 - m);
            final long y = getSingleRecordCorrection(rec, timeMs);
            sumWeightedY += weight * y;
            sumWeights += weight;
        }

        return (sumWeights > 0.0) ? Math.round(sumWeightedY / sumWeights) : 0L;
    }

    private static long getSingleRecordCorrection(final CorrectionRecord record, final long timeMs) {
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
            if (points.get(points.size() - 1).x < p.x) {
                points.add(p);
            } else if (points.get(points.size() - 1).x == p.x) {
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

    private static void loadTestSamplesFromSidecar() {
        try {
            final String sidecarPath = Layout.Files.name(Layout.Templates.RUN_ALL_LEAF_COMPLETIONS_);
            final File file = new File(sidecarPath);
            if (!file.isFile()) return;

            final Pattern pattern = Pattern.compile("\\[ALL_LEAF_COMPLETION]\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
            final List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (final String line : lines) {
                final Matcher m = pattern.matcher(line.trim());
                if (m.find()) {
                    final int testNum = Integer.parseInt(m.group(1));
                    final long passedMs = Long.parseLong(m.group(2));
                    final long etaMs = Long.parseLong(m.group(3));
                    currentTestSamples.add(new TestSample(testNum, passedMs, etaMs));
                }
            }
        } catch (final Exception ignored) {
        }
    }

    public static synchronized void recordRunCompletion(final long actualTotalMs) {
        if (actualTotalMs <= 0L) return;

        if (currentTestSamples.isEmpty()) {
            loadTestSamplesFromSidecar();
        }

        final List<TestError> testErrors = new ArrayList<>();
        for (final TestSample sample : currentTestSamples) {
            final long projectedTotalMs = sample.passedMs() + sample.etaMs();
            final long errorMs = projectedTotalMs - actualTotalMs;
            final double errorPercent = Math.round(((double) errorMs / actualTotalMs * 100.0) * 100.0) / 100.0;
            testErrors.add(new TestError(sample.testNum(), sample.passedMs(), sample.etaMs(), projectedTotalMs, errorMs, errorPercent));
        }

        final long initialPredicted = (currentInitialPredictedTotalMs > 0L)
                ? currentInitialPredictedTotalMs
                : (!testErrors.isEmpty() ? testErrors.get(0).projectedTotalMs() : actualTotalMs);
        final long overEstimationMs = Math.max(0L, initialPredicted - actualTotalMs);

        final Metadata meta = currentMetadata();
        final CorrectionRecord newRecord = new CorrectionRecord(
                meta,
                initialPredicted,
                actualTotalMs,
                overEstimationMs,
                testErrors,
                System.currentTimeMillis());

        final File file = new File(FILE_PATH);
        final File parentDir = file.getParentFile();
        if (null != parentDir && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        List<CorrectionRecord> records = new ArrayList<>();
        if (file.isFile()) {
            try {
                final List<CorrectionRecord> loaded = MAPPER.readValue(file, new TypeReference<List<CorrectionRecord>>() {});
                if (null != loaded) records.addAll(loaded);
            } catch (final Exception ignored) {
            }
        }

        final int MAX_HISTORY_PER_CONFIG = 5;
        final List<CorrectionRecord> matchingForMeta = new ArrayList<>();
        for (final CorrectionRecord rec : records) {
            if (null != rec.metadata() && rec.metadata().matches(meta)) {
                matchingForMeta.add(rec);
            }
        }

        while (matchingForMeta.size() >= MAX_HISTORY_PER_CONFIG) {
            final CorrectionRecord oldest = matchingForMeta.remove(0);
            records.remove(oldest);
        }

        records.add(newRecord);

        try {
            MAPPER.writeValue(file, records);
        } catch (final IOException e) {
            logEvent(S2, "Failed to save ETA correction file " + file.getAbsolutePath() + ": " + e.getMessage());
        }
    }
}
