package works.lysenko.base.output.tree;

import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages viewport panning, zoom calculations, and smooth camera movement
 * for tree progression visualizations.
 */
public final class TreeCameraTransform {

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

    public record Transform(double zoom, double translateX, double translateY) {}

    public record FocusBounds(int minCol, int maxCol, double minRow, double maxRow) {}

    private static volatile double currentZoom = 1.0;
    private static volatile double currentTranslateX = 0.0;
    private static volatile double currentTranslateY = 0.0;
    private static volatile boolean transformInitialized = false;

    private static double debugDx = 0.0;
    private static double debugDy = 0.0;
    private static double debugDz = 0.0;
    private static double debugMaxZoom = 1.0;

    private TreeCameraTransform() {
    }

    public static FocusBounds computeActiveFocusBounds(
            final TreeLayout layout,
            final Set<String> recentNodeKeys,
            final int target) {

        final Set<NodeData> focusNodes = new HashSet<>();

        for (final NodeData n : layout.nodes()) {
            if (recentNodeKeys.contains(n.id())) {
                focusNodes.add(n);
                if (null != n.parent()) focusNodes.add(n.parent());
            }
        }

        for (final NodeData n : layout.nodes()) {
            final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
            if (execs > 0) {
                focusNodes.add(n);
                if (null != n.parent()) focusNodes.add(n.parent());
            } else if (n.children().isEmpty() && TreeProgressMetrics.isTerminalScenario(n) && execs < TreeProgressMetrics.getTargetExecutions(n, target)) {
                focusNodes.add(n);
                if (null != n.parent()) focusNodes.add(n.parent());
            }
        }

        if (focusNodes.isEmpty()) {
            focusNodes.addAll(layout.nodes());
        }

        double focusMinRow = Double.MAX_VALUE;
        double focusMaxRow = 0.0;
        int focusMaxCol = 0;

        for (final NodeData n : focusNodes) {
            if (n.col() > focusMaxCol) focusMaxCol = n.col();
            if (n.row() < focusMinRow) focusMinRow = n.row();
            if (n.row() > focusMaxRow) focusMaxRow = n.row();
        }

        if (focusMinRow > focusMaxRow) {
            focusMinRow = 0.0;
            focusMaxRow = 0.0;
        }

        if (focusMaxRow - focusMinRow < 2.0) {
            focusMaxRow = focusMinRow + 2.0;
        }

        return new FocusBounds(0, focusMaxCol, focusMinRow, focusMaxRow);
    }

    public static Transform computeZoomTransform(
            final TreeLayout layout,
            final Set<String> recentNodeKeys,
            final int target,
            final int baseW,
            final int baseH) {

        final FocusBounds bounds = computeActiveFocusBounds(layout, recentNodeKeys, target);

        final double activeLeft = bounds.minCol() * COL_WIDTH + PADDING_X;
        final double activeRight = bounds.maxCol() * COL_WIDTH + CARD_WIDTH + PADDING_X;
        final double activeTop = bounds.minRow() * ROW_HEIGHT + headerHeight(baseW) + PADDING_Y;
        final double activeBottom = bounds.maxRow() * ROW_HEIGHT + CARD_HEIGHT + headerHeight(baseW) + PADDING_Y;

        final double activeW = Math.max(CARD_WIDTH, activeRight - activeLeft);
        final double activeH = Math.max(CARD_HEIGHT, activeBottom - activeTop);

        final double activeCenterX = (activeLeft + activeRight) / 2.0;
        final double activeCenterY = (activeTop + activeBottom) / 2.0;

        final double availW = baseW - PADDING_X * 2.0;
        final double availH = baseH - headerHeight(baseW) - PADDING_Y - BOTTOM_PADDING;

        final double scaleX = availW / activeW;
        final double scaleY = availH / activeH;
        final double maxSingleNodeZoom = Math.max(1.0, Math.min(availW / CARD_WIDTH, availH / CARD_HEIGHT));
        debugMaxZoom = maxSingleNodeZoom;
        final double zoom = Math.max(1.0, Math.min(Math.min(scaleX, scaleY), maxSingleNodeZoom));

        final double targetCenterX = baseW / 2.0;
        final double targetCenterY = headerHeight(baseW) + PADDING_Y + availH / 2.0;

        final double translateX = targetCenterX - activeCenterX * zoom;
        final double translateY = targetCenterY - activeCenterY * zoom;

        return new Transform(zoom, translateX, translateY);
    }

    public static synchronized Transform computeSmoothTransform(
            final TreeLayout layout,
            final Set<String> recentNodeKeys,
            final int target,
            final int baseW,
            final int baseH) {

        final Transform targetTx = computeZoomTransform(layout, recentNodeKeys, target, baseW, baseH);

        debugDx = targetTx.translateX() - currentTranslateX;
        debugDy = targetTx.translateY() - currentTranslateY;
        debugDz = targetTx.zoom() - currentZoom;

        if (!transformInitialized) {
            currentZoom = targetTx.zoom();
            currentTranslateX = targetTx.translateX();
            currentTranslateY = targetTx.translateY();
            transformInitialized = true;
        } else {
            final double alpha = 0.20;
            currentZoom += alpha * debugDz;
            currentTranslateX += alpha * debugDx;
            currentTranslateY += alpha * debugDy;
        }

        return new Transform(currentZoom, currentTranslateX, currentTranslateY);
    }

    public static synchronized boolean isTransformSettled(
            final TreeLayout layout,
            final Set<String> recentNodeKeys,
            final int target,
            final int baseW,
            final int baseH) {

        if (!transformInitialized) return true;

        final Transform targetTx = computeZoomTransform(layout, recentNodeKeys, target, baseW, baseH);

        final double epsilonZ = 0.005;
        final double epsilonT = 0.5;

        debugDx = targetTx.translateX() - currentTranslateX;
        debugDy = targetTx.translateY() - currentTranslateY;
        debugDz = targetTx.zoom() - currentZoom;

        return Math.abs(debugDz) < epsilonZ &&
               Math.abs(debugDx) < epsilonT &&
               Math.abs(debugDy) < epsilonT;
    }

    public static synchronized void reset() {
        currentZoom = 1.0;
        currentTranslateX = 0.0;
        currentTranslateY = 0.0;
        transformInitialized = false;
        debugDx = 0.0;
        debugDy = 0.0;
        debugDz = 0.0;
        debugMaxZoom = 1.0;
    }

    public static boolean isInitialized() {
        return transformInitialized;
    }

    public static double getDebugDx() {
        return debugDx;
    }

    public static double getDebugDy() {
        return debugDy;
    }

    public static double getDebugDz() {
        return debugDz;
    }

    public static double getDebugMaxZoom() {
        return debugMaxZoom;
    }
}
