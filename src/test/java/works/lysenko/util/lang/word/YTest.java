package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YTest {

    @Test
    void testInstantiation() {
        Y instance = new Y();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(Y.YAY);
        assertFalse(Y.YAY.isEmpty());
        assertNotNull(Y.YET);
        assertFalse(Y.YET.isEmpty());
    }
}
