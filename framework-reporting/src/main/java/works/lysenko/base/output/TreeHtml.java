package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.output.tree.NodeEventClassifier;
import works.lysenko.base.output.tree.TreeHtmlRenderer;
import works.lysenko.base.output.tree.TreeLayoutComputer;
import works.lysenko.base.output.tree.TreeLayoutOptimizer;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.scenario._TreeNodeView;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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

    private TreeHtml() {}

    public static void setKnownIssuesStore(final KnownIssuesStore store) {
        KnownIssuesStore.setGlobalStore(store);
    }

    public static KnownIssuesStore getKnownIssuesStore() {
        return KnownIssuesStore.getGlobalStore();
    }

    public static void setExpectedResponsesStore(final works.lysenko.base.responses.ExpectedResponsesStore store) {
        works.lysenko.base.responses.ExpectedResponsesStore.setGlobalStore(store);
    }

    public static works.lysenko.base.responses.ExpectedResponsesStore getExpectedResponsesStore() {
        return works.lysenko.base.responses.ExpectedResponsesStore.getGlobalStore();
    }

    /**
     * The NodeData class.
     */
    public static final class NodeData implements _TreeNodeView {
        private final String id;
        private final String label;
        private final String group;
        private int col;
        private double row;
        private int rowOffset = 0;
        private final _Result result;
        private final _Scenario scenario;
        private final List<NodeData> children = new ArrayList<>();
        private final List<KnownIssue> knownIssues = new ArrayList<>();
        private NodeData parent;
        private NodeEventClassifier.Classification precomputedClassification;
        private Boolean precomputedChildFailure;
        private Boolean precomputedExecutable;

        public NodeData(final String id, final String label, final String group, final int col, final double row, final int rowOffset, final _Result result, final _Scenario scenario) {
            this.id = id;
            this.label = label;
            this.group = group;
            this.col = col;
            this.row = row;
            this.rowOffset = rowOffset;
            this.result = result;
            this.scenario = scenario;
        }

        public NodeData(final String id, final String label, final String group, final int col, final double row, final _Result result, final _Scenario scenario) {
            this(id, label, group, col, row, 0, result, scenario);
        }

        public NodeData(final String id, final String label, final String group, final int col, final double row, final _Result result) {
            this(id, label, group, col, row, 0, result, null);
        }

        public String id() { return id; }
        public String label() { return label; }
        public String group() { return group; }
        public int col() { return col; }
        public void setCol(final int col) { this.col = col; }
        public double row() { return row; }
        public void setRow(final double row) { this.row = row; }
        public int rowOffset() { return rowOffset; }
        public void setRowOffset(final int rowOffset) { this.rowOffset = rowOffset; }
        public _Result result() { return result; }
        public _Scenario scenario() { return scenario; }
        public List<NodeData> children() { return children; }
        public List<KnownIssue> knownIssues() { return knownIssues; }
        public NodeData parent() { return parent; }
        public void setParent(final NodeData parent) { this.parent = parent; }
        public NodeEventClassifier.Classification precomputedClassification() { return precomputedClassification; }
        public Boolean precomputedChildFailure() { return precomputedChildFailure; }
        public Boolean precomputedExecutable() { return precomputedExecutable; }

        void setPrecomputed(
                final NodeEventClassifier.Classification classification,
                final boolean childFailure,
                final boolean executable) {
            this.precomputedClassification = classification;
            this.precomputedChildFailure = childFailure;
            this.precomputedExecutable = executable;
        }
    }

    private record ResultSnapshot(
            ExecutionStatus status,
            ScenarioType scenarioType,
            Fraction configuredWeight,
            Fraction upstreamWeight,
            Fraction downstreamWeight,
            List<_LogRecord> events,
            int executions,
            int instances,
            int effectiveEventsCount) implements _Result {

        static _Result from(final _Result source, final int effectiveEventsCount) {
            if (null == source) return null;
            final List<_LogRecord> srcEvents = source.getEvents();
            final List<_LogRecord> frozenEvents = (null == srcEvents || srcEvents.isEmpty())
                    ? List.of() : List.copyOf(srcEvents);
            return new ResultSnapshot(
                    source.status(),
                    source.getScenarioType(),
                    source.getConfiguredWeight(),
                    source.getUpstreamWeight(),
                    source.getDownstreamWeight(),
                    frozenEvents,
                    source.getExecutions(),
                    source.getInstances(),
                    effectiveEventsCount);
        }

        @Override public void addEvent(final _LogRecord lr) { throw new UnsupportedOperationException("Immutable snapshot"); }
        @Override public Fraction getConfiguredWeight() { return configuredWeight; }
        @Override public Fraction getDownstreamWeight() { return downstreamWeight; }
        @Override public int getEffectiveEventsCount() { return effectiveEventsCount; }
        @Override public List<_LogRecord> getEvents() { return events; }
        @Override public int getExecutions() { return executions; }
        @Override public int getInstances() { return instances; }
        @Override public ScenarioType getScenarioType() { return scenarioType; }
        @Override public Fraction getUpstreamWeight() { return upstreamWeight; }
        @Override public void updateStatus(final ExecutionStatus status) { throw new UnsupportedOperationException("Immutable snapshot"); }
        @Override public void tick() { throw new UnsupportedOperationException("Immutable snapshot"); }
    }

    /**
     * Captures a thread-safe, immutable snapshot of {@code layout} decoupled from live
     * mutable {@link _Result} and {@link _Scenario} instances so SVG rendering and
     * sonification can execute safely off the main test thread.
     *
     * @param layout live computed layout
     * @param ctx    accessible scenario context captured at the same instant
     * @return immutable layout snapshot with precomputed classifications and statuses
     */
    public static TreeLayout snapshotLayout(final TreeLayout layout, final AccessibleContext ctx) {
        if (null == layout || layout.nodes().isEmpty()) {
            return new TreeLayout(List.of(), List.of());
        }
        final AccessibleContext safeCtx = (null != ctx) ? ctx : new AccessibleContext(Set.of());
        final Map<NodeData, Boolean> liveChildFailureCache = new HashMap<>(layout.nodes().size() * 2);
        final Map<NodeData, Boolean> liveExecutableCache = new HashMap<>(layout.nodes().size() * 2);
        final Map<NodeData, NodeData> copyMap = new IdentityHashMap<>(layout.nodes().size() * 2);
        final List<NodeData> snapshotNodes = new ArrayList<>(layout.nodes().size());

        for (final NodeData orig : layout.nodes()) {
            final NodeEventClassifier.Classification cls = NodeEventClassifier.classify(orig);
            final boolean childFailure = hasChildFailure(orig, liveChildFailureCache);
            final boolean executable = isTheoreticallyExecutable(orig, safeCtx, liveExecutableCache);
            final _Result frozenResult = ResultSnapshot.from(orig.result(), cls.unclassifiedEventCount());
            final NodeData copy = new NodeData(
                    orig.id(),
                    orig.label(),
                    orig.group(),
                    orig.col(),
                    orig.row(),
                    orig.rowOffset(),
                    frozenResult,
                    null);
            if (!orig.knownIssues().isEmpty()) {
                copy.knownIssues().addAll(orig.knownIssues());
            }
            copy.setPrecomputed(cls, childFailure, executable);
            copyMap.put(orig, copy);
            snapshotNodes.add(copy);
        }

        for (final NodeData orig : layout.nodes()) {
            final NodeData copy = copyMap.get(orig);
            if (null != orig.parent()) {
                copy.setParent(copyMap.get(orig.parent()));
            }
            for (final NodeData child : orig.children()) {
                final NodeData copyChild = copyMap.get(child);
                if (null != copyChild) copy.children().add(copyChild);
            }
        }

        final List<Edge> snapshotEdges = new ArrayList<>(layout.edges().size());
        for (final Edge edge : layout.edges()) {
            final NodeData from = copyMap.get(edge.from());
            final NodeData to = copyMap.get(edge.to());
            if (null != from && null != to) {
                snapshotEdges.add(new Edge(from, to));
            }
        }
        return new TreeLayout(snapshotNodes, snapshotEdges);
    }

    public record Edge(NodeData from, NodeData to) {}

    public record TreeLayout(List<NodeData> nodes, List<Edge> edges, SummaryPlaque plaque) {
        public TreeLayout(List<NodeData> nodes, List<Edge> edges) {
            this(nodes, edges, null);
        }
    }

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
        return NodeEventClassifier.classify(n).hasFailure();
    }

    public static int getUnclassifiedEventCount(final NodeData n) {
        return NodeEventClassifier.classify(n).unclassifiedEventCount();
    }

    public static boolean hasWarning(final NodeData n) {
        return NodeEventClassifier.classify(n).hasWarning();
    }

    public static boolean hasNotice(final NodeData n) {
        return NodeEventClassifier.classify(n).hasNotice();
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
