package works.lysenko.base.ui;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.ui.tree.ScenarioOffsetSourceCodec;
import works.lysenko.base.ui.tree.ScenarioOffsetSourceCodec.Offsets;
import works.lysenko.base.ui.tree.TreeLayoutAutoSolver;
import works.lysenko.base.ui.tree.TreeLayoutAutoSolver.AspectPreset;
import works.lysenko.base.ui.tree.TreeLayoutAutoSolver.SolveProgress;
import works.lysenko.base.ui.tree.TreeLayoutAutoSolver.SolveResult;
import works.lysenko.base.ui.tree.TreeLayoutEditorCanvas;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.OffsetChange;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.Point2DGrid;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.PreviewResult;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.PreviewSettings;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.ScenarioBlueprint;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interactive visual editor for scenario {@code @VisualOffset} and {@code @VisualRowOffset}
 * annotations, featuring a live pan/zoom tree canvas, a toggleable hierarchy/table navigator,
 * node inspector with spinners & keyboard nudging, undo/redo, and safe source saving.
 */
public class TreeLayoutEditorPanel extends JPanel {

    private static final String CARD_TREE = "TREE";
    private static final String CARD_TABLE = "TABLE";

    private enum FilterMode {
        ALL, BRANCHES, SHIFTED, MODIFIED
    }

    private final TreeLayoutEditorModel model;
    private final TreeLayoutEditorCanvas canvas;

    // Top toolbar
    private final JCheckBox gridCheck = new JCheckBox("Grid", true);
    private final JCheckBox ghostsCheck = new JCheckBox("Raw Ghosts", false);
    private final JCheckBox compactedCheck = new JCheckBox("Compacted", true);
    private final JSpinner minSepSpinner;
    private final JLabel statusMetricsLabel = new JLabel("No scenarios loaded");

    // Left navigator
    private final JTextField searchField = new JTextField(12);
    private final JToggleButton treeViewBtn = new JToggleButton("Tree", true);
    private final JToggleButton tableViewBtn = new JToggleButton("Table", false);
    private final JToggleButton filterAllBtn = new JToggleButton("All", true);
    private final JToggleButton filterBranchesBtn = new JToggleButton("Branches", false);
    private final JToggleButton filterShiftedBtn = new JToggleButton("Shifted", false);
    private final JToggleButton filterModifiedBtn = new JToggleButton("Modified", false);
    private final CardLayout navigatorCardLayout = new CardLayout();
    private final JPanel navigatorCardPanel = new JPanel(navigatorCardLayout);

    // Tree view
    private final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Scenarios");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
    private final JTree scenarioTree = new JTree(treeModel);
    private final Map<String, DefaultMutableTreeNode> treeNodeByClassName = new HashMap<>();

    // Table view
    private final DefaultTableModel tableModel;
    private final JTable scenarioTable;
    private final TableRowSorter<DefaultTableModel> tableSorter;

