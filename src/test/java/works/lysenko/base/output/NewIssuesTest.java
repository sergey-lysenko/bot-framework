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

import static org.junit.jupiter.api.Assertions.assertTrue;

class NewIssuesTest {

    private Core previousCore;
    private works.lysenko.base.Exec previousExec;
    private TestProperties previousProperties;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousExec = Base.exec;
        previousProperties = Base.properties;

        Base.properties = new TestProperties();
        Base.properties.readTestConfiguration();

        originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        final Core core = (Core) SelectionFixture.allocate(Core.class);

        final Field resultsField = Core.class.getDeclaredField("results");
        resultsField.setAccessible(true);
        resultsField.set(core, new Results());

        final Field loggerField = Core.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(core, new works.lysenko.util.apis.log._Logs() {
            @Override public java.io.Closeable getLogWriter() { return null; }
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
        System.setOut(originalOut);

        NewIssues.setKnownIssuesStore(null);
        Base.properties = previousProperties;

        final Field coreField = Base.class.getDeclaredField("core");
        coreField.setAccessible(true);
        coreField.set(null, previousCore);

        final Field execField = Base.class.getDeclaredField("exec");
        execField.setAccessible(true);
        execField.set(null, previousExec);
    }

    @Test
    @DisplayName("NewIssues.run() prints Known Issues summary section")
    void testConsoleKnownIssuesSummary() {
        final _Scenario executedScenario = new Leaf(Fraction.ONE) {
            @Override
            public String getName() {
                return "com.example.ExecutedScenario";
            }

            @Override
            public String getSimpleName() {
                return "ExecutedScenario";
            }
        };

        final Result result = Base.core.getResults().count(executedScenario);

        // Add a known issue log record to result
        final _LogRecord lr = new LogRecord(1, System.currentTimeMillis(),
                new Event(1, EventType.FAILURE, 0, "Known error message", null));
        result.addEvent(lr);
        Base.exec.issues().add(lr);

        NewIssues.run();

        final String output = outContent.toString();
        assertTrue(output.contains("Known Issues summary") || output.contains("Known issues summary"),
                "Output should contain Known Issues summary section");
        assertTrue(output.contains("Confirmed Known Issues:"),
                "Output should contain Confirmed Known Issues header");
        assertTrue(output.contains("Known Issues NOT Reproduced:"),
                "Output should contain Known Issues NOT Reproduced header");
        assertTrue(output.contains("Known Issues NOT Checked:"),
                "Output should contain Known Issues NOT Checked header");
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

        final KnownIssuesStore customStore = new KnownIssuesStore(List.of(
                issueConfirmed, issueNotReproduced, issueNotChecked
        ));

        NewIssues.setKnownIssuesStore(customStore);

        final _Scenario executedScenario = new Leaf(Fraction.ONE) {
            @Override
            public String getName() {
                return "com.example.ExecutedScenario";
            }

            @Override
            public String getSimpleName() {
                return "ExecutedScenario";
            }
        };

        final Result result = Base.core.getResults().count(executedScenario);
        final _LogRecord lr = new LogRecord(1, System.currentTimeMillis(),
                new Event(1, EventType.FAILURE, 0, "Known error message", null));
        result.addEvent(lr);
        Base.exec.issues().add(lr);

        NewIssues.run();

        final String output = outContent.toString();

        assertTrue(output.contains("https://jira.company.com/browse/BUG-CONFIRMED"),
                "Output should reference confirmed issue link");
        assertTrue(output.contains("https://jira.company.com/browse/BUG-NOT-REPRODUCED"),
                "Output should reference not reproduced issue link");
        assertTrue(output.contains("https://jira.company.com/browse/BUG-NOT-CHECKED"),
                "Output should reference not checked issue link");

        // Verify correct section order / placement
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
}
