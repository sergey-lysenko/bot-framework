package works.lysenko.base.ui;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.g._Grid;
import works.lysenko.util.apis.grid.r._ColourSpreadRequest;
import works.lysenko.util.apis.grid.t._Geometry;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.apis.grid.t._RelativePosition;
import works.lysenko.util.data.records.diff.Pair;
import works.lysenko.util.data.type.Grid;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.gsrc.ColourSearchRequest;
import works.lysenko.util.grid.record.gsrc.ColoursSearchResult;
import works.lysenko.util.grid.record.gsrc.Geometry;
import works.lysenko.util.grid.record.gsrc.Resolution;
import works.lysenko.util.grid.record.misc.DrawGridResult;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.shared.lang.word.R.RULES;
import static works.lysenko.util.apis.grid.d._Resolution.readResolution;
import static works.lysenko.util.chrs.__.AN;
import static works.lysenko.util.chrs.__.AS;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.ADD;
import static works.lysenko.util.chrs.___.FIT;
import static works.lysenko.util.chrs.___.PNG;
import static works.lysenko.util.chrs.___.SRC;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.type.RelativePosition.rp;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.func.imgs.Painter.drawArea;
import static works.lysenko.util.func.imgs.Painter.drawGrid;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.grid.Fields.CDA;
import static works.lysenko.util.grid.Fields.CDB;
import static works.lysenko.util.grid.Fields.CDI;
import static works.lysenko.util.grid.record.gsrc.ColourSearchRequest.csr;
import static works.lysenko.util.grid.record.gsrc.Region.search;
import static works.lysenko.util.grid.record.gsrc.Routines.getRequestedGridLocation;
import static works.lysenko.util.lang.F.FMT_PERCENT_S;
import static works.lysenko.util.lang.P.PCT_100;
import static works.lysenko.util.lang.word.A.AMOUNT;
import static works.lysenko.util.lang.word.A.APPLIED;
import static works.lysenko.util.lang.word.A.AREA;
import static works.lysenko.util.lang.word.B.BORDER;
import static works.lysenko.util.lang.word.C.CENTER;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.C.COUNT;
import static works.lysenko.util.lang.word.C.CUTOFF;
import static works.lysenko.util.lang.word.D.DENSITY;
import static works.lysenko.util.lang.word.D.DETECTED;
import static works.lysenko.util.lang.word.E.EDITOR;
import static works.lysenko.util.lang.word.E.EXCEPTION;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.H.HELPER;
import static works.lysenko.util.lang.word.H.HORIZONTAL;
import static works.lysenko.util.lang.word.I.IGNORE;
import static works.lysenko.util.lang.word.I.IMAGE;
import static works.lysenko.util.lang.word.L.LOADED;
import static works.lysenko.util.lang.word.M.MASK;
import static works.lysenko.util.lang.word.O.OCCURED;
import static works.lysenko.util.lang.word.O.ORDER;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.P.PIXEL;
import static works.lysenko.util.lang.word.P.PROBLEM;
import static works.lysenko.util.lang.word.Q.QUOTAS;
import static works.lysenko.util.lang.word.R.RADIUS;
import static works.lysenko.util.lang.word.R.RECALCULATE;
import static works.lysenko.util.lang.word.R.RESOLUTION;
import static works.lysenko.util.lang.word.R.RESOURCES;
import static works.lysenko.util.lang.word.S.SAMPLES;
import static works.lysenko.util.lang.word.S.SAVED;
import static works.lysenko.util.lang.word.S.SCALE;
import static works.lysenko.util.lang.word.S.SCREENSHOT;
import static works.lysenko.util.lang.word.S.SHARE;
import static works.lysenko.util.lang.word.S.SUCCESSFULLY;
import static works.lysenko.util.lang.word.T.TARGET;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.lang.word.V.VERTICAL;
import static works.lysenko.util.prop.grid.Defaults.*;
import static works.lysenko.util.spec.Layout.Paths._GRIDS_;
import static works.lysenko.util.spec.Layout.Paths._RUNS_;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Grid Parameters Editor &amp; Helper.
 * Provides interactive visual editing, live recalculation, keyboard nudging,
 * pixel inspection, dominant color sampling, and saving directly to .grid files.
 */
public final class Helper extends JFrame {

    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 850;
    private static final int SIDEBAR_WIDTH = 340;
    private static final int SIDEBAR_INNER_INSET = 30;
    private static final int SIDEBAR_MIN_WIDTH = 240;
    private static final int IMAGE_MIN_WIDTH = 300;
    private static final int PANE_MIN_HEIGHT = 200;
    private static final int SPLIT_DIVIDER_LOCATION = 360;
    private static final int SCROLL_UNIT_INCREMENT = 16;

    private static final int PAD_SMALL = 4;
    private static final int PAD_LARGE = 8;
    private static final int GAP_TINY = 2;
    private static final int GAP_SMALL = 4;
    private static final int GAP_MEDIUM = 5;
    private static final int GAP_ZOOM = 6;
    private static final int STRUT_SMALL = 4;
    private static final int STRUT_MEDIUM = 6;

    private static final float DEFAULT_IMAGE_SCALE = 1.0F;
    private static final float FILE_LABEL_FONT_SIZE = 11.0f;
    private static final int ARROW_BTN_WIDTH = 42;
    private static final int ARROW_BTN_HEIGHT = 26;
    private static final int COLOURS_AREA_ROWS = 3;
    private static final int COLOURS_AREA_COLS = 20;
    private static final int COLOURS_SCROLL_HEIGHT = 60;
    private static final int SWATCH_SIZE = 28;
    private static final int SWATCH_COL_MAX_WIDTH = 40;
    private static final int TABLE_SCROLL_HEIGHT = 110;
    private static final int FIELD_LABEL_WIDTH = 140;
    private static final int FIELD_LABEL_HEIGHT = 22;
    private static final int METRIC_LABEL_WIDTH = 80;
    private static final int METRIC_LABEL_HEIGHT = 20;

    private static final int ZOOM_MIN = 10;
    private static final int ZOOM_MAX = 500;
    private static final int ZOOM_DEFAULT = 100;
    private static final int ZOOM_WHEEL_STEP = 15;
    private static final float ZOOM_SCALE_DIVISOR = 100.0f;
    private static final float ZOOM_FIT_MARGIN = 0.95f;
    private static final int ZOOM_SLIDER_WIDTH = 140;
    private static final int ZOOM_BTN_100_WIDTH = 65;
    private static final int ZOOM_BTN_FIT_WIDTH = 55;
    private static final int ZOOM_CTRL_HEIGHT = 24;

    private static final double NUDGE_SMALL = 0.002;
    private static final double NUDGE_LARGE = 0.01;
    private static final double CLAMP_MIN = 0.0;
    private static final double CLAMP_MAX = 1.0;

    private static final int PLACEHOLDER_WIDTH = 1080;
    private static final int PLACEHOLDER_HEIGHT = 2400;
    private static final int PLACEHOLDER_GRID_STEP = 100;
    private static final Color PLACEHOLDER_BG_COLOR = new Color(30, 30, 30);
    private static final Color PLACEHOLDER_GRID_COLOR = new Color(60, 60, 60);
    private static final Paint EXCLUDE_OVERLAY_COLOR = new Color(255, 0, 0, 100);

    private static final int SEARCH_MAX_DEPTH = 4;
    private static final int INITIAL_CANDIDATES_CAPACITY = 16;
    private static final int INITIAL_LINES_CAPACITY = 10;
    private static final int INITIAL_SB_CAPACITY = 64;
    private static final double PERCENT_MULTIPLIER = 100.0;
    private static final int DEFAULT_MIN_COLOURS = 2;

