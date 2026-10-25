package works.lysenko.tree.base.exceptional;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.ScenarioType;

import static org.junit.jupiter.api.Assertions.*;

class CoreTest {

    private static class DummyExceptionalCore extends Core {}

    @Test
    void testCoreTypeAndResolutionState() {
        final DummyExceptionalCore core = new DummyExceptionalCore();

        assertEquals(ScenarioType.CORE, core.type(), "Scenario type should be CORE");
        assertNull(core.exception(), "Initial exception should be null");
        assertNull(core.triplet(), "Initial triplet should be null");
        assertTrue(core.unresolved(), "Initially unresolved should be true");

        core.markAsResolved();
        assertFalse(core.unresolved(), "unresolved should be false after markAsResolved()");
    }
}
