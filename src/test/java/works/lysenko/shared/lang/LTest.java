package works.lysenko.shared.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LTest {

    @Test
    void testInstantiation() {
        final L instance = new L();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(L.LOGGING_OUT);
        assertFalse(L.LOGGING_OUT.isEmpty());
    }
}
