package works.lysenko.tree;

import org.junit.jupiter.api.Test;
import works.lysenko.tree.base.Leaf;

import static org.junit.jupiter.api.Assertions.*;

class FailedScenarioUnexecutableTest {

    private static class DummyLeaf extends Leaf {
        @Override
        public void action() {
            throw new RuntimeException("Simulated scenario failure");
        }
    }

    @Test
    void testFailedScenarioBecomesUnexecutable() {
        final DummyLeaf leaf = new DummyLeaf();
        assertFalse(leaf.hasFailed(), "New scenario should not be marked as failed");

        leaf.markAsFailed();
        assertTrue(leaf.hasFailed(), "Scenario should be marked as failed");
        assertFalse(leaf.isExecutable(), "Failed scenario must be non-executable");
    }
}
