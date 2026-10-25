package works.lysenko.shared.lang.chrs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Underscore2Test {

    @Test
    void testInstantiation() {
        final __ instance = new __();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(__.GN);
        assertEquals("gn", __.GN);
    }
}
