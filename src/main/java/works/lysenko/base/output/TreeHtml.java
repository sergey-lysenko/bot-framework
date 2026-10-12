package works.lysenko.base.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.output.svg.Groups;
import works.lysenko.base.output.svg.Parts;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Node;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.prop.tree.Optimization;
import org.apache.commons.math3.fraction.Fraction;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Files.writeToFile;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_TREE_HTML_;
import static works.lysenko.util.spec.Numbers.ZERO;

@SuppressWarnings({"UtilityClass", "ForeachStatement", "NestedMethodCall", "MethodWithMultipleLoops", "ClassWithoutLogger"})
public final class TreeHtml {

    private static final int COL_WIDTH = 260;
    private static final int ROW_HEIGHT = 56;
    private static final int CARD_WIDTH = 220;
    private static final int CARD_HEIGHT = 44;


    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static KnownIssuesStore knownIssuesStore = null;

    private TreeHtml() {}

    public static void setKnownIssuesStore(final KnownIssuesStore store) {
        knownIssuesStore = store;
    }

    private static KnownIssuesStore getKnownIssuesStore() {
        if (null == knownIssuesStore) {
            knownIssuesStore = new KnownIssuesStore();
        }
        return knownIssuesStore;
    }

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

        NodeData(final String id, final String label, final String group, final int col, final double row, final _Result result, final _Scenario scenario) {
            this.id = id;
            this.label = label;
            this.group = group;
            this.col = col;
            this.row = row;
            this.result = result;
            this.scenario = scenario;
        }

