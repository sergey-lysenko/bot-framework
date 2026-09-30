package works.lysenko.base.ui;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.g._Grid;
import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.t._Geometry;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.apis.grid.t._RelativePosition;
import works.lysenko.util.data.records.diff.Pair;
import works.lysenko.util.data.type.Grid;
import works.lysenko.util.data.type.RelativePosition;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.gsrc.ColourSearchRequest;
import works.lysenko.util.grid.record.gsrc.ColoursSearchResult;
import works.lysenko.util.grid.record.gsrc.Geometry;
import works.lysenko.util.grid.record.gsrc.Resolution;
import works.lysenko.util.grid.record.misc.DrawGridResult;
import works.lysenko.util.lang.word.M;
import works.lysenko.util.prop.grid.Defaults;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

import static works.lysenko.Base.log;
import static works.lysenko.util.apis.grid.d._Resolution.readResolution;
import static works.lysenko.util.apis.grid.g._GridProperties.getGridPropertiesByPath;
import static works.lysenko.util.chrs.__.AN;
import static works.lysenko.util.chrs.___.PNG;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.func.imgs.Painter.drawArea;
import static works.lysenko.util.func.imgs.Painter.drawGrid;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.grid.Fields.*;
import static works.lysenko.util.grid.record.gsrc.ColourSearchRequest.csr;
import static works.lysenko.util.grid.record.gsrc.Region.search;
import static works.lysenko.util.grid.record.gsrc.Routines.getRequestedGridLocation;
import static works.lysenko.util.lang.word.A.AREA;
import static works.lysenko.util.lang.word.C.CENTER;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.C.COORDINATE;
import static works.lysenko.util.lang.word.D.DENSITY;
import static works.lysenko.util.lang.word.E.EXCEPTION;
import static works.lysenko.util.lang.word.G.GRIDS;
import static works.lysenko.util.lang.word.H.HELPER;
import static works.lysenko.util.lang.word.H.HORIZONTAL;
import static works.lysenko.util.lang.word.O.OCCURED;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.P.PROBLEM;
import static works.lysenko.util.lang.word.R.RADIUS;
import static works.lysenko.util.lang.word.R.RECALCULATE;
import static works.lysenko.util.lang.word.R.RESOLUTION;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.lang.word.V.VERTICAL;
import static works.lysenko.util.spec.Layout.Paths._GRIDS_;
import static works.lysenko.util.spec.Layout.Paths._RUNS_;
import static works.lysenko.util.spec.Symbols.X;
import static works.lysenko.util.spec.Symbols.Y;
import static works.lysenko.util.spec.Symbols._COLON_;

/**
 * Grid Parameters Editor & Helper.
 * Provides interactive visual editing, live recalculation, keyboard nudging,
 * pixel inspection, dominant color sampling, and saving directly to .grid files.
 */
@SuppressWarnings({"ClassWithTooManyFields", "OverlyLongClass", "OverlyComplexClass"})
public final class Helper extends JFrame {

    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 850;
    private static final int SIDEBAR_WIDTH = 340;

    private final ImagePanel imagePanel;
    private final JScrollPane imageScrollPane;

    // Geometry fields
    private final JTextField horizontal;
    private final JTextField vertical;
    private final JTextField scale;
    private final JTextField resolution;
    private final JTextField mask;

    // Colour fields
    private final JTextArea colours;
    private final JTextField coloursAmount;
    private final JTextField coloursIgnore;
    private final JTextField coloursBorder;

    // Metrics fields (read-only)
    private final JTextField centerX;
    private final JTextField centerY;
    private final JTextField radius;
    private final JTextField samples;
    private final JTextField area;
    private final JTextField density;

    // Inspector
    private final JLabel hoverCoordLabel = new JLabel("X: -  Y: -");
    private final JLabel hoverColorLabel = new JLabel("RGB: -  Int: -");
    private final JPanel hoverSwatch = new JPanel();
    private Integer selectedColor = null;

    // Detected colors table
    private final DefaultTableModel detectedColorsModel;
    private final JTable detectedColorsTable;

    // Status bar
    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel fileLabel = new JLabel("No Grid File Loaded");

