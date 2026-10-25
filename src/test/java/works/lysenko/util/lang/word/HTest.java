package works.lysenko.util.lang.word;

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
        assertNotNull(H.HALTED);
        assertFalse(H.HALTED.isEmpty());
        assertNotNull(H.HAPPEN);
        assertFalse(H.HAPPEN.isEmpty());
        assertNotNull(H.HEIGHT);
        assertFalse(H.HEIGHT.isEmpty());
        assertNotNull(H.HELPER);
        assertFalse(H.HELPER.isEmpty());
        assertNotNull(H.HISTORY);
        assertFalse(H.HISTORY.isEmpty());
        assertNotNull(H.HORIZONTAL);
        assertFalse(H.HORIZONTAL.isEmpty());
        assertNotNull(H.HUE);
        assertFalse(H.HUE.isEmpty());
    }
}
