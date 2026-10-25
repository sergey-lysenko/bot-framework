package works.lysenko.util.prop.grid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.grid.Ranges.*;

class RangesTest {
    @Test
    void testDefaults() {
        assertTrue(width > 0);
        assertNotNull(new Ranges());
    }
}

