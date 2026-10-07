package works.lysenko.base.output;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import works.lysenko.Base;
import works.lysenko.util.spec.Layout;
import works.lysenko.util.spec.PropEnum;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Manages adaptive ETA correction based on historical execution data stored in var/eta_correction.json.
 * Mitigates early over-estimation by applying a fading correction coefficient for matching test configurations.
 */
public final class EtaCorrection {

    /**
     * Constant coefficient used for fine-tuning the applied ETA correction.
     */
    public static final double CORRECTION_COEFFICIENT = 0.5;

    private static final String FILE_PATH = Layout.Directories.VAR_ + "eta_correction.json";
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static long currentInitialPredictedTotalMs = 0L;
    private static CorrectionRecord cachedMatchingRecord = null;
    private static boolean recordSearched = false;

    private EtaCorrection() {
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
            long timestamp) {
    }

    public static synchronized void reset() {
        currentInitialPredictedTotalMs = 0L;
        cachedMatchingRecord = null;
        recordSearched = false;
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

    public static synchronized long adjustEta(final long rawEtaMs, final double progressRatio) {
        if (rawEtaMs <= 0L) return 0L;

        final CorrectionRecord record = getMatchingRecord();
        if (null == record || record.overEstimationMs() <= 0L) {
            return rawEtaMs;
        }

        final double fadingWeight = Math.max(0.0, 1.0 - Math.min(1.0, Math.max(0.0, progressRatio)));
        final long correctionMs = Math.round(record.overEstimationMs() * CORRECTION_COEFFICIENT * fadingWeight);
        return Math.max(0L, rawEtaMs - correctionMs);
    }

    public static synchronized CorrectionRecord getMatchingRecord() {
        if (!recordSearched) {
            recordSearched = true;
            cachedMatchingRecord = loadMatchingRecord(currentMetadata());
        }
        return cachedMatchingRecord;
    }

    private static CorrectionRecord loadMatchingRecord(final Metadata currentMeta) {
        final File file = new File(FILE_PATH);
        if (!file.isFile()) return null;

        try {
            final List<CorrectionRecord> records = MAPPER.readValue(file, new TypeReference<List<CorrectionRecord>>() {});
            if (null == records || records.isEmpty()) return null;

            for (int i = records.size() - 1; i >= 0; i--) {
                final CorrectionRecord rec = records.get(i);
                if (null != rec.metadata() && rec.metadata().matches(currentMeta)) {
                    return rec;
                }
            }
        } catch (final Exception e) {
            logEvent(S2, "Failed to load ETA correction file " + file.getAbsolutePath() + ": " + e.getMessage());
        }
        return null;
    }

    public static synchronized void recordRunCompletion(final long actualTotalMs) {
        if (actualTotalMs <= 0L || currentInitialPredictedTotalMs <= 0L) return;

        final long overEstimationMs = Math.max(0L, currentInitialPredictedTotalMs - actualTotalMs);
        final Metadata meta = currentMetadata();
        final CorrectionRecord newRecord = new CorrectionRecord(meta, currentInitialPredictedTotalMs, actualTotalMs, overEstimationMs, System.currentTimeMillis());

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

        records.removeIf(rec -> null != rec.metadata() && rec.metadata().matches(meta));
        records.add(newRecord);

        try {
            MAPPER.writeValue(file, records);
        } catch (final IOException e) {
            logEvent(S2, "Failed to save ETA correction file " + file.getAbsolutePath() + ": " + e.getMessage());
        }
    }
}
