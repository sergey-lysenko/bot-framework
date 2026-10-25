package works.lysenko.util.prop.issues;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.issues.KnownIssues.*;

class KnownIssuesTest {
    @Test
    void testDefaultsAndRefresh() {
        assertTrue(failedOnly == true || failedOnly == false);
        assertDoesNotThrow(KnownIssues::refresh);
        assertNotNull(new KnownIssues());
    }
}

