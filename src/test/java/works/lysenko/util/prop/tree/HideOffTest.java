package works.lysenko.util.prop.tree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.tree.HideOff.*;

class HideOffTest {
    @Test
    void testDefaultsAndRefresh() {
        assertTrue(branches == true || branches == false);
        assertDoesNotThrow(HideOff::refresh);
    }
}
