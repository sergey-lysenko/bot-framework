package works.lysenko.base.output.tree;

import com.fasterxml.jackson.databind.ObjectMapper;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.output.SummaryPlaque;
import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.prop.PropEnum;
import works.lysenko.util.prop.tree.Optimization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Generates interactive HTML and SVG report documents for scenario execution trees.
 */
public final class TreeHtmlRenderer {

    private static final int COL_WIDTH = 260;
    private static final int ROW_HEIGHT = 56;
    private static final int CARD_WIDTH = 220;
    private static final int CARD_HEIGHT = 44;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TreeHtmlRenderer() {
    }

    public static String renderKnownIssuesHtml(final List<KnownIssue> issues) {
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

    public static String escapeHtml(final String text) {
        if (null == text) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    public static String renderHtml(final List<NodeData> nodes, final List<Edge> edges) {
        final StringBuilder svgContent = new StringBuilder();
        final Set<_Scenario> accessibleScenarios = TreeLayoutComputer.getAccessibleScenarios();

        final List<Edge> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort(Comparator.comparingInt(e -> {
            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            return (toExecs > 0 && fromExecs > 0) ? 1 : 0;
        }));

        for (int i = 0; i < sortedEdges.size(); i++) {
            final Edge e = sortedEdges.get(i);
            TreeLayoutComputer.logProgressIfNeeded("rendering SVG edges", i + 1, sortedEdges.size());
            final int startX = e.from().col() * COL_WIDTH + CARD_WIDTH + 40;
            final int startY = (int) Math.round(e.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + 20);
            final int endX = e.to().col() * COL_WIDTH + 40;
            final int endY = (int) Math.round(e.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + 20);
            final int cX = (startX + endX) / 2;

            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            final boolean toWarning = (null != e.to().result() && e.to().result().status() == ExecutionStatus.WARNING);
            final boolean toFailure = TreeLayoutComputer.hasFailure(e.to());
            final boolean toChildFailure = TreeLayoutComputer.hasChildFailure(e.to());
            final boolean toTheoreticallyExecutable = TreeLayoutComputer.isTheoreticallyExecutable(e.to(), accessibleScenarios);

            final String edgeClass;
            if (toExecs > 0 && fromExecs > 0) {
                if (toFailure) {
                    edgeClass = "connector failed";
                } else if (toChildFailure) {
                    edgeClass = "connector child_failed";
                } else if (toWarning) {
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
                : (null != PropEnum._TEST_ALL_LEAFS.get()
                   && Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get()));

        final int target = (null != works.lysenko.Base.parameters)
                ? works.lysenko.Base.parameters.getAllLeafsExecutions()
                : (null != PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()
                   ? Math.max(1, PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) : 1);

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

        final VisualNodeResolver nodeResolver = 
                new VisualNodeResolver(TreeTheme.DEFAULT, accessibleScenarios, isAllLeafs, target, maxLeafOverRatio);
        final List<VisualNode> visualNodes = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            final NodeData n = nodes.get(i);
            TreeLayoutComputer.logProgressIfNeeded("rendering visual nodes", i + 1, nodes.size());
            final _Result res = n.result();
            final int execs = (null != res) ? res.getExecutions() : 0;
            final int eventCount = (null != res && null != res.getEvents()) ? res.getEvents().size() : 0;
            
            final VisualNode visualNode = nodeResolver.resolve(n);
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
        
        svgContent.append(new HtmlTreeRenderer(TreeTheme.DEFAULT).renderNodes(visualNodes));

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
                "      <rect id=\"cb\" width=\"220\" height=\"44\" rx=\"8\" ry=\"8\" />\n" +
                "      <rect id=\"pt\" x=\"8\" y=\"38\" width=\"204\" height=\"3\" fill=\"#0f172a\" opacity=\"0.7\" rx=\"1.5\" ry=\"1.5\" />\n" +
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
                "    if (" + Optimization.progression + ") { autoFit(); } else { scale = 1; panX = 40; panY = 40; updateTransform(); }\n" +
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
                "  if (" + Optimization.progression + ") {\n" +
                "    window.addEventListener('resize', autoFit);\n" +
                "    setTimeout(autoFit, 100);\n" +
                "  } else {\n" +
                "    updateTransform();\n" +
                "  }\n" +
                "</script>\n" +
                "</body></html>";
    }
}