    private static final String ARROW_LEFT = "◄";
    private static final String ARROW_UP = "▲";
    private static final String ARROW_DOWN = "▼";
    private static final String ARROW_RIGHT = "►";

    private static final String EXT_GRID = s(_DOT_, GRID);
    private static final String EXT_PNG = s(_DOT_, PNG);
    private static final String EXT_JPG = s(_DOT_, J, P, G);
    private static final String KEY_COLOURS_CUT = s(COLOURS, _DOT_, C, U, T);
    private static final String DEFAULT_BORDER_VALUE = s(_1_, _SLASH_, ZOOM_DEFAULT);

    private static final String FMT_COORD = s(_PRCNT_, _DOT_, _4_, F);
    private static final String FMT_SHARE = b(FMT_PERCENT_S, e(ROUND, s(_PRCNT_, _DOT_, _1_, F, _PRCNT_, _PRCNT_)));
    private static final String FMT_RGB_INT = "R:%d G:%d B:%d  Int:%d";

    private static final String INITIAL_COORD_TEXT = s(u(X), _COLON_, _SPACE_, _DASH_, _SPACE_, _SPACE_, u(Y), _COLON_, _SPACE_, _DASH_);
    private static final String INITIAL_COLOR_TEXT = "RGB: -  Int: -";
    private static final String STATUS_READY = "Ready";
    private static final String NO_GRID_FILE_LOADED = b(c(NO), c(GRID), c(FILE), c(LOADED));
    private static final String TITLE_GRID_EDITOR_PREFIX = s(b(c(GRID), c(EDITOR), s(_DASH_)), _SPACE_);
    private static final String FILTER_GRID_DESC = b(c(GRID), s(FILE, S), e(ROUND, s(_ASTRS_, EXT_GRID)));
    private static final String FILTER_PNG_DESC = b(u(PNG), s(c(IMAGE), S), e(ROUND, s(_ASTRS_, EXT_PNG)));
    private static final String ELLIPSIS_DOTS = s(_DOT_, _DOT_, _DOT_);
    private static final String TITLE_ERROR = c(works.lysenko.util.lang.word.E.ERROR);

    private static final Pattern RESOLUTION_PATTERN = Pattern.compile(s(CRCMFLX, REW_SOL, D, _PLUS_, X, REW_SOL, D, _PLUS_, _DOLLAR));

    private static final String[] SEARCH_DIRS = {
            _RUNS_,
            s(TARGET, File.separator, RUNS),
            RUNS,
            s(SRC, File.separator, MAIN, File.separator, RESOURCES)
    };

    private final ImagePanel imagePanel;
    private final JScrollPane imageScrollPane;
    private final JSlider slider = new JSlider(SwingConstants.HORIZONTAL, ZOOM_MIN, ZOOM_MAX, ZOOM_DEFAULT);
    private final GeometryFields geometryFields;
    private final ColourFields colourFields;
    private final MetricsFields metricsFields;
    private final InspectorControls inspector = new InspectorControls();
    private final EditorState state = new EditorState();

