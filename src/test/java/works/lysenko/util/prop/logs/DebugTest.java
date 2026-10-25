package works.lysenko.util.prop.logs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.logs.Debug.*;

class DebugTest {
    @Test
    void testDefaultsAndRefresh() {
        assertNotNull(adebug);
        assertNotNull(debug);
        assertNotNull(trace);
        assertDoesNotThrow(Debug::refresh);
        assertNotNull(new Debug());
    }
}

