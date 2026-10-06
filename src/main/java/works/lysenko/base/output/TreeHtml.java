package works.lysenko.base.output;

import works.lysenko.base.output.svg.Groups;
import works.lysenko.base.output.svg.Parts;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Node;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.type.Result;

import java.io.File;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.Base.core;
import static works.lysenko.Base.log;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Swap.f;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.func.type.Files.writeToFile;
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

    private TreeHtml() {}

    static final class NodeData {
        private final String id;
        private final String label;
        private final String group;
        private final int col;
        private double row;
        private final _Result result;
        private final List<NodeData> children = new ArrayList<>();
        private NodeData parent;

        NodeData(final String id, final String label, final String group, final int col, final double row, final _Result result) {
            this.id = id;
            this.label = label;
            this.group = group;
            this.col = col;
            this.row = row;
            this.result = result;
        }

        public String id() { return id; }
        public String label() { return label; }
        public String group() { return group; }
        public int col() { return col; }
        public double row() { return row; }
        public void setRow(final double row) { this.row = row; }
        public _Result result() { return result; }
        public List<NodeData> children() { return children; }
        public NodeData parent() { return parent; }
        public void setParent(final NodeData parent) { this.parent = parent; }
    }

    record Edge(NodeData from, NodeData to) {}

    record TreeLayout(List<NodeData> nodes, List<Edge> edges) {}

    static TreeLayout computeLayout(final TreeMap<String, Result> sorted) {

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

        // Harmonic centering: align parents vertically to the center of their children
        // and position each next level so plaques move lower to be closer to their parents
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
            sortedRoots.sort(Comparator.comparing(_Scenario::getSimpleName));
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
        final NodeData node = new NodeData(id, label, group, col, rows[0]++, resultFor(scenario, sorted));
        nodes.add(node);
        if (null != parent) {
            node.setParent(parent);
            parent.children().add(node);
            edges.add(new Edge(parent, node));
        }
        if (scenario instanceof _Node parentNode)
            for (final var child : parentNode.getPool().getPairList())
                processScenario(child.k(), node, col + 1, rows, ancestors, sorted, nodes, edges);
        ancestors.remove(scenario);
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

    static void optimizeLayout(final List<NodeData> nodes) {
        if (nodes.isEmpty()) return;

        final Map<Integer, List<NodeData>> colMap = new HashMap<>();
        int maxCol = 0;
        for (final NodeData n : nodes) {
            colMap.computeIfAbsent(n.col(), k -> new ArrayList<>()).add(n);
            if (n.col() > maxCol) maxCol = n.col();
        }

        // Initialize rows sequentially in each column
        for (final List<NodeData> colNodes : colMap.values()) {
            for (int i = 0; i < colNodes.size(); i++) {
                colNodes.get(i).setRow(i + 1.0);
            }
        }

        // Iterative barycentric relaxation with isotonic regression (PAVA)
        // Minimizes total edge distance while strictly enforcing the non-overlapping constraint (row[i+1] >= row[i] + 1.0)
        final int iterations = 24;
        for (int iter = 0; iter < iterations; iter++) {
            final boolean backward = (0 == iter % 2);
            final int startCol = backward ? maxCol : 0;
            final int endCol = backward ? 0 : maxCol;
            final int step = backward ? -1 : 1;

            for (int c = startCol; backward ? c >= endCol : c <= endCol; c += step) {
                final List<NodeData> colNodes = colMap.get(c);
                if (null == colNodes || colNodes.isEmpty()) continue;

                final record NodeTarget(NodeData node, double target) {}
                final List<NodeTarget> nodeTargets = new ArrayList<>(colNodes.size());
                for (final NodeData n : colNodes) {
                    double sum = 0.0;
                    int count = 0;
                    if (null != n.parent()) {
                        sum += n.parent().row();
                        count++;
                    }
                    for (final NodeData child : n.children()) {
                        sum += child.row();
                        count++;
                    }
                    final double target = (0 < count) ? (sum / count) : n.row();
                    nodeTargets.add(new NodeTarget(n, target));
                }

                nodeTargets.sort(Comparator.comparingDouble(NodeTarget::target));

                final double[] targets = new double[colNodes.size()];
                for (int i = 0; i < nodeTargets.size(); i++) {
                    colNodes.set(i, nodeTargets.get(i).node);
                    targets[i] = nodeTargets.get(i).target;
                }

                final double[] resolved = solveColumn1D(targets);
                for (int i = 0; i < colNodes.size(); i++) {
                    colNodes.get(i).setRow(resolved[i]);
                }
            }
        }

        // Normalize vertical offset so top node starts at row 1.0
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

    static void compactDisconnectedTrees(final List<NodeData> nodes) {

        final Map<NodeData, List<NodeData>> components = new IdentityHashMap<>();
        for (final NodeData node : nodes)
            components.computeIfAbsent(rootOf(node), ignored -> new ArrayList<>()).add(node);

        final List<List<NodeData>> ordered = new ArrayList<>(components.values());
        ordered.sort(Comparator.comparingDouble(TreeHtml::minimumRow));

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

    /**
     * Solves the 1D non-overlapping layout problem minimizing squared distance to targets
     * subject to y[i+1] >= y[i] + 1.0 and y[0] >= 1.0 using the Pool Adjacent Violators Algorithm (PAVA).
     */
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

    static String renderHtml(final List<NodeData> nodes, final List<Edge> edges) {
        final StringBuilder svgContent = new StringBuilder();

        // Sort edges so visited/active paths are rendered on top of unvisited paths
        final List<Edge> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort(Comparator.comparingInt(e -> {
            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            return (toExecs > 0 && fromExecs > 0) ? 1 : 0;
        }));

        // Render Edges (smooth cubic Béziers)
        for (final Edge e : sortedEdges) {
            final int startX = e.from().col() * COL_WIDTH + CARD_WIDTH + 40;
            final int startY = (int) Math.round(e.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + 20);
            final int endX = e.to().col() * COL_WIDTH + 40;
            final int endY = (int) Math.round(e.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + 20);
            final int cX = (startX + endX) / 2;

            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            final int toEvents = (null != e.to().result() && null != e.to().result().getEvents()) ? e.to().result().getEvents().size() : 0;

            final String edgeClass;
            if (toExecs > 0 && fromExecs > 0) {
                edgeClass = (toEvents > 0) ? "connector warning" : "connector visited";
            } else {
                edgeClass = "connector";
            }

            svgContent.append(String.format(
                    Locale.US,
                    "<path class=\"%s\" data-from=\"%s\" data-to=\"%s\" d=\"M %d %d C %d %d, %d %d, %d %d\" />\n",
                    edgeClass, e.from().id(), e.to().id(), startX, startY, cX, startY, cX, endY, endX, endY
            ));
        }

                // Render Nodes
        for (final NodeData n : nodes) {
            final int x = n.col() * COL_WIDTH + 40;
            final int y = (int) Math.round(n.row() * ROW_HEIGHT + 20);
            final _Result res = n.result();

            final int execs = (null != res) ? res.getExecutions() : 0;
            final int eventCount = (null != res && null != res.getEvents()) ? res.getEvents().size() : 0;

            String statusClass = "unvisited";
            String badgeColor = "#64748b";
            if (execs > 0) {
                if (eventCount == 0) {
                    statusClass = "passed";
                    badgeColor = "#22c55e";
                } else {
                    statusClass = "warning";
                    badgeColor = "#f59e0b";
                }
            }

            final StringBuilder eventDetails = new StringBuilder();
            if (null != res && null != res.getEvents()) {
                for (final _LogRecord lr : res.getEvents()) {
                    eventDetails.append(lr.toString().replace("\"", "&quot;").replace("'", "&apos;")).append("\\n");
                }
            }

            final String safeLabel = n.label().replace("<", "&lt;").replace(">", "&gt;");
            svgContent.append(String.format(
                    Locale.US,
                    "<g id=\"%s\" class=\"node-card %s\" transform=\"translate(%d, %d)\" onclick=\"showDetails('%s', '%s', '%d', '%d', '%s')\">\n" +
                    "  <rect width=\"%d\" height=\"%d\" />\n" +
                    "  <text x=\"14\" y=\"26\">%s</text>\n" +
                    "  <circle cx=\"%d\" cy=\"22\" r=\"6\" fill=\"%s\" />\n" +
                    "</g>\n",
                    n.id(), statusClass, x, y, safeLabel, n.group(), execs, eventCount, eventDetails,
                    CARD_WIDTH, CARD_HEIGHT, safeLabel, CARD_WIDTH - 18, badgeColor
            ));
        }

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<title>Scenario Execution Tree</title>\n" +
                "<style>\n" +
                "  :root { --bg: #0f172a; --panel-bg: #1e293b; --border: #334155; --text: #f8fafc; --text-muted: #94a3b8; --accent: #38bdf8; }\n" +
                "  * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
                "  body { background: var(--bg); color: var(--text); font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; overflow: hidden; width: 100vw; height: 100vh; display: flex; flex-direction: column; }\n" +
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
                "  .node-card.unvisited rect { fill: #1e293b; stroke: #475569; }\n" +
                "  .node-card text { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; fill: #f8fafc; }\n" +
                "  .connector { fill: none; stroke: #475569; stroke-width: 2; transition: stroke 0.2s, stroke-width 0.2s; }\n" +
                "  .connector.visited { stroke: #22c55e; stroke-width: 2.5; }\n" +
                "  .connector.warning { stroke: #f59e0b; stroke-width: 2.5; }\n" +
                "  .connector.active { stroke: var(--accent); stroke-width: 3.5; }\n" +
                "  #details-panel { position: absolute; right: 20px; bottom: 20px; width: 360px; max-height: 400px; overflow-y: auto; background: rgba(30, 41, 59, 0.95); border: 1px solid var(--border); backdrop-filter: blur(8px); border-radius: 8px; padding: 16px; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5); display: none; z-index: 20; }\n" +
                "  #details-panel h3 { font-size: 14px; margin-bottom: 8px; color: var(--accent); word-break: break-all; }\n" +
                "  #details-panel p { font-size: 12px; line-height: 1.6; color: var(--text-muted); }\n" +
                "  #details-panel .val { color: var(--text); font-weight: 500; }\n" +
                "  #details-panel pre { margin-top: 8px; background: #0f172a; padding: 8px; border-radius: 4px; font-size: 11px; color: #f59e0b; white-space: pre-wrap; word-break: break-all; }\n" +
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
                "  <svg id=\"tree-svg\"><g id=\"viewport\">\n" +
                svgContent +
                "  </g></svg>\n" +
                "  <div id=\"details-panel\">\n" +
                "    <h3 id=\"d-name\">Node</h3>\n" +
                "    <p>Group: <span id=\"d-group\" class=\"val\"></span></p>\n" +
                "    <p>Executions: <span id=\"d-exec\" class=\"val\"></span></p>\n" +
                "    <p>Events: <span id=\"d-events\" class=\"val\"></span></p>\n" +
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
                "  function showDetails(name, group, exec, events, details) {\n" +
                "    const p = document.getElementById('details-panel');\n" +
                "    p.style.display = 'block';\n" +
                "    document.getElementById('d-name').innerText = name;\n" +
                "    document.getElementById('d-group').innerText = group;\n" +
                "    document.getElementById('d-exec').innerText = exec;\n" +
                "    document.getElementById('d-events').innerText = events;\n" +
                "    const det = document.getElementById('d-details');\n" +
                "    if (details && details.trim().length > 0) { det.style.display = 'block'; det.innerText = details; }\n" +
                "    else { det.style.display = 'none'; }\n" +
                "  }\n" +
                "  updateTransform();\n" +
                "</script>\n" +
                "</body></html>";
    }
}
