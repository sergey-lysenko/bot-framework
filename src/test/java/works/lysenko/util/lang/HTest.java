package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HTest {

    @Test
    void testInstantiation() {
        H instance = new H();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(H.HAS_NO_WIDTH);
        assertFalse(H.HAS_NO_WIDTH.isEmpty());
        assertNotNull(H.HOW_DID_WE_GET_HERE);
        assertFalse(H.HOW_DID_WE_GET_HERE.isEmpty());
    }
}
