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
}
