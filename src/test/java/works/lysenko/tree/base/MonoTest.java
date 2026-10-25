package works.lysenko.tree.base;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.ScenarioType;

import static org.junit.jupiter.api.Assertions.*;

class MonoTest {

    private static class DummyMono extends Mono {}

    @Test
    void testMonoTypeAndSingleExecutionBehavior() {
        final DummyMono mono = new DummyMono();

        assertEquals(ScenarioType.MONO, mono.type(), "Scenario type should be MONO");
        assertTrue(mono.isExecutable(), "Fresh Mono scenario should be executable");
    }
}
