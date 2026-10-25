package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Sleeping.*;

class SleepingTest {
    @Test
    void testDefaults() {
        assertTrue(threshold >= 0);
        assertTrue(maxTypingDelay >= 0);
    }
}
