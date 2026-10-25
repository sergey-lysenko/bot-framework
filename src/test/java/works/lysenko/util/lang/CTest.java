package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CTest {

    @Test
    void testInstantiation() {
        C instance = new C();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(C.CHECKING_VISIBILITY_OF);
        assertFalse(C.CHECKING_VISIBILITY_OF.isEmpty());
        assertNotNull(C.CLICKING_ON);
        assertFalse(C.CLICKING_ON.isEmpty());
        assertNotNull(C.COMMA_SPACE);
        assertFalse(C.COMMA_SPACE.isEmpty());
        assertNotNull(C.COLOURS_RESULT);
        assertFalse(C.COLOURS_RESULT.isEmpty());
        assertNotNull(C.CAUGHT_DURING_EXECUTION_OF);
        assertFalse(C.CAUGHT_DURING_EXECUTION_OF.isEmpty());
        assertNotNull(C.CLOSING_TEST_SERVICE);
        assertFalse(C.CLOSING_TEST_SERVICE.isEmpty());
    }
}
