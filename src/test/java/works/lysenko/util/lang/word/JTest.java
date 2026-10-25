package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JTest {

    @Test
    void testInstantiation() {
        J instance = new J();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(J.JUST);
        assertFalse(J.JUST.isEmpty());
    }
}
