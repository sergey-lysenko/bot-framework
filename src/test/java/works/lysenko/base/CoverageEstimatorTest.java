package works.lysenko.base;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.tree.Ctrl;
import works.lysenko.tree.CoverageEstimator;
import works.lysenko.tree.base.Leaf;
import works.lysenko.tree.base.Mono;
import works.lysenko.tree.base.Node;
import works.lysenko.util.apis.scenario._Scenario;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static works.lysenko.util.func.type.fractions.Factory.fr;

class CoverageEstimatorTest {

    private Parameters previousParameters;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        previousParameters = works.lysenko.Base.parameters;
        works.lysenko.Base.parameters = null;
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        works.lysenko.Base.parameters = previousParameters;
    }

    @Test
    void testSingleLeaf() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));

        assertEquals(1, CoverageEstimator.estimateAverageCycles(rootCtrl));
    }

    @Test
    void testTwoLeafsWithCompletionWeight() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        final TestLeaf leaf2 = new TestLeaf(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf2, fr(1.0));

        // Theoretical average is ~2.18 cycles. Rounding gives 2.
        final int estimated = CoverageEstimator.estimateAverageCycles(rootCtrl);
        assertEquals(2, estimated);
    }

    @Test
    void testProgressConsumer() {
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
        for (int i = 1; i < progressUpdates.size(); i++) {
            assertTrue(progressUpdates.get(i) >= progressUpdates.get(i - 1));
        }
    }

    @Test
    void testMonoLeafWithTargetGreaterThanOneDoesNotHang() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf(fr(1.0));
        final TestMono mono = new TestMono(fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(mono, fr(1.0));

        final List<Integer> progressUpdates = new ArrayList<>();
        final int estimated = CoverageEstimator.estimateAverageCycles(rootCtrl, 3, progressUpdates::add);
        // Mono requires 1 cycle, leaf1 requires 3 cycles. Total should be ~3-4 cycles.
        assertTrue(estimated >= 3 && estimated <= 8, "Expected 3-8 cycles, got: " + estimated);
        assertEquals(100, progressUpdates.get(progressUpdates.size() - 1));
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
        // 4 leafs in a 2-level tree
        assertTrue(estimated >= 4 && estimated <= 10, "Estimated cycles should be reasonable: " + estimated);
    }

    @Test
    void testBiteHeistTreeStructure() {
        final Ctrl rootCtrl = new Ctrl(null);

        // Reports subtree (12 leafs)
        final TestNode snapshot = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));
        final TestNode sms = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));
        final TestNode fd = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));
        final TestNode orders = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));
        final TestNode repBilling = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));
        final TestNode reports = new TestNode(fr(1.0), snapshot, sms, fd, orders, repBilling);

        // Settings subtree (5 leafs)
        final TestNode confProd = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));
        final TestNode posConfig = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));
        final TestNode settings = new TestNode(fr(1.0), confProd, posConfig);

        // Billing subtree (7 leafs)
        final TestNode customers = new TestNode(fr(1.0), new TestLeaf(fr(1.0)));
        final TestNode resellers = new TestNode(fr(1.0), new TestLeaf(fr(1.0)));
        final TestNode invoices = new TestNode(fr(1.0), new TestLeaf(fr(1.0)));
        final TestNode modules = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));
        final TestNode billingRules = new TestNode(fr(1.0), new TestLeaf(fr(1.0)));
        final TestLeaf billDash = new TestLeaf(fr(1.0));
        final TestNode billing = new TestNode(fr(1.0), customers, resellers, invoices, modules, billingRules, billDash);

        // AuditLogs subtree (4 leafs)
        final TestNode auditLogs = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));

        // AiUsageReport subtree (1 leaf)
        final TestNode aiUsage = new TestNode(fr(1.0), new TestLeaf(fr(1.0)));

        // AppManagement subtree (1 leaf)
        final TestNode appMgmt = new TestNode(fr(1.0), new TestLeaf(fr(1.0)));

        // Partners subtree (1 leaf)
        final TestNode partners = new TestNode(fr(1.0), new TestLeaf(fr(1.0)));

        // Payments subtree (1 leaf)
        final TestNode payments = new TestNode(fr(1.0), new TestLeaf(fr(1.0)));

        // Direct leafs under CorrectLogin (3 leafs)
        final TestLeaf corrDash = new TestLeaf(fr(1.0));
        final TestLeaf firstDel = new TestLeaf(fr(1.0));
        final TestLeaf logout = new TestLeaf(fr(1.0));

        // CorrectLogin node (35 leafs total)
        final TestNode correctLogin = new TestNode(fr(1.0),
                reports, settings, billing, auditLogs, aiUsage, appMgmt, partners, payments,
                corrDash, firstDel, logout);

        // WrongLogin (1 leaf), WrongPassword (1 leaf)
        final TestLeaf wrongLogin = new TestLeaf(fr(1.0));
        final TestLeaf wrongPassword = new TestLeaf(fr(1.0));

        // SignIn root (37 leafs)
        final TestNode signIn = new TestNode(fr(1.0), correctLogin, wrongLogin, wrongPassword);

        rootCtrl.getPool().appendScenarioWithWeight(signIn, fr(1.0));

        assertEquals(37, rootCtrl.getAccessibleLeafs().size());
        final int estimated = CoverageEstimator.estimateAverageCycles(rootCtrl);
        System.out.println("BITEHEIST 37-LEAF TREE ESTIMATED CYCLES: " + estimated);
        assertTrue(estimated >= 50 && estimated <= 75,
                "Estimated should reflect balancing by unfinished descendant leaf weight, got: " + estimated);
    }

    @Test
    void testHighTargetPerformance() {
        final Ctrl rootCtrl = new Ctrl(null);
        final TestNode snapshot = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));
        final TestNode sms = new TestNode(fr(1.0), new TestLeaf(fr(1.0)), new TestLeaf(fr(1.0)));
        final TestNode reports = new TestNode(fr(1.0), snapshot, sms);
        rootCtrl.getPool().appendScenarioWithWeight(reports, fr(1.0));

        final long start = System.currentTimeMillis();
        final int estimated = CoverageEstimator.estimateAverageCycles(rootCtrl, 10);
        final long elapsed = System.currentTimeMillis() - start;
        assertTrue(estimated >= 40 && estimated <= 80, "Estimated should be reasonable, got: " + estimated);
        assertTrue(elapsed < 200, "100 trials with target 10 should complete within 200ms, took: " + elapsed + " ms");
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
