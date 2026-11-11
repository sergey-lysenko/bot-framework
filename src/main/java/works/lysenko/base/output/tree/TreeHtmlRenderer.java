package works.lysenko.base.output.tree;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.output.SummaryPlaque;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.loghtml.TemplateEngine;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.prop.PropEnum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.shared.lang.word.D.DETAILS;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.E.EVENTS;
import static works.lysenko.util.lang.word.I.ISSUE;
import static works.lysenko.util.lang.word.K.KNOWN;
import static works.lysenko.util.spec.Symbols._LFD_;
import static works.lysenko.util.spec.Symbols._SLASH_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * Generates interactive HTML and SVG report documents for scenario execution trees.
 */
@SuppressWarnings({"HardCodedStringLiteral", "ClassWithoutLogger", "ClassNamePrefixedWithPackageName", "WeakerAccess"})
public record TreeHtmlRenderer() {

    private static final int COL_WIDTH = 260;
    private static final int ROW_HEIGHT = 56;
    private static final int CARD_WIDTH = 220;
    private static final int CARD_HEIGHT = 44;
    private static final String TREE_TEMPLATE_RESOURCE = b(_SLASH_, "works", "lysenko", "base", "output", "tree-report-template.html");
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[0-9;]*m");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Renders known issues into HTML fragment for the node details panel.
     *
     * @param issues iterable of known issues associated with the node
     * @return rendered HTML string
     */
    public static String renderKnownIssuesHtml(final Iterable<KnownIssue> issues) {
        final StringBuilder sb = new StringBuilder(128);
        if (isNotNull(issues)) {
            for (final KnownIssue ki : issues) {
                sb.append("<div style=\"margin-bottom:8px;\">");
                if (hasText(ki.title())) {
                    sb.append(s("<div style=\"font-weight:600; color:#f8fafc;\">", escapeHtml(ki.title()), "</div>"));
                }
                if (hasText(ki.description())) {
                    sb.append(s("<div style=\"color:#94a3b8; font-size:11px; margin-top:2px;\">", escapeHtml(ki.description()), "</div>"));
                }
                if (hasText(ki.link())) {
                    final String safeLink = escapeHtml(ki.link());
                    sb.append(s("<div style=\"margin-top:4px;\"><a href=\"", safeLink,
                            "\" target=\"_blank\" style=\"color:#38bdf8; text-decoration:underline; font-size:11px;\">",
                            safeLink, "</a></div>"));
                }
                sb.append("</div>");
            }
        }
        return sb.toString();
    }

    private static boolean hasText(final String value) {
        return isNotNull(value) && !value.isBlank();
    }

