package works.lysenko.base.output;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Parameters;
import works.lysenko.base.output.EtaCorrection.CorrectionRecord;
import works.lysenko.base.output.EtaCorrection.Metadata;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EtaCorrectionTest {

    private Parameters previousParameters;
    private final File correctionFile = new File("var/eta_correction.json");

    @BeforeEach
    void setUp() throws Exception {
        previousParameters = Base.parameters;
        EtaCorrection.reset();
        if (correctionFile.exists()) {
            correctionFile.delete();
        }
    }

    @AfterEach
    void tearDown() {
        Base.parameters = previousParameters;
        EtaCorrection.reset();
        if (correctionFile.exists()) {
            correctionFile.delete();
        }
    }

    @Test
    void metadataMatchingComparesAllCrucialParameters() {
        final Metadata meta1 = new Metadata("TestA", "10", true, 5, "RootA", "AppA", 5000, "web");
        final Metadata meta2 = new Metadata("TestA", "10", true, 5, "RootA", "AppA", 5000, "web");
        final Metadata meta3 = new Metadata("TestA", "20", true, 5, "RootA", "AppA", 5000, "web");

        assertTrue(meta1.matches(meta2));
        assertFalse(meta1.matches(meta3));
    }

    @Test
    void recordRunCompletionSavesDatasetToVarDirectory() throws Exception {
        final Properties props = new Properties();
        props.setProperty("TEST", "Suite1");
        props.setProperty("PLATFORM", "chrome");
        props.setProperty("ALL_LEAFS", "true");
        props.setProperty("ALL_LEAFS_COUNT", "2");

        final Parameters parameters = new Parameters(props);
        Base.parameters = parameters;

        // Record initial prediction of 120s total, actual run time 60s
        EtaCorrection.recordInitialPrediction(120_000L);
        EtaCorrection.recordRunCompletion(60_000L);

        assertTrue(correctionFile.exists(), "Correction dataset file should be created in var directory");

        final String fileContent = Files.readString(correctionFile.toPath());
        assertTrue(fileContent.contains("Suite1"));
        assertTrue(fileContent.contains("overEstimationMs"));

        EtaCorrection.reset();

        final CorrectionRecord matching = EtaCorrection.getMatchingRecord();
        assertNotNull(matching, "Saved matching record should be retrieved");
        assertEquals(60_000L, matching.overEstimationMs());
    }

    @Test
    void adjustEtaMitigatesOverEstimationFadingWithProgress() throws Exception {
        final Properties props = new Properties();
        props.setProperty("TEST", "Suite2");
        props.setProperty("PLATFORM", "chrome");

        final Parameters parameters = new Parameters(props);
        Base.parameters = parameters;

        // Simulate historical record with 60,000ms over-estimation
        EtaCorrection.recordInitialPrediction(120_000L);
        EtaCorrection.recordRunCompletion(60_000L);

        EtaCorrection.reset();

        // At progress 0.0 (start of test): raw ETA 100s, correction = 60s * 0.5 * 1.0 = 30s
        final long adjustedStart = EtaCorrection.adjustEta(100_000L, 0.0);
        assertEquals(70_000L, adjustedStart, "Early ETA should be reduced by correction amount");

        // At progress 0.5 (halfway): raw ETA 50s, correction = 60s * 0.5 * 0.5 = 15s
        final long adjustedMid = EtaCorrection.adjustEta(50_000L, 0.5);
        assertEquals(35_000L, adjustedMid, "Halfway ETA correction should fade");

        // At progress 1.0 (completion): raw ETA 10s, correction = 0s
        final long adjustedEnd = EtaCorrection.adjustEta(10_000L, 1.0);
        assertEquals(10_000L, adjustedEnd, "Final ETA correction should fade to 0");
    }
}
