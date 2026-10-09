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
        assertTrue(KnownIssuesStore.matchesScenario("scenarios.absentData.*", "biteheist.tree.scenarios.absentData.Password", "Password"));
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

    @Test
    @DisplayName("Matches BiteHeist known issue patterns with whitespace and extra details")
    void testBiteHeistKnownIssues() {
        final String json = """
                {
                  "issues" : [ {
                    "scenario" : "signIn.correctLogin.partners.addNew.random.Save",
                    "title" : "NODEJS-3008",
                    "description" : "Nothing is happening after the Test Bot presses Save button in Add New Partner",
                    "link" : "https://biteheist.atlassian.net/browse/NODEJS-3008",
                    "pattern" : "[FAILURE] Expected condition failed: waiting for element to no longer be visible: By.xpath: //button[(@type='submit' or @type='button') and contains(., 'Save')] (tried for 30 second(s) with 500 milliseconds interval)"
                  }, {
                    "scenario" : "signIn.correctLogin.partners.addNew.AllEmpty",
                    "title" : "NODEJS-3009",
                    "description" : "Messages about required fields are too sparce",
                    "link" : "https://biteheist.atlassian.net/browse/NODEJS-3009",
                    "pattern" : "[SEVERE] Wrong label {Values are not equal: : 'Website is required', 'Please enter website'}"
                  }, {
                    "scenario" : "signIn.correctLogin.partners.addNew.random.absentData.Password",
                    "title" : "NODEJS-3010",
                    "description" : "No messages about required field after. cleanup of field text",
                    "link" : "https://biteheist.atlassian.net/browse/NODEJS-3010",
                    "pattern" : "[FAILURE] no such element: Unable to locate element: {\\"method\\":\\"xpath\\",\\"selector\\\":\\"//input[@type='password']/ancestor::div[@class='col-md-8']//div[@style and contains(@style, 'color: red')]\\"}"
                  } ]
                }
                """;

        final KnownIssuesStore store = new KnownIssuesStore(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        final _Scenario allEmptyScenario = new Leaf(Fraction.ONE) {
            @Override
            public String getName() {
                return "signIn.correctLogin.partners.addNew.AllEmpty";
            }

            @Override
            public String getSimpleName() {
                return "AllEmpty";
            }
        };

        final _LogRecord lrAllEmpty = new LogRecord(1, System.currentTimeMillis(),
                new Event(1, EventType.SEVERE, 0, "Wrong label {\nValues are not equal: : 'Website is required', 'Please enter website'\n}", null));

        final List<KnownIssue> matched1 = store.getForLogRecord(List.of(allEmptyScenario), lrAllEmpty);
        assertEquals(1, matched1.size(), "Should match AllEmpty known issue");
        assertEquals("NODEJS-3009", matched1.get(0).title());

        final _Scenario saveScenario = new Leaf(Fraction.ONE) {
            @Override
            public String getName() {
                return "signIn.correctLogin.partners.addNew.random.Save";
            }

            @Override
            public String getSimpleName() {
                return "Save";
            }
        };

        final _LogRecord lrSave = new LogRecord(2, System.currentTimeMillis(),
                new Event(2, EventType.FAILURE, 0, "Expected condition failed: waiting for element to no longer be visible: By.xpath: //button[(@type='submit' or @type='button') and contains(., 'Save')] (tried for 30 second(s) with 500 milliseconds interval) Build info: version: '4.18.1'", null));

        final List<KnownIssue> matched2 = store.getForLogRecord(List.of(saveScenario), lrSave);
        assertEquals(1, matched2.size(), "Should match Save known issue");
        assertEquals("NODEJS-3008", matched2.get(0).title());

        final _Scenario passwordScenario = new Leaf(Fraction.ONE) {
            @Override
            public String getName() {
                return "biteheist.tree.signIn.correctLogin.partners.addNew.random.absentData.Password";
            }

            @Override
            public String getSimpleName() {
                return "Password";
            }
        };

        final _LogRecord lrPassword = new LogRecord(3, System.currentTimeMillis(),
                new Event(3, EventType.FAILURE, 0, "no such element: Unable to locate element: {\"method\":\"xpath\",\"selector\":\"//input[@type='password']/ancestor::div[@class='col-md-8']//div[@style and contains(@style, 'color: red')]\"}\n  (Session info: chrome=155.0.8059.39)", null));

        final List<KnownIssue> matched3 = store.getForLogRecord(List.of(passwordScenario), lrPassword);
        assertEquals(1, matched3.size(), "Should match Password known issue");
        assertEquals("NODEJS-3010", matched3.get(0).title());
    }
}
