package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CTest {

    @Test
    void testInstantiation() {
        final C instance = new C();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(C.CANCEL);
        assertFalse(C.CANCEL.isEmpty());
        assertNotNull(C.CLOSE);
        assertFalse(C.CLOSE.isEmpty());
        assertNotNull(C.CLEAR);
        assertFalse(C.CLEAR.isEmpty());
        assertNotNull(C.CONFIG);
        assertFalse(C.CONFIG.isEmpty());
        assertNotNull(C.CONFIGURE);
        assertFalse(C.CONFIGURE.isEmpty());
        assertNotNull(C.CUSTOMERS);
        assertFalse(C.CUSTOMERS.isEmpty());
        assertNotNull(C.COMPLETE);
        assertFalse(C.COMPLETE.isEmpty());
    }
}
