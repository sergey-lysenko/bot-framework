package works.lysenko.tree;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.base.Parameters;
import works.lysenko.tree.base.Leaf;
import works.lysenko.tree.base.Mono;
import works.lysenko.tree.base.Node;
import works.lysenko.util.apis.scenario._Scenario;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

class CoverageEstimatorTest {

    private Parameters previousParameters;

    @BeforeEach
    void setUp() {
        previousParameters = works.lysenko.Base.parameters;
        works.lysenko.Base.parameters = null;
    }

    @AfterEach
    void tearDown() {
        works.lysenko.Base.parameters = previousParameters;
    }

    @Test
    void testNullOrEmptyRootCtrl() {
        assertEquals(0, CoverageEstimator.estimateAverageCycles(null), "Null rootCtrl should return 0");

        final Ctrl emptyCtrl = new Ctrl(null);
        assertEquals(0, CoverageEstimator.estimateAverageCycles(emptyCtrl), "RootCtrl with no accessible leafs should return 0");
    }

    @Test
    void testSingleLeaf() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));

        assertEquals(1, CoverageEstimator.estimateAverageCycles(rootCtrl));
        assertEquals(3, CoverageEstimator.estimateAverageCycles(rootCtrl, 3));
    }

    @Test
    void testTwoLeafsWithProgressConsumer() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        final TestLeaf leaf2 = new TestLeaf(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf2, fr(1.0));

        final List<Integer> progressUpdates = new ArrayList<>();
        final int estimated = CoverageEstimator.estimateAverageCycles(rootCtrl, 1, progressUpdates::add);
        assertEquals(2, estimated);
        assertFalse(progressUpdates.isEmpty());
        assertEquals(0, progressUpdates.get(0));
        assertEquals(100, progressUpdates.get(progressUpdates.size() - 1));
    }

    @Test
    void testMonoLeaf() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestMono mono = new TestMono(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(mono, fr(1.0));

        assertEquals(1, CoverageEstimator.estimateAverageCycles(rootCtrl, 5));
    }

    @Test
    void testMultiLevelTree() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        final TestLeaf leaf2 = new TestLeaf(fr(1.0));
        final TestNode node1 = new TestNode(fr(1.0), leaf1, leaf2);

        final TestLeaf leaf3 = new TestLeaf(fr(1.0));
        final TestLeaf leaf4 = new TestLeaf(fr(1.0));
        final TestNode node2 = new TestNode(fr(1.0), leaf3, leaf4);

        rootCtrl.getPool().appendScenarioWithWeight(node1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(node2, fr(1.0));

        final int estimated = CoverageEstimator.estimateAverageCycles(rootCtrl);
        assertTrue(estimated >= 4 && estimated <= 10, "Estimated cycles should be reasonable: " + estimated);
    }

    private static class TestLeaf extends Leaf {
        TestLeaf(final Fraction weight) {
            super(weight);
        }
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
