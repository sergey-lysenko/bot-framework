package works.lysenko.util.prop.logs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.logs.LongOperation.*;

class LongOperationTest {
    @Test
    void testDefaults() {
        assertTrue(threshold >= 0);
        assertNotNull(severity);
        assertNotNull(new LongOperation());
    }
}

