package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import works.lysenko.util.prop.PropEnum;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Mode.*;

class ModeTest {

    @Test
    void testResilientModeDefaultIsFalse() {
        assertNotNull(PropEnum._TEST_RESILIENT_MODE);
        assertFalse(resilient, "Mode mode should default to false");
        assertDoesNotThrow(Mode::refresh);
    }
}
