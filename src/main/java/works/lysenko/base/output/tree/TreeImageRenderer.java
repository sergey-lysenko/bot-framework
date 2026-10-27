package works.lysenko.base.output.tree;

import works.lysenko.base.output.ProgressionFrames;
import works.lysenko.base.output.ProgressionSettings;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logTrace;
import static works.lysenko.Base.parameters;
import static works.lysenko.Base.timer;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Renders scenario tree progression layouts as {@link BufferedImage} instances.
 */
public final class TreeImageRenderer {

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

    private static final Color BG_COLOR = new Color(0x0F, 0x17, 0x2A);
    private static final Color HEADER_BG = new Color(0x1E, 0x29, 0x3B);
    private static final Color HEADER_BORDER = new Color(0x33, 0x41, 0x55);
    private static final Color GRID_DOT = new Color(0x1E, 0x29, 0x3B, 180);
    private static final Color TEXT_WHITE = new Color(0xF8, 0xFA, 0xFC);
    private static final Color ACCENT_CYAN = new Color(0x38, 0xBD, 0xF8);
    private static final Color UNVISITED_EDGE = new Color(0x33, 0x41, 0x55, 140);
    private static final Color WARNING_AMBER = new Color(0xF5, 0x9E, 0x0B);
    private static final Color RED_FAILED = new Color(0xEF, 0x44, 0x44);
    private static final Color INDIGO_UPSET = new Color(0x81, 0x8C, 0xF8);
    private static final Color EXCLUDED_EDGE = new Color(0x24, 0x30, 0x44, 100);

