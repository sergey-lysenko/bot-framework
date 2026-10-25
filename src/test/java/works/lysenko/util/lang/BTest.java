package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BTest {

    @Test
    void testInstantiation() {
        B instance = new B();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(B.BY_ACTUAL_VALUE);
        assertFalse(B.BY_ACTUAL_VALUE.isEmpty());
        assertNotNull(B.BUT___);
        assertFalse(B.BUT___.isEmpty());
        assertNotNull(B.BUTTON_WITH_TEXT);
        assertFalse(B.BUTTON_WITH_TEXT.isEmpty());
    }
}
