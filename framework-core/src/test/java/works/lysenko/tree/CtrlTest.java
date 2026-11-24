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
    void testAccessibleLeavesNodesAndScenarios() {
        final DummyLeaf leaf1 = new DummyLeaf(fr(1.0));
        final DummyLeaf leaf2 = new DummyLeaf(fr(1.0));
        final DummyNode subNode = new DummyNode(fr(1.0), leaf1, leaf2);

        final DummyLeaf topLeaf = new DummyLeaf(fr(1.0));
        final DummyNode topNode = new DummyNode(fr(1.0), subNode, topLeaf);

        final Ctrl ctrl = new Ctrl(topNode, List.of(subNode, topLeaf));

        final Set<_Scenario> accessibleLeaves = ctrl.getAccessibleLeaves();
        assertEquals(3, accessibleLeaves.size(), "Should have 3 accessible leaves (leaf1, leaf2, topLeaf)");
        assertTrue(accessibleLeaves.contains(leaf1));
        assertTrue(accessibleLeaves.contains(leaf2));
        assertTrue(accessibleLeaves.contains(topLeaf));

        final Set<_Scenario> accessibleNodes = ctrl.getAccessibleNodes();
        assertEquals(1, accessibleNodes.size(), "Should have 1 accessible child node (subNode)");
        assertTrue(accessibleNodes.contains(subNode));

        final Set<_Scenario> accessibleScenarios = ctrl.getAccessibleScenarios();
        assertEquals(4, accessibleScenarios.size(), "Should have 4 total accessible scenarios");
    }

    @Test
    void testUpstreamWeightWithMixedActiveAndExhaustedSubtrees() {
        final DummyLeaf activeLeaf1 = new DummyLeaf(fr(2.0));
        final DummyLeaf activeLeaf2 = new DummyLeaf(fr(1.5));
        final DummyLeaf exhaustedLeaf = new DummyLeaf(fr(3.0));
        exhaustedLeaf.markAsExhausted();

        final DummyNode activeBranch = new DummyNode(fr(1.0), activeLeaf1, activeLeaf2, exhaustedLeaf);
        final DummyLeaf deadLeaf = new DummyLeaf(fr(5.0));
        deadLeaf.markAsExhausted();
        final DummyNode deadBranch = new DummyNode(fr(1.0), deadLeaf);

        final DummyNode root = new DummyNode(fr(1.0), activeBranch, deadBranch);
        // DummyNode ignores constructor weight -> pool weight is defaultWeight (0.0); only active leaves (2.0 + 1.5) contribute
        assertEquals(3.5, root.weightUpstream().doubleValue(), 1e-6);

        final Ctrl ctrl = new Ctrl(null, List.of(root));
        final Set<_Scenario> leaves = ctrl.getAccessibleLeaves();
        assertEquals(Set.of(activeLeaf1, activeLeaf2), leaves);
    }
}
