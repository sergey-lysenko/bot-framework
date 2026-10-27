package works.lysenko.base.output.tree;

import works.lysenko.base.output.ProgressionSettings;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static works.lysenko.Base.parameters;
import static works.lysenko.Base.timer;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Renders scenario tree progression layouts as SVG strings.
 */
public final class SvgTreeRenderer {

    private static final int COL_WIDTH = 270;
    private static final int ROW_HEIGHT = 60;
    private static final int CARD_WIDTH = 220;
    private static final int CARD_HEIGHT = 46;
    private static final int HEADER_HEIGHT_BASE = 60;

    private static int headerHeight(final int w) {
        return (int) Math.round(HEADER_HEIGHT_BASE * Math.max(1.0, w / 1200.0));
    }

    private static final int PADDING_X = 40;
    private static final int PADDING_Y = 32;
    private static final int BOTTOM_PADDING = 32;

    private SvgTreeRenderer() {
    }

    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final TreeHtml.AccessibleContext accessibleCtx,
            final TreeCameraTransform.Transform overrideTx,
            final Map<NodeData, Boolean> cachedExecutable,
            final int sessionBaseW,
            final int sessionBaseH) {

        final TreeHtml.AccessibleContext ctx = (null != accessibleCtx) ? accessibleCtx : new TreeHtml.AccessibleContext(accessibleScenarios);
        final Map<NodeData, Boolean> executableCache = (null != cachedExecutable) ? cachedExecutable : new HashMap<>();
        final Map<NodeData, Boolean> childFailureCache = new HashMap<>();

        int totalLeafs = 0;
        int coveredLeafs = 0;
        int totalExecs = 0;
        double maxLeafOverRatio = 0.0;

        int fullMaxCol = 0;
        double fullMaxRow = 0.0;

        final int totalStatsNodes = layout.nodes().size();
        for (int i = 0; i < totalStatsNodes; i++) {
            final NodeData n = layout.nodes().get(i);
            if (n.col() > fullMaxCol) fullMaxCol = n.col();
            if (n.row() > fullMaxRow) fullMaxRow = n.row();
            final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
            totalExecs += execs;
            if (ScenarioType.LEAF == TreeProgressMetrics.getScenarioType(n)) {
                maxLeafOverRatio = Math.max(maxLeafOverRatio, TreeProgressMetrics.getLeafOverExecutionRatio(n, target));
            }
            if (n.children().isEmpty() && TreeProgressMetrics.isTerminalScenario(n)) {
                if (TreeHtml.isTheoreticallyExecutable(n, ctx, executableCache) || execs > 0) {
                    totalLeafs++;
                    if (execs >= TreeProgressMetrics.getTargetExecutions(n, target)) coveredLeafs++;
                }
            }
        }

        final int baseW;
        final int baseH;
        if (sessionBaseW > 0 && sessionBaseH > 0) {
            baseW = sessionBaseW;
            baseH = sessionBaseH;
        } else {
            baseW = (int) evenDimension(Math.max(800L, ((long) fullMaxCol + 1L) * COL_WIDTH + PADDING_X * 2L));
            baseH = (int) evenDimension(Math.max(
                    400L,
                    (long) Math.ceil((fullMaxRow + 1.0) * ROW_HEIGHT + headerHeight(sessionBaseW) + PADDING_Y + BOTTOM_PADDING)));
        }

        final TreeCameraTransform.Transform tx = (null != overrideTx) ? overrideTx : TreeCameraTransform.computeZoomTransform(layout, recentNodeKeys, target, baseW, baseH);
        final double zoom = tx.zoom();
        final double translateX = tx.translateX();
        final double translateY = tx.translateY();

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
                baseW, baseH, baseW, baseH));
        sb.append("  <defs>\n");
        sb.append("    <pattern id=\"dot-grid\" width=\"24\" height=\"24\" patternUnits=\"userSpaceOnUse\" y=\"60\">\n");
        sb.append("      <rect width=\"24\" height=\"24\" fill=\"#0F172A\" />\n");
        sb.append("      <circle cx=\"1\" cy=\"1\" r=\"1\" fill=\"#1E293B\" opacity=\"0.7\" />\n");
        sb.append("    </pattern>\n");
        sb.append("    <pattern id=\"known-issue-hatch\" width=\"10\" height=\"10\" patternTransform=\"rotate(45 0 0)\" patternUnits=\"userSpaceOnUse\">\n");
        sb.append("      <line x1=\"0\" y1=\"0\" x2=\"0\" y2=\"10\" stroke=\"#fbbf24\" stroke-width=\"2\" stroke-opacity=\"0.45\" />\n");
        sb.append("      <line x1=\"0\" y1=\"0\" x2=\"10\" y2=\"0\" stroke=\"#fbbf24\" stroke-width=\"2\" stroke-opacity=\"0.45\" />\n");
        sb.append("    </pattern>\n");
        sb.append("    <rect id=\"cb\" width=\"" + CARD_WIDTH + "\" height=\"" + CARD_HEIGHT + "\" rx=\"8\" ry=\"8\" />\n");
        sb.append("    <rect id=\"pt\" x=\"8\" y=\"" + (CARD_HEIGHT - 8) + "\" width=\"" + (CARD_WIDTH - 16) + "\" height=\"4\" fill=\"#0f172a\" opacity=\"0.8\" rx=\"2\" ry=\"2\" />\n");
        sb.append("    <rect id=\"ho\" width=\"" + CARD_WIDTH + "\" height=\"" + CARD_HEIGHT + "\" fill=\"url(#known-issue-hatch)\" rx=\"8\" ry=\"8\" pointer-events=\"none\" />\n");
        sb.append("  </defs>\n");
        sb.append("  <style>\n");
        sb.append("    .tx1 { font-family: monospace; font-size: 10px; font-weight: bold; }\n");
        sb.append("    .tx2 { font-size: 12px; }\n");
        sb.append("    .tx3 { font-family: monospace; font-weight: bold; font-size: 11px; }\n");
        sb.append("    .tx4 { font-family: sans-serif; font-weight: bold; font-size: 15px; fill: #F8FAFC; }\n");
        sb.append("    .pb { fill: #0f172a; opacity: 0.8; }\n");
        sb.append("    .edge { fill: none; }\n");
        sb.append("  </style>\n");

        sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"#0F172A\" />\n");
        sb.append(String.format(Locale.US,
                "  <rect x=\"0\" y=\"60\" width=\"%d\" height=\"%d\" fill=\"url(#dot-grid)\" />\n",
                baseW, baseH - 60));

        sb.append(String.format(Locale.US,
                "  <g transform=\"translate(%.2f, %.2f) scale(%.4f)\">\n",
                translateX, translateY, zoom));

        final List<Edge> sortedEdges = new ArrayList<>(layout.edges());
        sortedEdges.sort(Comparator.comparingInt(e -> {
            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            return (toExecs > 0 && fromExecs > 0) ? 1 : 0;
        }));

        for (final Edge e : sortedEdges) {
            final int startX = e.from().col() * COL_WIDTH + CARD_WIDTH + PADDING_X;
            final int startY = (int) Math.round(e.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerHeight(baseW) + PADDING_Y);
            final int endX = e.to().col() * COL_WIDTH + PADDING_X;
            final int endY = (int) Math.round(e.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerHeight(baseW) + PADDING_Y);
            final int cX = (startX + endX) / 2;

            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            final boolean toWarning = (null != e.to().result() && e.to().result().status() == ExecutionStatus.WARNING);
            final boolean isRecent = recentNodeKeys.contains(e.to().id());

            String strokeColor = "#3341558C";
            float strokeW = 1.2f;
            String dash = "";

            if (isRecent) {
                strokeColor = "#38BDF8";
                strokeW = 3.0f;
            } else if (TreeHtml.hasFailure(e.to())) {
                strokeColor = "#EF4444";
                strokeW = 2.2f;
            } else if (TreeHtml.hasChildFailure(e.to(), childFailureCache)) {
                strokeColor = "#818CF8";
                strokeW = 1.2f;
                dash = "stroke-dasharray=\"3 3\" ";
            } else if (toExecs > 0 && fromExecs > 0) {
                if (toWarning) {
                    strokeColor = "#F59E0B";
                } else {
                    final java.awt.Color col = TreeProgressMetrics.getScenarioProgressColor(e.to(), target, maxLeafOverRatio);
                    strokeColor = TreeTheme.DEFAULT.hex(col);
                }
                strokeW = 2.2f;
            } else if (!TreeHtml.isTheoreticallyExecutable(e.to(), accessibleScenarios)) {
                strokeColor = "#24304464";
                strokeW = 1.0f;
                dash = "stroke-dasharray=\"3 3\" ";
            }

            sb.append(String.format(Locale.US,
                    "  <path class=\"edge\" d=\"M %d %d C %d %d, %d %d, %d %d\" stroke=\"%s\" stroke-width=\"%.1f\" %s/>\n",
                    startX, startY, cX, startY, cX, endY, endX, endY, strokeColor, strokeW, dash));
        }

        final boolean isAllLeafs = (null != parameters) ? parameters.isAllLeafs() : false;
        final VisualNodeResolver nodeResolver =
                new VisualNodeResolver(TreeTheme.DEFAULT, accessibleScenarios, isAllLeafs, target, maxLeafOverRatio, false, true, 0.0, headerHeight(baseW));
        final List<VisualNode> visualNodes = new ArrayList<>();
        for (final NodeData n : layout.nodes()) {
            visualNodes.add(nodeResolver.resolve(n, recentNodeKeys.contains(n.id()), childFailureCache, executableCache));
        }
        sb.append(new HtmlTreeRenderer(TreeTheme.DEFAULT).renderNodes(visualNodes));

        sb.append("  </g>\n");

        final double scale = Math.max(1.0, baseW / 1200.0);
        final int localW = (int) Math.round(baseW / scale);

        sb.append(String.format(Locale.US, "  <g transform=\"scale(%.4f)\">\n", scale));
        sb.append(String.format(Locale.US,
                "    <rect x=\"0\" y=\"0\" width=\"%d\" height=\"%d\" fill=\"#1E293B\" stroke=\"#334155\" stroke-width=\"1\" />\n",
                localW, HEADER_HEIGHT_BASE));
        sb.append(String.format(Locale.US,
                "    <circle cx=\"%d\" cy=\"%d\" r=\"6\" fill=\"#38BDF8\" />\n",
                PADDING_X + 6, HEADER_HEIGHT_BASE / 2));
        sb.append(String.format(Locale.US,
                "    <text class=\"tx4\" x=\"%d\" y=\"%d\">Scenario Execution Tree Progression</text>\n",
                PADDING_X + 22, (HEADER_HEIGHT_BASE / 2) + 5));

        int rightX = localW - PADDING_X;
        final int topY = (HEADER_HEIGHT_BASE - 22) / 2;

        final String elapsedText = (isNotNull(timer))
                ? "Elapsed: " + TreeProgressMetrics.formatElapsedTime(Math.max(0L, System.currentTimeMillis() - timer.startedAt()))
                : "Elapsed: --:--:--";
        rightX = appendHeaderBadgeSvg(sb, elapsedText, rightX, topY, "#334155", "#F8FAFC");
        rightX = appendHeaderBadgeSvg(sb, totalExecs + " total execs", rightX, topY, "#334155", "#F8FAFC");

        final String zoomStr = String.format(Locale.US, "Zoom: %.1fx", zoom);
        rightX = appendHeaderBadgeSvg(sb, zoomStr, rightX, topY, "#0284C7", "#FFFFFF");

        final String covBg = (coveredLeafs == totalLeafs && totalLeafs > 0) ? "#166534" : "#B45309";
        rightX = appendHeaderBadgeSvg(sb, "Leafs: " + coveredLeafs + "/" + totalLeafs, rightX, topY, covBg, "#F8FAFC");
        rightX = appendHeaderBadgeSvg(sb, "Target: " + target, rightX, topY, "#334155", "#CBD5E1");
        appendHeaderBadgeSvg(sb, badgeText, rightX, topY, "#0284C7", "#FFFFFF");

        if (ProgressionSettings.current().treeDebugEnabled()) {
            final String debugStr = String.format(Locale.US, "dx:%.1f dy:%.1f dz:%.3f maxZ:%.1fx",
                    TreeCameraTransform.getDebugDx(),
                    TreeCameraTransform.getDebugDy(),
                    TreeCameraTransform.getDebugDz(),
                    TreeCameraTransform.getDebugMaxZoom());
            sb.append(String.format(Locale.US, "  <text class=\"tx3\" x=\"%d\" y=\"%d\" fill=\"#64748B\">%s</text>\n", PADDING_X, HEADER_HEIGHT_BASE - 5, debugStr));

            final double ox = localW / 2.0;
            final double oy = (baseH / scale) / 2.0;
            sb.append(String.format(Locale.US, "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#F59E0B\" stroke-width=\"2\" />\n",
                    ox, oy, ox - TreeCameraTransform.getDebugDx(), oy - TreeCameraTransform.getDebugDy()));
            sb.append(String.format(Locale.US, "  <circle cx=\"%.1f\" cy=\"%.1f\" r=\"3\" fill=\"#F59E0B\" />\n", ox, oy));
            sb.append(String.format(Locale.US, "  <circle cx=\"%.1f\" cy=\"%.1f\" r=\"2\" fill=\"#F8FAFC\" />\n",
                    ox - TreeCameraTransform.getDebugDx(), oy - TreeCameraTransform.getDebugDy()));
        }

        sb.append("  </g>\n");

        sb.append("</svg>\n");
        return sb.toString();
    }

    private static int appendHeaderBadgeSvg(
            final StringBuilder sb,
            final String text,
            final int rightX,
            final int topY,
            final String bg,
            final String fg) {
        final int badgeW = text.length() * 7 + 16;
        final int leftX = rightX - badgeW;
        sb.append(String.format(Locale.US,
                "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"22\" rx=\"6\" ry=\"6\" fill=\"%s\" />\n" +
                "  <text class=\"tx3\" x=\"%d\" y=\"%d\" fill=\"%s\">%s</text>\n",
                leftX, topY, badgeW, bg, leftX + 8, topY + 15, fg, text));
        return leftX - 10;
    }

    private static long evenDimension(final long dimension) {
        if (dimension >= 16384L)
            return dimension;
        return (dimension & 1L) == 0L ? dimension : dimension + 1L;
    }
}
