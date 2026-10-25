package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Safeguard.*;

class SafeguardTest {
    @Test
    void testDefaults() {
        assertNotNull(scenarioDepth);
        assertNotNull(historyDepth);
    }
}
