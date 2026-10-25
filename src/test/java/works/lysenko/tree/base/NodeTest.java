package works.lysenko.tree.base;
import works.lysenko.testing.SelectionFixture;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.enums.Severity;

import java.io.Closeable;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

class NodeTest {

    private Core previousCore;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        final Core mockCore = (Core) SelectionFixture.allocate(Core.class);
        final Field loggerField = Core.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(mockCore, new _Logs() {
            @Override public Closeable getLogWriter() { return null; }
            @Override public int getSpanLength() { return 0; }
            @Override public Closeable getTelemetryWriter() { return null; }
            @Override public void log(String message) {}
            @Override public void log(int level, String message) {}
            @Override public void log(int level, String message, Long redefinedTime) {}
            @Override public void logEmptyLine() {}
            @Override public void logEvent(Severity severity, String message, String shortStackTrace) {}
        });
        Base.core = mockCore;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
    }

    private static class DummyLeaf extends Leaf {
        DummyLeaf(final Fraction weight) {
            super(weight);
        }
    }

    private static class DummyNode extends Node {
        DummyNode(final _Scenario... scenarios) {
            super(scenarios);
        }

        DummyNode(final Fraction weight, final _Scenario... scenarios) {
            super(weight);
            if (scenarios.length > 0) {
                setSubScenarios(scenarios);
            }
        }
    }

    @Test
    void testNodeTypeAndHalt() {
        final DummyNode node = new DummyNode();

        assertEquals(ScenarioType.NODE, node.type(), "Scenario type should be NODE");
        assertFalse(node.isScenarioHalted(), "Initially node should not be halted");

        node.halt();
        assertTrue(node.isScenarioHalted(), "Node should be halted after halt()");
    }

    @Test
    void testSubScenariosAndPool() {
        final DummyLeaf leaf1 = new DummyLeaf(fr(1.0));
        final DummyLeaf leaf2 = new DummyLeaf(fr(2.0));
        final DummyNode node = new DummyNode(leaf1, leaf2);

        assertNotNull(node.getPool(), "Pool should be initialized");
        assertEquals(2, node.getPool().getPairList().size(), "Pool size should reflect initial sub-scenarios");

        final DummyLeaf leaf3 = new DummyLeaf(fr(3.0));
        node.setSubScenarios(fr(3.0), leaf3);
        assertEquals(1, node.getPool().getPairList().size(), "Pool size should update after setSubScenarios");
    }

    @Test
    void testCalculateCombinations() {
        final DummyLeaf leaf1 = new DummyLeaf(fr(1.0));
        final DummyLeaf leaf2 = new DummyLeaf(fr(1.0));
        final DummyNode node = new DummyNode(leaf1, leaf2);

        assertEquals(2, node.calculateCombinations(false), "Total combinations should be sum of children");
    }
}
