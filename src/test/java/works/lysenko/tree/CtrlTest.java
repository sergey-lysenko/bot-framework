package works.lysenko.tree;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.tree.base.Leaf;
import works.lysenko.tree.base.Node;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

class CtrlTest {

    private static class DummyLeaf extends Leaf {
        DummyLeaf(final Fraction weight) {
            super(weight);
        }
    }

    private static class DummyNode extends Node {
        DummyNode(final Fraction weight, final _Scenario... scenarios) {
            super(scenarios);
        }
    }

    @Test
    void testCtrlConstructorAndGetters() {
        final DummyNode parent = new DummyNode(fr(1.0));
        final Ctrl ctrl = new Ctrl(parent);

        assertEquals(parent, ctrl.getParent(), "Parent should match instance passed to constructor");
        assertNotNull(ctrl.getPool(), "Pool should be initialized");
        assertTrue(ctrl.getAttempts() >= 0, "Attempts should be a non-negative integer");
    }

    @Test
    void testCtrlWithInitialPool() {
        final DummyNode parent = new DummyNode(fr(1.0));
        final DummyLeaf leaf1 = new DummyLeaf(fr(1.0));
        final DummyLeaf leaf2 = new DummyLeaf(fr(2.0));

        final Ctrl ctrl = new Ctrl(parent, List.of(leaf1, leaf2));
        assertEquals(2, ctrl.getWeightedList().size(), "Weighted list should contain two scenarios");
        assertEquals(2, ctrl.getPathsCount(false), "Paths count should be 2");
    }

    @Test
    void testAccessibleLeafsNodesAndScenarios() {
        final DummyLeaf leaf1 = new DummyLeaf(fr(1.0));
        final DummyLeaf leaf2 = new DummyLeaf(fr(1.0));
        final DummyNode subNode = new DummyNode(fr(1.0), leaf1, leaf2);

        final DummyLeaf topLeaf = new DummyLeaf(fr(1.0));
        final DummyNode topNode = new DummyNode(fr(1.0), subNode, topLeaf);

        final Ctrl ctrl = new Ctrl(topNode, List.of(subNode, topLeaf));

        final Set<_Scenario> accessibleLeafs = ctrl.getAccessibleLeafs();
        assertEquals(3, accessibleLeafs.size(), "Should have 3 accessible leafs (leaf1, leaf2, topLeaf)");
        assertTrue(accessibleLeafs.contains(leaf1));
        assertTrue(accessibleLeafs.contains(leaf2));
        assertTrue(accessibleLeafs.contains(topLeaf));

        final Set<_Scenario> accessibleNodes = ctrl.getAccessibleNodes();
        assertEquals(1, accessibleNodes.size(), "Should have 1 accessible child node (subNode)");
        assertTrue(accessibleNodes.contains(subNode));

        final Set<_Scenario> accessibleScenarios = ctrl.getAccessibleScenarios();
        assertEquals(4, accessibleScenarios.size(), "Should have 4 total accessible scenarios");
    }
}
