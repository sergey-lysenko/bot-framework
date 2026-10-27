package works.lysenko.base.output;

import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.output.tree.TreeHtmlRenderer;
import works.lysenko.base.output.tree.TreeLayoutComputer;
import works.lysenko.base.output.tree.TreeLayoutOptimizer;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.type.Result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static works.lysenko.util.data.strs.Bind.b;

/**
 * Entry point and data model declarations for scenario tree HTML visualization.
 */
@SuppressWarnings({"UtilityClass", "ClassWithoutLogger"})
public final class TreeHtml {

    private static KnownIssuesStore knownIssuesStore = null;

    private TreeHtml() {}

    public static void setKnownIssuesStore(final KnownIssuesStore store) {
        knownIssuesStore = store;
    }

    public static KnownIssuesStore getKnownIssuesStore() {
        if (null == knownIssuesStore) {
            knownIssuesStore = new KnownIssuesStore();
        }
        return knownIssuesStore;
    }

    /**
     * The NodeData class.
     */
    public static final class NodeData {
        private final String id;
        private final String label;
        private final String group;
        private int col;
        private double row;
        private final _Result result;
        private final _Scenario scenario;
        private final List<NodeData> children = new ArrayList<>();
        private final List<KnownIssue> knownIssues = new ArrayList<>();
        private NodeData parent;

        public NodeData(final String id, final String label, final String group, final int col, final double row, final _Result result, final _Scenario scenario) {
            this.id = id;
            this.label = label;
            this.group = group;
            this.col = col;
            this.row = row;
            this.result = result;
            this.scenario = scenario;
        }

        public NodeData(final String id, final String label, final String group, final int col, final double row, final _Result result) {
            this(id, label, group, col, row, result, null);
        }

        public String id() { return id; }
        public String label() { return label; }
        public String group() { return group; }
        public int col() { return col; }
        public void setCol(final int col) { this.col = col; }
        public double row() { return row; }
        public void setRow(final double row) { this.row = row; }
        public _Result result() { return result; }
        public _Scenario scenario() { return scenario; }
        public List<NodeData> children() { return children; }
        public List<KnownIssue> knownIssues() { return knownIssues; }
        public NodeData parent() { return parent; }
        public void setParent(final NodeData parent) { this.parent = parent; }
    }

    public record Edge(NodeData from, NodeData to) {}

    public record TreeLayout(List<NodeData> nodes, List<Edge> edges) {}

    public static final class AccessibleContext {
        public final Set<_Scenario> scenarios;
        public final Set<String> names;
        public final Set<Class<?>> classes;

        public AccessibleContext(final Set<_Scenario> scenarios) {
            this.scenarios = scenarios;
            this.names = new HashSet<>();
            this.classes = new HashSet<>();
            if (null != scenarios && !scenarios.isEmpty()) {
                for (final _Scenario s : scenarios) {
                    if (null == s) continue;
                    names.add(s.getName());
                    names.add(s.getSimpleName());
                    names.add(s.getShortName());
                    if (null != s.type()) {
                        final String tag = s.type().tag();
                        names.add(b(s.getSimpleName(), tag));
                        names.add(b(s.getShortName(), tag));
                        names.add(b(s.getName(), tag));
                    }
                    classes.add(s.getClass());
                    if (null != s.getClass().getSuperclass()) {
                        classes.add(s.getClass().getSuperclass());
                    }
                }
            }
        }
    }

    public static TreeLayout computeLayout(final TreeMap<String, Result> sorted) {
        return TreeLayoutComputer.computeLayout(sorted);
    }

    public static Set<_Scenario> rootsOf(final Iterable<? extends _Scenario> scenarios) {
        return TreeLayoutComputer.rootsOf(scenarios);
    }

    public static TreeLayout computeLayout(final Iterable<? extends _Scenario> roots, final TreeMap<String, Result> sorted) {
        return TreeLayoutComputer.computeLayout(roots, sorted);
    }

    public static boolean hasFailure(final NodeData n) {
        return TreeLayoutComputer.hasFailure(n);
    }

    public static boolean hasChildFailure(final NodeData n) {
        return TreeLayoutComputer.hasChildFailure(n);
    }

    public static boolean hasChildFailure(final NodeData n, final Map<NodeData, Boolean> cache) {
        return TreeLayoutComputer.hasChildFailure(n, cache);
    }

    public static boolean isTheoreticallyExecutable(final NodeData n, final Set<_Scenario> accessibleScenarios) {
        return TreeLayoutComputer.isTheoreticallyExecutable(n, accessibleScenarios);
    }

    public static boolean isTheoreticallyExecutable(
            final NodeData n,
            final Set<_Scenario> accessibleScenarios,
            final Map<NodeData, Boolean> cache) {
        return TreeLayoutComputer.isTheoreticallyExecutable(n, accessibleScenarios, cache);
    }

    public static boolean isTheoreticallyExecutable(
            final NodeData n,
            final AccessibleContext ctx,
            final Map<NodeData, Boolean> cache) {
        return TreeLayoutComputer.isTheoreticallyExecutable(n, ctx, cache);
    }

    public static boolean isTheoreticallyExecutable(final NodeData n) {
        return TreeLayoutComputer.isTheoreticallyExecutable(n);
    }

    public static Set<_Scenario> getAccessibleScenarios() {
        return TreeLayoutComputer.getAccessibleScenarios();
    }

    public static void treeStats() {
        TreeLayoutComputer.treeStats();
    }

    public static void optimizeLayout(final List<NodeData> nodes) {
        TreeLayoutOptimizer.optimizeLayout(nodes);
    }

    public static void compactDisconnectedTrees(final List<NodeData> nodes) {
        TreeLayoutOptimizer.compactDisconnectedTrees(nodes);
    }

    public static double[] solveColumn1D(final double[] targets) {
        return TreeLayoutOptimizer.solveColumn1D(targets);
    }

    public static String renderHtml(final List<NodeData> nodes, final List<Edge> edges) {
        return TreeHtmlRenderer.renderHtml(nodes, edges);
    }

    public static void pruneOffBranches(final List<NodeData> nodes, final List<Edge> edges) {
        TreeLayoutComputer.pruneOffBranches(nodes, edges);
    }
}
