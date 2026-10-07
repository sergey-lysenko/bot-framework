package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import works.lysenko.util.spec.PropEnum;

import static org.junit.jupiter.api.Assertions.*;

class ResilientTest {

    @Test
    void testResilientModeDefaultIsFalse() {
        assertNotNull(PropEnum._TEST_RESILIENT_MODE);
        assertFalse(Resilient.mode(), "Resilient mode should default to false");
    }
}
