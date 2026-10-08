package works.lysenko.base.output;

import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.output.svg.Groups;
import works.lysenko.base.output.svg.Parts;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Node;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.type.Result;
import org.apache.commons.math3.fraction.Fraction;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.Base.core;
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
        private final int col;
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

        final SortedMap<String, TreeMap<String, Result>> t = new Groups(TESTS, sorted);
        for (final Map.Entry<String, TreeMap<String, Result>> group : t.entrySet()) {
            processGroup(group, null, 0, dy, nodes, edges);
        }

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
        for (final _Scenario root : sortedRoots)
            processScenario(root, null, 0, rows, ancestors, sorted, nodes, edges);
        optimizeLayout(nodes);
        return new TreeLayout(nodes, edges);
    }

    private static void processScenario(final _Scenario scenario, final NodeData parent, final int col, final int[] rows,
                                        final Set<_Scenario> ancestors, final TreeMap<String, Result> sorted,
                                        final List<NodeData> nodes, final List<Edge> edges) {

        if (null == scenario || !ancestors.add(scenario)) return;
        final String label = b(scenario.getSimpleName(), scenario.type().tag());
        final String id = s("scenario_", nodes.size(), "_", scenario.getSimpleName());
        final String group = (null == parent) ? TESTS : parent.label();
        final NodeData node = new NodeData(id, label, group, col, rows[0]++, resultFor(scenario, sorted), scenario);

        final KnownIssuesStore store = getKnownIssuesStore();
        if (null != store) {
            final List<KnownIssue> matched = store.getForNode(scenario, node.result());
            if (null != matched && !matched.isEmpty()) {
                node.knownIssues().addAll(matched);
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
                processScenario(child.k(), node, col + 1, rows, ancestors, sorted, nodes, edges);
        }
        ancestors.remove(scenario);
    }

    static boolean hasFailure(final NodeData n) {

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

    static boolean hasChildFailure(final NodeData n) {

        if (null == n) return false;
        if (null != n.scenario()) {
            if (n.scenario().hasChildFailed() || n.scenario().getExecutionStatus() == works.lysenko.util.data.enums.ExecutionStatus.CHILD_FAILED) return true;
        }
        final _Result res = n.result();
        if (null != res) {
            return res.status() == works.lysenko.util.data.enums.ExecutionStatus.CHILD_FAILED;
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
            if (!parentIsStructural && !isTheoreticallyExecutable(n.parent(), accessibleScenarios)) {
                return false;
            }
        }
        if (null != accessibleScenarios && !accessibleScenarios.isEmpty()) {
            if (null != n.scenario()) {
                return accessibleScenarios.contains(n.scenario())
                        || accessibleScenarios.stream().anyMatch(s -> s.getName().equals(n.scenario().getName()));
            }
            return accessibleScenarios.stream().anyMatch(s ->
                    s.getSimpleName().equals(n.label())
                            || s.getShortName().equals(n.label())
                            || b(s.getSimpleName(), s.type().tag()).equals(n.label()));
        }
        if (null != n.scenario()) {
            final _Scenario s = n.scenario();
            final boolean combinationsPositive = s.calculateCombinations(true) > 0;
            final boolean executableOrConfigured = s.isExecutable() || isNotNull(s.weightConfigured());
            return executableOrConfigured && combinationsPositive;
        }
        if (null != n.result()) {
            final Fraction cw = n.result().getConfiguredWeight();
            if (null == cw) {
                return false;
            }
            final double cwVal = cw.doubleValue();
            final double dwVal = (null != n.result().getDownstreamWeight()) ? n.result().getDownstreamWeight().doubleValue() : 0.0;
            final double uwVal = (null != n.result().getUpstreamWeight()) ? n.result().getUpstreamWeight().doubleValue() : 0.0;
            if (cwVal <= 0.0 && dwVal <= 0.0 && uwVal <= 0.0) {
                return false;
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
                                     final List<NodeData> nodes, final List<Edge> edges) {
        final Map<String, NodeData> myNodes = new HashMap<>();
        final Parts parts = Parts.process(group.getValue());

        for (final Map.Entry<String, Result> entry : parts.head.entrySet()) {
            final int myRow = dy[col]++;
            final String rawKey = entry.getKey();
            final String shortKey = rawKey.split(SPACE)[ZERO];
            final String nodeId = s("col_", col, "_row_", myRow, "_", shortKey.replace('.', '_'));
            final NodeData nd = new NodeData(nodeId, rawKey, group.getKey(), col, myRow, entry.getValue());

            final KnownIssuesStore store = getKnownIssuesStore();
            if (null != store && null != nd.result()) {
                for (final KnownIssue ki : store.getIssues()) {
                    if (KnownIssuesStore.matchesScenario(ki.scenario(), rawKey, shortKey)) {
                        nd.knownIssues().add(ki);
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
            processGroup(subgroup, nextParent, col + 1, dy, nodes, edges);
        }
    }

    private static double columnWeight(final int col) {
        return Math.pow(2.0, Math.max(0, col));
    }

    static void optimizeLayout(final List<NodeData> nodes) {
        if (nodes.isEmpty()) return;

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

    private static String escapeJsString(final String text) {
        if (null == text) return "";
        return text.replace("\\", "\\\\")
                   .replace("'", "\\'")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "");
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

        for (final NodeData n : nodes) {
            final int x = n.col() * COL_WIDTH + 40;
            final int y = (int) Math.round(n.row() * ROW_HEIGHT + 20);
            final _Result res = n.result();

            final int execs = (null != res) ? res.getExecutions() : 0;
            final int eventCount = (null != res && null != res.getEvents()) ? res.getEvents().size() : 0;
            final boolean failure = hasFailure(n);
            final boolean childFailure = hasChildFailure(n);
            final boolean theoreticallyExecutable = isTheoreticallyExecutable(n);

            String statusClass = "unvisited";
            String badgeColor = "#64748b";
            String statusTitle = "Unvisited";
            if (failure) {
                statusClass = "failed";
                badgeColor = "#ef4444";
                statusTitle = "Failed";
            } else if (childFailure) {
                statusClass = "child_failed";
                badgeColor = "#818cf8";
                statusTitle = "Upset";
            } else if (execs > 0) {
                if (eventCount == 0) {
                    statusClass = "passed";
                    badgeColor = "#22c55e";
                    statusTitle = "Passed";
                } else {
                    statusClass = "warning";
                    badgeColor = "#f59e0b";
                    statusTitle = "Warning";
                }
            } else if (!theoreticallyExecutable) {
                statusClass = "non_executable";
                badgeColor = "#475569";
                statusTitle = "Non-executable";
            }

            final StringBuilder eventDetails = new StringBuilder();
            if (null != res && null != res.getEvents()) {
                for (final _LogRecord lr : res.getEvents()) {
                    eventDetails.append(lr.toString().replace("\"", "&quot;").replace("'", "&apos;")).append("\\n");
                }
            }

            final boolean hasKnownIssues = !n.knownIssues().isEmpty();
            final String kiHtml = renderKnownIssuesHtml(n.knownIssues());
            final String kiJsEscaped = escapeJsString(kiHtml);
            final String bugIndicator = hasKnownIssues ? String.format(Locale.US, "<text x=\"%d\" y=\"26\" font-size=\"12\">🐞</text>", CARD_WIDTH - 36) : "";

            final String safeLabel = n.label().replace("<", "&lt;").replace(">", "&gt;");
            svgContent.append(String.format(
                    Locale.US,
                    "<g id=\"%s\" class=\"node-card %s\" transform=\"translate(%d, %d)\" onclick=\"showDetails('%s', '%s', '%s', '%d', '%d', '%s', '%s')\">\n" +
                    "  <title>%s (%s)</title>\n" +
                    "  <rect width=\"%d\" height=\"%d\" />\n" +
                    "  <text x=\"14\" y=\"26\">%s</text>\n" +
                    "  %s\n" +
                    "  <circle cx=\"%d\" cy=\"22\" r=\"6\" fill=\"%s\" />\n" +
                    "</g>\n",
                    n.id(), statusClass, x, y, safeLabel, n.group(), statusTitle, execs, eventCount, eventDetails, kiJsEscaped,
                    safeLabel, statusTitle,
                    CARD_WIDTH, CARD_HEIGHT, safeLabel, bugIndicator, CARD_WIDTH - 18, badgeColor
            ));
        }

        final SummaryPlaque summaryPlaque = SummaryPlaque.computeFromNodes(nodes);

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
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
                "  .node-card rect { rx: 8; ry: 8; stroke-width: 1.5; }\n" +
                "  .node-card.passed rect { fill: #14532d; stroke: #22c55e; }\n" +
                "  .node-card.warning rect { fill: #713f12; stroke: #f59e0b; }\n" +
                "  .node-card.failed rect { fill: #450a0a; stroke: #ef4444; }\n" +
                "  .node-card.child_failed rect { fill: #1e1b4b; stroke: #818cf8; stroke-width: 1.2; stroke-dasharray: 3 3; }\n" +
                "  .node-card.unvisited rect { fill: #1e293b; stroke: #475569; }\n" +
                "  .node-card.non_executable rect { fill: #131a2a; stroke: #283548; stroke-width: 1.2; stroke-dasharray: 4 4; }\n" +
                "  .node-card.non_executable text { fill: #475569; }\n" +
                "  .node-card.non_executable circle { stroke: #283548; stroke-width: 1.2; stroke-dasharray: 2 2; }\n" +
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
                "  <svg id=\"tree-svg\"><g id=\"viewport\">\n" +
                svgContent +
                "  </g></svg>\n" +
                "  <div id=\"details-panel\">\n" +
                "    <h3 id=\"d-name\">Node</h3>\n" +
                "    <p>Group: <span id=\"d-group\" class=\"val\"></span></p>\n" +
                "    <p>Status: <span id=\"d-status\" class=\"val\"></span></p>\n" +
                "    <p>Executions: <span id=\"d-exec\" class=\"val\"></span></p>\n" +
                "    <p>Events: <span id=\"d-events\" class=\"val\"></span></p>\n" +
                "    <div id=\"d-known-issues\" style=\"display:none; margin-top:10px; padding:10px; background:rgba(192,132,252,0.12); border:1px solid rgba(192,132,252,0.3); border-radius:6px;\">\n" +
                "      <div style=\"font-weight:600; font-size:12px; color:#c084fc; margin-bottom:6px;\">🐞 Known Issues</div>\n" +
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
                "  function resetZoom() { scale = 1; panX = 40; panY = 40; updateTransform(); }\n" +
                "  document.querySelectorAll('.node-card').forEach(card => {\n" +
                "    card.addEventListener('mouseenter', () => {\n" +
                "      const id = card.id;\n" +
                "      document.querySelectorAll('.connector[data-from=\"' + id + '\"], .connector[data-to=\"' + id + '\"]').forEach(c => c.classList.add('active'));\n" +
                "    });\n" +
                "    card.addEventListener('mouseleave', () => {\n" +
                "      document.querySelectorAll('.connector.active').forEach(c => c.classList.remove('active'));\n" +
                "    });\n" +
                "  });\n" +
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
                "  updateTransform();\n" +
                "</script>\n" +
                "</body></html>";
    }
}
