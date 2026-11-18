package works.lysenko.base.test;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Parameters;
import works.lysenko.base.Results;
import works.lysenko.base.TestProperties;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.tree.Ctrl;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Repeater;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.prop.PropEnum;
import works.lysenko.util.prop.tree.Wandering;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static works.lysenko.util.func.type.fractions.Factory.fr;

@SuppressWarnings({"removal", "deprecation"})
class PatienceTest {

    private Core previousCore;
    private Parameters previousParameters;
    private TestProperties previousProperties;
    private works.lysenko.base.Test previousTest;
    private Fraction previousPatience;
    private Fraction previousSteepness;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousParameters = Base.parameters;
        previousProperties = Base.properties;
        previousTest = Base.test;
        previousPatience = Wandering.patience;
        previousSteepness = Wandering.steepness;

        Base.parameters = null;
        Base.properties = null;
        Wandering.patience = fr(2);
        Wandering.steepness = fr(1);

        final Core core = SelectionFixture.allocate(Core.class);
        final Field rf = Core.class.getDeclaredField("results");
        rf.setAccessible(true);
        rf.set(core, new Results());

        final _Logs silentLogger = new _Logs() {
            @Override public Closeable getLogWriter() { return null; }
            @Override public void flush() {}
            @Override public int getSpanLength() { return 0; }
            @Override public Closeable getTelemetryWriter() { return null; }
            @Override public void log(final String message) {}
            @Override public void log(final int level, final String message) {}
            @Override public void log(final int level, final String message, final Long redefinedTime) {}
            @Override public void logEmptyLine() {}
            @Override public void logEvent(final Severity severity, final String message, final String shortStackTrace) {}
        };
        final Field lf = Core.class.getDeclaredField("logger");
        lf.setAccessible(true);
        lf.set(core, silentLogger);
        Base.core = core;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
        Base.parameters = previousParameters;
        Base.properties = previousProperties;
        Base.test = previousTest;
        Wandering.patience = previousPatience;
        Wandering.steepness = previousSteepness;
    }

    @Test
    void progressTrackingAndEtaEstimateHandleInFlightAndCompletedCycles() throws Exception {
        final List<StubLeaf> leaves = installScenarioTreeWithLeaves(4);
        final StubRepeater repeater = new StubRepeater();
        final Stat stat = new Stat();
        final Patience patience = new Patience(repeater, stat);

        assertEquals(4.0, Patience.computeIdleBudget(0), 1e-6);
        assertEquals(50.0, Patience.computePatiencePercentage(2, 4.0), 1e-6);

        patience.resetProgress();
        assertEquals(0, patience.getLastProgressTestNumber());
        assertEquals(-1L, patience.estimateEtaMs(5_000L));

        // Cycle #1 executes leaf 0 -> recordCycleProgress advances lastProgressTestNumber to 1
        Base.core.getResults().count(leaves.get(0));
        repeater.setCompletedTests(1);
        stat.times().add(2_000L);
        patience.recordCycleProgress();
        assertEquals(1, patience.getLastProgressTestNumber());
        assertFalse(patience.isLost());
        assertEquals(0.0, patience.getPercentage(), 1e-6);

        // Cycle #2 is fruitless -> lastProgressTestNumber stays at 1, percentage becomes 25% (1 of 4)
        repeater.setCompletedTests(2);
        stat.times().add(2_000L);
        patience.recordCycleProgress();
        assertEquals(1, patience.getLastProgressTestNumber());
        assertFalse(patience.isLost());
        assertEquals(25.0, patience.getPercentage(), 1e-6);

        // Remaining cycles until abort (1 + 4 = 5): 5 - 2 = 3 cycles * 2_000 ms = 6_000 ms
        assertEquals(6_000L, patience.estimateEtaMs(4_000L));

        // Active in-flight cycle #3 running for 500 ms -> remaining = (2_000 - 500) + 2 * 2_000 = 5_500 ms
        repeater.setActiveCycle(3, 500L);
        assertEquals(5_500L, patience.estimateEtaMs(4_500L));
    }

    @Test
    void routinesIsAllLeafsModeReflectsPropertiesAndParameters() throws Exception {
        assertFalse(Routines.isAllLeafsMode());

        final Properties props = new Properties();
        props.setProperty(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.getPropertyName(), "2");
        final TestProperties tp = new TestProperties();
        final Field ccField = TestProperties.class.getDeclaredField("commonConfiguration");
        ccField.setAccessible(true);
        ccField.set(tp, new Properties());
        final Field theField = TestProperties.class.getDeclaredField("the");
        theField.setAccessible(true);
        theField.set(tp, props);
        Base.properties = tp;

        assertTrue(Routines.isAllLeafsMode());
    }

    private static List<StubLeaf> installScenarioTreeWithLeaves(final int leafCount) throws Exception {
        final Ctrl rootCtrl = new Ctrl(null);
        final List<StubLeaf> leaves = new ArrayList<>(leafCount);
        for (int i = 0; i < leafCount; i++) {
            final StubLeaf leaf = new StubLeaf();
            leaves.add(leaf);
            rootCtrl.getPool().appendScenarioWithWeight(leaf, fr(1));
        }

        final Exec testExec = SelectionFixture.allocate(Exec.class);
        final Field ctrlField = Exec.class.getDeclaredField("ctrl");
        ctrlField.setAccessible(true);
        ctrlField.set(testExec, rootCtrl);

        final works.lysenko.base.Test session = SelectionFixture.allocate(works.lysenko.base.Test.class);
        final Field execField = works.lysenko.base.Test.class.getDeclaredField("executor");
        execField.setAccessible(true);
        execField.set(session, testExec);

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(Base.core, session);
        Base.test = session;
        return leaves;
    }

    private static final class StubLeaf extends Leaf {
        StubLeaf() {
            super(Fraction.ONE);
        }
    }

    private static final class StubRepeater implements _Repeater {
        private int completedTests = 0;
        private Integer activeCycle = null;
        private long inFlightMs = 0L;

        void setCompletedTests(final int completedTests) {
            this.completedTests = completedTests;
        }

        void setActiveCycle(final Integer activeCycle, final long inFlightMs) {
            this.activeCycle = activeCycle;
            this.inFlightMs = inFlightMs;
        }

        @Override public void addToHistory(final _Scenario scenario) {}
        @Override public void addToScenarios(final _Scenario scenario) {}
        @Override public void close() {}
        @Override public Integer getCurrent() { return activeCycle; }

        @Override
        public List<List<String>> getHistory() {
            final List<List<String>> list = new ArrayList<>(completedTests);
            for (int i = 0; i < completedTests; i++) {
                list.add(List.of("step"));
            }
            return list;
        }

        @Override public long getInFlightMs(final long nowMs) { return inFlightMs; }
        @Override public List<List<String>> getSummary() { return List.of(); }
        @Override public int getTestsCount() { return completedTests; }
        @Override public Integer getTotalTests() { return null; }
        @Override public void run() {}
    }
}
