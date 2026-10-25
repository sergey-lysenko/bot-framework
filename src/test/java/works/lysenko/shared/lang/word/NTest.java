package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NTest {

    @Test
    void testInstantiation() {
        final N instance = new N();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(N.NEW);
        assertFalse(N.NEW.isEmpty());
    }
}
