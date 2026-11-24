package works.lysenko.base.eta;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Parameters;
import works.lysenko.base.Results;
import works.lysenko.base.TestProperties;
import works.lysenko.base.eta.correction.CompletionSidecarReader;
import works.lysenko.base.eta.correction.CorrectionInterpolator;
import works.lysenko.base.eta.correction.CorrectionStore;
import works.lysenko.base.eta.model.CorrectionRecord;
import works.lysenko.base.eta.model.EtaMetadata;
import works.lysenko.base.eta.model.TestError;
import works.lysenko.base.eta.model.TestSample;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Repeater;
import works.lysenko.util.prop.PropEnum;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EtaCorrectionTest {

    private Core previousCore;
    private Parameters previousParameters;
    private final File correctionFile = new File("var/eta_correction.json");

    @BeforeEach
    void setUp() {
        previousCore = Base.core;
        previousParameters = Base.parameters;
        Base.core = null;
        EtaCorrection.reset();
        if (correctionFile.exists()) {
            correctionFile.delete();
        }
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
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
        props.setProperty("EXHAUSTIVE", "true");
        props.setProperty("EXHAUSTIVE_PASSES", "2");

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
            raw.setProperty(PropEnum._TEST_EXHAUSTIVE.getPropertyName(), "true");
            raw.setProperty(PropEnum._TEST_EXHAUSTIVE_PASSES.getPropertyName(), "3");
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
            assertTrue(meta.exhaustive());
            assertEquals(3, meta.exhaustivePasses());
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
                "[EXHAUSTIVE_COMPLETION] 1 12000 0 95000 1 LeafAlpha",
                "[EXHAUSTIVE_COMPLETION] 2 24500 37.5 68000 0 unknown",
                "[EXHAUSTIVE_COMPLETION] 3 40000 50000 2 LeafBeta",
                "[EXHAUSTIVE_COMPLETION] 4 55000 32000 LeafLegacy"
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

    @Test
    void recordRunCompletionSkipsAbortedAndIncompleteRuns() throws Exception {
        final Properties exhaustiveProps = new Properties();
        exhaustiveProps.setProperty("TEST", "GuardedE2E");
        exhaustiveProps.setProperty("EXHAUSTIVE", "true");
        Base.parameters = new Parameters(exhaustiveProps);

        final _Scenario dummyLeaf = (_Scenario) java.lang.reflect.Proxy.newProxyInstance(
                _Scenario.class.getClassLoader(),
                new Class<?>[]{_Scenario.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isExecutable" -> Boolean.TRUE;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "dummyLeaf";
                    default -> {
                        final Class<?> rt = method.getReturnType();
                        if (rt == boolean.class) yield Boolean.FALSE;
                        if (rt == int.class) yield 0;
                        if (rt == long.class) yield 0L;
                        if (rt == double.class) yield 0.0;
                        yield null;
                    }
                });

        // 1. Preemptively stopped run must not be persisted
        Base.core = createStubCore(true, true, Set.of(dummyLeaf), 74, 0);
        EtaCorrection.recordTestSample(74, 1_450_000L, 0L);
        EtaCorrection.recordRunCompletion(1_470_000L);
        assertFalse(correctionFile.exists(), "Stopped run must not be written to eta_correction.json");

        // 2. Incomplete EXHAUSTIVE run (e.g. patience abort) must not be persisted
        EtaCorrection.reset();
        Base.core = createStubCore(false, false, Set.of(dummyLeaf), 74, 0);
        EtaCorrection.recordTestSample(74, 1_450_000L, 3_500_000L);
        EtaCorrection.recordRunCompletion(1_470_000L);
        assertFalse(correctionFile.exists(), "Incomplete EXHAUSTIVE run must not be written");

        // 3. Incomplete FIXED_TESTS run must not be persisted
        EtaCorrection.reset();
        final Properties fixedProps = new Properties();
        fixedProps.setProperty("TEST", "GuardedFixed");
        fixedProps.setProperty("TESTS", "55");
        fixedProps.setProperty("EXHAUSTIVE", "false");
        Base.parameters = new Parameters(fixedProps);
        Base.core = createStubCore(false, false, Set.of(), 27, 55);
        EtaCorrection.recordTestSample(27, 678_000L, 757_000L);
        EtaCorrection.recordRunCompletion(679_000L);
        assertFalse(correctionFile.exists(), "Incomplete FIXED_TESTS run must not be written");

        // 4. Valid completed EXHAUSTIVE run IS persisted
        EtaCorrection.reset();
        Base.parameters = new Parameters(exhaustiveProps);
        Base.core = createStubCore(false, true, Set.of(dummyLeaf), 252, 0);
        EtaCorrection.recordTestSample(1, 20_000L, 4_500_000L);
        EtaCorrection.recordTestSample(252, 4_490_000L, 0L);
        EtaCorrection.recordRunCompletion(4_493_000L);
        assertTrue(correctionFile.exists(), "Completed EXHAUSTIVE run should be persisted");
    }

    @Test
    void ignoresTaintedAbortedRecordsFromDiskInBothAdditiveAndScaleCalibration() {
        final EtaMetadata e2eMeta = new EtaMetadata("E2E", "", true, 1, "root", "", 5000, "Chrome");

        // Aborted E2E run stopped at test 74 with +58m fake overestimation (matching production taint)
        final CorrectionRecord taintedAborted = new CorrectionRecord(
                e2eMeta,
                5_892_523L,
                1_470_673L,
                4_421_850L,
                List.of(
                        new TestError(1, 24_178L, 7_063_480L, 7_087_658L, 5_616_985L, 381.93),
                        new TestError(74, 1_454_909L, 3_499_668L, 4_954_577L, 3_483_904L, 236.89)),
                1000L);

        // Clean completed E2E run (actual 4,500s, accurate mid-run ETA, ends with etaMs = 0)
        final CorrectionRecord cleanComplete = new CorrectionRecord(
                e2eMeta,
                6_000_000L,
                4_500_000L,
                1_500_000L,
                List.of(
                        new TestError(1, 30_000L, 6_000_000L, 6_030_000L, 1_530_000L, 34.0),
                        new TestError(150, 2_700_000L, 1_800_000L, 4_500_000L, 0L, 0.0),
                        new TestError(252, 4_498_000L, 0L, 4_498_000L, -2_000L, -0.04)),
                2000L);

        assertFalse(CorrectionInterpolator.isValidRecord(taintedAborted), "Aborted EXHAUSTIVE record should be detected as tainted");
        assertTrue(CorrectionInterpolator.isValidRecord(cleanComplete), "Completed EXHAUSTIVE record should be valid");

        final EtaMetadata fixedMeta = new EtaMetadata("Wave", "55", false, 1, "root", "", 5000, "Chrome");
        final CorrectionRecord taintedFixed = new CorrectionRecord(
                fixedMeta, 2_000_000L, 680_000L, 1_320_000L,
                List.of(new TestError(27, 678_000L, 757_000L, 1_435_000L, 755_000L, 111.0)), 1000L);
        assertFalse(CorrectionInterpolator.isValidRecord(taintedFixed), "Aborted FIXED_TESTS record should be detected as tainted");

        final List<CorrectionRecord> mixed = List.of(taintedAborted, cleanComplete);
        // At elapsed = 2,700,000ms (60% done), tainted record would have injected ~+3,484,000ms if not ignored!
        assertEquals(0L, CorrectionInterpolator.computeWeightedCorrection(mixed, 2_700_000L));
        // Past completed run duration, additive correction decays to 0
        assertEquals(0L, CorrectionInterpolator.computeWeightedCorrection(mixed, 5_000_000L));
        // Progress-indexed remaining scale at 60% progress should be 1.0 (not distorted by tainted record)
        assertEquals(1_800_000L, CorrectionInterpolator.applyRemainingScale(2_700_000L, 1_800_000L, mixed));
    }

    @Test
    void progressIndexedCalibrationIsSpeedInvariantAndAvoidsNegativeCliff() {
        final EtaMetadata maMeta = new EtaMetadata("MA", "", true, 1, "root", "", 5000, "Chrome");

        // Systematic 4x overestimation early (like MA dynamic pruning): actualTotal = 400s
        // At t=100s, rawEta=1200s -> rawProgress = 100/1300 = 0.0769, actualRemaining = 300s -> scale = 0.25
        // At t=300s, rawEta=300s  -> rawProgress = 300/600  = 0.5000, actualRemaining = 100s -> scale = 0.3333
        final CorrectionRecord cleanMa = new CorrectionRecord(
                maMeta,
                1_600_000L,
                400_000L,
                1_200_000L,
                List.of(
                        new TestError(10, 100_000L, 1_200_000L, 1_300_000L, 900_000L, 225.0),
                        new TestError(35, 300_000L, 300_000L, 600_000L, 200_000L, 50.0),
                        new TestError(50, 399_000L, 0L, 399_000L, -1_000L, -0.25)),
                1000L);

        // Same speed as history: elapsed=100s, rawEta=1200s -> calibrated=300s (exact)
        assertEquals(300_000L, CorrectionInterpolator.applyRemainingScale(100_000L, 1_200_000L, List.of(cleanMa)));

        // 2x faster machine: elapsed=50s, rawEta=600s (additive 600-900 would have gone negative & cliffed to 600s!)
        // Progress-indexed scale predicts exact 150s!
        assertEquals(150_000L, CorrectionInterpolator.applyRemainingScale(50_000L, 600_000L, List.of(cleanMa)));

        // 2x slower machine: elapsed=200s, rawEta=2400s -> predicts exact 600s!
        assertEquals(600_000L, CorrectionInterpolator.applyRemainingScale(200_000L, 2_400_000L, List.of(cleanMa)));
    }

    @Test
    void defaultEtaProviderOnResetClearsEtaCorrectionCacheAndSamples() {
        final Properties props = new Properties();
        props.setProperty("TEST", "ResetCacheSuite");
        Base.parameters = new Parameters(props);

        // Populate cache with empty disk lookup and stage an in-memory sample
        assertTrue(EtaCorrection.getMatchingRecords().isEmpty());
        EtaCorrection.recordTestSample(1, 10_000L, 90_000L);

        // Append a valid record externally on disk
        final EtaMetadata meta = EtaCorrection.currentMetadata();
        CorrectionStore.appendRecord(new CorrectionRecord(
                meta, 200_000L, 100_000L, 100_000L,
                List.of(
                        new TestError(1, 20_000L, 160_000L, 180_000L, 80_000L, 80.0),
                        new TestError(2, 99_000L, 0L, 99_000L, -1_000L, -1.0)),
                1000L));

        // Before reset, cached list is still empty
        assertTrue(EtaCorrection.getMatchingRecords().isEmpty());

        // DefaultEtaProvider.onReset() must reset EtaCorrection cache so new record is picked up
        new DefaultEtaProvider().onReset();
        assertEquals(1, EtaCorrection.getMatchingRecords().size());
    }

    private static Core createStubCore(
            final boolean stopFlag,
            final boolean allLeavesExecuted,
            final Set<_Scenario> accessibleLeaves,
            final int completedTests,
            final int totalTests) throws Exception {
        final Core core = SelectionFixture.allocate(Core.class);
        core.setStopFlag(stopFlag);

        final Results results = new Results();
        if (allLeavesExecuted) {
            for (final _Scenario leaf : accessibleLeaves) {
                results.count(leaf);
            }
        }
        final Field rf = Core.class.getDeclaredField("results");
        rf.setAccessible(true);
        rf.set(core, results);

        final List<List<String>> history = new ArrayList<>();
        for (int i = 0; i < completedTests; i++) history.add(List.of("ok"));

        final _Repeater repeater = new _Repeater() {
            @Override public void addToHistory(final _Scenario scenario) {}
            @Override public void addToScenarios(final _Scenario scenario) {}
            @Override public void close() {}
            @Override public Integer getCurrent() { return completedTests; }
            @Override public List<List<String>> getHistory() { return history; }
            @Override public List<List<String>> getSummary() { return List.of(); }
            @Override public int getTestsCount() { return completedTests; }
            @Override public Integer getTotalTests() { return totalTests > 0 ? totalTests : null; }
            @Override public void run() {}
        };

        final works.lysenko.util.apis.scenario._Ctrl ctrl = (works.lysenko.util.apis.scenario._Ctrl) java.lang.reflect.Proxy.newProxyInstance(
                works.lysenko.util.apis.scenario._Ctrl.class.getClassLoader(),
                new Class<?>[]{works.lysenko.util.apis.scenario._Ctrl.class},
                (proxy, method, args) -> "getAccessibleLeaves".equals(method.getName()) ? accessibleLeaves : null);

        final works.lysenko.util.apis.test._Exec execProxy = (works.lysenko.util.apis.test._Exec) java.lang.reflect.Proxy.newProxyInstance(
                works.lysenko.util.apis.test._Exec.class.getClassLoader(),
                new Class<?>[]{works.lysenko.util.apis.test._Exec.class},
                (proxy, method, args) -> "ctrl".equals(method.getName()) ? ctrl : null);

        final works.lysenko.util.apis.test._Test testSession = (works.lysenko.util.apis.test._Test) java.lang.reflect.Proxy.newProxyInstance(
                works.lysenko.util.apis.test._Test.class.getClassLoader(),
                new Class<?>[]{works.lysenko.util.apis.test._Test.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "repeater" -> repeater;
                    case "executor" -> execProxy;
                    case "getPatiencePercentage" -> 0.0;
                    default -> null;
                });

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(core, testSession);
        return core;
    }
}
