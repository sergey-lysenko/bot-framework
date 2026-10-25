package works.lysenko.util.prop.tree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.tree.Include.*;

class IncludeTest {
    @Test
    void testDefaultsAndRefresh() {
        assertTrue(upstream == true || upstream == false);
        assertTrue(downstream == true || downstream == false);
        assertDoesNotThrow(Include::refresh);
        assertNotNull(new Include());
    }
}

