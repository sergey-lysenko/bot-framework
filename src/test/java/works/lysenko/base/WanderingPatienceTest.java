package works.lysenko.base;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.parameters.PropertyValidator;
import works.lysenko.base.test.Stat;
import works.lysenko.base.ui.UserInterface;
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

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

@SuppressWarnings({"removal", "deprecation"})
class WanderingPatienceTest {

    private Core previousCore;
    private Parameters previousParameters;
    private TestProperties previousProperties;
    private Exec previousExec;
    private works.lysenko.base.Test previousTest;
    private Stopwatch previousTimer;
    private Fraction previousPatience;
    private Fraction previousSteepness;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousParameters = Base.parameters;
        previousProperties = Base.properties;
        previousExec = Base.exec;
        previousTest = Base.test;
        previousTimer = Base.timer;
        previousPatience = Wandering.patience;
        previousSteepness = Wandering.steepness;

        Base.parameters = null;
        Base.properties = null;
        Base.exec = null;
        Base.timer = null;
        Wandering.patience = fr(2);
        Wandering.steepness = fr(1);

        final Core core = (Core) SelectionFixture.allocate(Core.class);
        final Results results = new Results();
        final Field rf = Core.class.getDeclaredField("results");
        rf.setAccessible(true);
        rf.set(core, results);

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
        Base.exec = previousExec;
        Base.test = previousTest;
        Base.timer = previousTimer;
        Wandering.patience = previousPatience;
        Wandering.steepness = previousSteepness;
    }

    private static final class StubLeaf extends Leaf {
        StubLeaf() {
            super(Fraction.ONE);
        }
    }

    private static final class FixedStopwatch extends Stopwatch {
        private long elapsedMs;

        FixedStopwatch(final long elapsedMs) {
            this.elapsedMs = elapsedMs;
        }

        void setElapsedMs(final long elapsedMs) {
            this.elapsedMs = elapsedMs;
        }

        @Override
        public long msSinceStart() {
            return elapsedMs;
        }
    }

    private static final class MutableHistoryRepeater implements _Repeater {
        private int completedTests = 0;

        void setCompletedTests(final int count) {
            this.completedTests = count;
        }

        @Override public void addToHistory(final _Scenario scenario) {}
        @Override public void addToScenarios(final _Scenario scenario) {}
        @Override public void close() {}
        @Override public Integer getCurrent() { return null; }

        @Override
        public List<List<String>> getHistory() {
            final List<List<String>> list = new ArrayList<>(completedTests);
            for (int i = 0; i < completedTests; i++) {
                list.add(List.of("step"));
            }
            return list;
        }

        @Override public List<List<String>> getSummary() { return List.of(); }
        @Override public int getTestsCount() { return completedTests; }
        @Override public Integer getTotalTests() { return null; }
        @Override public void run() {}
    }

    private static List<StubLeaf> populateLeaves(final Ctrl rootCtrl, final int leafCount) {
        final List<StubLeaf> leaves = new ArrayList<>(leafCount);
        for (int i = 0; i < leafCount; i++) {
            final StubLeaf leaf = new StubLeaf();
            leaves.add(leaf);
            rootCtrl.getPool().appendScenarioWithWeight(leaf, fr(1.0));
        }
        return leaves;
    }

    private static works.lysenko.base.Test buildTestSession(final int leafCount, final MutableHistoryRepeater repeater) throws Exception {
        final Ctrl rootCtrl = new Ctrl(null);
        populateLeaves(rootCtrl, leafCount);
        return attachSession(rootCtrl, repeater);
    }

    private static works.lysenko.base.Test attachSession(final Ctrl rootCtrl, final MutableHistoryRepeater repeater) throws Exception {
        final works.lysenko.base.Test session = (works.lysenko.base.Test) SelectionFixture.allocate(works.lysenko.base.Test.class);
        final works.lysenko.base.test.Exec testExec = (works.lysenko.base.test.Exec) SelectionFixture.allocate(works.lysenko.base.test.Exec.class);

        final Field ctrlField = works.lysenko.base.test.Exec.class.getDeclaredField("ctrl");
        ctrlField.setAccessible(true);
        ctrlField.set(testExec, rootCtrl);

        final Field execField = works.lysenko.base.Test.class.getDeclaredField("executor");
        execField.setAccessible(true);
        execField.set(session, testExec);

        final Field repeaterField = works.lysenko.base.Test.class.getDeclaredField("repeater");
        repeaterField.setAccessible(true);
        repeaterField.set(session, repeater);

        final Field statField = works.lysenko.base.Test.class.getDeclaredField("statistics");
        statField.setAccessible(true);
        statField.set(session, new Stat());

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(Base.core, session);
        Base.test = session;
        return session;
    }

    private static void bindTestProperties(final Properties props) throws Exception {
        final TestProperties tp = new TestProperties();
        final Field ccField = TestProperties.class.getDeclaredField("commonConfiguration");
        ccField.setAccessible(true);
        ccField.set(tp, new Properties());
        final Field theField = TestProperties.class.getDeclaredField("the");
        theField.setAccessible(true);
        theField.set(tp, props);
        Base.properties = tp;
    }

    @Test
    void patienceResetsToZeroOnProgressAndPreventsEarlyFakePatience() throws Exception {
        final MutableHistoryRepeater repeater = new MutableHistoryRepeater();
        final works.lysenko.base.Test session = buildTestSession(89, repeater);

        // Before Test #1 runs
        repeater.setCompletedTests(0);
        assertFalse(session.isPatienceLost(0));
        assertEquals(0.0, session.getPatiencePercentage(), 1e-6);

        // Leaf progress achieved on Test #3 -> patience resets to 0%
        repeater.setCompletedTests(3);
        assertFalse(session.isPatienceLost(3));
        assertEquals(0.0, session.getPatiencePercentage(), 1e-6);

        // 5 fruitless cycles after Test #3 (completedTests = 8, lastProgress = 3) on an 89-leaf tree
        repeater.setCompletedTests(8);
        assertFalse(session.isPatienceLost(3), "5 early fruitless cycles on an 89-leaf tree must not abort");
        assertEquals(500.0 / 89.0, session.getPatiencePercentage(), 1e-4);

        // 11 fruitless cycles after Test #9 (completedTests = 20, lastProgress = 9) on an 89-leaf tree
        repeater.setCompletedTests(20);
        assertFalse(session.isPatienceLost(9), "11 early fruitless cycles on an 89-leaf tree must not abort");
        assertEquals(1100.0 / 89.0, session.getPatiencePercentage(), 1e-4);

        // New leaf progress on Test #21 -> resets back to 0%
        repeater.setCompletedTests(21);
        assertFalse(session.isPatienceLost(21));
        assertEquals(0.0, session.getPatiencePercentage(), 1e-6);
    }

    @Test
    void patienceTerminatesAtExactBudgetWhenZeroLeafsEverFit() throws Exception {
        final MutableHistoryRepeater repeater = new MutableHistoryRepeater();
        final works.lysenko.base.Test session = buildTestSession(5, repeater);

        // With 5 leaves and patience=2, idleBudget = 5 fruitless cycles even when lastProgressTestNumber == 0
        for (int completed = 1; completed <= 4; completed++) {
            repeater.setCompletedTests(completed);
            assertFalse(session.isPatienceLost(0), "Should continue below 100% budget at cycle " + completed);
            assertEquals(completed * 20.0, session.getPatiencePercentage(), 1e-6);
        }

        // After the 5th fruitless cycle (idleCycles = 5 == idleBudget), patience reaches 100% and terminates cleanly
        repeater.setCompletedTests(5);
        assertTrue(session.isPatienceLost(0), "Suite with zero fitting leaves must terminate once 100% idle budget is reached");
        assertEquals(100.0, session.getPatiencePercentage(), 1e-6);
    }

    @Test
    void patienceScalesWithConfiguredFactorAndLateRunProgress() throws Exception {
        final MutableHistoryRepeater repeater = new MutableHistoryRepeater();
        final works.lysenko.base.Test session = buildTestSession(89, repeater);

        // Late run where lastProgressTestNumber = 114 > 89 (patience = 2 -> idleBudget = 114)
        repeater.setCompletedTests(227); // 113 idle cycles -> <100%
        assertFalse(session.isPatienceLost(114));

        repeater.setCompletedTests(228); // 114 idle cycles -> 100%
        assertTrue(session.isPatienceLost(114));
        assertEquals(100.0, session.getPatiencePercentage(), 1e-6);

        // Refreshing TestProperties updates Wandering.patience and Wandering.steepness
        final Properties props = new Properties();
        props.setProperty(PropEnum._TREE_ALL_LEAFS_PATIENCE.getPropertyName(), "4");
        props.setProperty(PropEnum._TREE_ALL_LEAFS_PATIENCE_STEEPNESS.getPropertyName(), "1/2");
        bindTestProperties(props);

        TestProperties.refreshAllPropertyRecords();
        assertEquals(fr(4), Wandering.patience);
        assertEquals(fr(1, 2), Wandering.steepness);

        // With patience = 4 on an 89-leaf tree at progress = 10, idleBudget = (4 / 2) * 89 = 178
        repeater.setCompletedTests(10 + 177);
        assertFalse(session.isPatienceLost(10));

        repeater.setCompletedTests(10 + 178);
        assertTrue(session.isPatienceLost(10));
        assertEquals(100.0, session.getPatiencePercentage(), 1e-6);
    }

    @Test
    void steepnessShapesLinearSlowToQuickAndQuickToSlowCurvesSymmetrically() throws Exception {
        final MutableHistoryRepeater repeater = new MutableHistoryRepeater();
        final works.lysenko.base.Test session = buildTestSession(10, repeater); // idleBudget = 10

        // Linear (steepness = 1)
        Wandering.steepness = fr(1);
        repeater.setCompletedTests(2);
        assertFalse(session.isPatienceLost(0));
        assertEquals(20.0, session.getPatiencePercentage(), 1e-6);
        repeater.setCompletedTests(5);
        assertFalse(session.isPatienceLost(0));
        assertEquals(50.0, session.getPatiencePercentage(), 1e-6);
        repeater.setCompletedTests(8);
        assertFalse(session.isPatienceLost(0));
        assertEquals(80.0, session.getPatiencePercentage(), 1e-6);

        // Slow-to-quick parabola (steepness = 2 -> r^2)
        Wandering.steepness = fr(2);
        repeater.setCompletedTests(2);
        assertFalse(session.isPatienceLost(0));
        assertEquals(4.0, session.getPatiencePercentage(), 1e-6);
        repeater.setCompletedTests(5);
        assertFalse(session.isPatienceLost(0));
        assertEquals(25.0, session.getPatiencePercentage(), 1e-6);
        repeater.setCompletedTests(8);
        assertFalse(session.isPatienceLost(0));
        assertEquals(64.0, session.getPatiencePercentage(), 1e-6);
        repeater.setCompletedTests(10);
        assertTrue(session.isPatienceLost(0));
        assertEquals(100.0, session.getPatiencePercentage(), 1e-6);

        // Quick-to-slow symmetric parabola (steepness = 1/2 -> 1 - (1 - r)^2)
        Wandering.steepness = fr(1, 2);
        repeater.setCompletedTests(2);
        assertFalse(session.isPatienceLost(0));
        assertEquals(36.0, session.getPatiencePercentage(), 1e-6);
        repeater.setCompletedTests(5);
        assertFalse(session.isPatienceLost(0));
        assertEquals(75.0, session.getPatiencePercentage(), 1e-6);
        repeater.setCompletedTests(8);
        assertFalse(session.isPatienceLost(0));
        assertEquals(96.0, session.getPatiencePercentage(), 1e-6);
        repeater.setCompletedTests(10);
        assertTrue(session.isPatienceLost(0));
        assertEquals(100.0, session.getPatiencePercentage(), 1e-6);
    }

    @Test
    void syncPatienceAtCycleEndEliminatesPhaseLagAndCouplesCleverEta() throws Exception {
        final Ctrl rootCtrl = new Ctrl(null);
        final List<StubLeaf> leaves = populateLeaves(rootCtrl, 10); // 10 leaves -> idleBudget = 10
        final MutableHistoryRepeater repeater = new MutableHistoryRepeater();
        final works.lysenko.base.Test session = attachSession(rootCtrl, repeater);

        final Properties props = new Properties();
        props.setProperty(PropEnum._TEST_ALL_LEAFS.getPropertyName(), "true");
        bindTestProperties(props);

        // Simulate 5 of 10 leaves executed across cycles #1..#10 (each cycle takes 10_000 ms -> elapsed = 100_000 ms)
        for (int i = 0; i < 5; i++) {
            Base.core.getResults().count(leaves.get(i));
        }
        final FixedStopwatch clock = new FixedStopwatch(100_000L);
        Base.timer = clock;

        // Cycle #9 was fruitless (after leaf progress at #8), then Cycle #10 hits a new leaf goal:
        session.syncPatienceAtCycleEnd(8, true);
        session.syncPatienceAtCycleEnd(9, false);
        assertEquals(10.0, session.getPatiencePercentage(), 1e-6);

        // Cycle #10 hits leaf #5 -> patience immediately drops to 0% at cycle #10 end (no 1-cycle lag!)
        session.syncPatienceAtCycleEnd(10, true);
        assertEquals(0.0, session.getPatiencePercentage(), 1e-6);
        // At 0% patience (w = 0): pure leafEtaMs = (10 - 5) * (100_000 / 5) = 100_000 ms
        assertEquals(100_000L, UserInterface.calculateEtaMs());

        // Now 5 fruitless wandering cycles run (#11..#15), each taking 4_000 ms -> elapsed = 120_000 ms at #15
        // Record cycle durations in statistics: 10 cycles of 10_000 ms + 5 cycles of 4_000 ms => avgCycleMs = 8_000 ms
        for (int i = 0; i < 10; i++) session.statistics().times().add(10_000L);
        for (int i = 0; i < 5; i++) session.statistics().times().add(4_000L);
        repeater.setCompletedTests(15);
        clock.setElapsedMs(120_000L);

        // Linear steepness = 1 -> at #15 (5 / 10 fruitless), w = 0.50
        // leafEtaMs = 5 * (120_000 / 5) = 120_000 ms; patienceEtaMs = (20 - 15) * 8_000 = 40_000 ms
        Wandering.steepness = fr(1);
        session.syncPatienceAtCycleEnd(15, false);
        assertEquals(50.0, session.getPatiencePercentage(), 1e-6);
        assertEquals(80_000L, UserInterface.calculateEtaMs()); // 0.50 * 120_000 + 0.50 * 40_000

        // Slow-to-quick steepness = 2 -> w = 0.25 -> leans toward leafEtaMs: 0.75 * 120_000 + 0.25 * 40_000 = 100_000
        Wandering.steepness = fr(2);
        session.syncPatienceAtCycleEnd(15, false);
        assertEquals(25.0, session.getPatiencePercentage(), 1e-6);
        assertEquals(100_000L, UserInterface.calculateEtaMs());

        // Quick-to-slow steepness = 1/2 -> w = 0.75 -> leans toward patienceEtaMs: 0.25 * 120_000 + 0.75 * 40_000 = 60_000
        Wandering.steepness = fr(1, 2);
        session.syncPatienceAtCycleEnd(15, false);
        assertEquals(75.0, session.getPatiencePercentage(), 1e-6);
        assertEquals(60_000L, UserInterface.calculateEtaMs());

        // Final fruitless cycle #20 (10 / 10 fruitless) -> w = 1.0, patienceEtaMs = 0 -> ETA = 0 ms!
        for (int i = 0; i < 5; i++) session.statistics().times().add(4_000L);
        repeater.setCompletedTests(20);
        clock.setElapsedMs(140_000L);
        session.syncPatienceAtCycleEnd(20, false);
        assertEquals(100.0, session.getPatiencePercentage(), 1e-6);
        assertEquals(0L, UserInterface.calculateEtaMs());
    }

    @Test
    void propertyValidatorAcceptsPositiveSteepnessFractionsAndRejectsNonPositive() {
        final String key = PropEnum._TREE_ALL_LEAFS_PATIENCE_STEEPNESS.getPropertyName();
        assertNull(PropertyValidator.validate(key, "1"));
        assertNull(PropertyValidator.validate(key, "2"));
        assertNull(PropertyValidator.validate(key, "1/2"));
        assertNull(PropertyValidator.validate(key, "0.5"));
        assertNotNull(PropertyValidator.validate(key, "0"));
        assertNotNull(PropertyValidator.validate(key, "-1/2"));
    }
}

