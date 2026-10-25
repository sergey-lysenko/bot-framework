package works.lysenko.util.grid.validation;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.grid.expected.ColoursQuotas;

import static org.junit.jupiter.api.Assertions.*;

class BackgroundValidatorTest {

    @Test
    void testBackgroundValidatorCreation() {
        BackgroundValidator bv = BackgroundValidator.bv("locator", (ColoursQuotas) null, null, Severity.S0);
        assertNotNull(bv);
        assertEquals("locator", bv.locator());
        assertEquals(Severity.S0, bv.failureSeverity());
        assertNull(bv.gridName());
        assertNull(bv.expected());
        assertNull(bv.inImage());
        assertNull(bv.foreground());
    }
}