    // Inspector
    private final JLabel inspectorNameLabel = new JLabel("No node selected");
    private final JLabel inspectorMetaLabel = new JLabel("Click any node on the canvas or navigator");
    private final JLabel inspectorPosLabel = new JLabel(" ");
    private final JSpinner colOffsetSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));
    private final JSpinner rowOffsetSpinner = new JSpinner(new SpinnerNumberModel(0, -99, 99, 1));
    private final JButton resetColBtn = new JButton("0");
    private final JButton resetRowBtn = new JButton("0");
    private final JButton revertNodeBtn = new JButton("Revert Node");
    private final JButton focusNodeBtn = new JButton("Focus");

    // Bottom actions
    private final JButton undoBtn = new JButton("Undo");
    private final JButton redoBtn = new JButton("Redo");
    private final JButton revertAllBtn = new JButton("Revert All");
    private final JButton resetAllZeroBtn = new JButton("Reset All to 0");
    private final JComboBox<AspectPreset> aspectPresetCombo = new JComboBox<>(AspectPreset.values());
    private final JButton autoLayoutBtn = new JButton("Auto-Layout");
    private final JButton saveBtn = new JButton("Save to Sources");

    private FilterMode filterMode = FilterMode.ALL;
    private String selectedClassName;
    private String loadedRootPackage;
    private boolean syncingUi;
    private boolean initialFitPending = true;
    private SwingWorker<SolveResult, SolveProgress> activeSolver;
    private final AtomicBoolean solverCancelled = new AtomicBoolean(false);

    public TreeLayoutEditorPanel() {
        this(new TreeLayoutEditorModel(), true);
    }

    TreeLayoutEditorPanel(final TreeLayoutEditorModel model, final boolean autoLoadFromPackage) {
        this.model = Objects.requireNonNull(model, "model");
        this.canvas = new TreeLayoutEditorCanvas(this.model);
        this.minSepSpinner = new JSpinner(new SpinnerNumberModel(
                Math.max(0, model.previewSettings().minOffsetSeparation), 0, 10, 1));

        setLayout(new BorderLayout(0, 0));

        // Table model & table
        tableModel = new DefaultTableModel(new Object[]{"Scenario", "Col Δ", "Row Δ", "Class"}, 0) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return column == 1 || column == 2;
            }

            @Override
            public Class<?> getColumnClass(final int columnIndex) {
                return (columnIndex == 1 || columnIndex == 2) ? Integer.class : String.class;
            }
        };
        scenarioTable = new JTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);
        configureTable();
        configureTree();
        configureCanvasCallbacks();
        configureInspector();
        installKeyBindings();

        add(buildTopToolbar(), BorderLayout.NORTH);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPane(), canvas);
        splitPane.setDividerLocation(340);
        splitPane.setResizeWeight(0.0);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);

        add(buildBottomBar(), BorderLayout.SOUTH);

        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                if (initialFitPending && canvas.getWidth() > 0 && canvas.getHeight() > 0 && !model.isEmpty()) {
                    initialFitPending = false;
                    canvas.fitToView();
                }
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(final ComponentEvent e) {
                final String currentRoot = works.lysenko.util.prop.tree.Scenario.root;
                if (autoLoadFromPackage
                        && !Objects.equals(loadedRootPackage, currentRoot)
                        && model.modifiedCount() == 0) {
                    reloadFromSources();
                }
            }
        });

        if (autoLoadFromPackage) {
            reloadFromSources();
        } else {
            refreshAllViews(true);
        }
    }

    public TreeLayoutEditorModel getModel() {
        return model;
    }

    public TreeLayoutEditorCanvas getCanvas() {
        return canvas;
    }

    public boolean isTreeViewActive() {
        return treeViewBtn.isSelected();
    }

    public void setTreeViewActive(final boolean treeView) {
        if (treeView) {
            treeViewBtn.setSelected(true);
            navigatorCardLayout.show(navigatorCardPanel, CARD_TREE);
        } else {
            tableViewBtn.setSelected(true);
            navigatorCardLayout.show(navigatorCardPanel, CARD_TABLE);
        }
        syncSelectionToNavigator();
    }

    public AspectPreset getSelectedAspectPreset() {
        final Object item = aspectPresetCombo.getSelectedItem();
        return (item instanceof AspectPreset preset) ? preset : AspectPreset.WIDESCREEN_16_9;
    }

    public void setSelectedAspectPreset(final AspectPreset preset) {
        aspectPresetCombo.setSelectedItem(Objects.requireNonNull(preset, "preset"));
    }

    private JPanel buildTopToolbar() {
        final JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCC, 0xCC, 0xCC)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));

        final JButton fitBtn = compactButton("Fit", "Fit entire tree to canvas (F)");
        fitBtn.addActionListener(e -> canvas.fitToView());

        final JButton zoom100Btn = compactButton("100%", "Reset zoom to 100%");
        zoom100Btn.addActionListener(e -> canvas.resetZoom());

        final JButton focusBtn = compactButton("Focus", "Center canvas on selected node");
        focusBtn.addActionListener(e -> {
            if (null != selectedClassName) canvas.focusNode(selectedClassName, true);
        });

        gridCheck.setToolTipText("Show column/row grid lines and indices");
        gridCheck.addActionListener(e -> canvas.setShowGrid(gridCheck.isSelected()));

        ghostsCheck.setToolTipText("Show dashed ghost outlines for pre-compaction positions of shifted nodes");
        ghostsCheck.addActionListener(e -> canvas.setShowRawGhosts(ghostsCheck.isSelected()));

        compactedCheck.setToolTipText("Toggle between compacted layout (actual output) and raw initial offsets");
        compactedCheck.addActionListener(e -> {
            model.previewSettings().compacted = compactedCheck.isSelected();
            ghostsCheck.setEnabled(compactedCheck.isSelected());
            recomputeAndRepaint();
        });

        minSepSpinner.setPreferredSize(new Dimension(52, 24));
        minSepSpinner.setToolTipText("Preview .tree.min.offset.separation (does not alter test configuration)");
        minSepSpinner.addChangeListener(e -> {
            if (syncingUi) return;
            model.previewSettings().minOffsetSeparation = ((Number) minSepSpinner.getValue()).intValue();
            recomputeAndRepaint();
        });

        final JButton optionsBtn = compactButton("Options ▾", "Toggle preview compaction stages");
        final JPopupMenu optionsMenu = buildPreviewOptionsMenu();
        optionsBtn.addActionListener(e -> optionsMenu.show(optionsBtn, 0, optionsBtn.getHeight()));

        controls.add(fitBtn);
        controls.add(zoom100Btn);
        controls.add(focusBtn);
        controls.add(gridCheck);
        controls.add(compactedCheck);
        controls.add(ghostsCheck);
        controls.add(new JLabel("Min sep:"));
        controls.add(minSepSpinner);
        controls.add(optionsBtn);

        statusMetricsLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        statusMetricsLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));

        toolbar.add(controls, BorderLayout.WEST);
        toolbar.add(statusMetricsLabel, BorderLayout.EAST);
        return toolbar;
    }

    private JPopupMenu buildPreviewOptionsMenu() {
        final JPopupMenu menu = new JPopupMenu();
        final PreviewSettings ps = model.previewSettings();

        final JCheckBoxMenuItem hOff = new JCheckBoxMenuItem("Reduce Horizontal Offsets", ps.reduceHorizontalOffsets);
        hOff.addActionListener(e -> { ps.reduceHorizontalOffsets = hOff.isSelected(); recomputeAndRepaint(); });

        final JCheckBoxMenuItem vOff = new JCheckBoxMenuItem("Reduce Vertical Offsets", ps.reduceVerticalOffsets);
        vOff.addActionListener(e -> { ps.reduceVerticalOffsets = vOff.isSelected(); recomputeAndRepaint(); });

        final JCheckBoxMenuItem hGap = new JCheckBoxMenuItem("Reduce Horizontal Gaps", ps.reduceHorizontalGaps);
        hGap.addActionListener(e -> { ps.reduceHorizontalGaps = hGap.isSelected(); recomputeAndRepaint(); });

        final JCheckBoxMenuItem vGap = new JCheckBoxMenuItem("Reduce Vertical Gaps", ps.reduceVerticalGaps);
        vGap.addActionListener(e -> { ps.reduceVerticalGaps = vGap.isSelected(); recomputeAndRepaint(); });

        menu.add(hOff);
        menu.add(vOff);
        menu.addSeparator();
        menu.add(hGap);
        menu.add(vGap);
        return menu;
    }

    private JPanel buildLeftPane() {
        final JPanel left = new JPanel(new BorderLayout(0, 6));
        left.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 4));
        left.setMinimumSize(new Dimension(260, 200));

        // Top navigator controls (search + Tree/Table toggle + filter chips)
        final JPanel navHeader = new JPanel(new BorderLayout(4, 4));

        final JPanel searchRow = new JPanel(new BorderLayout(4, 0));
        final JPanel searchInputWrap = new JPanel(new BorderLayout(2, 0));
        searchField.setToolTipText("Filter scenarios by name or class");
        final JButton clearSearchBtn = compactButton("×", "Clear search");
        clearSearchBtn.addActionListener(e -> searchField.setText(""));
        searchInputWrap.add(searchField, BorderLayout.CENTER);
        searchInputWrap.add(clearSearchBtn, BorderLayout.EAST);

        final JPanel viewToggleWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        final ButtonGroup viewGroup = new ButtonGroup();
        styleToggle(treeViewBtn);
        styleToggle(tableViewBtn);
        viewGroup.add(treeViewBtn);
        viewGroup.add(tableViewBtn);
        treeViewBtn.addActionListener(e -> setTreeViewActive(true));
        tableViewBtn.addActionListener(e -> setTreeViewActive(false));
        viewToggleWrap.add(treeViewBtn);
        viewToggleWrap.add(tableViewBtn);

        searchRow.add(searchInputWrap, BorderLayout.CENTER);
        searchRow.add(viewToggleWrap, BorderLayout.EAST);

        final JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        final ButtonGroup filterGroup = new ButtonGroup();
        for (final JToggleButton btn : new JToggleButton[]{filterAllBtn, filterBranchesBtn, filterShiftedBtn, filterModifiedBtn}) {
            styleToggle(btn);
            filterGroup.add(btn);
            filterRow.add(btn);
        }
        filterAllBtn.addActionListener(e -> setFilterMode(FilterMode.ALL));
        filterBranchesBtn.addActionListener(e -> setFilterMode(FilterMode.BRANCHES));
        filterShiftedBtn.addActionListener(e -> setFilterMode(FilterMode.SHIFTED));
        filterModifiedBtn.addActionListener(e -> setFilterMode(FilterMode.MODIFIED));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(final DocumentEvent e) { rebuildNavigatorViews(); }
            @Override public void removeUpdate(final DocumentEvent e) { rebuildNavigatorViews(); }
            @Override public void changedUpdate(final DocumentEvent e) { rebuildNavigatorViews(); }
        });

        navHeader.add(searchRow, BorderLayout.NORTH);
        navHeader.add(filterRow, BorderLayout.SOUTH);

        // Navigator cards (Tree / Table)
        navigatorCardPanel.add(new JScrollPane(scenarioTree), CARD_TREE);
        navigatorCardPanel.add(new JScrollPane(scenarioTable), CARD_TABLE);
        navigatorCardLayout.show(navigatorCardPanel, CARD_TREE);

        left.add(navHeader, BorderLayout.NORTH);
        left.add(navigatorCardPanel, BorderLayout.CENTER);
        left.add(buildInspectorPanel(), BorderLayout.SOUTH);
        return left;
    }

    private JPanel buildInspectorPanel() {
        final JPanel inspector = new JPanel(new GridBagLayout());
        inspector.setBorder(BorderFactory.createTitledBorder("Selected Node"));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        inspectorNameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        inspectorMetaLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        inspectorMetaLabel.setForeground(new Color(0x64, 0x74, 0x8B));
        inspectorPosLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        inspector.add(inspectorNameLabel, gbc);

        gbc.gridy = 1;
        inspector.add(inspectorMetaLabel, gbc);

        gbc.gridy = 2;
        inspector.add(inspectorPosLabel, gbc);

        // @VisualOffset spinner row
        gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.0;
        inspector.add(new JLabel("Col Offset (@VisualOffset):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        inspector.add(colOffsetSpinner, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        resetColBtn.setMargin(new Insets(1, 6, 1, 6));
        resetColBtn.setToolTipText("Reset @VisualOffset to 0");
        inspector.add(resetColBtn, gbc);

        // @VisualRowOffset spinner row
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        inspector.add(new JLabel("Row Offset (@VisualRowOffset):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        inspector.add(rowOffsetSpinner, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        resetRowBtn.setMargin(new Insets(1, 6, 1, 6));
        resetRowBtn.setToolTipText("Reset @VisualRowOffset to 0");
        inspector.add(resetRowBtn, gbc);

        // Quick action row
        final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        revertNodeBtn.setMargin(new Insets(2, 8, 2, 8));
        focusNodeBtn.setMargin(new Insets(2, 8, 2, 8));
        actions.add(revertNodeBtn);
        actions.add(focusNodeBtn);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3; gbc.weightx = 1.0;
        inspector.add(actions, gbc);

        final JLabel hint = new JLabel("Canvas: Click select · Arrows ±1 · Shift+Arrows ±5 · 0 reset");
        hint.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 10));
        hint.setForeground(new Color(0x64, 0x74, 0x8B));
        gbc.gridy = 6;
        inspector.add(hint, gbc);

        return inspector;
    }

    private JPanel buildBottomBar() {
        final JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xCC, 0xCC, 0xCC)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        final JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        undoBtn.addActionListener(e -> performUndo());
        redoBtn.addActionListener(e -> performRedo());
        revertAllBtn.setToolTipText("Discard unsaved edits and restore offsets from sources");
        revertAllBtn.addActionListener(e -> performRevertAll());
        resetAllZeroBtn.setToolTipText("Set all @VisualOffset and @VisualRowOffset values to 0 (undoable)");
        resetAllZeroBtn.addActionListener(e -> performResetAllToZero());

        final JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(2, 22));

        aspectPresetCombo.setSelectedItem(AspectPreset.WIDESCREEN_16_9);
        aspectPresetCombo.setToolTipText("Target proportions for Auto-Layout");
        autoLayoutBtn.setToolTipText("Optimize @VisualOffset and @VisualRowOffset starting from current layout (undoable)");
        autoLayoutBtn.addActionListener(e -> performAutoLayoutOrCancel());

        left.add(undoBtn);
        left.add(redoBtn);
        left.add(revertAllBtn);
        left.add(resetAllZeroBtn);
        left.add(sep);
        left.add(aspectPresetCombo);
        left.add(autoLayoutBtn);

        final JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        final JButton reloadBtn = new JButton("Reload from Sources");
        reloadBtn.addActionListener(e -> confirmAndReload());

        final JButton browserBtn = new JButton("Open in Browser");
        browserBtn.addActionListener(e -> openInBrowser());

        saveBtn.setFont(saveBtn.getFont().deriveFont(Font.BOLD));
        saveBtn.addActionListener(e -> saveToSources());

        right.add(reloadBtn);
        right.add(browserBtn);
        right.add(saveBtn);

        bottom.add(left, BorderLayout.WEST);
        bottom.add(right, BorderLayout.EAST);
        return bottom;
    }

    private void configureTree() {
        scenarioTree.setRootVisible(false);
        scenarioTree.setShowsRootHandles(true);
        scenarioTree.setRowHeight(21);
        scenarioTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        scenarioTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(
                    final JTree tree, final Object value, final boolean sel,
                    final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof TreeItem item) {
                    final Offsets off = model.currentOffsets(item.className());
                    final boolean dirty = model.isModified(item.className());
                    final StringBuilder text = new StringBuilder();
                    if (dirty) text.append("● ");
                    text.append(item.simpleName());
                    if (!off.isZero()) {
                        text.append("  [").append(formatOffsetTag(off)).append("]");
                    }
                    setText(text.toString());
                    setFont(getFont().deriveFont(item.isLeaf() ? Font.PLAIN : Font.BOLD));
                    if (!sel) {
                        if (dirty) {
                            setForeground(new Color(0xB4, 0x53, 0x09));
                        } else if (!off.isZero()) {
                            setForeground(new Color(0x03, 0x69, 0xA1));
                        } else if (item.isLeaf()) {
                            setForeground(new Color(0x64, 0x74, 0x8B));
                        }
                    }
                }
                return this;
            }
        });

        scenarioTree.addTreeSelectionListener(e -> {
            if (syncingUi) return;
            final TreePath path = scenarioTree.getSelectionPath();
            if (null == path) return;
            final Object last = path.getLastPathComponent();
            if (last instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof TreeItem item) {
                selectScenario(item.className(), false);
            }
        });
    }

    private void configureTable() {
        scenarioTable.setRowHeight(22);
        scenarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scenarioTable.getTableHeader().setReorderingAllowed(false);
        scenarioTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        scenarioTable.setRowSorter(tableSorter);

        final Comparator<String> strCmp = Comparator.nullsFirst(Comparator.naturalOrder());
        tableSorter.setComparator(0, strCmp);
        tableSorter.setComparator(3, strCmp);

        scenarioTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        scenarioTable.getColumnModel().getColumn(1).setPreferredWidth(54);
        scenarioTable.getColumnModel().getColumn(2).setPreferredWidth(54);
        scenarioTable.getColumnModel().getColumn(3).setPreferredWidth(160);

        final DefaultTableCellRenderer highlightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    final JTable table, final Object value, final boolean isSelected,
                    final boolean hasFocus, final int row, final int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                final int modelRow = table.convertRowIndexToModel(row);
                final String className = (String) tableModel.getValueAt(modelRow, 3);
                final boolean dirty = null != className && model.isModified(className);
                final boolean shifted = null != className && model.isShifted(className);
                if (!isSelected) {
                    if (dirty) {
                        setForeground(new Color(0xB4, 0x53, 0x09));
                    } else if (shifted) {
                        setForeground(new Color(0x03, 0x69, 0xA1));
                    } else {
                        setForeground(table.getForeground());
                    }
                }
                setHorizontalAlignment((column == 1 || column == 2) ? RIGHT : LEFT);
                return this;
            }
        };
        for (int c = 0; c < 4; c++) {
            scenarioTable.getColumnModel().getColumn(c).setCellRenderer(highlightRenderer);
        }

        scenarioTable.getSelectionModel().addListSelectionListener(e -> {
            if (syncingUi || e.getValueIsAdjusting()) return;
            final int viewRow = scenarioTable.getSelectedRow();
            if (viewRow < 0) return;
            final int modelRow = scenarioTable.convertRowIndexToModel(viewRow);
            final String className = (String) tableModel.getValueAt(modelRow, 3);
            selectScenario(className, false);
        });

        tableModel.addTableModelListener(e -> {
            if (syncingUi || e.getType() != TableModelEvent.UPDATE) return;
            final int row = e.getFirstRow();
            final int col = e.getColumn();
            if (row < 0 || row >= tableModel.getRowCount() || (col != 1 && col != 2)) return;
            final String className = (String) tableModel.getValueAt(row, 3);
            final Object colVal = tableModel.getValueAt(row, 1);
            final Object rowVal = tableModel.getValueAt(row, 2);
            final int newCol = (colVal instanceof Number n) ? Math.max(0, n.intValue()) : 0;
            final int newRow = (rowVal instanceof Number n) ? n.intValue() : 0;
            if (model.setOffsets(className, newCol, newRow)) {
                refreshAfterOffsetMutation();
            } else {
                // Clamp negative colOffset display back to 0 if user typed negative value
                syncTableRowForClass(className);
            }
        });
    }

    private void configureCanvasCallbacks() {
        canvas.setOnSelectionChanged(className -> selectScenario(className, false));
        canvas.setOnNudge((deltaCol, deltaRow) -> {
            if (null != selectedClassName && model.nudgeOffsets(selectedClassName, deltaCol, deltaRow)) {
                refreshAfterOffsetMutation();
            }
        });
        canvas.setOnResetSelected(() -> {
            if (null != selectedClassName && model.setOffsets(selectedClassName, 0, 0)) {
                refreshAfterOffsetMutation();
            }
        });
    }

    private void configureInspector() {
        colOffsetSpinner.addChangeListener(e -> {
            if (syncingUi || null == selectedClassName) return;
            final int nextCol = ((Number) colOffsetSpinner.getValue()).intValue();
            final int curRow = model.currentOffsets(selectedClassName).rowOffset();
            if (model.setOffsets(selectedClassName, nextCol, curRow)) {
                refreshAfterOffsetMutation();
            }
        });
        rowOffsetSpinner.addChangeListener(e -> {
            if (syncingUi || null == selectedClassName) return;
            final int curCol = model.currentOffsets(selectedClassName).colOffset();
            final int nextRow = ((Number) rowOffsetSpinner.getValue()).intValue();
            if (model.setOffsets(selectedClassName, curCol, nextRow)) {
                refreshAfterOffsetMutation();
            }
        });
        resetColBtn.addActionListener(e -> {
            if (null != selectedClassName) {
                final int curRow = model.currentOffsets(selectedClassName).rowOffset();
                if (model.setOffsets(selectedClassName, 0, curRow)) {
                    refreshAfterOffsetMutation();
                }
            }
        });
        resetRowBtn.addActionListener(e -> {
            if (null != selectedClassName) {
                final int curCol = model.currentOffsets(selectedClassName).colOffset();
                if (model.setOffsets(selectedClassName, curCol, 0)) {
                    refreshAfterOffsetMutation();
                }
            }
        });
        revertNodeBtn.addActionListener(e -> {
            if (null != selectedClassName && model.revertNode(selectedClassName)) {
                refreshAfterOffsetMutation();
            }
        });
        focusNodeBtn.addActionListener(e -> {
            if (null != selectedClassName) {
                canvas.focusNode(selectedClassName, true);
            }
        });
    }

    private void installKeyBindings() {
        final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask), "tree-editor-undo");
        getActionMap().put("tree-editor-undo", new AbstractAction() {
            @Override public void actionPerformed(final ActionEvent e) { performUndo(); }
        });
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask | InputEvent.SHIFT_DOWN_MASK), "tree-editor-redo");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuMask), "tree-editor-redo");
        getActionMap().put("tree-editor-redo", new AbstractAction() {
            @Override public void actionPerformed(final ActionEvent e) { performRedo(); }
        });
    }

    private void setFilterMode(final FilterMode mode) {
        this.filterMode = mode;
        rebuildNavigatorViews();
    }

    public void reloadFromSources() {
        if (isSolverRunning()) {
            solverCancelled.set(true);
            activeSolver.cancel(false);
        }
        final String rootPackage = works.lysenko.util.prop.tree.Scenario.root;
        loadedRootPackage = rootPackage;
        final Path baseDir = Paths.get(System.getProperty("user.dir"));
        model.loadFromPackage(rootPackage, baseDir);
        selectedClassName = null;
        initialFitPending = true;
        refreshAllViews(true);
    }

    private void confirmAndReload() {
        if (model.modifiedCount() > 0) {
            final int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Discard " + model.modifiedCount() + " unsaved offset change(s) and reload from sources?",
                    "Confirm Reload",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        reloadFromSources();
    }

    private void performUndo() {
        if (isSolverRunning()) return;
        if (model.undo()) {
            refreshAfterOffsetMutation();
        }
    }

    private void performRedo() {
        if (isSolverRunning()) return;
        if (model.redo()) {
            refreshAfterOffsetMutation();
        }
    }

    private void performRevertAll() {
        if (isSolverRunning()) return;
        if (model.modifiedCount() == 0) return;
        if (model.revertAll()) {
            refreshAfterOffsetMutation();
        }
    }

    private void performResetAllToZero() {
        if (isSolverRunning()) return;
        stopTableEditing();
        if (model.shiftedCount() == 0) return;
        if (model.resetAllToZero()) {
            refreshAfterOffsetMutation();
        }
    }

    private boolean isSolverRunning() {
        return null != activeSolver && !activeSolver.isDone();
    }

    private void performAutoLayoutOrCancel() {
        if (isSolverRunning()) {
            solverCancelled.set(true);
            activeSolver.cancel(false);
            autoLayoutBtn.setEnabled(false);
            statusMetricsLabel.setText("Cancelling auto-layout…");
            return;
        }
        stopTableEditing();
        if (model.isEmpty()) return;

        final AspectPreset preset = getSelectedAspectPreset();
        final List<ScenarioBlueprint> rootsSnapshot = List.copyOf(model.roots());
        final Map<String, Offsets> startOffsets = model.snapshotCurrentOffsets();
        final PreviewSettings settingsSnapshot = model.previewSettings().copy();

        solverCancelled.set(false);
        setSolverUiState(true);
        statusMetricsLabel.setForeground(new Color(0x03, 0x69, 0xA1));
        statusMetricsLabel.setText("Optimizing (" + preset.label() + ")…");

        activeSolver = new SwingWorker<>() {
            @Override
            protected SolveResult doInBackground() {
                return TreeLayoutAutoSolver.solve(
                        rootsSnapshot,
                        startOffsets,
                        settingsSnapshot,
                        preset,
                        solverCancelled::get,
                        this::publish);
            }

            @Override
            protected void process(final List<SolveProgress> chunks) {
                if (chunks.isEmpty() || solverCancelled.get()) return;
                final SolveProgress p = chunks.get(chunks.size() - 1);
                statusMetricsLabel.setText(String.format(Locale.ROOT,
                        "Optimizing (%s)… %s · %d×%d (%.2f:1)",
                        preset.label(),
                        p.stage(),
                        p.bestMetrics().cols(),
                        (int) Math.ceil(p.bestMetrics().rows()),
                        p.bestMetrics().aspectRatio()));
            }

            @Override
            protected void done() {
                activeSolver = null;
                setSolverUiState(false);
                try {
                    final SolveResult result = get();
                    if (result.cancelled()) {
                        updateStatusAndButtons();
                        return;
                    }
                    if (result.improved() && model.applyOffsetsBatch(result.offsets())) {
                        refreshAfterOffsetMutation();
                        canvas.fitToView();
                        statusMetricsLabel.setText(statusMetricsLabel.getText()
                                + " · optimized (" + preset.label() + ")");
                    } else {
                        updateStatusAndButtons();
                        statusMetricsLabel.setText("Already optimal for " + preset.label()
                                + " · " + statusMetricsLabel.getText());
                    }
                } catch (final Exception ex) {
                    updateStatusAndButtons();
                }
            }
        };
        activeSolver.execute();
    }

    private void setSolverUiState(final boolean running) {
        autoLayoutBtn.setText(running ? "Cancel…" : "Auto-Layout");
        autoLayoutBtn.setEnabled(running || !model.isEmpty());
        aspectPresetCombo.setEnabled(!running && !model.isEmpty());
        if (running) {
            undoBtn.setEnabled(false);
            redoBtn.setEnabled(false);
            revertAllBtn.setEnabled(false);
            resetAllZeroBtn.setEnabled(false);
            saveBtn.setEnabled(false);
            colOffsetSpinner.setEnabled(false);
            rowOffsetSpinner.setEnabled(false);
            resetColBtn.setEnabled(false);
            resetRowBtn.setEnabled(false);
            revertNodeBtn.setEnabled(false);
        } else {
            updateInspector();
            updateStatusAndButtons();
        }
    }

    private void selectScenario(final String className, final boolean centerCanvas) {
        selectedClassName = className;
        canvas.setSelectedClassName(className);
        syncSelectionToNavigator();
        updateInspector();
        if (centerCanvas && null != className) {
            canvas.focusNode(className, false);
        }
    }

    private void recomputeAndRepaint() {
        model.recomputePreview();
        updateInspector();
        updateStatusAndButtons();
        canvas.repaint();
    }

    private void refreshAfterOffsetMutation() {
        if (filterMode == FilterMode.SHIFTED || filterMode == FilterMode.MODIFIED) {
            rebuildNavigatorViews();
        } else {
            syncTableAllRows();
            scenarioTree.repaint();
        }
        updateInspector();
        updateStatusAndButtons();
        canvas.repaint();
    }

    private void refreshAllViews(final boolean fitCanvas) {
        rebuildNavigatorViews();
        canvas.setSelectedClassName(selectedClassName);
        updateInspector();
        updateStatusAndButtons();
        if (fitCanvas && canvas.getWidth() > 0 && canvas.getHeight() > 0) {
            initialFitPending = false;
            canvas.fitToView();
        } else {
            canvas.repaint();
        }
    }

    private void rebuildNavigatorViews() {
        syncingUi = true;
        try {
            // 1. Rebuild JTree keeping ancestor paths for matching nodes
            treeRoot.removeAllChildren();
            treeNodeByClassName.clear();
            final String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
            for (final ScenarioBlueprint rootBp : model.roots()) {
                final DefaultMutableTreeNode built = buildFilteredTreeNode(rootBp, query);
                if (null != built) {
                    treeRoot.add(built);
                }
            }
            treeModel.reload();
            expandTreeAppropriately(!query.isEmpty() || filterMode != FilterMode.ALL);

            // 2. Rebuild JTable rows
            tableModel.setRowCount(0);
            for (final ScenarioBlueprint bp : model.blueprintsByClassName().values()) {
                if (matchesFilter(bp, query)) {
                    final Offsets off = model.currentOffsets(bp.className());
                    tableModel.addRow(new Object[]{bp.simpleName(), off.colOffset(), off.rowOffset(), bp.className()});
                }
            }
        } finally {
            syncingUi = false;
        }
        syncSelectionToNavigator();
        updateStatusAndButtons();
    }

    private DefaultMutableTreeNode buildFilteredTreeNode(final ScenarioBlueprint bp, final String query) {
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                new TreeItem(bp.className(), bp.simpleName(), bp.isLeaf()));
        boolean anyChildKept = false;
        for (final ScenarioBlueprint child : bp.children()) {
            final DefaultMutableTreeNode childNode = buildFilteredTreeNode(child, query);
            if (null != childNode) {
                node.add(childNode);
                anyChildKept = true;
            }
        }
        if (matchesFilter(bp, query) || anyChildKept) {
            treeNodeByClassName.putIfAbsent(bp.className(), node);
            return node;
        }
        return null;
    }

    private boolean matchesFilter(final ScenarioBlueprint bp, final String query) {
        if (!query.isEmpty()) {
            final boolean nameMatch = bp.simpleName().toLowerCase(Locale.ROOT).contains(query)
                    || bp.className().toLowerCase(Locale.ROOT).contains(query);
            if (!nameMatch) return false;
        }
        return switch (filterMode) {
            case ALL -> true;
            case BRANCHES -> !bp.isLeaf();
            case SHIFTED -> model.isShifted(bp.className());
            case MODIFIED -> model.isModified(bp.className());
        };
    }

    private void expandTreeAppropriately(final boolean expandAll) {
        if (expandAll) {
            for (int i = 0; i < scenarioTree.getRowCount(); i++) {
                scenarioTree.expandRow(i);
            }
        } else {
            // Expand top 2 levels by default so major branches are immediately visible
            for (int i = 0; i < treeRoot.getChildCount(); i++) {
                final DefaultMutableTreeNode rootChild = (DefaultMutableTreeNode) treeRoot.getChildAt(i);
                scenarioTree.expandPath(new TreePath(rootChild.getPath()));
                for (int j = 0; j < rootChild.getChildCount(); j++) {
                    final DefaultMutableTreeNode grandChild = (DefaultMutableTreeNode) rootChild.getChildAt(j);
                    scenarioTree.expandPath(new TreePath(grandChild.getPath()));
                }
            }
        }
    }

    private void syncSelectionToNavigator() {
        syncingUi = true;
        try {
            if (null == selectedClassName) {
                scenarioTree.clearSelection();
                scenarioTable.clearSelection();
                return;
            }
            final DefaultMutableTreeNode treeNode = treeNodeByClassName.get(selectedClassName);
            if (null != treeNode) {
                final TreePath path = new TreePath(treeNode.getPath());
                scenarioTree.setSelectionPath(path);
                scenarioTree.scrollPathToVisible(path);
            } else {
                scenarioTree.clearSelection();
            }

            boolean foundInTable = false;
            for (int mRow = 0; mRow < tableModel.getRowCount(); mRow++) {
                if (selectedClassName.equals(tableModel.getValueAt(mRow, 3))) {
                    final int vRow = scenarioTable.convertRowIndexToView(mRow);
                    if (vRow >= 0) {
                        scenarioTable.setRowSelectionInterval(vRow, vRow);
                        scenarioTable.scrollRectToVisible(scenarioTable.getCellRect(vRow, 0, true));
                        foundInTable = true;
                    }
                    break;
                }
            }
            if (!foundInTable) {
                scenarioTable.clearSelection();
            }
        } finally {
            syncingUi = false;
        }
    }

    private void syncTableAllRows() {
        syncingUi = true;
        try {
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                final String cls = (String) tableModel.getValueAt(r, 3);
                final Offsets off = model.currentOffsets(cls);
                tableModel.setValueAt(off.colOffset(), r, 1);
                tableModel.setValueAt(off.rowOffset(), r, 2);
            }
        } finally {
            syncingUi = false;
        }
    }

    private void syncTableRowForClass(final String className) {
        syncingUi = true;
        try {
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                if (className.equals(tableModel.getValueAt(r, 3))) {
                    final Offsets off = model.currentOffsets(className);
                    tableModel.setValueAt(off.colOffset(), r, 1);
                    tableModel.setValueAt(off.rowOffset(), r, 2);
                    break;
                }
            }
        } finally {
            syncingUi = false;
        }
    }

    private void updateInspector() {
        syncingUi = true;
        try {
            final ScenarioBlueprint bp = (null != selectedClassName) ? model.blueprint(selectedClassName) : null;
            final boolean hasSel = null != bp;

            colOffsetSpinner.setEnabled(hasSel);
            rowOffsetSpinner.setEnabled(hasSel);
            resetColBtn.setEnabled(hasSel);
            resetRowBtn.setEnabled(hasSel);
            focusNodeBtn.setEnabled(hasSel);

            if (!hasSel) {
                inspectorNameLabel.setText("No node selected");
                inspectorMetaLabel.setText("Click any node on the canvas or navigator");
                inspectorPosLabel.setText(" ");
                colOffsetSpinner.setValue(0);
                rowOffsetSpinner.setValue(0);
                revertNodeBtn.setEnabled(false);
                return;
            }

            final Offsets cur = model.currentOffsets(selectedClassName);
            final boolean dirty = model.isModified(selectedClassName);
            inspectorNameLabel.setText((dirty ? "● " : "") + bp.simpleName());

            final String parentCls = model.parentClassName(selectedClassName);
            final ScenarioBlueprint parentBp = (null != parentCls) ? model.blueprint(parentCls) : null;
            final String parentLabel = (null != parentBp) ? parentBp.simpleName() : "(root)";
            inspectorMetaLabel.setText(String.format("Parent: %s · Subtree: %d node%s",
                    parentLabel, bp.subtreeSize(), bp.subtreeSize() == 1 ? "" : "s"));

            final PreviewResult preview = model.latestPreview();
            final List<NodeData> nodes = preview.nodesByClassName().get(selectedClassName);
            if (null != nodes && !nodes.isEmpty()) {
                final NodeData n = nodes.get(0);
                final Point2DGrid raw = preview.rawPositionByNodeId().get(n.id());
                final StringBuilder pos = new StringBuilder(
                        String.format("Grid: C%d, R%.2f", n.col(), n.row()));
                if (model.previewSettings().compacted && null != raw
                        && (raw.col() != n.col() || Math.abs(raw.row() - n.row()) > 0.05)) {
                    pos.append(String.format("  (raw: C%d, R%.2f)", raw.col(), raw.row()));
                }
                inspectorPosLabel.setText(pos.toString());
            } else {
                inspectorPosLabel.setText(" ");
            }

            colOffsetSpinner.setValue(cur.colOffset());
            rowOffsetSpinner.setValue(cur.rowOffset());
            revertNodeBtn.setEnabled(dirty);
        } finally {
            syncingUi = false;
        }
    }

    private void updateStatusAndButtons() {
        final PreviewResult preview = model.latestPreview();
        final int shifted = model.shiftedCount();
        final int modified = model.modifiedCount();

        filterShiftedBtn.setText(shifted > 0 ? ("Shifted (" + shifted + ")") : "Shifted");
        filterModifiedBtn.setText(modified > 0 ? ("Modified (" + modified + ")") : "Modified");

        if (preview.layout().nodes().isEmpty()) {
            statusMetricsLabel.setText("No scenarios loaded");
        } else {
            final double ratio = TreeLayoutAutoSolver.aspectRatio(preview);
            final StringBuilder sb = new StringBuilder();
            sb.append(preview.totalCols()).append(" cols × ")
                    .append((int) Math.ceil(preview.totalRows())).append(" rows")
                    .append(String.format(Locale.ROOT, " (%.2f:1)", ratio))
                    .append(" · ").append(shifted).append(" shifted");
            if (modified > 0) {
                sb.append(" · ").append(modified).append(" unsaved");
            }
            if (preview.overlapCount() > 0) {
                sb.append(" · ").append(preview.overlapCount()).append(" overlaps!");
                statusMetricsLabel.setForeground(new Color(0xDC, 0x26, 0x26));
            } else {
                statusMetricsLabel.setForeground(new Color(0x47, 0x55, 0x69));
            }
            statusMetricsLabel.setText(sb.toString());
        }

        final boolean solving = isSolverRunning();
        undoBtn.setEnabled(!solving && model.canUndo());
        redoBtn.setEnabled(!solving && model.canRedo());
        revertAllBtn.setEnabled(!solving && modified > 0);
        resetAllZeroBtn.setEnabled(!solving && shifted > 0);
        aspectPresetCombo.setEnabled(!solving && !model.isEmpty());
        autoLayoutBtn.setEnabled(solving || !model.isEmpty());
        saveBtn.setEnabled(!solving && modified > 0);
        saveBtn.setText(modified > 0 ? ("Save (" + modified + ") to Sources") : "Save to Sources");
    }

    private void openInBrowser() {
        stopTableEditing();
        final PreviewResult preview = model.latestPreview();
        if (preview.layout().nodes().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No scenarios to preview.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            final String html = TreeHtml.renderHtml(preview.layout());
            final File tempHtml = File.createTempFile("tree-preview-", ".html");
            works.lysenko.util.func.type.Files.writeToFile(html, tempHtml.getAbsolutePath());
            Desktop.getDesktop().browse(tempHtml.toURI());
        } catch (final Exception ex) {
            JOptionPane.showMessageDialog(this, "Error generating browser preview: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveToSources() {
        stopTableEditing();
        final List<OffsetChange> changes = model.modifiedChanges();
        if (changes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No changes detected.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final StringBuilder diff = new StringBuilder();
        for (final OffsetChange c : changes) {
            diff.append(String.format("%-32s  (%+d, %+d)  →  (%+d, %+d)%n",
                    c.simpleName(),
                    c.initial().colOffset(), c.initial().rowOffset(),
                    c.current().colOffset(), c.current().rowOffset()));
        }

        final JTextArea area = new JTextArea(diff.toString(), Math.min(14, changes.size() + 1), 54);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        final int confirm = JOptionPane.showConfirmDialog(
                this,
                new Object[]{"Apply offset changes to " + changes.size() + " Java source file(s)?", new JScrollPane(area)},
                "Confirm Save to Sources",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        final Path baseDir = Paths.get(System.getProperty("user.dir"));
        int savedCount = 0;
        final StringBuilder errors = new StringBuilder();
        for (final OffsetChange change : changes) {
            final Path sourcePath = ScenarioOffsetSourceCodec.resolveSourcePath(baseDir, change.className());
            try {
                ScenarioOffsetSourceCodec.writeOffsets(sourcePath, change.current());
                model.markSaved(change.className());
                savedCount++;
            } catch (final Exception ex) {
                errors.append(change.className()).append(": ").append(ex.getMessage()).append('\n');
            }
        }

        refreshAfterOffsetMutation();

        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Saved " + savedCount + " file(s), with errors:\n" + errors,
                    "Partial Save", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Successfully updated " + savedCount + " source file(s).\nRecompile the test project for annotations to take effect.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void stopTableEditing() {
        if (scenarioTable.isEditing() && null != scenarioTable.getCellEditor()) {
            scenarioTable.getCellEditor().stopCellEditing();
        }
    }

    private static JButton compactButton(final String label, final String tooltip) {
        final JButton btn = new JButton(label);
        btn.setMargin(new Insets(2, 8, 2, 8));
        btn.setToolTipText(tooltip);
        btn.setFocusable(false);
        return btn;
    }

    private static void styleToggle(final JToggleButton btn) {
        btn.setMargin(new Insets(2, 7, 2, 7));
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        btn.setFocusable(false);
    }

    private static String formatOffsetTag(final Offsets o) {
        if (o.colOffset() != 0 && o.rowOffset() != 0) {
            return String.format("c+%d, r%+d", o.colOffset(), o.rowOffset());
        }
        if (o.colOffset() != 0) {
            return String.format("c+%d", o.colOffset());
        }
        return String.format("r%+d", o.rowOffset());
    }

    private record TreeItem(String className, String simpleName, boolean isLeaf) {
        @Override
        public String toString() {
            return simpleName;
        }
    }
}
