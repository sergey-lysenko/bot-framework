package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Similarity.*;

class SimilarityTest {
    @Test
    void testDefaults() {
        assertTrue(delta >= 0);
        assertTrue(depth >= 0);
        assertTrue(limit >= 0);
        assertNotNull(exceptions);
    }
}
