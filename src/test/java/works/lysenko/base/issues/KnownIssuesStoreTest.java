package works.lysenko.base.issues;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.data.type.logr.type.Event;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnownIssuesStoreTest {

    @Test
    @DisplayName("Matches exact FQN and simple name scenarios")
    void testExactScenarioMatching() {
        assertTrue(KnownIssuesStore.matchesScenario("com.example.scenarios.MyScenario", "com.example.scenarios.MyScenario", "MyScenario"));
        assertTrue(KnownIssuesStore.matchesScenario("MyScenario", "com.example.scenarios.MyScenario", "MyScenario"));
        assertFalse(KnownIssuesStore.matchesScenario("OtherScenario", "com.example.scenarios.MyScenario", "MyScenario"));
    }

    @Test
    @DisplayName("Matches wildcard scenario patterns")
    void testWildcardScenarioMatching() {
        assertTrue(KnownIssuesStore.matchesScenario("com.example.*", "com.example.scenarios.MyScenario", "MyScenario"));
        assertTrue(KnownIssuesStore.matchesScenario("*.MyScenario", "com.example.scenarios.MyScenario", "MyScenario"));
        assertFalse(KnownIssuesStore.matchesScenario("com.other.*", "com.example.scenarios.MyScenario", "MyScenario"));
    }

    @Test
    @DisplayName("Parses JSON stream and matches scenario with pattern")
    void testJsonParsingAndPatternMatching() {
        final String json = """
                {
                  "issues": [
                    {
                      "scenario": "com.example.LoginScenario",
                      "title": "Staging Auth Failure",
                      "description": "Auth service timeout on staging",
                      "link": "https://jira.company.com/browse/BUG-100",
                      "pattern": ".*TimeoutException.*"
                    }
                  ]
                }
                """;

        final KnownIssuesStore store = new KnownIssuesStore(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, store.getIssues().size());

        final _Scenario scenario = new Leaf(Fraction.ONE) {
            @Override
            public String getName() {
                return "com.example.LoginScenario";
            }

            @Override
            public String getSimpleName() {
                return "LoginScenario";
            }
        };

        final Result emptyResult = new Result(scenario);
        List<KnownIssue> matched = store.getForNode(scenario, emptyResult);
        assertTrue(matched.isEmpty());

        final Result timeoutResult = new Result(scenario);
        final _LogRecord logRecord = new LogRecord(1, System.currentTimeMillis(), new Event(1, EventType.FAILURE, 0, "java.util.concurrent.TimeoutException: Connection timed out", null));
        timeoutResult.addEvent(logRecord);

        matched = store.getForNode(scenario, timeoutResult);
        assertEquals(1, matched.size());
        assertEquals("Staging Auth Failure", matched.get(0).title());
        assertEquals("https://jira.company.com/browse/BUG-100", matched.get(0).link());
    }
}
