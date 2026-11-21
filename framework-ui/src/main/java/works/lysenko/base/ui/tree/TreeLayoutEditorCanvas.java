package works.lysenko.base.ui.tree;

import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.ui.tree.ScenarioOffsetSourceCodec.Offsets;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.Point2DGrid;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.PreviewResult;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Interactive dark-themed {@code Graphics2D} canvas for visualizing and selecting scenario
 * tree nodes with pan, zoom, subtree highlighting, raw-vs-compacted ghost overlays, and
 * keyboard nudging.
 */
public final class TreeLayoutEditorCanvas extends JPanel {

    private static final double COL_WIDTH = 260.0;
    private static final double ROW_HEIGHT = 56.0;
    private static final double CARD_WIDTH = 220.0;
    private static final double CARD_HEIGHT = 44.0;
    private static final double WORLD_PAD_X = 56.0;
    private static final double WORLD_PAD_Y = 48.0;

    private static final double MIN_ZOOM = 0.10;
    private static final double MAX_ZOOM = 2.50;
    private static final int DRAG_THRESHOLD_PX = 4;

    private static final Color BG = new Color(0x0F, 0x17, 0x2A);
    private static final Color GRID_LINE = new Color(0x1E, 0x29, 0x3B);
    private static final Color RULER_TEXT = new Color(0x47, 0x55, 0x69);

    private static final Color EDGE_NORMAL = new Color(0x47, 0x55, 0x69);
    private static final Color EDGE_SUBTREE = new Color(0x38, 0xBD, 0xF8, 175);
    private static final Color EDGE_ANCESTOR = new Color(0x38, 0xBD, 0xF8);

    private static final Color CARD_FILL = new Color(0x1E, 0x29, 0x3B);
    private static final Color CARD_STROKE = new Color(0x47, 0x55, 0x69);
    private static final Color CARD_SHIFTED_FILL = new Color(0x17, 0x2E, 0x4A);
    private static final Color CARD_SHIFTED_STROKE = new Color(0x38, 0xBD, 0xF8);
    private static final Color CARD_SUBTREE_FILL = new Color(0x1E, 0x3A, 0x5F);
    private static final Color CARD_SELECTED_FILL = new Color(0x0C, 0x4A, 0x6E);
    private static final Color CARD_SELECTED_STROKE = new Color(0x7D, 0xD3, 0xFC);
    private static final Color CARD_HOVER_STROKE = new Color(0x94, 0xA3, 0xB8);

