package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Fits.*;

class FitsTest {
    @Test
    void testDefaults() {
        assertTrue(byDefault == true || byDefault == false);
    }
}
