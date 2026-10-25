package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BTest {

    @Test
    void testInstantiation() {
        final B instance = new B();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(B.BOX);
        assertFalse(B.BOX.isEmpty());
        assertNotNull(B.BILLING);
        assertFalse(B.BILLING.isEmpty());
    }
}