    private Helper() {

        setTitle(b(c(GRID), c(PARAMETERS), c(HELPER)));
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        imagePanel = new ImagePanel();
        imagePanel.setScale(DEFAULT_IMAGE_SCALE);
        imagePanel.setFocusable(true);
        imageScrollPane = new JScrollPane(imagePanel);
        imageScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        imageScrollPane.getHorizontalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        imageScrollPane.setMinimumSize(new Dimension(IMAGE_MIN_WIDTH, PANE_MIN_HEIGHT));

        final JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.PAGE_AXIS));
        controlPanel.setBorder(new EmptyBorder(PAD_LARGE, PAD_LARGE, PAD_LARGE, PAD_LARGE));

        controlPanel.add(SidebarBuilder.buildFileCard(state.fileLabel(),
                this::saveCurrentGrid, this::autoLocateScreenshot, this::recalculate));
        controlPanel.add(Box.createVerticalStrut(STRUT_MEDIUM));

        geometryFields = buildGeometryCard(controlPanel);
        controlPanel.add(Box.createVerticalStrut(STRUT_MEDIUM));

        metricsFields = SidebarBuilder.buildMetricsCard(controlPanel);
        controlPanel.add(Box.createVerticalStrut(STRUT_MEDIUM));

        colourFields = SidebarBuilder.buildColoursCard(controlPanel, this::recalculate);
        controlPanel.add(Box.createVerticalStrut(STRUT_MEDIUM));

        controlPanel.add(SidebarBuilder.buildInspectCard(inspector,
                this::addInspectedColor, this::sampleGridColours, this::applyDetectedColours));

        final JScrollPane sideScrollPane = new JScrollPane(controlPanel);
        sideScrollPane.setPreferredSize(new Dimension(SIDEBAR_WIDTH, DEFAULT_HEIGHT));
        sideScrollPane.setMinimumSize(new Dimension(SIDEBAR_MIN_WIDTH, PANE_MIN_HEIGHT));
        sideScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sideScrollPane, imageScrollPane);
        splitPane.setDividerLocation(SPLIT_DIVIDER_LOCATION);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        add(splitPane, BorderLayout.CENTER);

        add(SidebarBuilder.buildBottomBar(state.statusLabel(), slider, this::zoomChanged, this::fitImageToView),
                BorderLayout.PAGE_END);

        setupMenu();
        setupImageInteractivity();
        setupKeyBindings();
    }

    private Helper(final File file) {

        this();
        if (isNotNull(file) && file.exists()) {
            final String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(EXT_GRID)) {
                loadGridFile(file);
                autoLocateScreenshot();
            } else if (name.endsWith(EXT_PNG) || name.endsWith(EXT_JPG)) {
                loadImageFile(file);
            }
        }
    }

    /**
     * Entry point for running Grid Editor from IDE or CLI.
     *
     * @param args command-line arguments
     */
    static void main(final String[] args) {

        ControlPanel.initializeLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            final File file = (0 < args.length) ? new File(args[0]) : null;
            final Helper frame = new Helper(file);
            frame.setVisible(true);
        });
    }

    private GeometryFields buildGeometryCard(final Container parent) {

        final JPanel geomCard = new JPanel();
        geomCard.setLayout(new BoxLayout(geomCard, BoxLayout.PAGE_AXIS));
        geomCard.setBorder(BorderFactory.createTitledBorder("Geometry"));

        final JTextField resField = addField(geomCard, s(c(RESOLUTION), _COLON_), resolution);
        final JTextField scaleField = addField(geomCard, s(c(SCALE), _COLON_), scale);
        final JTextField horizField = addField(geomCard, s(c(HORIZONTAL), _COLON_), horizontal);
        final JTextField vertField = addField(geomCard, s(c(VERTICAL), _COLON_), vertical);

        geomCard.add(buildNudgeRow());
        parent.add(geomCard);
        return new GeometryFields(resField, scaleField, horizField, vertField);
    }

    private JPanel buildNudgeRow() {

        final JPanel nudgeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP_SMALL, GAP_TINY));
        final Dimension arrowDim = new Dimension(ARROW_BTN_WIDTH, ARROW_BTN_HEIGHT);

        nudgeRow.add(SidebarBuilder.createNudgeButton(ARROW_LEFT, arrowDim, () -> nudgeHorizontal(-NUDGE_SMALL)));
        nudgeRow.add(SidebarBuilder.createNudgeButton(ARROW_UP, arrowDim, () -> nudgeVertical(-NUDGE_SMALL)));
        nudgeRow.add(SidebarBuilder.createNudgeButton(ARROW_DOWN, arrowDim, () -> nudgeVertical(NUDGE_SMALL)));
        nudgeRow.add(SidebarBuilder.createNudgeButton(ARROW_RIGHT, arrowDim, () -> nudgeHorizontal(NUDGE_SMALL)));
        return nudgeRow;
    }

    private void setupMenu() {

        final JMenuBar menuBar = new JMenuBar();
        final int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        final JMenu fileMenu = new JMenu(c(FILE));
        final JMenuItem openGridItem = new JMenuItem(b(c(OPEN), s(c(GRID), ELLIPSIS_DOTS)));
        openGridItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask));
        openGridItem.addActionListener(_ -> chooseAndOpenGrid());

        final JMenuItem openImgItem = new JMenuItem(b(c(OPEN), s(c(SCREENSHOT), ELLIPSIS_DOTS)));
        openImgItem.addActionListener(_ -> chooseAndOpenImage());

        final JMenuItem saveItem = new JMenuItem(b(c(SAVE), c(GRID)));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask));
        saveItem.addActionListener(_ -> saveCurrentGrid());

        final JMenuItem saveAsItem = new JMenuItem(b(c(SAVE), c(GRID), s(c(AS), ELLIPSIS_DOTS)));
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask | InputEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(_ -> saveGridAs());

        final JMenuItem exitItem = new JMenuItem(c(EXIT));
        exitItem.addActionListener(_ -> dispose());

        fileMenu.add(openGridItem);
        fileMenu.add(openImgItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        final JMenu editMenu = new JMenu(c(EDIT));
        final JMenuItem recalcItem = new JMenuItem(c(RECALCULATE));
        recalcItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutMask));
        recalcItem.addActionListener(_ -> recalculate());

        final JMenuItem sampleItem = new JMenuItem("Sample Grid Colours");
        sampleItem.addActionListener(_ -> sampleGridColours());

        final JMenuItem copyColoursItem = new JMenuItem("Copy Colours to Clipboard");
        copyColoursItem.addActionListener(_ -> copyColoursToClipboard());

        editMenu.add(recalcItem);
        editMenu.add(sampleItem);
        editMenu.add(copyColoursItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        setJMenuBar(menuBar);
    }

    private void copyColoursToClipboard() {

        final String sel = colourFields.coloursText();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sel), null);
        state.setStatus("Colours copied to clipboard.");
    }

    private void setupImageInteractivity() {

        imagePanel.addMouseMotionListener(new ImageMouseMotionListener());
        imagePanel.addMouseListener(new ImageMouseListener());
        imageScrollPane.addMouseWheelListener(this::handleMouseWheelZoom);
    }

    private void handleMouseWheelZoom(final MouseWheelEvent e) {

        if (e.isMetaDown() || e.isControlDown()) {
            final int notches = e.getWheelRotation();
            final int current = slider.getValue();
            final long target = current - (long) notches * ZOOM_WHEEL_STEP;
            slider.setValue(Math.clamp(target, ZOOM_MIN, ZOOM_MAX));
            e.consume();
        }
    }

    private void setupKeyBindings() {

        final JComponent root = getRootPane();
        final InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap am = root.getActionMap();

        registerNudge(im, am, NudgeCommand.LEFT);
        registerNudge(im, am, NudgeCommand.RIGHT);
        registerNudge(im, am, NudgeCommand.UP);
        registerNudge(im, am, NudgeCommand.DOWN);
        registerNudge(im, am, NudgeCommand.LEFT_BIG);
        registerNudge(im, am, NudgeCommand.RIGHT_BIG);
        registerNudge(im, am, NudgeCommand.UP_BIG);
        registerNudge(im, am, NudgeCommand.DOWN_BIG);
    }

    private void registerNudge(final InputMap im, final ActionMap am, final NudgeCommand command) {

        im.put(command.keyStroke(), command);
        am.put(command, new NudgeAction(command));
    }

    private void nudgeHorizontal(final double delta) {

        try {
            final double val = fr(geometryFields.horizontalText()).doubleValue() + delta;
            geometryFields.setHorizontalFormatted(Math.clamp(val, CLAMP_MIN, CLAMP_MAX));
            recalculate();
        } catch (final IllegalArgumentException | ArithmeticException _) {
            // Ignore malformed numeric input while typing
        }
    }

    private void nudgeVertical(final double delta) {

        try {
            final double val = fr(geometryFields.verticalText()).doubleValue() + delta;
            geometryFields.setVerticalFormatted(Math.clamp(val, CLAMP_MIN, CLAMP_MAX));
            recalculate();
        } catch (final IllegalArgumentException | ArithmeticException _) {
            // Ignore malformed numeric input while typing
        }
    }

    private void centerGridAtImagePoint(final Point imgPoint, final BufferedImage source) {

        final double horiz = (double) imgPoint.x / source.getWidth(null);
        final double vert = (double) imgPoint.y / source.getHeight(null);
        geometryFields.setCoordinates(horiz, vert);
        recalculate();
    }

    private void updatePixelInspection(final Point panelPoint) {

        final BufferedImage source = state.sourceImg();
        final Point imgP = imagePanel.toImageCoordinates(panelPoint.x, panelPoint.y);
        if (isNotNull(imgP) && isNotNull(source)) {
            final int rgb = source.getRGB(imgP.x, imgP.y);
            inspector.updateHover(imgP, rgb);
        }
    }

    private void addInspectedColor() {

        final Integer selected = inspector.selectedColor();
        if (isNotNull(selected)) {
            colourFields.appendColorIfAbsent(selected);
            state.setStatus(s("Added color ", selected, " to field."));
        }
    }

    private void loadGridFile(final File file) {

        if (isNotNull(file) && file.exists()) {
            state.setCurrentGridFile(file);
            state.setFileName(file.getName());
            setTitle(s(TITLE_GRID_EDITOR_PREFIX, file.getName()));

            try {
                final Properties rawProps = FileSupport.readProperties(file);
                geometryFields.loadFrom(rawProps);
                colourFields.loadFrom(rawProps);

                state.setStatus(s(b(c(LOADED), GRID), _COLON_, _SPACE_, file.getName()));
                if (isNotNull(state.sourceImg())) {
                    recalculate();
                } else {
                    updateEmptyImagePlaceholder();
                }
            } catch (final IOException | IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, sn(s(b(c(FAILED), TO, LOAD, GRID), _COLON_, _SPACE_), e.getMessage()),
                        TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadImageFile(final File file) {

        if (isNotNull(file) && file.exists()) {
            try {
                final BufferedImage loaded = ImageIO.read(file);
                state.setSourceImg(loaded);
                state.setStatus(s(b(c(LOADED), SCREENSHOT), _COLON_, _SPACE_, file.getName(),
                        _SPACE_, e(ROUND, s(loaded.getWidth(), X, loaded.getHeight()))));
                recalculate();
                fitImageToView();
            } catch (final IOException e) {
                JOptionPane.showMessageDialog(this, sn(s(b(c(FAILED), TO, LOAD, SCREENSHOT), _COLON_, _SPACE_), e.getMessage()),
                        TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void autoLocateScreenshot() {

        final File found = FileSupport.findRecentScreenshot(state.currentGridFile());
        if (isNotNull(found)) {
            loadImageFile(found);
        } else {
            state.setStatus("No recent screenshot found automatically. Please open one manually.");
        }
    }

    private void chooseAndOpenGrid() {

        final JFileChooser chooser = FileSupport.createFileChooser(_GRIDS_, FILTER_GRID_DESC, GRID);
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            loadGridFile(chooser.getSelectedFile());
            if (isNull(state.sourceImg())) autoLocateScreenshot();
        }
    }

    private void chooseAndOpenImage() {

        final JFileChooser chooser = FileSupport.createFileChooser(_RUNS_, FILTER_PNG_DESC, PNG);
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            loadImageFile(chooser.getSelectedFile());
        }
    }

    private void saveCurrentGrid() {

        final File current = state.currentGridFile();
        if (isNull(current)) {
            saveGridAs();
        } else {
            writeGridToFile(current);
        }
    }

    private void saveGridAs() {

        final JFileChooser chooser = FileSupport.createFileChooser(_GRIDS_, FILTER_GRID_DESC, GRID);
        final File current = state.currentGridFile();
        if (isNotNull(current)) chooser.setSelectedFile(current);
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(this)) {
            writeGridToFile(FileSupport.ensureGridExtension(chooser.getSelectedFile()));
        }
    }

    private void writeGridToFile(final File targetFile) {

        try {
            final Collection<String> lines = new ArrayList<>(INITIAL_LINES_CAPACITY);
            geometryFields.appendToLines(lines);
            colourFields.appendToLines(lines);

            Files.write(targetFile.toPath(), lines, StandardCharsets.UTF_8);
            state.setCurrentGridFile(targetFile);
            state.setFileName(targetFile.getName());
            setTitle(s(TITLE_GRID_EDITOR_PREFIX, targetFile.getName()));
            state.setStatus(s(b(c(SAVED), GRID), _COLON_, _SPACE_, targetFile.getName()));
            JOptionPane.showMessageDialog(this, b(c(SAVED), SUCCESSFULLY, TO, targetFile.getName()),
                    c(SAVED), JOptionPane.INFORMATION_MESSAGE);
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(this, sn(s(b(c(FAILED), TO, SAVE), _COLON_, _SPACE_), e.getMessage()),
                    b(c(SAVE), TITLE_ERROR), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sampleGridColours() {

        if (!state.hasSamplingContext()) {
            recalculate();
        }
        final BufferedImage source = state.sourceImg();
        final Geometry geometry = state.lastGeometry();
        if (isNull(source) || isNull(geometry)) {
            JOptionPane.showMessageDialog(this, "Please open a screenshot first to sample grid colours.",
                    c(INFO), JOptionPane.INFORMATION_MESSAGE);
        } else {
            executeGridSampling(source, geometry);
        }
    }

    private void executeGridSampling(final BufferedImage source, final Geometry geometry) {

        try {
            final _RelativePosition pos = rp(fr(geometryFields.horizontalText()), fr(geometryFields.verticalText()));
            final Request req = new Request("sample", source, pos, fr(geometryFields.scaleText()),
                    state.lastResolution(), null, null);
            final _Grid grid = new Grid(source, geometry, req);

            final Map<Integer, Pair<Integer, String>> countMap = grid.calculator().countPixelsByColor();
            final int totalSamples = grid.getSamplePoints().size();
            inspector.populateDetectedColors(countMap, totalSamples);

            state.setStatus(String.format(Locale.US, "Sampled %d grid points across %d distinct colours.",
                    totalSamples, countMap.size()));
        } catch (final IllegalArgumentException | IllegalStateException | ArithmeticException e) {
            JOptionPane.showMessageDialog(this, sn("Sampling failed: ", e.getMessage()),
                    "Sampling Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyDetectedColours() {

        if (0 == inspector.detectedRowCount()) {
            sampleGridColours();
        }
        final int rows = inspector.detectedRowCount();
        if (0 < rows) {
            colourFields.setColoursText(inspector.formatDetectedColours(metricsFields.samplesCount()));
            colourFields.applyDefaultsIfEmpty(rows);
            state.setStatus(b(c(APPLIED), s(rows), DETECTED, COLOURS, TO, s(CONFIGURATION, _DOT_)));
        }
    }

    private void updateEmptyImagePlaceholder() {

        try {
            final Resolution to = readResolution(geometryFields.resolutionText());
            final Dimension dims = ImageSupport.parsePlaceholderDimensions(geometryFields.resolutionText());
            final BufferedImage canvas = ImageSupport.createPlaceholderCanvas(dims.width, dims.height);

            final Geometry at = getRequestedGridLocation(
                    dims.width, dims.height,
                    fr(geometryFields.horizontalText()).doubleValue(),
                    fr(geometryFields.verticalText()).doubleValue(),
                    fr(geometryFields.scaleText()).doubleValue(),
                    to.ratio());
            state.setLastCalculation(at, to);
            metricsFields.updateGeometry(at);

            final DrawGridResult result = drawGrid(canvas, at, to, null);
            metricsFields.updateDrawResult(result);

            imagePanel.setImage(result.image());
            imageScrollPane.repaint();
            fitImageToView();
        } catch (final IllegalArgumentException | IllegalStateException | ArithmeticException _) {
            // Ignore malformed inputs while editing
        }
    }

    private void recalculate() {

        final BufferedImage source = state.sourceImg();
        if (isNotNull(source)) {
            ColourSearchRequest unknownRequest = null;
            RuntimeException failure = null;
            try {
                final Resolution to = readResolution(geometryFields.resolutionText());
                final Geometry at = getRequestedGridLocation(
                        source.getWidth(), source.getHeight(),
                        fr(geometryFields.horizontalText()).doubleValue(),
                        fr(geometryFields.verticalText()).doubleValue(),
                        fr(geometryFields.scaleText()).doubleValue(),
                        to.ratio());
                state.setLastCalculation(at, to);

                final ColourSearchRequest request = csr(colourFields.maskText());
                if (isNotNull(request) && !request.getUnknownColours().isEmpty()) {
                    unknownRequest = request;
                } else {
                    updateValuesAndRepaint(request, ImageSupport.copySource(source), at, to);
                }
            } catch (final IllegalArgumentException | IllegalStateException | ArithmeticException e) {
                failure = e;
            }
            reportRecalculateOutcome(unknownRequest, failure);
        }
    }

    private void reportRecalculateOutcome(final _ColourSpreadRequest unknownRequest, final RuntimeException failure) {

        if (isNotNull(unknownRequest)) {
            JOptionPane.showMessageDialog(this, b(s1(unknownRequest.getUnknownColours().size(), b(UNKNOWN, COLOUR)),
                            java.util.Arrays.toString(unknownRequest.getUnknownColours().toArray())), c(PROBLEM),
                    JOptionPane.ERROR_MESSAGE);
        } else if (isNotNull(failure)) {
            JOptionPane.showMessageDialog(this,
                    b(c(AN), EXCEPTION, s(OCCURED, _COLON_), failure.getMessage(), Arrays.toLines(failure.getStackTrace())),
                    c(EXCEPTION),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateValuesAndRepaint(final ColourSearchRequest request, final BufferedImage image,
                                        final _Geometry geometry, final _Resolution resolutionValue) {

        metricsFields.updateGeometry(geometry);
        final ColoursSearchResult result = search(request, image);
        final Collection<_Region> exclude = isNotNull(result) ? result.regions() : null;
        final BufferedImage rendered = ImageSupport.applyExclusions(image, exclude);
        final DrawGridResult drawResult = drawGrid(rendered, geometry, resolutionValue, exclude);
        metricsFields.updateDrawResult(drawResult);
        imagePanel.setImage(drawResult.image());
        imageScrollPane.repaint();
    }

    private void zoomChanged(final ChangeEvent e) {

        final int val = slider.getValue();
        final float sc = val / ZOOM_SCALE_DIVISOR;
        imagePanel.setScale(sc);
        imageScrollPane.revalidate();
        imageScrollPane.repaint();
    }

    private void fitImageToView() {

        final BufferedImage source = state.sourceImg();
        if (isNotNull(source)) {
            final Dimension viewSize = imageScrollPane.getViewport().getSize();
            if (0 < viewSize.width && 0 < viewSize.height) {
                slider.setValue(ImageSupport.computeFitZoomPercent(viewSize, source));
            }
        }
    }

    private JTextField addField(final Container panel, final String labelText, final String initialValue) {

        return SidebarBuilder.addEditableField(panel, labelText, initialValue, this::recalculate);
    }

    private enum SidebarBuilder {
        ;

        private static JPanel buildFileCard(final JLabel fileLabel, final Runnable onSave,
                                            final Runnable onAutoImg, final Runnable onRecalc) {

            final JPanel fileCard = new JPanel(new BorderLayout(GAP_MEDIUM, GAP_MEDIUM));
            fileCard.setBorder(BorderFactory.createTitledBorder(b(c(GRID), c(FILE))));
            fileLabel.setFont(fileLabel.getFont().deriveFont(Font.BOLD, FILE_LABEL_FONT_SIZE));
            fileCard.add(fileLabel, BorderLayout.PAGE_START);

            final JPanel fileBtnRow = new JPanel(new GridLayout(1, 3, GAP_SMALL, GAP_SMALL));
            final JButton saveBtn = new JButton(c(SAVE));
            saveBtn.setToolTipText("Save changes to grid file (Cmd+S)");
            saveBtn.addActionListener(_ -> onSave.run());

            final JButton autoImgBtn = new JButton("Find Img");
            autoImgBtn.setToolTipText("Auto-locate recent screenshot for this grid");
            autoImgBtn.addActionListener(_ -> onAutoImg.run());

            final JButton recalcBtn = new JButton(c(RECALCULATE));
            recalcBtn.addActionListener(_ -> onRecalc.run());

            fileBtnRow.add(saveBtn);
            fileBtnRow.add(autoImgBtn);
            fileBtnRow.add(recalcBtn);
            fileCard.add(fileBtnRow, BorderLayout.PAGE_END);
            return fileCard;
        }

        private static ColourFields buildColoursCard(final Container parent, final Runnable onEnter) {

            final JPanel coloursCard = new JPanel();
            coloursCard.setLayout(new BoxLayout(coloursCard, BoxLayout.PAGE_AXIS));
            coloursCard.setBorder(BorderFactory.createTitledBorder(b(c(COLOURS), s(_AMPRS_), c(RULES))));

            final JLabel clrLbl = new JLabel(b(c(COLOURS), s(e(ROUND, c(QUOTAS)), _COLON_)));
            clrLbl.setAlignmentX(LEFT_ALIGNMENT);
            coloursCard.add(clrLbl);

            final JTextArea coloursArea = new JTextArea(COLOURS_AREA_ROWS, COLOURS_AREA_COLS);
            coloursArea.setLineWrap(true);
            coloursArea.setWrapStyleWord(false);
            final JScrollPane clrScroll = new JScrollPane(coloursArea);
            clrScroll.setAlignmentX(LEFT_ALIGNMENT);
            clrScroll.setPreferredSize(new Dimension(SIDEBAR_WIDTH - SIDEBAR_INNER_INSET, COLOURS_SCROLL_HEIGHT));
            coloursCard.add(clrScroll);

            final JTextField amountField = addEditableField(coloursCard, b(c(COLOURS), s(c(AMOUNT), _COLON_)), EMPTY, onEnter);
            final JTextField ignoreField = addEditableField(coloursCard, b(c(COLOURS), s(c(IGNORE), _COLON_)), EMPTY, onEnter);
            final JTextField borderField = addEditableField(coloursCard, b(c(COLOURS), s(c(BORDER), _SLASH_, c(CUTOFF), _COLON_)), EMPTY, onEnter);
            final JTextField maskField = addEditableField(coloursCard, s(c(MASK), _COLON_), EMPTY, onEnter);

            parent.add(coloursCard);
            return new ColourFields(coloursArea, amountField, ignoreField, borderField, maskField);
        }

        private static JPanel buildInspectCard(final InspectorControls inspector, final Runnable onAddColor,
                                               final Runnable onSampleGrid, final Runnable onApplyDetected) {

            final JPanel inspectCard = new JPanel();
            inspectCard.setLayout(new BoxLayout(inspectCard, BoxLayout.PAGE_AXIS));
            inspectCard.setBorder(BorderFactory.createTitledBorder("Pixel Inspector & Dominant Colours"));

            final JPanel hoverRow = new JPanel(new BorderLayout(GAP_MEDIUM, GAP_MEDIUM));
            final JPanel hoverSwatch = inspector.hoverSwatch();
            hoverSwatch.setPreferredSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));
            hoverSwatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            hoverSwatch.setBackground(Color.WHITE);
            hoverRow.add(hoverSwatch, BorderLayout.LINE_START);

            final JPanel hoverTextPanel = new JPanel(new GridLayout(2, 1));
            hoverTextPanel.add(inspector.hoverCoordLabel());
            hoverTextPanel.add(inspector.hoverColorLabel());
            hoverRow.add(hoverTextPanel, BorderLayout.CENTER);
            inspectCard.add(hoverRow);
            inspectCard.add(Box.createVerticalStrut(STRUT_SMALL));

            final JPanel btnInspectRow = new JPanel(new GridLayout(1, 2, GAP_SMALL, GAP_SMALL));
            final JButton addColorBtn = new JButton(b(s(_PLUS_), c(ADD), c(PIXEL)));
            addColorBtn.setToolTipText("Add inspected pixel color to Colours list");
            addColorBtn.addActionListener(_ -> onAddColor.run());

            final JButton sampleGridBtn = new JButton("Sample Grid");
            sampleGridBtn.setToolTipText("Analyze all grid sample points on screenshot");
            sampleGridBtn.addActionListener(_ -> onSampleGrid.run());

            btnInspectRow.add(addColorBtn);
            btnInspectRow.add(sampleGridBtn);
            inspectCard.add(btnInspectRow);
            inspectCard.add(Box.createVerticalStrut(STRUT_SMALL));

            final JTable detectedColorsTable = new JTable(inspector.detectedColorsModel());
            detectedColorsTable.getColumnModel().getColumn(0).setMaxWidth(SWATCH_COL_MAX_WIDTH);
            detectedColorsTable.getColumnModel().getColumn(0).setCellRenderer(new ColorSwatchCellRenderer());
            final JScrollPane tableScroll = new JScrollPane(detectedColorsTable);
            tableScroll.setPreferredSize(new Dimension(SIDEBAR_WIDTH - SIDEBAR_INNER_INSET, TABLE_SCROLL_HEIGHT));
            tableScroll.setAlignmentX(LEFT_ALIGNMENT);
            inspectCard.add(tableScroll);

            final JButton fillColoursBtn = new JButton("Apply Detected Colours to Field");
            fillColoursBtn.setAlignmentX(LEFT_ALIGNMENT);
            fillColoursBtn.addActionListener(_ -> onApplyDetected.run());
            inspectCard.add(Box.createVerticalStrut(STRUT_SMALL));
            inspectCard.add(fillColoursBtn);
            return inspectCard;
        }

        private static JPanel buildBottomBar(final JLabel statusLabel, final JSlider slider,
                                             final ChangeListener onZoomChanged, final Runnable onFit) {

            final JPanel bottomBar = new JPanel(new BorderLayout(GAP_MEDIUM, GAP_MEDIUM));
            bottomBar.setBorder(new EmptyBorder(PAD_SMALL, PAD_LARGE, PAD_SMALL, PAD_LARGE));
            bottomBar.add(statusLabel, BorderLayout.LINE_START);

            final JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, GAP_ZOOM, 0));
            zoomPanel.add(new JLabel(s(c(ZOOM), _COLON_)));
            slider.setPreferredSize(new Dimension(ZOOM_SLIDER_WIDTH, ZOOM_CTRL_HEIGHT));
            slider.addChangeListener(onZoomChanged);
            zoomPanel.add(slider);

            final JButton zoom100 = new JButton(PCT_100);
            zoom100.setPreferredSize(new Dimension(ZOOM_BTN_100_WIDTH, ZOOM_CTRL_HEIGHT));
            zoom100.addActionListener(_ -> slider.setValue(ZOOM_DEFAULT));
            zoomPanel.add(zoom100);

            final JButton zoomFit = new JButton(c(FIT));
            zoomFit.setPreferredSize(new Dimension(ZOOM_BTN_FIT_WIDTH, ZOOM_CTRL_HEIGHT));
            zoomFit.addActionListener(_ -> onFit.run());
            zoomPanel.add(zoomFit);

            bottomBar.add(zoomPanel, BorderLayout.LINE_END);
            return bottomBar;
        }

        private static JButton createNudgeButton(final String label, final Dimension size, final Runnable action) {

            final JButton button = new JButton(label);
            button.setPreferredSize(size);
            button.addActionListener(_ -> action.run());
            return button;
        }

        private static MetricsFields buildMetricsCard(final Container parent) {

            final JPanel metricsCard = new JPanel(new GridLayout(3, 2, GAP_SMALL, GAP_TINY));
            metricsCard.setBorder(BorderFactory.createTitledBorder("Calculated Metrics"));
            final JTextField cx = addReadOnlyField(metricsCard, b(c(CENTER), s(u(X), _COLON_)));
            final JTextField cy = addReadOnlyField(metricsCard, b(c(CENTER), s(u(Y), _COLON_)));
            final JTextField rad = addReadOnlyField(metricsCard, s(c(RADIUS), _COLON_));
            final JTextField smp = addReadOnlyField(metricsCard, s(c(SAMPLES), _COLON_));
            final JTextField ar = addReadOnlyField(metricsCard, s(c(AREA), _COLON_));
            final JTextField den = addReadOnlyField(metricsCard, s(c(DENSITY), _COLON_));
            parent.add(metricsCard);
            return new MetricsFields(cx, cy, rad, smp, ar, den);
        }

        private static JTextField addEditableField(final Container panel, final String labelText,
                                                   final String initialValue, final Runnable onEnter) {

            final JPanel row = new JPanel(new BorderLayout(GAP_MEDIUM, GAP_TINY));
            final JLabel label = new JLabel(labelText);
            label.setPreferredSize(new Dimension(FIELD_LABEL_WIDTH, FIELD_LABEL_HEIGHT));
            final JTextField field = new JTextField(initialValue);
            field.addKeyListener(new EnterKeyListener(onEnter));
            row.add(label, BorderLayout.LINE_START);
            row.add(field, BorderLayout.CENTER);
            panel.add(row);
            panel.add(Box.createVerticalStrut(GAP_TINY));
            return field;
        }

        private static JTextField addReadOnlyField(final Container panel, final String labelText) {

            final JPanel row = new JPanel(new BorderLayout(GAP_MEDIUM, GAP_TINY));
            final JLabel label = new JLabel(labelText);
            label.setPreferredSize(new Dimension(METRIC_LABEL_WIDTH, METRIC_LABEL_HEIGHT));
            final JTextField field = new JTextField();
            field.setEditable(false);
            row.add(label, BorderLayout.LINE_START);
            row.add(field, BorderLayout.CENTER);
            panel.add(row);
            return field;
        }
    }

    private static final class EnterKeyListener extends KeyAdapter {

        private final Runnable onEnter;

        private EnterKeyListener(final Runnable onEnter) {

            this.onEnter = onEnter;
        }

        @Override
        public void keyPressed(final KeyEvent e) {

            if (KeyEvent.VK_ENTER == e.getKeyCode()) onEnter.run();
        }
    }

    private enum FileSupport {
        ;

        private static Properties readProperties(final File file) throws IOException {

            final Properties rawProps = new Properties();
            try (final InputStream in = Files.newInputStream(file.toPath())) {
                rawProps.load(in);
            }
            return rawProps;
        }

        private static JFileChooser createFileChooser(final String defaultDir, final String description, final String extension) {

            final JFileChooser chooser = new JFileChooser();
            final File preferredDir = new File(defaultDir);
            chooser.setCurrentDirectory(preferredDir.exists() ? preferredDir : new File(s(_DOT_)));
            chooser.setFileFilter(new FileNameExtensionFilter(description, extension));
            return chooser;
        }

        private static File ensureGridExtension(final File selected) {

            return selected.getName().toLowerCase(Locale.ROOT).endsWith(EXT_GRID)
                    ? selected
                    : new File(s(selected.getAbsolutePath(), EXT_GRID));
        }

        private static File findRecentScreenshot(final File gridFile) {

            final List<File> candidates = collectCandidateScreenshots();
            final File result;
            if (candidates.isEmpty()) {
                result = null;
            } else {
                final File matched = findMatchingCandidate(candidates, gridFile);
                if (isNotNull(matched)) {
                    result = matched;
                } else {
                    candidates.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    result = candidates.getFirst();
                }
            }
            return result;
        }

        private static List<File> collectCandidateScreenshots() {

            final List<File> candidates = new ArrayList<>(INITIAL_CANDIDATES_CAPACITY);
            for (final String dirPath : SEARCH_DIRS) {
                collectFromDirectory(dirPath, candidates);
            }
            return candidates;
        }

        private static void collectFromDirectory(final String dirPath, final Collection<? super File> candidates) {

            final File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                collectPngFiles(dir, candidates, SEARCH_MAX_DEPTH);
            }
        }

        private static File findMatchingCandidate(final Iterable<? extends File> candidates, final File gridFile) {

            File match = null;
            if (isNotNull(gridFile)) {
                final String baseName = gridFile.getName().replace(EXT_GRID, EMPTY).toLowerCase(Locale.ROOT);
                for (final File candidate : candidates) {
                    if (isNull(match) && candidate.getName().toLowerCase(Locale.ROOT).contains(baseName)) {
                        match = candidate;
                    }
                }
            }
            return match;
        }

        private static void collectPngFiles(final File dir, final Collection<? super File> list, final int depth) {

            if (0 <= depth) {
                final File[] files = dir.listFiles();
                if (isNotNull(files)) {
                    for (final File entry : files) {
                        if (entry.isDirectory()) {
                            collectPngFiles(entry, list, depth - 1);
                        } else if (entry.getName().toLowerCase(Locale.ROOT).endsWith(EXT_PNG)) {
                            list.add(entry);
                        }
                    }
                }
            }
        }
    }

    private enum ImageSupport {
        ;

        private static Dimension parsePlaceholderDimensions(final String rawResolution) {

            int width = PLACEHOLDER_WIDTH;
            int height = PLACEHOLDER_HEIGHT;
            final String resText = rawResolution.strip();
            if (RESOLUTION_PATTERN.matcher(resText).matches()) {
                final String[] parts = resText.split(s(X));
                width = Integer.parseInt(parts[0]);
                height = Integer.parseInt(parts[1]);
            }
            return new Dimension(width, height);
        }

        private static BufferedImage createPlaceholderCanvas(final int width, final int height) {

            final BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final Graphics2D graphics = canvas.createGraphics();
            try {
                graphics.setColor(PLACEHOLDER_BG_COLOR);
                graphics.fillRect(0, 0, width, height);
                graphics.setColor(PLACEHOLDER_GRID_COLOR);
                drawVerticalPlaceholderLines(graphics, width, height);
                drawHorizontalPlaceholderLines(graphics, width, height);
            } finally {
                graphics.dispose();
            }
            return canvas;
        }

        private static void drawVerticalPlaceholderLines(final Graphics2D graphics, final int width, final int height) {

            for (int x = 0; x < width; x += PLACEHOLDER_GRID_STEP) {
                graphics.drawLine(x, 0, x, height);
            }
        }

        private static void drawHorizontalPlaceholderLines(final Graphics2D graphics, final int width, final int height) {

            for (int y = 0; y < height; y += PLACEHOLDER_GRID_STEP) {
                graphics.drawLine(0, y, width, y);
            }
        }

        private static BufferedImage copySource(final BufferedImage source) {

            final BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
            final Graphics2D g = copy.createGraphics();
            try {
                g.drawImage(source, 0, 0, null);
            } finally {
                g.dispose();
            }
            return copy;
        }

        private static BufferedImage applyExclusions(final BufferedImage image, final Iterable<? extends _Region> exclude) {

            BufferedImage rendered = image;
            if (isNotNull(exclude)) {
                for (final _Region region : exclude) {
                    rendered = drawArea(rendered, EXCLUDE_OVERLAY_COLOR, region);
                }
            }
            return rendered;
        }

        private static int computeFitZoomPercent(final Dimension viewSize, final RenderedImage source) {

            final float scaleX = (float) viewSize.width / source.getWidth();
            final float scaleY = (float) viewSize.height / source.getHeight();
            final float fit = Math.min(scaleX, scaleY) * ZOOM_FIT_MARGIN;
            return truncateToInt(fit * ZOOM_SCALE_DIVISOR);
        }

        private static int truncateToInt(final float value) {

            return Math.toIntExact(Math.round(Math.floor(value)));
        }
    }

    private enum NudgeCommand {
        LEFT(true, -NUDGE_SMALL, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)),
        RIGHT(true, NUDGE_SMALL, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)),
        UP(false, -NUDGE_SMALL, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
        DOWN(false, NUDGE_SMALL, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
        LEFT_BIG(true, -NUDGE_LARGE, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK)),
        RIGHT_BIG(true, NUDGE_LARGE, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK)),
        UP_BIG(false, -NUDGE_LARGE, KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK)),
        DOWN_BIG(false, NUDGE_LARGE, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK));

        private final boolean horizontalAxis;
        private final double delta;
        private final KeyStroke keyStroke;

        NudgeCommand(final boolean horizontalAxis, final double delta, final KeyStroke keyStroke) {

            this.horizontalAxis = horizontalAxis;
            this.delta = delta;
            this.keyStroke = keyStroke;
        }

        private boolean isHorizontalAxis() {

            return horizontalAxis;
        }

        private double delta() {

            return delta;
        }

        private KeyStroke keyStroke() {

            return keyStroke;
        }
    }

    private final class NudgeAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = 1L;
        private final NudgeCommand command;

        private NudgeAction(final NudgeCommand command) {

            this.command = command;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {

            if (command.isHorizontalAxis()) {
                nudgeHorizontal(command.delta());
            } else {
                nudgeVertical(command.delta());
            }
        }

        @Override
        public Object clone() throws CloneNotSupportedException {

            throw new CloneNotSupportedException(getClass().getName());
        }
    }

    private final class ImageMouseMotionListener extends MouseMotionAdapter {

        @Override
        public void mouseMoved(final MouseEvent e) {

            updatePixelInspection(e.getPoint());
        }

        @Override
        public void mouseDragged(final MouseEvent e) {

            final BufferedImage source = state.sourceImg();
            if (isNotNull(source)) {
                final Point imgPoint = imagePanel.toImageCoordinates(e.getX(), e.getY());
                if (isNotNull(imgPoint)) {
                    centerGridAtImagePoint(imgPoint, source);
                }
            }
        }
    }

    private final class ImageMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(final MouseEvent e) {

            final BufferedImage source = state.sourceImg();
            final Point imgPoint = imagePanel.toImageCoordinates(e.getX(), e.getY());
            if (isNotNull(imgPoint) && isNotNull(source)) {
                inspector.setSelectedColor(source.getRGB(imgPoint.x, imgPoint.y));
                updatePixelInspection(e.getPoint());
                if (e.isShiftDown() || 2 == e.getClickCount()) {
                    centerGridAtImagePoint(imgPoint, source);
                }
            }
        }
    }

    private static final class DetectedColorsTableModel extends DefaultTableModel {

        @Serial
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNS = {"Swatch", "Color ID", c(COUNT), c(SHARE)};

        private DetectedColorsTableModel() {

            super(COLUMNS, 0);
        }

        @Override
        public boolean isCellEditable(final int row, final int column) {

            return false;
        }
    }

    private static final class ColorSwatchCellRenderer extends DefaultTableCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected, final boolean hasFocus,
                                                       final int row, final int column) {

            final JPanel panel = new JPanel();
            if (value instanceof final Color color) panel.setBackground(color);
            panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            return panel;
        }
    }

    private record GeometryFields(JTextField resolutionField, JTextField scaleField,
                                  JTextField horizontalField, JTextField verticalField) {

        private String resolutionText() {

            return resolutionField.getText();
        }

        private String scaleText() {

            return scaleField.getText();
        }

        private String horizontalText() {

            return horizontalField.getText();
        }

        private String verticalText() {

            return verticalField.getText();
        }

        private void setHorizontalFormatted(final double value) {

            horizontalField.setText(String.format(Locale.US, FMT_COORD, value));
        }

        private void setVerticalFormatted(final double value) {

            verticalField.setText(String.format(Locale.US, FMT_COORD, value));
        }

        private void setCoordinates(final double horiz, final double vert) {

            setHorizontalFormatted(horiz);
            setVerticalFormatted(vert);
        }

        private void loadFrom(final Properties rawProps) {

            resolutionField.setText(rawProps.getProperty(RESOLUTION, resolution));
            scaleField.setText(rawProps.getProperty(SCALE, scale));
            horizontalField.setText(rawProps.getProperty(HORIZONTAL, horizontal));
            verticalField.setText(rawProps.getProperty(VERTICAL, vertical));
        }

        private void appendToLines(final Collection<? super String> lines) {

            lines.add(s(RESOLUTION, _EQUAL_, resolutionField.getText().strip()));
            lines.add(s(SCALE, _EQUAL_, scaleField.getText().strip()));
            addIfNotDefault(lines, HORIZONTAL, horizontalField.getText(), horizontal);
            addIfNotDefault(lines, VERTICAL, verticalField.getText(), vertical);
        }

        private static void addIfNotDefault(final Collection<? super String> lines, final String key,
                                            final String value, final String defaultValue) {

            final String trimmed = value.strip();
            if (!Objects.equals(defaultValue, trimmed)) {
                lines.add(s(key, _EQUAL_, trimmed));
            }
        }
    }

    private record ColourFields(JTextArea coloursArea, JTextField amountField,
                                JTextField ignoreField, JTextField borderField,
                                JTextField maskField) {

        private String coloursText() {

            return coloursArea.getText();
        }

        private void setColoursText(final String text) {

            coloursArea.setText(text);
        }

        private String maskText() {

            return maskField.getText();
        }

        private void appendColorIfAbsent(final int colorInt) {

            final String current = coloursArea.getText().strip();
            final String toAdd = String.valueOf(colorInt);
            if (current.isEmpty()) {
                coloursArea.setText(toAdd);
            } else if (!current.contains(toAdd)) {
                coloursArea.setText(s(current, _COMMA_, _SPACE_, toAdd));
            }
        }

        private void loadFrom(final Properties rawProps) {

            coloursArea.setText(rawProps.getProperty(COLOURS, EMPTY));
            amountField.setText(rawProps.getProperty(CDA, EMPTY));
            ignoreField.setText(rawProps.getProperty(CDI, EMPTY));

            final String brdVal = rawProps.getProperty(CDB, null);
            final String cutVal = rawProps.getProperty(KEY_COLOURS_CUT, null);
            borderField.setText(isNotNull(brdVal) ? brdVal : Objects.requireNonNullElse(cutVal, EMPTY));
        }

        private void appendToLines(final Collection<? super String> lines) {

            addIfNotBlank(lines, COLOURS, coloursArea.getText());
            addIfNotBlank(lines, CDA, amountField.getText());
            addIfNotBlank(lines, CDI, ignoreField.getText());
            addIfNotBlank(lines, CDB, borderField.getText());
            addIfNotBlank(lines, MASK, maskField.getText());
        }

        private void applyDefaultsIfEmpty(final int rowCount) {

            if (amountField.getText().isBlank()) {
                amountField.setText(s(Math.min(DEFAULT_MIN_COLOURS, rowCount), _PLUS_));
            }
            if (ignoreField.getText().isBlank()) {
                ignoreField.setText(ORDER);
            }
            if (borderField.getText().isBlank()) {
                borderField.setText(DEFAULT_BORDER_VALUE);
            }
        }

        private static void addIfNotBlank(final Collection<? super String> lines, final String key, final String value) {

            final String trimmed = value.strip();
            if (!trimmed.isEmpty()) {
                lines.add(s(key, _EQUAL_, trimmed));
            }
        }
    }

    private record MetricsFields(JTextField centerX, JTextField centerY, JTextField radius,
                                 JTextField samples, JTextField area, JTextField density) {

        private void updateGeometry(final _Geometry geometry) {

            centerX.setText(s(geometry.center().X()));
            centerY.setText(s(geometry.center().Y()));
            radius.setText(s(ImageSupport.truncateToInt(geometry.radius())));
        }

        private void updateDrawResult(final DrawGridResult result) {

            samples.setText(s(result.samples()));
            area.setText(s(result.area()));
            density.setText(percentString(result.samples(), result.area()));
        }

        private int samplesCount() {

            return Integer.parseInt(samples.getText());
        }
    }

    private static final class InspectorControls {

        private final JLabel hoverCoordLabel = new JLabel(INITIAL_COORD_TEXT);
        private final JLabel hoverColorLabel = new JLabel(INITIAL_COLOR_TEXT);
        private final JPanel hoverSwatch = new JPanel();
        private final DefaultTableModel detectedColorsModel = new DetectedColorsTableModel();
        private Integer selectedColor = null;

        private JLabel hoverCoordLabel() {

            return hoverCoordLabel;
        }

        private JLabel hoverColorLabel() {

            return hoverColorLabel;
        }

        private JPanel hoverSwatch() {

            return hoverSwatch;
        }

        private DefaultTableModel detectedColorsModel() {

            return detectedColorsModel;
        }

        private Integer selectedColor() {

            return selectedColor;
        }

        private void setSelectedColor(final int rgb) {

            selectedColor = rgb;
        }

        private void updateHover(final Point imgPoint, final int rgb) {

            final Color color = new Color(rgb, true);
            hoverSwatch.setBackground(color);
            hoverCoordLabel.setText(s(u(X), _COLON_, _SPACE_, imgPoint.x, _SPACE_, _SPACE_, u(Y), _COLON_, _SPACE_, imgPoint.y));
            hoverColorLabel.setText(String.format(Locale.US, FMT_RGB_INT, color.getRed(), color.getGreen(), color.getBlue(), rgb));
            selectedColor = rgb;
        }

        private void populateDetectedColors(final Map<Integer, Pair<Integer, String>> countMap, final int totalSamples) {

            detectedColorsModel.setRowCount(0);
            final List<Map.Entry<Integer, Pair<Integer, String>>> sorted = new ArrayList<>(countMap.entrySet());
            sorted.sort((e1, e2) -> Integer.compare(e2.getValue().left(), e1.getValue().left()));
            for (final Map.Entry<Integer, Pair<Integer, String>> entry : sorted) {
                addDetectedColorRow(entry.getKey(), entry.getValue().left(), totalSamples);
            }
        }

        private void addDetectedColorRow(final int colorInt, final int count, final int totalSamples) {

            final Fraction frac = fr((double) count / totalSamples);
            final String share = String.format(Locale.US, FMT_SHARE, ts(frac, true),
                    ((double) count / totalSamples) * PERCENT_MULTIPLIER);
            detectedColorsModel.addRow(new Object[]{new Color(colorInt, true), colorInt, count, share});
        }

        private int detectedRowCount() {

            return detectedColorsModel.getRowCount();
        }

        private String formatDetectedColours(final int total) {

            final StringBuilder sb = new StringBuilder(INITIAL_SB_CAPACITY);
            final int rows = detectedColorsModel.getRowCount();
            for (int i = 0; i < rows; i++) {
                final int colorInt = (Integer) detectedColorsModel.getValueAt(i, 1);
                final int count = (Integer) detectedColorsModel.getValueAt(i, 2);
                final Fraction frac = fr((double) count / total);
                if (0 < i) sb.append(_COMMA_).append(_SPACE_);
                sb.append(colorInt).append(VRT_BAR).append(ts(frac, true));
            }
            return sb.toString();
        }
    }

    private static final class EditorState {

        private final JLabel statusLabel = new JLabel(STATUS_READY);
        private final JLabel fileLabel = new JLabel(NO_GRID_FILE_LOADED);
        private File currentGridFile = null;
        private BufferedImage sourceImg = null;
        private Geometry lastGeometry = null;
        private Resolution lastResolution = null;

        private JLabel statusLabel() {

            return statusLabel;
        }

        private JLabel fileLabel() {

            return fileLabel;
        }

        private void setStatus(final String text) {

            statusLabel.setText(text);
        }

        private void setFileName(final String name) {

            fileLabel.setText(name);
        }

        private File currentGridFile() {

            return currentGridFile;
        }

        private void setCurrentGridFile(final File file) {

            currentGridFile = file;
        }

        private BufferedImage sourceImg() {

            return sourceImg;
        }

        private void setSourceImg(final BufferedImage image) {

            sourceImg = image;
        }

        private Geometry lastGeometry() {

            return lastGeometry;
        }

        private Resolution lastResolution() {

            return lastResolution;
        }

        private void setLastCalculation(final Geometry geometry, final Resolution resolutionValue) {

            lastGeometry = geometry;
            lastResolution = resolutionValue;
        }

        private boolean hasSamplingContext() {

            return isNotNull(sourceImg) && isNotNull(lastGeometry) && isNotNull(lastResolution);
        }
    }
}
