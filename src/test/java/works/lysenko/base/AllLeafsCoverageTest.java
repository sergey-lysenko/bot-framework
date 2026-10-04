package works.lysenko.base;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import works.lysenko.Base;
import works.lysenko.tree.Ctrl;
import works.lysenko.tree.base.Leaf;
import works.lysenko.tree.base.Mono;
import works.lysenko.tree.base.Node;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Exec;
import works.lysenko.util.apis.test._Repeater;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.data.enums.ExecutionParameter;
import works.lysenko.util.spec.PropEnum;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

@SuppressWarnings({"removal", "deprecation"})
class AllLeafsCoverageTest {

    private Core previousCore;
    private Parameters previousParameters;
    private TestProperties previousProperties;
    private Exec previousExec;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousParameters = Base.parameters;
        previousProperties = Base.properties;
        previousExec = Base.exec;
        Base.parameters = null;
        Base.properties = null;
        Base.exec = null;
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
        Base.parameters = previousParameters;
        Base.properties = previousProperties;
        Base.exec = previousExec;
    }

    private static void setTestProperty(final String key, final String value) {
        try {
            if (Base.properties == null) {
                final TestProperties testProperties = new TestProperties();
                final Field ccField = TestProperties.class.getDeclaredField("commonConfiguration");
                ccField.setAccessible(true);
                ccField.set(testProperties, new Properties());

                final Field theField = TestProperties.class.getDeclaredField("the");
                theField.setAccessible(true);
                theField.set(testProperties, new Properties());
                Base.properties = testProperties;
            }
            final Field theField = TestProperties.class.getDeclaredField("the");
            theField.setAccessible(true);
            final Properties props = (Properties) theField.get(Base.properties);
            props.put(key, value);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testTestPropertiesValidationWithAllLeafsCount() throws Exception {
        final TestProperties tp = new TestProperties();
        final Field ccField = TestProperties.class.getDeclaredField("commonConfiguration");
        ccField.setAccessible(true);
        ccField.set(tp, new Properties());
        Base.properties = tp;

        // Default parameters (ALL_LEAFS_COUNT = 1)
        final java.util.Properties props1 = new java.util.Properties();
        props1.put(ExecutionParameter.ALL_LEAFS_COUNT.name(), "1");
        Base.parameters = new Parameters(props1);

        final java.util.Properties the1 = new java.util.Properties();
        if (1 < Base.parameters.getAllLeafsCount()) {
            the1.setProperty(PropEnum._ALL_LEAFS_COUNT.getPropertyName(), String.valueOf(Base.parameters.getAllLeafsCount()));
        } else {
            the1.remove(PropEnum._ALL_LEAFS_COUNT.getPropertyName());
        }

        final Field theField = TestProperties.class.getDeclaredField("the");
        theField.setAccessible(true);
        theField.set(tp, the1);

        final Method getDefaultsMethod = TestProperties.class.getDeclaredMethod("getDefaults");
        getDefaultsMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        final java.util.Map<String, String> defaults = (java.util.Map<String, String>) getDefaultsMethod.invoke(tp);

        final works.lysenko.util.data.records.PropertiesMeta meta1 = new works.lysenko.util.data.records.PropertiesMeta(new java.util.HashMap<>(defaults), new java.util.ArrayList<>(0));
        assertDoesNotThrow(() -> works.lysenko.base.properties.Renderer.outputAndValidate(tp.getSorted(), meta1));

        // Custom count (ALL_LEAFS_COUNT = 3)
        final java.util.Properties props3 = new java.util.Properties();
        props3.put(ExecutionParameter.ALL_LEAFS_COUNT.name(), "3");
        Base.parameters = new Parameters(props3);

        final java.util.Properties the3 = new java.util.Properties();
        if (1 < Base.parameters.getAllLeafsCount()) {
            the3.setProperty(PropEnum._ALL_LEAFS_COUNT.getPropertyName(), String.valueOf(Base.parameters.getAllLeafsCount()));
        } else {
            the3.remove(PropEnum._ALL_LEAFS_COUNT.getPropertyName());
        }

        theField.set(tp, the3);
        final works.lysenko.util.data.records.PropertiesMeta meta3 = new works.lysenko.util.data.records.PropertiesMeta(new java.util.HashMap<>(defaults), new java.util.ArrayList<>(0));
        assertDoesNotThrow(() -> works.lysenko.base.properties.Renderer.outputAndValidate(tp.getSorted(), meta3));
    }

    @Test
    void testPropEnumAllLeafsCount() {
        assertEquals(".all.leafs.count", PropEnum._ALL_LEAFS_COUNT.getPropertyName());
        assertEquals(Integer.class, PropEnum._ALL_LEAFS_COUNT.type());
        assertEquals("1", PropEnum._ALL_LEAFS_COUNT.defaultValue());
        assertEquals(1, (int) PropEnum._ALL_LEAFS_COUNT.get());
    }

    @Test
    void testParametersAllLeafsCount() {
        final Parameters pDefault = new Parameters(new Properties());
        assertEquals(1, pDefault.getAllLeafsCount());
        assertFalse(pDefault.isAllLeafs());

        setTestProperty(PropEnum._ALL_LEAFS_COUNT.getPropertyName(), "3");
        final Parameters p3 = new Parameters(new Properties());
        assertEquals(3, p3.getAllLeafsCount());
        assertTrue(p3.isAllLeafs());
    }

    @Test
    void testMultiCountLeafExecutionTrackingInCore() throws Exception {
        setTestProperty(PropEnum._ALL_LEAFS_COUNT.getPropertyName(), "2");
        Base.parameters = new Parameters(new Properties());

        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        final TestLeaf leaf2 = new TestLeaf(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf2, fr(1.0));

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final _Test mockTest = (_Test) unsafe.allocateInstance(works.lysenko.base.Test.class);
        final _Exec mockExec = (_Exec) unsafe.allocateInstance(works.lysenko.base.test.Exec.class);

        final Field ctrlField = works.lysenko.base.test.Exec.class.getDeclaredField("ctrl");
        ctrlField.setAccessible(true);
        ctrlField.set(mockExec, rootCtrl);

        final Field execField = works.lysenko.base.Test.class.getDeclaredField("executor");
        execField.setAccessible(true);
        execField.set(mockTest, mockExec);

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(Base.core, mockTest);

        assertEquals(0, Base.core.getExecutedLeafsCount());
        assertFalse(Base.core.areAllLeafsExecuted());

        Base.core.getResults().count(leaf1);
        assertEquals(0, Base.core.getExecutedLeafsCount());
        assertFalse(Base.core.areAllLeafsExecuted());

        Base.core.getResults().count(leaf1);
        assertEquals(1, Base.core.getExecutedLeafsCount());
        assertFalse(Base.core.areAllLeafsExecuted());

        Base.core.getResults().count(leaf2);
        assertEquals(1, Base.core.getExecutedLeafsCount());
        assertFalse(Base.core.areAllLeafsExecuted());

        Base.core.getResults().count(leaf2);
        assertEquals(2, Base.core.getExecutedLeafsCount());
        assertTrue(Base.core.areAllLeafsExecuted());
    }

    @Test
    void testMultiCountLeafExecutionWithMonoInCore() throws Exception {
        setTestProperty(PropEnum._ALL_LEAFS_COUNT.getPropertyName(), "3");
        Base.parameters = new Parameters(new Properties());

        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        final TestMono mono = new TestMono(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(mono, fr(1.0));

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final _Test mockTest = (_Test) unsafe.allocateInstance(works.lysenko.base.Test.class);
        final _Exec mockExec = (_Exec) unsafe.allocateInstance(works.lysenko.base.test.Exec.class);

        final Field ctrlField = works.lysenko.base.test.Exec.class.getDeclaredField("ctrl");
        ctrlField.setAccessible(true);
        ctrlField.set(mockExec, rootCtrl);

        final Field execField = works.lysenko.base.Test.class.getDeclaredField("executor");
        execField.setAccessible(true);
        execField.set(mockTest, mockExec);

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(Base.core, mockTest);

        assertEquals(0, Base.core.getExecutedLeafsCount());
        assertFalse(Base.core.areAllLeafsExecuted());

        // Mono executed once - reaches its maximum (1)
        Base.core.getResults().count(mono);
        assertEquals(1, Base.core.getExecutedLeafsCount());
        assertFalse(Base.core.areAllLeafsExecuted());

        // Leaf1 executed twice - not yet at target (3)
        Base.core.getResults().count(leaf1);
        Base.core.getResults().count(leaf1);
        assertEquals(1, Base.core.getExecutedLeafsCount());
        assertFalse(Base.core.areAllLeafsExecuted());

        // Leaf1 executed third time - reaches target (3)
        Base.core.getResults().count(leaf1);
        assertEquals(2, Base.core.getExecutedLeafsCount());
        assertTrue(Base.core.areAllLeafsExecuted());
    }

    @Test
    void testParameterAndPropertyDefinitions() {
        assertEquals("ALL_LEAFS", ExecutionParameter.ALL_LEAFS.name());
        assertEquals("false", ExecutionParameter.ALL_LEAFS.def());

        assertEquals(".all.leafs", PropEnum._ALL_LEAFS.getPropertyName());
        assertEquals(Boolean.class, PropEnum._ALL_LEAFS.type());
        assertEquals("false", PropEnum._ALL_LEAFS.defaultValue());
        assertFalse((boolean) PropEnum._ALL_LEAFS.get());
    }

    @Test
    void testParametersIsAllLeafs() {
        final Properties propsFalse = new Properties();
        propsFalse.put(ExecutionParameter.ALL_LEAFS.name(), "false");
        final Parameters pFalse = new Parameters(propsFalse);
        assertFalse(pFalse.isAllLeafs());

        final Properties propsTrue = new Properties();
        propsTrue.put(ExecutionParameter.ALL_LEAFS.name(), "true");
        final Parameters pTrue = new Parameters(propsTrue);
        assertTrue(pTrue.isAllLeafs());
    }

    @Test
    void testAccessibleLeafsCalculation() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        final TestLeaf leaf2 = new TestLeaf(fr(2.0));
        final TestLeaf zeroLeaf = new TestLeaf(fr(0.0));

        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf2, fr(2.0));
        rootCtrl.getPool().appendScenarioWithWeight(zeroLeaf, fr(0.0));

        final Set<_Scenario> leafs = rootCtrl.getAccessibleLeafs();
        assertEquals(2, leafs.size());
        assertTrue(leafs.contains(leaf1));
        assertTrue(leafs.contains(leaf2));
        assertFalse(leafs.contains(zeroLeaf));

        // Theoretical minimum cycles:
        assertEquals(2, rootCtrl.getPathsCount(true));
    }

    @Test
    void testNestedNodesAccessibleLeafs() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));

        final TestLeaf leaf2 = new TestLeaf(fr(1.0));
        final TestLeaf leaf3 = new TestLeaf(fr(1.0));
        final TestNode node = new TestNode(fr(1.0), leaf2, leaf3);
        rootCtrl.getPool().appendScenarioWithWeight(node, fr(1.0));

        final Set<_Scenario> leafs = rootCtrl.getAccessibleLeafs();
        assertEquals(3, leafs.size());
        assertTrue(leafs.contains(leaf1));
        assertTrue(leafs.contains(leaf2));
        assertTrue(leafs.contains(leaf3));
        assertEquals(3, rootCtrl.getPathsCount(true));
    }

    @Test
    void testRenderTestAlignment() {
        final works.lysenko.util.data.type.LogRecord recordWithTest = new works.lysenko.util.data.type.LogRecord(1, 1000L, null);
        final works.lysenko.util.data.type.LogRecord recordWithoutTest = new works.lysenko.util.data.type.LogRecord(null, 1000L, null);

        // When totalTests is 50 (width 2)
        final String renderedWithTest50 = recordWithTest.renderTest(50);
        final String renderedWithoutTest50 = recordWithoutTest.renderTest(50);
        assertEquals("[ 1]", renderedWithTest50);
        assertEquals("[  ]", renderedWithoutTest50);
        assertEquals(renderedWithTest50.length(), renderedWithoutTest50.length());

        // When totalTests is 100 (width 3)
        final String renderedWithTest100 = recordWithTest.renderTest(100);
        final String renderedWithoutTest100 = recordWithoutTest.renderTest(100);
        assertEquals("[  1]", renderedWithTest100);
        assertEquals("[   ]", renderedWithoutTest100);
        assertEquals(renderedWithTest100.length(), renderedWithoutTest100.length());

        // When totalTests is null, fallback to 2 spaces [  ]
        final String renderedWithoutTestNull = recordWithoutTest.renderTest(null);
        assertEquals("[  ]", renderedWithoutTestNull);
    }

    @Test
    void testLeafExecutionTrackingInCore() throws Exception {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        final TestLeaf leaf2 = new TestLeaf(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf2, fr(1.0));

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final _Test mockTest = (_Test) unsafe.allocateInstance(works.lysenko.base.Test.class);
        final _Exec mockExec = (_Exec) unsafe.allocateInstance(works.lysenko.base.test.Exec.class);

        final Field ctrlField = works.lysenko.base.test.Exec.class.getDeclaredField("ctrl");
        ctrlField.setAccessible(true);
        ctrlField.set(mockExec, rootCtrl);

        final Field execField = works.lysenko.base.Test.class.getDeclaredField("executor");
        execField.setAccessible(true);
        execField.set(mockTest, mockExec);

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(Base.core, mockTest);

        assertEquals(2, Base.core.getAccessibleLeafs().size());
        assertEquals(2, Base.core.getActiveScenarioPaths());
        assertEquals(0, Base.core.getExecutedLeafsCount());
        assertFalse(Base.core.areAllLeafsExecuted());

        // Execute leaf 1
        Base.core.getResults().count(leaf1);
        assertEquals(1, Base.core.getExecutedLeafsCount());
        assertFalse(Base.core.areAllLeafsExecuted());

        // Execute leaf 1 again
        Base.core.getResults().count(leaf1);
        assertEquals(1, Base.core.getExecutedLeafsCount());
        assertFalse(Base.core.areAllLeafsExecuted());

        // Execute leaf 2
        Base.core.getResults().count(leaf2);
        assertEquals(2, Base.core.getExecutedLeafsCount());
        assertTrue(Base.core.areAllLeafsExecuted());
    }

    @Test
    void testIsNotExhaustedWithAllLeafsCount() throws Exception {
        setTestProperty(PropEnum._ALL_LEAFS_COUNT.getPropertyName(), "2");
        Base.parameters = new Parameters(new Properties());

        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final works.lysenko.base.Test testInstance = (works.lysenko.base.Test) unsafe.allocateInstance(works.lysenko.base.Test.class);
        final _Exec mockExec = (_Exec) unsafe.allocateInstance(works.lysenko.base.test.Exec.class);

        final Field ctrlField = works.lysenko.base.test.Exec.class.getDeclaredField("ctrl");
        ctrlField.setAccessible(true);
        ctrlField.set(mockExec, rootCtrl);

        final Field execField = works.lysenko.base.Test.class.getDeclaredField("executor");
        execField.setAccessible(true);
        execField.set(testInstance, mockExec);

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(Base.core, testInstance);

        final Method isNotExhaustedMethod = works.lysenko.base.Test.class.getDeclaredMethod("isNotExhausted");
        isNotExhaustedMethod.setAccessible(true);

        // Before any execution, not exhausted
        assertTrue((boolean) isNotExhaustedMethod.invoke(testInstance));
        assertEquals(0, Base.core.getExecutedLeafExecutionsCount());
        assertEquals(2, Base.core.getTotalLeafExecutionsCount());

        // After 1 execution, still not exhausted because target count is 2!
        Base.core.getResults().count(leaf1);
        assertTrue((boolean) isNotExhaustedMethod.invoke(testInstance));
        assertEquals(1, Base.core.getExecutedLeafExecutionsCount());

        // After 2 executions, exhausted!
        Base.core.getResults().count(leaf1);
        assertFalse((boolean) isNotExhaustedMethod.invoke(testInstance));
        assertEquals(2, Base.core.getExecutedLeafExecutionsCount());
    }

    @Test
    void testPropertyLoggingCycleProtection() throws Exception {
        setTestProperty(PropEnum._ALL_LEAFS_COUNT.getPropertyName(), "5");
        setTestProperty(PropEnum._DEBUG.getPropertyName(), "true");

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final Exec mockExec = (Exec) unsafe.allocateInstance(Exec.class);
        Base.exec = mockExec;

        final _Repeater mockRepeater = new _Repeater() {
            @Override
            public void addToHistory(_Scenario scenario) {}
            @Override
            public void addToScenarios(_Scenario scenario) {}
            @Override
            public void close() {}
            @Override
            public Integer getCurrent() {
                // Mimics Repeater accessing property
                return PropEnum._ALL_LEAFS_COUNT.get();
            }
            @Override
            public java.util.List<java.util.List<String>> getHistory() { return java.util.List.of(); }
            @Override
            public java.util.List<java.util.List<String>> getSummary() { return java.util.List.of(); }
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

        final _Logs mockLogger = new _Logs() {
            @Override
            public java.io.Closeable getLogWriter() { return null; }
            @Override
            public int getSpanLength() { return 0; }
            @Override
            public java.io.Closeable getTelemetryWriter() { return null; }
            @Override
            public void log(String message) { log(0, message); }
            @Override
            public void log(int level, String message) {
                // Mimics Processor.createLogRecord querying current test number
                Base.core.getCurrentTestNumber();
            }
            @Override
            public void log(int level, String message, Long redefinedTime) {
                Base.core.getCurrentTestNumber();
            }
            @Override
            public void logEmptyLine() {}
            @Override
            public void logEvent(works.lysenko.util.data.enums.Severity severity, String message, String shortStackTrace) {}
            @Override
            public void logKnownIssue(String s) {}
        };

        final Field loggerField = Core.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(Base.core, mockLogger);

        // Querying PropEnum._ALL_LEAFS_COUNT.get() when debug is active and exec is not null
        // must not cause any recursion or StackOverflowError
        assertDoesNotThrow(() -> {
            final Integer val = PropEnum._ALL_LEAFS_COUNT.get();
            assertEquals(5, val);
        });
    }

    private static class TestLeaf extends Leaf {
        TestLeaf(final Fraction weight) {
            super(weight);
        }
    }

    @Test
    void testMonoScenarioRemainsInAccessibleLeafsAfterExecution() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        final TestMono mono = new TestMono(fr(1.0));

        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(mono, fr(1.0));

        // Before execution
        assertTrue(mono.isExecutable());
        Set<_Scenario> leafs = rootCtrl.getAccessibleLeafs();
        assertEquals(2, leafs.size());
        assertTrue(leafs.contains(mono));

        // Simulate Mono execution (sets executed = true)
        try {
            final Field executedField = Mono.class.getDeclaredField("executed");
            executedField.setAccessible(true);
            executedField.set(mono, true);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Mono is no longer executable for next cycles
        assertFalse(mono.isExecutable());

        // But it remains accessible in the suite and counted for total / executed
        leafs = rootCtrl.getAccessibleLeafs();
        assertEquals(2, leafs.size(), "Mono must remain in accessible leafs even after executed");
        assertTrue(leafs.contains(mono));
    }

    private static class TestMono extends Mono {
        TestMono(final Fraction weight) {
            try {
                final Field f = works.lysenko.tree.Core.class.getDeclaredField("codeWeight");
                f.setAccessible(true);
                f.set(this, weight);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class TestNode extends Node {
        TestNode(final Fraction weight, final _Scenario... scenarios) {
            super(scenarios);
            try {
                final Field f = works.lysenko.tree.Core.class.getDeclaredField("codeWeight");
                f.setAccessible(true);
                f.set(this, weight);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