    private static final Color TEXT_PRIMARY = new Color(0xF8, 0xFA, 0xFC);
    private static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);
    private static final Color BADGE_BG = new Color(0x0F, 0x17, 0x2A, 210);
    private static final Color BADGE_TEXT = new Color(0x38, 0xBD, 0xF8);
    private static final Color DIRTY_COLOR = new Color(0xF5, 0x9E, 0x0B);
    private static final Color GHOST_COLOR = new Color(0xF5, 0x9E, 0x0B, 130);

    private static final Font CARD_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font CARD_FONT_BOLD = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final Font BADGE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 10);
    private static final Font RULER_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    private static final Font PLACEHOLDER_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

    private static final Stroke STROKE_THIN = new BasicStroke(1.2f);
    private static final Stroke STROKE_EDGE = new BasicStroke(1.8f);
    private static final Stroke STROKE_EDGE_ACTIVE = new BasicStroke(2.8f);
    private static final Stroke STROKE_SELECTED = new BasicStroke(2.6f);
    private static final Stroke STROKE_DASHED = new BasicStroke(
            1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 4f}, 0f);

    private final TreeLayoutEditorModel model;
    private Consumer<String> onSelectionChanged = ignored -> {};
    private BiConsumer<Integer, Integer> onNudge = (dx, dy) -> {};
    private Runnable onResetSelected = () -> {};

    private String selectedClassName;
    private String hoveredNodeId;
    private boolean showGrid = true;
    private boolean showRawGhosts = false;

    private double zoom = 0.75;
    private double panX = 24.0;
    private double panY = 24.0;

    private Point pressPoint;
    private double pressPanX;
    private double pressPanY;
    private boolean dragging;

    public TreeLayoutEditorCanvas(final TreeLayoutEditorModel model) {
        this.model = Objects.requireNonNull(model, "model");
        setBackground(BG);
        setOpaque(true);
        setFocusable(true);
        setToolTipText("");

        final MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                requestFocusInWindow();
                pressPoint = e.getPoint();
                pressPanX = panX;
                pressPanY = panY;
                dragging = false;
            }

            @Override
            public void mouseDragged(final MouseEvent e) {
                if (null == pressPoint) return;
                final int dx = e.getX() - pressPoint.x;
                final int dy = e.getY() - pressPoint.y;
                if (!dragging && (Math.abs(dx) >= DRAG_THRESHOLD_PX || Math.abs(dy) >= DRAG_THRESHOLD_PX)) {
                    dragging = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                if (dragging) {
                    panX = pressPanX + dx;
                    panY = pressPanY + dy;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                if (dragging) {
                    dragging = false;
                    updateCursorForPoint(e.getPoint());
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    final NodeData hit = hitTest(e.getX(), e.getY());
                    final String hitClass = (null != hit) ? model.latestPreview().classNameByNodeId().get(hit.id()) : null;
                    setSelectedClassName(hitClass);
                    onSelectionChanged.accept(hitClass);
                    if (null != hit && e.getClickCount() >= 2) {
                        focusNode(hitClass, true);
                    }
                }
            }

            @Override
            public void mouseMoved(final MouseEvent e) {
                final NodeData hit = hitTest(e.getX(), e.getY());
                final String nextHover = (null != hit) ? hit.id() : null;
                if (!Objects.equals(hoveredNodeId, nextHover)) {
                    hoveredNodeId = nextHover;
                    updateCursorForPoint(e.getPoint());
                    setToolTipText(buildTooltip(hit));
                    repaint();
                }
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                if (null != hoveredNodeId) {
                    hoveredNodeId = null;
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(final MouseWheelEvent e) {
                final double factor = (e.getPreciseWheelRotation() < 0) ? 1.12 : (1.0 / 1.12);
                zoomAround(e.getX(), e.getY(), zoom * factor);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                final int step = e.isShiftDown() ? 5 : 1;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> {
                        if (null != selectedClassName) { onNudge.accept(-step, 0); e.consume(); }
                    }
                    case KeyEvent.VK_RIGHT -> {
                        if (null != selectedClassName) { onNudge.accept(step, 0); e.consume(); }
                    }
                    case KeyEvent.VK_UP -> {
                        if (null != selectedClassName) { onNudge.accept(0, -step); e.consume(); }
                    }
                    case KeyEvent.VK_DOWN -> {
                        if (null != selectedClassName) { onNudge.accept(0, step); e.consume(); }
                    }
                    case KeyEvent.VK_0, KeyEvent.VK_NUMPAD0, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE -> {
                        if (null != selectedClassName) { onResetSelected.run(); e.consume(); }
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        setSelectedClassName(null);
                        onSelectionChanged.accept(null);
                        e.consume();
                    }
                    case KeyEvent.VK_F -> {
                        fitToView();
                        e.consume();
                    }
                    case KeyEvent.VK_EQUALS, KeyEvent.VK_PLUS, KeyEvent.VK_ADD -> {
                        zoomAround(getWidth() / 2.0, getHeight() / 2.0, zoom * 1.18);
                        e.consume();
                    }
                    case KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT -> {
                        zoomAround(getWidth() / 2.0, getHeight() / 2.0, zoom / 1.18);
                        e.consume();
                    }
                    default -> {
                    }
                }
            }
        });
    }

    public void setOnSelectionChanged(final Consumer<String> listener) {
        this.onSelectionChanged = (null != listener) ? listener : ignored -> {};
    }

    public void setOnNudge(final BiConsumer<Integer, Integer> listener) {
        this.onNudge = (null != listener) ? listener : (dx, dy) -> {};
    }

    public void setOnResetSelected(final Runnable listener) {
        this.onResetSelected = (null != listener) ? listener : () -> {};
    }

    public void setSelectedClassName(final String className) {
        if (!Objects.equals(this.selectedClassName, className)) {
            this.selectedClassName = className;
            repaint();
        }
    }

    public String getSelectedClassName() {
        return selectedClassName;
    }

    public void setShowGrid(final boolean showGrid) {
        this.showGrid = showGrid;
        repaint();
    }

    public void setShowRawGhosts(final boolean showRawGhosts) {
        this.showRawGhosts = showRawGhosts;
        repaint();
    }

    public double getZoom() {
        return zoom;
    }

    public void resetZoom() {
        zoom = 1.0;
        panX = 24.0;
        panY = 24.0;
        repaint();
    }

    public void fitToView() {
        final PreviewResult preview = model.latestPreview();
        if (preview.layout().nodes().isEmpty() || getWidth() <= 0 || getHeight() <= 0) {
            resetZoom();
            return;
        }
        final double worldW = WORLD_PAD_X * 2.0 + Math.max(1, preview.totalCols()) * COL_WIDTH;
        final double worldH = WORLD_PAD_Y * 2.0 + Math.max(1.0, preview.totalRows()) * ROW_HEIGHT;
        final double scaleX = (getWidth() - 24.0) / worldW;
        final double scaleY = (getHeight() - 24.0) / worldH;
        zoom = clamp(Math.min(1.0, Math.min(scaleX, scaleY)), MIN_ZOOM, MAX_ZOOM);
        panX = (getWidth() - worldW * zoom) / 2.0;
        panY = (getHeight() - worldH * zoom) / 2.0;
        repaint();
    }

    public void focusNode(final String className, final boolean boostZoom) {
        if (null == className || getWidth() <= 0 || getHeight() <= 0) return;
        final List<NodeData> nodes = model.latestPreview().nodesByClassName().get(className);
        if (null == nodes || nodes.isEmpty()) return;
        final NodeData target = nodes.get(0);
        if (boostZoom && zoom < 0.75) {
            zoom = 0.85;
        }
        final double cx = nodeX(target.col()) + CARD_WIDTH / 2.0;
        final double cy = nodeY(target.row()) + CARD_HEIGHT / 2.0;
        panX = getWidth() / 2.0 - cx * zoom;
        panY = getHeight() / 2.0 - cy * zoom;
        repaint();
    }

    private void zoomAround(final double screenX, final double screenY, final double nextZoom) {
        final double clamped = clamp(nextZoom, MIN_ZOOM, MAX_ZOOM);
        final double worldX = (screenX - panX) / zoom;
        final double worldY = (screenY - panY) / zoom;
        zoom = clamped;
        panX = screenX - worldX * zoom;
        panY = screenY - worldY * zoom;
        repaint();
    }

    private void updateCursorForPoint(final Point pt) {
        final NodeData hit = hitTest(pt.x, pt.y);
        setCursor(Cursor.getPredefinedCursor(null != hit ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public NodeData hitTest(final int screenX, final int screenY) {
        final double wx = (screenX - panX) / zoom;
        final double wy = (screenY - panY) / zoom;
        final List<NodeData> nodes = model.latestPreview().layout().nodes();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            final NodeData n = nodes.get(i);
            final double nx = nodeX(n.col());
            final double ny = nodeY(n.row());
            if (wx >= nx && wx <= nx + CARD_WIDTH && wy >= ny && wy <= ny + CARD_HEIGHT) {
                return n;
            }
        }
        return null;
    }

    private String buildTooltip(final NodeData node) {
        if (null == node) return null;
        final PreviewResult preview = model.latestPreview();
        final String className = preview.classNameByNodeId().get(node.id());
        if (null == className) return null;
        final Offsets cur = model.currentOffsets(className);
        final Offsets init = model.initialOffsets(className);
        final Point2DGrid raw = preview.rawPositionByNodeId().get(node.id());
        final StringBuilder sb = new StringBuilder("<html><body style='font-family:monospace;padding:2px'>");
        sb.append("<b>").append(escapeHtml(node.label())).append("</b><br/>");
        sb.append("<span style='color:#64748b'>").append(escapeHtml(className)).append("</span><br/>");
        sb.append(String.format("Grid: col=%d, row=%.2f", node.col(), node.row()));
        if (null != raw && (raw.col() != node.col() || Math.abs(raw.row() - node.row()) > 0.01)) {
            sb.append(String.format(" (raw: col=%d, row=%.2f)", raw.col(), raw.row()));
        }
        sb.append("<br/>");
        sb.append(String.format("Offsets: col=%+d, row=%+d", cur.colOffset(), cur.rowOffset()));
        if (!cur.equals(init)) {
            sb.append(String.format(" <i>(saved: col=%+d, row=%+d)</i>", init.colOffset(), init.rowOffset()));
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            final PreviewResult preview = model.latestPreview();
            final List<NodeData> nodes = preview.layout().nodes();
            if (nodes.isEmpty()) {
                paintPlaceholder(g2);
                return;
            }

            g2.translate(panX, panY);
            g2.scale(zoom, zoom);

            if (showGrid) {
                paintGrid(g2, preview);
            }

            final Set<NodeData> selectedNodes = Collections.newSetFromMap(new IdentityHashMap<>());
            final Set<NodeData> subtreeNodes = Collections.newSetFromMap(new IdentityHashMap<>());
            final Set<NodeData> ancestorNodes = Collections.newSetFromMap(new IdentityHashMap<>());
            if (null != selectedClassName) {
                final List<NodeData> matched = preview.nodesByClassName().get(selectedClassName);
                if (null != matched) {
                    for (final NodeData sel : matched) {
                        selectedNodes.add(sel);
                        collectSubtree(sel, subtreeNodes);
                        NodeData cur = sel;
                        while (null != cur) {
                            ancestorNodes.add(cur);
                            cur = cur.parent();
                        }
                    }
                }
            }

            if (showRawGhosts && model.previewSettings().compacted) {
                paintRawGhosts(g2, preview);
            }

            paintEdges(g2, preview.layout().edges(), subtreeNodes, ancestorNodes);
            paintNodes(g2, preview, selectedNodes, subtreeNodes);
        } finally {
            g2.dispose();
        }
    }

    private void paintPlaceholder(final Graphics2D g2) {
        g2.setFont(PLACEHOLDER_FONT);
        g2.setColor(TEXT_MUTED);
        final String msg = "No scenarios loaded. Check .test.root and click \"Reload from Sources\".";
        final FontMetrics fm = g2.getFontMetrics();
        final int x = Math.max(16, (getWidth() - fm.stringWidth(msg)) / 2);
        final int y = Math.max(32, getHeight() / 2);
        g2.drawString(msg, x, y);
    }

    private void paintGrid(final Graphics2D g2, final PreviewResult preview) {
        final int cols = Math.max(1, preview.totalCols());
        final int rows = Math.max(1, (int) Math.ceil(preview.totalRows()));
        final double xStart = WORLD_PAD_X;
        final double yStart = WORLD_PAD_Y;
        final double xEnd = xStart + cols * COL_WIDTH;
        final double yEnd = yStart + rows * ROW_HEIGHT;

        g2.setColor(GRID_LINE);
        g2.setStroke(STROKE_THIN);
        for (int c = 0; c <= cols; c++) {
            final double x = xStart + c * COL_WIDTH - 20.0;
            g2.draw(new Line2D.Double(x, yStart - 16.0, x, yEnd));
        }
        for (int r = 0; r <= rows; r++) {
            final double y = yStart + r * ROW_HEIGHT - 6.0;
            g2.draw(new Line2D.Double(xStart - 20.0, y, xEnd, y));
        }

        if (zoom >= 0.30) {
            g2.setFont(RULER_FONT);
            g2.setColor(RULER_TEXT);
            for (int c = 0; c < cols; c++) {
                final double x = nodeX(c) + 4.0;
                g2.drawString("C" + c, (float) x, (float) (yStart - 22.0));
            }
            for (int r = 1; r <= rows; r++) {
                final double y = nodeY(r) + 26.0;
                g2.drawString("R" + r, (float) (xStart - 46.0), (float) y);
            }
        }
    }

    private void paintRawGhosts(final Graphics2D g2, final PreviewResult preview) {
        final Map<String, Point2DGrid> rawPositions = preview.rawPositionByNodeId();
        g2.setStroke(STROKE_DASHED);
        g2.setColor(GHOST_COLOR);

        for (final NodeData n : preview.layout().nodes()) {
            if (n.colOffset() == 0 && n.rowOffset() == 0) continue;
            final Point2DGrid raw = rawPositions.get(n.id());
            if (null == raw) continue;
            if (raw.col() == n.col() && Math.abs(raw.row() - n.row()) < 0.05) continue;

            final double gx = nodeX(raw.col());
            final double gy = nodeY(raw.row());
            final double nx = nodeX(n.col());
            final double ny = nodeY(n.row());
            g2.draw(new RoundRectangle2D.Double(gx, gy, CARD_WIDTH, CARD_HEIGHT, 8, 8));
            g2.draw(new Line2D.Double(
                    gx + CARD_WIDTH / 2.0, gy + CARD_HEIGHT / 2.0,
                    nx + CARD_WIDTH / 2.0, ny + CARD_HEIGHT / 2.0));
        }
    }

    private void paintEdges(
            final Graphics2D g2,
            final List<Edge> edges,
            final Set<NodeData> subtreeNodes,
            final Set<NodeData> ancestorNodes) {
        final boolean hasSelection = !subtreeNodes.isEmpty();
        for (final Edge edge : edges) {
            final NodeData from = edge.from();
            final NodeData to = edge.to();
            final boolean isAncestorEdge = ancestorNodes.contains(from) && ancestorNodes.contains(to);
            final boolean isSubtreeEdge = subtreeNodes.contains(from) && subtreeNodes.contains(to);

            if (isAncestorEdge) {
                g2.setColor(EDGE_ANCESTOR);
                g2.setStroke(STROKE_EDGE_ACTIVE);
            } else if (isSubtreeEdge) {
                g2.setColor(EDGE_SUBTREE);
                g2.setStroke(STROKE_EDGE_ACTIVE);
            } else {
                g2.setColor(hasSelection ? new Color(0x33, 0x41, 0x55, 160) : EDGE_NORMAL);
                g2.setStroke(STROKE_EDGE);
            }

            final double x1 = nodeX(from.col()) + CARD_WIDTH;
            final double y1 = nodeY(from.row()) + CARD_HEIGHT / 2.0;
            final double x2 = nodeX(to.col());
            final double y2 = nodeY(to.row()) + CARD_HEIGHT / 2.0;
            final double mx = (x1 + x2) / 2.0;
            g2.draw(new CubicCurve2D.Double(x1, y1, mx, y1, mx, y2, x2, y2));
        }
    }

    private void paintNodes(
            final Graphics2D g2,
            final PreviewResult preview,
            final Set<NodeData> selectedNodes,
            final Set<NodeData> subtreeNodes) {
        final boolean drawLabels = zoom >= 0.24;
        final boolean drawBadges = zoom >= 0.30;

        for (final NodeData n : preview.layout().nodes()) {
            final String className = preview.classNameByNodeId().get(n.id());
            final Offsets cur = (null != className) ? model.currentOffsets(className) : Offsets.ZERO;
            final boolean shifted = !cur.isZero();
            final boolean dirty = null != className && model.isModified(className);
            final boolean selected = selectedNodes.contains(n);
            final boolean inSubtree = subtreeNodes.contains(n);
            final boolean hovered = Objects.equals(hoveredNodeId, n.id());

            final double x = nodeX(n.col());
            final double y = nodeY(n.row());
            final RoundRectangle2D card = new RoundRectangle2D.Double(x, y, CARD_WIDTH, CARD_HEIGHT, 8, 8);

            if (selected) {
                g2.setColor(CARD_SELECTED_FILL);
            } else if (inSubtree) {
                g2.setColor(CARD_SUBTREE_FILL);
            } else if (shifted) {
                g2.setColor(CARD_SHIFTED_FILL);
            } else {
                g2.setColor(CARD_FILL);
            }
            g2.fill(card);

            if (selected) {
                g2.setColor(CARD_SELECTED_STROKE);
                g2.setStroke(STROKE_SELECTED);
            } else if (dirty) {
                g2.setColor(DIRTY_COLOR);
                g2.setStroke(STROKE_EDGE);
            } else if (shifted) {
                g2.setColor(CARD_SHIFTED_STROKE);
                g2.setStroke(STROKE_EDGE);
            } else if (hovered) {
                g2.setColor(CARD_HOVER_STROKE);
                g2.setStroke(STROKE_EDGE);
            } else {
                g2.setColor(CARD_STROKE);
                g2.setStroke(STROKE_THIN);
            }
            g2.draw(card);

            if (dirty) {
                g2.setColor(DIRTY_COLOR);
                g2.fillOval((int) Math.round(x + CARD_WIDTH - 12), (int) Math.round(y + 6), 7, 7);
            }

            if (drawLabels) {
                final String badge = (drawBadges && shifted) ? formatBadge(cur) : null;
                final double badgeReserve = (null != badge) ? (badge.length() * 6.5 + 22.0) : (dirty ? 18.0 : 10.0);
                final int maxTextWidth = (int) Math.max(40.0, CARD_WIDTH - 18.0 - badgeReserve);

                g2.setFont((selected || shifted) ? CARD_FONT_BOLD : CARD_FONT);
                g2.setColor((selected || shifted || inSubtree || !n.children().isEmpty()) ? TEXT_PRIMARY : TEXT_MUTED);
                final String text = ellipsize(n.label(), g2.getFontMetrics(), maxTextWidth);
                g2.drawString(text, (float) (x + 10.0), (float) (y + 27.0));

                if (null != badge) {
                    g2.setFont(BADGE_FONT);
                    final FontMetrics bfm = g2.getFontMetrics();
                    final int bw = bfm.stringWidth(badge) + 10;
                    final int bh = 18;
                    final double bx = x + CARD_WIDTH - bw - (dirty ? 16.0 : 8.0);
                    final double by = y + (CARD_HEIGHT - bh) / 2.0;
                    final RoundRectangle2D pill = new RoundRectangle2D.Double(bx, by, bw, bh, 9, 9);
                    g2.setColor(BADGE_BG);
                    g2.fill(pill);
                    g2.setColor(dirty ? DIRTY_COLOR : CARD_SHIFTED_STROKE);
                    g2.setStroke(STROKE_THIN);
                    g2.draw(pill);
                    g2.setColor(dirty ? DIRTY_COLOR : BADGE_TEXT);
                    g2.drawString(badge, (float) (bx + 5.0), (float) (by + 13.0));
                }
            }
        }
    }

    private static String formatBadge(final Offsets o) {
        if (o.colOffset() != 0 && o.rowOffset() != 0) {
            return String.format("+%d,%+d", o.colOffset(), o.rowOffset());
        }
        if (o.colOffset() != 0) {
            return String.format("c+%d", o.colOffset());
        }
        return String.format("r%+d", o.rowOffset());
    }

    private static String ellipsize(final String text, final FontMetrics fm, final int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) return text;
        final String ellipsis = "…";
        final int ellW = fm.stringWidth(ellipsis);
        int end = text.length();
        while (end > 1 && fm.stringWidth(text.substring(0, end)) + ellW > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private static void collectSubtree(final NodeData root, final Set<NodeData> out) {
        if (null == root || !out.add(root)) return;
        for (final NodeData child : root.children()) {
            collectSubtree(child, out);
        }
    }

    private static double nodeX(final int col) {
        return WORLD_PAD_X + col * COL_WIDTH;
    }

    private static double nodeY(final double row) {
        return WORLD_PAD_Y + (row - 1.0) * ROW_HEIGHT;
    }

    private static double clamp(final double val, final double min, final double max) {
        return Math.max(min, Math.min(max, val));
    }

    private static String escapeHtml(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
