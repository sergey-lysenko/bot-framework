package works.lysenko.util.call;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.tree.Ctrl;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.PropEnum;
import works.lysenko.util.prop.core.Mode;
import works.lysenko.util.prop.tree.Notify;
import works.lysenko.util.prop.tree.Scenario;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * Covers {@link Selector#call()}: fitting, retrying after misfits, and how a failed selection is reported
 * depending on strict, tolerant, resilient and all-leafs modes.
 */
class SelectorCallTest {

    private SelectionFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = SelectionFixture.install();
        Mode.resilient = false;
        Scenario.ignoreFailedSelection = false;
        Notify.doesNotFit = true;
        Notify.notExecutable = true;
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    // ----------------------------------------------------------------------------- successful selection

    @Test
    void fittingScenarioIsReturned() throws Exception {
        final Ctrl ctrl = new Ctrl(null);
        final Leaf fits = new FittingLeaf();
        ctrl.getPool().appendScenarioWithWeight(fits, fr(1.0));

        assertSame(fits, new Selector(ctrl, 3).call());
        assertTrue(fixture.events.isEmpty(), String.valueOf(fixture.events));
    }

    @Test
    void scenarioThatDoesNotFitIsSkippedInFavourOfOneThatDoes() throws Exception {
        final Ctrl ctrl = new Ctrl(null);
        final Leaf misfit = new MisfitLeaf();
        final Leaf fits = new FittingLeaf();
        ctrl.getPool().appendScenarioWithWeight(misfit, fr(1000.0));
        ctrl.getPool().appendScenarioWithWeight(fits, fr(1.0));

        // The misfit is almost always sampled first; it must be discarded and the selection retried.
        for (int i = 0; i < 10; i++) {
            assertSame(fits, new Selector(ctrl, 5).call());
        }
    }

    @Test
    void doesNotFitNoticeIsLoggedAtS3WhenEnabled() throws Exception {
        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(new MisfitLeaf(), fr(1.0));
        Scenario.ignoreFailedSelection = true;

        assertNull(new Selector(ctrl, 3).call());

        assertTrue(fixture.events.stream().anyMatch(e -> e.startsWith("S3:") && e.toLowerCase().contains("does not fit")),
                String.valueOf(fixture.events));
    }

    @Test
    void doesNotFitNoticeIsSuppressedWhenDisabled() throws Exception {
        Notify.doesNotFit = false;
        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(new MisfitLeaf(), fr(1.0));
        Scenario.ignoreFailedSelection = true;

        assertNull(new Selector(ctrl, 3).call());

        assertTrue(fixture.events.stream().noneMatch(e -> e.toLowerCase().contains("does not fit")),
                String.valueOf(fixture.events));
    }

    // ----------------------------------------------------------------------------- failed selection

    @Test
    void strictModeReportsS0AndStopsTests() throws Exception {
        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(new MisfitLeaf(), fr(1.0));

        assertNull(new Selector(ctrl, 3).call());

        assertTrue(fixture.events.stream().anyMatch(e -> e.startsWith("S0:") && e.toLowerCase().contains("unable to fit any scenario")),
                String.valueOf(fixture.events));
        assertTrue(fixture.core().getStopFlag(), "strict failure at the root must stop the tests");
    }

    @Test
    void ignoreFailedSelectionDowngradesToS3AndKeepsRunning() throws Exception {
        Scenario.ignoreFailedSelection = true;
        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(new MisfitLeaf(), fr(1.0));

        assertNull(new Selector(ctrl, 3).call());

        assertNoSevereFailure();
        assertTrue(fixture.events.stream().anyMatch(e -> e.startsWith("S3:") && e.toLowerCase().contains("unable to fit any scenario")),
                String.valueOf(fixture.events));
        assertTrue(fixture.events.stream().anyMatch(e -> e.toLowerCase().contains("will go on as requested")),
                String.valueOf(fixture.events));
        assertFalse(fixture.core().getStopFlag());
    }

    @Test
    void resilientModeDowngradesToS3AndKeepsRunning() throws Exception {
        Mode.resilient = true;
        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(new MisfitLeaf(), fr(1.0));

        assertNull(new Selector(ctrl, 3).call());

        assertNoSevereFailure();
        assertTrue(fixture.events.stream().anyMatch(e -> e.toLowerCase().contains("will go on as requested")),
                String.valueOf(fixture.events));
        assertFalse(fixture.core().getStopFlag());
    }

    @Test
    void allLeafsModeKeepsRunningEvenWhenNothingFits() throws Exception {
        fixture.setProperty(PropEnum._TEST_ALL_LEAFS, "true");
        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(new MisfitLeaf(), fr(1.0));

        assertNull(new Selector(ctrl, 3).call());

        assertNoSevereFailure();
        assertFalse(fixture.core().getStopFlag());
    }

    @Test
    void strictFailureBelowRootMarksParentExhaustedInsteadOfStopping() throws Exception {
        final Leaf parent = new FittingLeaf();
        final Ctrl ctrl = new Ctrl(parent);
        ctrl.getPool().appendScenarioWithWeight(new MisfitLeaf(), fr(1.0));

        assertNull(new Selector(ctrl, 3).call());

        assertTrue(parent.isExhausted());
        assertFalse(fixture.core().getStopFlag(), "a nested failure must not stop the whole session");
    }

    @Test
    void failureInTolerantModeDoesNotExhaustParent() throws Exception {
        Mode.resilient = true;
        final Leaf parent = new FittingLeaf();
        final Ctrl ctrl = new Ctrl(parent);
        ctrl.getPool().appendScenarioWithWeight(new MisfitLeaf(), fr(1.0));

        assertNull(new Selector(ctrl, 3).call());

        assertFalse(parent.isExhausted());
    }

    @Test
    void exhaustedAttemptsAreReportedAtS2() throws Exception {
        Scenario.ignoreFailedSelection = true;
        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(new MisfitLeaf(), fr(1.0));
        ctrl.getPool().appendScenarioWithWeight(new MisfitLeaf(), fr(1.0));

        assertNull(new Selector(ctrl, 1).call());

        assertTrue(fixture.events.stream().anyMatch(e -> e.startsWith("S2:") && e.toLowerCase().contains("exhausted")),
                String.valueOf(fixture.events));
    }

    // ----------------------------------------------------------------------------- execution candidates

    @Test
    void nonExecutableScenariosAreNotCandidates() {
        final Ctrl ctrl = new Ctrl(null);
        final Leaf runnable = new FittingLeaf();
        final Leaf blocked = new BlockedLeaf();
        ctrl.getPool().appendScenarioWithWeight(runnable, fr(1.0));
        ctrl.getPool().appendScenarioWithWeight(blocked, fr(1.0));

        final List<KeyValue<_Scenario, Fraction>> candidates = new Selector(ctrl, 1).getExecutionCandidates();

        assertEquals(1, candidates.size());
        assertSame(runnable, candidates.get(0).k());
    }

    @Test
    void notExecutableNoticeFollowsConfiguration() {
        final Ctrl ctrl = new Ctrl(null);
        ctrl.getPool().appendScenarioWithWeight(new BlockedLeaf(), fr(1.0));

        new Selector(ctrl, 1).getExecutionCandidates();
        assertTrue(fixture.events.stream().anyMatch(e -> e.startsWith("S3:") && e.toLowerCase().contains("is not executable")),
                String.valueOf(fixture.events));

        fixture.events.clear();
        Notify.notExecutable = false;
        new Selector(ctrl, 1).getExecutionCandidates();
        assertTrue(fixture.events.isEmpty(), String.valueOf(fixture.events));
    }

    // ----------------------------------------------------------------------------- helpers

    private void assertNoSevereFailure() {
        assertTrue(fixture.events.stream().noneMatch(e -> e.startsWith("S0:")),
                "no S0 failure expected: " + fixture.events);
    }

    private static final class FittingLeaf extends Leaf {
        FittingLeaf() {
            super(Fraction.ONE);
        }

        @Override
        public boolean fits() {
            return true;
        }
    }

    private static final class MisfitLeaf extends Leaf {
        MisfitLeaf() {
            super(Fraction.ONE);
        }

        @Override
        public boolean fits() {
            return false;
        }
    }

    private static final class BlockedLeaf extends Leaf {
        BlockedLeaf() {
            super(Fraction.ONE);
        }

        @Override
        public boolean isExecutable() {
            return false;
        }
    }
}
