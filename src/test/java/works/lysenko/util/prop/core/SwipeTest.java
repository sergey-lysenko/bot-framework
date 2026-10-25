package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Swipe.*;

class SwipeTest {
    @Test
    void testDefaults() {
        assertNotNull(start);
        assertNotNull(stop);
        assertNotNull(line);
    }
}
