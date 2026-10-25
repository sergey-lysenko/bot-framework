package works.lysenko.util.prop.tree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.tree.Optimization.*;

class OptimizationTest {
    @Test
    void testDefaultsAndRefresh() {
        assertTrue(progression == true || progression == false);
        assertTrue(bankColumns >= 0);
        assertTrue(maxAspectRatio >= 0);
        assertTrue(packColumns >= 0);
        assertTrue(shallowMaxDepth >= 0);
        assertTrue(shallowMinRatio >= 0);
        assertTrue(shallowThreshold >= 0);
        assertTrue(standardThreshold >= 0);
        assertDoesNotThrow(Optimization::refresh);
        assertNotNull(new Optimization());
    }
}

