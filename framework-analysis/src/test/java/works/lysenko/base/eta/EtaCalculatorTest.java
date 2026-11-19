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
import works.lysenko.base.Stopwatch;
import works.lysenko.base.eta.calc.EtaFormatter;
import works.lysenko.base.eta.calc.EtaFormulas;
import works.lysenko.base.eta.model.CycleStats;
import works.lysenko.base.ui.Directory;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Repeater;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static works.lysenko.util.spec.Layout.Paths._DASH_;

class EtaCalculatorTest {

    private Core previousCore;
    private Parameters previousParameters;
    private works.lysenko.base.Test previousTest;
    private Stopwatch previousTimer;
    private final Path dashDir = Path.of(_DASH_);

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousParameters = Base.parameters;
        previousTest = Base.test;
        previousTimer = Base.timer;
        EtaCorrection.reset();
        cleanupDash();
    }

    @AfterEach
    void tearDown() throws Exception {
        Base.core = previousCore;
        Base.parameters = previousParameters;
        Base.test = previousTest;
        Base.timer = previousTimer;
        EtaCorrection.reset();
        cleanupDash();
    }

    private void cleanupDash() throws Exception {
        if (Files.exists(dashDir)) {
            try (final var stream = Files.walk(dashDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    void pureEstimationHelpersMatchExpectedFormulas() {
        assertEquals(0L, EtaCalculator.estimateAllLeafsEtaMs(500L, 5, 10, 0.0, -1L));
        assertEquals(90_000L, EtaCalculator.estimateAllLeafsEtaMs(30_000L, 25, 100, 0.0, -1L));
        // 50% patience blend between leafEtaMs (90_000) and patienceEtaMs (30_000) -> 60_000
        assertEquals(60_000L, EtaCalculator.estimateAllLeafsEtaMs(30_000L, 25, 100, 50.0, 30_000L));

        assertEquals(0L, EtaCalculator.estimateFixedTestsEtaMs(10_000L, 0, 20));
        assertEquals(30_000L, EtaCalculator.estimateFixedTestsEtaMs(10_000L, 5, 20));
        assertEquals(0L, EtaCalculator.estimateFixedTestsEtaMs(40_000L, 20, 20));

        assertEquals(45_000L, EtaCalculator.estimateLeafFallbackEtaMs(15_000L, 10, 40));
    }

    @Test
    void formatMsAndFormatPatienceHandleEdgeCases() {
        assertEquals("00:00", EtaCalculator.formatMs(0L));
        assertEquals("00:00", EtaCalculator.formatMs(-5000L));
        assertEquals("01:05", EtaCalculator.formatMs(65_400L));
        assertEquals("59:59", EtaCalculator.formatMs(3_599_000L));
        assertEquals("01:02:03", EtaCalculator.formatMs(3_723_000L));

        assertEquals("0", EtaFormatter.formatPatience(0.0));
        assertEquals("12.5", EtaFormatter.formatPatience(12.5));
        assertEquals("33.33", EtaFormatter.formatPatience(33.3333));
        assertEquals("100", EtaFormatter.formatPatience(100.0));
    }

    @Test
    void computeCycleStatsCalculatesMeanStdDevAndPredictedDuration() {
        final CycleStats stats = EtaFormulas.computeCycleStats(
                List.of(100_000L, 80_000L),
                100_000L,
                80_000L,
                5_000L
        );
        assertEquals(90_000L, stats.meanEtaMs());
        assertEquals(10_000L, stats.stdDevEtaMs());
        assertEquals(25_000L, stats.predictedMs());
    }

    @Test
    void directoryPublishesLiveAndTerminalEtaJsonToDash() throws Exception {
        final Core core = (Core) SelectionFixture.allocate(Core.class);
        final Field rf = Core.class.getDeclaredField("results");
        rf.setAccessible(true);
        rf.set(core, new Results());

        final works.lysenko.base.Test testSession = (works.lysenko.base.Test) SelectionFixture.allocate(works.lysenko.base.Test.class);
        final _Repeater repeater = new _Repeater() {
            @Override public void addToHistory(final _Scenario scenario) {}
            @Override public void addToScenarios(final _Scenario scenario) {}
            @Override public void close() {}
            @Override public Integer getCurrent() { return 6; }
            @Override public List<List<String>> getHistory() {
                final List<List<String>> h = new ArrayList<>();
                for (int i = 0; i < 5; i++) h.add(List.of("ok"));
                return h;
            }
            @Override public List<List<String>> getSummary() { return List.of(); }
            @Override public int getTestsCount() { return 5; }
            @Override public Integer getTotalTests() { return 20; }
            @Override public void run() {}
        };
        final Field repField = works.lysenko.base.Test.class.getDeclaredField("repeater");
        repField.setAccessible(true);
        repField.set(testSession, repeater);

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(core, testSession);

        final Properties props = new Properties();
        props.setProperty("TEST", "FixedSuite");
        props.setProperty("ALL_LEAFS", "false");
        Base.parameters = new Parameters(props);
        Base.core = core;
        Base.test = testSession;
        Base.timer = new Stopwatch() {
            @Override
            public long msSinceStart() {
                return 20_000L;
            }
        };

        final Directory directory = new Directory(false);
        directory.setTitle("'FixedSuite' • Test 6/20 25%");

        final Path etaPath = dashDir.resolve("eta");
        assertTrue(Files.exists(etaPath), "dash/eta should be created when Directory publishes title/info");

        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, Object> payload = mapper.readValue(Files.readString(etaPath), new TypeReference<>() {});
        assertEquals("ESTIMATED", payload.get("state"));
        assertEquals("FIXED_TESTS", payload.get("mode"));
        assertEquals(60_000, ((Number) payload.get("remainingMs")).intValue());
        assertEquals(5, ((Number) payload.get("executed")).intValue());
        assertEquals(20, ((Number) payload.get("total")).intValue());
        assertEquals("01:00", payload.get("formattedRemaining"));
        assertEquals("(ETA 01:00)", payload.get("label"));
        assertEquals("'FixedSuite' • Test 6/20 25%", payload.get("title"));

        directory.dispose();
        final Map<String, Object> donePayload = mapper.readValue(Files.readString(etaPath), new TypeReference<>() {});
        assertEquals("DONE", donePayload.get("state"));
        assertEquals(0, ((Number) donePayload.get("remainingMs")).intValue());
        assertEquals("(ETA Done)", donePayload.get("label"));
    }
}
