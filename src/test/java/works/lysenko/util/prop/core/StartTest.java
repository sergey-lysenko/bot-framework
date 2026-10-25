package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Start.*;

class StartTest {
    @Test
    void testDefaults() {
        assertTrue(bundleId == null || bundleId instanceof String);
        assertTrue(dashboardScreen >= 0);
    }
}
