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
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Exec;
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

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousParameters = Base.parameters;
        previousProperties = Base.properties;
        Base.parameters = null;
        Base.properties = null;
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
    }

    private static void setTestProperty(final String key, final String value) {
        try {
            final TestProperties testProperties = new TestProperties();
            final Field ccField = TestProperties.class.getDeclaredField("commonConfiguration");
        ccField.setAccessible(true);
        ccField.set(testProperties, new Properties());

        final Field theField = TestProperties.class.getDeclaredField("the");
            theField.setAccessible(true);
            final Properties props = new Properties();
            props.put(key, value);
            theField.set(testProperties, props);
            Base.properties = testProperties;
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
