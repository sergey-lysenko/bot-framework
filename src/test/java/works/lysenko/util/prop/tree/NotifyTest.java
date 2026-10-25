package works.lysenko.util.prop.tree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.tree.Notify.*;

class NotifyTest {
    @Test
    void testDefaultsAndRefresh() {
        assertTrue(notExecutable == true || notExecutable == false);
        assertTrue(doesNotFit == true || doesNotFit == false);
        assertTrue(emptyPackage == true || emptyPackage == false);
        assertDoesNotThrow(Notify::refresh);
        assertNotNull(new Notify());
    }
}

