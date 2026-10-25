package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LTest {

    @Test
    void testInstantiation() {
        L instance = new L();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(L.LOG_WRITER_IS_NOT___);
        assertFalse(L.LOG_WRITER_IS_NOT___.isEmpty());
    }
}
