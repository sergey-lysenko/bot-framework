package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PTest {

    @Test
    void testInstantiation() {
        final P instance = new P();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(P.PASSWORD);
        assertFalse(P.PASSWORD.isEmpty());
        assertNotNull(P.PARTNERS);
        assertFalse(P.PARTNERS.isEmpty());
        assertNotNull(P.PAYMENTS);
        assertFalse(P.PAYMENTS.isEmpty());
        assertNotNull(P.PRODUCTS);
        assertFalse(P.PRODUCTS.isEmpty());
        assertNotNull(P.PROFILE);
        assertFalse(P.PROFILE.isEmpty());
    }
}
