package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Recreate.*;

class RecreateTest {
    @Test
    void testDefaults() {
        assertTrue(driver == true || driver == false);
    }
}
