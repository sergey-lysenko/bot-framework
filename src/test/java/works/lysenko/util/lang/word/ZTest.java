package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZTest {

    @Test
    void testInstantiation() {
        Z instance = new Z();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(Z.ZERO);
        assertFalse(Z.ZERO.isEmpty());
    }
}