    private static final Stroke DASHED_EDGE_STROKE = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f);
    private static final Stroke DASHED_UPSET_STROKE = new BasicStroke(
            1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f);

    private TreeImageRenderer() {
    }

    @FunctionalInterface
    public interface TexturePaintCreatedCallback {
        void onCreated(TexturePaint paint);
    }

    public static BufferedImage renderTreeProgression(
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
            final TexturePaint cachedDotPaint,
            final TexturePaintCreatedCallback dotPaintCallback) {

        final long tRenderStart = System.currentTimeMillis();
        final TreeHtml.AccessibleContext ctx = (null != accessibleCtx) ? accessibleCtx : new TreeHtml.AccessibleContext(accessibleScenarios);

        final Map<NodeData, Boolean> executableCache = (null != cachedExecutable) ? cachedExecutable : new HashMap<>();
        final Map<NodeData, Boolean> childFailureCache = new HashMap<>();

        int totalLeafs = 0;
        int coveredLeafs = 0;
        int totalExecs = 0;
        double maxLeafOverRatio = 0.0;

        final int totalStatsNodes = layout.nodes().size();
        final Set<Integer> statsMilestones = new HashSet<>();
        if (totalStatsNodes > 0) {
            statsMilestones.add(Math.max(1, (int) Math.round(totalStatsNodes * 0.25)));
            statsMilestones.add(Math.max(1, (int) Math.round(totalStatsNodes * 0.50)));
            statsMilestones.add(Math.max(1, (int) Math.round(totalStatsNodes * 0.75)));
            statsMilestones.add(totalStatsNodes);
        }

        for (int i = 0; i < totalStatsNodes; i++) {
            final NodeData n = layout.nodes().get(i);
            final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
            totalExecs += execs;
            if (ScenarioType.LEAF == TreeProgressMetrics.getScenarioType(n)) {
                maxLeafOverRatio = Math.max(maxLeafOverRatio, TreeProgressMetrics.getLeafOverExecutionRatio(n, target));
            }
            if (n.children().isEmpty() && TreeProgressMetrics.isTerminalScenario(n)) {
                if (TreeHtml.isTheoreticallyExecutable(n, ctx, executableCache) || execs > 0) {
                    totalLeafs++;
                    if (execs >= TreeProgressMetrics.getTargetExecutions(n, target))
                        coveredLeafs++;
                }
            }
            final int current = i + 1;
            if (statsMilestones.contains(current)) {
                final int percent = (int) Math.round((current * 100.0) / totalStatsNodes);
                logTrace(String.format("stats: %d/%d (%d%%)", current, totalStatsNodes, percent));
            }
        }

        final int baseW;
        final int baseH;
        if (sessionBaseW > 0 && sessionBaseH > 0) {
            baseW = sessionBaseW;
            baseH = sessionBaseH;
        } else {
            int fullMaxCol = 0;
            double fullMaxRow = 0.0;
            for (final NodeData n : layout.nodes()) {
                if (n.col() > fullMaxCol) fullMaxCol = n.col();
                if (n.row() > fullMaxRow) fullMaxRow = n.row();
            }
            baseW = (int) evenDimension(Math.max(800L, ((long) fullMaxCol + 1L) * COL_WIDTH + PADDING_X * 2L));
            baseH = (int) evenDimension(Math.max(
                    400L,
                    (long) Math.ceil((fullMaxRow + 1.0) * ROW_HEIGHT + headerHeight(sessionBaseW) + PADDING_Y + BOTTOM_PADDING)));
        }

        final TreeCameraTransform.Transform tx = (null != overrideTx) ? overrideTx : TreeCameraTransform.computeZoomTransform(layout, recentNodeKeys, target, baseW, baseH);
        final double zoom = tx.zoom();
        final double translateX = tx.translateX();
        final double translateY = tx.translateY();

        final long maxPixels = ProgressionFrames.maxFramePixels(ProgressionSettings.current().maxFramePixels());
        if (baseW > ProgressionFrames.MAX_DIMENSION
                || baseH > ProgressionFrames.MAX_DIMENSION
                || (long) baseW * baseH > maxPixels) {
            throw new IllegalArgumentException("Rendered tree frame would exceed the configured limit ("
                    + baseW + "x" + baseH + ", max pixels " + maxPixels + ")");
        }

        final BufferedImage img = new BufferedImage(baseW, baseH, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fill background
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, baseW, baseH);

        TexturePaint dotPaint = cachedDotPaint;
        if (dotPaint == null) {
            final BufferedImage dotTile = new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB);
            final Graphics2D tileG = dotTile.createGraphics();
            tileG.setColor(BG_COLOR);
            tileG.fillRect(0, 0, 24, 24);
            tileG.setColor(GRID_DOT);
            tileG.fillRect(0, 0, 1, 1);
            tileG.dispose();

            dotPaint = new TexturePaint(dotTile,
                    new java.awt.geom.Rectangle2D.Double(0, headerHeight(baseW), 24, 24));
            if (null != dotPaintCallback) {
                dotPaintCallback.onCreated(dotPaint);
            }
        }
        g.setPaint(dotPaint);
        g.fillRect(0, headerHeight(baseW), baseW, baseH - headerHeight(baseW));

        final long tInit = System.currentTimeMillis() - tRenderStart;

        // Apply zoom transform for edges & nodes
        final java.awt.geom.AffineTransform origTx = g.getTransform();
        g.translate(translateX, translateY);
        g.scale(zoom, zoom);

        // Draw edges
        final long tEdgeStart = System.currentTimeMillis();
        drawEdges(g, layout.edges(), target, maxLeafOverRatio, recentNodeKeys, accessibleScenarios, childFailureCache, baseW);
        final long tEdges = System.currentTimeMillis() - tEdgeStart;

        // Draw nodes
        final long tNodeStart = System.currentTimeMillis();
        final boolean isAllLeafs = (null != parameters) ? parameters.isAllLeafs() : false;
        final VisualNodeResolver nodeResolver =
                new VisualNodeResolver(TreeTheme.DEFAULT, accessibleScenarios, isAllLeafs, target, maxLeafOverRatio, false, true, 0.0, headerHeight(baseW));
        final List<VisualNode> visualNodes = new ArrayList<>();
        final int totalNodes = layout.nodes().size();
        final Set<Integer> nodeMilestones = new HashSet<>();
        if (totalNodes > 0) {
            nodeMilestones.add(Math.max(1, (int) Math.round(totalNodes * 0.25)));
            nodeMilestones.add(Math.max(1, (int) Math.round(totalNodes * 0.50)));
            nodeMilestones.add(Math.max(1, (int) Math.round(totalNodes * 0.75)));
            nodeMilestones.add(totalNodes);
        }

        for (int i = 0; i < totalNodes; i++) {
            final NodeData n = layout.nodes().get(i);
            visualNodes.add(nodeResolver.resolve(n, recentNodeKeys.contains(n.id()), childFailureCache, executableCache));
            final int current = i + 1;
            if (nodeMilestones.contains(current)) {
                final int percent = (int) Math.round((current * 100.0) / totalNodes);
                logTrace(String.format("nodes: %d/%d (%d%%)", current, totalNodes, percent));
            }
        }

        new GraphicsTreeRenderer(TreeTheme.DEFAULT).renderNodes(g, visualNodes);
        final long tNodes = System.currentTimeMillis() - tNodeStart;

        // Restore transform for header
        g.setTransform(origTx);

        // Draw header
        drawHeader(g, baseW, badgeText, target, coveredLeafs, totalLeafs, totalExecs, zoom);

        g.dispose();
        final long tTotal = System.currentTimeMillis() - tRenderStart;
        logDebug(String.format("Canvas (%dx%d) in %d ms (init: %d ms, edges: %d ms, nodes: %d ms)",
                baseW, baseH, tTotal, tInit, tEdges, tNodes));
        return img;
    }

    private static void drawEdges(
            final Graphics2D g,
            final List<Edge> edges,
            final int target,
            final double maxLeafOverRatio,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final Map<NodeData, Boolean> childFailureCache,
            final int baseW) {

        final List<Edge> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort(Comparator.comparingInt(e -> {
            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            return (toExecs > 0 && fromExecs > 0) ? 1 : 0;
        }));

        final int totalEdges = sortedEdges.size();
        final Set<Integer> edgeMilestones = new HashSet<>();
        if (totalEdges > 0) {
            edgeMilestones.add(Math.max(1, (int) Math.round(totalEdges * 0.25)));
            edgeMilestones.add(Math.max(1, (int) Math.round(totalEdges * 0.50)));
            edgeMilestones.add(Math.max(1, (int) Math.round(totalEdges * 0.75)));
            edgeMilestones.add(totalEdges);
        }

        for (int i = 0; i < totalEdges; i++) {
            final Edge e = sortedEdges.get(i);
            final int current = i + 1;
            if (edgeMilestones.contains(current)) {
                final int percent = (int) Math.round((current * 100.0) / totalEdges);
                logTrace(String.format("edges: %d/%d (%d%%)", current, totalEdges, percent));
            }

            final int startX = e.from().col() * COL_WIDTH + CARD_WIDTH + PADDING_X;
            final int startY = (int) Math
                    .round(e.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerHeight(baseW) + PADDING_Y);
            final int endX = e.to().col() * COL_WIDTH + PADDING_X;
            final int endY = (int) Math
                    .round(e.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerHeight(baseW) + PADDING_Y);
            final int cX = (startX + endX) / 2;

            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            final boolean toWarning = (null != e.to().result() && e.to().result().status() == ExecutionStatus.WARNING);

            final Path2D.Double path = new Path2D.Double();
            path.moveTo(startX, startY);
            path.curveTo(cX, startY, cX, endY, endX, endY);

            final boolean isRecent = recentNodeKeys.contains(e.to().id());

            if (isRecent) {
                // Outer glowing aura for recently changed branch edge
                g.setColor(new Color(0x38, 0xBD, 0xF8, 90));
                g.setStroke(new BasicStroke(5.0f));
                g.draw(path);

                g.setColor(ACCENT_CYAN);
                g.setStroke(new BasicStroke(3.0f));
                g.draw(path);
            } else if (TreeHtml.hasFailure(e.to())) {
                g.setColor(RED_FAILED);
                g.setStroke(new BasicStroke(2.2f));
                g.draw(path);
            } else if (TreeHtml.hasChildFailure(e.to(), childFailureCache)) {
                g.setColor(INDIGO_UPSET);
                g.setStroke(DASHED_UPSET_STROKE);
                g.draw(path);
            } else if (toExecs > 0 && fromExecs > 0) {
                if (toWarning) {
                    g.setColor(WARNING_AMBER);
                } else {
                    g.setColor(TreeProgressMetrics.getScenarioProgressColor(e.to(), target, maxLeafOverRatio));
                }
                g.setStroke(new BasicStroke(2.2f));
                g.draw(path);
            } else if (!TreeHtml.isTheoreticallyExecutable(e.to(), accessibleScenarios)) {
                g.setColor(EXCLUDED_EDGE);
                g.setStroke(DASHED_EDGE_STROKE);
                g.draw(path);
            } else {
                g.setColor(UNVISITED_EDGE);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(path);
            }
        }
    }

    private static void drawHeader(
            final Graphics2D g,
            final int canvasW,
            final String badgeText,
            final int target,
            final int coveredLeafs,
            final int totalLeafs,
            final int totalExecs,
            final double zoom) {

        final double scale = Math.max(1.0, canvasW / 1200.0);
        final java.awt.geom.AffineTransform origTx = g.getTransform();
        g.scale(scale, scale);
        final int localCanvasW = (int) Math.round(canvasW / scale);

        g.setColor(HEADER_BG);
        g.fillRect(0, 0, localCanvasW, HEADER_HEIGHT_BASE);
        g.setColor(HEADER_BORDER);
        g.drawLine(0, HEADER_HEIGHT_BASE, localCanvasW, HEADER_HEIGHT_BASE);

        g.setColor(ACCENT_CYAN);
        g.fillOval(PADDING_X, (HEADER_HEIGHT_BASE - 12) / 2, 12, 12);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(TEXT_WHITE);
        g.drawString("Scenario Execution Tree Progression", PADDING_X + 22, (HEADER_HEIGHT_BASE / 2) + 5);

        int rightX = localCanvasW - PADDING_X;
        final int topY = (HEADER_HEIGHT_BASE - 22) / 2;

        final String elapsedText = (isNotNull(timer))
                ? "Elapsed: " + TreeProgressMetrics.formatElapsedTime(Math.max(0L, System.currentTimeMillis() - timer.startedAt()))
                : "Elapsed: --:--:--";
        rightX -= drawBadge(g, elapsedText, rightX, topY, new Color(51, 65, 85), TEXT_WHITE) + 10;

        rightX -= drawBadge(g, totalExecs + " total execs", rightX, topY, new Color(51, 65, 85), TEXT_WHITE) + 10;

        final String zoomStr = String.format(Locale.US, "Zoom: %.1fx", zoom);
        rightX -= drawBadge(g, zoomStr, rightX, topY, new Color(2, 132, 199), Color.WHITE) + 10;

        final Color covBg = (coveredLeafs == totalLeafs && totalLeafs > 0) ? new Color(22, 101, 52)
                : new Color(180, 83, 9);
        rightX -= drawBadge(g, "Leafs: " + coveredLeafs + "/" + totalLeafs, rightX, topY, covBg, TEXT_WHITE) + 10;

        rightX -= drawBadge(g, "Target: " + target, rightX, topY, new Color(51, 65, 85), new Color(203, 213, 225)) + 10;

        drawBadge(g, badgeText, rightX, topY, new Color(2, 132, 199), Color.WHITE);

        if (ProgressionSettings.current().treeDebugEnabled()) {
            final String debugStr = String.format(Locale.US, "dx:%.1f dy:%.1f dz:%.3f maxZ:%.1fx",
                    TreeCameraTransform.getDebugDx(),
                    TreeCameraTransform.getDebugDy(),
                    TreeCameraTransform.getDebugDz(),
                    TreeCameraTransform.getDebugMaxZoom());
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            g.setColor(new Color(100, 116, 139));
            g.drawString(debugStr, PADDING_X, HEADER_HEIGHT_BASE - 5);

            final double ox = localCanvasW / 2.0;
            final double oy = (canvasW / scale) / 2.0;
            g.setColor(new Color(245, 158, 11));
            g.setStroke(new BasicStroke(2.0f));
            g.drawLine((int) ox, (int) oy, (int) (ox - TreeCameraTransform.getDebugDx()), (int) (oy - TreeCameraTransform.getDebugDy()));
            g.fillOval((int) (ox - 3), (int) (oy - 3), 6, 6);
            g.setColor(new Color(248, 250, 252));
            g.fillOval((int) (ox - TreeCameraTransform.getDebugDx() - 2), (int) (oy - TreeCameraTransform.getDebugDy() - 2), 4, 4);
        }
    }

    private static int drawBadge(
            final Graphics2D g,
            final String text,
            final int rightX,
            final int topY,
            final Color bg,
            final Color fg) {

        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        final FontMetrics fm = g.getFontMetrics();
        final int textW = fm.stringWidth(text);
        final int badgeW = textW + 16;
        final int badgeH = 22;
        final int leftX = rightX - badgeW;

        g.setColor(bg);
        g.fillRoundRect(leftX, topY, badgeW, badgeH, 6, 6);
        g.setColor(fg);
        g.drawString(text, leftX + 8, topY + 15);
        return badgeW;
    }

    private static long evenDimension(final long dimension) {
        if (dimension >= 16384L)
            return dimension;
        return (dimension & 1L) == 0L ? dimension : dimension + 1L;
    }
}
