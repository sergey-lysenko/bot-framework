package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Assertions.*;

class AssertionsTest {
    @Test
    void testDefaultsAndRefresh() {
        assertNotNull(exception);
        assertNotNull(severity);
        assertDoesNotThrow(Assertions::refresh);
    }
}
