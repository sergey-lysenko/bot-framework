package works.lysenko.tree.base;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.ScenarioType;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionalTest {

    private static class DummyExceptional extends Exceptional {}

    @Test
    void testTypeAndInstantiation() {
        final DummyExceptional exceptional = new DummyExceptional();
        assertNotNull(exceptional);
        assertEquals(ScenarioType.CORE, exceptional.type(), "Exceptional scenario type should be CORE");
        assertTrue(exceptional.unresolved(), "Initially unresolved status should be true");
    }
}
