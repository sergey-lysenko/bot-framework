package works.lysenko.base.ui.tree;

import works.lysenko.base.output.TreeHtml;

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
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.__.BR;
import static works.lysenko.util.chrs.___.AND;
import static works.lysenko.util.chrs.___.COL;
import static works.lysenko.util.chrs.___.RAW;
import static works.lysenko.util.chrs.___.ROW;
import static works.lysenko.util.chrs.____.BODY;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.chrs.____.GRID;
import static works.lysenko.util.chrs.____.HTML;
import static works.lysenko.util.chrs.____.ROOT;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.lang.F.FMT_COL_TAG;
import static works.lysenko.util.lang.F.FMT_PERCENT_2F;
import static works.lysenko.util.lang.F.FMT_PERCENT_D;
import static works.lysenko.util.lang.F.FMT_PERCENT_PLUS_D;
import static works.lysenko.util.lang.F.FMT_ROW_TAG;
import static works.lysenko.util.lang.N.NO_SCENARIOS_LOADED;
import static works.lysenko.util.lang.word.C.CHECK;
import static works.lysenko.util.lang.word.C.CLICK;
import static works.lysenko.util.lang.word.M.MODEL;
import static works.lysenko.util.lang.word.O.OFFSETS;
import static works.lysenko.util.lang.word.R.RELOAD;
import static works.lysenko.util.lang.word.S.SAVED;
import static works.lysenko.util.lang.word.S.SOURCES;
import static works.lysenko.util.lang.word.S.SPAN;
import static works.lysenko.util.spec.Symbols.A;
import static works.lysenko.util.spec.Symbols.B;
import static works.lysenko.util.spec.Symbols.C;
import static works.lysenko.util.spec.Symbols.D;
import static works.lysenko.util.spec.Symbols.DBL_QUO;
import static works.lysenko.util.spec.Symbols.E;
import static works.lysenko.util.spec.Symbols.ELLIPSIS;
import static works.lysenko.util.spec.Symbols.F;
import static works.lysenko.util.spec.Symbols.G;
import static works.lysenko.util.spec.Symbols.GRT_THN;
import static works.lysenko.util.spec.Symbols.I;
import static works.lysenko.util.spec.Symbols.L;
import static works.lysenko.util.spec.Symbols.LSS_THN;
import static works.lysenko.util.spec.Symbols.M;
import static works.lysenko.util.spec.Symbols.N;
import static works.lysenko.util.spec.Symbols.O;
import static works.lysenko.util.spec.Symbols.P;
import static works.lysenko.util.spec.Symbols.R;
import static works.lysenko.util.spec.Symbols.S;
import static works.lysenko.util.spec.Symbols.SEM_CLN;
import static works.lysenko.util.spec.Symbols.T;
import static works.lysenko.util.spec.Symbols.X;
import static works.lysenko.util.spec.Symbols.Y;
import static works.lysenko.util.spec.Symbols._2_;
import static works.lysenko.util.spec.Symbols._4_;
import static works.lysenko.util.spec.Symbols._6_;
import static works.lysenko.util.spec.Symbols._7_;
import static works.lysenko.util.spec.Symbols._8_;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._DOT_;
import static works.lysenko.util.spec.Symbols._EQUAL_;
import static works.lysenko.util.spec.Symbols._NUMBR_;
import static works.lysenko.util.spec.Symbols._PLUS_;
import static works.lysenko.util.spec.Symbols._QUOTE_;
import static works.lysenko.util.spec.Symbols._SLASH_;
import static works.lysenko.util.spec.Symbols._SPACE_;

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
    private static final double HALF = 2.0;
    private static final double CARD_ARC = 8.0;
    private static final double MIN_DETAIL_ZOOM = 0.30;

    private final TreeLayoutEditorModel model;
    private final ViewportState viewport = new ViewportState();
    private final CanvasCallbacks callbacks = new CanvasCallbacks();
    private final CanvasRenderer renderer = new CanvasRenderer();

    private String selectedClassName = null;
    private String hoveredNodeId = null;
    private boolean showGrid = true;
    private boolean showRawGhosts = false;

    /**
     * Creates a new interactive tree layout canvas bound to the given editor model.
     *
     * @param model editor model supplying preview geometry and offset state
     */
    public TreeLayoutEditorCanvas(final TreeLayoutEditorModel model) {
        this.model = Objects.requireNonNull(model, MODEL);
        setBackground(CanvasTheme.BG);
        setOpaque(true);
        setFocusable(true);
        setToolTipText(EMPTY);

        final MouseAdapter mouse = new CanvasMouseHandler();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
        addKeyListener(new CanvasKeyHandler());
    }

    /**
     * Registers a listener invoked whenever the user changes node selection on the canvas.
     *
     * @param listener callback receiving the selected scenario FQN or {@code null} when cleared
     */
    public void setOnSelectionChanged(final Consumer<String> listener) {
        callbacks.setOnSelectionChanged(listener);
    }

    /**
     * Registers a listener invoked when the user nudges the selected node via arrow keys.
     *
     * @param listener callback receiving {@code (deltaCol, deltaRow)}
     */
    public void setOnNudge(final BiConsumer<Integer, Integer> listener) {
        callbacks.setOnNudge(listener);
    }

    /**
     * Registers a listener invoked when the user triggers offset reset for the selected node.
     *
     * @param listener callback to execute on reset
     */
    public void setOnResetSelected(final Runnable listener) {
        callbacks.setOnResetSelected(listener);
    }

    /**
     * Updates the highlighted scenario class name and repaints if it changed.
     *
     * @param className fully-qualified scenario class name, or {@code null} to clear selection
     */
    public void setSelectedClassName(final String className) {
        if (!Objects.equals(selectedClassName, className)) {
            selectedClassName = className;
            repaint();
        }
    }

    /**
     * Toggles visibility of the background column/row grid and rulers.
     *
     * @param showGrid {@code true} to display grid lines and rulers
     */
    public void setShowGrid(final boolean showGrid) {
        this.showGrid = showGrid;
        repaint();
    }

    /**
     * Toggles visibility of pre-compaction raw ghost positions for shifted nodes.
     *
     * @param showRawGhosts {@code true} to draw raw position ghost outlines
     */
    public void setShowRawGhosts(final boolean showRawGhosts) {
        this.showRawGhosts = showRawGhosts;
        repaint();
    }

    /**
     * Resets zoom to 100% and pan to the default top-left margin.
     */
    public void resetZoom() {
        viewport.reset();
        repaint();
    }

    /**
     * Scales and centers the entire preview layout inside the current canvas viewport.
     */
    public void fitToView() {
        final TreeLayoutEditorModel.PreviewResult preview = model.latestPreview();
        if (preview.layout().nodes().isEmpty() || 0 >= getWidth() || 0 >= getHeight()) {
            resetZoom();
        } else {
            viewport.fit(getWidth(), getHeight(), preview.totalCols(), preview.totalRows());
            repaint();
        }
    }

    /**
     * Centers the viewport on the first node matching {@code className}.
     *
     * @param className fully-qualified scenario class name to center
     * @param boostZoom {@code true} to raise low zoom levels for legibility
     */
    public void focusNode(final String className, final boolean boostZoom) {
        if (null != className && 0 < getWidth() && 0 < getHeight()) {
            final List<TreeHtml.NodeData> nodes = model.latestPreview().nodesByClassName().get(className);
            if (null != nodes && !nodes.isEmpty()) {
                final TreeHtml.NodeData target = nodes.getFirst();
                viewport.focusOn(getWidth(), getHeight(), target.col(), target.row(), boostZoom);
                repaint();
            }
        }
    }

    /**
     * Finds the topmost scenario node card containing the given screen coordinates.
     *
     * @param screenX X coordinate in canvas pixels
     * @param screenY Y coordinate in canvas pixels
     * @return matching node, or {@code null} if none was hit
     */
    public TreeHtml.NodeData hitTest(final int screenX, final int screenY) {
        final double wx = viewport.toWorldX(screenX);
        final double wy = viewport.toWorldY(screenY);
        final List<TreeHtml.NodeData> nodes = model.latestPreview().layout().nodes();
        TreeHtml.NodeData hit = null;
        for (int i = nodes.size() - 1; 0 <= i && null == hit; i--) {
            final TreeHtml.NodeData node = nodes.get(i);
            if (containsWorldPoint(node, wx, wy)) {
                hit = node;
            }
        }
        return hit;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            final TreeLayoutEditorModel.PreviewResult preview = model.latestPreview();
            final List<TreeHtml.NodeData> nodes = preview.layout().nodes();
            if (nodes.isEmpty()) {
                CanvasRenderer.paintPlaceholder(g2, getWidth(), getHeight());
            } else {
                paintScene(g2, preview, nodes.size());
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintScene(
            final Graphics2D g2,
            final TreeLayoutEditorModel.PreviewResult preview,
            final int nodeCount) {
        g2.translate(viewport.panX(), viewport.panY());
        g2.scale(viewport.zoom(), viewport.zoom());

        if (showGrid) {
            renderer.paintGrid(g2, preview, viewport.zoom());
        }

        final Set<TreeHtml.NodeData> selectedNodes = Collections.newSetFromMap(new IdentityHashMap<>(nodeCount));
        final Set<TreeHtml.NodeData> subtreeNodes = Collections.newSetFromMap(new IdentityHashMap<>(nodeCount));
        final Set<TreeHtml.NodeData> ancestorNodes = Collections.newSetFromMap(new IdentityHashMap<>(nodeCount));
        collectSelectionHighlights(preview, selectedClassName, selectedNodes, subtreeNodes, ancestorNodes);

        if (showRawGhosts && model.previewSettings().compacted) {
            renderer.paintRawGhosts(g2, preview);
        }

        renderer.paintEdges(g2, preview.layout().edges(), subtreeNodes, ancestorNodes);
        renderer.paintNodes(g2, model, preview, selectedNodes, subtreeNodes, hoveredNodeId, viewport.zoom());
    }

    private void zoomAroundPoint(final double screenX, final double screenY, final double factor) {
        viewport.zoomAround(screenX, screenY, viewport.zoom() * factor);
        repaint();
    }

    private static boolean containsWorldPoint(final TreeHtml.NodeData node, final double wx, final double wy) {
        final double nx = nodeX(node.col());
        final double ny = nodeY(node.row());
        final boolean inX = wx >= nx && wx <= nx + CARD_WIDTH;
        final boolean inY = wy >= ny && wy <= ny + CARD_HEIGHT;
        return inX && inY;
    }

    private static void collectSelectionHighlights(
            final TreeLayoutEditorModel.PreviewResult preview,
            final String className,
            final Collection<? super TreeHtml.NodeData> selectedNodes,
            final Set<? super TreeHtml.NodeData> subtreeNodes,
            final Collection<? super TreeHtml.NodeData> ancestorNodes) {
        if (null != className) {
            final List<TreeHtml.NodeData> matched = preview.nodesByClassName().get(className);
            if (null != matched) {
                for (final TreeHtml.NodeData sel : matched) {
                    selectedNodes.add(sel);
                    collectSubtree(sel, subtreeNodes);
                    collectAncestors(sel, ancestorNodes);
                }
            }
        }
    }

    private static void collectAncestors(
            final TreeHtml.NodeData start,
            final Collection<? super TreeHtml.NodeData> out) {
        TreeHtml.NodeData cur = start;
        while (null != cur) {
            out.add(cur);
            cur = cur.parent();
        }
    }

    private static void collectSubtree(final TreeHtml.NodeData root, final Set<? super TreeHtml.NodeData> out) {
        if (null != root && out.add(root)) {
            for (final TreeHtml.NodeData child : root.children()) {
                collectSubtree(child, out);
            }
        }
    }

    private static double nodeX(final int col) {
        return WORLD_PAD_X + col * COL_WIDTH;
    }

    private static double nodeY(final double row) {
        return WORLD_PAD_Y + (row - 1.0) * ROW_HEIGHT;
    }

    private static int toInt(final double value) {
        return Math.toIntExact(Math.round(value));
    }

    private enum CanvasTheme {
        ;

        private static final Color BG = new Color(0x0F, 0x17, 0x2A);
        private static final Color GRID_LINE = new Color(0x1E, 0x29, 0x3B);
        private static final Color RULER_TEXT = new Color(0x47, 0x55, 0x69);

        private static final Color EDGE_NORMAL = new Color(0x47, 0x55, 0x69);
        private static final Color EDGE_DIMMED = new Color(0x33, 0x41, 0x55, 160);
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

        private static final float[] DASH_PATTERN = {5.0f, 4.0f};
        private static final Stroke STROKE_THIN = new BasicStroke(1.2f);
        private static final Stroke STROKE_EDGE = new BasicStroke(1.8f);
        private static final Stroke STROKE_EDGE_ACTIVE = new BasicStroke(2.8f);
        private static final Stroke STROKE_SELECTED = new BasicStroke(2.6f);
        private static final Stroke STROKE_DASHED = new BasicStroke(
                1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH_PATTERN, 0.0f);
    }

    private static final class CanvasCallbacks {

        private Consumer<String> onSelectionChanged = CanvasCallbacks::ignoreString;
        private BiConsumer<Integer, Integer> onNudge = CanvasCallbacks::ignoreNudge;
        private Runnable onResetSelected = CanvasCallbacks::noOp;

        private void setOnSelectionChanged(final Consumer<String> listener) {
            onSelectionChanged = (null == listener) ? CanvasCallbacks::ignoreString : listener;
        }

        private void setOnNudge(final BiConsumer<Integer, Integer> listener) {
            onNudge = (null == listener) ? CanvasCallbacks::ignoreNudge : listener;
        }

        private void setOnResetSelected(final Runnable listener) {
            onResetSelected = (null == listener) ? CanvasCallbacks::noOp : listener;
        }

        private void fireSelectionChanged(final String className) {
            onSelectionChanged.accept(className);
        }

        private void fireNudge(final int deltaCol, final int deltaRow) {
            onNudge.accept(deltaCol, deltaRow);
        }

        private void fireResetSelected() {
            onResetSelected.run();
        }

        private static void ignoreString(final String ignored) {
            // default no-op selection listener
        }

        private static void ignoreNudge(final Integer ignoredCol, final Integer ignoredRow) {
            // default no-op nudge listener
        }

        private static void noOp() {
            // default no-op reset listener
        }
    }

    private static final class ViewportState {

        private static final double MIN_ZOOM = 0.10;
        private static final double MAX_ZOOM = 2.50;
        private static final double INITIAL_ZOOM = 0.75;
        private static final double DEFAULT_ZOOM = 1.0;
        private static final double INITIAL_PAN = 24.0;
        private static final double FIT_MARGIN = 24.0;
        private static final double FOCUS_BOOST_THRESHOLD = 0.75;
        private static final double FOCUS_BOOST_ZOOM = 0.85;
        private static final int DRAG_THRESHOLD_PX = 4;

        private double zoom = INITIAL_ZOOM;
        private double panX = INITIAL_PAN;
        private double panY = INITIAL_PAN;
        private Point pressPoint = null;
        private double pressPanX = 0.0;
        private double pressPanY = 0.0;
        private boolean dragging = false;

        private double zoom() {
            return zoom;
        }

        private double panX() {
            return panX;
        }

        private double panY() {
            return panY;
        }

        private double toWorldX(final int screenX) {
            return (screenX - panX) / zoom;
        }

        private double toWorldY(final int screenY) {
            return (screenY - panY) / zoom;
        }

        private void reset() {
            zoom = DEFAULT_ZOOM;
            panX = INITIAL_PAN;
            panY = INITIAL_PAN;
        }

        private void fit(final int width, final int height, final int totalCols, final double totalRows) {
            final double worldW = WORLD_PAD_X * HALF + Math.max(1, totalCols) * COL_WIDTH;
            final double worldH = WORLD_PAD_Y * HALF + Math.max(1.0, totalRows) * ROW_HEIGHT;
            final double scaleX = (width - FIT_MARGIN) / worldW;
            final double scaleY = (height - FIT_MARGIN) / worldH;
            zoom = Math.clamp(Math.min(DEFAULT_ZOOM, Math.min(scaleX, scaleY)), MIN_ZOOM, MAX_ZOOM);
            panX = (width - worldW * zoom) / HALF;
            panY = (height - worldH * zoom) / HALF;
        }

        private void focusOn(
                final int width,
                final int height,
                final int col,
                final double row,
                final boolean boostZoom) {
            if (boostZoom && FOCUS_BOOST_THRESHOLD > zoom) {
                zoom = FOCUS_BOOST_ZOOM;
            }
            final double cx = nodeX(col) + CARD_WIDTH / HALF;
            final double cy = nodeY(row) + CARD_HEIGHT / HALF;
            panX = width / HALF - cx * zoom;
            panY = height / HALF - cy * zoom;
        }

        private void zoomAround(final double screenX, final double screenY, final double nextZoom) {
            final double clamped = Math.clamp(nextZoom, MIN_ZOOM, MAX_ZOOM);
            final double worldX = (screenX - panX) / zoom;
            final double worldY = (screenY - panY) / zoom;
            zoom = clamped;
            panX = screenX - worldX * zoom;
            panY = screenY - worldY * zoom;
        }

        private void beginPress(final Point point) {
            pressPoint = point;
            pressPanX = panX;
            pressPanY = panY;
            dragging = false;
        }

        private void updateDrag(final int currentX, final int currentY) {
            if (null != pressPoint) {
                final int dx = currentX - pressPoint.x;
                final int dy = currentY - pressPoint.y;
                if (!dragging && isPastDragThreshold(dx, dy)) {
                    dragging = true;
                }
                if (dragging) {
                    panX = pressPanX + dx;
                    panY = pressPanY + dy;
                }
            }
        }

        private boolean isDragging() {
            return dragging;
        }

        private void endDrag() {
            dragging = false;
        }

        private static boolean isPastDragThreshold(final int dx, final int dy) {
            return DRAG_THRESHOLD_PX <= Math.abs(dx) || DRAG_THRESHOLD_PX <= Math.abs(dy);
        }
    }

    private final class CanvasMouseHandler extends MouseAdapter {

        private static final int DOUBLE_CLICK_COUNT = 2;
        private static final double WHEEL_ZOOM_FACTOR = 1.12;

        @Override
        public void mousePressed(final MouseEvent e) {
            requestFocusInWindow();
            viewport.beginPress(e.getPoint());
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            viewport.updateDrag(e.getX(), e.getY());
            if (viewport.isDragging()) {
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                repaint();
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            if (viewport.isDragging()) {
                viewport.endDrag();
                updateCursorForPoint(e.getPoint());
            } else if (MouseEvent.BUTTON1 == e.getButton()) {
                selectAtPoint(e.getX(), e.getY(), e.getClickCount());
            }
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            final TreeHtml.NodeData hit = hitTest(e.getX(), e.getY());
            final String nextHover = (null == hit) ? null : hit.id();
            if (!Objects.equals(hoveredNodeId, nextHover)) {
                hoveredNodeId = nextHover;
                updateCursorForPoint(e.getPoint());
                setToolTipText(TooltipFormatter.buildTooltip(model, hit));
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
            final double factor = (0.0 > e.getPreciseWheelRotation())
                    ? WHEEL_ZOOM_FACTOR
                    : (1.0 / WHEEL_ZOOM_FACTOR);
            zoomAroundPoint(e.getX(), e.getY(), factor);
        }

        private void selectAtPoint(final int screenX, final int screenY, final int clickCount) {
            final TreeHtml.NodeData hit = hitTest(screenX, screenY);
            final String hitClass = (null == hit)
                    ? null
                    : model.latestPreview().classNameByNodeId().get(hit.id());
            setSelectedClassName(hitClass);
            callbacks.fireSelectionChanged(hitClass);
            if (null != hit && DOUBLE_CLICK_COUNT <= clickCount) {
                focusNode(hitClass, true);
            }
        }

        private void updateCursorForPoint(final Point point) {
            final boolean overNode = null != hitTest(point.x, point.y);
            setCursor(Cursor.getPredefinedCursor(overNode ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }
    }

    private final class CanvasKeyHandler extends KeyAdapter {

        private static final int DEFAULT_NUDGE_STEP = 1;
        private static final int SHIFT_NUDGE_STEP = 5;
        private static final double KEY_ZOOM_FACTOR = 1.18;

        private static final Map<Integer, int[]> NUDGE_DIRECTIONS = Map.of(
                KeyEvent.VK_LEFT, new int[]{-1, 0},
                KeyEvent.VK_RIGHT, new int[]{1, 0},
                KeyEvent.VK_UP, new int[]{0, -1},
                KeyEvent.VK_DOWN, new int[]{0, 1});
        private static final Set<Integer> RESET_KEYS = Set.of(
                KeyEvent.VK_0, KeyEvent.VK_NUMPAD0, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE);
        private static final Set<Integer> ZOOM_IN_KEYS = Set.of(
                KeyEvent.VK_EQUALS, KeyEvent.VK_PLUS, KeyEvent.VK_ADD);
        private static final Set<Integer> ZOOM_OUT_KEYS = Set.of(
                KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT);

        @Override
        public void keyPressed(final KeyEvent e) {
            final int step = e.isShiftDown() ? SHIFT_NUDGE_STEP : DEFAULT_NUDGE_STEP;
            final int keyCode = e.getKeyCode();
            final boolean consumed = isSelectionActionHandled(keyCode, step)
                    || isNavigationActionHandled(keyCode)
                    || isZoomActionHandled(keyCode);
            if (consumed) {
                e.consume();
            }
        }

        private boolean hasSelection() {
            return null != selectedClassName;
        }

        private boolean isSelectionActionHandled(final int keyCode, final int step) {
            boolean handled = false;
            if (hasSelection()) {
                final int[] dir = NUDGE_DIRECTIONS.get(keyCode);
                if (null != dir) {
                    callbacks.fireNudge(dir[0] * step, dir[1] * step);
                    handled = true;
                } else if (RESET_KEYS.contains(keyCode)) {
                    callbacks.fireResetSelected();
                    handled = true;
                }
            }
            return handled;
        }

        private boolean isNavigationActionHandled(final int keyCode) {
            boolean handled = true;
            if (KeyEvent.VK_ESCAPE == keyCode) {
                setSelectedClassName(null);
                callbacks.fireSelectionChanged(null);
            } else if (KeyEvent.VK_F == keyCode) {
                fitToView();
            } else {
                handled = false;
            }
            return handled;
        }

        private boolean isZoomActionHandled(final int keyCode) {
            boolean handled = true;
            if (ZOOM_IN_KEYS.contains(keyCode)) {
                zoomAroundPoint(getWidth() / HALF, getHeight() / HALF, KEY_ZOOM_FACTOR);
            } else if (ZOOM_OUT_KEYS.contains(keyCode)) {
                zoomAroundPoint(getWidth() / HALF, getHeight() / HALF, 1.0 / KEY_ZOOM_FACTOR);
            } else {
                handled = false;
            }
            return handled;
        }
    }

    private static final class CanvasRenderer {

        private static final int PLACEHOLDER_MIN_X = 16;
        private static final int PLACEHOLDER_MIN_Y = 32;
        private static final double GHOST_ROW_EPSILON = 0.05;
        private static final double GRID_COL_OFFSET = 20.0;
        private static final double GRID_ROW_TOP_OFFSET = 16.0;
        private static final double GRID_ROW_OFFSET = 6.0;
        private static final double RULER_COL_X_OFFSET = 4.0;
        private static final double RULER_COL_Y_OFFSET = 22.0;
        private static final double RULER_ROW_X_OFFSET = 46.0;
        private static final double RULER_ROW_Y_OFFSET = 26.0;

        private static final String RULER_COL_PREFIX = c(s(C));
        private static final String RULER_ROW_PREFIX = c(s(R));
        private static final String PLACEHOLDER_MESSAGE = b(
                s(NO_SCENARIOS_LOADED, _DOT_),
                c(CHECK),
                s(_DOT_, TEST, _DOT_, ROOT),
                AND,
                CLICK,
                s(DBL_QUO, b(c(RELOAD), FROM, c(SOURCES)), DBL_QUO, _DOT_));

        private final Line2D.Double reusableLine = new Line2D.Double();
        private final RoundRectangle2D.Double reusableGhost = new RoundRectangle2D.Double();
        private final CubicCurve2D.Double reusableCurve = new CubicCurve2D.Double();
        private final NodeCardRenderer nodeCardRenderer = new NodeCardRenderer();

        private static void paintPlaceholder(final Graphics2D g2, final int width, final int height) {
            g2.setFont(CanvasTheme.PLACEHOLDER_FONT);
            g2.setColor(CanvasTheme.TEXT_MUTED);
            final FontMetrics fm = g2.getFontMetrics();
            final int x = Math.max(PLACEHOLDER_MIN_X, (width - fm.stringWidth(PLACEHOLDER_MESSAGE)) / 2);
            final int y = Math.max(PLACEHOLDER_MIN_Y, height / 2);
            g2.drawString(PLACEHOLDER_MESSAGE, x, y);
        }

        private void paintGrid(
                final Graphics2D g2,
                final TreeLayoutEditorModel.PreviewResult preview,
                final double zoom) {
            final int cols = Math.max(1, preview.totalCols());
            final int rows = Math.max(1, toInt(Math.ceil(preview.totalRows())));
            final double xEnd = WORLD_PAD_X + cols * COL_WIDTH;
            final double yEnd = WORLD_PAD_Y + rows * ROW_HEIGHT;

            g2.setColor(CanvasTheme.GRID_LINE);
            g2.setStroke(CanvasTheme.STROKE_THIN);
            paintGridColumns(g2, cols, yEnd);
            paintGridRows(g2, rows, xEnd);

            if (MIN_DETAIL_ZOOM <= zoom) {
                g2.setFont(CanvasTheme.RULER_FONT);
                g2.setColor(CanvasTheme.RULER_TEXT);
                paintColumnRuler(g2, cols);
                paintRowRuler(g2, rows);
            }
        }

        private void paintGridColumns(final Graphics2D g2, final int cols, final double yEnd) {
            for (int col = 0; col <= cols; col++) {
                final double x = WORLD_PAD_X + col * COL_WIDTH - GRID_COL_OFFSET;
                reusableLine.setLine(x, WORLD_PAD_Y - GRID_ROW_TOP_OFFSET, x, yEnd);
                g2.draw(reusableLine);
            }
        }

        private void paintGridRows(final Graphics2D g2, final int rows, final double xEnd) {
            for (int row = 0; row <= rows; row++) {
                final double y = WORLD_PAD_Y + row * ROW_HEIGHT - GRID_ROW_OFFSET;
                reusableLine.setLine(WORLD_PAD_X - GRID_COL_OFFSET, y, xEnd, y);
                g2.draw(reusableLine);
            }
        }

        private static void paintColumnRuler(final Graphics2D g2, final int cols) {
            final int y = toInt(WORLD_PAD_Y - RULER_COL_Y_OFFSET);
            for (int col = 0; col < cols; col++) {
                g2.drawString(s(RULER_COL_PREFIX, col), toInt(nodeX(col) + RULER_COL_X_OFFSET), y);
            }
        }

        private static void paintRowRuler(final Graphics2D g2, final int rows) {
            final int x = toInt(WORLD_PAD_X - RULER_ROW_X_OFFSET);
            for (int row = 1; row <= rows; row++) {
                g2.drawString(s(RULER_ROW_PREFIX, row), x, toInt(nodeY(row) + RULER_ROW_Y_OFFSET));
            }
        }

        private void paintRawGhosts(final Graphics2D g2, final TreeLayoutEditorModel.PreviewResult preview) {
            final Map<String, TreeLayoutEditorModel.Point2DGrid> rawPositions = preview.rawPositionByNodeId();
            g2.setStroke(CanvasTheme.STROKE_DASHED);
            g2.setColor(CanvasTheme.GHOST_COLOR);

            for (final TreeHtml.NodeData node : preview.layout().nodes()) {
                final TreeLayoutEditorModel.Point2DGrid raw = rawPositions.get(node.id());
                if (hasMovedFromRaw(node, raw)) {
                    final double gx = nodeX(raw.col());
                    final double gy = nodeY(raw.row());
                    final double nx = nodeX(node.col());
                    final double ny = nodeY(node.row());
                    reusableGhost.setRoundRect(gx, gy, CARD_WIDTH, CARD_HEIGHT, CARD_ARC, CARD_ARC);
                    g2.draw(reusableGhost);
                    reusableLine.setLine(
                            gx + CARD_WIDTH / HALF, gy + CARD_HEIGHT / HALF,
                            nx + CARD_WIDTH / HALF, ny + CARD_HEIGHT / HALF);
                    g2.draw(reusableLine);
                }
            }
        }

        private static boolean hasMovedFromRaw(
                final TreeHtml.NodeData node,
                final TreeLayoutEditorModel.Point2DGrid raw) {
            return hasOffset(node) && isDisplacedFromRaw(node, raw);
        }

        private static boolean hasOffset(final TreeHtml.NodeData node) {
            return 0 != node.colOffset() || 0 != node.rowOffset();
        }

        private static boolean isDisplacedFromRaw(
                final TreeHtml.NodeData node,
                final TreeLayoutEditorModel.Point2DGrid raw) {
            return null != raw
                    && (raw.col() != node.col() || GHOST_ROW_EPSILON <= Math.abs(raw.row() - node.row()));
        }

        private void paintEdges(
                final Graphics2D g2,
                final Iterable<TreeHtml.Edge> edges,
                final Collection<TreeHtml.NodeData> subtreeNodes,
                final Collection<TreeHtml.NodeData> ancestorNodes) {
            final boolean hasSelection = !subtreeNodes.isEmpty();
            for (final TreeHtml.Edge edge : edges) {
                final TreeHtml.NodeData from = edge.from();
                final TreeHtml.NodeData to = edge.to();
                applyEdgeStyle(g2,
                        ancestorNodes.contains(from) && ancestorNodes.contains(to),
                        subtreeNodes.contains(from) && subtreeNodes.contains(to),
                        hasSelection);

                final double x1 = nodeX(from.col()) + CARD_WIDTH;
                final double y1 = nodeY(from.row()) + CARD_HEIGHT / HALF;
                final double x2 = nodeX(to.col());
                final double y2 = nodeY(to.row()) + CARD_HEIGHT / HALF;
                final double mx = (x1 + x2) / HALF;
                reusableCurve.setCurve(x1, y1, mx, y1, mx, y2, x2, y2);
                g2.draw(reusableCurve);
            }
        }

        private static void applyEdgeStyle(
                final Graphics2D g2,
                final boolean isAncestorEdge,
                final boolean isSubtreeEdge,
                final boolean hasSelection) {
            if (isAncestorEdge) {
                g2.setColor(CanvasTheme.EDGE_ANCESTOR);
                g2.setStroke(CanvasTheme.STROKE_EDGE_ACTIVE);
            } else if (isSubtreeEdge) {
                g2.setColor(CanvasTheme.EDGE_SUBTREE);
                g2.setStroke(CanvasTheme.STROKE_EDGE_ACTIVE);
            } else {
                g2.setColor(hasSelection ? CanvasTheme.EDGE_DIMMED : CanvasTheme.EDGE_NORMAL);
                g2.setStroke(CanvasTheme.STROKE_EDGE);
            }
        }

        private void paintNodes(
                final Graphics2D g2,
                final TreeLayoutEditorModel model,
                final TreeLayoutEditorModel.PreviewResult preview,
                final Collection<TreeHtml.NodeData> selectedNodes,
                final Collection<TreeHtml.NodeData> subtreeNodes,
                final String hoveredNodeId,
                final double zoom) {
            nodeCardRenderer.paintNodes(g2, model, preview, selectedNodes, subtreeNodes, hoveredNodeId, zoom);
        }
    }

    private static final class NodeCardRenderer {

        private static final int SINGLE_CHAR_MIN_LENGTH = 1;
        private static final double MIN_LABEL_ZOOM = 0.24;
        private static final double PILL_ARC = 9.0;
        private static final double DIRTY_DOT_OFFSET_X = 12.0;
        private static final double DIRTY_DOT_OFFSET_Y = 6.0;
        private static final int DIRTY_DOT_DIAMETER = 7;
        private static final double BADGE_CHAR_WIDTH = 6.5;
        private static final double BADGE_RESERVE_PAD = 22.0;
        private static final double DIRTY_RESERVE_WIDTH = 18.0;
        private static final double CLEAN_RESERVE_WIDTH = 10.0;
        private static final double MIN_TEXT_WIDTH = 40.0;
        private static final double CARD_TEXT_RESERVE = 18.0;
        private static final double CARD_TEXT_X_OFFSET = 10.0;
        private static final double CARD_TEXT_Y_OFFSET = 27.0;
        private static final int BADGE_WIDTH_PAD = 10;
        private static final int BADGE_HEIGHT = 18;
        private static final double BADGE_RIGHT_MARGIN_DIRTY = 16.0;
        private static final double BADGE_RIGHT_MARGIN_CLEAN = 8.0;
        private static final double BADGE_TEXT_X_OFFSET = 5.0;
        private static final double BADGE_TEXT_Y_OFFSET = 13.0;

        private static final String ELLIPSIS_STR = s(ELLIPSIS);
        private static final String FMT_BOTH_BADGE = s(_PLUS_, FMT_PERCENT_D, _COMMA_, FMT_PERCENT_PLUS_D);

        private final RoundRectangle2D.Double reusableCard = new RoundRectangle2D.Double();
        private final RoundRectangle2D.Double reusablePill = new RoundRectangle2D.Double();

        private void paintNodes(
                final Graphics2D g2,
                final TreeLayoutEditorModel model,
                final TreeLayoutEditorModel.PreviewResult preview,
                final Collection<TreeHtml.NodeData> selectedNodes,
                final Collection<TreeHtml.NodeData> subtreeNodes,
                final String hoveredNodeId,
                final double zoom) {
            final boolean drawLabels = MIN_LABEL_ZOOM <= zoom;
            final boolean drawBadges = MIN_DETAIL_ZOOM <= zoom;

            for (final TreeHtml.NodeData node : preview.layout().nodes()) {
                final String className = preview.classNameByNodeId().get(node.id());
                final ScenarioOffsetSourceCodec.Offsets cur = (null == className)
                        ? ScenarioOffsetSourceCodec.Offsets.ZERO
                        : model.currentOffsets(className);
                final boolean dirty = null != className && model.isModified(className);
                paintSingleNode(g2, node, cur, dirty,
                        selectedNodes.contains(node),
                        subtreeNodes.contains(node),
                        Objects.equals(hoveredNodeId, node.id()),
                        drawLabels, drawBadges);
            }
        }

        private void paintSingleNode(
                final Graphics2D g2,
                final TreeHtml.NodeData node,
                final ScenarioOffsetSourceCodec.Offsets cur,
                final boolean dirty,
                final boolean selected,
                final boolean inSubtree,
                final boolean hovered,
                final boolean drawLabels,
                final boolean drawBadges) {
            final boolean shifted = !cur.isZero();
            final double x = nodeX(node.col());
            final double y = nodeY(node.row());
            reusableCard.setRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, CARD_ARC, CARD_ARC);

            g2.setColor(resolveCardFill(selected, inSubtree, shifted));
            g2.fill(reusableCard);

            applyCardBorder(g2, selected, dirty, shifted, hovered);
            g2.draw(reusableCard);

            if (dirty) {
                g2.setColor(CanvasTheme.DIRTY_COLOR);
                g2.fillOval(
                        toInt(x + CARD_WIDTH - DIRTY_DOT_OFFSET_X),
                        toInt(y + DIRTY_DOT_OFFSET_Y),
                        DIRTY_DOT_DIAMETER,
                        DIRTY_DOT_DIAMETER);
            }

            if (drawLabels) {
                paintNodeLabelAndBadge(g2, node, cur, x, y, shifted, dirty, selected, inSubtree, drawBadges);
            }
        }

        private static Color resolveCardFill(
                final boolean selected,
                final boolean inSubtree,
                final boolean shifted) {
            final Color fill;
            if (selected) {
                fill = CanvasTheme.CARD_SELECTED_FILL;
            } else if (inSubtree) {
                fill = CanvasTheme.CARD_SUBTREE_FILL;
            } else {
                fill = shifted ? CanvasTheme.CARD_SHIFTED_FILL : CanvasTheme.CARD_FILL;
            }
            return fill;
        }

        private static void applyCardBorder(
                final Graphics2D g2,
                final boolean selected,
                final boolean dirty,
                final boolean shifted,
                final boolean hovered) {
            if (selected) {
                g2.setColor(CanvasTheme.CARD_SELECTED_STROKE);
                g2.setStroke(CanvasTheme.STROKE_SELECTED);
            } else if (dirty || shifted) {
                g2.setColor(dirty ? CanvasTheme.DIRTY_COLOR : CanvasTheme.CARD_SHIFTED_STROKE);
                g2.setStroke(CanvasTheme.STROKE_EDGE);
            } else {
                g2.setColor(hovered ? CanvasTheme.CARD_HOVER_STROKE : CanvasTheme.CARD_STROKE);
                g2.setStroke(hovered ? CanvasTheme.STROKE_EDGE : CanvasTheme.STROKE_THIN);
            }
        }

        private void paintNodeLabelAndBadge(
                final Graphics2D g2,
                final TreeHtml.NodeData node,
                final ScenarioOffsetSourceCodec.Offsets cur,
                final double x,
                final double y,
                final boolean shifted,
                final boolean dirty,
                final boolean selected,
                final boolean inSubtree,
                final boolean drawBadges) {
            final String badge = (drawBadges && shifted) ? formatBadge(cur) : null;
            final double badgeReserve = computeBadgeReserve(badge, dirty);
            final int maxTextWidth = toInt(Math.max(MIN_TEXT_WIDTH, CARD_WIDTH - CARD_TEXT_RESERVE - badgeReserve));

            g2.setFont((selected || shifted) ? CanvasTheme.CARD_FONT_BOLD : CanvasTheme.CARD_FONT);
            final boolean highlighted = selected || shifted || inSubtree;
            final boolean emphasized = highlighted || !node.children().isEmpty();
            g2.setColor(emphasized ? CanvasTheme.TEXT_PRIMARY : CanvasTheme.TEXT_MUTED);
            final String text = ellipsize(node.label(), g2.getFontMetrics(), maxTextWidth);
            g2.drawString(text, toInt(x + CARD_TEXT_X_OFFSET), toInt(y + CARD_TEXT_Y_OFFSET));

            if (null != badge) {
                paintBadgePill(g2, badge, x, y, dirty);
            }
        }

        private void paintBadgePill(
                final Graphics2D g2,
                final String badge,
                final double cardX,
                final double cardY,
                final boolean dirty) {
            g2.setFont(CanvasTheme.BADGE_FONT);
            final FontMetrics bfm = g2.getFontMetrics();
            final int bw = bfm.stringWidth(badge) + BADGE_WIDTH_PAD;
            final double rightMargin = dirty ? BADGE_RIGHT_MARGIN_DIRTY : BADGE_RIGHT_MARGIN_CLEAN;
            final double bx = cardX + CARD_WIDTH - bw - rightMargin;
            final double by = cardY + (CARD_HEIGHT - BADGE_HEIGHT) / HALF;
            reusablePill.setRoundRect(bx, by, bw, BADGE_HEIGHT, PILL_ARC, PILL_ARC);
            final Shape pill = reusablePill;
            g2.setColor(CanvasTheme.BADGE_BG);
            g2.fill(pill);
            g2.setColor(dirty ? CanvasTheme.DIRTY_COLOR : CanvasTheme.CARD_SHIFTED_STROKE);
            g2.setStroke(CanvasTheme.STROKE_THIN);
            g2.draw(pill);
            g2.setColor(dirty ? CanvasTheme.DIRTY_COLOR : CanvasTheme.BADGE_TEXT);
            g2.drawString(badge, toInt(bx + BADGE_TEXT_X_OFFSET), toInt(by + BADGE_TEXT_Y_OFFSET));
        }

        private static double computeBadgeReserve(final CharSequence badge, final boolean dirty) {
            final double reserve;
            if (null == badge) {
                reserve = dirty ? DIRTY_RESERVE_WIDTH : CLEAN_RESERVE_WIDTH;
            } else {
                reserve = badge.length() * BADGE_CHAR_WIDTH + BADGE_RESERVE_PAD;
            }
            return reserve;
        }

        private static String formatBadge(final ScenarioOffsetSourceCodec.Offsets offsets) {
            final String formatted;
            if (0 != offsets.colOffset() && 0 != offsets.rowOffset()) {
                formatted = String.format(Locale.ROOT, FMT_BOTH_BADGE, offsets.colOffset(), offsets.rowOffset());
            } else if (0 != offsets.colOffset()) {
                formatted = String.format(Locale.ROOT, FMT_COL_TAG, offsets.colOffset());
            } else {
                formatted = String.format(Locale.ROOT, FMT_ROW_TAG, offsets.rowOffset());
            }
            return formatted;
        }

        private static String ellipsize(final String text, final FontMetrics fm, final int maxWidth) {
            String result = text;
            if (maxWidth < fm.stringWidth(text)) {
                final int ellW = fm.stringWidth(ELLIPSIS_STR);
                int end = text.length();
                while (SINGLE_CHAR_MIN_LENGTH < end && maxWidth < fm.stringWidth(text.substring(0, end)) + ellW) {
                    end--;
                }
                result = s(text.substring(0, end), ELLIPSIS_STR);
            }
            return result;
        }
    }

    private enum TooltipFormatter {
        ;

        private static final int INITIAL_TOOLTIP_CAPACITY = 256;
        private static final double RAW_ROW_TOOLTIP_EPSILON = 0.01;

        private static final String TAG_BR = s(LSS_THN, BR, _SLASH_, GRT_THN);
        private static final String TAG_HTML_OPEN = s(
                LSS_THN, HTML, GRT_THN, LSS_THN, BODY, _SPACE_,
                S, T, Y, L, E, _EQUAL_, _QUOTE_,
                F, O, N, T, _DASH_, F, A, M, I, L, Y, _COLON_, M, O, N, O, S, P, A, C, E, SEM_CLN,
                P, A, D, D, I, N, G, _COLON_, _2_, P, X, _QUOTE_, GRT_THN);
        private static final String TAG_B_OPEN = s(LSS_THN, B, GRT_THN);
        private static final String TAG_B_CLOSE_BR = s(LSS_THN, _SLASH_, B, GRT_THN, TAG_BR);
        private static final String TAG_SPAN_OPEN = s(
                LSS_THN, SPAN, _SPACE_, S, T, Y, L, E, _EQUAL_, _QUOTE_,
                C, O, L, O, R, _COLON_, _NUMBR_, _6_, _4_, _7_, _4_, _8_, B, _QUOTE_, GRT_THN);
        private static final String TAG_SPAN_CLOSE_BR = s(LSS_THN, _SLASH_, SPAN, GRT_THN, TAG_BR);
        private static final String TAG_HTML_CLOSE = s(
                LSS_THN, _SLASH_, BODY, GRT_THN, LSS_THN, _SLASH_, HTML, GRT_THN);

        private static final String FMT_GRID_TOOLTIP = b(
                s(c(GRID), _COLON_),
                s(COL, _EQUAL_, FMT_PERCENT_D, _COMMA_),
                s(ROW, _EQUAL_, FMT_PERCENT_2F));
        private static final String FMT_RAW_TOOLTIP = s(_SPACE_, e(ROUND, b(
                s(RAW, _COLON_),
                s(COL, _EQUAL_, FMT_PERCENT_D, _COMMA_),
                s(ROW, _EQUAL_, FMT_PERCENT_2F))));
        private static final String FMT_OFFSETS_TOOLTIP = b(
                s(c(OFFSETS), _COLON_),
                s(COL, _EQUAL_, FMT_PERCENT_PLUS_D, _COMMA_),
                s(ROW, _EQUAL_, FMT_PERCENT_PLUS_D));
        private static final String FMT_SAVED_TOOLTIP = s(
                _SPACE_, LSS_THN, I, GRT_THN,
                e(ROUND, b(
                        s(SAVED, _COLON_),
                        s(COL, _EQUAL_, FMT_PERCENT_PLUS_D, _COMMA_),
                        s(ROW, _EQUAL_, FMT_PERCENT_PLUS_D))),
                LSS_THN, _SLASH_, I, GRT_THN);

        private static String buildTooltip(final TreeLayoutEditorModel model, final TreeHtml.NodeData node) {
            String tooltip = null;
            if (null != node) {
                final TreeLayoutEditorModel.PreviewResult preview = model.latestPreview();
                final String className = preview.classNameByNodeId().get(node.id());
                if (null != className) {
                    tooltip = formatTooltipHtml(model, preview, node, className);
                }
            }
            return tooltip;
        }

        private static String formatTooltipHtml(
                final TreeLayoutEditorModel model,
                final TreeLayoutEditorModel.PreviewResult preview,
                final TreeHtml.NodeData node,
                final String className) {
            final ScenarioOffsetSourceCodec.Offsets cur = model.currentOffsets(className);
            final ScenarioOffsetSourceCodec.Offsets init = model.initialOffsets(className);
            final TreeLayoutEditorModel.Point2DGrid raw = preview.rawPositionByNodeId().get(node.id());

            final StringBuilder sb = new StringBuilder(INITIAL_TOOLTIP_CAPACITY);
            sb.append(TAG_HTML_OPEN)
                    .append(TAG_B_OPEN).append(escapeHtml(node.label())).append(TAG_B_CLOSE_BR)
                    .append(TAG_SPAN_OPEN).append(escapeHtml(className)).append(TAG_SPAN_CLOSE_BR)
                    .append(String.format(Locale.ROOT, FMT_GRID_TOOLTIP, node.col(), node.row()));
            if (hasRawDelta(node, raw)) {
                sb.append(String.format(Locale.ROOT, FMT_RAW_TOOLTIP, raw.col(), raw.row()));
            }
            sb.append(TAG_BR)
                    .append(String.format(Locale.ROOT, FMT_OFFSETS_TOOLTIP, cur.colOffset(), cur.rowOffset()));
            if (!cur.equals(init)) {
                sb.append(String.format(Locale.ROOT, FMT_SAVED_TOOLTIP, init.colOffset(), init.rowOffset()));
            }
            sb.append(TAG_HTML_CLOSE);
            return sb.toString();
        }

        private static boolean hasRawDelta(
                final TreeHtml.NodeData node,
                final TreeLayoutEditorModel.Point2DGrid raw) {
            return null != raw
                    && (raw.col() != node.col() || RAW_ROW_TOOLTIP_EPSILON < Math.abs(raw.row() - node.row()));
        }

        private static String escapeHtml(final String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
