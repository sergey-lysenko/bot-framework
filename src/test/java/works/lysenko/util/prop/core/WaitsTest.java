package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Waits.*;

class WaitsTest {
    @Test
    void testDefaults() {
        assertTrue(implicit >= 0);
        assertTrue(explicit >= 0);
    }
}
