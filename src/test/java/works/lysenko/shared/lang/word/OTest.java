package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OTest {

    @Test
    void testInstantiation() {
        final O instance = new O();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(O.ORDERS);
        assertFalse(O.ORDERS.isEmpty());
        assertNotNull(O.OPEN);
        assertFalse(O.OPEN.isEmpty());
    }
}
