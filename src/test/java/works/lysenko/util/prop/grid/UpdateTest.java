package works.lysenko.util.prop.grid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.grid.Update.*;

class UpdateTest {
    @Test
    void testDefaults() {
        assertTrue(colour == true || colour == false);
        assertTrue(hsv == true || hsv == false);
        assertTrue(passed == true || passed == false);
        assertTrue(polychromy == true || polychromy == false);
        assertTrue(shrink == true || shrink == false);
    }
}