    /**
     * Escapes special HTML characters in text content.
     *
     * @param text raw text
     * @return escaped HTML string
     */
    public static String escapeHtml(final String text) {
        return (null == text) ? "" : text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Renders the complete interactive HTML scenario tree document.
     *
     * @param nodes list of tree nodes
     * @param edges list of tree edges
     * @return complete HTML document string
     */
    public static String renderHtml(final List<TreeHtml.NodeData> nodes, final List<TreeHtml.Edge> edges) {
        final StringBuilder svgContent = new StringBuilder(4096);
        final Set<_Scenario> accessibleScenarios = TreeLayoutComputer.getAccessibleScenarios();

        renderSvgEdges(edges, accessibleScenarios, svgContent);

        final boolean isAllLeafs = isAllLeafsMode();
        final int target = resolveTargetExecutions();
        final double maxLeafOverRatio = computeMaxLeafOverRatio(nodes, target);

        final VisualNodeResolver nodeResolver =
                new VisualNodeResolver(TreeTheme.DEFAULT, accessibleScenarios, isAllLeafs, target, maxLeafOverRatio);
        final List<VisualNode> visualNodes = new ArrayList<>(nodes.size());
        final Map<String, Map<String, Object>> nodeDataMap = new LinkedHashMap<>(nodes.size());

        for (int i = 0; i < nodes.size(); i++) {
            final TreeHtml.NodeData node = nodes.get(i);
            TreeLayoutComputer.logProgressIfNeeded("rendering visual nodes", i + 1, nodes.size());
            final VisualNode visualNode = nodeResolver.resolve(node);
            visualNodes.add(visualNode);
            nodeDataMap.put(node.id(), buildNodeInfo(node, visualNode));
        }

        svgContent.append(new HtmlTreeRenderer(TreeTheme.DEFAULT).renderNodes(visualNodes));

        final SummaryPlaque summaryPlaque = SummaryPlaque.computeFromNodes(nodes);
        final String nodeDataJson = serializeNodeData(nodeDataMap);

        final Map<String, String> replacements = new LinkedHashMap<>(4);
        replacements.put(s("{{", "SUMMARY_PLAQUE_CSS", "}}"), SummaryPlaque.css());
        replacements.put(s("{{", "SUMMARY_PLAQUE_HTML", "}}"), summaryPlaque.renderHtml());
        replacements.put(s("{{", "SVG_CONTENT", "}}"), svgContent.toString());
        replacements.put(s("{{", "NODE_DATA_JSON", "}}"), nodeDataJson);

        return TemplateEngine.replaceTemplate(TemplateEngine.loadTemplate(TREE_TEMPLATE_RESOURCE), replacements);
    }

    private static void renderSvgEdges(
            final List<TreeHtml.Edge> edges,
            final Set<_Scenario> accessibleScenarios,
            final StringBuilder svgContent) {
        final List<TreeHtml.Edge> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort(Comparator.comparingInt(e -> {
            final int toExecs = isNotNull(e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = isNotNull(e.from().result()) ? e.from().result().getExecutions() : 0;
            return (0 < toExecs && 0 < fromExecs) ? 1 : 0;
        }));

        for (int i = 0; i < sortedEdges.size(); i++) {
            final TreeHtml.Edge edge = sortedEdges.get(i);
            TreeLayoutComputer.logProgressIfNeeded("rendering SVG edges", i + 1, sortedEdges.size());
            final int startX = edge.from().col() * COL_WIDTH + CARD_WIDTH + 40;
            final long startY = Math.round(edge.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + 20);
            final int endX = edge.to().col() * COL_WIDTH + 40;
            final long endY = Math.round(edge.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + 20);
            final int cX = (startX + endX) / 2;
            final String edgeClass = resolveEdgeClass(edge, accessibleScenarios);

            svgContent.append(String.format(
                    Locale.US,
                    s("<path class=\"%s\" data-from=\"%s\" data-to=\"%s\" d=\"M %d %d C %d %d, %d %d, %d %d\" />", _LFD_),
                    edgeClass, edge.from().id(), edge.to().id(), startX, startY, cX, startY, cX, endY, endX, endY
            ));
        }
    }

    private static String resolveEdgeClass(final TreeHtml.Edge edge, final Set<_Scenario> accessibleScenarios) {
        final int toExecs = isNotNull(edge.to().result()) ? edge.to().result().getExecutions() : 0;
        final int fromExecs = isNotNull(edge.from().result()) ? edge.from().result().getExecutions() : 0;
        final String resultClass;
        if (0 < toExecs && 0 < fromExecs) {
            resultClass = resolveVisitedEdgeClass(edge.to());
        } else if (TreeLayoutComputer.isTheoreticallyExecutable(edge.to(), accessibleScenarios)) {
            resultClass = "connector";
        } else {
            resultClass = "connector non_executable";
        }
        return resultClass;
    }

    private static String resolveVisitedEdgeClass(final TreeHtml.NodeData targetNode) {
        final String visitedClass;
        if (TreeLayoutComputer.hasFailure(targetNode)) {
            visitedClass = "connector failed";
        } else if (TreeLayoutComputer.hasChildFailure(targetNode)) {
            visitedClass = "connector child_failed";
        } else {
            visitedClass = resolvePassingEdgeClass(targetNode);
        }
        return visitedClass;
    }

    private static String resolvePassingEdgeClass(final TreeHtml.NodeData targetNode) {
        final String passingClass;
        if (TreeHtml.hasWarning(targetNode)) {
            passingClass = "connector warning";
        } else if (TreeHtml.hasNotice(targetNode)) {
            passingClass = "connector notice";
        } else {
            passingClass = "connector visited";
        }
        return passingClass;
    }

    private static boolean isAllLeafsMode() {
        return isNotNull(works.lysenko.Base.parameters)
                ? works.lysenko.Base.parameters.isAllLeafs()
                : Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get());
    }

    private static int resolveTargetExecutions() {
        final int result;
        if (isNotNull(works.lysenko.Base.parameters)) {
            result = works.lysenko.Base.parameters.getAllLeafsExecutions();
        } else {
            final Integer propVal = PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get();
            result = isNotNull(propVal) ? Math.max(1, propVal) : 1;
        }
        return result;
    }

    private static double computeMaxLeafOverRatio(final Iterable<TreeHtml.NodeData> nodes, final int target) {
        double maxLeafOverRatio = 0.0;
        final int scenarioTarget = Math.max(1, target);
        for (final TreeHtml.NodeData node : nodes) {
            if (node.children().isEmpty() && isNotNull(node.result())) {
                final int execs = node.result().getExecutions();
                if (execs > scenarioTarget) {
                    maxLeafOverRatio = Math.max(maxLeafOverRatio, (double) (execs - scenarioTarget) / scenarioTarget);
                }
            }
        }
        return maxLeafOverRatio;
    }

    private static Map<String, Object> buildNodeInfo(final TreeHtml.NodeData node, final VisualNode visualNode) {
        final _Result res = node.result();
        final int execs = isNotNull(res) ? res.getExecutions() : 0;
        final int eventCount = (isNotNull(res) && isNotNull(res.getEvents())) ? res.getEvents().size() : 0;
        final String eventDetails = formatEventDetails(res);
        final String kiHtml = renderKnownIssuesHtml(node.knownIssues());
        final String safeLabel = node.label().replace("<", "&lt;").replace(">", "&gt;");
        final String execLinksHtml = renderExecLinksHtml(node, execs);

        final Map<String, Object> nodeInfo = new LinkedHashMap<>(9);
        nodeInfo.put("name", safeLabel);
        nodeInfo.put("group", node.group());
        nodeInfo.put("status", s(visualNode.badgeText(), (visualNode.hasKnownIssues() ? s(_SPACE_, e(ROUND, b(c(KNOWN), c(ISSUE)))) : "")));
        nodeInfo.put("exec", execs);
        nodeInfo.put("execLinksHtml", execLinksHtml);
        nodeInfo.put(EVENTS, eventCount);
        nodeInfo.put(DETAILS, eventDetails);
        nodeInfo.put("kiHtml", kiHtml);
        return nodeInfo;
    }

    private static String renderExecLinksHtml(final TreeHtml.NodeData node, final int execs) {
        if (execs <= 0) {
            return "";
        }
        final String label = node.label() != null ? node.label() : "";
        final String group = node.group() != null ? node.group() : "";
        final String fullPath = (hasText(group) && !label.startsWith(group)) ? (group + "." + label) : label;
        final String safePath = escapeHtml(fullPath);

        final StringBuilder sb = new StringBuilder(64);
        sb.append("<span style=\"font-size:11px; color:#94a3b8;\">(");
        for (int i = 1; i <= execs; i++) {
            if (i > 1) {
                sb.append(", ");
            }
            sb.append("<a class=\"exec-link\" href=\"#test-").append(i).append("-").append(safePath)
              .append("\" data-cycle=\"").append(i).append("\" data-scenario=\"").append(safePath)
              .append("\" style=\"color:#38bdf8; text-decoration:underline; font-weight:600;\">#")
              .append(i).append("</a>");
        }
        sb.append(")</span>");
        return sb.toString();
    }

    private static String formatEventDetails(final _Result res) {
        final StringBuilder eventDetails = new StringBuilder(128);
        if (isNotNull(res) && isNotNull(res.getEvents())) {
            final Matcher ansiMatcher = ANSI_PATTERN.matcher("");
            for (final _LogRecord lr : res.getEvents()) {
                final String rawMsg = isNotNull(lr.text()) ? lr.text() : lr.toString();
                if (hasText(rawMsg)) {
                    final String cleanMsg = ansiMatcher.reset(rawMsg).replaceAll("").strip();
                    eventDetails.append(cleanMsg).append(_LFD_);
                }
            }
        }
        return eventDetails.toString();
    }

    private static String serializeNodeData(final Map<String, Map<String, Object>> nodeDataMap) {
        String nodeDataJson = "{}";
        try {
            nodeDataJson = MAPPER.writeValueAsString(nodeDataMap);
        } catch (final JsonProcessingException ignored) {
        }
        return nodeDataJson;
    }
}
