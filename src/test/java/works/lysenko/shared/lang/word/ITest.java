package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ITest {

    @Test
    void testInstantiation() {
        final I instance = new I();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(I.IMPROPER);
        assertFalse(I.IMPROPER.isEmpty());
        assertNotNull(I.INVOICES);
        assertFalse(I.INVOICES.isEmpty());
    }
}
