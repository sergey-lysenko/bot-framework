package works.lysenko.shared.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FTest {

    @Test
    void testInstantiation() {
        final F instance = new F();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(F.FILLING_IN_DATA_FOR);
        assertFalse(F.FILLING_IN_DATA_FOR.isEmpty());
    }
}
