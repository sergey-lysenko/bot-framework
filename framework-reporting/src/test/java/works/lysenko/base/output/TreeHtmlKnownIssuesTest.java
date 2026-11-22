package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.data.type.logr.type.Event;

import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class TreeHtmlKnownIssuesTest {

    private KnownIssuesStore previousStore;

    @BeforeEach
    void setUp() {
        previousStore = TreeHtml.getKnownIssuesStore();
    }

    @AfterEach
    void tearDown() {
        TreeHtml.setKnownIssuesStore(previousStore);
    }

    @Test
    @DisplayName("TreeLayoutComputer resolves matching known issues from result events and renders HTML badges")
    void testTreeHtmlKnownIssuesResolvedAndRendered() {
        final KnownIssue issue = new KnownIssue(
                "TestScenario",
                "Known Timeout",
                "Server is slow during peak hours",
                "https://jira.example.com/BUG-123",
                "PeakTimeoutError"
        );
        final KnownIssue blankPatternIssue = new KnownIssue(
                "CleanScenario",
                "Blank Pattern Issue",
                "Should never auto-match",
                "https://jira.example.com/BUG-999",
                ""
        );

        TreeHtml.setKnownIssuesStore(new KnownIssuesStore(List.of(issue, blankPatternIssue)));

        final _Scenario failingScenario = new Leaf(Fraction.ONE) {
            @Override public String getName() { return "demo.TestScenario"; }
            @Override public String getShortName() { return "TestScenario"; }
            @Override public String getSimpleName() { return "TestScenario"; }
            @Override public ScenarioType type() { return ScenarioType.LEAF; }
        };
        final _Scenario cleanScenario = new Leaf(Fraction.ONE) {
            @Override public String getName() { return "demo.CleanScenario"; }
            @Override public String getShortName() { return "CleanScenario"; }
            @Override public String getSimpleName() { return "CleanScenario"; }
            @Override public ScenarioType type() { return ScenarioType.LEAF; }
        };

        final Result failingRes = new Result(failingScenario);
        failingRes.tick();
        failingRes.addEvent(new LogRecord(1, 100L,
                new Event(1, EventType.FAILURE, 0, "Encountered PeakTimeoutError on /api", null), failingScenario, null));

        final Result cleanRes = new Result(cleanScenario);
        cleanRes.tick();

        final TreeMap<String, Result> sorted = new TreeMap<>();
        sorted.put(works.lysenko.util.data.strs.Bind.b(failingScenario.getShortName(), failingScenario.type().tag()), failingRes);
        sorted.put(works.lysenko.util.data.strs.Bind.b(cleanScenario.getShortName(), cleanScenario.type().tag()), cleanRes);

        final TreeLayout layout = TreeHtml.computeLayout(List.of(failingScenario, cleanScenario), sorted);
        final TreeHtml.NodeData failingNode = layout.nodes().stream()
                .filter(n -> n.scenario() == failingScenario).findFirst().orElseThrow();
        final TreeHtml.NodeData cleanNode = layout.nodes().stream()
                .filter(n -> n.scenario() == cleanScenario).findFirst().orElseThrow();

        assertEquals(1, failingNode.knownIssues().size(), "Matching event must attach Known Timeout via TreeLayoutComputer");
        assertTrue(cleanNode.knownIssues().isEmpty(), "Clean passing scenario must NOT match blank-pattern known issue");

        final String html = TreeHtml.renderHtml(layout.nodes(), layout.edges());
        assertNotNull(html);
        assertTrue(html.contains("known-issue"));
        assertTrue(html.contains("url(#known-issue-hatch)"));
        assertTrue(html.contains("Known Timeout"));
        assertTrue(html.contains("Server is slow during peak hours"));
        assertTrue(html.contains("https://jira.example.com/BUG-123"));
        assertTrue(html.contains("Known Issues:"));
        assertFalse(html.contains("Blank Pattern Issue"));
    }
}
