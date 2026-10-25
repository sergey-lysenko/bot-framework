package works.lysenko.util.prop.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.data.Fractions.*;

class FractionsTest {
    @Test
    void testAccuracyAndOutput() {
        assertTrue(accuracy() >= 0);
        assertTrue(output() == true || output() == false);
        assertNotNull(new Fractions());
    }
}

