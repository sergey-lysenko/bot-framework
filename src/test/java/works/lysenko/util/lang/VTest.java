package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VTest {

    @Test
    void testInstantiation() {
        V instance = new V();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(V.VALUE_IS_TO_BIG___);
        assertFalse(V.VALUE_IS_TO_BIG___.isEmpty());
        assertNotNull(V.VALUES_ARE_NOT_EQUAL);
        assertFalse(V.VALUES_ARE_NOT_EQUAL.isEmpty());
    }
}
