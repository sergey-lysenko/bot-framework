package works.lysenko.util.prop.logs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.logs.Time.*;

class TimeTest {
    @Test
    void testDefaults() {
        assertNotNull(stamp);
    }
}
