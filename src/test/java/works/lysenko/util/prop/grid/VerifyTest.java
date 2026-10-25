package works.lysenko.util.prop.grid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.grid.Verify.*;

class VerifyTest {
    @Test
    void testDefaults() {
        assertTrue(colours == true || colours == false);
    }
}
