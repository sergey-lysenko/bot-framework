package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Results;
import works.lysenko.base.TestProperties;
import works.lysenko.base.exec.Issues;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.responses.ExpectedResponse;
import works.lysenko.base.responses.ExpectedResponsesStore;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.data.type.logr.type.Event;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NewIssuesTest {

    private Core previousCore;
    private works.lysenko.base.Exec previousExec;
    private TestProperties previousProperties;
    private ExpectedResponsesStore previousExpStore;
    private KnownIssuesStore previousGlobalKiStore;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut;

    private static _Scenario scenario(final String fqn, final String simpleName) {
        return new Leaf(Fraction.ONE) {
            @Override public String getName() { return fqn; }
            @Override public String getSimpleName() { return simpleName; }
        };
    }

    @BeforeEach
    void setUp() throws Exception {
        originalOut = System.out;
        previousCore = Base.core;
        previousExec = Base.exec;
        previousProperties = Base.properties;
        previousExpStore = TreeHtml.getExpectedResponsesStore();
        previousGlobalKiStore = KnownIssuesStore.getGlobalStore();

        TreeHtml.setExpectedResponsesStore(new ExpectedResponsesStore(List.of()));

        Base.properties = new TestProperties();
        Base.properties.readTestConfiguration();

        System.setOut(new PrintStream(outContent));

        final Core core = (Core) SelectionFixture.allocate(Core.class);

        final Field resultsField = Core.class.getDeclaredField("results");
        resultsField.setAccessible(true);
        resultsField.set(core, new Results());

        final Field loggerField = Core.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(core, new works.lysenko.util.apis.log._Logs() {
            @Override public java.io.Closeable getLogWriter() { return null; }
            @Override public void flush() {}
            @Override public int getSpanLength() { return 0; }
            @Override public java.io.Closeable getTelemetryWriter() { return null; }
            @Override public void log(String message) { System.out.println(message); }
            @Override public void log(int level, String message) { System.out.println(message); }
            @Override public void log(int level, String message, Long redefinedTime) { System.out.println(message); }
            @Override public void logEmptyLine() { System.out.println(); }
            @Override public void logEvent(works.lysenko.util.data.enums.Severity severity, String message, String shortStackTrace) { System.out.println(message); }
        });

        final Field coreField = Base.class.getDeclaredField("core");
        coreField.setAccessible(true);
        coreField.set(null, core);

        final works.lysenko.base.Exec exec = (works.lysenko.base.Exec) SelectionFixture.allocate(works.lysenko.base.Exec.class);
        final Field issuesField = works.lysenko.base.Exec.class.getDeclaredField("issues");
        issuesField.setAccessible(true);
        issuesField.set(exec, new Issues());

        final Field execField = Base.class.getDeclaredField("exec");
        execField.setAccessible(true);
        execField.set(null, exec);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (null != originalOut) {
            System.setOut(originalOut);
        }

        NewIssues.setKnownIssuesStore(null);
        KnownIssuesStore.setGlobalStore(previousGlobalKiStore);
        TreeHtml.setExpectedResponsesStore(previousExpStore);
        Base.properties = previousProperties;

        final Field coreField = Base.class.getDeclaredField("core");
        coreField.setAccessible(true);
        coreField.set(null, previousCore);

        final Field execField = Base.class.getDeclaredField("exec");
        execField.setAccessible(true);
        execField.set(null, previousExec);
    }

    @Test
    @DisplayName("NewIssues.run() separates unmatched new issues from confirmed known issues")
    void testConsoleNewAndKnownIssuesSummary() {
        final KnownIssue known = new KnownIssue("com.example.ExecutedScenario", "Known", "Desc", "https://jira/BUG-1", "Known error message");
        NewIssues.setKnownIssuesStore(new KnownIssuesStore(List.of(known)));

        final _Scenario executedScenario = scenario("com.example.ExecutedScenario", "ExecutedScenario");
        final Result result = Base.core.getResults().count(executedScenario);

        final _LogRecord lrKnown = new LogRecord(1, 100L, new Event(1, EventType.FAILURE, 0, "Known error message", null), executedScenario, null);
        final _LogRecord lrNew = new LogRecord(2, 200L, new Event(2, EventType.FAILURE, 0, "Brand new regression error", null), executedScenario, null);
        result.addEvent(lrKnown);
        result.addEvent(lrNew);
        Base.exec.issues().add(lrKnown);
        Base.exec.issues().add(lrNew);

        NewIssues.run();

        final String output = outContent.toString();
        assertTrue(output.contains("Brand new regression error"), "Unmatched failure must be printed under New Issues");
        assertFalse(output.contains("Known error message"), "Matched known issue event must not be printed under New Issues");
        assertTrue(output.contains("https://jira/BUG-1 1"), "Confirmed known issue must report only its matching test number");
    }

    @Test
    @DisplayName("Categorizes Known Issues into Confirmed, Not Reproduced, and Not Checked with Link referencing")
    void testKnownIssuesCategorization() {
        final KnownIssue issueConfirmed = new KnownIssue(
                "com.example.ExecutedScenario",
                "Confirmed Issue Title",
                "Description",
                "https://jira.company.com/browse/BUG-CONFIRMED",
                "Known error message"
        );

        final KnownIssue issueNotReproduced = new KnownIssue(
                "com.example.ExecutedScenario",
                "Not Reproduced Title",
                "Description",
                "https://jira.company.com/browse/BUG-NOT-REPRODUCED",
                "NonExistentPattern"
        );

        final KnownIssue issueNotChecked = new KnownIssue(
                "com.example.UnexecutedScenario",
                "Not Checked Title",
                "Description",
                "https://jira.company.com/browse/BUG-NOT-CHECKED",
                "UncheckedPattern"
        );

        NewIssues.setKnownIssuesStore(new KnownIssuesStore(List.of(
                issueConfirmed, issueNotReproduced, issueNotChecked
        )));

        final _Scenario executedScenario = scenario("com.example.ExecutedScenario", "ExecutedScenario");
        final Result result = Base.core.getResults().count(executedScenario);
        final _LogRecord lr = new LogRecord(1, System.currentTimeMillis(),
                new Event(1, EventType.FAILURE, 0, "Known error message", null));
        result.addEvent(lr);
        Base.exec.issues().add(lr);

        NewIssues.run();

        final String output = outContent.toString();

        final int confirmedIdx = output.indexOf("Confirmed Known Issues:");
        final int notReproIdx = output.indexOf("Known Issues NOT Reproduced:");
        final int notCheckedIdx = output.indexOf("Known Issues NOT Checked:");

        final int confirmedLinkIdx = output.indexOf("https://jira.company.com/browse/BUG-CONFIRMED");
        final int notReproLinkIdx = output.indexOf("https://jira.company.com/browse/BUG-NOT-REPRODUCED");
        final int notCheckedLinkIdx = output.indexOf("https://jira.company.com/browse/BUG-NOT-CHECKED");

        assertTrue(confirmedIdx < confirmedLinkIdx && confirmedLinkIdx < notReproIdx,
                "Confirmed link should appear under Confirmed section");
        assertTrue(notReproIdx < notReproLinkIdx && notReproLinkIdx < notCheckedIdx,
                "Not Reproduced link should appear under Not Reproduced section");
        assertTrue(notCheckedIdx < notCheckedLinkIdx,
                "Not Checked link should appear under Not Checked section");
    }

    @Test
    @DisplayName("Confirmed Known Issues section lists space-separated tests strictly per matched issue")
    void testConfirmedKnownIssuesAccuratePerIssueTestNumbers() {
        final KnownIssue issueAlpha = new KnownIssue("com.example.ExecutedScenario", "Alpha", "Desc", "https://jira/BUG-ALPHA", "ALPHA_FAIL");
        final KnownIssue issueBeta = new KnownIssue("com.example.ExecutedScenario", "Beta", "Desc", "https://jira/BUG-BETA", "BETA_FAIL");

        NewIssues.setKnownIssuesStore(new KnownIssuesStore(List.of(issueAlpha, issueBeta)));

        final _Scenario executedScenario = scenario("com.example.ExecutedScenario", "ExecutedScenario");
        final Result result = Base.core.getResults().count(executedScenario);

        final _LogRecord lr1 = new LogRecord(1, 100L, new Event(1, EventType.FAILURE, 0, "ALPHA_FAIL", null), executedScenario, null);
        final _LogRecord lr2 = new LogRecord(3, 200L, new Event(2, EventType.FAILURE, 0, "ALPHA_FAIL", null), executedScenario, null);
        final _LogRecord lr3 = new LogRecord(7, 300L, new Event(3, EventType.FAILURE, 0, "BETA_FAIL", null), executedScenario, null);

        result.addEvent(lr1);
        result.addEvent(lr2);
        result.addEvent(lr3);
        Base.exec.issues().add(lr1);
        Base.exec.issues().add(lr2);
        Base.exec.issues().add(lr3);

        final KnownIssuesSummary summary = NewIssues.computeKnownIssuesSummary();
        assertEquals(Set.of(1, 3), summary.confirmed().get("https://jira/BUG-ALPHA").tests(),
                "BUG-ALPHA must ONLY attribute tests 1 and 3, not test 7 from BUG-BETA");
        assertEquals(Set.of(7), summary.confirmed().get("https://jira/BUG-BETA").tests(),
                "BUG-BETA must ONLY attribute test 7, not tests 1 and 3 from BUG-ALPHA");
    }

    @Test
    @DisplayName("Events from WrongLogin never confirm a BotCafe known issue even with identical pattern")
    void testNoCrossScenarioBleeding() {
        final KnownIssue botCafeBug = new KnownIssue(
                "biteheist.tree.BotCafe",
                "NODEJS-3355",
                "Blocklist crash",
                "https://jira/NODEJS-3355",
                "static/js/main\\.[a-f0-9]+\\.js\\s+\\d+:\\d+\\s+\\w+"
        );
        NewIssues.setKnownIssuesStore(new KnownIssuesStore(List.of(botCafeBug)));

        final _Scenario wrongLogin = scenario("biteheist.tree.WrongLogin", "WrongLogin");
        final Result wlResult = Base.core.getResults().count(wrongLogin);
        final _LogRecord lr = new LogRecord(17, 500L,
                new Event(1, EventType.WARNING, 0, "[BROWSER] https://stg/static/js/main.ab12cd.js 1:3360947 o", null),
                wrongLogin, null);
        wlResult.addEvent(lr);
        Base.exec.issues().add(lr);

        final KnownIssuesSummary summary = NewIssues.computeKnownIssuesSummary();
        assertFalse(summary.confirmed().containsKey("https://jira/NODEJS-3355"),
                "WrongLogin event must NOT confirm BotCafe known issue");
        assertTrue(summary.notChecked().containsKey("https://jira/NODEJS-3355"),
                "Unexecuted BotCafe issue must remain in NOT Checked");
    }

    @Test
    @DisplayName("Deduplicates multi-scenario known issues across Confirmed > NOT Reproduced > NOT Checked")
    void testMultiScenarioIssueDeduplication() {
        final KnownIssue multiConfirmedA = new KnownIssue("com.example.ScenA", "MULTI-1", "Desc", "https://jira/MULTI-1", "BOOM");
        final KnownIssue multiConfirmedB = new KnownIssue("com.example.ScenB", "MULTI-1", "Desc", "https://jira/MULTI-1", "BOOM");
        final KnownIssue multiConfirmedC = new KnownIssue("com.example.ScenC", "MULTI-1", "Desc", "https://jira/MULTI-1", "BOOM");

        final KnownIssue multiNotReproB = new KnownIssue("com.example.ScenB", "MULTI-2", "Desc", "https://jira/MULTI-2", "NEVER_SEEN");
        final KnownIssue multiNotReproC = new KnownIssue("com.example.ScenC", "MULTI-2", "Desc", "https://jira/MULTI-2", "NEVER_SEEN");

        NewIssues.setKnownIssuesStore(new KnownIssuesStore(List.of(
                multiConfirmedA, multiConfirmedB, multiConfirmedC, multiNotReproB, multiNotReproC
        )));

        final _Scenario scenA = scenario("com.example.ScenA", "ScenA");
        final _Scenario scenB = scenario("com.example.ScenB", "ScenB");
        final Result resA = Base.core.getResults().count(scenA);
        Base.core.getResults().count(scenB); // executed cleanly; ScenC unexecuted

        final _LogRecord boom = new LogRecord(4, 100L, new Event(1, EventType.FAILURE, 0, "BOOM", null), scenA, null);
        resA.addEvent(boom);
        Base.exec.issues().add(boom);

        final KnownIssuesSummary summary = NewIssues.computeKnownIssuesSummary();
        // MULTI-1 must appear ONLY in Confirmed
        assertTrue(summary.confirmed().containsKey("https://jira/MULTI-1"));
        assertFalse(summary.notReproduced().containsKey("https://jira/MULTI-1"));
        assertFalse(summary.notChecked().containsKey("https://jira/MULTI-1"));

        // MULTI-2 was checked in ScenB (even though ScenC did not run) -> ONLY in NOT Reproduced
        assertFalse(summary.confirmed().containsKey("https://jira/MULTI-2"));
        assertTrue(summary.notReproduced().containsKey("https://jira/MULTI-2"));
        assertFalse(summary.notChecked().containsKey("https://jira/MULTI-2"));
    }

    @Test
    @DisplayName("Tracks Pretest lifecycle phase for NOT Reproduced, Confirmed, and ExpectedResponse suppression")
    void testPretestLifecyclePhaseReporting() {
        final KnownIssue pretestIssue = new KnownIssue("Pretest", "NODEJS-3295", "Sign in 502", "https://jira/NODEJS-3295", "api/auth/local.*502");
        final KnownIssue globalMaps = new KnownIssue("*", "NODEJS-3194", "Maps warning", "https://jira/NODEJS-3194", "loading=async");
        NewIssues.setKnownIssuesStore(new KnownIssuesStore(List.of(pretestIssue, globalMaps)));

        TreeHtml.setExpectedResponsesStore(new ExpectedResponsesStore(List.of(
                new ExpectedResponse("Pretest", "favicon\\.ico.*404", "Expected favicon 404")
        )));

        Base.core.getResults().recordPhase("Pretest");
        final _LogRecord mapsLr = new LogRecord(8, 100L,
                new Event(1, EventType.WARNING, 0, "[BROWSER] Google Maps without loading=async", null), null, "Pretest");
        final _LogRecord faviconLr = new LogRecord(8, 120L,
                new Event(2, EventType.WARNING, 0, "[BROWSER] /favicon.ico 404", null), null, "Pretest");
        Base.core.getResults().addEvent(mapsLr);
        Base.core.getResults().addEvent(faviconLr);
        Base.exec.issues().add(mapsLr);
        Base.exec.issues().add(faviconLr);

        final KnownIssuesSummary summary = NewIssues.computeKnownIssuesSummary();
        assertTrue(summary.confirmed().containsKey("https://jira/NODEJS-3194"));
        assertEquals(Set.of(8), summary.confirmed().get("https://jira/NODEJS-3194").tests());
        assertTrue(summary.notReproduced().containsKey("https://jira/NODEJS-3295"),
                "Executed Pretest phase must classify unhit Pretest issue as NOT Reproduced, not NOT Checked");
        assertFalse(summary.notChecked().containsKey("https://jira/NODEJS-3295"));

        NewIssues.run();
        final String output = outContent.toString();
        assertFalse(output.contains("favicon.ico"), "Expected Pretest response must be suppressed from New Issues");
    }
}
