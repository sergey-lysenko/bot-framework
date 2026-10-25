package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MTest {

    @Test
    void testInstantiation() {
        final M instance = new M();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(M.MANAGEMENT);
        assertFalse(M.MANAGEMENT.isEmpty());
        assertNotNull(M.MANY);
        assertFalse(M.MANY.isEmpty());
        assertNotNull(M.MODULE);
        assertFalse(M.MODULE.isEmpty());
        assertNotNull(M.MODULES);
        assertFalse(M.MODULES.isEmpty());
    }
}
