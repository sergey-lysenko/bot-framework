package works.lysenko.util.data.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinsTest {

    @Test
    void testOperations() {
        final Pins pins1 = new Pins(10);
        final Pins pins2 = new Pins(10);

        pins1.set(0, 3, true);
        assertEquals(3, pins1.cardinality());
        assertTrue(pins1.is(0));
        assertFalse(pins1.is(5));

        pins2.set(2, 5, true);
        assertTrue(Pins.intersects(pins1, pins2));

        final Pins copy = (Pins) pins1.copy();
        assertEquals(3, copy.cardinality());

        assertNotNull(pins1.asStringList());
        assertNotNull(pins1.toString());
    }
}
