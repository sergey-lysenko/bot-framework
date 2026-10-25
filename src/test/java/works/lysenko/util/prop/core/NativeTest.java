package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Native.*;

class NativeTest {
    @Test
    void testDefaults() {
        assertTrue(x > 0);
        assertTrue(y > 0);
    }
}
