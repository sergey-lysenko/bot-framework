package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangerResultTest {

    @Test
    void testIsPassedAndRender() {
        assertTrue(RangerResult.OK.isPassed());
        assertFalse(RangerResult.NOT_OK.isPassed());
        assertNotNull(RangerResult.OK.render());
    }
}
