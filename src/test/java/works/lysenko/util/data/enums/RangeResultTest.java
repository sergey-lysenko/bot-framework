package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeResultTest {

    @Test
    void testIsPassedAndRender() {
        assertTrue(RangeResult.OK.isPassed());
        assertFalse(RangeResult.NOT_OK.isPassed());
        assertNotNull(RangeResult.OK.render());
    }
}
