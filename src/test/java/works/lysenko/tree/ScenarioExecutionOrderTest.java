package works.lysenko.tree;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Exec;
import works.lysenko.base.Results;
import works.lysenko.base.Stopwatch;
import works.lysenko.base.TestProperties;
import works.lysenko.base.exec.Scenarios;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Repeater;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.data.enums.Severity;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"removal", "deprecation"})
class ScenarioExecutionOrderTest {

    private Core previousCore;
    private Exec previousExec;
    private Stopwatch previousTimer;
    private TestProperties previousProperties;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousExec = Base.exec;
        previousTimer = Base.timer;
        previousProperties = Base.properties;

        Base.timer = new Stopwatch();
        Base.properties = new TestProperties();

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final Core core = (Core) unsafe.allocateInstance(Core.class);
        final Results results = new Results();
        final Field rf = Core.class.getDeclaredField("results");
        rf.setAccessible(true);
        rf.set(core, results);
        Base.core = core;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
        Base.exec = previousExec;
        Base.timer = previousTimer;
        Base.properties = previousProperties;
    }

    private static class DummyScenario extends Leaf {
        @Override
        public void action() {}
    }

    @Test
    void testExecutionOrderInIsOk() throws Exception {
        final List<String> eventOrder = new ArrayList<>();
        final Scenarios scenarios = new Scenarios();

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);

        final _Repeater mockRepeater = new _Repeater() {
            @Override
            public void addToHistory(_Scenario scenario) {}

            @Override
            public void addToScenarios(_Scenario scenario) {
                eventOrder.add("REPEATER_ADD_TO_SCENARIOS:depth=" + scenarios.depth());
            }

            @Override
            public void close() {}

            @Override
            public Integer getCurrent() { return 1; }

            @Override
            public List<List<String>> getHistory() { return List.of(); }

            @Override
            public List<List<String>> getSummary() { return List.of(); }

            @Override
            public int getTestsCount() { return 1; }

            @Override
            public Integer getTotalTests() { return 1; }

            @Override
            public void run() {}
        };

        final _Test mockTest = (_Test) unsafe.allocateInstance(works.lysenko.base.Test.class);
        final Field repeaterField = works.lysenko.base.Test.class.getDeclaredField("repeater");
        repeaterField.setAccessible(true);
        repeaterField.set(mockTest, mockRepeater);

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(Base.core, mockTest);

        final _Logs mockLogs = new _Logs() {
            @Override public Closeable getLogWriter() { return null; }
            @Override public int getSpanLength() { return 0; }
            @Override public Closeable getTelemetryWriter() { return null; }
            @Override public void log(String message) {}
            @Override public void log(int level, String message) {
                eventOrder.add("LOG_LEVEL_" + level + ":" + message + ":depth=" + scenarios.depth());
            }
            @Override public void log(int level, String message, Long redefinedTime) {
                eventOrder.add("LOG_LEVEL_" + level + ":" + message + ":depth=" + scenarios.depth());
            }
            @Override public void logEmptyLine() {
                eventOrder.add("LOG_EMPTY_LINE:depth=" + scenarios.depth());
            }
            @Override public void logEvent(Severity severity, String message, String shortStackTrace) {}
        };

        final Field loggerField = Core.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(Base.core, mockLogs);

        final Exec mockExec = (Exec) unsafe.allocateInstance(Exec.class);
        final Field scenariosField = Exec.class.getDeclaredField("scenarios");
        scenariosField.setAccessible(true);
        scenariosField.set(mockExec, scenarios);

        Base.exec = mockExec;

        final DummyScenario scenario = new DummyScenario();
        // Warm up / ensure properties are read
        scenario.weightConfigured();
        eventOrder.clear();

        final boolean ok = scenario.isOk();
        assertTrue(ok);

        // Verify key ordering:
        // 1. LOG_EMPTY_LINE happens first (at depth 0, before push)
        assertEquals("LOG_EMPTY_LINE:depth=0", eventOrder.get(0));

        // 2. Scenario title (level 0) happens second (at depth 1, after push)
        assertTrue(eventOrder.get(1).startsWith("LOG_LEVEL_0:") && eventOrder.get(1).endsWith(":depth=1"));

        // 3. Repeater addToScenarios happens immediately after scenario title (at depth 1)
        assertEquals("REPEATER_ADD_TO_SCENARIOS:depth=1", eventOrder.get(2));
    }
}
