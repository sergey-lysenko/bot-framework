package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KTest {

    @Test
    void testInstantiation() {
        K instance = new K();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(K.KEYS);
        assertFalse(K.KEYS.isEmpty());
        assertNotNull(K.KNOWN);
        assertFalse(K.KNOWN.isEmpty());
        assertNotNull(K.KRUSKAL);
        assertFalse(K.KRUSKAL.isEmpty());
    }
}
