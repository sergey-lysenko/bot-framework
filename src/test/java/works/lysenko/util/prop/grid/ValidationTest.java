package works.lysenko.util.prop.grid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.grid.Validation.*;

class ValidationTest {
    @Test
    void testDefaults() {
        assertTrue(scaleEpsilon >= 0);
        assertNotNull(fences);
        assertNotNull(defaultColoursBorder);
        assertNotNull(defaultColoursCutoff);
        assertNotNull(hsbThreshold);
    }
}
