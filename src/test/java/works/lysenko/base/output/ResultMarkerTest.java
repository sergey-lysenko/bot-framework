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

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final Core core = (Core) unsafe.allocateInstance(Core.class);

        final Field resultsField = Core.class.getDeclaredField("results");
        resultsField.setAccessible(true);
        resultsField.set(core, new Results());

        Base.core = core;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
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
}