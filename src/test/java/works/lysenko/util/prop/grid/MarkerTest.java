package works.lysenko.util.prop.grid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.grid.Marker.*;

class MarkerTest {
    @Test
    void testDefaults() {
        assertNotNull(colour);
        assertNotNull(new Marker());
    }
}

