package works.lysenko.base.eta.correction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import works.lysenko.base.eta.model.CorrectionRecord;
import works.lysenko.base.eta.model.EtaMetadata;
import works.lysenko.util.spec.Layout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;

/**
 * JSON persistence repository managing historical {@link CorrectionRecord} entries in {@code var/eta_correction.json}.
 */
public record CorrectionStore() {

    /**
     * Relative path to the persisted ETA calibration dataset file.
     */
    public static final String FILE_PATH = Layout.Directories.VAR_ + "eta_correction.json";

    /**
     * Maximum number of historical run records retained per distinct configuration fingerprint.
     */
    public static final int MAX_HISTORY_PER_CONFIG = 5;

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Loads all historical calibration records matching the specified configuration metadata.
     *
     * @param metadata configuration metadata to match
     * @return chronological list of matching {@link CorrectionRecord} entries
     */
    public static List<CorrectionRecord> loadMatchingRecords(final EtaMetadata metadata) {
        if (null == metadata) return List.of();
        final File file = new File(FILE_PATH);
        if (!file.isFile()) return List.of();

        try {
            final List<CorrectionRecord> records = MAPPER.readValue(file, new TypeReference<>() {});
            if (null == records || records.isEmpty()) return List.of();

            final List<CorrectionRecord> matching = new ArrayList<>();
            for (final CorrectionRecord rec : records) {
                if (null != rec.metadata() && rec.metadata().matches(metadata)) {
                    matching.add(rec);
                }
            }
            return matching;
        } catch (final Exception e) {
            logEvent(S2, "Failed to load ETA correction file " + file.getAbsolutePath() + ": " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Appends a new completed run calibration record while pruning older entries beyond {@link #MAX_HISTORY_PER_CONFIG}.
     *
     * @param newRecord calibration record to persist
     */
    public static void appendRecord(final CorrectionRecord newRecord) {
        if (null == newRecord) return;

        final File file = new File(FILE_PATH);
        final File parentDir = file.getParentFile();
        if (null != parentDir && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        final List<CorrectionRecord> records = new ArrayList<>();
        if (file.isFile()) {
            try {
                final List<CorrectionRecord> loaded = MAPPER.readValue(file, new TypeReference<>() {});
                if (null != loaded) records.addAll(loaded);
            } catch (final Exception ignored) {
            }
        }

        final EtaMetadata meta = newRecord.metadata();
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
