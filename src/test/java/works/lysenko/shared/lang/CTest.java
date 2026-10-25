package works.lysenko.shared.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CTest {

    @Test
    void testInstantiation() {
        final C instance = new C();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(C.CLOSING_THE_DIALOGUE_BOX);
        assertFalse(C.CLOSING_THE_DIALOGUE_BOX.isEmpty());
    }
}
