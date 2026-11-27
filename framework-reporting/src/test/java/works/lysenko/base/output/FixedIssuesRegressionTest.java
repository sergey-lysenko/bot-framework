package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.output.SummaryPlaque.RunConfig;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.plaque.PlaqueMetricsCalculator;
import works.lysenko.base.output.tree.NodeEventClassifier;
import works.lysenko.base.output.tree.TreeTheme;
import works.lysenko.base.output.tree.VisualNode;
import works.lysenko.base.output.tree.VisualNodeResolver;
import works.lysenko.base.responses.ExpectedResponsesStore;
import works.lysenko.tree.base.Leaf;
import works.lysenko.tree.base.Node;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.data.type.logr.type.Event;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class FixedIssuesRegressionTest {

    private KnownIssuesStore previousKnownStore;
    private KnownIssuesStore previousFixedStore;
    private ExpectedResponsesStore previousExpectedStore;

    @BeforeEach
    void setUp() {
        previousKnownStore = TreeHtml.getKnownIssuesStore();
        previousFixedStore = TreeHtml.getFixedIssuesStore();
        previousExpectedStore = TreeHtml.getExpectedResponsesStore();

        TreeHtml.setKnownIssuesStore(new KnownIssuesStore(List.of()));
        TreeHtml.setFixedIssuesStore(new KnownIssuesStore(List.of()));
        TreeHtml.setExpectedResponsesStore(new ExpectedResponsesStore(List.of()));
    }

    @AfterEach
    void tearDown() {
        TreeHtml.setKnownIssuesStore(previousKnownStore);
        TreeHtml.setFixedIssuesStore(previousFixedStore);
        TreeHtml.setExpectedResponsesStore(previousExpectedStore);
    }

    private static _Scenario leaf(final String name) {
        return new Leaf(Fraction.ONE) {
            @Override public String getName() { return "demo." + name; }
            @Override public String getShortName() { return name; }
            @Override public String getSimpleName() { return name; }
            @Override public ScenarioType type() { return ScenarioType.LEAF; }
        };
    }

    private static _Scenario parentNode(final String name, final _Scenario child) {
        return new Node(child) {
            @Override public String getName() { return "demo." + name; }
            @Override public String getShortName() { return name; }
            @Override public String getSimpleName() { return name; }
        };
    }

    private static String key(final _Scenario s) {
        return works.lysenko.util.data.strs.Bind.b(s.getShortName(), s.type().tag());
    }

    @Test
    @DisplayName("Failure-tier fixed issue match classifies as REGRESSION (vivid electric magenta) and updates plaque")
    void testFailureTierRegression() {
        final KnownIssue fixedCrash = new KnownIssue(
                "CheckoutLeaf", "FIX-500", "Fixed NPE on checkout",
                "https://jira.example.com/FIX-500", "CheckoutNullPointer",
                false, "2026-04-10T10:00:00Z", "run-20260410");
        TreeHtml.setFixedIssuesStore(new KnownIssuesStore(List.of(fixedCrash)));

        final _Scenario checkout = leaf("CheckoutLeaf");
        final Result res = new Result(checkout);
        res.tick();
        res.addEvent(new LogRecord(1, 100L,
                new Event(1, EventType.FAILURE, 0, "CheckoutNullPointer occurred", null), checkout, null));

        final TreeMap<String, Result> sorted = new TreeMap<>();
        sorted.put(key(checkout), res);

        final TreeLayout layout = TreeHtml.computeLayout(List.of(checkout), sorted);
        final NodeData node = layout.nodes().get(0);

        final NodeEventClassifier.Classification cls = NodeEventClassifier.classify(node);
        assertTrue(cls.hasRegression());
        assertTrue(cls.hasFailure(), "Failure-level regression must also count as failure for upstream propagation");
        assertFalse(cls.hasRegressionWarning());
        assertEquals(1, node.fixedIssues().size());

        final VisualNodeResolver resolver = new VisualNodeResolver(TreeTheme.DEFAULT, Set.of(checkout), false);
        final VisualNode vn = resolver.resolve(node, false);
        assertEquals("REGRESSION", vn.statusName());
        assertTrue(vn.hasRegressions());
        assertEquals("#e879f9", TreeTheme.DEFAULT.hex(vn.borderColor()));
        assertEquals("#4a044e", TreeTheme.DEFAULT.hex(vn.backgroundColor()));

        final SummaryPlaque plaque = PlaqueMetricsCalculator.computeFromNodes(
                layout.nodes(), new RunConfig("smoke", "", "", "", "", "", ""));
        assertEquals(1, plaque.regression());
        assertEquals(0, plaque.failed());
        assertEquals(0, plaque.regressionWarning());
        assertTrue(plaque.visibleTreeColumns().stream().anyMatch(c -> "Regression".equals(c.label())));
        assertFalse(plaque.visibleTreeColumns().stream().anyMatch(c -> "Reg. Warn".equals(c.label())));

        final String html = TreeHtml.renderHtml(layout.nodes(), layout.edges());
        assertTrue(html.contains("node-card regression"));
        assertTrue(html.contains("FIX-500"));
        assertTrue(html.contains("Resolved: 2026-04-10T10:00:00Z"));
        assertTrue(html.contains("Verified in run: run-20260410"));
    }

    @Test
    @DisplayName("Warning-tier fixed issue match classifies as REGRESSION_WARNING (soft rose-magenta)")
    void testWarningTierRegression() {
        final KnownIssue fixedWarn = new KnownIssue(
                "WarnLeaf", "FIX-200", "Fixed console warning",
                "https://jira.example.com/FIX-200", "DeprecatedApiWarning");
        TreeHtml.setFixedIssuesStore(new KnownIssuesStore(List.of(fixedWarn)));

        final _Scenario warnLeaf = leaf("WarnLeaf");
        final Result res = new Result(warnLeaf);
        res.tick();
        res.addEvent(new LogRecord(1, 100L,
                new Event(1, EventType.WARNING, 0, "DeprecatedApiWarning logged", null), warnLeaf, null));

        final TreeMap<String, Result> sorted = new TreeMap<>();
        sorted.put(key(warnLeaf), res);

        final TreeLayout layout = TreeHtml.computeLayout(List.of(warnLeaf), sorted);
        final NodeData node = layout.nodes().get(0);

        final NodeEventClassifier.Classification cls = NodeEventClassifier.classify(node);
        assertFalse(cls.hasRegression());
        assertFalse(cls.hasFailure());
        assertTrue(cls.hasRegressionWarning());
        assertTrue(cls.hasWarning());

        final VisualNodeResolver resolver = new VisualNodeResolver(TreeTheme.DEFAULT, Set.of(warnLeaf), false);
        final VisualNode vn = resolver.resolve(node, false);
        assertEquals("REGRESSION_WARNING", vn.statusName());
        assertTrue(vn.hasRegressions());
        assertEquals("#f472b6", TreeTheme.DEFAULT.hex(vn.borderColor()));
        assertEquals("#3b0d2c", TreeTheme.DEFAULT.hex(vn.backgroundColor()));

        final SummaryPlaque plaque = PlaqueMetricsCalculator.computeFromNodes(
                layout.nodes(), new RunConfig("smoke", "", "", "", "", "", ""));
        assertEquals(0, plaque.regression());
        assertEquals(1, plaque.regressionWarning());
        assertEquals(0, plaque.warning());
        assertTrue(plaque.visibleTreeColumns().stream().anyMatch(c -> "Reg. Warn".equals(c.label())));
        assertFalse(plaque.visibleTreeColumns().stream().anyMatch(c -> "Regression".equals(c.label())));
    }

    @Test
    @DisplayName("Severity wins: unknown S1 failure overrides S2 fixed-issue match (stays red FAILED)")
    void testSeverityWinsOverWarningRegression() {
        final KnownIssue fixedWarn = new KnownIssue(
                "MixedLeaf", "FIX-W", "Fixed warning",
                "https://jira.example.com/FIX-W", "OLD_WARNING_PATTERN");
        TreeHtml.setFixedIssuesStore(new KnownIssuesStore(List.of(fixedWarn)));

        final _Scenario mixed = leaf("MixedLeaf");
        final Result res = new Result(mixed);
        res.tick();
        res.addEvent(new LogRecord(1, 100L,
                new Event(1, EventType.WARNING, 0, "OLD_WARNING_PATTERN seen", null), mixed, null));
        res.addEvent(new LogRecord(1, 120L,
                new Event(2, EventType.FAILURE, 0, "Brand new unknown S1 crash", null), mixed, null));

        final TreeMap<String, Result> sorted = new TreeMap<>();
        sorted.put(key(mixed), res);

        final TreeLayout layout = TreeHtml.computeLayout(List.of(mixed), sorted);
        final NodeData node = layout.nodes().get(0);

        final NodeEventClassifier.Classification cls = NodeEventClassifier.classify(node);
        assertTrue(cls.hasFailure());
        assertFalse(cls.hasRegression(), "Warning-tier fixed issue must NOT elevate unknown S1 failure to REGRESSION");
        assertFalse(cls.hasRegressionWarning(), "hasRegressionWarning must be suppressed when node has S1 failure");

        final VisualNodeResolver resolver = new VisualNodeResolver(TreeTheme.DEFAULT, Set.of(mixed), false);
        assertEquals("FAILED", resolver.resolve(node, false).statusName());

        final SummaryPlaque plaque = PlaqueMetricsCalculator.computeFromNodes(
                layout.nodes(), new RunConfig("smoke", "", "", "", "", "", ""));
        assertEquals(1, plaque.failed());
        assertEquals(0, plaque.regression());
        assertEquals(0, plaque.regressionWarning());
    }

    @Test
    @DisplayName("Tie-breaker: Regression wins when same severity tier has both unknown and fixed-issue events")
    void testTieBreakerRegressionWins() {
        final KnownIssue fixedFail = new KnownIssue("FailTieLeaf", "FIX-F", "Desc", "https://jira/FIX-F", "OLD_CRASH");
        final KnownIssue fixedWarn = new KnownIssue("WarnTieLeaf", "FIX-W", "Desc", "https://jira/FIX-W", "OLD_WARN");
        TreeHtml.setFixedIssuesStore(new KnownIssuesStore(List.of(fixedFail, fixedWarn)));

        final _Scenario failTie = leaf("FailTieLeaf");
        final Result failRes = new Result(failTie);
        failRes.tick();
        failRes.addEvent(new LogRecord(1, 100L, new Event(1, EventType.FAILURE, 0, "Brand new S1 failure", null), failTie, null));
        failRes.addEvent(new LogRecord(1, 110L, new Event(2, EventType.FAILURE, 0, "OLD_CRASH returned", null), failTie, null));

        final _Scenario warnTie = leaf("WarnTieLeaf");
        final Result warnRes = new Result(warnTie);
        warnRes.tick();
        warnRes.addEvent(new LogRecord(2, 200L, new Event(1, EventType.WARNING, 0, "Brand new S2 warning", null), warnTie, null));
        warnRes.addEvent(new LogRecord(2, 210L, new Event(2, EventType.WARNING, 0, "OLD_WARN returned", null), warnTie, null));

        final TreeMap<String, Result> sorted = new TreeMap<>();
        sorted.put(key(failTie), failRes);
        sorted.put(key(warnTie), warnRes);

        final TreeLayout layout = TreeHtml.computeLayout(List.of(failTie, warnTie), sorted);
        final VisualNodeResolver resolver = new VisualNodeResolver(TreeTheme.DEFAULT, Set.of(failTie, warnTie), false);

        final NodeData failNode = layout.nodes().stream().filter(n -> n.scenario() == failTie).findFirst().orElseThrow();
        final NodeData warnNode = layout.nodes().stream().filter(n -> n.scenario() == warnTie).findFirst().orElseThrow();

        assertEquals("REGRESSION", resolver.resolve(failNode, false).statusName());
        assertEquals("REGRESSION_WARNING", resolver.resolve(warnNode, false).statusName());
    }

    @Test
    @DisplayName("Active known issue takes precedence over fixed issue (both downgraded and non-downgraded)")
    void testActiveKnownIssuePrecedenceOverFixedIssue() {
        final KnownIssue activeDowngraded = new KnownIssue("DowngradedLeaf", "ACT-1", "Active", "https://jira/ACT-1", "SHARED_BUG", true);
        final KnownIssue activeSevere = new KnownIssue("SevereLeaf", "ACT-2", "Active", "https://jira/ACT-2", "SHARED_BUG", false);
        final KnownIssue fixedWildcard = new KnownIssue("*", "OLD-1", "Fixed", "https://jira/OLD-1", "SHARED_BUG", false);

        TreeHtml.setKnownIssuesStore(new KnownIssuesStore(List.of(activeDowngraded, activeSevere)));
        TreeHtml.setFixedIssuesStore(new KnownIssuesStore(List.of(fixedWildcard)));

        final _Scenario downgradedLeaf = leaf("DowngradedLeaf");
        final Result r1 = new Result(downgradedLeaf);
        r1.tick();
        r1.addEvent(new LogRecord(1, 100L, new Event(1, EventType.FAILURE, 0, "Hit SHARED_BUG", null), downgradedLeaf, null));

        final _Scenario severeLeaf = leaf("SevereLeaf");
        final Result r2 = new Result(severeLeaf);
        r2.tick();
        r2.addEvent(new LogRecord(2, 200L, new Event(2, EventType.FAILURE, 0, "Hit SHARED_BUG", null), severeLeaf, null));

        final TreeMap<String, Result> sorted = new TreeMap<>();
        sorted.put(key(downgradedLeaf), r1);
        sorted.put(key(severeLeaf), r2);

        final TreeLayout layout = TreeHtml.computeLayout(List.of(downgradedLeaf, severeLeaf), sorted);
        final VisualNodeResolver resolver = new VisualNodeResolver(TreeTheme.DEFAULT, Set.of(downgradedLeaf, severeLeaf), false);

        final NodeData n1 = layout.nodes().stream().filter(n -> n.scenario() == downgradedLeaf).findFirst().orElseThrow();
        final NodeData n2 = layout.nodes().stream().filter(n -> n.scenario() == severeLeaf).findFirst().orElseThrow();

        assertTrue(n1.fixedIssues().isEmpty(), "Event matched by active known issue must not attach fixed issue");
        assertEquals("WARNING", resolver.resolve(n1, false).statusName());

        assertTrue(n2.fixedIssues().isEmpty(), "Event matched by active known issue must not attach fixed issue");
        assertEquals("FAILED", resolver.resolve(n2, false).statusName());
    }

    @Test
    @DisplayName("Parent of a REGRESSION leaf resolves to UPSET")
    void testParentOfRegressionLeafIsUpset() {
        final KnownIssue fixedCrash = new KnownIssue("RegressedChild", "FIX-9", "Desc", "https://jira/FIX-9", "CHILD_BOOM");
        TreeHtml.setFixedIssuesStore(new KnownIssuesStore(List.of(fixedCrash)));

        final _Scenario child = leaf("RegressedChild");
        final _Scenario parent = parentNode("ParentGroup", child);

        final Result parentRes = new Result(parent);
        parentRes.tick();
        final Result childRes = new Result(child);
        childRes.tick();
        childRes.addEvent(new LogRecord(1, 100L, new Event(1, EventType.FAILURE, 0, "CHILD_BOOM", null), child, null));

        final TreeMap<String, Result> sorted = new TreeMap<>();
        sorted.put(key(parent), parentRes);
        sorted.put(key(child), childRes);

        final TreeLayout layout = TreeHtml.computeLayout(List.of(parent), sorted);
        final VisualNodeResolver resolver = new VisualNodeResolver(TreeTheme.DEFAULT, Set.of(parent, child), false);

        final NodeData parentData = layout.nodes().stream().filter(n -> n.scenario() == parent).findFirst().orElseThrow();
        final NodeData childData = layout.nodes().stream().filter(n -> n.scenario() == child).findFirst().orElseThrow();

        assertEquals("REGRESSION", resolver.resolve(childData, false).statusName());
        assertEquals("UPSET", resolver.resolve(parentData, false).statusName());
    }
}
