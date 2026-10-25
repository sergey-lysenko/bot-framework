package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.StackTrace.*;

class StackTraceTest {
    @Test
    void testDefaults() {
        assertNotNull(full);
        assertNotNull(numbers);
        assertNotNull(references);
    }
}
