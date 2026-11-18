package works.lysenko.tree;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.tree.base.Leaf;
import works.lysenko.tree.base.Node;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

class PoolTest {

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
    void testAddAndAppendScenarioWithWeight() {
        final DummyNode parent = new DummyNode(fr(1.0));
        final Pool pool = new Pool(parent);

        final DummyLeaf leaf1 = new DummyLeaf(fr(1.0));
        final DummyLeaf leaf2 = new DummyLeaf(fr(2.0));

        pool.appendScenarioWithWeight(leaf1, fr(1.0));
        pool.appendScenarioWithWeight(leaf2, fr(2.0));

        assertEquals(2, pool.getPairList().size(), "Pool pair list size should be 2");
        assertEquals(leaf1, pool.getPairList().get(0).k());
        assertEquals(fr(1.0), pool.getPairList().get(0).v());
        assertEquals(parent, leaf1.getParentScenario(), "Parent should be attached to child Core scenario");
    }

    @Test
    void testAddIterableAndMap() {
        final DummyNode parent = new DummyNode(fr(1.0));
        final Pool pool = new Pool(parent);

        final DummyLeaf leaf1 = new DummyLeaf(fr(0.5));
        final DummyLeaf leaf2 = new DummyLeaf(fr(0.5));

        pool.add(List.of(leaf1, leaf2));
        assertEquals(2, pool.getPairList().size());

        final DummyLeaf leaf3 = new DummyLeaf(fr(3.0));
        pool.appendScenariosMap(Map.of(leaf3, fr(3.0)));
        assertEquals(3, pool.getPairList().size());

        final DummyLeaf leaf4 = new DummyLeaf(fr(4.0));
        pool.appendScenariosOfWeight(List.of(leaf4), fr(4.0));
        assertEquals(4, pool.getPairList().size());
    }

    @Test
    void testGetSortedSet() {
        final DummyNode parent = new DummyNode(fr(1.0));
        final Pool pool = new Pool(parent);

        final DummyLeaf leaf1 = new DummyLeaf(fr(1.0));
        final DummyLeaf leaf2 = new DummyLeaf(fr(1.0));

        pool.appendScenarioWithWeight(leaf1, fr(1.0));
        pool.appendScenarioWithWeight(leaf2, fr(1.0));

        final Set<_Scenario> sortedSet = pool.getSortedSet();
        assertEquals(2, sortedSet.size(), "Sorted set should contain both leafs");
        assertTrue(sortedSet.contains(leaf1));
        assertTrue(sortedSet.contains(leaf2));
    }
}
