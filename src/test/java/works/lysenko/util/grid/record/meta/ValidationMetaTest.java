package works.lysenko.util.grid.record.meta;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Severity;

import static org.junit.jupiter.api.Assertions.*;

class ValidationMetaTest {

    @Test
    void testValidationMetaInstantiation() {
        ValidationMeta meta = new ValidationMeta(Severity.S1, null, null, 5, null);
        assertEquals(Severity.S1, meta.max());
        assertNull(meta.subject());
        assertNull(meta.margin());
        assertEquals(5, meta.fences());
        assertNull(meta.vr());
    }
}
