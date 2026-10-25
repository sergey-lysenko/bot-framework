package works.lysenko.util.prop.grid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.grid.Stamps.*;

class StampsTest {
    @Test
    void testDefaults() {
        assertTrue(process == true || process == false);
        assertTrue(display == true || display == false);
        assertTrue(compress == true || compress == false);
    }
}
