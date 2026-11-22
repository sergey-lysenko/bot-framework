package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Exec;
import works.lysenko.base.exec.Issues;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.output.TreeHtml.AccessibleContext;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.tree.base.Leaf;
import works.lysenko.tree.base.Node;
import works.lysenko.util.apis.log._LogData;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.prop.issues.KnownIssues;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class TreeLayoutCachingTest {

    private Exec previousExec;
    private KnownIssuesStore previousKnownIssuesStore;
    private boolean previousFailedOnly;

    private static final class NamedLeaf extends Leaf {
        private final String fqn;
        private final String simple;

        NamedLeaf(final Fraction weight, final String fqn, final String simple) {
            super(weight);
            this.fqn = fqn;
            this.simple = simple;
        }

        @Override public String getName() { return fqn; }
        @Override public String getShortName() { return fqn; }
        @Override public String getSimpleName() { return simple; }
        @Override public void action() {}
    }

    private static final class NamedNode extends Node {
        private final String fqn;
        private final String simple;

        NamedNode(final String fqn, final String simple, final _Scenario... children) {
            super(children);
            this.fqn = fqn;
            this.simple = simple;
        }

        @Override public String getName() { return fqn; }
        @Override public String getShortName() { return fqn; }
        @Override public String getSimpleName() { return simple; }
    }

    private record SimpleLogRecord(String text, _Scenario scenario) implements _LogRecord {
        @Override public _LogData data() { return null; }
        @Override public int setSpan(final long currentTime) { return 0; }
        @Override public int span() { return 0; }
        @Override public int test() { return 1; }
        @Override public Long time() { return 0L; }
    }

    @BeforeEach
    void setUp() {
        previousExec = Base.exec;
        previousKnownIssuesStore = TreeHtml.getKnownIssuesStore();
        previousFailedOnly = KnownIssues.failedOnly;
    }

    @AfterEach
    void tearDown() {
        Base.exec = previousExec;
        TreeHtml.setKnownIssuesStore(previousKnownIssuesStore);
        KnownIssues.failedOnly = previousFailedOnly;
    }

    @Test
    void knownIssuesScanRespectsScenarioProvenanceAndResetsOnNewList() throws Exception {
        KnownIssues.failedOnly = false;
        try (final SelectionFixture ignored = SelectionFixture.install()) {
            final NamedLeaf leaf = new NamedLeaf(Fraction.ONE, "demo.RootNode.LeafA", "LeafA");
            final NamedNode root = new NamedNode("demo.RootNode", "RootNode", leaf);
            final TreeMap<String, Result> sorted = new TreeMap<>();

            final Issues issues = new Issues();
            issues.add(new SimpleLogRecord("Booting test run", root));
            // Matching pattern emitted by an UNRELATED scenario (leaf) must NOT attach to root!
            issues.add(new SimpleLogRecord("Observed CRITICAL_TIMEOUT_999 while waiting", leaf));
            final Field issuesField = Exec.class.getDeclaredField("issues");
            issuesField.setAccessible(true);
            issuesField.set(Base.exec, issues);

            final KnownIssue issue = new KnownIssue("RootNode", "Boom", "Desc", "", "CRITICAL_TIMEOUT_999", false);
            TreeHtml.setKnownIssuesStore(new KnownIssuesStore(List.of(issue)));

            // 1. Record from leaf must NOT bleed onto root
            final TreeLayout layoutBefore = TreeHtml.computeLayout(List.of(root), sorted);
            assertTrue(layoutBefore.nodes().get(0).knownIssues().isEmpty(),
                    "Log record belonging to LeafA must not attach RootNode known issue");

            // 2. Append matching record owned by root: scan attaches it to root
            issues.add(new SimpleLogRecord("Observed CRITICAL_TIMEOUT_999 while waiting", root));
            final TreeLayout layoutAfter = TreeHtml.computeLayout(List.of(root), sorted);
            assertEquals(1, layoutAfter.nodes().get(0).knownIssues().size());

            // 3. Replace Issues with a fresh list without the match: must reset cleanly
            final Issues freshIssues = new Issues();
            freshIssues.add(new SimpleLogRecord("Clean run", root));
            issuesField.set(Base.exec, freshIssues);
            final TreeLayout layoutReset = TreeHtml.computeLayout(List.of(root), sorted);
            assertTrue(layoutReset.nodes().get(0).knownIssues().isEmpty());
        }
    }

    @Test
    void isTheoreticallyExecutableCachesParentHierarchyAndMatchesSummaryPlaque() {
        final NamedLeaf leaf = new NamedLeaf(Fraction.ONE, "demo.Root.Mid.Leaf", "Leaf");
        final NamedNode mid = new NamedNode("demo.Root.Mid", "Mid", leaf);
        final NamedNode root = new NamedNode("demo.Root", "Root", mid);

        final TreeLayout layout = TreeHtml.computeLayout(List.of(root), new TreeMap<>());
        final Set<_Scenario> accessible = Set.of(root, mid, leaf);
        final AccessibleContext ctx = new AccessibleContext(accessible);
        final Map<NodeData, Boolean> execCache = new HashMap<>();
        final Map<NodeData, Boolean> childFailCache = new HashMap<>();

        final NodeData leafNode = layout.nodes().stream()
                .filter(n -> n.scenario() == leaf)
                .findFirst()
                .orElseThrow();

        assertTrue(TreeHtml.isTheoreticallyExecutable(leafNode, ctx, execCache));
        // Parent chain (root and mid) must also be memoized during leaf evaluation
        assertEquals(3, execCache.size(), "Evaluating leaf should cache itself and all unexecuted ancestors");

        final SummaryPlaque.RunConfig cfg = new SummaryPlaque.RunConfig("Suite", "Pool", null, null, null);
        final SummaryPlaque cachedPlaque = SummaryPlaque.computeFromNodes(layout.nodes(), cfg, ctx, execCache, childFailCache);
        assertEquals(3, cachedPlaque.total());
        assertEquals(3, cachedPlaque.executable());
        assertEquals(0, cachedPlaque.executed());
    }

    @Test
    void treeTrackerReflectsDynamicallyAddedScenariosWithoutStaleCache() throws Exception {
        TreeTracker.reset();
        try (final SelectionFixture fixture = SelectionFixture.install()) {
            final NamedLeaf initialLeaf = new NamedLeaf(Fraction.ONE, "demo.Root.InitialLeaf", "InitialLeaf");
            final NamedNode root = new NamedNode("demo.Root", "Root", initialLeaf);
            final works.lysenko.tree.Ctrl rootCtrl = new works.lysenko.tree.Ctrl(null, List.of(root));

            final works.lysenko.base.Test testObj = SelectionFixture.allocate(works.lysenko.base.Test.class);
            final works.lysenko.util.apis.test._Exec fakeExec = (works.lysenko.util.apis.test._Exec) java.lang.reflect.Proxy.newProxyInstance(
                    works.lysenko.util.apis.test._Exec.class.getClassLoader(),
                    new Class<?>[]{works.lysenko.util.apis.test._Exec.class},
                    (p, m, a) -> "ctrl".equals(m.getName()) ? rootCtrl : ("getRootScenariosList".equals(m.getName()) ? Set.of(root) : null));
            final Field execField = works.lysenko.base.Test.class.getDeclaredField("executor");
            execField.setAccessible(true);
            execField.set(testObj, fakeExec);
            final Field testField = works.lysenko.base.Core.class.getDeclaredField("test");
            testField.setAccessible(true);
            testField.set(fixture.core(), testObj);

            // Frame 1: only InitialLeaf exists
            final TreeLayout layout1 = TreeHtml.computeLayout(List.of(root), new TreeMap<>());
            final String svg1 = TreeTracker.renderTreeProgressionSvg(layout1, 1, 1);
            assertTrue(svg1.contains("Leaves: 0/1"), "Initial frame should see 1 accessible leaf");

            // Mutate tree dynamically: add a second leaf to root's pool
            final NamedLeaf addedLeaf = new NamedLeaf(Fraction.ONE, "demo.Root.AddedLeaf", "AddedLeaf");
            root.getPool().add(List.of(addedLeaf));

            // Frame 2: must immediately see both leaves as accessible (not stuck at 1 or marked OFF)
            final TreeLayout layout2 = TreeHtml.computeLayout(List.of(root), new TreeMap<>());
            final String svg2 = TreeTracker.renderTreeProgressionSvg(layout2, 1, 2);
            assertTrue(svg2.contains("Leaves: 0/2"), "Second frame must reflect dynamically added leaf");
            assertFalse(svg2.contains("non_executable"), "Dynamically added leaf must not be marked non_executable");
        } finally {
            TreeTracker.reset();
        }
    }

    private record SeverityLogRecord(String text, works.lysenko.util.data.enums.EventType type) implements _LogRecord {
        @Override public _LogData data() {
            return new works.lysenko.util.data.type.logr.type.Event(1L, type, 0, text, "");
        }
        @Override public int setSpan(final long currentTime) { return 0; }
        @Override public int span() { return 0; }
        @Override public int test() { return 1; }
        @Override public Long time() { return 0L; }
    }

    @Test
    void nodeEventClassifierUpdatesIncrementallyAcrossBrowserSuppressionAndKnownIssues() {
        final works.lysenko.base.responses.ExpectedResponsesStore prevExp = TreeHtml.getExpectedResponsesStore();
        try {
            final works.lysenko.base.responses.ExpectedResponse expRule =
                    new works.lysenko.base.responses.ExpectedResponse("*", "Google Maps JavaScript API warning", "expected");
            TreeHtml.setExpectedResponsesStore(new works.lysenko.base.responses.ExpectedResponsesStore(List.of(expRule)));

            final NamedLeaf leaf = new NamedLeaf(Fraction.ONE, "demo.Root.LeafX", "LeafX");
            final Result res = new Result(leaf);
            res.tick();
            final NodeData node = new NodeData("id1", "LeafX", "Root", 1, 1.0, res, leaf);

            // 1. Add an S2 [BROWSER] event (not yet matched by expected rule)
            res.addEvent(new SeverityLogRecord("[BROWSER] random console warning", works.lysenko.util.data.enums.EventType.WARNING));
            assertFalse(TreeHtml.hasFailure(node));
            assertTrue(TreeHtml.hasWarning(node));
            assertEquals(1, TreeHtml.getUnclassifiedEventCount(node));

            // 2. Append an event matching the expected rule -> retroactively suppresses [BROWSER] companion warning
            res.addEvent(new SeverityLogRecord("[BROWSER] Google Maps JavaScript API warning", works.lysenko.util.data.enums.EventType.WARNING));
            assertFalse(TreeHtml.hasFailure(node));
            assertFalse(TreeHtml.hasWarning(node));
            assertEquals(0, TreeHtml.getUnclassifiedEventCount(node));

            // 3. Append a non-browser S3 notice -> shows notice even though nodeMatchedExpected is true
            res.addEvent(new SeverityLogRecord("Minor layout shift", works.lysenko.util.data.enums.EventType.NOTICE));
            assertTrue(TreeHtml.hasNotice(node));
            assertEquals(1, TreeHtml.getUnclassifiedEventCount(node));

            // 4. Append a non-browser S0 failure -> fails, then decreases to warning when KnownIssue attaches
            res.addEvent(new SeverityLogRecord("Fatal timeout XYZ", works.lysenko.util.data.enums.EventType.FAILURE));
            assertTrue(TreeHtml.hasFailure(node));
            assertFalse(TreeHtml.hasWarning(node));
            assertFalse(TreeHtml.hasNotice(node));

            node.knownIssues().add(new KnownIssue("LeafX", "Timeout", "Desc", "", "Fatal timeout XYZ", true));
            assertFalse(TreeHtml.hasFailure(node));
            assertTrue(TreeHtml.hasWarning(node));
            assertFalse(TreeHtml.hasNotice(node));
        } finally {
            TreeHtml.setExpectedResponsesStore(prevExp);
        }
    }
}
