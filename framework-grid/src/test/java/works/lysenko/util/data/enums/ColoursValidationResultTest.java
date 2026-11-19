package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColoursValidationResultTest {

    @Test
    void testIsPassedAndRender() {
        assertTrue(ColoursValidationResult.OK.isPassed());
        assertTrue(ColoursValidationResult.WITHIN_STATIC_MARGIN.isPassed());
        assertTrue(ColoursValidationResult.WITHIN_DYNAMIC_MARGIN.isPassed());

        assertFalse(ColoursValidationResult.NOT_OK.isPassed());
        assertFalse(ColoursValidationResult.RANGER_FAILED.isPassed());

        assertNotNull(ColoursValidationResult.OK.render());
        assertNotNull(ColoursValidationResult.NOT_OK.render());
    }
}
