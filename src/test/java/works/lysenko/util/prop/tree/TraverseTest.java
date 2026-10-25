package works.lysenko.util.prop.tree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.tree.Traverse.*;

class TraverseTest {
    @Test
    void testDefaultsAndRefresh() {
        assertTrue(extensions == true || extensions == false);
        assertDoesNotThrow(Traverse::refresh);
        assertDoesNotThrow(Traverse::freeze);
        assertDoesNotThrow(Traverse::unfreeze);
        assertNotNull(new Traverse());
    }
}

