package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UTest {

    @Test
    void testInstantiation() {
        final U instance = new U();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(U.USAGE);
        assertFalse(U.USAGE.isEmpty());
    }
}
