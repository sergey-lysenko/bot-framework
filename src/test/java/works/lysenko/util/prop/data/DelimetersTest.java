package works.lysenko.util.prop.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.data.Delimeters.*;

class DelimetersTest {
    @Test
    void testDelimiters() {
        assertTrue(L0 != 0);
        assertTrue(L1 != 0);
        assertTrue(L2 != 0);
        assertNotNull(new Delimeters());
    }
}

