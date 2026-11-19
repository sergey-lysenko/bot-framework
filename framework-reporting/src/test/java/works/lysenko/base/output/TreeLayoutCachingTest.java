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

    private record SimpleLogRecord(String text) implements _LogRecord {
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
    void incrementalKnownIssuesScanDetectsNewRecordsAndResetsOnNewList() throws Exception {
        KnownIssues.failedOnly = false;
        try (final SelectionFixture ignored = SelectionFixture.install()) {
            final Issues issues = new Issues();
            issues.add(new SimpleLogRecord("Booting test run"));
            issues.add(new SimpleLogRecord("All good so far"));
            final Field issuesField = Exec.class.getDeclaredField("issues");
            issuesField.setAccessible(true);
            issuesField.set(Base.exec, issues);

            final KnownIssue issue = new KnownIssue("RootNode", "Boom", "Desc", "", "CRITICAL_TIMEOUT_999", false);
            TreeHtml.setKnownIssuesStore(new KnownIssuesStore(List.of(issue)));

            final NamedLeaf leaf = new NamedLeaf(Fraction.ONE, "demo.RootNode.LeafA", "LeafA");
            final NamedNode root = new NamedNode("demo.RootNode", "RootNode", leaf);
            final TreeMap<String, Result> sorted = new TreeMap<>();

            // 1. Before matching log appears: no known issue attached
            final TreeLayout layoutBefore = TreeHtml.computeLayout(List.of(root), sorted);
            assertTrue(layoutBefore.nodes().get(0).knownIssues().isEmpty());

            // 2. Append matching record to the same Issues list: incremental scan finds it
            issues.add(new SimpleLogRecord("Observed CRITICAL_TIMEOUT_999 while waiting"));
            final TreeLayout layoutAfter = TreeHtml.computeLayout(List.of(root), sorted);
            assertEquals(1, layoutAfter.nodes().get(0).knownIssues().size());

            // 3. Replace Issues with a fresh list without the match: cache must reset cleanly
            final Issues freshIssues = new Issues();
            freshIssues.add(new SimpleLogRecord("Clean run"));
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
}
