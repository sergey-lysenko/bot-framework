package works.lysenko.util.func.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BooleansTest {

    @Test
    void testBooleansInstantiation() {
        Booleans booleans = new Booleans();
        assertNotNull(booleans);
    }

    @Test
    void testIsFalse() {
        assertTrue(Booleans.isFalse(false));
        assertFalse(Booleans.isFalse(true));
        assertFalse(Booleans.isFalse(null));
    }
}
