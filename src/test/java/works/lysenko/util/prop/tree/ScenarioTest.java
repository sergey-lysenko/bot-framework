package works.lysenko.util.prop.tree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.tree.Scenario.*;

class ScenarioTest {
    @Test
    void testDefaultsAndRefresh() {
        assertNotNull(ignoredList);
        assertNotNull(completionWeight);
        assertNotNull(defaultWeight);
        assertNotNull(selectionRetries);
        assertTrue(ignoreFailedSelection == true || ignoreFailedSelection == false);
        assertTrue(forbidOverexecution == true || forbidOverexecution == false);
        assertTrue(unreachableLeafPruning == true || unreachableLeafPruning == false);
        assertTrue(branchStalePenalty == true || branchStalePenalty == false);
        assertNotNull(root);
        assertDoesNotThrow(Scenario::refresh);
    }
}
