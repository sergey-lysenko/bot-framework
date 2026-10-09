package works.lysenko.base.output;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Results;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultMarkerTest {

    private Core previousCore;
    private works.lysenko.base.Exec previousExec;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousExec = Base.exec;
        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final Core core = (Core) unsafe.allocateInstance(Core.class);

        final Field resultsField = Core.class.getDeclaredField("results");
        resultsField.setAccessible(true);
        resultsField.set(core, new Results());

        final Field loggerField = Core.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(core, new works.lysenko.util.apis.log._Logs() {
            @Override public java.io.Closeable getLogWriter() { return null; }
            @Override public int getSpanLength() { return 0; }
            @Override public java.io.Closeable getTelemetryWriter() { return null; }
            @Override public void log(String message) {}
            @Override public void log(int level, String message) {}
            @Override public void log(int level, String message, Long redefinedTime) {}
            @Override public void logEmptyLine() {}
            @Override public void logEvent(works.lysenko.util.data.enums.Severity severity, String message, String shortStackTrace) {}
        });

        Base.core = core;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
        Base.exec = previousExec;
    }

    private static String invokeGenerateFailureMessage() throws Exception {
        final Method m = ResultMarker.class.getDeclaredMethod("generateFailureMessage");
        m.setAccessible(true);
        return (String) m.invoke(null);
    }

    @Test
    void testPreemptiveStopWithExplicitReason() throws Exception {
        Base.core.setStopFlag(true);
        Base.core.setStopReason("Unable to select a scenario among candidates [SignIn:0]: cumulative probability is zero");

        final String message = invokeGenerateFailureMessage();
        assertEquals(" = [FAILURE] Unable to select a scenario among candidates [SignIn:0]: cumulative probability is zero = ", message);
    }

    @Test
    void testPreemptiveStopWithoutExplicitReason() throws Exception {
        Base.core.setStopFlag(true);

        final String message = invokeGenerateFailureMessage();
        assertEquals(" = [FAILURE] Tests had been stopped preemptively = ", message);
    }

    @Test
    void testExecutionFinishedWithFailedScenariosSummary() throws Exception {
        Base.core.setStopFlag(false);

        // Add a scenario marked as failed
        final Field resultsField = Core.class.getDeclaredField("results");
        resultsField.setAccessible(true);
        final Results results = (Results) resultsField.get(Base.core);

        final Field mapField = Results.class.getDeclaredField("results");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final java.util.Map<_Scenario, Result> map = (java.util.Map<_Scenario, Result>) mapField.get(results);

        final _Scenario dummyScenario = new works.lysenko.tree.base.Leaf() {
            @Override
            public boolean fits() { return true; }
            @Override
            public void action() {}
            @Override
            public boolean verify() { return true; }
            @Override
            public boolean hasFailed() { return true; }
        };
        final Result res = new Result(dummyScenario);
        res.updateStatus(ExecutionStatus.FAILED);
        map.put(dummyScenario, res);

        final String message = invokeGenerateFailureMessage();
        assertEquals(" = [FAILURE] Execution finished with 1 failed scenario = ", message);
    }

    @Test
    void testNotImplementedStopsExecution() throws Exception {
        Base.core.setStopFlag(false);
        works.lysenko.util.func.core.Assertions.notImplemented();

        assertTrue(Base.core.getStopFlag(), "notImplemented() must set stop flag");
        assertEquals("Not implemented", Base.core.getStopReason(), "notImplemented() must set stop reason");

        final String message = invokeGenerateFailureMessage();
        assertEquals(" = [FAILURE] Not implemented = ", message);
    }

    @Test
    void testNotImplementedFalseDoesNotStopExecution() {
        Base.core.setStopFlag(false);
        works.lysenko.util.func.core.Assertions.notImplemented(false);

        org.junit.jupiter.api.Assertions.assertFalse(Base.core.getStopFlag(), "notImplemented(false) must not set stop flag");
    }
    @Test
    void testRootStopTestsWithReasonSetsPlaque() throws Exception {
        Base.core.setStopFlag(false);
        final works.lysenko.tree.Ctrl rootCtrl = new works.lysenko.tree.Ctrl(null);
        rootCtrl.stopTests("Unable to select a scenario among candidates [SignIn:0]: cumulative probability is zero");

        assertTrue(Base.core.getStopFlag());
        assertEquals("Unable to select a scenario among candidates [SignIn:0]: cumulative probability is zero", Base.core.getStopReason());

        final String message = invokeGenerateFailureMessage();
        assertEquals(" = [FAILURE] Unable to select a scenario among candidates [SignIn:0]: cumulative probability is zero = ", message);
    }

    @Test
    void testPreemptiveStopWithExplicitReasonAndScenario() throws Exception {
        Base.core.setStopFlag(true);
        Base.core.setStopReason("Not implemented");
        final _Scenario dummyScenario = new works.lysenko.tree.base.Leaf() {
            @Override
            public boolean fits() { return true; }
            @Override
            public void action() {}
            @Override
            public boolean verify() { return true; }
        };
        Base.core.setStopScenario(dummyScenario);

        final String message = invokeGenerateFailureMessage();
        assertEquals(String.format(" = [FAILURE] Not implemented in '%s' = ", dummyScenario.getName()), message);
    }

    @Test
    void testPreemptiveStopWithoutExplicitReasonWithScenario() throws Exception {
        Base.core.setStopFlag(true);
        final _Scenario dummyScenario = new works.lysenko.tree.base.Leaf() {
            @Override
            public boolean fits() { return true; }
            @Override
            public void action() {}
            @Override
            public boolean verify() { return true; }
        };
        Base.core.setStopScenario(dummyScenario);

        final String message = invokeGenerateFailureMessage();
        assertEquals(String.format(" = [FAILURE] Tests had been stopped preemptively in '%s' = ", dummyScenario.getName()), message);
    }

    @Test
    void testOriginatingScenarioFromResultsWhenStopScenarioNull() throws Exception {
        Base.core.setStopFlag(true);
        Base.core.setStopReason("Not implemented");

        final Field resultsField = Core.class.getDeclaredField("results");
        resultsField.setAccessible(true);
        final Results results = (Results) resultsField.get(Base.core);

        final Field mapField = Results.class.getDeclaredField("results");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final java.util.Map<_Scenario, Result> map = (java.util.Map<_Scenario, Result>) mapField.get(results);

        final _Scenario dummyScenario = new works.lysenko.tree.base.Leaf() {
            @Override
            public boolean fits() { return true; }
            @Override
            public void action() {}
            @Override
            public boolean verify() { return true; }
            @Override
            public boolean hasFailed() { return true; }
        };
        final Result res = new Result(dummyScenario);
        res.updateStatus(ExecutionStatus.FAILED);
        map.put(dummyScenario, res);

        final String message = invokeGenerateFailureMessage();
        assertEquals(String.format(" = [FAILURE] Not implemented in '%s' = ", dummyScenario.getName()), message);
    }

    @Test
    void testNotImplementedWithRunningScenarioCapturesStopScenarioAndPlaque() throws Exception {
        Base.core.setStopFlag(false);
        final _Scenario dummyScenario = new works.lysenko.tree.base.Leaf() {
            @Override
            public boolean fits() { return true; }
            @Override
            public void action() {}
            @Override
            public boolean verify() { return true; }
        };

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final works.lysenko.base.Exec mockExec = (works.lysenko.base.Exec) unsafe.allocateInstance(works.lysenko.base.Exec.class);

        final Field scenariosField = works.lysenko.base.Exec.class.getDeclaredField("scenarios");
        scenariosField.setAccessible(true);
        final works.lysenko.base.exec.Scenarios scenarios = new works.lysenko.base.exec.Scenarios();
        final Field stackField = works.lysenko.base.exec.Scenarios.class.getDeclaredField("stack");
        stackField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final java.util.ArrayDeque<_Scenario> stack = (java.util.ArrayDeque<_Scenario>) stackField.get(scenarios);
        stack.push(dummyScenario);
        scenariosField.set(mockExec, scenarios);
        Base.exec = mockExec;

        works.lysenko.util.func.core.Assertions.notImplemented();

        assertTrue(Base.core.getStopFlag());
        assertEquals("Not implemented", Base.core.getStopReason());
        assertEquals(dummyScenario, Base.core.getStopScenario());

        final String message = invokeGenerateFailureMessage();
        assertEquals(String.format(" = [FAILURE] Not implemented in '%s' = ", dummyScenario.getName()), message);
    }
}