package works.lysenko.tree;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

class CoreTest {

    private static class DummyCore extends Core {
        DummyCore() {
            super();
        }

        DummyCore(final Fraction weight) {
            super(weight);
        }

        @Override
        public Fraction weightUpstream() {
            return Fraction.ZERO;
        }
    }

    @Test
    void testConstructorsAndWeights() {
        final DummyCore defaultCore = new DummyCore();
        assertNull(defaultCore.weightCoded(), "Code weight should default to null");

        final Fraction customWeight = fr(2.5);
        final DummyCore weightedCore = new DummyCore(customWeight);
        assertEquals(customWeight, weightedCore.weightCoded(), "Code weight should match constructor value");
        assertEquals(customWeight, weightedCore.weightConfigured(), "Configured weight should return code weight when present");
    }

    @Test
    void testParentAndNames() {
        final DummyCore child = new DummyCore();
        final DummyCore parent = new DummyCore();

        assertNull(child.getParentScenario(), "Parent should be null initially");
        child.setParentScenario(parent);
        assertEquals(parent, child.getParentScenario(), "Parent scenario should match set instance");

        assertEquals(DummyCore.class.getName(), child.getName(), "getName() should return FQN");
        assertEquals(DummyCore.class.getSimpleName(), child.getSimpleName(), "getSimpleName() should return class simple name");
        assertEquals("tree", child.getParentScenarioName(child), "Parent scenario name should extract package segment");
    }

    @Test
    void testStaleCount() {
        final DummyCore core = new DummyCore();
        assertEquals(0, core.getStaleCount(), "Initial stale count should be 0");

        core.incrementStaleCount();
        assertEquals(1, core.getStaleCount(), "Stale count should be 1 after increment");

        core.incrementStaleCount();
        assertEquals(2, core.getStaleCount(), "Stale count should be 2 after second increment");

        core.resetStaleCount();
        assertEquals(0, core.getStaleCount(), "Stale count should be 0 after reset");
    }

    @Test
    void testExhaustedState() {
        final DummyCore core = new DummyCore();
        assertFalse(core.isExhausted(), "Initially should not be exhausted");

        core.markAsExhausted();
        assertTrue(core.isExhausted(), "Should be exhausted after marking");
        assertFalse(core.isExecutable(), "Exhausted scenario should not be executable");
    }

    @Test
    void testFailureStatesAndExecutionStatus() {
        final DummyCore core = new DummyCore();
        assertFalse(core.hasFailed(), "Initially failed should be false");
        assertFalse(core.hasChildFailed(), "Initially childFailed should be false");
        assertEquals(ExecutionStatus.UNVISITED, core.getExecutionStatus(), "Initial status should be UNVISITED");

        core.markAsFailed();
        assertTrue(core.hasFailed(), "Should return true for hasFailed()");
        assertEquals(ExecutionStatus.FAILED, core.getExecutionStatus(), "Status should be FAILED");
        assertFalse(core.isExecutable(), "Failed scenario should not be executable");

        final DummyCore childFailCore = new DummyCore();
        childFailCore.markAsChildFailed();
        assertTrue(childFailCore.hasChildFailed(), "Should return true for hasChildFailed()");
        assertEquals(ExecutionStatus.CHILD_FAILED, childFailCore.getExecutionStatus(), "Status should be CHILD_FAILED");
    }

    @Test
    void testListAndFits() {
        final DummyCore core = new DummyCore();
        assertEquals(works.lysenko.util.prop.core.Fits.byDefault, core.fits(), "fits() should return default fit boolean");
        assertEquals(1, core.list().size(), "list() should contain exactly this scenario");
        assertTrue(core.list().contains(core), "list() set should contain core instance");
    }
}
