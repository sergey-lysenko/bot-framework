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
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols._LFD_;

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

    private SvgTreeRenderer() {
    }

    /**
     * Immutable rendering parameters captured on the main thread so SVG construction can run
     * completely asynchronously without touching global mutable state.
     */
    public record RenderContext(
            boolean isExhaustive,
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
            final boolean exhaustive = (null != parameters) && parameters.isExhaustive();
            final long maxPx = ProgressionFrames.maxFramePixels(ProgressionSettings.current().maxFramePixels());
            return new RenderContext(
                    exhaustive,
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

        int totalLeaves = 0;
        int coveredLeaves = 0;
        int totalExecs = 0;
        double maxLeafOverRatio = 0.0;

        int fullMaxCol = 0;
        double fullMinRow = Double.MAX_VALUE;
        double fullMaxRow = 0.0;

        final int totalStatsNodes = layout.nodes().size();
        for (int i = 0; i < totalStatsNodes; i++) {
            final NodeData n = layout.nodes().get(i);
            if (n.col() > fullMaxCol) fullMaxCol = n.col();
            if (n.row() < fullMinRow) fullMinRow = n.row();
            if (n.row() > fullMaxRow) fullMaxRow = n.row();
            final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
            totalExecs += execs;
            if (ScenarioType.LEAF == TreeProgressMetrics.getScenarioType(n)) {
                maxLeafOverRatio = Math.max(maxLeafOverRatio, TreeProgressMetrics.getLeafOvershootRatio(n, target));
            }
            if (n.children().isEmpty() && TreeProgressMetrics.isTerminalScenario(n)) {
                if (TreeHtml.isTheoreticallyExecutable(n, ctx, executableCache) || execs > 0) {
                    totalLeaves++;
                    if (execs >= TreeProgressMetrics.getTargetExecutions(n, target)) coveredLeaves++;
                }
            }
        }
        if (fullMinRow > fullMaxRow) {
            fullMinRow = 0.0;
            fullMaxRow = 0.0;
        }

        final int baseW;
        final int baseH;
        if (sessionBaseW > 0 && sessionBaseH > 0) {
            baseW = sessionBaseW;
            baseH = sessionBaseH;
        } else {
            baseW = TreeCameraTransform.computeFrameWidth(fullMaxCol);
            baseH = TreeCameraTransform.computeFrameHeight(fullMinRow, fullMaxRow, baseW);
        }
        final int headerH = headerHeight(baseW);

        final long maxPixels = rctx.maxPixels();
        if (baseW > ProgressionFrames.MAX_DIMENSION
                || baseH > ProgressionFrames.MAX_DIMENSION
                || (long) baseW * baseH > maxPixels) {
            throw new IllegalArgumentException(s("Rendered tree frame would exceed the configured limit (",
                    baseW, "x", baseH, ", max pixels ", maxPixels, ")"));
        }

        final TreeCameraTransform.Transform tx = (null != overrideTx) ? overrideTx : TreeCameraTransform.computeZoomTransform(layout, recentNodeKeys, target, baseW, baseH);
        final double zoom = tx.zoom();
        final double translateX = tx.translateX();
        final double translateY = tx.translateY();

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                s("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">", _LFD_),
                baseW, baseH, baseW, baseH));
        sb.append("  <defs>").append(_LFD_);
        ProgressionChrome.appendDotGridPattern(sb, headerH);
        sb.append("    <pattern id=\"known-issue-hatch\" width=\"10\" height=\"10\" patternTransform=\"rotate(45 0 0)\" patternUnits=\"userSpaceOnUse\">").append(_LFD_);
        sb.append("      <line x1=\"0\" y1=\"0\" x2=\"0\" y2=\"10\" stroke=\"#fbbf24\" stroke-width=\"2\" stroke-opacity=\"0.45\" />").append(_LFD_);
        sb.append("      <line x1=\"0\" y1=\"0\" x2=\"10\" y2=\"0\" stroke=\"#fbbf24\" stroke-width=\"2\" stroke-opacity=\"0.45\" />").append(_LFD_);
        sb.append("    </pattern>").append(_LFD_);
        sb.append(s("    <rect id=\"cb\" width=\"", CARD_WIDTH, "\" height=\"", CARD_HEIGHT, "\" rx=\"8\" ry=\"8\" />", _LFD_));
        sb.append(s("    <rect id=\"pt\" x=\"8\" y=\"", CARD_HEIGHT - 8, "\" width=\"", CARD_WIDTH - 16, "\" height=\"4\" fill=\"#0f172a\" opacity=\"0.8\" rx=\"2\" ry=\"2\" />", _LFD_));
        sb.append(s("    <rect id=\"ho\" width=\"", CARD_WIDTH, "\" height=\"", CARD_HEIGHT, "\" fill=\"url(#known-issue-hatch)\" rx=\"8\" ry=\"8\" pointer-events=\"none\" />", _LFD_));
        sb.append("  </defs>").append(_LFD_);
        sb.append("  <style>").append(_LFD_);
        sb.append("    .tx1 { font-family: monospace; font-size: 10px; font-weight: bold; }").append(_LFD_);
        sb.append("    .tx2 { font-family: sans-serif; font-size: 12px; }").append(_LFD_);
        sb.append("    .tx3 { font-family: monospace; font-weight: bold; font-size: 11px; }").append(_LFD_);
        sb.append("    .tx4 { font-family: sans-serif; font-weight: bold; font-size: 15px; fill: #F8FAFC; }").append(_LFD_);
        sb.append("    .pb { fill: #0f172a; opacity: 0.8; }").append(_LFD_);
        sb.append("    .edge { fill: none; }").append(_LFD_);
        sb.append("  </style>").append(_LFD_);

        ProgressionChrome.appendCanvasBackground(sb, baseW, baseH, headerH);

        sb.append(String.format(Locale.US,
                s("  <g transform=\"translate(%.2f, %.2f) scale(%.4f)\">", _LFD_),
                translateX, translateY, zoom));

        final List<Edge> sortedEdges = new ArrayList<>(layout.edges());
        sortedEdges.sort(Comparator.comparingInt(e -> {
            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            return (toExecs > 0 && fromExecs > 0) ? 1 : 0;
        }));

        for (final Edge e : sortedEdges) {
            final int startX = (int) Math.round(e.from().visualCol() * COL_WIDTH + CARD_WIDTH + PADDING_X);
            final int startY = (int) Math.round(e.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerH + PADDING_Y);
            final int endX = (int) Math.round(e.to().visualCol() * COL_WIDTH + PADDING_X);
            final int endY = (int) Math.round(e.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerH + PADDING_Y);
            final int cX = (startX + endX) / 2;

            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            final NodeEventClassifier.Classification toCls = NodeEventClassifier.classify(e.to());
            final boolean toRegression = toCls.hasRegression();
            final boolean toFailure = toCls.hasFailure();
            final boolean toRegressionWarning = toCls.hasRegressionWarning();
            final boolean toWarning = toCls.hasWarning();
            final boolean toNotice = !toWarning && toCls.hasNotice();
            final boolean isRecent = recentNodeKeys.contains(e.to().id());

            String strokeColor = "#334155";
            String strokeOpacity = "stroke-opacity=\"0.55\" ";
            float strokeW = 1.2f;
            String dash = "";

            if (isRecent) {
                sb.append(String.format(Locale.US,
                        s("  <path class=\"edge\" fill=\"none\" d=\"M %d %d C %d %d, %d %d, %d %d\" stroke=\"#38BDF8\" stroke-opacity=\"0.35\" stroke-width=\"5.0\" />", _LFD_),
                        startX, startY, cX, startY, cX, endY, endX, endY));
                strokeColor = "#38BDF8";
                strokeOpacity = "";
                strokeW = 3.0f;
            } else if (toRegression) {
                strokeColor = "#E879F9";
                strokeOpacity = "";
                strokeW = 2.5f;
            } else if (toFailure) {
                strokeColor = "#EF4444";
                strokeOpacity = "";
                strokeW = 2.2f;
            } else if (TreeHtml.hasChildFailure(e.to(), childFailureCache)) {
                strokeColor = "#818CF8";
                strokeOpacity = "";
                strokeW = 1.2f;
                dash = "stroke-dasharray=\"3 3\" ";
            } else if (toExecs > 0 && fromExecs > 0) {
                if (toRegressionWarning) {
                    strokeColor = "#F472B6";
                } else if (toWarning) {
                    strokeColor = "#F59E0B";
                } else if (toNotice) {
                    strokeColor = "#06B6D4";
                } else {
                    final java.awt.Color col = TreeProgressMetrics.getScenarioProgressColor(e.to(), target, maxLeafOverRatio, rctx.isExhaustive());
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
                    s("  <path class=\"edge\" fill=\"none\" d=\"M %d %d C %d %d, %d %d, %d %d\" stroke=\"%s\" %sstroke-width=\"%.1f\" %s/>", _LFD_),
                    startX, startY, cX, startY, cX, endY, endX, endY, strokeColor, strokeOpacity, strokeW, dash));
        }

        final boolean isExhaustive = rctx.isExhaustive();
        final VisualNodeResolver nodeResolver =
                new VisualNodeResolver(TreeTheme.DEFAULT, ctx, isExhaustive, target, maxLeafOverRatio, false, true, 0.0, headerH);
        final List<VisualNode> visualNodes = new ArrayList<>();
        for (final NodeData n : layout.nodes()) {
            visualNodes.add(nodeResolver.resolve(n, recentNodeKeys.contains(n.id()), childFailureCache, executableCache));
        }
        sb.append(new HtmlTreeRenderer(TreeTheme.DEFAULT).renderNodes(visualNodes));

        sb.append("  </g>").append(_LFD_);

        final double scale = baseW / 1200.0;
        final int localW = (int) Math.round(baseW / scale);

        sb.append(String.format(Locale.US, s("  <g transform=\"scale(%.4f)\">", _LFD_), scale));
        ProgressionChrome.appendHeaderBar(sb, localW);

        final int hudMarginX = 24;
        int rightX = localW - hudMarginX;
        final int topY = (HEADER_HEIGHT_BASE - ProgressionChrome.BADGE_HEIGHT) / 2;

        // Right-to-left placement -> left-to-right display:
        // Test #N | Leaves: X/Y (P%) | Target: T | Zoom: Z.ZZx | M execs | ETA: MM:SS | Elapsed: HH:MM:SS
        rightX = ProgressionChrome.appendBorderedBadge(sb, rctx.elapsedBadge(), rightX, topY, ProgressionChrome.BADGE_SLATE);
        final ProgressionChrome.EtaBadge etaBadge = rctx.etaBadge();
        rightX = ProgressionChrome.appendBorderedBadge(sb, etaBadge.text(), rightX, topY, etaBadge.style());
        rightX = ProgressionChrome.appendBorderedBadge(sb, s(totalExecs, " execs"), rightX, topY, ProgressionChrome.BADGE_SLATE);

        final String zoomStr = String.format(Locale.US, "Zoom: %.2fx", zoom);
        rightX = ProgressionChrome.appendBorderedBadge(sb, zoomStr, rightX, topY, ProgressionChrome.BADGE_SKY);

        rightX = ProgressionChrome.appendBorderedBadge(sb, s("Target: ", target), rightX, topY, ProgressionChrome.BADGE_SLATE);
        rightX = ProgressionChrome.appendBorderedBadge(
                sb,
                ProgressionChrome.formatLeavesBadge(coveredLeaves, totalLeaves),
                rightX,
                topY,
                ProgressionChrome.leavesBadgeStyle(coveredLeaves, totalLeaves));
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
            final int totalCols = layout.nodes().isEmpty() ? 0 : (fullMaxCol + 1);
            final String debugStr = String.format(Locale.US, "dx:%.1f dy:%.1f dz:%.3f maxZ:%.2fx cols:%d",
                    rctx.debugDx(),
                    rctx.debugDy(),
                    rctx.debugDz(),
                    rctx.debugMaxZoom(),
                    totalCols);
            sb.append("    <g id=\"tree-debug-overlay\">").append(_LFD_);
            sb.append(String.format(Locale.US, s("      <text class=\"tx3\" x=\"%d\" y=\"%d\" fill=\"#64748B\">%s</text>", _LFD_), PADDING_X, HEADER_HEIGHT_BASE - 5, debugStr));

            final double ox = localW / 2.0;
            final double oy = (baseH / scale) / 2.0;
            sb.append(String.format(Locale.US, s("      <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#F59E0B\" stroke-width=\"2\" />", _LFD_),
                    ox, oy, ox - rctx.debugDx(), oy - rctx.debugDy()));
            sb.append(String.format(Locale.US, s("      <circle cx=\"%.1f\" cy=\"%.1f\" r=\"3\" fill=\"#F59E0B\" />", _LFD_), ox, oy));
            sb.append(String.format(Locale.US, s("      <circle cx=\"%.1f\" cy=\"%.1f\" r=\"2\" fill=\"#F8FAFC\" />", _LFD_),
                    ox - rctx.debugDx(), oy - rctx.debugDy()));
            sb.append("    </g>").append(_LFD_);
        }

        final works.lysenko.base.output.SummaryPlaque plaque = layout.plaque() != null
                ? layout.plaque()
                : works.lysenko.base.output.SummaryPlaque.computeFromNodes(
                        layout.nodes(),
                        rctx.runConfig(),
                        ctx,
                        executableCache,
                        childFailureCache);
        final int plaqueMarginX = 16;
        final int plaqueW = Math.max(810, localW - plaqueMarginX * 2);
        renderPlaqueSvg(plaque, sb, plaqueMarginX, HEADER_HEIGHT_BASE + 16, plaqueW);

        sb.append("  </g>").append(_LFD_);

        sb.append("</svg>").append(_LFD_);
        return sb.toString();
    }

    private record PlaqueConfigItem(String label, String value, String valueColor) {
        double estimatedWidthPx() {
            return (label.length() + value.length()) * 6.8 + 6.0;
        }
    }

    private static void renderPlaqueSvg(
            final works.lysenko.base.output.SummaryPlaque plaque,
            final StringBuilder sb,
            final int x,
            final int y,
            final int plaqueW) {
        if (plaque == null) return;

        final List<PlaqueConfigItem> configItems = new ArrayList<>();
        if (hasText(plaque.suite())) configItems.add(new PlaqueConfigItem("SUITE:", plaque.suite(), "#F8FAFC"));
        if (hasText(plaque.pool())) configItems.add(new PlaqueConfigItem("POOL:", plaque.pool(), "#F8FAFC"));
        if (hasText(plaque.domain())) configItems.add(new PlaqueConfigItem("DOMAIN:", plaque.domain(), "#F8FAFC"));
        if (hasText(plaque.origin())) configItems.add(new PlaqueConfigItem("ORIGIN:", plaque.origin(), "#F8FAFC"));
        if (hasText(plaque.tag())) configItems.add(new PlaqueConfigItem("TAG:", plaque.tag(), "#F8FAFC"));
        if (hasText(plaque.pathsSummary())) configItems.add(new PlaqueConfigItem("PATHS:", plaque.pathsSummary(), "#F8FAFC"));
        if (plaque.knownIssues() > 0) configItems.add(new PlaqueConfigItem("KNOWN ISSUES:", String.valueOf(plaque.knownIssues()), "#C084FC"));

        final boolean hasMode = hasText(plaque.mode());
        final int modeBadgeW = hasMode ? (int) Math.ceil(plaque.mode().length() * 6.8) + 18 : 0;
        final int modeBadgeX = plaqueW - 16 - modeBadgeW;

        final int firstLineX = 176;
        final double itemGapPx = 12.0;
        final double firstLineMaxPx = Math.max(120.0, (hasMode ? modeBadgeX - 16 : plaqueW - 16) - firstLineX);
        final double nextLineMaxPx = Math.max(120.0, plaqueW - 32);

        final List<List<PlaqueConfigItem>> configLines = new ArrayList<>();
        List<PlaqueConfigItem> currentLine = new ArrayList<>();
        double currentLinePx = 0.0;
        for (final PlaqueConfigItem item : configItems) {
            final double maxPx = configLines.isEmpty() ? firstLineMaxPx : nextLineMaxPx;
            final double sepPx = currentLine.isEmpty() ? 0.0 : itemGapPx;
            final double itemPx = item.estimatedWidthPx();
            if (!currentLine.isEmpty() && currentLinePx + sepPx + itemPx > maxPx) {
                configLines.add(currentLine);
                currentLine = new ArrayList<>();
                currentLine.add(item);
                currentLinePx = itemPx;
            } else {
                currentLine.add(item);
                currentLinePx += sepPx + itemPx;
            }
        }
        if (!currentLine.isEmpty()) {
            configLines.add(currentLine);
        }

        final List<works.lysenko.base.output.SummaryPlaque.MetricColumn> visibleCols = plaque.visibleTreeColumns();
        final int extraLines = Math.max(0, configLines.size() - 1);
        final int yOffset = extraLines * 14;
        final int leftEdge = 96;
        final int plaqueH = 105 + yOffset;

        sb.append(String.format(Locale.US,
                s("    <g id=\"exec-stats-plaque\" transform=\"translate(%d, %d)\">", _LFD_), x, y));

        sb.append(String.format(Locale.US,
                s("      <rect width=\"%d\" height=\"%d\" rx=\"8\" ry=\"8\" fill=\"#0F172A\" fill-opacity=\"0.92\" stroke=\"#38BDF8\" stroke-width=\"1.5\" stroke-opacity=\"0.4\" />", _LFD_),
                plaqueW, plaqueH));

        sb.append("      <text x=\"16\" y=\"22\" fill=\"#F8FAFC\" font-size=\"12\" font-weight=\"bold\" font-family=\"sans-serif\">⚡ Execution Statistics</text>").append(_LFD_);

        if (hasMode) {
            sb.append(String.format(Locale.US,
                    s("      <rect x=\"%d\" y=\"9\" width=\"%d\" height=\"18\" rx=\"9\" ry=\"9\" fill=\"#A855F7\" fill-opacity=\"0.15\" stroke=\"#C084FC\" stroke-opacity=\"0.35\" stroke-width=\"1\" />", _LFD_),
                    modeBadgeX, modeBadgeW));
            sb.append(String.format(Locale.US,
                    s("      <text x=\"%d\" y=\"21\" fill=\"#C084FC\" font-size=\"9.5\" font-weight=\"bold\" font-family=\"monospace\" text-anchor=\"middle\">%s</text>", _LFD_),
                    modeBadgeX + (modeBadgeW / 2), ProgressionChrome.escapeXml(plaque.mode())));
        }

        for (int i = 0; i < configLines.size(); i++) {
            final int lineX = (i == 0) ? firstLineX : 16;
            final int lineY = 22 + i * 14;
            final List<PlaqueConfigItem> lineItems = configLines.get(i);
            sb.append(String.format(Locale.US,
                    "      <text x=\"%d\" y=\"%d\" font-size=\"10\" font-family=\"monospace\">",
                    lineX, lineY));
            for (int j = 0; j < lineItems.size(); j++) {
                final PlaqueConfigItem item = lineItems.get(j);
                if (j > 0) {
                    sb.append(String.format(Locale.US,
                            "<tspan dx=\"12\" fill=\"#94A3B8\" font-weight=\"normal\">%s</tspan>",
                            ProgressionChrome.escapeXml(item.label())));
                } else {
                    sb.append(String.format(Locale.US,
                            "<tspan fill=\"#94A3B8\" font-weight=\"normal\">%s</tspan>",
                            ProgressionChrome.escapeXml(item.label())));
                }
                sb.append(String.format(Locale.US,
                        "<tspan dx=\"4\" fill=\"%s\" font-weight=\"bold\">%s</tspan>",
                        item.valueColor(), ProgressionChrome.escapeXml(item.value())));
            }
            sb.append("</text>").append(_LFD_);
        }

        final int divY = 32 + yOffset;
        sb.append(String.format(Locale.US,
                s("      <line x1=\"12\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#334155\" stroke-width=\"1\" />", _LFD_),
                divY, plaqueW - 12, divY));

        final double slotW = (double) (plaqueW - 16 - leftEdge) / Math.max(1, visibleCols.size());
        final int headerRowY = 48 + yOffset;
        final int amountRowY = 68 + yOffset;
        final int pctRowY = 88 + yOffset;

        for (int c = 0; c < visibleCols.size(); c++) {
            final works.lysenko.base.output.SummaryPlaque.MetricColumn col = visibleCols.get(c);
            final int colX = (int) Math.round(leftEdge + (c + 0.5) * slotW);
            sb.append(String.format(Locale.US,
                    s("      <text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"8.5\" font-weight=\"bold\" font-family=\"monospace\" text-anchor=\"middle\">%s</text>", _LFD_),
                    colX, headerRowY, col.colorHex(), ProgressionChrome.escapeXml(col.label().toUpperCase(Locale.ROOT))));
        }

        sb.append(String.format(Locale.US,
                s("      <text x=\"16\" y=\"%d\" fill=\"#CBD5E1\" font-size=\"10\" font-weight=\"bold\" font-family=\"sans-serif\">Amount</text>", _LFD_),
                amountRowY));
        for (int c = 0; c < visibleCols.size(); c++) {
            final works.lysenko.base.output.SummaryPlaque.MetricColumn col = visibleCols.get(c);
            final int colX = (int) Math.round(leftEdge + (c + 0.5) * slotW);
            sb.append(String.format(Locale.US,
                    s("      <text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"11\" font-weight=\"bold\" font-family=\"monospace\" text-anchor=\"middle\">%d</text>", _LFD_),
                    colX, amountRowY, col.colorHex(), col.amount()));
        }

        sb.append(String.format(Locale.US,
                s("      <text x=\"16\" y=\"%d\" fill=\"#CBD5E1\" font-size=\"10\" font-weight=\"bold\" font-family=\"sans-serif\">Percentage</text>", _LFD_),
                pctRowY));
        for (int c = 0; c < visibleCols.size(); c++) {
            final works.lysenko.base.output.SummaryPlaque.MetricColumn col = visibleCols.get(c);
            final int colX = (int) Math.round(leftEdge + (c + 0.5) * slotW);
            sb.append(String.format(Locale.US,
                    s("      <text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"10\" font-weight=\"bold\" font-family=\"monospace\" text-anchor=\"middle\">%s</text>", _LFD_),
                    colX, pctRowY, col.colorHex(), col.percentage()));
        }

        sb.append("    </g>").append(_LFD_);
    }

    private static boolean hasText(final String text) {
        return text != null && !text.isBlank();
    }
}
