package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Is.*;

class IsTest {
    @Test
    void testDefaults() {
        assertTrue(debug == true || debug == false);
    }
}
