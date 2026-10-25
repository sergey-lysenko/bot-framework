package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OTest {

    @Test
    void testInstantiation() {
        O instance = new O();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(O.OOS);
        assertFalse(O.OOS.isEmpty());
        assertNotNull(O.OOSR);
        assertFalse(O.OOSR.isEmpty());
        assertNotNull(O.ORIGINAL_EXPECTATIONS_PERSISTED);
        assertFalse(O.ORIGINAL_EXPECTATIONS_PERSISTED.isEmpty());
    }
}
