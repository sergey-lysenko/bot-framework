package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FTest {

    @Test
    void testInstantiation() {
        F instance = new F();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(F.FULLSCREEN_IMAGE);
        assertFalse(F.FULLSCREEN_IMAGE.isEmpty());
    }
}
