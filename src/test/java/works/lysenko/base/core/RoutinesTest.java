package works.lysenko.base.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Routines}.
 */
class RoutinesTest {

    @Test
    void testEnvironmentChecks() {
        assertNotNull(Routines.DOTS);
        assertFalse(Routines.DOTS.isEmpty());

        // Test CI check doesn't throw exceptions
        boolean isCI = Routines.isInsideCI();
        // Test Docker check doesn't throw exceptions
        boolean isDocker = Routines.isInsideDocker();
    }
}
