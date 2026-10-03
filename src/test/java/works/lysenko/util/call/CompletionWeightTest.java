package works.lysenko.util.call;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Results;
import works.lysenko.tree.Ctrl;
import works.lysenko.tree.base.Leaf;
import works.lysenko.tree.base.Node;
import works.lysenko.tree.base.Mono;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.spec.PropEnum;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

@SuppressWarnings({"removal", "deprecation"})
class CompletionWeightTest {

    private Core previousCore;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
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
    }

    @Test
    void testPropertyDefaults() {
        assertEquals(".completion.weight", PropEnum._COMPLETION_WEIGHT.getPropertyName());
        assertNotNull(Scenario.completionWeight);
        assertEquals(fr(4.5), Scenario.completionWeight);
        assertEquals(9, Scenario.completionWeight.getNumerator());
        assertEquals(2, Scenario.completionWeight.getDenominator());
    }

    @Test
    void testResultsExecutionCounting() {
        final Results results = (Results) Base.core.getResults();
        final TestLeaf leaf = new TestLeaf(fr(1.0));

        assertEquals(0, results.getExecutions(leaf));
        results.count(leaf);
        assertEquals(1, results.getExecutions(leaf));
        results.count(leaf);
        assertEquals(2, results.getExecutions(leaf));
    }

    @Test
    void testCompletionWeightAppliedToUnexecutedCandidates() {
        final Results results = (Results) Base.core.getResults();
        final TestLeaf unexecutedLeaf = new TestLeaf(fr(1.0));
        final TestLeaf executedLeaf = new TestLeaf(fr(1.0));

        results.count(executedLeaf);

        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(unexecutedLeaf, fr(1.0));
        ctrl.getPool().appendScenarioWithWeight(executedLeaf, fr(1.0));

        final Selector selector = new Selector(ctrl, 1);
        final List<KeyValue<works.lysenko.util.apis.scenario._Scenario, Fraction>> candidates = selector.getExecutionCandidates();

        assertEquals(2, candidates.size());

        final Fraction unexecutedWeight = candidates.stream()
                .filter(kv -> kv.k() == unexecutedLeaf)
                .findFirst().orElseThrow().v();

        final Fraction executedWeight = candidates.stream()
                .filter(kv -> kv.k() == executedLeaf)
                .findFirst().orElseThrow().v();

        // 1.0 + 4.5 = 5.5 (11/2)
        assertEquals(fr(5.5), unexecutedWeight);
        assertEquals(11, unexecutedWeight.getNumerator());
        assertEquals(2, unexecutedWeight.getDenominator());

        // executed remains at base weight 1.0
        assertEquals(fr(1.0), executedWeight);
    }

    @Test
    void testCompletionWeightNotAppliedToZeroWeightScenario() {
        final TestLeaf zeroWeightLeaf = new TestLeaf(fr(0.0));

        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(zeroWeightLeaf, fr(0.0));

        final Selector selector = new Selector(ctrl, 1);
        final List<KeyValue<works.lysenko.util.apis.scenario._Scenario, Fraction>> candidates = selector.getExecutionCandidates();

        assertEquals(1, candidates.size());
        assertEquals(fr(0.0), candidates.get(0).v());
    }

    @Test
    void testMonoScenarioBecomesNonExecutableAfterRun() {
        final TestMono mono = new TestMono(fr(1.0));
        assertTrue(mono.isExecutable());

        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(mono, fr(1.0));

        final Selector selector = new Selector(ctrl, 1);
        List<KeyValue<works.lysenko.util.apis.scenario._Scenario, Fraction>> candidates = selector.getExecutionCandidates();
        assertEquals(1, candidates.size());
        assertEquals(fr(5.5), candidates.get(0).v());
    }

    private static class TestLeaf extends Leaf {
        TestLeaf(final Fraction weight) {
            super(weight);
        }
    }

    private static class TestMono extends Mono {
        TestMono(final Fraction weight) {
            super();
        }
        @Override
        public Fraction weightConfigured() {
            return fr(1.0);
        }
    }

    @Test
    void testCompletionWeightPropagatedUpTheTreeToParentNode() {
        final Results results = (Results) Base.core.getResults();
        final TestLeaf executedLeaf = new TestLeaf(fr(1.0));
        final TestLeaf unexecutedLeaf = new TestLeaf(fr(1.0));

        results.count(executedLeaf);

        final TestNode parentWithUnexecuted = new TestNode(fr(1.0), executedLeaf, unexecutedLeaf);
        results.count(parentWithUnexecuted);
        assertEquals(1, results.getExecutions(parentWithUnexecuted));

        final TestNode fullyExecutedParent = new TestNode(fr(1.0), executedLeaf);
        results.count(fullyExecutedParent);
        assertEquals(1, results.getExecutions(fullyExecutedParent));

        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(parentWithUnexecuted, fr(1.0));
        ctrl.getPool().appendScenarioWithWeight(fullyExecutedParent, fr(1.0));

        final Selector selector = new Selector(ctrl, 1);
        final List<KeyValue<works.lysenko.util.apis.scenario._Scenario, Fraction>> candidates = selector.getExecutionCandidates();

        assertEquals(2, candidates.size());

        final Fraction unexecutedParentWeight = candidates.stream()
                .filter(kv -> kv.k() == parentWithUnexecuted)
                .findFirst().orElseThrow().v();

        final Fraction fullyExecutedParentWeight = candidates.stream()
                .filter(kv -> kv.k() == fullyExecutedParent)
                .findFirst().orElseThrow().v();

        // parentWithUnexecuted has unexecuted leaf in its subtree, so gets boosted: 1.0 + 4.5 = 5.5
        assertEquals(fr(5.5), unexecutedParentWeight);

        // fullyExecutedParent has all leafs executed, so remains at base weight 1.0
        assertEquals(fr(1.0), fullyExecutedParentWeight);

        // Now execute the remaining leaf
        results.count(unexecutedLeaf);

        final Selector selectorAfter = new Selector(ctrl, 1);
        final List<KeyValue<works.lysenko.util.apis.scenario._Scenario, Fraction>> candidatesAfter = selectorAfter.getExecutionCandidates();
        final Fraction weightAfter = candidatesAfter.stream()
                .filter(kv -> kv.k() == parentWithUnexecuted)
                .findFirst().orElseThrow().v();

        // Now parentWithUnexecuted also has all leafs executed, so drops back to base weight 1.0
        assertEquals(fr(1.0), weightAfter);
    }

    @Test
    void testCompletionWeightPropagatedThroughMultipleLevels() {
        final Results results = (Results) Base.core.getResults();
        final TestLeaf deepLeaf = new TestLeaf(fr(1.0));
        final TestNode childNode = new TestNode(fr(1.0), deepLeaf);
        final TestNode grandParent = new TestNode(fr(1.0), childNode);

        // Mark grandParent and childNode as executed
        results.count(grandParent);
        results.count(childNode);

        // deepLeaf is unexecuted (0 executions)
        assertEquals(1, results.getExecutions(grandParent));
        assertEquals(1, results.getExecutions(childNode));
        assertEquals(0, results.getExecutions(deepLeaf));

        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(grandParent, fr(1.0));

        final Selector selector = new Selector(ctrl, 1);
        final List<KeyValue<works.lysenko.util.apis.scenario._Scenario, Fraction>> candidates = selector.getExecutionCandidates();

        assertEquals(1, candidates.size());
        // grandParent gets completionWeight boost 1.0 + 4.5 = 5.5
        assertEquals(fr(5.5), candidates.get(0).v());

        // Now execute deepLeaf
        results.count(deepLeaf);

        final Selector selectorAfter = new Selector(ctrl, 1);
        final List<KeyValue<works.lysenko.util.apis.scenario._Scenario, Fraction>> candidatesAfter = selectorAfter.getExecutionCandidates();
        // Drops back to base weight 1.0
        assertEquals(fr(1.0), candidatesAfter.get(0).v());
    }

    private static class TestNode extends Node {
        TestNode(final Fraction weight, final works.lysenko.util.apis.scenario._Scenario... scenarios) {
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
