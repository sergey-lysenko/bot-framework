package works.lysenko.base.output.tree;

import works.lysenko.base.output.ProgressionFrames;
import works.lysenko.base.output.ProgressionSettings;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.svg.ProgressionChrome;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ScenarioType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static works.lysenko.Base.parameters;

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

    /**
     * Immutable rendering parameters captured on the main thread so SVG construction can run
     * completely asynchronously without touching global mutable state.
     */
    public record RenderContext(
            boolean isAllLeafs,
            long maxPixels,
            String elapsedBadge,
            ProgressionChrome.EtaBadge etaBadge,
            works.lysenko.base.output.SummaryPlaque.RunConfig runConfig,
            boolean treeDebugEnabled,
            double debugDx,
            double debugDy,
            double debugDz,
            double debugMaxZoom) {

        public static RenderContext captureCurrent() {
            final boolean allLeafs = (null != parameters) && parameters.isAllLeafs();
            final long maxPx = ProgressionFrames.maxFramePixels(ProgressionSettings.current().maxFramePixels());
            return new RenderContext(
                    allLeafs,
                    maxPx,
                    ProgressionChrome.formatElapsedBadge(),
                    ProgressionChrome.resolveEtaBadge(),
                    works.lysenko.base.output.SummaryPlaque.RunConfig.fromParameters(parameters),
                    ProgressionSettings.current().treeDebugEnabled(),
                    TreeCameraTransform.getDebugDx(),
                    TreeCameraTransform.getDebugDy(),
                    TreeCameraTransform.getDebugDz(),
                    TreeCameraTransform.getDebugMaxZoom());
        }
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
        return renderTreeProgressionSvg(
                layout,
                target,
                badgeText,
                recentNodeKeys,
                accessibleScenarios,
                accessibleCtx,
                overrideTx,
                cachedExecutable,
                sessionBaseW,
                sessionBaseH,
                RenderContext.captureCurrent());
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
            final int sessionBaseH,
            final RenderContext renderContext) {

        final RenderContext rctx = (null != renderContext) ? renderContext : RenderContext.captureCurrent();
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
                    (long) Math.ceil((fullMaxRow + 1.0) * ROW_HEIGHT + headerHeight(baseW) + PADDING_Y + BOTTOM_PADDING)));
        }
        final int headerH = headerHeight(baseW);

        final long maxPixels = rctx.maxPixels();
        if (baseW > ProgressionFrames.MAX_DIMENSION
                || baseH > ProgressionFrames.MAX_DIMENSION
                || (long) baseW * baseH > maxPixels) {
            throw new IllegalArgumentException("Rendered tree frame would exceed the configured limit ("
                    + baseW + "x" + baseH + ", max pixels " + maxPixels + ")");
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
        ProgressionChrome.appendDotGridPattern(sb, headerH);
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
        sb.append("    .tx2 { font-family: sans-serif; font-size: 12px; }\n");
        sb.append("    .tx3 { font-family: monospace; font-weight: bold; font-size: 11px; }\n");
        sb.append("    .tx4 { font-family: sans-serif; font-weight: bold; font-size: 15px; fill: #F8FAFC; }\n");
        sb.append("    .pb { fill: #0f172a; opacity: 0.8; }\n");
        sb.append("    .edge { fill: none; }\n");
        sb.append("  </style>\n");

        ProgressionChrome.appendCanvasBackground(sb, baseW, baseH, headerH);

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
            final int startY = (int) Math.round(e.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerH + PADDING_Y);
            final int endX = e.to().col() * COL_WIDTH + PADDING_X;
            final int endY = (int) Math.round(e.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerH + PADDING_Y);
            final int cX = (startX + endX) / 2;

            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            final boolean toWarning = TreeHtml.hasWarning(e.to());
            final boolean toNotice = !toWarning && TreeHtml.hasNotice(e.to());
            final boolean isRecent = recentNodeKeys.contains(e.to().id());

            String strokeColor = "#334155";
            String strokeOpacity = "stroke-opacity=\"0.55\" ";
            float strokeW = 1.2f;
            String dash = "";

            if (isRecent) {
                sb.append(String.format(Locale.US,
                        "  <path class=\"edge\" fill=\"none\" d=\"M %d %d C %d %d, %d %d, %d %d\" stroke=\"#38BDF8\" stroke-opacity=\"0.35\" stroke-width=\"5.0\" />\n",
                        startX, startY, cX, startY, cX, endY, endX, endY));
                strokeColor = "#38BDF8";
                strokeOpacity = "";
                strokeW = 3.0f;
            } else if (TreeHtml.hasFailure(e.to())) {
                strokeColor = "#EF4444";
                strokeOpacity = "";
                strokeW = 2.2f;
            } else if (TreeHtml.hasChildFailure(e.to(), childFailureCache)) {
                strokeColor = "#818CF8";
                strokeOpacity = "";
                strokeW = 1.2f;
                dash = "stroke-dasharray=\"3 3\" ";
            } else if (toExecs > 0 && fromExecs > 0) {
                if (toWarning) {
                    strokeColor = "#F59E0B";
                } else if (toNotice) {
                    strokeColor = "#06B6D4";
                } else {
                    final java.awt.Color col = TreeProgressMetrics.getScenarioProgressColor(e.to(), target, maxLeafOverRatio, rctx.isAllLeafs());
                    strokeColor = TreeTheme.DEFAULT.hex(col);
                }
                strokeOpacity = "";
                strokeW = 2.2f;
            } else if (!TreeHtml.isTheoreticallyExecutable(e.to(), ctx, executableCache)) {
                strokeColor = "#243044";
                strokeOpacity = "stroke-opacity=\"0.39\" ";
                strokeW = 1.0f;
                dash = "stroke-dasharray=\"3 3\" ";
            }

            sb.append(String.format(Locale.US,
                    "  <path class=\"edge\" fill=\"none\" d=\"M %d %d C %d %d, %d %d, %d %d\" stroke=\"%s\" %sstroke-width=\"%.1f\" %s/>\n",
                    startX, startY, cX, startY, cX, endY, endX, endY, strokeColor, strokeOpacity, strokeW, dash));
        }

        final boolean isAllLeafs = rctx.isAllLeafs();
        final VisualNodeResolver nodeResolver =
                new VisualNodeResolver(TreeTheme.DEFAULT, ctx, isAllLeafs, target, maxLeafOverRatio, false, true, 0.0, headerH);
        final List<VisualNode> visualNodes = new ArrayList<>();
        for (final NodeData n : layout.nodes()) {
            visualNodes.add(nodeResolver.resolve(n, recentNodeKeys.contains(n.id()), childFailureCache, executableCache));
        }
        sb.append(new HtmlTreeRenderer(TreeTheme.DEFAULT).renderNodes(visualNodes));

        sb.append("  </g>\n");

        final double scale = baseW / 1200.0;
        final int localW = (int) Math.round(baseW / scale);

        sb.append(String.format(Locale.US, "  <g transform=\"scale(%.4f)\">\n", scale));
        ProgressionChrome.appendHeaderBar(sb, localW);

        final int hudMarginX = 24;
        int rightX = localW - hudMarginX;
        final int topY = (HEADER_HEIGHT_BASE - ProgressionChrome.BADGE_HEIGHT) / 2;

        // Right-to-left placement -> left-to-right display:
        // Test #N | Leafs: X/Y (P%) | Target: T | Zoom: Z.Zx | M execs | ETA: MM:SS | Elapsed: HH:MM:SS
        rightX = ProgressionChrome.appendBorderedBadge(sb, rctx.elapsedBadge(), rightX, topY, ProgressionChrome.BADGE_SLATE);
        final ProgressionChrome.EtaBadge etaBadge = rctx.etaBadge();
        rightX = ProgressionChrome.appendBorderedBadge(sb, etaBadge.text(), rightX, topY, etaBadge.style());
        rightX = ProgressionChrome.appendBorderedBadge(sb, totalExecs + " execs", rightX, topY, ProgressionChrome.BADGE_SLATE);

        final String zoomStr = String.format(Locale.US, "Zoom: %.1fx", zoom);
        rightX = ProgressionChrome.appendBorderedBadge(sb, zoomStr, rightX, topY, ProgressionChrome.BADGE_SKY);

        rightX = ProgressionChrome.appendBorderedBadge(sb, "Target: " + target, rightX, topY, ProgressionChrome.BADGE_SLATE);
        rightX = ProgressionChrome.appendBorderedBadge(
                sb,
                ProgressionChrome.formatLeafsBadge(coveredLeafs, totalLeafs),
                rightX,
                topY,
                ProgressionChrome.leafsBadgeStyle(coveredLeafs, totalLeafs));
        rightX = ProgressionChrome.appendBorderedBadge(sb, badgeText, rightX, topY, ProgressionChrome.BADGE_SKY);

        final String titleText;
        if (rightX >= 330) {
            titleText = "Scenario Execution Tree Progression";
        } else if (rightX >= 240) {
            titleText = "Scenario Execution Tree";
        } else if (rightX >= 100) {
            titleText = "Tree";
        } else {
            titleText = "";
        }

        ProgressionChrome.appendHeaderTitle(sb, hudMarginX, titleText);

        if (rctx.treeDebugEnabled()) {
            final String debugStr = String.format(Locale.US, "dx:%.1f dy:%.1f dz:%.3f maxZ:%.1fx",
                    rctx.debugDx(),
                    rctx.debugDy(),
                    rctx.debugDz(),
                    rctx.debugMaxZoom());
            sb.append("    <g id=\"tree-debug-overlay\">\n");
            sb.append(String.format(Locale.US, "      <text class=\"tx3\" x=\"%d\" y=\"%d\" fill=\"#64748B\">%s</text>\n", PADDING_X, HEADER_HEIGHT_BASE - 5, debugStr));

            final double ox = localW / 2.0;
            final double oy = (baseH / scale) / 2.0;
            sb.append(String.format(Locale.US, "      <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#F59E0B\" stroke-width=\"2\" />\n",
                    ox, oy, ox - rctx.debugDx(), oy - rctx.debugDy()));
            sb.append(String.format(Locale.US, "      <circle cx=\"%.1f\" cy=\"%.1f\" r=\"3\" fill=\"#F59E0B\" />\n", ox, oy));
            sb.append(String.format(Locale.US, "      <circle cx=\"%.1f\" cy=\"%.1f\" r=\"2\" fill=\"#F8FAFC\" />\n",
                    ox - rctx.debugDx(), oy - rctx.debugDy()));
            sb.append("    </g>\n");
        }

        final works.lysenko.base.output.SummaryPlaque plaque = layout.plaque() != null
                ? layout.plaque()
                : works.lysenko.base.output.SummaryPlaque.computeFromNodes(
                        layout.nodes(),
                        rctx.runConfig(),
                        ctx,
                        executableCache,
                        childFailureCache);
        renderPlaqueSvg(plaque, sb, 16, HEADER_HEIGHT_BASE + 16);

        sb.append("  </g>\n");

        sb.append("</svg>\n");
        return sb.toString();
    }

    private static void renderPlaqueSvg(final works.lysenko.base.output.SummaryPlaque plaque, final StringBuilder sb, final int x, final int y) {
        if (plaque == null) return;

        final List<String> configItems = new ArrayList<>();
        if (hasText(plaque.suite())) configItems.add("SUITE: " + plaque.suite());
        if (hasText(plaque.pool())) configItems.add("POOL: " + plaque.pool());
        if (hasText(plaque.domain())) configItems.add("DOMAIN: " + plaque.domain());
        if (hasText(plaque.origin())) configItems.add("ORIGIN: " + plaque.origin());
        if (hasText(plaque.tag())) configItems.add("TAG: " + plaque.tag());
        if (hasText(plaque.pathsSummary())) configItems.add("PATHS: " + plaque.pathsSummary());
        if (plaque.knownIssues() > 0) configItems.add("KNOWN ISSUES: " + plaque.knownIssues());

        final int maxFirstLineChars = 88;
        final int maxNextLineChars = 112;
        final List<String> configLines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (final String item : configItems) {
            final int maxChars = configLines.isEmpty() ? maxFirstLineChars : maxNextLineChars;
            final int sepLen = currentLine.length() == 0 ? 0 : 2;
            if (currentLine.length() > 0 && currentLine.length() + sepLen + item.length() > maxChars) {
                configLines.add(currentLine.toString());
                currentLine = new StringBuilder(item);
            } else {
                if (sepLen > 0) currentLine.append("  ");
                currentLine.append(item);
            }
        }
        if (currentLine.length() > 0) {
            configLines.add(currentLine.toString());
        }

        final int extraLines = Math.max(0, configLines.size() - 1);
        final int yOffset = extraLines * 14;
        final int plaqueW = 760;
        final int plaqueH = 105 + yOffset;

        sb.append(String.format(Locale.US,
                "    <g id=\"exec-stats-plaque\" transform=\"translate(%d, %d)\">\n", x, y));

        sb.append(String.format(Locale.US,
                "      <rect width=\"%d\" height=\"%d\" rx=\"8\" ry=\"8\" fill=\"#0F172A\" fill-opacity=\"0.92\" stroke=\"#38BDF8\" stroke-width=\"1.5\" stroke-opacity=\"0.4\" />\n",
                plaqueW, plaqueH));

        sb.append("      <text x=\"16\" y=\"22\" fill=\"#F8FAFC\" font-size=\"12\" font-weight=\"bold\" font-family=\"sans-serif\">⚡ Execution Statistics</text>\n");

        for (int i = 0; i < configLines.size(); i++) {
            final int lineX = (i == 0) ? 170 : 16;
            final int lineY = 22 + i * 14;
            sb.append(String.format(Locale.US,
                    "      <text x=\"%d\" y=\"%d\" fill=\"#38BDF8\" font-size=\"10\" font-weight=\"bold\" font-family=\"monospace\">%s</text>\n",
                    lineX, lineY, ProgressionChrome.escapeXml(configLines.get(i))));
        }

        final int divY = 32 + yOffset;
        sb.append(String.format(Locale.US,
                "      <line x1=\"12\" y1=\"%d\" x2=\"748\" y2=\"%d\" stroke=\"#334155\" stroke-width=\"1\" />\n",
                divY, divY));

        final boolean hasHidden = plaque.hidden() > 0;
        final int totalPassed = plaque.totalPassed();
        final String passedPctStr = works.lysenko.base.output.SummaryPlaque.formatPct(totalPassed, plaque.total());

        final String[] cols = {"TOTAL", "EXECUTABLE", "OFF", "EXECUTED", "UPSET", "PASSED", "WARNING", "FAILED"};
        final String[] colors = {"#38BDF8", "#C084FC", "#94A3B8", "#60A5FA", "#818CF8", "#22C55E", "#F59E0B", "#EF4444"};
        final int[] amounts = {
                plaque.total(),
                plaque.executable(),
                plaque.off(),
                plaque.executed(),
                plaque.upset(),
                totalPassed,
                plaque.warning(),
                plaque.failed()
        };
        final String[] pctStrs = {
                works.lysenko.base.output.SummaryPlaque.formatPct(plaque.total(), plaque.total()),
                works.lysenko.base.output.SummaryPlaque.formatPct(plaque.executable(), plaque.total()),
                works.lysenko.base.output.SummaryPlaque.formatPct(plaque.off(), plaque.total()),
                works.lysenko.base.output.SummaryPlaque.formatPct(plaque.executed(), plaque.total()),
                works.lysenko.base.output.SummaryPlaque.formatPct(plaque.upset(), plaque.total()),
                passedPctStr,
                works.lysenko.base.output.SummaryPlaque.formatPct(plaque.warning(), plaque.total()),
                works.lysenko.base.output.SummaryPlaque.formatPct(plaque.failed(), plaque.total())
        };

        final int colStart = 90;
        final int colStep = 82;
        final int headerRowY = 48 + yOffset;
        final int amountRowY = 68 + yOffset;
        final int pctRowY = 88 + yOffset;

        for (int c = 0; c < cols.length; c++) {
            final int colX = colStart + c * colStep;
            sb.append(String.format(Locale.US,
                    "      <text x=\"%d\" y=\"%d\" fill=\"#94A3B8\" font-size=\"9\" font-weight=\"bold\" font-family=\"monospace\" text-anchor=\"middle\">%s</text>\n",
                    colX, headerRowY, cols[c]));
        }

        sb.append(String.format(Locale.US,
                "      <text x=\"16\" y=\"%d\" fill=\"#CBD5E1\" font-size=\"10\" font-weight=\"bold\" font-family=\"sans-serif\">Amount</text>\n",
                amountRowY));
        for (int c = 0; c < amounts.length; c++) {
            final int colX = colStart + c * colStep;
            if (c == 5 && hasHidden) {
                sb.append(String.format(Locale.US,
                        "      <text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"bold\" font-family=\"monospace\" text-anchor=\"middle\">" +
                        "<tspan fill=\"#22C55E\">%d </tspan>" +
                        "<tspan fill=\"#94A3B8\" font-weight=\"normal\" font-size=\"10\">(%d)</tspan>" +
                        "</text>\n",
                        colX, amountRowY, totalPassed, plaque.hidden()));
            } else {
                sb.append(String.format(Locale.US,
                        "      <text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"11\" font-weight=\"bold\" font-family=\"monospace\" text-anchor=\"middle\">%d</text>\n",
                        colX, amountRowY, colors[c], amounts[c]));
            }
        }

        sb.append(String.format(Locale.US,
                "      <text x=\"16\" y=\"%d\" fill=\"#CBD5E1\" font-size=\"10\" font-weight=\"bold\" font-family=\"sans-serif\">Percentage</text>\n",
                pctRowY));
        for (int c = 0; c < pctStrs.length; c++) {
            final int colX = colStart + c * colStep;
            sb.append(String.format(Locale.US,
                    "      <text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"10\" font-weight=\"bold\" font-family=\"monospace\" text-anchor=\"middle\">%s</text>\n",
                    colX, pctRowY, colors[c], pctStrs[c]));
        }

        sb.append("    </g>\n");
    }

    private static boolean hasText(final String text) {
        return text != null && !text.isBlank();
    }

    private static long evenDimension(final long dimension) {
        if (dimension >= 16384L)
            return dimension;
        return (dimension & 1L) == 0L ? dimension : dimension + 1L;
    }
}