    // Zoom
    private final JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 10, 500, 100);

    // State
    private File currentGridFile = null;
    private File currentImageFile = null;
    private BufferedImage sourceImg = null;
    private BufferedImage targetImg = null;
    private Geometry lastGeometry = null;
    private Resolution lastResolution = null;

    /**
     * Constructs a new instance of the Helper editor.
     */
    @SuppressWarnings({"OverlyLongMethod", "MagicNumber"})
    public Helper() {

        setTitle(b(c(GRID), c(PARAMETERS), c(HELPER)));
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Center: image in scroll pane
        imagePanel = new ImagePanel();
        imagePanel.setScale(1.0F);
        imagePanel.setFocusable(true);
        imageScrollPane = new JScrollPane(imagePanel);
        imageScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        imageScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // Sidebar
        final JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // 1. File & Action Panel
        final JPanel fileCard = new JPanel(new BorderLayout(5, 5));
        fileCard.setBorder(BorderFactory.createTitledBorder("Grid File"));
        fileLabel.setFont(fileLabel.getFont().deriveFont(Font.BOLD, 11.0f));
        fileCard.add(fileLabel, BorderLayout.NORTH);

        final JPanel fileBtnRow = new JPanel(new GridLayout(1, 3, 4, 4));
        final JButton saveBtn = new JButton("Save");
        saveBtn.setToolTipText("Save changes to grid file (Cmd+S)");
        saveBtn.addActionListener(e -> saveCurrentGrid());
        final JButton autoImgBtn = new JButton("Find Img");
        autoImgBtn.setToolTipText("Auto-locate recent screenshot for this grid");
        autoImgBtn.addActionListener(e -> autoLocateScreenshot());
        final JButton recalcBtn = new JButton(c(RECALCULATE));
        recalcBtn.addActionListener(e -> recalculate());
        fileBtnRow.add(saveBtn);
        fileBtnRow.add(autoImgBtn);
        fileBtnRow.add(recalcBtn);
        fileCard.add(fileBtnRow, BorderLayout.SOUTH);
        controlPanel.add(fileCard);
        controlPanel.add(Box.createVerticalStrut(6));

        // 2. Geometry Panel
        final JPanel geomCard = new JPanel();
        geomCard.setLayout(new BoxLayout(geomCard, BoxLayout.Y_AXIS));
        geomCard.setBorder(BorderFactory.createTitledBorder("Geometry"));

        resolution = addField(geomCard, s(c(RESOLUTION), _COLON_), Defaults.resolution);
        scale = addField(geomCard, s(c(SCALE), _COLON_), Defaults.scale);
        horizontal = addField(geomCard, s(c(HORIZONTAL), _COLON_), Defaults.horizontal);
        vertical = addField(geomCard, s(c(VERTICAL), _COLON_), Defaults.vertical);

        // Nudge buttons
        final JPanel nudgeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        final JButton leftBtn = new JButton("◄");
        final JButton upBtn = new JButton("▲");
        final JButton downBtn = new JButton("▼");
        final JButton rightBtn = new JButton("►");
        final Dimension arrowDim = new Dimension(42, 26);
        leftBtn.setPreferredSize(arrowDim);
        upBtn.setPreferredSize(arrowDim);
        downBtn.setPreferredSize(arrowDim);
        rightBtn.setPreferredSize(arrowDim);

        leftBtn.addActionListener(e -> nudgeHorizontal(-0.002));
        rightBtn.addActionListener(e -> nudgeHorizontal(0.002));
        upBtn.addActionListener(e -> nudgeVertical(-0.002));
        downBtn.addActionListener(e -> nudgeVertical(0.002));

        nudgeRow.add(leftBtn);
        nudgeRow.add(upBtn);
        nudgeRow.add(downBtn);
        nudgeRow.add(rightBtn);
        geomCard.add(nudgeRow);

        controlPanel.add(geomCard);
        controlPanel.add(Box.createVerticalStrut(6));

        // 3. Computed Metrics Panel
        final JPanel metricsCard = new JPanel(new GridLayout(3, 2, 4, 2));
        metricsCard.setBorder(BorderFactory.createTitledBorder("Calculated Metrics"));
        centerX = addReadOnlyField(metricsCard, "Center X:");
        centerY = addReadOnlyField(metricsCard, "Center Y:");
        radius = addReadOnlyField(metricsCard, "Radius:");
        samples = addReadOnlyField(metricsCard, "Samples:");
        area = addReadOnlyField(metricsCard, "Area:");
        density = addReadOnlyField(metricsCard, "Density:");
        controlPanel.add(metricsCard);
        controlPanel.add(Box.createVerticalStrut(6));

        // 4. Colours & Rules Panel
        final JPanel coloursCard = new JPanel();
        coloursCard.setLayout(new BoxLayout(coloursCard, BoxLayout.Y_AXIS));
        coloursCard.setBorder(BorderFactory.createTitledBorder("Colours & Rules"));

        final JLabel clrLbl = new JLabel("Colours (Quotas):");
        clrLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        coloursCard.add(clrLbl);
        colours = new JTextArea(3, 20);
        colours.setLineWrap(true);
        colours.setWrapStyleWord(false);
        final JScrollPane clrScroll = new JScrollPane(colours);
        clrScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        clrScroll.setPreferredSize(new Dimension(SIDEBAR_WIDTH - 30, 60));
        coloursCard.add(clrScroll);

        coloursAmount = addField(coloursCard, "Colours Amount:", StringUtils.EMPTY);
        coloursIgnore = addField(coloursCard, "Colours Ignore:", StringUtils.EMPTY);
        coloursBorder = addField(coloursCard, "Colours Border/Cutoff:", StringUtils.EMPTY);
        mask = addField(coloursCard, s(c(M.MASK), _COLON_), StringUtils.EMPTY);

        controlPanel.add(coloursCard);
        controlPanel.add(Box.createVerticalStrut(6));

        // 5. Pixel Inspector & Detected Colors Panel
        final JPanel inspectCard = new JPanel();
        inspectCard.setLayout(new BoxLayout(inspectCard, BoxLayout.Y_AXIS));
        inspectCard.setBorder(BorderFactory.createTitledBorder("Pixel Inspector & Dominant Colours"));

        final JPanel hoverRow = new JPanel(new BorderLayout(5, 5));
        hoverSwatch.setPreferredSize(new Dimension(28, 28));
        hoverSwatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        hoverSwatch.setBackground(Color.WHITE);
        hoverRow.add(hoverSwatch, BorderLayout.WEST);

        final JPanel hoverTextPanel = new JPanel(new GridLayout(2, 1));
        hoverTextPanel.add(hoverCoordLabel);
        hoverTextPanel.add(hoverColorLabel);
        hoverRow.add(hoverTextPanel, BorderLayout.CENTER);
        inspectCard.add(hoverRow);
        inspectCard.add(Box.createVerticalStrut(4));

        final JPanel btnInspectRow = new JPanel(new GridLayout(1, 2, 4, 4));
        final JButton addColorBtn = new JButton("+ Add Pixel");
        addColorBtn.setToolTipText("Add inspected pixel color to Colours list");
        addColorBtn.addActionListener(e -> addInspectedColor());

        final JButton sampleGridBtn = new JButton("Sample Grid");
        sampleGridBtn.setToolTipText("Analyze all grid sample points on screenshot");
        sampleGridBtn.addActionListener(e -> sampleGridColours());

        btnInspectRow.add(addColorBtn);
        btnInspectRow.add(sampleGridBtn);
        inspectCard.add(btnInspectRow);
        inspectCard.add(Box.createVerticalStrut(4));

        // Table of detected colors
        final String[] cols = {"Swatch", "Color ID", "Count", "Share"};
        detectedColorsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(final int r, final int c) {
                return false;
            }
        };
        detectedColorsTable = new JTable(detectedColorsModel);
        detectedColorsTable.getColumnModel().getColumn(0).setMaxWidth(40);
        detectedColorsTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                           final boolean isSelected, final boolean hasFocus,
                                                           final int row, final int col) {
                final JPanel p = new JPanel();
                if (value instanceof Color c) p.setBackground(c);
                p.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                return p;
            }
        });
        final JScrollPane tableScroll = new JScrollPane(detectedColorsTable);
        tableScroll.setPreferredSize(new Dimension(SIDEBAR_WIDTH - 30, 110));
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        inspectCard.add(tableScroll);

        final JButton fillColoursBtn = new JButton("Apply Detected Colours to Field");
        fillColoursBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        fillColoursBtn.addActionListener(e -> applyDetectedColours());
        inspectCard.add(Box.createVerticalStrut(4));
        inspectCard.add(fillColoursBtn);

        controlPanel.add(inspectCard);

        final JScrollPane sideScrollPane = new JScrollPane(controlPanel);
        sideScrollPane.setPreferredSize(new Dimension(SIDEBAR_WIDTH, DEFAULT_HEIGHT));
        sideScrollPane.setMinimumSize(new Dimension(240, 200));
        sideScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        imageScrollPane.setMinimumSize(new Dimension(300, 200));

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sideScrollPane, imageScrollPane);
        splitPane.setDividerLocation(360);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        add(splitPane, BorderLayout.CENTER);

        // Bottom Bar (Status + Zoom)
        final JPanel bottomBar = new JPanel(new BorderLayout(5, 5));
        bottomBar.setBorder(new EmptyBorder(4, 8, 4, 8));
        bottomBar.add(statusLabel, BorderLayout.WEST);

        final JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        zoomPanel.add(new JLabel("Zoom:"));
        slider.setPreferredSize(new Dimension(140, 24));
        slider.addChangeListener(this::zoomChanged);
        zoomPanel.add(slider);

        final JButton zoom100 = new JButton("100%");
        zoom100.setPreferredSize(new Dimension(65, 24));
        zoom100.addActionListener(e -> slider.setValue(100));
        zoomPanel.add(zoom100);

        final JButton zoomFit = new JButton("Fit");
        zoomFit.setPreferredSize(new Dimension(55, 24));
        zoomFit.addActionListener(e -> fitImageToView());
        zoomPanel.add(zoomFit);

        bottomBar.add(zoomPanel, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);

        // Setup listeners and menus
        setupMenu();
        setupImageInteractivity();
        setupKeyBindings();
    }

    /**
     * Constructs Helper with an initial grid or image file.
     *
     * @param file the file to open
     */
    public Helper(final File file) {

        this();
        if (isNotNull(file) && file.exists()) {
            final String name = file.getName().toLowerCase();
            if (name.endsWith(".grid")) {
                loadGridFile(file);
                autoLocateScreenshot();
            } else if (name.endsWith(".png") || name.endsWith(".jpg")) {
                loadImageFile(file);
            }
        }
    }

    /**
     * Entry point for running Grid Editor from IDE or CLI.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            final File file = (args.length > 0) ? new File(args[0]) : null;
            final Helper frame = new Helper(file);
            frame.setVisible(true);
        });
    }

    private void setupMenu() {

        final JMenuBar menuBar = new JMenuBar();

        // File Menu
        final JMenu fileMenu = new JMenu("File");
        final JMenuItem openGridItem = new JMenuItem("Open Grid...");
        openGridItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        openGridItem.addActionListener(e -> chooseAndOpenGrid());

        final JMenuItem openImgItem = new JMenuItem("Open Screenshot...");
        openImgItem.addActionListener(e -> chooseAndOpenImage());

        final JMenuItem saveItem = new JMenuItem("Save Grid");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.addActionListener(e -> saveCurrentGrid());

        final JMenuItem saveAsItem = new JMenuItem("Save Grid As...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveGridAs());

        final JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());

        fileMenu.add(openGridItem);
        fileMenu.add(openImgItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit Menu
        final JMenu editMenu = new JMenu("Edit");
        final JMenuItem recalcItem = new JMenuItem("Recalculate");
        recalcItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        recalcItem.addActionListener(e -> recalculate());

        final JMenuItem sampleItem = new JMenuItem("Sample Grid Colours");
        sampleItem.addActionListener(e -> sampleGridColours());

        final JMenuItem copyColoursItem = new JMenuItem("Copy Colours to Clipboard");
        copyColoursItem.addActionListener(e -> {
            final String sel = colours.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sel), null);
            statusLabel.setText("Colours copied to clipboard.");
        });

        editMenu.add(recalcItem);
        editMenu.add(sampleItem);
        editMenu.add(copyColoursItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        setJMenuBar(menuBar);
    }

    private void setupImageInteractivity() {

        imagePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                updatePixelInspection(e.getPoint());
            }

            @Override
            public void mouseDragged(final MouseEvent e) {
                if (isNotNull(sourceImg)) {
                    final Point p = imagePanel.toImageCoordinates(e.getX(), e.getY());
                    if (isNotNull(p)) {
                        final double h = (double) p.x / sourceImg.getWidth(null);
                        final double v = (double) p.y / sourceImg.getHeight(null);
                        horizontal.setText(String.format(Locale.US, "%.4f", h));
                        vertical.setText(String.format(Locale.US, "%.4f", v));
                        recalculate();
                    }
                }
            }
        });

        imagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final Point p = imagePanel.toImageCoordinates(e.getX(), e.getY());
                if (isNotNull(p) && isNotNull(sourceImg)) {
                    selectedColor = sourceImg.getRGB(p.x, p.y);
                    updatePixelInspection(e.getPoint());
                    if (e.isShiftDown() || 2 == e.getClickCount()) {
                        // Shift-click or double click centers the grid here
                        final double h = (double) p.x / sourceImg.getWidth(null);
                        final double v = (double) p.y / sourceImg.getHeight(null);
                        horizontal.setText(String.format(Locale.US, "%.4f", h));
                        vertical.setText(String.format(Locale.US, "%.4f", v));
                        recalculate();
                    }
                }
            }
        });

        // Mouse wheel zooming with Cmd/Ctrl
        imageScrollPane.addMouseWheelListener(e -> {
            if (e.isMetaDown() || e.isControlDown()) {
                final int notches = e.getWheelRotation();
                final int current = slider.getValue();
                slider.setValue(Math.max(10, Math.min(500, current - notches * 15)));
                e.consume();
            }
        });
    }

    private void setupKeyBindings() {

        final JComponent root = getRootPane();
        final InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "nudgeLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nudgeRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "nudgeUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "nudgeDown");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK), "nudgeLeftBig");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK), "nudgeRightBig");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK), "nudgeUpBig");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK), "nudgeDownBig");

        am.put("nudgeLeft", new AbstractAction() { public void actionPerformed(final ActionEvent e) { nudgeHorizontal(-0.002); } });
        am.put("nudgeRight", new AbstractAction() { public void actionPerformed(final ActionEvent e) { nudgeHorizontal(0.002); } });
        am.put("nudgeUp", new AbstractAction() { public void actionPerformed(final ActionEvent e) { nudgeVertical(-0.002); } });
        am.put("nudgeDown", new AbstractAction() { public void actionPerformed(final ActionEvent e) { nudgeVertical(0.002); } });

        am.put("nudgeLeftBig", new AbstractAction() { public void actionPerformed(final ActionEvent e) { nudgeHorizontal(-0.01); } });
        am.put("nudgeRightBig", new AbstractAction() { public void actionPerformed(final ActionEvent e) { nudgeHorizontal(0.01); } });
        am.put("nudgeUpBig", new AbstractAction() { public void actionPerformed(final ActionEvent e) { nudgeVertical(-0.01); } });
        am.put("nudgeDownBig", new AbstractAction() { public void actionPerformed(final ActionEvent e) { nudgeVertical(0.01); } });
    }

    private void nudgeHorizontal(final double delta) {

        try {
            final double val = fr(horizontal.getText()).doubleValue() + delta;
            horizontal.setText(String.format(Locale.US, "%.4f", Math.max(0.0, Math.min(1.0, val))));
            recalculate();
        } catch (final Exception ignored) {}
    }

    private void nudgeVertical(final double delta) {

        try {
            final double val = fr(vertical.getText()).doubleValue() + delta;
            vertical.setText(String.format(Locale.US, "%.4f", Math.max(0.0, Math.min(1.0, val))));
            recalculate();
        } catch (final Exception ignored) {}
    }

    private void updatePixelInspection(final Point panelPoint) {

        final Point imgP = imagePanel.toImageCoordinates(panelPoint.x, panelPoint.y);
        if (isNotNull(imgP) && isNotNull(sourceImg)) {
            final int rgb = sourceImg.getRGB(imgP.x, imgP.y);
            final Color c = new Color(rgb, true);
            hoverSwatch.setBackground(c);
            hoverCoordLabel.setText("X: " + imgP.x + "  Y: " + imgP.y);
            hoverColorLabel.setText(String.format(Locale.US, "R:%d G:%d B:%d  Int:%d", c.getRed(), c.getGreen(), c.getBlue(), rgb));
            selectedColor = rgb;
        }
    }

    private void addInspectedColor() {

        if (isNotNull(selectedColor)) {
            final String current = colours.getText().trim();
            final String toAdd = String.valueOf(selectedColor);
            if (current.isEmpty()) {
                colours.setText(toAdd);
            } else if (!current.contains(toAdd)) {
                colours.setText(current + ", " + toAdd);
            }
            statusLabel.setText("Added color " + selectedColor + " to field.");
        }
    }

    public void loadGridFile(final File file) {

        if (isNull(file) || !file.exists()) return;
        currentGridFile = file;
        fileLabel.setText(file.getName());
        setTitle("Grid Editor - " + file.getName());

        try {
            final Properties rawProps = new Properties();
            try (final java.io.InputStream in = java.nio.file.Files.newInputStream(file.toPath())) {
                rawProps.load(in);
            }

            resolution.setText(rawProps.getProperty("resolution", Defaults.resolution));
            scale.setText(rawProps.getProperty("scale", Defaults.scale));
            horizontal.setText(rawProps.getProperty("horizontal", Defaults.horizontal));
            vertical.setText(rawProps.getProperty("vertical", Defaults.vertical));

            colours.setText(rawProps.getProperty("colours", StringUtils.EMPTY));
            coloursAmount.setText(rawProps.getProperty("colours.amount", StringUtils.EMPTY));
            coloursIgnore.setText(rawProps.getProperty("colours.ignore", StringUtils.EMPTY));

            final String brdVal = rawProps.getProperty("colours.border", null);
            final String cutVal = rawProps.getProperty("colours.cut", null);
            coloursBorder.setText(isNotNull(brdVal) ? brdVal : (isNotNull(cutVal) ? cutVal : StringUtils.EMPTY));

            statusLabel.setText("Loaded grid: " + file.getName());
            if (isNotNull(sourceImg)) {
                recalculate();
            } else {
                updateEmptyImagePlaceholder();
            }
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load grid: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadImageFile(final File file) {

        if (isNull(file) || !file.exists()) return;
        try {
            sourceImg = ImageIO.read(file);
            currentImageFile = file;
            statusLabel.setText("Loaded screenshot: " + file.getName() + " (" + sourceImg.getWidth() + "x" + sourceImg.getHeight() + ")");
            recalculate();
            fitImageToView();
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to load screenshot: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void autoLocateScreenshot() {

        final File found = findRecentScreenshot(currentGridFile);
        if (isNotNull(found)) {
            loadImageFile(found);
        } else {
            statusLabel.setText("No recent screenshot found automatically. Please open one manually.");
        }
    }

    private static File findRecentScreenshot(final File gridFile) {

        final List<File> candidates = new ArrayList<>();
        final String[] searchDirs = {_RUNS_, "target/runs", "runs", "src/main/resources"};

        for (final String dirPath : searchDirs) {
            final File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                collectPngFiles(dir, candidates, 4);
            }
        }

        if (candidates.isEmpty()) return null;

        // If grid file is known, prioritize screenshots containing the grid base name
        if (isNotNull(gridFile)) {
            final String baseName = gridFile.getName().replace(".grid", "").toLowerCase();
            for (final File f : candidates) {
                if (f.getName().toLowerCase().contains(baseName)) return f;
            }
        }

        // Otherwise return the most recently modified screenshot
        candidates.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        return candidates.get(0);
    }

    private static void collectPngFiles(final File dir, final List<File> list, final int depth) {

        if (depth < 0) return;
        final File[] files = dir.listFiles();
        if (isNull(files)) return;
        for (final File f : files) {
            if (f.isDirectory()) {
                collectPngFiles(f, list, depth - 1);
            } else if (f.getName().toLowerCase().endsWith(".png")) {
                list.add(f);
            }
        }
    }

    private void chooseAndOpenGrid() {

        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(_GRIDS_).exists() ? new File(_GRIDS_) : new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter("Grid files (*.grid)", "grid"));
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            loadGridFile(chooser.getSelectedFile());
            if (isNull(sourceImg)) autoLocateScreenshot();
        }
    }

    private void chooseAndOpenImage() {

        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(_RUNS_).exists() ? new File(_RUNS_) : new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            loadImageFile(chooser.getSelectedFile());
        }
    }

    private void saveCurrentGrid() {

        if (isNull(currentGridFile)) {
            saveGridAs();
        } else {
            writeGridToFile(currentGridFile);
        }
    }

    private void saveGridAs() {

        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(_GRIDS_).exists() ? new File(_GRIDS_) : new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter("Grid files (*.grid)", "grid"));
        if (isNotNull(currentGridFile)) chooser.setSelectedFile(currentGridFile);
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(this)) {
            File f = chooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".grid")) {
                f = new File(f.getAbsolutePath() + ".grid");
            }
            writeGridToFile(f);
        }
    }

    private void writeGridToFile(final File targetFile) {

        try {
            final List<String> lines = new ArrayList<>();
            lines.add("resolution=" + resolution.getText().trim());
            lines.add("scale=" + scale.getText().trim());

            final String hVal = horizontal.getText().trim();
            final String vVal = vertical.getText().trim();
            if (!Defaults.horizontal.equals(hVal)) lines.add("horizontal=" + hVal);
            if (!Defaults.vertical.equals(vVal)) lines.add("vertical=" + vVal);

            final String clrVal = colours.getText().trim();
            if (!clrVal.isEmpty()) lines.add("colours=" + clrVal);

            final String amtVal = coloursAmount.getText().trim();
            if (!amtVal.isEmpty()) lines.add("colours.amount=" + amtVal);

            final String ignVal = coloursIgnore.getText().trim();
            if (!ignVal.isEmpty()) lines.add("colours.ignore=" + ignVal);

            final String brdVal = coloursBorder.getText().trim();
            if (!brdVal.isEmpty()) lines.add("colours.border=" + brdVal);

            final String mskVal = mask.getText().trim();
            if (!mskVal.isEmpty()) lines.add("mask=" + mskVal);

            Files.write(targetFile.toPath(), lines, StandardCharsets.UTF_8);
            currentGridFile = targetFile;
            fileLabel.setText(targetFile.getName());
            setTitle("Grid Editor - " + targetFile.getName());
            statusLabel.setText("Saved grid: " + targetFile.getName());
            JOptionPane.showMessageDialog(this, "Saved successfully to " + targetFile.getName(), "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sampleGridColours() {

        if (isNull(sourceImg) || isNull(lastGeometry) || isNull(lastResolution)) {
            recalculate();
            if (isNull(sourceImg) || isNull(lastGeometry)) {
                JOptionPane.showMessageDialog(this, "Please open a screenshot first to sample grid colours.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }

        try {
            final _RelativePosition pos = RelativePosition.rp(fr(horizontal.getText()), fr(vertical.getText()));
            final Request req = new Request("sample", (BufferedImage) sourceImg, pos, fr(scale.getText()), lastResolution, null, null);
            final _Grid grid = new Grid((BufferedImage) sourceImg, lastGeometry, req);

            final Map<Integer, Pair<Integer, String>> countMap = grid.calculator().countPixelsByColor();
            final int totalSamples = grid.getSamplePoints().size();

            detectedColorsModel.setRowCount(0);
            final List<Map.Entry<Integer, Pair<Integer, String>>> sorted = new ArrayList<>(countMap.entrySet());
            sorted.sort((e1, e2) -> Integer.compare(e2.getValue().left(), e1.getValue().left()));

            for (final Map.Entry<Integer, Pair<Integer, String>> entry : sorted) {
                final int colorInt = entry.getKey();
                final int count = entry.getValue().left();
                final Fraction frac = fr((double) count / totalSamples);
                final String share = String.format(Locale.US, "%s (%.1f%%)", ts(frac, true), ((double) count / totalSamples) * 100.0);
                detectedColorsModel.addRow(new Object[]{new Color(colorInt, true), colorInt, count, share});
            }

            statusLabel.setText(String.format(Locale.US, "Sampled %d grid points across %d distinct colours.", totalSamples, countMap.size()));
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(this, "Sampling failed: " + e.getMessage(), "Sampling Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyDetectedColours() {

        if (0 == detectedColorsModel.getRowCount()) {
            sampleGridColours();
        }
        if (0 == detectedColorsModel.getRowCount()) return;

        final StringBuilder sb = new StringBuilder();
        final int rows = detectedColorsModel.getRowCount();
        for (int i = 0; i < rows; i++) {
            final int colorInt = (Integer) detectedColorsModel.getValueAt(i, 1);
            final int count = (Integer) detectedColorsModel.getValueAt(i, 2);
            final int total = Integer.parseInt(samples.getText());
            final Fraction frac = fr((double) count / total);
            if (i > 0) sb.append(", ");
            sb.append(colorInt).append("|").append(ts(frac, true));
        }

        colours.setText(sb.toString());
        if (coloursAmount.getText().trim().isEmpty()) {
            coloursAmount.setText(Math.min(2, rows) + "+");
        }
        if (coloursIgnore.getText().trim().isEmpty()) {
            coloursIgnore.setText("order");
        }
        if (coloursBorder.getText().trim().isEmpty()) {
            coloursBorder.setText("1/100");
        }
        statusLabel.setText("Applied " + rows + " detected colours to configuration.");
    }

    private void updateEmptyImagePlaceholder() {

        try {
            final Resolution to = readResolution(resolution.getText());
            int w = 1080;
            int h = 2400;
            final String resText = resolution.getText().trim();
            if (resText.matches("^\\d+x\\d+$")) {
                final String[] parts = resText.split("x");
                w = Integer.parseInt(parts[0]);
                h = Integer.parseInt(parts[1]);
            }
            final BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g = canvas.createGraphics();
            g.setColor(new Color(30, 30, 30));
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(60, 60, 60));
            for (int x = 0; x < w; x += 100) g.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 100) g.drawLine(0, y, w, y);
            g.dispose();

            final Geometry at = getRequestedGridLocation(
                    w, h,
                    fr(horizontal.getText()).doubleValue(),
                    fr(vertical.getText()).doubleValue(),
                    fr(scale.getText()).doubleValue(),
                    to.ratio());
            lastGeometry = at;
            lastResolution = to;

            centerX.setText(s(at.center().X()));
            centerY.setText(s(at.center().Y()));
            radius.setText(s((int) at.radius()));

            final DrawGridResult result = drawGrid(canvas, at, to, null);
            samples.setText(s(result.samples()));
            area.setText(s(result.area()));
            density.setText(percentString(result.samples(), result.area()));

            imagePanel.setImage(result.image());
            imageScrollPane.repaint();
            fitImageToView();
        } catch (final Exception ignored) {}
    }

    private void recalculate() {

        if (isNotNull(sourceImg)) {
            try {
                final Resolution to = readResolution(resolution.getText());
                final Geometry at = getRequestedGridLocation(
                        ((RenderedImage) sourceImg).getWidth(), ((RenderedImage) sourceImg).getHeight(),
                        fr(horizontal.getText()).doubleValue(),
                        fr(vertical.getText()).doubleValue(),
                        fr(scale.getText()).doubleValue(),
                        to.ratio());
                lastGeometry = at;
                lastResolution = to;

                final ColourSearchRequest request = csr(mask.getText());
                if (isNotNull(request) && !request.getUnknownColours().isEmpty()) {
                    JOptionPane.showMessageDialog(this, b(s1(request.getUnknownColours().size(), b(UNKNOWN, COLOUR)),
                            java.util.Arrays.toString(request.getUnknownColours().toArray())), c(PROBLEM),
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    updateValuesAndRepaint(request, copySource(), at, to);
                }
            } catch (final RuntimeException e) {
                JOptionPane.showMessageDialog(this,
                        b(c(AN), EXCEPTION, s(OCCURED, _COLON_), e.getMessage(), Arrays.toLines(e.getStackTrace())),
                        c(EXCEPTION),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateValuesAndRepaint(final ColourSearchRequest request, BufferedImage image,
                                        final _Geometry geometry, final _Resolution resolutionValue) {

        centerX.setText(s(geometry.center().X()));
        centerY.setText(s(geometry.center().Y()));
        radius.setText(s((int) geometry.radius()));
        final Paint colour = new Color(255, 0, 0, 100);
        final ColoursSearchResult result = search(request, image);
        final Collection<_Region> exclude = isNotNull(result) ? result.regions() : null;
        if (isNotNull(exclude)) for (final _Region region : exclude) image = drawArea(image, colour, region);
        targetImg = drawAndUpdateImage(image, geometry, resolutionValue, exclude);
        imagePanel.setImage(targetImg);
        imageScrollPane.repaint();
    }

    private BufferedImage drawAndUpdateImage(final BufferedImage image, final _Geometry geometry,
                                             final _Resolution resVal, final Iterable<? extends _Region> exclude) {

        final DrawGridResult result = drawGrid(image, geometry, resVal, exclude);
        samples.setText(s(result.samples()));
        area.setText(s(result.area()));
        density.setText(percentString(result.samples(), result.area()));
        return result.image();
    }

    private BufferedImage copySource() {

        final BufferedImage copy = new BufferedImage(((RenderedImage) sourceImg).getWidth(),
                ((RenderedImage) sourceImg).getHeight(), ((BufferedImage) sourceImg).getType());
        final Graphics2D g = copy.createGraphics();
        try {
            g.drawImage(sourceImg, 0, 0, null);
        } finally {
            g.dispose();
        }
        return copy;
    }

    private void zoomChanged(final ChangeEvent e) {

        final int val = slider.getValue();
        final float sc = val / 100.0f;
        imagePanel.setScale(sc);
        imageScrollPane.revalidate();
        imageScrollPane.repaint();
    }

    private void fitImageToView() {

        if (isNull(sourceImg)) return;
        final Dimension viewSize = imageScrollPane.getViewport().getSize();
        if (viewSize.width <= 0 || viewSize.height <= 0) return;
        final float scaleX = (float) viewSize.width / sourceImg.getWidth();
        final float scaleY = (float) viewSize.height / sourceImg.getHeight();
        final float fit = Math.min(scaleX, scaleY) * 0.95f;
        slider.setValue((int) (fit * 100));
    }

    private JTextField addField(final JPanel panel, final String labelText, final String initialValue) {

        final JPanel row = new JPanel(new BorderLayout(5, 2));
        final JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(140, 22));
        final JTextField field = new JTextField(initialValue);
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) recalculate();
            }
        });
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        panel.add(row);
        panel.add(Box.createVerticalStrut(2));
        return field;
    }

    private JTextField addReadOnlyField(final JPanel panel, final String labelText) {

        final JPanel row = new JPanel(new BorderLayout(5, 2));
        final JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(80, 20));
        final JTextField field = new JTextField();
        field.setEditable(false);
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        panel.add(row);
        return field;
    }

    private static boolean isNull(final Object o) {

        return null == o;
    }
}
