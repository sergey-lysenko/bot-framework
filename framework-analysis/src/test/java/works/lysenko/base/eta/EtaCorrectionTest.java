package works.lysenko.base.eta;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Parameters;
import works.lysenko.base.TestProperties;
import works.lysenko.base.eta.correction.CompletionSidecarReader;
import works.lysenko.base.eta.model.CorrectionRecord;
import works.lysenko.base.eta.model.EtaMetadata;
import works.lysenko.base.eta.model.TestError;
import works.lysenko.base.eta.model.TestSample;
import works.lysenko.util.prop.PropEnum;

import java.io.File;
import java.lang.reflect.Field;
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
    void setUp() {
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
        final EtaMetadata meta1 = new EtaMetadata("TestA", "10", true, 5, "RootA", "AppA", 5000, "web");
        final EtaMetadata meta2 = new EtaMetadata("TestA", "10", true, 5, "RootA", "AppA", 5000, "web");
        final EtaMetadata meta3 = new EtaMetadata("TestA", "20", true, 5, "RootA", "AppA", 5000, "web");

        assertTrue(meta1.matches(meta2));
        assertFalse(meta1.matches(meta3));
    }

    @Test
    void recordRunCompletionSavesDatasetToVarDirectory() throws Exception {
        final Properties props = new Properties();
        props.setProperty("TEST", "Suite1");
        props.setProperty("PLATFORM", "chrome");
        props.setProperty("ALL_LEAFS", "true");
        props.setProperty("ALL_LEAFS_EXECUTIONS", "2");

        Base.parameters = new Parameters(props);

        // Record initial prediction of 120s total, actual run time 60s
        EtaCorrection.recordInitialPrediction(120_000L);
        EtaCorrection.recordRunCompletion(60_000L);

        assertTrue(correctionFile.exists(), "Correction dataset file should be created in var directory");

        final String fileContent = Files.readString(correctionFile.toPath());
        assertTrue(fileContent.contains("Suite1"));
        assertTrue(fileContent.contains("overEstimationMs"));

        EtaCorrection.reset();

        final ObjectMapper mapper = new ObjectMapper();
        final List<CorrectionRecord> records = mapper.readValue(correctionFile, new TypeReference<>() {});
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
        final List<CorrectionRecord> records = mapper.readValue(correctionFile, new TypeReference<>() {});
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
        final List<CorrectionRecord> records = mapper.readValue(correctionFile, new TypeReference<>() {});
        assertEquals(1, records.size());

        final TestError err = records.get(0).testErrors().get(0);
        assertEquals(-20_000L, err.errorMs(), "Underestimation error should be negative");
        assertEquals(-20.0, err.errorPercent());
    }

    @Test
    void getEstimationCorrectionInterpolatesOverTime() {
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

        assertEquals(20_000L, EtaCorrection.getEstimationCorrection(0L));
        assertEquals(15_000L, EtaCorrection.getEstimationCorrection(5_000L));
        assertEquals(10_000L, EtaCorrection.getEstimationCorrection(10_000L));
        assertEquals(7_500L, EtaCorrection.getEstimationCorrection(17_500L));
        assertEquals(5_000L, EtaCorrection.getEstimationCorrection(25_000L));
        assertEquals(5_000L, EtaCorrection.getEstimationCorrection(50_000L));
    }

    @Test
    void multiRunHistoryWeightedMovingAverage() throws Exception {
        final Properties props = new Properties();
        props.setProperty("TEST", "MultiRunSuite");
        Base.parameters = new Parameters(props);

        EtaCorrection.recordInitialPrediction(120_000L);
        EtaCorrection.recordRunCompletion(100_000L);

        EtaCorrection.reset();
        Base.parameters = new Parameters(props);

        EtaCorrection.recordInitialPrediction(110_000L);
        EtaCorrection.recordRunCompletion(100_000L);

        final ObjectMapper mapper = new ObjectMapper();
        final List<CorrectionRecord> records = mapper.readValue(correctionFile, new TypeReference<>() {});
        assertEquals(2, records.size(), "Both run records should be retained in history");

        EtaCorrection.reset();
        Base.parameters = new Parameters(props);

        final long weightedCorrection = EtaCorrection.getEstimationCorrection(0L);
        assertEquals(14_444L, weightedCorrection, "Correction should be weighted average of recent runs");
    }

    @Test
    void currentMetadataUsesRefreshedIntermediatePropertyCaches() throws Exception {
        final TestProperties previousProps = Base.properties;
        try {
            Base.parameters = null;

            final TestProperties tp = new TestProperties();
            final Field ccField = TestProperties.class.getDeclaredField("commonConfiguration");
            ccField.setAccessible(true);
            ccField.set(tp, new Properties());

            final Properties raw = new Properties();
            raw.setProperty(PropEnum._TEST_ROOT.getPropertyName(), "demo.root");
            raw.setProperty(PropEnum._TEST_TESTS.getPropertyName(), "42");
            raw.setProperty(PropEnum._TEST_ALL_LEAFS.getPropertyName(), "true");
            raw.setProperty(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.getPropertyName(), "3");
            raw.setProperty(PropEnum._TEST_APP.getPropertyName(), "sample.apk");
            raw.setProperty(PropEnum._TEST_PAUSE_LENGTH.getPropertyName(), "2500");
            raw.setProperty(PropEnum._WEBD_FORCED_PLATFORM.getPropertyName(), "android");

            final Field theField = TestProperties.class.getDeclaredField("the");
            theField.setAccessible(true);
            theField.set(tp, raw);
            Base.properties = tp;

            TestProperties.refreshAllPropertyRecords();

            // Clear Base.properties so direct PropEnum.get() would return defaults, proving cache is used
            Base.properties = null;

            final EtaMetadata meta = EtaCorrection.currentMetadata();
            assertEquals("demo.root", meta.test());
            assertEquals("42", meta.tests());
            assertTrue(meta.allLeafs());
            assertEquals(3, meta.allLeafsExecutions());
            assertEquals("demo.root", meta.root());
            assertEquals("sample.apk", meta.app());
            assertEquals(2500, meta.pauseLength());
            assertEquals("android", meta.platform());
        } finally {
            Base.properties = previousProps;
            TestProperties.refreshAllPropertyRecords();
        }
    }

    @Test
    void sidecarReaderParsesModernPatienceAndLegacyCompletionFormats() {
        final List<String> lines = List.of(
                "[ALL_LEAF_COMPLETION] 1 12000 0 95000 1 LeafAlpha",
                "[ALL_LEAF_COMPLETION] 2 24500 37.5 68000 0 unknown",
                "[ALL_LEAF_COMPLETION] 3 40000 50000 2 LeafBeta",
                "[ALL_LEAF_COMPLETION] 4 55000 32000 LeafLegacy"
        );

        final List<TestSample> samples = CompletionSidecarReader.parseLines(lines);
        assertEquals(4, samples.size());

        assertEquals(new TestSample(1, 12_000L, 95_000L), samples.get(0));
        assertEquals(new TestSample(2, 24_500L, 68_000L), samples.get(1));
        assertEquals(new TestSample(3, 40_000L, 50_000L), samples.get(2));
        assertEquals(new TestSample(4, 55_000L, 32_000L), samples.get(3));
    }

    @Test
    void currentMetadataPrefersRuntimeParametersTestsOverCachedProperty() {
        final Integer previousCachedTests = works.lysenko.util.prop.core.Test.tests;
        try {
            works.lysenko.util.prop.core.Test.tests = 100;
            final Properties props = new Properties();
            props.setProperty("TESTS", "7");
            Base.parameters = new Parameters(props);

            final EtaMetadata meta = EtaCorrection.currentMetadata();
            assertEquals("7", meta.tests(), "Runtime Base.parameters TESTS should override cached Test.tests");
        } finally {
            works.lysenko.util.prop.core.Test.tests = previousCachedTests;
        }
    }
}
