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
    }

    @Test
    void testSaveRunKnownIssues() {
        assertDoesNotThrow(() -> KnownIssuesLoader.saveRunKnownIssues());
    }
}
