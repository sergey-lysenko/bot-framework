package works.lysenko.util.prop.grid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.grid.Allowed.*;

class AllowedTest {
    @Test
    void testDefaults() {
        assertNotNull(defaultRangedMargin);
        assertNotNull(defaultPolychromyMargin);
        assertNotNull(defaultHSVmargin);
        assertNotNull(noValidation);
        assertTrue(marginSeverityReduction >= 0);
        assertTrue(otherSeverityReduction >= 0);
    }
}
