package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ETest {

    @Test
    void testInstantiation() {
        final E instance = new E();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(E.EMAIL);
        assertFalse(E.EMAIL.isEmpty());
        assertNotNull(E.EDIT);
        assertFalse(E.EDIT.isEmpty());
        assertNotNull(E.EXPORT);
        assertFalse(E.EXPORT.isEmpty());
    }
}
