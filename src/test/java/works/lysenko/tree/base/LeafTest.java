package works.lysenko.tree.base;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.ScenarioType;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

class LeafTest {

    private static class DummyLeaf extends Leaf {
        DummyLeaf() {
            super();
        }

        DummyLeaf(final Fraction weight) {
            super(weight);
        }
    }

    @Test
    void testLeafTypeAndUpstreamWeight() {
        final DummyLeaf leaf = new DummyLeaf(fr(1.5));

        assertEquals(ScenarioType.LEAF, leaf.type(), "Scenario type should be LEAF");
        assertEquals(Fraction.ZERO, leaf.weightUpstream(), "Upstream weight for Leaf should always be ZERO");
        assertEquals(fr(1.5), leaf.weightCoded(), "Coded weight should match constructor argument");
    }

    @Test
    void testIsGoalFulfilledWithNullCore() {
        final DummyLeaf leaf = new DummyLeaf();
        assertFalse(leaf.isGoalFulfilled(), "isGoalFulfilled should return false when Base.core is null");
    }

    @Test
    void testListSingleScenario() {
        final DummyLeaf leaf = new DummyLeaf();
        assertEquals(1, leaf.list().size(), "list() set size should be 1");
        assertTrue(leaf.list().contains(leaf));
    }
}
