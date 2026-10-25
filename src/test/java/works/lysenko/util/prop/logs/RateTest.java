package works.lysenko.util.prop.logs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.logs.Rate.*;

class RateTest {
    @Test
    void testDefaults() {
        assertTrue(red >= 0);
        assertTrue(yellow >= 0);
        assertTrue(green >= 0);
        assertTrue(blue >= 0);
    }
}
