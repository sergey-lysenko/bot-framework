package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Time.*;

class TimeTest {
    @Test
    void testDefaults() {
        assertTrue(pauseLength >= 0);
    }
}
