package works.lysenko.shared.lang.word;

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
        assertNotNull(F.FILLING);
        assertFalse(F.FILLING.isEmpty());
        assertNotNull(F.FIRST);
        assertFalse(F.FIRST.isEmpty());
        assertNotNull(F.FILTERS);
        assertFalse(F.FILTERS.isEmpty());
    }
}
