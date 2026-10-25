package works.lysenko.shared.lang.chrs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Underscore3Test {

    @Test
    void testInstantiation() {
        final ___ instance = new ___();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(___.CSV);
        assertEquals("csv", ___.CSV);
    }
}
