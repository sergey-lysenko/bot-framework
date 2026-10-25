package works.lysenko.util.prop.grid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.grid.Defaults.*;

class DefaultsTest {
    @Test
    void testDefaults() {
        assertNotNull(horizontal);
        assertNotNull(vertical);
        assertNotNull(scale);
        assertNotNull(resolution);
        assertNotNull(new Defaults());
    }
}

