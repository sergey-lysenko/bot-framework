package works.lysenko.base.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Issues}.
 */
class IssuesTest {

    @Test
    void testIssuesOperations() {
        Issues issues = new Issues();
        assertTrue(issues.isLatestEmpty());
        assertNotNull(issues.latest());
        assertNotNull(issues.latestCopy());
        assertTrue(issues.latestCopy().isEmpty());
    }
}
