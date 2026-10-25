package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridValidationResultTest {

    @Test
    void testCopyOrdinalAndIsPassed() {
        assertEquals(GridValidationResult.OK, GridValidationResult.copyOrdinalFrom(ColoursValidationResult.OK));
        assertTrue(GridValidationResult.OK.isPassed());
        assertFalse(GridValidationResult.NOT_OK.isPassed());
        assertNotNull(GridValidationResult.OK.render());
    }
}
