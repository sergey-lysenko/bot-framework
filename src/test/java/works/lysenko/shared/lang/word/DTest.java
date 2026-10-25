package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DTest {

    @Test
    void testInstantiation() {
        final D instance = new D();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(D.DELIVERY);
        assertFalse(D.DELIVERY.isEmpty());
        assertNotNull(D.DATE);
        assertFalse(D.DATE.isEmpty());
        assertNotNull(D.DETAILS);
        assertFalse(D.DETAILS.isEmpty());
    }
}
