package works.lysenko.base.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Scenarios}.
 */
class ScenariosTest {

    @Test
    void testScenariosStackOperations() {
        Scenarios scenarios = new Scenarios();
        assertEquals(0, scenarios.depth());
        assertNull(scenarios.current());
        assertNull(scenarios.currentString(false));

        scenarios.minDepth(3);
        assertEquals(3, scenarios.minDepth());
    }
}
