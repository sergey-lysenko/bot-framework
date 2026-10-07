package works.lysenko.base.output;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Parameters;
import works.lysenko.base.output.EtaCorrection.CorrectionRecord;
import works.lysenko.base.output.EtaCorrection.Metadata;
import works.lysenko.base.output.EtaCorrection.TestError;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
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

        final ObjectMapper mapper = new ObjectMapper();
        final List<CorrectionRecord> records = mapper.readValue(correctionFile, new TypeReference<List<CorrectionRecord>>() {});
        assertNotNull(records, "Saved records should be deserialized");
        assertEquals(1, records.size());
        assertEquals(60_000L, records.get(0).overEstimationMs());
    }

    @Test
    void recordRunCompletionSavesPerTestErrorsDataset() throws Exception {
        final Properties props = new Properties();
        props.setProperty("TEST", "SuitePerTest");
        props.setProperty("PLATFORM", "chrome");

        Base.parameters = new Parameters(props);

        // Record per-test samples (passedMs, etaMs)
        EtaCorrection.recordTestSample(1, 10_000L, 100_000L); // Projected = 110,000ms
        EtaCorrection.recordTestSample(2, 25_000L, 80_000L);  // Projected = 105,000ms

        // Run completes in 100,000ms
        EtaCorrection.recordRunCompletion(100_000L);

        final ObjectMapper mapper = new ObjectMapper();
        final List<CorrectionRecord> records = mapper.readValue(correctionFile, new TypeReference<List<CorrectionRecord>>() {});
        assertEquals(1, records.size());

        final List<TestError> errors = records.get(0).testErrors();
        assertNotNull(errors);
        assertEquals(2, errors.size());

        final TestError err1 = errors.get(0);
        assertEquals(1, err1.testNum());
        assertEquals(10_000L, err1.passedMs());
        assertEquals(100_000L, err1.etaMs());
        assertEquals(110_000L, err1.projectedTotalMs());
        assertEquals(10_000L, err1.errorMs(), "Overestimation error should be positive");
        assertEquals(10.0, err1.errorPercent());

        final TestError err2 = errors.get(1);
        assertEquals(2, err2.testNum());
        assertEquals(25_000L, err2.passedMs());
        assertEquals(80_000L, err2.etaMs());
        assertEquals(105_000L, err2.projectedTotalMs());
        assertEquals(5_000L, err2.errorMs(), "Overestimation error should be positive");
        assertEquals(5.0, err2.errorPercent());
    }

    @Test
    void recordRunCompletionHandlesUnderestimation() throws Exception {
        final Properties props = new Properties();
        props.setProperty("TEST", "SuiteUnderestimation");

        Base.parameters = new Parameters(props);

        // Projected = 80,000ms, actual = 100,000ms
        EtaCorrection.recordTestSample(1, 10_000L, 70_000L);
        EtaCorrection.recordRunCompletion(100_000L);

        final ObjectMapper mapper = new ObjectMapper();
        final List<CorrectionRecord> records = mapper.readValue(correctionFile, new TypeReference<List<CorrectionRecord>>() {});
        assertEquals(1, records.size());

        final TestError err = records.get(0).testErrors().get(0);
        assertEquals(-20_000L, err.errorMs(), "Underestimation error should be negative");
        assertEquals(-20.0, err.errorPercent());
    }

    @Test
    void getEstimationCorrectionInterpolatesOverTime() throws Exception {
        final Properties props = new Properties();
        props.setProperty("TEST", "InterpolationSuite");
        Base.parameters = new Parameters(props);

        // Record initial prediction 120s total -> overestimation at t=0 is 20,000ms
        EtaCorrection.recordInitialPrediction(120_000L);
        // Test 1: passed 10s, ETA 100s -> projected 110s -> error +10,000ms
        EtaCorrection.recordTestSample(1, 10_000L, 100_000L);
        // Test 2: passed 25s, ETA 80s -> projected 105s -> error +5,000ms
        EtaCorrection.recordTestSample(2, 25_000L, 80_000L);

        // Actual run completion 100s
        EtaCorrection.recordRunCompletion(100_000L);

        EtaCorrection.reset();
        Base.parameters = new Parameters(props);

        // At t = 0: initial error = 20,000ms
        assertEquals(20_000L, EtaCorrection.getEstimationCorrection(0L));

        // At t = 5,000ms (halfway between 0 and 10,000): interpolated = 15,000ms
        assertEquals(15_000L, EtaCorrection.getEstimationCorrection(5_000L));

        // At t = 10,000ms: error = 10,000ms
        assertEquals(10_000L, EtaCorrection.getEstimationCorrection(10_000L));

        // At t = 17,500ms (halfway between 10,000 and 25,000): interpolated = 7,500ms
        assertEquals(7_500L, EtaCorrection.getEstimationCorrection(17_500L));

        // At t = 25,000ms: error = 5,000ms
        assertEquals(5_000L, EtaCorrection.getEstimationCorrection(25_000L));

        // Beyond last test (t = 50,000ms): returns last error 5,000ms
        assertEquals(5_000L, EtaCorrection.getEstimationCorrection(50_000L));
    }

    @Test
    void multiRunHistoryWeightedMovingAverage() throws Exception {
        final Properties props = new Properties();
        props.setProperty("TEST", "MultiRunSuite");
        Base.parameters = new Parameters(props);

        // Run 1: initial prediction 120s, actual 100s -> error at t=0 is 20,000ms
        EtaCorrection.recordInitialPrediction(120_000L);
        EtaCorrection.recordRunCompletion(100_000L);

        EtaCorrection.reset();
        Base.parameters = new Parameters(props);

        // Run 2: initial prediction 110s, actual 100s -> error at t=0 is 10,000ms
        EtaCorrection.recordInitialPrediction(110_000L);
        EtaCorrection.recordRunCompletion(100_000L);

        // Check history saved in var/eta_correction.json
        final ObjectMapper mapper = new ObjectMapper();
        final List<CorrectionRecord> records = mapper.readValue(correctionFile, new TypeReference<List<CorrectionRecord>>() {});
        assertEquals(2, records.size(), "Both run records should be retained in history");

        EtaCorrection.reset();
        Base.parameters = new Parameters(props);

        // Weighted moving average for t=0:
        // Run 1 (older): weight = 0.8^1 = 0.8, y1 = 20,000ms
        // Run 2 (newer): weight = 0.8^0 = 1.0, y2 = 10,000ms
        // Weighted average = (0.8 * 20,000 + 1.0 * 10,000) / 1.8 = 26,000 / 1.8 = 14,444ms
        final long weightedCorrection = EtaCorrection.getEstimationCorrection(0L);
        assertEquals(14_444L, weightedCorrection, "Correction should be weighted average of recent runs");
    }
}
