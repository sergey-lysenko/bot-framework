package works.lysenko.base.issues;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnownIssuesLoaderTest {

    @Test
    void testLoadFromStreamJson() {
        final String json = """
                {
                  "issues": [
                    {
                      "scenario": "LoginScenario",
                      "title": "Timeout",
                      "description": "Network timeout",
                      "link": "http://jira",
                      "pattern": "timeout"
                    }
                  ]
                }
                """;
        final List<KnownIssue> issues = KnownIssuesLoader.loadFromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(issues);
        assertEquals(1, issues.size());
        assertEquals("LoginScenario", issues.get(0).scenario());
        assertEquals("Timeout", issues.get(0).title());
    }

    @Test
    void testLoadFromStreamProperties() {
        final String props = "MyScenario=some error pattern\n";
        final List<KnownIssue> issues = KnownIssuesLoader.loadFromStream(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(issues);
        assertEquals(1, issues.size());
        assertEquals("MyScenario", issues.get(0).scenario());
        assertEquals("some error pattern", issues.get(0).pattern());
    }

    @Test
    void testLoadFromStreamEmptyJsonIssuesWithSchema() {
        final String json = """
                {
                  "$schema": "./issues.schema.json",
                  "issues": []
                }
                """;
        final List<KnownIssue> issues = KnownIssuesLoader.loadFromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(issues);
        assertTrue(issues.isEmpty(), "Empty JSON issues list must not fall back to Properties parsing");
    }

    @Test
    void testLoadFromStreamEmptyJsonObjectAndArray() {
        assertTrue(KnownIssuesLoader.loadFromStream(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8))).isEmpty());
        assertTrue(KnownIssuesLoader.loadFromStream(new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8))).isEmpty());
    }

    @Test
    void testLoadFromNullStream() {
        assertTrue(KnownIssuesLoader.loadFromStream(null).isEmpty());
        assertTrue(KnownIssuesLoader.loadFixedFromStream(null).isEmpty());
    }

    @Test
    void testLoadFixedFromStreamParsesMetadataAndForcesNoDowngrade() {
        final String json = """
                {
                  "$schema": "./issues.schema.json",
                  "issues": [
                    {
                      "scenarios": ["LoginScenario", "SignIn.CorrectLogin"],
                      "title": "NODEJS-3100",
                      "description": "Previously fixed crash on sign in",
                      "link": "https://jira.example.com/browse/NODEJS-3100",
                      "pattern": "NullPointerException.*SignIn",
                      "decrease-to-warning": true,
                      "resolvedAt": "2026-04-10T12:00:00Z",
                      "verifiedInRun": "run-20260410-1430"
                    }
                  ]
                }
                """;
        final List<KnownIssue> fixed = KnownIssuesLoader.loadFixedFromStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(2, fixed.size());
        assertEquals("LoginScenario", fixed.get(0).scenario());
        assertEquals("SignIn.CorrectLogin", fixed.get(1).scenario());
        for (final KnownIssue issue : fixed) {
            assertEquals("NODEJS-3100", issue.title());
            assertEquals("2026-04-10T12:00:00Z", issue.resolvedAt());
            assertEquals("run-20260410-1430", issue.verifiedInRun());
            assertFalse(issue.decreaseToWarning(), "Fixed issues must never downgrade severity to warning");
        }
    }

    @Test
    void testKnownIssueAsFixedGuard() {
        final KnownIssue active = new KnownIssue(
                "LoginScenario", "Title", "Desc", "https://jira/1", "err", true, "2026-05-01T00:00:00Z", "run-42");
        assertTrue(active.decreaseToWarning());
        final KnownIssue guard = active.asFixedGuard();
        assertFalse(guard.decreaseToWarning());
        assertEquals("2026-05-01T00:00:00Z", guard.resolvedAt());
        assertEquals("run-42", guard.verifiedInRun());
        assertSame(guard, guard.asFixedGuard(), "asFixedGuard on an already-guarded issue should return itself");
    }
}