        NodeData(final String id, final String label, final String group, final int col, final double row, final _Result result) {
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

    public static TreeLayout computeLayout(final TreeMap<String, Result> sorted) {

        if (null != core) {
            final Set<_Scenario> roots = rootsOf(core.getRootScenarios());
            if (null != roots && !roots.isEmpty()) return computeLayout(roots, sorted);
        }
        final List<NodeData> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();
        final int[] dy = new int[100];
        Arrays.fill(dy, 1);

        final List<_LogRecord> logRecords = (!works.lysenko.util.prop.issues.KnownIssues.failedOnly && isNotNull(exec) && isNotNull(exec.issues()))
                ? exec.issues().latest() : Collections.emptyList();

        final SortedMap<String, TreeMap<String, Result>> t = new Groups(TESTS, sorted);
        for (final Map.Entry<String, TreeMap<String, Result>> group : t.entrySet()) {
            processGroup(group, null, 0, dy, nodes, edges, logRecords);
        }

        pruneOffBranches(nodes, edges);
        optimizeLayout(nodes);
        return new TreeLayout(nodes, edges);
    }

    static Set<_Scenario> rootsOf(final Iterable<? extends _Scenario> scenarios) {

        final Set<_Scenario> roots = Collections.newSetFromMap(new IdentityHashMap<>());
        if (null == scenarios) return roots;
        for (final _Scenario scenario : scenarios)
            if (null != scenario) roots.add(scenario);
        for (final _Scenario scenario : roots.toArray(_Scenario[]::new))
            if (scenario instanceof _Node node)
                for (final _Scenario child : node.getPool().getPairList().stream().map(pair -> pair.k()).toList())
                    roots.remove(child);
        return roots;
    }

    static TreeLayout computeLayout(final Iterable<? extends _Scenario> roots, final TreeMap<String, Result> sorted) {

        final List<NodeData> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();
        final int[] rows = {1};
        final Set<_Scenario> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<_Scenario> sortedRoots = new ArrayList<>();
        if (null != roots) {
            for (final _Scenario r : roots) {
                if (null != r) sortedRoots.add(r);
            }
            sortedRoots.sort(Comparator.comparing(_Scenario::getSimpleName).thenComparing(_Scenario::getName));
        }

        final List<_LogRecord> logRecords = (!works.lysenko.util.prop.issues.KnownIssues.failedOnly && isNotNull(exec) && isNotNull(exec.issues()))
                ? exec.issues().latest() : Collections.emptyList();

        for (final _Scenario root : sortedRoots)
            processScenario(root, null, 0, rows, ancestors, sorted, nodes, edges, logRecords);
        pruneOffBranches(nodes, edges);
        optimizeLayout(nodes);
        return new TreeLayout(nodes, edges);
    }

    private static void processScenario(final _Scenario scenario, final NodeData parent, final int col, final int[] rows,
                                        final Set<_Scenario> ancestors, final TreeMap<String, Result> sorted,
                                        final List<NodeData> nodes, final List<Edge> edges,
                                        final List<_LogRecord> logRecords) {

        if (null == scenario || !ancestors.add(scenario)) return;
        final String label = b(scenario.getSimpleName(), scenario.type().tag());
        final String id = s("scenario_", nodes.size(), "_", scenario.getSimpleName());
        final String group = (null == parent) ? TESTS : parent.label();
        final NodeData node = new NodeData(id, label, group, col, rows[0]++, resultFor(scenario, sorted), scenario);

        final KnownIssuesStore store = getKnownIssuesStore();
        if (null != store) {
            final List<KnownIssue> matchedByNode = store.getForNode(scenario, node.result());
            if (null != matchedByNode && !matchedByNode.isEmpty()) {
                for (final KnownIssue ki : matchedByNode) {
                    if (!node.knownIssues().contains(ki)) node.knownIssues().add(ki);
                }
            }
            if (!works.lysenko.util.prop.issues.KnownIssues.failedOnly && null != logRecords && !logRecords.isEmpty()) {
                for (final KnownIssue ki : store.getIssues()) {
                    if (node.knownIssues().contains(ki)) continue;
                    final String scenarioTarget = ki.scenario();
                    final boolean scenarioMatches = (null == scenarioTarget || scenarioTarget.isBlank())
                            || works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(scenarioTarget, scenario.getName(), scenario.getSimpleName());
                    if (scenarioMatches) {
                        for (final _LogRecord lr : logRecords) {
                            if (works.lysenko.base.issues.KnownIssuesMatcher.matchesPatternForRecord(ki.pattern(), lr)) {
                                node.knownIssues().add(ki);
                                break;
                            }
                        }
                    }
                }
            }
        }

        nodes.add(node);
        if (null != parent) {
            node.setParent(parent);
            parent.children().add(node);
            edges.add(new Edge(parent, node));
        }
        if (scenario instanceof _Node parentNode) {
            final List<works.lysenko.util.data.records.KeyValue<_Scenario, org.apache.commons.math3.fraction.Fraction>> pairs =
                    new ArrayList<>(parentNode.getPool().getPairList());
            pairs.sort(Comparator.comparing((works.lysenko.util.data.records.KeyValue<_Scenario, org.apache.commons.math3.fraction.Fraction> p) -> p.k().getSimpleName())
                    .thenComparing(p -> p.k().getName()));
            for (final var child : pairs)
                processScenario(child.k(), node, col + 1, rows, ancestors, sorted, nodes, edges, logRecords);
        }
        ancestors.remove(scenario);
    }

    public static boolean hasFailure(final NodeData n) {

        if (null == n) return false;
        if (null != n.scenario()) {
            if (n.scenario().hasFailed() || n.scenario().getExecutionStatus() == works.lysenko.util.data.enums.ExecutionStatus.FAILED) return true;
        }
        final _Result res = n.result();
        if (null != res) {
            return res.status() == works.lysenko.util.data.enums.ExecutionStatus.FAILED;
        }
        return false;
    }

    public static boolean hasChildFailure(final NodeData n) {

        if (null == n) return false;
        if (null != n.scenario()) {
            if (n.scenario().hasChildFailed() || n.scenario().getExecutionStatus() == works.lysenko.util.data.enums.ExecutionStatus.CHILD_FAILED) return true;
        }
        final _Result res = n.result();
        if (null != res) {
            if (res.status() == works.lysenko.util.data.enums.ExecutionStatus.CHILD_FAILED) return true;
        }
        if (null != n.children() && !n.children().isEmpty()) {
            for (final NodeData child : n.children()) {
                if (hasFailure(child) || hasChildFailure(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines whether a scenario node is theoretically executable in the current run configuration.
     *
     * @param n                   node to evaluate
     * @param accessibleScenarios set of scenarios accessible in the current session
     * @return true if theoretically executable, false if excluded by configuration or saturation
     */
    public static boolean isTheoreticallyExecutable(final NodeData n, final Set<_Scenario> accessibleScenarios) {
        if (null == n) {
            return false;
        }
        if (null != n.parent()) {
            final boolean parentIsStructural = null == n.parent().scenario() && null == n.parent().result();
            final boolean parentWasExecuted = null != n.parent().result() && n.parent().result().getExecutions() > 0;
            if (!parentIsStructural && !parentWasExecuted && !isTheoreticallyExecutable(n.parent(), accessibleScenarios)) {
                return false;
            }
        }
        if (null != n.result() && n.result().getExecutions() > 0) {
            return true;
        }
        if (null != n.scenario()) {
            final _Scenario sc = n.scenario();
            if (sc.hasFailed() || sc.isExhausted()) {
                return false;
            }
            final double cw = (sc.weightConfigured() != null) ? sc.weightConfigured().doubleValue() :
                              ((sc.weightCoded() != null) ? sc.weightCoded().doubleValue() : 1.0);
            final double dw = (sc.weightDownstream() != null) ? sc.weightDownstream().doubleValue() : 0.0;
            final double uw = (sc.weightUpstream() != null) ? sc.weightUpstream().doubleValue() : 0.0;
            if (cw <= 0.0 && dw <= 0.0 && uw <= 0.0) {
                return false;
            }
        }
        if (null != accessibleScenarios && !accessibleScenarios.isEmpty()) {
            if (null != n.scenario()) {
                final _Scenario sc = n.scenario();
                return accessibleScenarios.contains(sc)
                        || accessibleScenarios.stream().anyMatch(s ->
                                s.getName().equals(sc.getName())
                                || s.getSimpleName().equals(sc.getSimpleName())
                                || s.getShortName().equals(sc.getShortName())
                                || s.getClass().equals(sc.getClass().getSuperclass())
                                || sc.getClass().equals(s.getClass().getSuperclass()));
            }
            final String label = n.label();
            return accessibleScenarios.stream().anyMatch(s -> {
                final String tag = (null != s.type()) ? s.type().tag() : "";
                return s.getSimpleName().equals(label)
                        || s.getShortName().equals(label)
                        || s.getName().equals(label)
                        || b(s.getSimpleName(), tag).equals(label)
                        || b(s.getShortName(), tag).equals(label)
                        || b(s.getName(), tag).equals(label);
            });
        }
        if (null != n.scenario()) {
            final _Scenario s = n.scenario();
            return s.calculateCombinations(false) > 0;
        }
        if (null != n.result()) {
            final Fraction cw = n.result().getConfiguredWeight();
            if (null != cw) {
                final double cwVal = cw.doubleValue();
                final double dwVal = (null != n.result().getDownstreamWeight()) ? n.result().getDownstreamWeight().doubleValue() : 0.0;
                final double uwVal = (null != n.result().getUpstreamWeight()) ? n.result().getUpstreamWeight().doubleValue() : 0.0;
                if (cwVal <= 0.0 && dwVal <= 0.0 && uwVal <= 0.0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines whether a scenario node is theoretically executable using the active framework core.
     *
     * @param n node to evaluate
     * @return true if theoretically executable
     */
    public static boolean isTheoreticallyExecutable(final NodeData n) {
        return isTheoreticallyExecutable(n, getAccessibleScenarios());
    }

    static Set<_Scenario> getAccessibleScenarios() {
        if (isNotNull(core)) {
            try {
                final Set<_Scenario> accessible = core.getAccessibleScenarios();
                if (isNotNull(accessible) && !accessible.isEmpty()) {
                    return accessible;
                }
            } catch (final Throwable ignored) {
                // Ignore any core access issues
            }
        }
        return Collections.emptySet();
    }

    private static Result resultFor(final _Scenario scenario, final TreeMap<String, Result> sorted) {

        if (null != core && null != core.getResults()) {
            final Result result = core.getResults().getResult(scenario);
            if (null != result) return result;
        }
        final Result result = sorted.get(b(scenario.getShortName(), scenario.type().tag()));
        return (null != result) ? result : new Result(scenario);
    }

    public static void treeStats() {
        try {
            final TreeMap<String, Result> sorted = core.getResults().getSortedStrings(false);
            final TreeLayout layout = computeLayout(sorted);
            final String html = renderHtml(layout.nodes(), layout.edges());
            final String fileName = name(RUN_TREE_HTML_);
            writeToFile(html, fileName);
        } catch (final Throwable t) {
            logEvent(S2, s("Failed to generate tree.html: ", t.getMessage()));
        }
    }

    private static void processGroup(final Map.Entry<String, ? extends TreeMap<String, Result>> group,
                                     final NodeData parentNode,
                                     final int col, final int[] dy,
                                     final List<NodeData> nodes, final List<Edge> edges,
                                     final List<_LogRecord> logRecords) {
        final Map<String, NodeData> myNodes = new HashMap<>();
        final Parts parts = Parts.process(group.getValue());

        for (final Map.Entry<String, Result> entry : parts.head.entrySet()) {
            final int myRow = dy[col]++;
            final String rawKey = entry.getKey();
            final String shortKey = rawKey.split(SPACE)[ZERO];
            final String nodeId = s("col_", col, "_row_", myRow, "_", shortKey.replace('.', '_'));
            final NodeData nd = new NodeData(nodeId, rawKey, group.getKey(), col, myRow, entry.getValue());

            final KnownIssuesStore store = getKnownIssuesStore();
            if (null != store) {
                if (isNotNull(nd.result())) {
                    for (final KnownIssue ki : store.getIssues()) {
                        if (works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), rawKey, shortKey)) {
                            if (works.lysenko.base.issues.KnownIssuesMatcher.matchesPattern(ki.pattern(), nd.result())) {
                                if (!nd.knownIssues().contains(ki)) nd.knownIssues().add(ki);
                            }
                        }
                    }
                }
                if (!works.lysenko.util.prop.issues.KnownIssues.failedOnly && null != logRecords && !logRecords.isEmpty()) {
                    for (final KnownIssue ki : store.getIssues()) {
                        if (nd.knownIssues().contains(ki)) continue;
                        if (works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), rawKey, shortKey)) {
                            for (final _LogRecord lr : logRecords) {
                                if (works.lysenko.base.issues.KnownIssuesMatcher.matchesPatternForRecord(ki.pattern(), lr)) {
                                    nd.knownIssues().add(ki);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            nodes.add(nd);
            myNodes.put(shortKey.toLowerCase(), nd);

            if (null != parentNode) {
                nd.setParent(parentNode);
                edges.add(new Edge(parentNode, nd));
                parentNode.children().add(nd);
            }
        }

        final Groups groups = Groups.process(parts.body);
        for (final Map.Entry<String, TreeMap<String, Result>> subgroup : groups.entrySet()) {
            final String subKey = subgroup.getKey().toLowerCase();
            final NodeData subParent = myNodes.get(subKey);
            final NodeData nextParent = (null != subParent) ? subParent : parentNode;
            processGroup(subgroup, nextParent, col + 1, dy, nodes, edges, logRecords);
        }
    }

    private static double columnWeight(final int col) {
        return Math.pow(2.0, Math.max(0, col));
    }

    private static void packLeafChildren(final List<NodeData> nodes) {
        if (null == nodes || nodes.isEmpty()) return;

        for (final NodeData parent : nodes) {
            final List<NodeData> allChildren = parent.children();
            if (allChildren.size() >= 6) {
                final boolean allAreLeaves = allChildren.stream().allMatch(c -> c.children().isEmpty());
                if (allAreLeaves) {
                    final int perCol = (int) Math.ceil(allChildren.size() / 2.0);
                    for (int i = 0; i < allChildren.size(); i++) {
                        final NodeData leaf = allChildren.get(i);
                        final int extraCol = i / perCol;
                        leaf.setCol(parent.col() + 1 + extraCol);
                    }
                }
            }
        }
    }

    static void optimizeLayout(final List<NodeData> nodes) {
        if (nodes.isEmpty()) return;

        if (Optimization.enabled) {
            packLeafChildren(nodes);
        }

        final Map<Integer, List<NodeData>> colMap = new TreeMap<>();
        int maxCol = 0;
        for (final NodeData n : nodes) {
            colMap.computeIfAbsent(n.col(), k -> new ArrayList<>()).add(n);
            if (n.col() > maxCol) maxCol = n.col();
        }

        for (int c = 0; c <= maxCol; c++) {
            final List<NodeData> colNodes = colMap.get(c);
            if (null == colNodes || colNodes.isEmpty()) continue;
            colNodes.sort((n1, n2) -> {
                final double p1 = (null != n1.parent()) ? n1.parent().row() : n1.row();
                final double p2 = (null != n2.parent()) ? n2.parent().row() : n2.row();
                final int pCmp = Double.compare(p1, p2);
                if (pCmp != 0) return pCmp;
                final int lCmp = n1.label().compareTo(n2.label());
                if (lCmp != 0) return lCmp;
                return n1.id().compareTo(n2.id());
            });
            final double[] targets = new double[colNodes.size()];
            for (int i = 0; i < colNodes.size(); i++) {
                final NodeData n = colNodes.get(i);
                targets[i] = (null != n.parent()) ? n.parent().row() : (i + 1.0);
            }
            final double[] resolved = solveColumn1D(targets);
            for (int i = 0; i < colNodes.size(); i++) {
                colNodes.get(i).setRow(resolved[i]);
            }
        }

        final int iterations = 32;
        for (int iter = 0; iter < iterations; iter++) {
            final boolean backward = (1 == iter % 2);
            final int startCol = backward ? maxCol : 0;
            final int endCol = backward ? 0 : maxCol;
            final int step = backward ? -1 : 1;

            for (int c = startCol; backward ? c >= endCol : c <= endCol; c += step) {
                final List<NodeData> colNodes = colMap.get(c);
                if (null == colNodes || colNodes.isEmpty()) continue;

                final Map<NodeData, Double> parentRowMap = new HashMap<>();
                final Map<NodeData, Double> childrenRowMap = new HashMap<>();
                final Map<NodeData, Double> targetMap = new HashMap<>();

                for (final NodeData n : colNodes) {
                    double parentSum = 0.0;
                    double parentWeight = 0.0;
                    if (null != n.parent()) {
                        final double wP = columnWeight(c - 1);
                        parentSum += n.parent().row() * wP;
                        parentWeight += wP;
                    }

                    double childSum = 0.0;
                    double childWeight = 0.0;
                    final double wC = columnWeight(c);
                    for (final NodeData child : n.children()) {
                        if (!child.children().isEmpty()) {
                            childSum += child.row() * wC;
                            childWeight += wC;
                        }
                    }

                    if (parentWeight > 0.0) {
                        parentRowMap.put(n, parentSum / parentWeight);
                    }
                    if (childWeight > 0.0) {
                        childrenRowMap.put(n, childSum / childWeight);
                    }

                    final double target;
                    if (parentWeight > 0.0 && childWeight > 0.0) {
                        target = (parentSum + childSum) / (parentWeight + childWeight);
                    } else if (parentWeight > 0.0) {
                        target = parentSum / parentWeight;
                    } else if (childWeight > 0.0) {
                        target = childSum / childWeight;
                    } else {
                        target = n.row();
                    }
                    targetMap.put(n, target);
                }

                interpolateSiblingTargets(colNodes, parentRowMap, childrenRowMap, targetMap);

                colNodes.sort((n1, n2) -> {
                    final double p1 = parentRowMap.containsKey(n1) ? parentRowMap.get(n1) : n1.row();
                    final double p2 = parentRowMap.containsKey(n2) ? parentRowMap.get(n2) : n2.row();
                    final int pCmp = Double.compare(p1, p2);
                    if (pCmp != 0) return pCmp;

                    final double c1 = childrenRowMap.containsKey(n1) ? childrenRowMap.get(n1) : n1.row();
                    final double c2 = childrenRowMap.containsKey(n2) ? childrenRowMap.get(n2) : n2.row();
                    final int cCmp = Double.compare(c1, c2);
                    if (cCmp != 0) return cCmp;

                    final int tCmp = Double.compare(targetMap.get(n1), targetMap.get(n2));
                    if (tCmp != 0) return tCmp;

                    final int lCmp = n1.label().compareTo(n2.label());
                    if (lCmp != 0) return lCmp;

                    return n1.id().compareTo(n2.id());
                });

                final double[] targets = new double[colNodes.size()];
                for (int i = 0; i < colNodes.size(); i++) {
                    targets[i] = targetMap.get(colNodes.get(i));
                }

                final double[] resolved = solveColumn1D(targets);
                for (int i = 0; i < colNodes.size(); i++) {
                    colNodes.get(i).setRow(resolved[i]);
                }
            }
        }

        double minRow = Double.MAX_VALUE;
        for (final NodeData n : nodes) {
            if (n.row() < minRow) minRow = n.row();
        }
        if (minRow < Double.MAX_VALUE && minRow > 1.0) {
            final double shift = minRow - 1.0;
            for (final NodeData n : nodes) {
                n.setRow(n.row() - shift);
            }
        }
        compactDisconnectedTrees(nodes);
    }

    private static void interpolateSiblingTargets(
            final List<NodeData> colNodes,
            final Map<NodeData, Double> parentRowMap,
            final Map<NodeData, Double> childrenRowMap,
            final Map<NodeData, Double> targetMap) {

        final Map<NodeData, List<NodeData>> parentGroups = new LinkedHashMap<>();
        for (final NodeData n : colNodes) {
            parentGroups.computeIfAbsent(n.parent(), k -> new ArrayList<>()).add(n);
        }

        for (final List<NodeData> siblings : parentGroups.values()) {
            if (siblings.size() <= 1) continue;

            final List<Integer> fixedIndices = new ArrayList<>();
            for (int i = 0; i < siblings.size(); i++) {
                if (childrenRowMap.containsKey(siblings.get(i))) {
                    fixedIndices.add(i);
                }
            }

            if (fixedIndices.isEmpty()) continue;

            for (int k = 0; k < siblings.size(); k++) {
                final NodeData s = siblings.get(k);
                if (childrenRowMap.containsKey(s)) continue;

                int leftIdx = -1;
                for (final int f : fixedIndices) {
                    if (f < k) leftIdx = f;
                    else break;
                }

                int rightIdx = -1;
                for (final int f : fixedIndices) {
                    if (f > k) { rightIdx = f; break; }
                }

                final double parentRow = parentRowMap.getOrDefault(s, s.row());

                if (leftIdx != -1 && rightIdx != -1) {
                    final double leftTarget = targetMap.get(siblings.get(leftIdx));
                    final double rightTarget = targetMap.get(siblings.get(rightIdx));
                    final double ratio = (double) (k - leftIdx) / (rightIdx - leftIdx);
                    targetMap.put(s, leftTarget + ratio * (rightTarget - leftTarget));
                } else if (leftIdx != -1) {
                    final double leftTarget = targetMap.get(siblings.get(leftIdx));
                    targetMap.put(s, leftTarget + (k - leftIdx) * 1.0);
                } else if (rightIdx != -1) {
                    final double rightTarget = targetMap.get(siblings.get(rightIdx));
                    targetMap.put(s, Math.max(parentRow, rightTarget - (rightIdx - k) * 1.0));
                }
            }
        }
    }

    static void compactDisconnectedTrees(final List<NodeData> nodes) {

        final Map<NodeData, List<NodeData>> components = new IdentityHashMap<>();
        for (final NodeData node : nodes)
            components.computeIfAbsent(rootOf(node), ignored -> new ArrayList<>()).add(node);

        final List<List<NodeData>> ordered = new ArrayList<>(components.values());
        ordered.sort((c1, c2) -> {
            final int mCmp = Double.compare(minimumRow(c1), minimumRow(c2));
            if (mCmp != 0) return mCmp;
            final String l1 = c1.isEmpty() ? "" : c1.get(0).label();
            final String l2 = c2.isEmpty() ? "" : c2.get(0).label();
            final int lCmp = l1.compareTo(l2);
            if (lCmp != 0) return lCmp;
            final String id1 = c1.isEmpty() ? "" : c1.get(0).id();
            final String id2 = c2.isEmpty() ? "" : c2.get(0).id();
            return id1.compareTo(id2);
        });

        double nextRow = 1.0;
        for (final List<NodeData> component : ordered) {
            final double minimum = minimumRow(component);
            final double shift = nextRow - minimum;
            if (0.0 != shift)
                for (final NodeData node : component)
                    node.setRow(node.row() + shift);
            nextRow = maximumRow(component) + 1.0;
        }
    }

    private static NodeData rootOf(final NodeData node) {

        NodeData root = node;
        while (null != root.parent()) root = root.parent();
        return root;
    }

    private static double minimumRow(final List<NodeData> nodes) {

        return nodes.stream().mapToDouble(NodeData::row).min().orElse(1.0);
    }

    private static double maximumRow(final List<NodeData> nodes) {

        return nodes.stream().mapToDouble(NodeData::row).max().orElse(1.0);
    }

    static double[] solveColumn1D(final double[] targets) {
        final int m = targets.length;
        if (0 == m) return new double[0];

        final class Block {
            double sum;
            int count;
            Block(final double sum, final int count) {
                this.sum = sum;
                this.count = count;
            }
            double avg() {
                return sum / count;
            }
        }

        final List<Block> blocks = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            final double z = targets[i] - i;
            blocks.add(new Block(z, 1));
            while (blocks.size() > 1) {
                final Block b2 = blocks.get(blocks.size() - 1);
                final Block b1 = blocks.get(blocks.size() - 2);
                if (b1.avg() > b2.avg()) {
                    b1.sum += b2.sum;
                    b1.count += b2.count;
                    blocks.remove(blocks.size() - 1);
                } else {
                    break;
                }
            }
        }

        final double[] w = new double[m];
        int idx = 0;
        for (final Block b : blocks) {
            final double avg = b.avg();
            for (int i = 0; i < b.count; i++) {
                w[idx++] = avg;
            }
        }

        if (w[0] < 1.0) {
            final double shift = 1.0 - w[0];
            for (int i = 0; i < m; i++) {
                w[i] += shift;
            }
        }

        final double[] y = new double[m];
        for (int i = 0; i < m; i++) {
            y[i] = w[i] + i;
        }
        return y;
    }

    private static String renderKnownIssuesHtml(final List<KnownIssue> issues) {
        if (null == issues || issues.isEmpty()) return "";
        final StringBuilder sb = new StringBuilder();
        for (final KnownIssue ki : issues) {
            sb.append("<div style=\"margin-bottom:8px;\">");
            if (isNotNull(ki.title()) && !ki.title().isBlank()) {
                sb.append("<div style=\"font-weight:600; color:#f8fafc;\">").append(escapeHtml(ki.title())).append("</div>");
            }
            if (isNotNull(ki.description()) && !ki.description().isBlank()) {
                sb.append("<div style=\"color:#94a3b8; font-size:11px; margin-top:2px;\">").append(escapeHtml(ki.description())).append("</div>");
            }
            if (isNotNull(ki.link()) && !ki.link().isBlank()) {
                final String safeLink = escapeHtml(ki.link());
                sb.append("<div style=\"margin-top:4px;\"><a href=\"").append(safeLink).append("\" target=\"_blank\" style=\"color:#38bdf8; text-decoration:underline; font-size:11px;\">").append(safeLink).append("</a></div>");
            }
            sb.append("</div>");
        }
        return sb.toString();
    }

    private static String escapeHtml(final String text) {
        if (null == text) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    static String renderHtml(final List<NodeData> nodes, final List<Edge> edges) {
        final StringBuilder svgContent = new StringBuilder();

        final List<Edge> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort(Comparator.comparingInt(e -> {
            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            return (toExecs > 0 && fromExecs > 0) ? 1 : 0;
        }));

        for (final Edge e : sortedEdges) {
            final int startX = e.from().col() * COL_WIDTH + CARD_WIDTH + 40;
            final int startY = (int) Math.round(e.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + 20);
            final int endX = e.to().col() * COL_WIDTH + 40;
            final int endY = (int) Math.round(e.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + 20);
            final int cX = (startX + endX) / 2;

            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            final int toEvents = (null != e.to().result() && null != e.to().result().getEvents()) ? e.to().result().getEvents().size() : 0;
            final boolean toFailure = hasFailure(e.to());
            final boolean toChildFailure = hasChildFailure(e.to());
            final boolean toTheoreticallyExecutable = isTheoreticallyExecutable(e.to());

            final String edgeClass;
            if (toExecs > 0 && fromExecs > 0) {
                if (toFailure) {
                    edgeClass = "connector failed";
                } else if (toChildFailure) {
                    edgeClass = "connector child_failed";
                } else if (toEvents > 0) {
                    edgeClass = "connector warning";
                } else {
                    edgeClass = "connector visited";
                }
            } else if (!toTheoreticallyExecutable) {
                edgeClass = "connector non_executable";
            } else {
                edgeClass = "connector";
            }

            svgContent.append(String.format(
                    Locale.US,
                    "<path class=\"%s\" data-from=\"%s\" data-to=\"%s\" d=\"M %d %d C %d %d, %d %d, %d %d\" />\n",
                    edgeClass, e.from().id(), e.to().id(), startX, startY, cX, startY, cX, endY, endX, endY
            ));
        }

        final Map<String, Map<String, Object>> nodeDataMap = new LinkedHashMap<>();

        final boolean isAllLeafs = (null != works.lysenko.Base.parameters)
                ? works.lysenko.Base.parameters.isAllLeafs()
                : (null != works.lysenko.util.spec.PropEnum._TEST_ALL_LEAFS.get()
                   && Boolean.TRUE.equals(works.lysenko.util.spec.PropEnum._TEST_ALL_LEAFS.get()));

        final int target = (null != works.lysenko.Base.parameters)
                ? works.lysenko.Base.parameters.getAllLeafsCount()
                : (null != works.lysenko.util.spec.PropEnum._TEST_ALL_LEAFS_COUNT.get()
                   ? Math.max(1, works.lysenko.util.spec.PropEnum._TEST_ALL_LEAFS_COUNT.get()) : 1);

        double maxLeafOverRatio = 0.0;
        for (final NodeData n : nodes) {
            if (n.children().isEmpty()) {
                final int scenarioTarget = Math.max(1, target);
                final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
                if (execs > scenarioTarget) {
                    maxLeafOverRatio = Math.max(maxLeafOverRatio, (double) (execs - scenarioTarget) / scenarioTarget);
                }
            }
        }

        final works.lysenko.base.output.tree.VisualNodeResolver nodeResolver = 
                new works.lysenko.base.output.tree.VisualNodeResolver(works.lysenko.base.output.tree.TreeTheme.DEFAULT, getAccessibleScenarios(), isAllLeafs, target, maxLeafOverRatio);
        final List<works.lysenko.base.output.tree.VisualNode> visualNodes = new ArrayList<>();

        for (final NodeData n : nodes) {
            final _Result res = n.result();
            final int execs = (null != res) ? res.getExecutions() : 0;
            final int eventCount = (null != res && null != res.getEvents()) ? res.getEvents().size() : 0;
            
            final works.lysenko.base.output.tree.VisualNode visualNode = nodeResolver.resolve(n);
            visualNodes.add(visualNode);

            final StringBuilder eventDetails = new StringBuilder();
            if (null != res && null != res.getEvents()) {
                for (final _LogRecord lr : res.getEvents()) {
                    eventDetails.append(lr.toString()).append("\n");
                }
            }

            final String kiHtml = renderKnownIssuesHtml(n.knownIssues());
            final String safeLabel = n.label().replace("<", "&lt;").replace(">", "&gt;");

            final Map<String, Object> nodeInfo = new LinkedHashMap<>();
            nodeInfo.put("name", safeLabel);
            nodeInfo.put("group", n.group());
            nodeInfo.put("status", visualNode.badgeText() + (visualNode.hasKnownIssues() ? " (Known Issue)" : ""));
            nodeInfo.put("exec", execs);
            nodeInfo.put("events", eventCount);
            nodeInfo.put("details", eventDetails.toString());
            nodeInfo.put("kiHtml", kiHtml);
            nodeDataMap.put(n.id(), nodeInfo);
        }
        
        svgContent.append(new works.lysenko.base.output.tree.HtmlTreeRenderer(works.lysenko.base.output.tree.TreeTheme.DEFAULT).renderNodes(visualNodes));

        final SummaryPlaque summaryPlaque = SummaryPlaque.computeFromNodes(nodes);

        String nodeDataJson = "{}";
        try {
            nodeDataJson = MAPPER.writeValueAsString(nodeDataMap);
        } catch (final Exception ignored) {
        }

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Scenario Execution Tree</title>\n" +
                "<style>\n" +
                "  :root { --bg: #0f172a; --panel-bg: #1e293b; --border: #334155; --text: #f8fafc; --text-muted: #94a3b8; --accent: #38bdf8; }\n" +
                "  * { box-sizing: border-box; margin: 0; padding: 0; }  body { background: var(--bg); color: var(--text); font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; overflow: hidden; width: 100vw; height: 100vh; display: flex; flex-direction: column; }\n" +
                "  header { background: var(--panel-bg); border-bottom: 1px solid var(--border); padding: 12px 20px; display: flex; justify-content: space-between; align-items: center; z-index: 10; }\n" +
                "  h1 { font-size: 16px; font-weight: 600; display: flex; align-items: center; gap: 8px; }\n" +
                "  .badge { background: #0284c7; color: white; font-size: 11px; padding: 2px 8px; border-radius: 12px; font-weight: 500; }\n" +
                "  .controls { display: flex; gap: 8px; align-items: center; }\n" +
                "  button { background: #334155; color: var(--text); border: 1px solid #475569; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; }\n" +
                "  button:hover { background: #475569; }\n" +
                "  #canvas-container { flex: 1; position: relative; cursor: grab; user-select: none; background-image: radial-gradient(circle, #1e293b 1px, transparent 1px); background-size: 24px 24px; }\n" +
                "  #canvas-container:active { cursor: grabbing; }\n" +
                "  svg { width: 100%; height: 100%; position: absolute; top: 0; left: 0; }\n" +
                "  .node-card { cursor: pointer; transition: filter 0.15s; }\n" +
                "  .node-card:hover { filter: drop-shadow(0 0 10px rgba(56, 189, 248, 0.4)); }\n" +
                "  .node-card rect.card-bg { rx: 8; ry: 8; stroke-width: 1.5; }\n" +
                "  .node-card.child_failed rect.card-bg { fill: #1e1b4b; stroke: #818cf8; stroke-width: 1.2; stroke-dasharray: 3 3; }\n" +
                "  .node-card.non_executable rect.card-bg { fill: #131a2a; stroke: #283548; stroke-width: 1.2; stroke-dasharray: 4 4; }\n" +
                "  .node-card.non_executable text { fill: #475569; }\n" +
                "  .node-card.non_executable circle { stroke: #283548; stroke-width: 1.2; stroke-dasharray: 2 2; }\n" +
                "  .node-card.known-issue rect.card-bg { stroke: #f59e0b; stroke-width: 2; }\n" +
                "  .node-card text { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; fill: #f8fafc; }\n" +
                "  .connector { fill: none; stroke: #475569; stroke-width: 2; transition: stroke 0.2s, stroke-width 0.2s; }\n" +
                "  .connector.visited { stroke: #22c55e; stroke-width: 2.5; }\n" +
                "  .connector.warning { stroke: #f59e0b; stroke-width: 2.5; }\n" +
                "  .connector.failed { stroke: #ef4444; stroke-width: 2.5; }\n" +
                "  .connector.child_failed { stroke: #818cf8; stroke-width: 1.5; stroke-dasharray: 3 3; }\n" +
                "  .connector.non_executable { stroke: #243044; stroke-width: 1.2; stroke-dasharray: 4 4; opacity: 0.6; }\n" +
                "  .connector.active { stroke: var(--accent); stroke-width: 3.5; opacity: 1.0; }\n" +
                "  #details-panel { position: absolute; right: 20px; bottom: 20px; width: 360px; max-height: 400px; overflow-y: auto; background: rgba(30, 41, 59, 0.95); border: 1px solid var(--border); backdrop-filter: blur(8px); border-radius: 8px; padding: 16px; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5); display: none; z-index: 20; }\n" +
                "  #details-panel h3 { font-size: 14px; margin-bottom: 8px; color: var(--accent); word-break: break-all; }\n" +
                "  #details-panel p { font-size: 12px; line-height: 1.6; color: var(--text-muted); }\n" +
                "  #details-panel .val { color: var(--text); font-weight: 500; }\n" +
                "  #details-panel pre { margin-top: 8px; background: #0f172a; padding: 8px; border-radius: 4px; font-size: 11px; color: #f59e0b; white-space: pre-wrap; word-break: break-all; }\n" +
                SummaryPlaque.css() +
                "</style>\n" +
                "</head>\n<body>\n" +
                "<header>\n" +
                "  <h1>⚡ Scenario Tree <span class=\"badge\">Interactive</span></h1>\n" +
                "  <div class=\"controls\">\n" +
                "    <button onclick=\"zoomIn()\">Zoom +</button>\n" +
                "    <button onclick=\"zoomOut()\">Zoom -</button>\n" +
                "    <button onclick=\"resetZoom()\">Reset</button>\n" +
                "  </div>\n" +
                "</header>\n" +
                "<div id=\"canvas-container\">\n" +
                "  <div id=\"tree-summary-plaque\" style=\"position: absolute; top: 16px; left: 16px; z-index: 15;\">\n" +
                summaryPlaque.renderHtml() +
                "  </div>\n" +
                "  <svg id=\"tree-svg\">\n" +
                "    <defs>\n" +
                "      <pattern id=\"known-issue-hatch\" width=\"12\" height=\"12\" patternUnits=\"userSpaceOnUse\">\n" +
                "        <path d=\"M 0 12 L 12 0 M -3 3 L 3 -3 M 9 15 L 15 9 M 0 0 L 12 12 M -3 9 L 3 15 M 9 -3 L 15 3\" stroke=\"#fbbf24\" stroke-width=\"1.8\" stroke-opacity=\"0.6\" />\n" +
                "      </pattern>\n" +
                "    </defs>\n" +
                "    <g id=\"viewport\">\n" +
                svgContent +
                "  </g></svg>\n" +
                "  <div id=\"details-panel\">\n" +
                "    <h3 id=\"d-name\">Node</h3>\n" +
                "    <p>Group: <span id=\"d-group\" class=\"val\"></span></p>\n" +
                "    <p>Status: <span id=\"d-status\" class=\"val\"></span></p>\n" +
                "    <p>Executions: <span id=\"d-exec\" class=\"val\"></span></p>\n" +
                "    <p>Events: <span id=\"d-events\" class=\"val\"></span></p>\n" +
                "    <div id=\"d-known-issues\" style=\"display:none; margin-top:10px; padding:10px; background:rgba(245,158,11,0.12); border:1px solid rgba(245,158,11,0.35); border-radius:6px;\">\n" +
                "      <div style=\"font-weight:600; font-size:12px; color:#fbbf24; margin-bottom:6px;\">Known Issues</div>\n" +
                "      <div id=\"d-ki-content\" style=\"font-size:11px; color:#f8fafc;\"></div>\n" +
                "    </div>\n" +
                "    <pre id=\"d-details\"></pre>\n" +
                "  </div>\n" +
                "</div>\n" +
                "<script>\n" +
                "  let scale = 1, panX = 40, panY = 40, isPanning = false, startX, startY;\n" +
                "  const viewport = document.getElementById('viewport');\n" +
                "  const container = document.getElementById('canvas-container');\n" +
                "  function updateTransform() { viewport.setAttribute('transform', `translate(${panX}, ${panY}) scale(${scale})`); }\n" +
                "  container.addEventListener('mousedown', (e) => { isPanning = true; startX = e.clientX - panX; startY = e.clientY - panY; });\n" +
                "  window.addEventListener('mousemove', (e) => { if (!isPanning) return; panX = e.clientX - startX; panY = e.clientY - startY; updateTransform(); });\n" +
                "  window.addEventListener('mouseup', () => isPanning = false);\n" +
                "  container.addEventListener('wheel', (e) => { e.preventDefault(); const factor = e.deltaY < 0 ? 1.1 : 0.9; scale = Math.min(Math.max(0.2, scale * factor), 4); updateTransform(); });\n" +
                "  function zoomIn() { scale = Math.min(scale * 1.2, 4); updateTransform(); }\n" +
                "  function zoomOut() { scale = Math.max(scale / 1.2, 0.2); updateTransform(); }\n" +
                "  function autoFit() {\n" +
                "    try {\n" +
                "      const bbox = viewport.getBBox();\n" +
                "      if (!bbox || bbox.width <= 0 || bbox.height <= 0) return;\n" +
                "      const cW = container.clientWidth || window.innerWidth;\n" +
                "      const cH = container.clientHeight || window.innerHeight;\n" +
                "      const sX = (cW - 80) / bbox.width;\n" +
                "      const sY = (cH - 80) / bbox.height;\n" +
                "      scale = Math.min(Math.max(0.15, Math.min(sX, sY)), 1.5);\n" +
                "      panX = (cW - bbox.width * scale) / 2 - bbox.x * scale;\n" +
                "      panY = (cH - bbox.height * scale) / 2 - bbox.y * scale;\n" +
                "      updateTransform();\n" +
                "    } catch (e) {}\n" +
                "  }\n" +
                "  function resetZoom() {\n" +
                "    if (" + Optimization.enabled + ") { autoFit(); } else { scale = 1; panX = 40; panY = 40; updateTransform(); }\n" +
                "  }\n" +
                "  document.querySelectorAll('.node-card').forEach(card => {\n" +
                "    card.addEventListener('mouseenter', () => {\n" +
                "      const id = card.id;\n" +
                "      document.querySelectorAll('.connector[data-from=\"' + id + '\"], .connector[data-to=\"' + id + '\"]').forEach(c => c.classList.add('active'));\n" +
                "    });\n" +
                "    card.addEventListener('mouseleave', () => {\n" +
                "      document.querySelectorAll('.connector.active').forEach(c => c.classList.remove('active'));\n" +
                "    });\n" +
                "  });\n" +
                "  const nodeDataMap = " + nodeDataJson + ";\n" +
                "  function showNodeDetails(id) {\n" +
                "    const data = nodeDataMap[id];\n" +
                "    if (!data) return;\n" +
                "    showDetails(data.name, data.group, data.status, data.exec, data.events, data.details, data.kiHtml);\n" +
                "  }\n" +
                "  function showDetails(name, group, status, exec, events, details, kiHtml) {\n" +
                "    const p = document.getElementById('details-panel');\n" +
                "    p.style.display = 'block';\n" +
                "    document.getElementById('d-name').innerText = name;\n" +
                "    document.getElementById('d-group').innerText = group;\n" +
                "    document.getElementById('d-status').innerText = status;\n" +
                "    document.getElementById('d-exec').innerText = exec;\n" +
                "    document.getElementById('d-events').innerText = events;\n" +
                "    const kiBox = document.getElementById('d-known-issues');\n" +
                "    const kiContent = document.getElementById('d-ki-content');\n" +
                "    if (kiHtml && kiHtml.trim().length > 0) {\n" +
                "      kiBox.style.display = 'block';\n" +
                "      kiContent.innerHTML = kiHtml;\n" +
                "    } else {\n" +
                "      kiBox.style.display = 'none';\n" +
                "      kiContent.innerHTML = '';\n" +
                "    }\n" +
                "    const det = document.getElementById('d-details');\n" +
                "    if (details && details.trim().length > 0) { det.style.display = 'block'; det.innerText = details; }\n" +
                "    else { det.style.display = 'none'; }\n" +
                "  }\n" +
                "  if (" + Optimization.enabled + ") {\n" +
                "    window.addEventListener('resize', autoFit);\n" +
                "    setTimeout(autoFit, 100);\n" +
                "  } else {\n" +
                "    updateTransform();\n" +
                "  }\n" +
                "</script>\n" +
                "</body></html>";
    }

    private static void pruneOffBranches(final List<NodeData> nodes, final List<Edge> edges) {
        if (!works.lysenko.util.prop.tree.HideOffBranches.b()) return;

        final Set<NodeData> toRemove = new HashSet<>();
        for (final NodeData n : nodes) {
            if (!isTheoreticallyExecutable(n)) {
                toRemove.add(n);
            }
        }

        if (toRemove.isEmpty()) return;

        nodes.removeAll(toRemove);

        edges.removeIf(e -> toRemove.contains(e.from()) || toRemove.contains(e.to()));

        for (final NodeData n : nodes) {
            n.children().removeAll(toRemove);
        }
    }
}
