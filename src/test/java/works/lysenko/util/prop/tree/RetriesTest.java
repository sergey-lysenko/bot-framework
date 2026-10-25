package works.lysenko.util.prop.tree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.tree.Retries.*;

class RetriesTest {
    @Test
    void testDefaultsAndRefresh() {
        assertTrue(selection >= 0);
        assertNotNull(action);
        assertDoesNotThrow(Retries::refresh);
        assertNotNull(new Retries());
    }
}

