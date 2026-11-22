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
        assertTrue(KnownIssuesMatcher.matchesScenario("com.example.scenarios.MyScenario", "com.example.scenarios.MyScenario", "MyScenario"));
        assertTrue(KnownIssuesMatcher.matchesScenario("MyScenario", "com.example.scenarios.MyScenario", "MyScenario"));
        assertFalse(KnownIssuesMatcher.matchesScenario("OtherScenario", "com.example.scenarios.MyScenario", "MyScenario"));
    }

    @Test
    @DisplayName("Matches wildcard scenario patterns")
    void testWildcardScenarioMatching() {
        assertTrue(KnownIssuesMatcher.matchesScenario("com.example.*", "com.example.scenarios.MyScenario", "MyScenario"));
        assertTrue(KnownIssuesMatcher.matchesScenario("*.MyScenario", "com.example.scenarios.MyScenario", "MyScenario"));
        assertTrue(KnownIssuesMatcher.matchesScenario("scenarios.absentData.*", "biteheist.tree.scenarios.absentData.Password", "Password"));
        assertFalse(KnownIssuesMatcher.matchesScenario("com.other.*", "com.example.scenarios.MyScenario", "MyScenario"));
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
                    "pattern" : "Wrong label"
                  }, {
                    "scenario" : "signIn.correctLogin.partners.addNew.random.absentData.Password",
                    "title" : "NODEJS-3010",
                    "description" : "No messages about required field after. cleanup of field text",
                    "link" : "https://biteheist.atlassian.net/browse/NODEJS-3010",
                    "pattern" : "Wrong label"
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
                new Event(3, EventType.FAILURE, 0, "Wrong label { Values are not equal: : 'Password is required', 'Required' }", null));

        final List<KnownIssue> matched3 = store.getForLogRecord(List.of(passwordScenario), lrPassword);
        assertEquals(1, matched3.size(), "Should match Password known issue");
        assertEquals("NODEJS-3010", matched3.get(0).title());
    }

    @Test
    @DisplayName("Matches wildcard scenario pattern with getForNode andResult events")
    void testWildcardGetForNode() {
        final String json = """
                {
                  "issues" : [ {
                    "scenario" : "signIn.correctLogin.partners.addNew.random.absentData.*",
                    "title" : "NODEJS-3010",
                    "description" : "No messages about required field after cleanup of field text",
                    "link" : "https://biteheist.atlassian.net/browse/NODEJS-3010",
                    "pattern" : "Wrong label"
                  } ]
                }
                """;

        final KnownIssuesStore store = new KnownIssuesStore(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        final _Scenario addressScenario = new Leaf(Fraction.ONE) {
            @Override
            public String getName() {
                return "biteheist.tree.signIn.correctLogin.partners.addNew.random.absentData.Address";
            }

            @Override
            public String getSimpleName() {
                return "Address";
            }
        };

        final Result result = new Result(addressScenario);
        result.addEvent(new LogRecord(1, System.currentTimeMillis(),
                new Event(1, EventType.FAILURE, 0, "Wrong label { Values are not equal: : 'Address is required', 'Required' }", null)));

        final List<KnownIssue> matched = store.getForNode(addressScenario, result);
        assertEquals(1, matched.size(), "Wildcard scenario with getForNode should match Address scenario result");
        assertEquals("NODEJS-3010", matched.get(0).title());
    }

    @Test
    @DisplayName("Log record matching a known pattern in one scenario does not cross-contaminate unrelated scenarios")
    void testLogRecordDoesNotCrossContaminateUnrelatedScenarios() {
        final String json = """
                {
                  "issues" : [ {
                    "scenario" : "signIn.correctLogin.partners.addNew.random.absentData.*",
                    "title" : "NODEJS-3010",
                    "description" : "No messages about required field after cleanup of field text",
                    "link" : "https://biteheist.atlassian.net/browse/NODEJS-3010",
                    "pattern" : "Wrong label"
                  } ]
                }
                """;

        final KnownIssuesStore store = new KnownIssuesStore(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        final _Scenario billingScenario = new Leaf(Fraction.ONE) {
            @Override
            public String getName() {
                return "biteheist.tree.signIn.correctLogin.billing.Billing";
            }

            @Override
            public String getSimpleName() {
                return "Billing";
            }
        };

        // Log record ACTUALLY contains the pattern "Wrong label" AND mentions absentData in stack text,
        // but originated inside Billing — must NOT match absentData.* issue!
        final _LogRecord lrInBilling = new LogRecord(1, System.currentTimeMillis(),
                new Event(1, EventType.SEVERE, 0, "Wrong label in Billing (ref: absentData.Password)", null),
                billingScenario, null);

        assertTrue(store.getForLogRecord(billingScenario, lrInBilling).isEmpty(),
                "Known issue for absentData should NOT match an event emitted by Billing even when pattern matches");
        assertTrue(store.getForLogRecord(lrInBilling).isEmpty(),
                "Provenance-based lookup must isolate Billing event from absentData known issue");
    }

    @Test
    @DisplayName("Multi-scenario and multi-issue parsing requires matching events and rejects blank patterns")
    void testMultiScenarioAndMultiIssueParsing() {
        final String json = """
                {
                  "issues": [
                    {
                      "title": "NODEJS-3132",
                      "description": "Uncaught React component crash",
                      "link": "https://biteheist.atlassian.net/browse/NODEJS-3132",
                      "pattern": "ReactMinifiedError",
                      "scenarios": [
                        "signIn.login.appManagement.AddNew",
                        "signIn.login.Dashboard"
                      ]
                    }
                  ],
                  "scenarios": [
                    {
                      "scenario": "signIn.login.partners.addNew.random.Save",
                      "issues": [
                        {
                          "title": "NODEJS-3008",
                          "link": "https://biteheist.atlassian.net/browse/NODEJS-3008",
                          "pattern": "SaveTimeout"
                        },
                        {
                          "title": "NODEJS-3009",
                          "link": "https://biteheist.atlassian.net/browse/NODEJS-3009",
                          "pattern": ""
                        }
                      ]
                    }
                  ]
                }
                """;

        final KnownIssuesStore store = new KnownIssuesStore(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(4, store.getIssues().size());

        final _Scenario appMgmtScenario = new Leaf(Fraction.ONE) {
            @Override public String getName() { return "biteheist.tree.signIn.login.appManagement.AddNew"; }
            @Override public String getSimpleName() { return "AddNew"; }
        };

        final _Scenario dashboardScenario = new Leaf(Fraction.ONE) {
            @Override public String getName() { return "biteheist.tree.signIn.login.Dashboard"; }
            @Override public String getSimpleName() { return "Dashboard"; }
        };

        final _Scenario saveScenario = new Leaf(Fraction.ONE) {
            @Override public String getName() { return "biteheist.tree.signIn.login.partners.addNew.random.Save"; }
            @Override public String getSimpleName() { return "Save"; }
        };

        // Passing 0-event results must NEVER match
        assertTrue(store.getForNode(appMgmtScenario, new Result(appMgmtScenario)).isEmpty());
        assertTrue(store.getForNode(dashboardScenario, new Result(dashboardScenario)).isEmpty());
        assertTrue(store.getForNode(saveScenario, new Result(saveScenario)).isEmpty());

        final Result appMgmtResult = new Result(appMgmtScenario);
        appMgmtResult.addEvent(new LogRecord(1, 100L, new Event(1, EventType.FAILURE, 0, "Uncaught ReactMinifiedError #31", null), appMgmtScenario, null));
        final List<KnownIssue> appMgmtMatches = store.getForNode(appMgmtScenario, appMgmtResult);
        assertEquals(1, appMgmtMatches.size());
        assertEquals("NODEJS-3132", appMgmtMatches.get(0).title());

        final Result saveResult = new Result(saveScenario);
        saveResult.addEvent(new LogRecord(2, 200L, new Event(2, EventType.FAILURE, 0, "Hit SaveTimeout on submit", null), saveScenario, null));
        final List<KnownIssue> saveMatches = store.getForNode(saveScenario, saveResult);
        assertEquals(1, saveMatches.size(), "Blank-pattern NODEJS-3009 must not auto-match");
        assertEquals("NODEJS-3008", saveMatches.get(0).title());
    }

    @Test
    @DisplayName("Matches Pretest lifecycle phase provenance for both KnownIssues and ExpectedResponses")
    void testPretestPhaseProvenanceAndDiagnosticsProvider() {
        final KnownIssuesStore prevKi = KnownIssuesStore.getGlobalStore();
        final works.lysenko.base.responses.ExpectedResponsesStore prevExp = works.lysenko.base.responses.ExpectedResponsesStore.getGlobalStore();
        try {
            final KnownIssue pretestBug = new KnownIssue("Pretest", "NODEJS-3295", "Sign in 502", "https://jira/NODEJS-3295", "api/auth/local.*502", true);
            final KnownIssue globalMaps = new KnownIssue("*", "NODEJS-3194", "Maps async", "https://jira/NODEJS-3194", "loading=async", true);
            final KnownIssue catchAll = new KnownIssue("*", "BAD-RULE", "Catch all", "https://jira/BAD", ".*", true);
            KnownIssuesStore.setGlobalStore(new KnownIssuesStore(List.of(pretestBug, globalMaps, catchAll)));

            final works.lysenko.base.responses.ExpectedResponse pretestExp =
                    new works.lysenko.base.responses.ExpectedResponse("Pretest", "favicon\\.ico.*404", "Ignore favicon");
            final works.lysenko.base.responses.ExpectedResponse globalExp =
                    new works.lysenko.base.responses.ExpectedResponse("*", "ExpectedGlobalNotice", "Global expected");
            works.lysenko.base.responses.ExpectedResponsesStore.setGlobalStore(
                    new works.lysenko.base.responses.ExpectedResponsesStore(List.of(pretestExp, globalExp)));

            final DefaultDiagnosticsProvider provider = new DefaultDiagnosticsProvider();

            final _LogRecord pretest502 = new LogRecord(1, 100L,
                    new Event(1, EventType.FAILURE, 0, "[BROWSER] /api/auth/local responded with 502", null), null, "Pretest");
            final _LogRecord pretestMaps = new LogRecord(2, 200L,
                    new Event(2, EventType.WARNING, 0, "[BROWSER] Google Maps API loaded without loading=async", null), null, "Pretest");
            final _LogRecord pretestUnrelated = new LogRecord(3, 300L,
                    new Event(3, EventType.FAILURE, 0, "Completely new unexpected failure", null), null, "Pretest");
            final _LogRecord pretestFavicon = new LogRecord(4, 400L,
                    new Event(4, EventType.WARNING, 0, "[BROWSER] /favicon.ico 404", null), null, "Pretest");
            final _LogRecord globalExpected = new LogRecord(5, 500L,
                    new Event(5, EventType.WARNING, 0, "ExpectedGlobalNotice", null), null, "Pretest");

            assertTrue(provider.shouldDecreaseToWarning(null, pretest502), "Pretest phase should match scenario='Pretest' known issue");
            assertTrue(provider.shouldDecreaseToWarning(null, pretestMaps), "Pretest phase should match scenario='*' known issue");
            assertFalse(provider.shouldDecreaseToWarning(null, pretestUnrelated), "Catch-all '*' + '.*' must NOT match unrelated Pretest failures");
            assertTrue(provider.isExpectedEvent(null, null, pretestFavicon), "Pretest phase should match scenario='Pretest' expected response");
            assertTrue(provider.isExpectedEvent(null, null, globalExpected), "Pretest phase should match scenario='*' expected response");
            assertFalse(provider.isExpectedEvent(null, null, pretest502));
        } finally {
            KnownIssuesStore.setGlobalStore(prevKi);
            works.lysenko.base.responses.ExpectedResponsesStore.setGlobalStore(prevExp);
        }
    }

    @Test
    @DisplayName("Incremental node cursor detects newly appended events and handles multiple scenarios on one Result")
    void testGetForNodeIncrementalCursor() {
        final String json = """
                {
                  "issues": [
                    {
                      "scenario": "*",
                      "title": "BUG-1",
                      "pattern": "ALPHA_ERR"
                    },
                    {
                      "scenario": "demo.Target",
                      "title": "BUG-2",
                      "pattern": "BETA_ERR"
                    }
                  ]
                }
                """;
        final KnownIssuesStore store = new KnownIssuesStore(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        final _Scenario target = new Leaf(Fraction.ONE) {
            @Override public String getName() { return "demo.Target"; }
            @Override public String getSimpleName() { return "Target"; }
        };
        final _Scenario other = new Leaf(Fraction.ONE) {
            @Override public String getName() { return "demo.Other"; }
            @Override public String getSimpleName() { return "Other"; }
        };

        final Result result = new Result(target);
        result.addEvent(new LogRecord(1, System.currentTimeMillis(), new Event(1, EventType.WARNING, 0, "Normal event", null)));

        // Initial scan: no match
        assertTrue(store.getForNode(target, result).isEmpty());
        assertTrue(store.getForNode(other, result).isEmpty());

        // Append ALPHA_ERR -> both target and other match BUG-1
        result.addEvent(new LogRecord(2, System.currentTimeMillis(), new Event(2, EventType.FAILURE, 0, "Hit ALPHA_ERR", null)));
        assertEquals(List.of("BUG-1"), store.getForNode(target, result).stream().map(KnownIssue::title).toList());
        assertEquals(List.of("BUG-1"), store.getForNode(other, result).stream().map(KnownIssue::title).toList());

        // Append BETA_ERR -> target now matches both BUG-1 and BUG-2, while other stays at BUG-1
        result.addEvent(new LogRecord(3, System.currentTimeMillis(), new Event(3, EventType.FAILURE, 0, "Hit BETA_ERR", null)));
        assertEquals(List.of("BUG-1", "BUG-2"), store.getForNode(target, result).stream().map(KnownIssue::title).toList());
        assertEquals(List.of("BUG-1"), store.getForNode(other, result).stream().map(KnownIssue::title).toList());

        // Mutating returned list must not corrupt internal cached matches
        final List<KnownIssue> returned = store.getForNode(target, result);
        returned.clear();
        assertEquals(2, store.getForNode(target, result).size());
    }
}
