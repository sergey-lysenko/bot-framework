package works.lysenko.base.eta;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Stopwatch;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.util.spec.Layout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static works.lysenko.util.spec.Layout.Templates.RUN_ETA_DEBUG_;

class EtaDebugTest {

    private Core previousCore;
    private Stopwatch previousTimer;
    private Path etaDebugPath;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousTimer = Base.timer;
        EtaDebug.reset();
        AllLeafCompletions.reset();
        etaDebugPath = Path.of(Layout.Files.name(RUN_ETA_DEBUG_));
        Files.deleteIfExists(etaDebugPath);
    }

    @AfterEach
    void tearDown() throws Exception {
        Base.core = previousCore;
        Base.timer = previousTimer;
        EtaDebug.reset();
        AllLeafCompletions.reset();
        if (null != etaDebugPath) {
            Files.deleteIfExists(etaDebugPath);
        }
    }

    @Test
    void startRecordAndEndCycleWritesEtaDebugSidecarEntry() throws Exception {
        final Core core = (Core) SelectionFixture.allocate(Core.class);
        Base.core = core;

        final long[] clockMs = {10_000L};
        Base.timer = new Stopwatch() {
            @Override
            public long msSinceStart() {
                return clockMs[0];
            }
        };

        etaDebugPath = Path.of(Layout.Files.name(RUN_ETA_DEBUG_));
        Files.deleteIfExists(etaDebugPath);

        EtaDebug.startCycle("SampleLeaf");
        EtaDebug.recordSample(90_000L);
        EtaDebug.recordSample(70_000L);
        clockMs[0] = 15_000L;
        EtaDebug.endCycle("SampleLeaf");

        assertTrue(Files.exists(etaDebugPath), "ETA debug sidecar file should be created");
        final List<String> lines = Files.readAllLines(etaDebugPath);
        assertEquals(1, lines.size());
        // Projected totals recorded at t=10_000: 100_000 and 80_000 -> mean=90_000, stdDev=10_000
        assertEquals("[ETA_DEBUG] 0 5000 90000 10000 90000 SampleLeaf", lines.get(0));
    }

    @Test
    void constantProjectedTotalCountdownProducesZeroInstabilityAndUsesCyclePrediction() throws Exception {
        final works.lysenko.base.Parameters prevParams = Base.parameters;
        final works.lysenko.base.Test prevTest = Base.test;
        try {
            final Core core = (Core) SelectionFixture.allocate(Core.class);
            final java.lang.reflect.Field rf = Core.class.getDeclaredField("results");
            rf.setAccessible(true);
            rf.set(core, new works.lysenko.base.Results());

            final int[] completed = {18};
            final works.lysenko.base.Test testSession = (works.lysenko.base.Test) SelectionFixture.allocate(works.lysenko.base.Test.class);
            final works.lysenko.util.apis.test._Repeater repeater = new works.lysenko.util.apis.test._Repeater() {
                @Override public void addToHistory(final works.lysenko.util.apis.scenario._Scenario scenario) {}
                @Override public void addToScenarios(final works.lysenko.util.apis.scenario._Scenario scenario) {}
                @Override public void close() {}
                @Override public Integer getCurrent() { return completed[0] + 1; }
                @Override public List<List<String>> getHistory() {
                    final List<List<String>> h = new java.util.ArrayList<>(completed[0]);
                    for (int i = 0; i < completed[0]; i++) h.add(List.of("ok"));
                    return h;
                }
                @Override public List<List<String>> getSummary() { return List.of(); }
                @Override public int getTestsCount() { return completed[0]; }
                @Override public Integer getTotalTests() { return 20; }
                @Override public long getInFlightMs(final long nowMs) { return 10_000L; }
                @Override public void run() {}
            };
            final java.lang.reflect.Field repField = works.lysenko.base.Test.class.getDeclaredField("repeater");
            repField.setAccessible(true);
            repField.set(testSession, repeater);
            final java.lang.reflect.Field testField = Core.class.getDeclaredField("test");
            testField.setAccessible(true);
            testField.set(core, testSession);

            final java.util.Properties props = new java.util.Properties();
            props.setProperty("TEST", "StableSuite");
            props.setProperty("ALL_LEAFS", "false");
            Base.parameters = new works.lysenko.base.Parameters(props);
            Base.core = core;
            Base.test = testSession;

            // 18/20 completed at 180s -> avg 10s/test -> remaining ETA = 20s -> projected total = 200s
            final long[] clockMs = {180_000L};
            Base.timer = new Stopwatch() {
                @Override public long msSinceStart() { return clockMs[0]; }
            };

            etaDebugPath = Path.of(Layout.Files.name(RUN_ETA_DEBUG_));
            Files.deleteIfExists(etaDebugPath);

            EtaDebug.startCycle("NearEndLeaf");
            // 19/20 completed at 190s -> remaining ETA = 10s -> projected total still 200s
            completed[0] = 19;
            clockMs[0] = 190_000L;
            EtaDebug.endCycle("NearEndLeaf");

            final List<String> lines = Files.readAllLines(etaDebugPath);
            assertEquals(1, lines.size());
            // testNum=19, realMs=10_000, meanProjected=200_000, stdDev=0 (zero instability!), predictedCycle=10_000
            assertEquals("[ETA_DEBUG] 19 10000 200000 0 10000 NearEndLeaf", lines.get(0));
        } finally {
            Base.parameters = prevParams;
            Base.test = prevTest;
        }
    }
}
