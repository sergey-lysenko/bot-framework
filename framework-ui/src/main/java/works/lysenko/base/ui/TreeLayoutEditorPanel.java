package works.lysenko.base.ui;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.ui.tree.ScenarioOffsetSourceCodec;
import works.lysenko.base.ui.tree.TreeLayoutAutoSolver;
import works.lysenko.base.ui.tree.TreeLayoutEditorCanvas;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel;
import works.lysenko.util.prop.tree.VisualOffset;
import works.lysenko.util.prop.tree.VisualRowOffset;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Brackets.*;
import static works.lysenko.util.data.strs.Bind.*;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.*;
import static works.lysenko.util.data.strs.Swap.*;
import static works.lysenko.util.data.strs.Wrap.*;
import static works.lysenko.util.func.type.Files.writeToFile;
import static works.lysenko.util.lang.A.*;
import static works.lysenko.util.lang.C.*;
import static works.lysenko.util.lang.D.*;
import static works.lysenko.util.lang.F.*;
import static works.lysenko.util.lang.N.*;
import static works.lysenko.util.lang.P.*;
import static works.lysenko.util.lang.S.*;
import static works.lysenko.util.lang.T.*;
import static works.lysenko.util.lang.word.A.*;
import static works.lysenko.util.lang.word.B.*;
import static works.lysenko.util.lang.word.C.*;
import static works.lysenko.util.lang.word.D.*;
import static works.lysenko.util.lang.word.E.*;
import static works.lysenko.util.lang.word.F.*;
import static works.lysenko.util.lang.word.G.*;
import static works.lysenko.util.lang.word.H.*;
import static works.lysenko.util.lang.word.I.*;
import static works.lysenko.util.lang.word.J.*;
import static works.lysenko.util.lang.word.L.*;
import static works.lysenko.util.lang.word.M.*;
import static works.lysenko.util.lang.word.N.*;
import static works.lysenko.util.lang.word.O.*;
import static works.lysenko.util.lang.word.P.*;
import static works.lysenko.util.lang.word.R.*;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.lang.word.T.*;
import static works.lysenko.util.lang.word.T.TREE;
import static works.lysenko.util.lang.word.U.*;
import static works.lysenko.util.lang.word.V.*;
import static works.lysenko.util.prop.PropEnum.*;
import static works.lysenko.util.prop.tree.Scenario.root;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Interactive visual editor for scenario {@code @VisualOffset} and {@code @VisualRowOffset}
 * annotations, featuring a live pan/zoom tree canvas, a toggleable hierarchy/table navigator,
 * node inspector with spinners & keyboard nudging, undo/redo, and safe source saving.
 */
public final class TreeLayoutEditorPanel extends JPanel {

    private static final String AT_VISUAL_OFFSET = s(_AT_SGN, VisualOffset.class.getSimpleName());
    private static final String AT_VISUAL_ROW_OFFSET = s(_AT_SGN, VisualRowOffset.class.getSimpleName());

    private static final Color COLOR_BORDER = new Color(0xCC, 0xCC, 0xCC);
    private static final Color COLOR_MUTED = new Color(0x64, 0x74, 0x8B);
    private static final Color COLOR_DIRTY = new Color(0xB4, 0x53, 0x09);
    private static final Color COLOR_SHIFTED = new Color(0x03, 0x69, 0xA1);
    private static final Color COLOR_ERROR = new Color(0xDC, 0x26, 0x26);
    private static final Color COLOR_STATUS = new Color(0x47, 0x55, 0x69);

    private static final Font FONT_MONO_10 = new Font(Font.MONOSPACED, Font.PLAIN, 10);
    private static final Font FONT_MONO_11 = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    private static final Font FONT_MONO_12 = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font FONT_SANS_11 = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    private static final Font FONT_SANS_ITALIC_10 = new Font(Font.SANS_SERIF, Font.ITALIC, 10);
    private static final Font FONT_SANS_BOLD_13 = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    private static final int SPLIT_DIVIDER_LOCATION = 340;
    private static final int MIN_SEP_SPINNER_WIDTH = 52;
    private static final int MIN_SEP_SPINNER_HEIGHT = 24;
    private static final int MIN_SEP_MAX = 10;
    private static final int LEFT_PANE_MIN_WIDTH = 260;
    private static final int LEFT_PANE_MIN_HEIGHT = 200;
    private static final int SEPARATOR_HEIGHT = 22;
    private static final int TREE_ROW_HEIGHT = 21;
    private static final int TABLE_ROW_HEIGHT = 22;
    private static final int COL_WIDTH_SCENARIO = 140;
    private static final int COL_WIDTH_OFFSET = 54;
    private static final int COL_WIDTH_CLASS = 160;
    private static final int COL_SCENARIO = 0;
    private static final int COL_OFFSET = 1;
    private static final int COL_ROW_OFFSET = 2;
    private static final int COL_CLASS = 3;
    private static final int TABLE_COLUMN_COUNT = 4;
    private static final int SPINNER_MAX_OFFSET = 99;
    private static final int SPINNER_MIN_ROW_OFFSET = -99;
    private static final int SEARCH_FIELD_COLUMNS = 12;
    private static final int DIFF_AREA_MAX_ROWS = 14;
    private static final int DIFF_AREA_COLS = 54;
    private static final int INITIAL_MAP_CAPACITY = 64;
    private static final int INITIAL_SB_CAPACITY = 64;
    private static final int INITIAL_DIFF_CAPACITY = 256;
    private static final double ROW_DELTA_EPSILON = 0.05;

    private enum FilterMode {
        ALL, BRANCHES, SHIFTED, MODIFIED
    }

    private enum KeyCommand {
        UNDO, REDO
    }

    private final TreeLayoutEditorModel model;
    private final TreeLayoutEditorCanvas canvas;
    private final JLabel statusMetricsLabel = new JLabel(NO_SCENARIOS_LOADED);
    private final NavigatorHeaderControls navHeader = new NavigatorHeaderControls();
    private final TreeNavigatorControls treeNav = new TreeNavigatorControls();
    private final TableNavigatorControls tableNav = new TableNavigatorControls();
    private final InspectorControls inspector = new InspectorControls();
    private final BottomBarControls bottomBar = new BottomBarControls();
    private final EditorState state = new EditorState();

    /**
     * Creates a new tree layout editor panel and automatically loads scenarios from the configured root package.
     */
    public TreeLayoutEditorPanel() {

        this(new TreeLayoutEditorModel(), true);
    }

    TreeLayoutEditorPanel(final TreeLayoutEditorModel model, final boolean autoLoadFromPackage) {

        this.model = Objects.requireNonNull(model, MODEL);
        canvas = new TreeLayoutEditorCanvas(this.model);

        setLayout(new BorderLayout(0, 0));

        tableNav.configure(new ScenarioTableCellRenderer(), this::onTableSelectionChanged, this::onTableModelUpdated);
        treeNav.configure(new ScenarioTreeCellRenderer(), this::onTreeSelectionChanged);
        configureCanvasCallbacks();
        inspector.configure(
                this::onColOffsetSpinnerChanged,
                this::onRowOffsetSpinnerChanged,
                this::resetSelectedColOffset,
                this::resetSelectedRowOffset,
                this::revertSelectedNode,
                this::focusSelectedNode);
        installKeyBindings();

        add(buildTopToolbar(), BorderLayout.PAGE_START);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPane(), canvas);
        splitPane.setDividerLocation(SPLIT_DIVIDER_LOCATION);
        splitPane.setResizeWeight(0.0);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);

        add(buildBottomBar(), BorderLayout.PAGE_END);

        canvas.addComponentListener(new CanvasResizeListener());
        addComponentListener(new PanelShownListener(autoLoadFromPackage));

        if (autoLoadFromPackage) {
            reloadFromSources();
        } else {
            refreshAllViews();
        }
    }

    /**
     * Returns the underlying editor model managing scenario blueprints and offset edits.
     *
     * @return the editor model
     */
    public TreeLayoutEditorModel getModel() {

        return model;
    }

    TreeLayoutEditorCanvas getCanvas() {

        return canvas;
    }

    boolean isTreeViewActive() {

        return navHeader.isTreeViewSelected();
    }

    void setTreeViewActive(final boolean treeView) {

        navHeader.showView(treeView);
        syncSelectionToNavigator();
    }

    TreeLayoutAutoSolver.AspectPreset getSelectedAspectPreset() {

        return bottomBar.selectedAspectPreset();
    }

    void setSelectedAspectPreset(final TreeLayoutAutoSolver.AspectPreset preset) {

        bottomBar.selectAspectPreset(preset);
    }

    private JPanel buildTopToolbar() {

        final JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 2));

        final JButton fitBtn = compactButton(c(FIT), b(c(FIT), ENTIRE, TREE, TO, CANVAS, e(ROUND, u(F))));
        fitBtn.addActionListener(_ -> canvas.fitToView());

        final JButton zoom100Btn = compactButton(PCT_100, b(c(RESET), ZOOM, TO, PCT_100));
        zoom100Btn.addActionListener(_ -> canvas.resetZoom());

        final JButton focusBtn = compactButton(c(FOCUS), b(c(CENTER), CANVAS, ON, SELECTED, NODE));
        focusBtn.addActionListener(_ -> focusSelectedNode());

        final JCheckBox gridCheck = new JCheckBox(c(GRID), true);
        gridCheck.setToolTipText(b(c(SHOW), s(COLUMN, _SLASH_, ROW), GRID, LINES, AND, INDICES));
        gridCheck.addActionListener(_ -> canvas.setShowGrid(gridCheck.isSelected()));

        final JCheckBox ghostsCheck = new JCheckBox(b(c(RAW), c(GHOSTS)), false);
        ghostsCheck.setToolTipText(b(c(SHOW), DASHED, GHOST, OUTLINES, FOR, d(PRE, COMPACTION), POSITIONS, OF, SHIFTED, NODES));
        ghostsCheck.addActionListener(_ -> canvas.setShowRawGhosts(ghostsCheck.isSelected()));

        final JCheckBox compactedCheck = new JCheckBox(c(COMPACTED), true);
        compactedCheck.setToolTipText(b(c(TOGGLE), BETWEEN, COMPACTED, LAYOUT, e(ROUND, b(ACTUAL, OUTPUT)), AND, RAW, INITIAL, OFFSETS));
        compactedCheck.addActionListener(_ -> onCompactedToggled(compactedCheck.isSelected(), ghostsCheck));

        final JSpinner minSepSpinner = new JSpinner(new SpinnerNumberModel(
                Math.max(0, model.previewSettings().minOffsetSeparation), 0, MIN_SEP_MAX, 1));
        minSepSpinner.setPreferredSize(new Dimension(MIN_SEP_SPINNER_WIDTH, MIN_SEP_SPINNER_HEIGHT));
        minSepSpinner.setToolTipText(b(c(PREVIEW), _TREE_MIN_OFFSET_SEPARATION.getPropertyName(),
                e(ROUND, b(DOES, NOT, ALTER, TEST, CONFIGURATION))));
        minSepSpinner.addChangeListener(_ -> onMinSepChanged(minSepSpinner));

        final JButton optionsBtn = compactButton(b(c(OPTIONS), s(DWN_TRI)), b(c(TOGGLE), PREVIEW, COMPACTION, STAGES));
        final JPopupMenu optionsMenu = buildPreviewOptionsMenu();
        optionsBtn.addActionListener(_ -> optionsMenu.show(optionsBtn, 0, optionsBtn.getHeight()));

        controls.add(fitBtn);
        controls.add(zoom100Btn);
        controls.add(focusBtn);
        controls.add(gridCheck);
        controls.add(compactedCheck);
        controls.add(ghostsCheck);
        controls.add(new JLabel(b(c(MIN), s(SEP, _COLON_))));
        controls.add(minSepSpinner);
        controls.add(optionsBtn);

        statusMetricsLabel.setFont(FONT_MONO_11);
        statusMetricsLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));

        toolbar.add(controls, BorderLayout.LINE_START);
        toolbar.add(statusMetricsLabel, BorderLayout.LINE_END);
        return toolbar;
    }

    private void onCompactedToggled(final boolean compacted, final JCheckBox ghostsCheck) {

        model.previewSettings().compacted = compacted;
        ghostsCheck.setEnabled(compacted);
        recomputeAndRepaint();
    }

    private void onMinSepChanged(final JSpinner minSepSpinner) {

        if (state.isInteractive()) {
            model.previewSettings().minOffsetSeparation = ((Number) minSepSpinner.getValue()).intValue();
            recomputeAndRepaint();
        }
    }

    private JPopupMenu buildPreviewOptionsMenu() {

        final JPopupMenu menu = new JPopupMenu();
        final TreeLayoutEditorModel.PreviewSettings ps = model.previewSettings();

        final JCheckBoxMenuItem hOff = new JCheckBoxMenuItem(b(c(REDUCE), c(HORIZONTAL), c(OFFSETS)), ps.reduceHorizontalOffsets);
        hOff.addActionListener(_ -> {
            ps.reduceHorizontalOffsets = hOff.isSelected();
            recomputeAndRepaint();
        });

        final JCheckBoxMenuItem vOff = new JCheckBoxMenuItem(b(c(REDUCE), c(VERTICAL), c(OFFSETS)), ps.reduceVerticalOffsets);
        vOff.addActionListener(_ -> {
            ps.reduceVerticalOffsets = vOff.isSelected();
            recomputeAndRepaint();
        });

        final JCheckBoxMenuItem hGap = new JCheckBoxMenuItem(b(c(REDUCE), c(HORIZONTAL), c(GAPS)), ps.reduceHorizontalGaps);
        hGap.addActionListener(_ -> {
            ps.reduceHorizontalGaps = hGap.isSelected();
            recomputeAndRepaint();
        });

        final JCheckBoxMenuItem vGap = new JCheckBoxMenuItem(b(c(REDUCE), c(VERTICAL), c(GAPS)), ps.reduceVerticalGaps);
        vGap.addActionListener(_ -> {
            ps.reduceVerticalGaps = vGap.isSelected();
            recomputeAndRepaint();
        });

        menu.add(hOff);
        menu.add(vOff);
        menu.addSeparator();
        menu.add(hGap);
        menu.add(vGap);
        return menu;
    }

    private JPanel buildLeftPane() {

        return navHeader.buildLeftPane(
                treeNav.scrollPane(),
                tableNav.scrollPane(),
                inspector.buildPanel(),
                new SearchDocumentListener(),
                this::setTreeViewActive,
                this::setFilterMode);
    }

    private JPanel buildBottomBar() {

        return bottomBar.buildPanel(
                this::performUndo,
                this::performRedo,
                this::performRevertAll,
                this::performResetAllToZero,
                this::performAutoLayoutOrCancel,
                this::confirmAndReload,
                this::openInBrowser,
                this::saveToSources);
    }

    private void onTreeSelectionChanged() {

        if (state.isInteractive()) {
            final TreeItem selected = treeNav.selectedItem();
            if (null != selected) {
                selectScenario(selected.className());
            }
        }
    }

    private void onTableSelectionChanged(final ListSelectionEvent e) {

        if (state.isInteractive() && !e.getValueIsAdjusting()) {
            final String className = tableNav.selectedClassName();
            if (null != className) {
                selectScenario(className);
            }
        }
    }

    private void onTableModelUpdated(final TableModelEvent e) {

        if (state.isInteractive() && TableModelEvent.UPDATE == e.getType()) {
            final int row = e.getFirstRow();
            final int col = e.getColumn();
            if (tableNav.isEditableOffsetCell(row, col)) {
                applyTableRowEdit(row);
            }
        }
    }

    private void applyTableRowEdit(final int row) {

        final String className = tableNav.classNameAtModelRow(row);
        final int newCol = tableNav.colOffsetAtModelRow(row);
        final int newRow = tableNav.rowOffsetAtModelRow(row);
        if (model.setOffsets(className, newCol, newRow)) {
            refreshAfterOffsetMutation();
        } else {
            syncTableRowForClass(className);
        }
    }

    private void configureCanvasCallbacks() {

        canvas.setOnSelectionChanged(this::selectScenario);
        canvas.setOnNudge(this::onCanvasNudge);
        canvas.setOnResetSelected(this::onCanvasResetSelected);
    }

    private void onCanvasNudge(final int deltaCol, final int deltaRow) {

        final String selected = state.selectedClassName();
        if (null != selected && model.nudgeOffsets(selected, deltaCol, deltaRow)) {
            refreshAfterOffsetMutation();
        }
    }

    private void onCanvasResetSelected() {

        final String selected = state.selectedClassName();
        if (null != selected && model.setOffsets(selected, 0, 0)) {
            refreshAfterOffsetMutation();
        }
    }

    private void onColOffsetSpinnerChanged() {

        final String selected = state.selectedClassName();
        if (state.isInteractive() && null != selected) {
            final int nextCol = inspector.colOffsetValue();
            final int curRow = model.currentOffsets(selected).rowOffset();
            if (model.setOffsets(selected, nextCol, curRow)) {
                refreshAfterOffsetMutation();
            }
        }
    }

    private void onRowOffsetSpinnerChanged() {

        final String selected = state.selectedClassName();
        if (state.isInteractive() && null != selected) {
            final int curCol = model.currentOffsets(selected).colOffset();
            final int nextRow = inspector.rowOffsetValue();
            if (model.setOffsets(selected, curCol, nextRow)) {
                refreshAfterOffsetMutation();
            }
        }
    }

    private void resetSelectedColOffset() {

        final String selected = state.selectedClassName();
        if (null != selected) {
            final int curRow = model.currentOffsets(selected).rowOffset();
            if (model.setOffsets(selected, 0, curRow)) {
                refreshAfterOffsetMutation();
            }
        }
    }

    private void resetSelectedRowOffset() {

        final String selected = state.selectedClassName();
        if (null != selected) {
            final int curCol = model.currentOffsets(selected).colOffset();
            if (model.setOffsets(selected, curCol, 0)) {
                refreshAfterOffsetMutation();
            }
        }
    }

    private void revertSelectedNode() {

        final String selected = state.selectedClassName();
        if (null != selected && model.revertNode(selected)) {
            refreshAfterOffsetMutation();
        }
    }

    private void focusSelectedNode() {

        final String selected = state.selectedClassName();
        if (null != selected) {
            canvas.focusNode(selected, true);
        }
    }

    private void installKeyBindings() {

        final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask), TREE_EDITOR_UNDO);
        getActionMap().put(TREE_EDITOR_UNDO, new KeyBindingAction(KeyCommand.UNDO));
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask | InputEvent.SHIFT_DOWN_MASK), TREE_EDITOR_REDO);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuMask), TREE_EDITOR_REDO);
        getActionMap().put(TREE_EDITOR_REDO, new KeyBindingAction(KeyCommand.REDO));
    }

    private void setFilterMode(final FilterMode mode) {

        state.setFilterMode(mode);
        rebuildNavigatorViews();
    }

    private void reloadFromSources() {

        state.cancelActiveSolverIfRunning();
        final String rootPackage = root;
        state.setLoadedRootPackage(rootPackage);
        final Path baseDir = Path.of(EMPTY).toAbsolutePath();
        model.loadFromPackage(rootPackage, baseDir);
        state.setSelectedClassName(null);
        state.setInitialFitPending(true);
        refreshAllViews();
    }

    private void confirmAndReload() {

        boolean proceed = true;
        if (0 < model.modifiedCount()) {
            final int choice = JOptionPane.showConfirmDialog(
                    this,
                    b(c(DISCARD), s(model.modifiedCount()), UNSAVED, OFFSET,
                            s(CHANGE, e(ROUND, S)), AND, RELOAD, FROM, s(SOURCES, QUS_MRK)),
                    b(c(CONFIRM), c(RELOAD)),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            proceed = JOptionPane.YES_OPTION == choice;
        }
        if (proceed) {
            reloadFromSources();
        }
    }

    private void performUndo() {

        if (!state.isSolverRunning() && model.undo()) {
            refreshAfterOffsetMutation();
        }
    }

    private void performRedo() {

        if (!state.isSolverRunning() && model.redo()) {
            refreshAfterOffsetMutation();
        }
    }

    private void performRevertAll() {

        if (!state.isSolverRunning() && 0 < model.modifiedCount() && model.revertAll()) {
            refreshAfterOffsetMutation();
        }
    }

    private void performResetAllToZero() {

        if (!state.isSolverRunning()) {
            tableNav.stopEditing();
            if (0 < model.shiftedCount() && model.resetAllToZero()) {
                refreshAfterOffsetMutation();
            }
        }
    }

    private void performAutoLayoutOrCancel() {

        if (state.isSolverRunning()) {
            cancelActiveSolver();
        } else {
            tableNav.stopEditing();
            if (!model.isEmpty()) {
                startAutoLayout();
            }
        }
    }

    private void cancelActiveSolver() {

        state.cancelActiveSolverIfRunning();
        bottomBar.disableAutoLayoutButton();
        statusMetricsLabel.setText(b(c(CANCELLING), s(d(AUTO, LAYOUT), ELLIPSIS)));
    }

    private void startAutoLayout() {

        final TreeLayoutAutoSolver.AspectPreset preset = getSelectedAspectPreset();
        final List<TreeLayoutEditorModel.ScenarioBlueprint> rootsSnapshot = List.copyOf(model.roots());
        final Map<String, ScenarioOffsetSourceCodec.Offsets> startOffsets = model.snapshotCurrentOffsets();
        final TreeLayoutEditorModel.PreviewSettings settingsSnapshot = model.previewSettings().copy();

        state.resetSolverCancelled();
        setSolverUiState(true);
        statusMetricsLabel.setForeground(COLOR_SHIFTED);
        statusMetricsLabel.setText(b(c(OPTIMIZING), s(e(ROUND, preset.label()), ELLIPSIS)));

        final AutoLayoutWorker worker = new AutoLayoutWorker(rootsSnapshot, startOffsets, settingsSnapshot, preset);
        state.setActiveSolver(worker);
        worker.execute();
    }

    private void setSolverUiState(final boolean running) {

        bottomBar.setSolverRunning(running, !model.isEmpty());
        if (running) {
            inspector.disableEditableControls();
        } else {
            updateInspector();
            updateStatusAndButtons();
        }
    }

    private void selectScenario(final String className) {

        state.setSelectedClassName(className);
        canvas.setSelectedClassName(className);
        syncSelectionToNavigator();
        updateInspector();
    }

    private void recomputeAndRepaint() {

        model.recomputePreview();
        updateInspector();
        updateStatusAndButtons();
        canvas.repaint();
    }

    private void refreshAfterOffsetMutation() {

        if (FilterMode.SHIFTED == state.filterMode() || FilterMode.MODIFIED == state.filterMode()) {
            rebuildNavigatorViews();
        } else {
            syncTableAllRows();
            treeNav.repaint();
        }
        updateInspector();
        updateStatusAndButtons();
        canvas.repaint();
    }

    private void refreshAllViews() {

        rebuildNavigatorViews();
        canvas.setSelectedClassName(state.selectedClassName());
        updateInspector();
        updateStatusAndButtons();
        if (0 < canvas.getWidth() && 0 < canvas.getHeight()) {
            state.setInitialFitPending(false);
            canvas.fitToView();
        } else {
            canvas.repaint();
        }
    }

    private void rebuildNavigatorViews() {

        state.setInteractive(false);
        try {
            final String query = navHeader.searchQuery();
            rebuildTreeModel(query);
            rebuildTableModel(query);
        } finally {
            state.setInteractive(true);
        }
        syncSelectionToNavigator();
        updateStatusAndButtons();
    }

    private void rebuildTreeModel(final CharSequence query) {

        treeNav.clearForRebuild();
        for (final TreeLayoutEditorModel.ScenarioBlueprint rootBp : model.roots()) {
            final DefaultMutableTreeNode built = buildFilteredTreeNode(rootBp, query);
            if (null != built) {
                treeNav.addRootNode(built);
            }
        }
        treeNav.finishRebuild(!query.isEmpty() || FilterMode.ALL != state.filterMode());
    }

    private void rebuildTableModel(final CharSequence query) {

        tableNav.clearRows();
        for (final TreeLayoutEditorModel.ScenarioBlueprint bp : model.blueprintsByClassName().values()) {
            if (matchesFilter(bp, query)) {
                tableNav.addRow(bp, model.currentOffsets(bp.className()));
            }
        }
    }

    private DefaultMutableTreeNode buildFilteredTreeNode(final TreeLayoutEditorModel.ScenarioBlueprint bp, final CharSequence query) {

        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                new TreeItem(bp.className(), bp.simpleName(), bp.isLeaf()));
        boolean anyChildKept = false;
        for (final TreeLayoutEditorModel.ScenarioBlueprint child : bp.children()) {
            final DefaultMutableTreeNode childNode = buildFilteredTreeNode(child, query);
            if (null != childNode) {
                node.add(childNode);
                anyChildKept = true;
            }
        }
        final DefaultMutableTreeNode result;
        if (matchesFilter(bp, query) || anyChildKept) {
            treeNav.registerNode(bp.className(), node);
            result = node;
        } else {
            result = null;
        }
        return result;
    }

    private boolean matchesFilter(final TreeLayoutEditorModel.ScenarioBlueprint bp, final CharSequence query) {

        final boolean queryMatches = query.isEmpty()
                || bp.simpleName().toLowerCase(Locale.ROOT).contains(query)
                || bp.className().toLowerCase(Locale.ROOT).contains(query);
        final boolean modeMatches = switch (state.filterMode()) {
            case ALL -> true;
            case BRANCHES -> !bp.isLeaf();
            case SHIFTED -> model.isShifted(bp.className());
            case MODIFIED -> model.isModified(bp.className());
        };
        return queryMatches && modeMatches;
    }

    private void syncSelectionToNavigator() {

        state.setInteractive(false);
        try {
            final String selected = state.selectedClassName();
            if (null == selected) {
                treeNav.clearSelection();
                tableNav.clearSelection();
            } else {
                treeNav.selectClass(selected);
                tableNav.selectClass(selected);
            }
        } finally {
            state.setInteractive(true);
        }
    }

    private void syncTableAllRows() {

        state.setInteractive(false);
        try {
            tableNav.syncAllRows(model);
        } finally {
            state.setInteractive(true);
        }
    }

    private void syncTableRowForClass(final String className) {

        state.setInteractive(false);
        try {
            tableNav.syncRowForClass(className, model.currentOffsets(className));
        } finally {
            state.setInteractive(true);
        }
    }

    private void updateInspector() {

        state.setInteractive(false);
        try {
            final String selected = state.selectedClassName();
            final TreeLayoutEditorModel.ScenarioBlueprint bp = (null == selected) ? null : model.blueprint(selected);
            final boolean hasSel = null != bp;
            inspector.setControlsEnabled(hasSel);
            if (hasSel) {
                populateInspector(selected, bp);
            } else {
                inspector.clear();
            }
        } finally {
            state.setInteractive(true);
        }
    }

    private void populateInspector(final String selected, final TreeLayoutEditorModel.ScenarioBlueprint bp) {

        final ScenarioOffsetSourceCodec.Offsets cur = model.currentOffsets(selected);
        final boolean dirty = model.isModified(selected);
        final String parentCls = model.parentClassName(selected);
        final TreeLayoutEditorModel.ScenarioBlueprint parentBp = (null == parentCls) ? null : model.blueprint(parentCls);
        final String parentLabel = (null == parentBp) ? e(ROUND, ROOT) : parentBp.simpleName();
        final String metaText = String.format(FMT_INSPECTOR_META, parentLabel, bp.subtreeSize(), s1(bp.subtreeSize()));
        final String posText = formatSelectedPosition(selected);

        inspector.populate(s(dirty ? DIRTY_MARKER : EMPTY, bp.simpleName()), metaText, posText, cur, dirty);
    }

    private String formatSelectedPosition(final String selected) {

        final TreeLayoutEditorModel.PreviewResult preview = model.latestPreview();
        final List<TreeHtml.NodeData> nodes = preview.nodesByClassName().get(selected);
        final String text;
        if (null == nodes || nodes.isEmpty()) {
            text = s(_SPACE_);
        } else {
            final TreeHtml.NodeData nodeData = nodes.getFirst();
            final TreeLayoutEditorModel.Point2DGrid raw = preview.rawPositionByNodeId().get(nodeData.id());
            final StringBuilder pos = new StringBuilder(String.format(FMT_GRID_POS, nodeData.col(), nodeData.row()));
            if (hasCompactionDelta(raw, nodeData)) {
                pos.append(String.format(FMT_RAW_POS, raw.col(), raw.row()));
            }
            text = pos.toString();
        }
        return text;
    }

    private boolean hasCompactionDelta(final TreeLayoutEditorModel.Point2DGrid raw, final TreeHtml.NodeData nodeData) {

        final boolean compactedWithRaw = model.previewSettings().compacted && null != raw;
        return compactedWithRaw && (raw.col() != nodeData.col() || ROW_DELTA_EPSILON < Math.abs(raw.row() - nodeData.row()));
    }

    private void updateStatusAndButtons() {

        final TreeLayoutEditorModel.PreviewResult preview = model.latestPreview();
        final int shifted = model.shiftedCount();
        final int modified = model.modifiedCount();

        navHeader.updateFilterBadges(shifted, modified);
        updateStatusMetrics(preview, shifted, modified);

        final boolean solving = state.isSolverRunning();
        final boolean idle = !solving;
        final boolean hasScenarios = !model.isEmpty();
        bottomBar.updateState(solving, idle, hasScenarios, model.canUndo(), model.canRedo(), shifted, modified);
    }

    private void updateStatusMetrics(
            final TreeLayoutEditorModel.PreviewResult preview, final int shifted, final int modified) {

        if (preview.layout().nodes().isEmpty()) {
            statusMetricsLabel.setText(NO_SCENARIOS_LOADED);
        } else {
            final double ratio = TreeLayoutAutoSolver.aspectRatio(preview);
            final int totalRows = Math.toIntExact(Math.round(Math.ceil(preview.totalRows())));
            final StringBuilder sb = new StringBuilder(INITIAL_SB_CAPACITY);
            sb.append(b(s(preview.totalCols()), COLS, s(MLT_SGN), s(totalRows), ROWS))
                    .append(String.format(Locale.ROOT, FMT_RATIO_SUFFIX, ratio))
                    .append(SEP_MID_DOT).append(b(s(shifted), SHIFTED));
            if (0 < modified) {
                sb.append(SEP_MID_DOT).append(b(s(modified), UNSAVED));
            }
            if (0 < preview.overlapCount()) {
                sb.append(SEP_MID_DOT).append(b(s(preview.overlapCount()), s(OVERLAPS, _EXCL_)));
                statusMetricsLabel.setForeground(COLOR_ERROR);
            } else {
                statusMetricsLabel.setForeground(COLOR_STATUS);
            }
            statusMetricsLabel.setText(sb.toString());
        }
    }

    private void openInBrowser() {

        tableNav.stopEditing();
        final TreeLayoutEditorModel.PreviewResult preview = model.latestPreview();
        if (preview.layout().nodes().isEmpty()) {
            JOptionPane.showMessageDialog(this, b(c(NO), SCENARIOS, TO, s(PREVIEW, _DOT_)),
                    c(INFO), JOptionPane.INFORMATION_MESSAGE);
        } else {
            try {
                final String html = TreeHtml.renderHtml(preview.layout());
                final File tempHtml = File.createTempFile(s(d(TREE, PREVIEW), _DASH_), s(_DOT_, HTML));
                writeToFile(html, tempHtml.getAbsolutePath());
                Desktop.getDesktop().browse(tempHtml.toURI());
            } catch (final IOException ex) {
                JOptionPane.showMessageDialog(this,
                        sn(b(c(ERROR), GENERATING, BROWSER, s(PREVIEW, _COLON_)), _SPACE_, ex.getMessage()),
                        c(ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveToSources() {

        tableNav.stopEditing();
        final List<TreeLayoutEditorModel.OffsetChange> changes = model.modifiedChanges();
        if (changes.isEmpty()) {
            JOptionPane.showMessageDialog(this, b(c(NO), CHANGES, s(DETECTED, _DOT_)),
                    c(INFO), JOptionPane.INFORMATION_MESSAGE);
        } else if (isSaveConfirmed(changes)) {
            applySourceChanges(changes);
        }
    }

    private boolean isSaveConfirmed(final Collection<TreeLayoutEditorModel.OffsetChange> changes) {

        final JTextArea area = buildChangesDiffArea(changes);
        final int confirm = JOptionPane.showConfirmDialog(
                this,
                new Object[]{
                        b(c(APPLY), OFFSET, CHANGES, TO, s(changes.size()), c(JAVA), SOURCE,
                                s(FILE, e(ROUND, S), QUS_MRK)),
                        new JScrollPane(area)},
                b(c(CONFIRM), SAVE_TO_SOURCES),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return JOptionPane.OK_OPTION == confirm;
    }

    private static JTextArea buildChangesDiffArea(final Collection<TreeLayoutEditorModel.OffsetChange> changes) {

        final StringBuilder diff = new StringBuilder(INITIAL_DIFF_CAPACITY);
        for (final TreeLayoutEditorModel.OffsetChange change : changes) {
            diff.append(String.format(FMT_DIFF_LINE,
                    change.simpleName(),
                    change.initial().colOffset(), change.initial().rowOffset(),
                    change.current().colOffset(), change.current().rowOffset()));
        }
        final JTextArea area = new JTextArea(diff.toString(), Math.min(DIFF_AREA_MAX_ROWS, changes.size() + 1), DIFF_AREA_COLS);
        area.setEditable(false);
        area.setFont(FONT_MONO_12);
        return area;
    }

    private void applySourceChanges(final Iterable<TreeLayoutEditorModel.OffsetChange> changes) {

        final Path baseDir = Path.of(EMPTY).toAbsolutePath();
        int savedCount = 0;
        final StringBuilder errors = new StringBuilder(INITIAL_SB_CAPACITY);
        for (final TreeLayoutEditorModel.OffsetChange change : changes) {
            final Path sourcePath = ScenarioOffsetSourceCodec.resolveSourcePath(baseDir, change.className());
            try {
                ScenarioOffsetSourceCodec.writeOffsets(sourcePath, change.current());
                model.markSaved(change.className());
                savedCount++;
            } catch (final IOException ex) {
                errors.append(change.className()).append(_COLON_).append(_SPACE_).append(ex.getMessage()).append(_LFD_);
            }
        }

        refreshAfterOffsetMutation();

        if (errors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    s(b(c(SUCCESSFULLY), UPDATED, s(savedCount), SOURCE, s(FILE, e(ROUND, S), _DOT_)),
                            _LFD_, b(c(RECOMPILE), THE, TEST, PROJECT, FOR, ANNOTATIONS, TO, TAKE, s(EFFECT, _DOT_))),
                    c(SAVED), JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    s(b(c(SAVED), s(savedCount), s(FILE, e(ROUND, S), _COMMA_), WITH, s(ERRORS, _COLON_)),
                            _LFD_, errors),
                    b(c(PARTIAL), c(SAVE)), JOptionPane.WARNING_MESSAGE);
        }
    }

    private static JButton compactButton(final String label, final String tooltip) {

        final JButton btn = new JButton(label);
        btn.setMargin(new Insets(2, 8, 2, 8));
        btn.setToolTipText(tooltip);
        btn.setFocusable(false);
        return btn;
    }

    private static final class EditorState {

        private FilterMode filterMode = FilterMode.ALL;
        private String selectedClassName = null;
        private String loadedRootPackage = null;
        private boolean interactive = true;
        private boolean initialFitPending = true;
        private SwingWorker<TreeLayoutAutoSolver.SolveResult, TreeLayoutAutoSolver.SolveProgress> activeSolver = null;
        private final AtomicBoolean solverCancelled = new AtomicBoolean(false);

        private FilterMode filterMode() {

            return filterMode;
        }

        private void setFilterMode(final FilterMode mode) {

            filterMode = mode;
        }

        private String selectedClassName() {

            return selectedClassName;
        }

        private void setSelectedClassName(final String className) {

            selectedClassName = className;
        }

        private String loadedRootPackage() {

            return loadedRootPackage;
        }

        private void setLoadedRootPackage(final String rootPackage) {

            loadedRootPackage = rootPackage;
        }

        private boolean isInteractive() {

            return interactive;
        }

        private void setInteractive(final boolean value) {

            interactive = value;
        }

        private boolean isInitialFitPending() {

            return initialFitPending;
        }

        private void setInitialFitPending(final boolean pending) {

            initialFitPending = pending;
        }

        private boolean isSolverRunning() {

            return null != activeSolver && !activeSolver.isDone();
        }

        private boolean isSolverCancelled() {

            return solverCancelled.get();
        }

        private void resetSolverCancelled() {

            solverCancelled.set(false);
        }

        private void setActiveSolver(
                final SwingWorker<TreeLayoutAutoSolver.SolveResult, TreeLayoutAutoSolver.SolveProgress> worker) {

            activeSolver = worker;
        }

        private void clearActiveSolver() {

            activeSolver = null;
        }

        private void cancelActiveSolverIfRunning() {

            if (isSolverRunning()) {
                solverCancelled.set(true);
                activeSolver.cancel(false);
            }
        }
    }

    private static final class NavigatorHeaderControls {

        private final JTextField searchField = new JTextField(SEARCH_FIELD_COLUMNS);
        private final JToggleButton treeViewBtn = new JToggleButton(c(TREE), true);
        private final JToggleButton tableViewBtn = new JToggleButton(c(TABLE), false);
        private final JToggleButton filterShiftedBtn = new JToggleButton(c(SHIFTED), false);
        private final JToggleButton filterModifiedBtn = new JToggleButton(c(MODIFIED), false);
        private final CardLayout cardLayout = new CardLayout();
        private final JPanel cardPanel = new JPanel(cardLayout);

        private JPanel buildLeftPane(
                final JScrollPane treeScrollPane,
                final JScrollPane tableScrollPane,
                final JPanel inspectorPanel,
                final DocumentListener searchListener,
                final Consumer<? super Boolean> onViewToggle,
                final Consumer<? super FilterMode> onFilterChanged) {

            final JPanel left = new JPanel(new BorderLayout(0, 6));
            left.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 4));
            left.setMinimumSize(new Dimension(LEFT_PANE_MIN_WIDTH, LEFT_PANE_MIN_HEIGHT));

            final JPanel header = new JPanel(new BorderLayout(4, 4));
            header.add(buildSearchRow(onViewToggle), BorderLayout.PAGE_START);
            header.add(buildFilterRow(onFilterChanged), BorderLayout.PAGE_END);
            searchField.getDocument().addDocumentListener(searchListener);

            cardPanel.add(treeScrollPane, CARD_TREE);
            cardPanel.add(tableScrollPane, CARD_TABLE);
            cardLayout.show(cardPanel, CARD_TREE);

            left.add(header, BorderLayout.PAGE_START);
            left.add(cardPanel, BorderLayout.CENTER);
            left.add(inspectorPanel, BorderLayout.PAGE_END);
            return left;
        }

        private JPanel buildSearchRow(final Consumer<? super Boolean> onViewToggle) {

            final JPanel searchRow = new JPanel(new BorderLayout(4, 0));
            final JPanel searchInputWrap = new JPanel(new BorderLayout(2, 0));
            searchField.setToolTipText(b(c(FILTER), SCENARIOS, BY, NAME, OR, CLASS));
            final JButton clearSearchBtn = compactButton(s(MLT_SGN), b(c(CLEAR), SEARCH));
            clearSearchBtn.addActionListener(_ -> searchField.setText(EMPTY));
            searchInputWrap.add(searchField, BorderLayout.CENTER);
            searchInputWrap.add(clearSearchBtn, BorderLayout.LINE_END);

            final JPanel viewToggleWrap = new JPanel(new FlowLayout(FlowLayout.TRAILING, 2, 0));
            final ButtonGroup viewGroup = new ButtonGroup();
            styleToggle(treeViewBtn);
            styleToggle(tableViewBtn);
            viewGroup.add(treeViewBtn);
            viewGroup.add(tableViewBtn);
            treeViewBtn.addActionListener(_ -> onViewToggle.accept(Boolean.TRUE));
            tableViewBtn.addActionListener(_ -> onViewToggle.accept(Boolean.FALSE));
            viewToggleWrap.add(treeViewBtn);
            viewToggleWrap.add(tableViewBtn);

            searchRow.add(searchInputWrap, BorderLayout.CENTER);
            searchRow.add(viewToggleWrap, BorderLayout.LINE_END);
            return searchRow;
        }

        private JPanel buildFilterRow(final Consumer<? super FilterMode> onFilterChanged) {

            final JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 3, 0));
            final JToggleButton filterAllBtn = new JToggleButton(c(ALL), true);
            final JToggleButton filterBranchesBtn = new JToggleButton(c(BRANCHES), false);
            final ButtonGroup filterGroup = new ButtonGroup();
            for (final JToggleButton btn : new JToggleButton[]{filterAllBtn, filterBranchesBtn, filterShiftedBtn, filterModifiedBtn}) {
                styleToggle(btn);
                filterGroup.add(btn);
                filterRow.add(btn);
            }
            filterAllBtn.addActionListener(_ -> onFilterChanged.accept(FilterMode.ALL));
            filterBranchesBtn.addActionListener(_ -> onFilterChanged.accept(FilterMode.BRANCHES));
            filterShiftedBtn.addActionListener(_ -> onFilterChanged.accept(FilterMode.SHIFTED));
            filterModifiedBtn.addActionListener(_ -> onFilterChanged.accept(FilterMode.MODIFIED));
            return filterRow;
        }

        private static void styleToggle(final JToggleButton btn) {

            btn.setMargin(new Insets(2, 7, 2, 7));
            btn.setFont(FONT_SANS_11);
            btn.setFocusable(false);
        }

        private boolean isTreeViewSelected() {

            return treeViewBtn.isSelected();
        }

        private void showView(final boolean treeView) {

            if (treeView) {
                treeViewBtn.setSelected(true);
                cardLayout.show(cardPanel, CARD_TREE);
            } else {
                tableViewBtn.setSelected(true);
                cardLayout.show(cardPanel, CARD_TABLE);
            }
        }

        private String searchQuery() {

            return searchField.getText().strip().toLowerCase(Locale.ROOT);
        }

        private void updateFilterBadges(final int shifted, final int modified) {

            filterShiftedBtn.setText(0 < shifted ? b(c(SHIFTED), e(ROUND, shifted)) : c(SHIFTED));
            filterModifiedBtn.setText(0 < modified ? b(c(MODIFIED), e(ROUND, modified)) : c(MODIFIED));
        }
    }

    private static final class TreeNavigatorControls {

        private final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(c(SCENARIOS));
        private final DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
        private final JTree scenarioTree = new JTree(treeModel);
        private final Map<String, DefaultMutableTreeNode> treeNodeByClassName = new HashMap<>(INITIAL_MAP_CAPACITY);

        private void configure(final TreeCellRenderer renderer, final Runnable onSelectionChanged) {

            scenarioTree.setRootVisible(false);
            scenarioTree.setShowsRootHandles(true);
            scenarioTree.setRowHeight(TREE_ROW_HEIGHT);
            scenarioTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            scenarioTree.setCellRenderer(renderer);
            scenarioTree.addTreeSelectionListener(_ -> onSelectionChanged.run());
        }

        private JScrollPane scrollPane() {

            return new JScrollPane(scenarioTree);
        }

        private void clearForRebuild() {

            treeRoot.removeAllChildren();
            treeNodeByClassName.clear();
        }

        private void addRootNode(final MutableTreeNode node) {

            treeRoot.add(node);
        }

        private void registerNode(final String className, final DefaultMutableTreeNode node) {

            treeNodeByClassName.putIfAbsent(className, node);
        }

        private void finishRebuild(final boolean expandAll) {

            treeModel.reload();
            if (expandAll) {
                for (int i = 0; scenarioTree.getRowCount() > i; i++) {
                    scenarioTree.expandRow(i);
                }
            } else {
                expandTopTwoLevels();
            }
        }

        private void expandTopTwoLevels() {

            for (int i = 0; treeRoot.getChildCount() > i; i++) {
                expandNodeAndDirectChildren((DefaultMutableTreeNode) treeRoot.getChildAt(i));
            }
        }

        private void expandNodeAndDirectChildren(final DefaultMutableTreeNode parentNode) {

            expandNodePath(parentNode);
            for (int j = 0; parentNode.getChildCount() > j; j++) {
                expandNodePath((DefaultMutableTreeNode) parentNode.getChildAt(j));
            }
        }

        private void expandNodePath(final DefaultMutableTreeNode node) {

            scenarioTree.expandPath(new TreePath(node.getPath()));
        }

        private void clearSelection() {

            scenarioTree.clearSelection();
        }

        private void selectClass(final String className) {

            final DefaultMutableTreeNode treeNode = treeNodeByClassName.get(className);
            if (null == treeNode) {
                scenarioTree.clearSelection();
            } else {
                final TreePath path = new TreePath(treeNode.getPath());
                scenarioTree.setSelectionPath(path);
                scenarioTree.scrollPathToVisible(path);
            }
        }

        private TreeItem selectedItem() {

            final TreePath path = scenarioTree.getSelectionPath();
            final TreeItem result;
            if (null != path
                    && path.getLastPathComponent() instanceof final DefaultMutableTreeNode node
                    && node.getUserObject() instanceof final TreeItem item) {
                result = item;
            } else {
                result = null;
            }
            return result;
        }

        private void repaint() {

            scenarioTree.repaint();
        }
    }

    private static final class TableNavigatorControls {

        private final DefaultTableModel tableModel = new OffsetTableModel();
        private final JTable scenarioTable = new JTable(tableModel);

        private void configure(
                final TableCellRenderer renderer,
                final ListSelectionListener selectionListener,
                final TableModelListener modelListener) {

            scenarioTable.setRowHeight(TABLE_ROW_HEIGHT);
            scenarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            scenarioTable.getTableHeader().setReorderingAllowed(false);
            scenarioTable.setFont(FONT_MONO_12);

            final TableRowSorter<DefaultTableModel> tableSorter = new TableRowSorter<>(tableModel);
            scenarioTable.setRowSorter(tableSorter);

            final Comparator<String> strCmp = Comparator.nullsFirst(Comparator.naturalOrder());
            tableSorter.setComparator(COL_SCENARIO, strCmp);
            tableSorter.setComparator(COL_CLASS, strCmp);

            scenarioTable.getColumnModel().getColumn(COL_SCENARIO).setPreferredWidth(COL_WIDTH_SCENARIO);
            scenarioTable.getColumnModel().getColumn(COL_OFFSET).setPreferredWidth(COL_WIDTH_OFFSET);
            scenarioTable.getColumnModel().getColumn(COL_ROW_OFFSET).setPreferredWidth(COL_WIDTH_OFFSET);
            scenarioTable.getColumnModel().getColumn(COL_CLASS).setPreferredWidth(COL_WIDTH_CLASS);

            for (int col = 0; TABLE_COLUMN_COUNT > col; col++) {
                scenarioTable.getColumnModel().getColumn(col).setCellRenderer(renderer);
            }

            scenarioTable.getSelectionModel().addListSelectionListener(selectionListener);
            tableModel.addTableModelListener(modelListener);
        }

        private JScrollPane scrollPane() {

            return new JScrollPane(scenarioTable);
        }

        private void clearRows() {

            tableModel.setRowCount(0);
        }

        private void addRow(final TreeLayoutEditorModel.ScenarioBlueprint bp, final ScenarioOffsetSourceCodec.Offsets off) {

            tableModel.addRow(new Object[]{bp.simpleName(), off.colOffset(), off.rowOffset(), bp.className()});
        }

        private String selectedClassName() {

            final int viewRow = scenarioTable.getSelectedRow();
            final String result;
            if (0 <= viewRow) {
                final int modelRow = scenarioTable.convertRowIndexToModel(viewRow);
                result = classNameAtModelRow(modelRow);
            } else {
                result = null;
            }
            return result;
        }

        private boolean isEditableOffsetCell(final int row, final int col) {

            final boolean validRow = 0 <= row && tableModel.getRowCount() > row;
            final boolean offsetCol = COL_OFFSET == col || COL_ROW_OFFSET == col;
            return validRow && offsetCol;
        }

        private String classNameAtModelRow(final int modelRow) {

            return (String) tableModel.getValueAt(modelRow, COL_CLASS);
        }

        private int colOffsetAtModelRow(final int modelRow) {

            final Object colVal = tableModel.getValueAt(modelRow, COL_OFFSET);
            return (colVal instanceof final Number num) ? Math.max(0, num.intValue()) : 0;
        }

        private int rowOffsetAtModelRow(final int modelRow) {

            final Object rowVal = tableModel.getValueAt(modelRow, COL_ROW_OFFSET);
            return (rowVal instanceof final Number num) ? num.intValue() : 0;
        }

        private void clearSelection() {

            scenarioTable.clearSelection();
        }

        private void selectClass(final String className) {

            final int modelRow = findModelRow(className);
            final int viewRow = (0 <= modelRow) ? scenarioTable.convertRowIndexToView(modelRow) : -1;
            if (0 <= viewRow) {
                scenarioTable.setRowSelectionInterval(viewRow, viewRow);
                scenarioTable.scrollRectToVisible(scenarioTable.getCellRect(viewRow, COL_SCENARIO, true));
            } else {
                scenarioTable.clearSelection();
            }
        }

        private int findModelRow(final String className) {

            int foundRow = -1;
            int row = 0;
            final int rowCount = tableModel.getRowCount();
            while (rowCount > row && 0 > foundRow) {
                if (Objects.equals(className, tableModel.getValueAt(row, COL_CLASS))) {
                    foundRow = row;
                }
                row++;
            }
            return foundRow;
        }

        private void syncAllRows(final TreeLayoutEditorModel editorModel) {

            for (int r = 0; tableModel.getRowCount() > r; r++) {
                final String cls = classNameAtModelRow(r);
                final ScenarioOffsetSourceCodec.Offsets off = editorModel.currentOffsets(cls);
                tableModel.setValueAt(off.colOffset(), r, COL_OFFSET);
                tableModel.setValueAt(off.rowOffset(), r, COL_ROW_OFFSET);
            }
        }

        private void syncRowForClass(final String className, final ScenarioOffsetSourceCodec.Offsets off) {

            final int row = findModelRow(className);
            if (0 <= row) {
                tableModel.setValueAt(off.colOffset(), row, COL_OFFSET);
                tableModel.setValueAt(off.rowOffset(), row, COL_ROW_OFFSET);
            }
        }

        private void stopEditing() {

            if (scenarioTable.isEditing() && null != scenarioTable.getCellEditor()) {
                scenarioTable.getCellEditor().stopCellEditing();
            }
        }
    }

    private static final class InspectorControls {

        private final JLabel nameLabel = new JLabel(NO_NODE_SELECTED);
        private final JLabel metaLabel = new JLabel(CLICK_ANY_NODE___);
        private final JLabel posLabel = new JLabel(s(_SPACE_));
        private final JSpinner colOffsetSpinner = new JSpinner(new SpinnerNumberModel(0, 0, SPINNER_MAX_OFFSET, 1));
        private final JSpinner rowOffsetSpinner = new JSpinner(new SpinnerNumberModel(0, SPINNER_MIN_ROW_OFFSET, SPINNER_MAX_OFFSET, 1));
        private final JButton resetColBtn = new JButton(s(_0_));
        private final JButton resetRowBtn = new JButton(s(_0_));
        private final JButton revertNodeBtn = new JButton(b(c(REVERT), c(NODE)));
        private final JButton focusNodeBtn = new JButton(c(FOCUS));

        private void configure(
                final Runnable onColChange,
                final Runnable onRowChange,
                final Runnable onResetCol,
                final Runnable onResetRow,
                final Runnable onRevert,
                final Runnable onFocus) {

            colOffsetSpinner.addChangeListener(_ -> onColChange.run());
            rowOffsetSpinner.addChangeListener(_ -> onRowChange.run());
            resetColBtn.addActionListener(_ -> onResetCol.run());
            resetRowBtn.addActionListener(_ -> onResetRow.run());
            revertNodeBtn.addActionListener(_ -> onRevert.run());
            focusNodeBtn.addActionListener(_ -> onFocus.run());
        }

        private int colOffsetValue() {

            return ((Number) colOffsetSpinner.getValue()).intValue();
        }

        private int rowOffsetValue() {

            return ((Number) rowOffsetSpinner.getValue()).intValue();
        }

        private JPanel buildPanel() {

            final JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder(b(c(SELECTED), c(NODE))));

            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 4, 2, 4);
            gbc.anchor = GridBagConstraints.LINE_START;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;

            nameLabel.setFont(FONT_SANS_BOLD_13);
            metaLabel.setFont(FONT_MONO_10);
            metaLabel.setForeground(COLOR_MUTED);
            posLabel.setFont(FONT_MONO_11);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 3;
            panel.add(nameLabel, gbc);

            gbc.gridy = 1;
            panel.add(metaLabel, gbc);

            gbc.gridy = 2;
            panel.add(posLabel, gbc);

            gbc.gridy = 3;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            panel.add(new JLabel(b(c(COL), c(OFFSET), s(e(ROUND, AT_VISUAL_OFFSET), _COLON_))), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(colOffsetSpinner, gbc);
            gbc.gridx = 2;
            gbc.weightx = 0.0;
            resetColBtn.setMargin(new Insets(1, 6, 1, 6));
            resetColBtn.setToolTipText(b(c(RESET), AT_VISUAL_OFFSET, TO, s(_0_)));
            panel.add(resetColBtn, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.weightx = 0.0;
            panel.add(new JLabel(b(c(ROW), c(OFFSET), s(e(ROUND, AT_VISUAL_ROW_OFFSET), _COLON_))), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(rowOffsetSpinner, gbc);
            gbc.gridx = 2;
            gbc.weightx = 0.0;
            resetRowBtn.setMargin(new Insets(1, 6, 1, 6));
            resetRowBtn.setToolTipText(b(c(RESET), AT_VISUAL_ROW_OFFSET, TO, s(_0_)));
            panel.add(resetRowBtn, gbc);

            final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
            revertNodeBtn.setMargin(new Insets(2, 8, 2, 8));
            focusNodeBtn.setMargin(new Insets(2, 8, 2, 8));
            actions.add(revertNodeBtn);
            actions.add(focusNodeBtn);

            gbc.gridx = 0;
            gbc.gridy = 5;
            gbc.gridwidth = 3;
            gbc.weightx = 1.0;
            panel.add(actions, gbc);

            final JLabel hint = new JLabel(b(s(c(CANVAS), _COLON_), c(CLICK), SELECT, s(MID_DOT),
                    c(ARROWS), s(PLS_MNS, _1_), s(MID_DOT), s(c(SHIFT), _PLUS_, c(ARROWS)), s(PLS_MNS, _5_),
                    s(MID_DOT), s(_0_), RESET));
            hint.setFont(FONT_SANS_ITALIC_10);
            hint.setForeground(COLOR_MUTED);
            gbc.gridy = 6;
            panel.add(hint, gbc);

            return panel;
        }

        private void setControlsEnabled(final boolean enabled) {

            colOffsetSpinner.setEnabled(enabled);
            rowOffsetSpinner.setEnabled(enabled);
            resetColBtn.setEnabled(enabled);
            resetRowBtn.setEnabled(enabled);
            focusNodeBtn.setEnabled(enabled);
        }

        private void disableEditableControls() {

            colOffsetSpinner.setEnabled(false);
            rowOffsetSpinner.setEnabled(false);
            resetColBtn.setEnabled(false);
            resetRowBtn.setEnabled(false);
            revertNodeBtn.setEnabled(false);
        }

        private void clear() {

            nameLabel.setText(NO_NODE_SELECTED);
            metaLabel.setText(CLICK_ANY_NODE___);
            posLabel.setText(s(_SPACE_));
            colOffsetSpinner.setValue(0);
            rowOffsetSpinner.setValue(0);
            revertNodeBtn.setEnabled(false);
        }

        private void populate(
                final String nameText, final String metaText, final String posText,
                final ScenarioOffsetSourceCodec.Offsets offsets, final boolean dirty) {

            nameLabel.setText(nameText);
            metaLabel.setText(metaText);
            posLabel.setText(posText);
            colOffsetSpinner.setValue(offsets.colOffset());
            rowOffsetSpinner.setValue(offsets.rowOffset());
            revertNodeBtn.setEnabled(dirty);
        }
    }

    private static final class BottomBarControls {

        private final JButton undoBtn = new JButton(c(UNDO));
        private final JButton redoBtn = new JButton(c(REDO));
        private final JButton revertAllBtn = new JButton(b(c(REVERT), c(ALL)));
        private final JButton resetAllZeroBtn = new JButton(b(c(RESET), c(ALL), TO, s(_0_)));
        private final JComboBox<TreeLayoutAutoSolver.AspectPreset> aspectPresetCombo =
                new JComboBox<>(TreeLayoutAutoSolver.AspectPreset.values());
        private final JButton autoLayoutBtn = new JButton(AUTO_LAYOUT);
        private final JButton saveBtn = new JButton(SAVE_TO_SOURCES);

        private JPanel buildPanel(
                final Runnable onUndo,
                final Runnable onRedo,
                final Runnable onRevertAll,
                final Runnable onResetAll,
                final Runnable onAutoLayout,
                final Runnable onReload,
                final Runnable onBrowser,
                final Runnable onSave) {

            final JPanel bottom = new JPanel(new BorderLayout(8, 0));
            bottom.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));

            final JPanel left = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
            undoBtn.addActionListener(_ -> onUndo.run());
            redoBtn.addActionListener(_ -> onRedo.run());
            revertAllBtn.setToolTipText(b(c(DISCARD), UNSAVED, EDITS, AND, RESTORE, OFFSETS, FROM, SOURCES));
            revertAllBtn.addActionListener(_ -> onRevertAll.run());
            resetAllZeroBtn.setToolTipText(b(c(SET), ALL, AT_VISUAL_OFFSET, AND, AT_VISUAL_ROW_OFFSET,
                    VALUES, TO, s(_0_), e(ROUND, UNDOABLE)));
            resetAllZeroBtn.addActionListener(_ -> onResetAll.run());

            final JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
            sep.setPreferredSize(new Dimension(2, SEPARATOR_HEIGHT));

            aspectPresetCombo.setSelectedItem(TreeLayoutAutoSolver.AspectPreset.WIDESCREEN_16_9);
            aspectPresetCombo.setToolTipText(b(c(TARGET), PROPORTIONS, FOR, AUTO_LAYOUT));
            autoLayoutBtn.setToolTipText(b(c(OPTIMIZE), AT_VISUAL_OFFSET, AND, AT_VISUAL_ROW_OFFSET,
                    STARTING, FROM, CURRENT, LAYOUT, e(ROUND, UNDOABLE)));
            autoLayoutBtn.addActionListener(_ -> onAutoLayout.run());

            left.add(undoBtn);
            left.add(redoBtn);
            left.add(revertAllBtn);
            left.add(resetAllZeroBtn);
            left.add(sep);
            left.add(aspectPresetCombo);
            left.add(autoLayoutBtn);

            final JPanel right = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
            final JButton reloadBtn = new JButton(b(c(RELOAD), FROM, c(SOURCES)));
            reloadBtn.addActionListener(_ -> onReload.run());

            final JButton browserBtn = new JButton(b(c(OPEN), IN, c(BROWSER)));
            browserBtn.addActionListener(_ -> onBrowser.run());

            saveBtn.setFont(saveBtn.getFont().deriveFont(Font.BOLD));
            saveBtn.addActionListener(_ -> onSave.run());

            right.add(reloadBtn);
            right.add(browserBtn);
            right.add(saveBtn);

            bottom.add(left, BorderLayout.LINE_START);
            bottom.add(right, BorderLayout.LINE_END);
            return bottom;
        }

        private TreeLayoutAutoSolver.AspectPreset selectedAspectPreset() {

            final Object item = aspectPresetCombo.getSelectedItem();
            return (item instanceof final TreeLayoutAutoSolver.AspectPreset preset)
                    ? preset
                    : TreeLayoutAutoSolver.AspectPreset.WIDESCREEN_16_9;
        }

        private void selectAspectPreset(final TreeLayoutAutoSolver.AspectPreset preset) {

            aspectPresetCombo.setSelectedItem(Objects.requireNonNull(preset, PRESET));
        }

        private void disableAutoLayoutButton() {

            autoLayoutBtn.setEnabled(false);
        }

        private void setSolverRunning(final boolean running, final boolean hasScenarios) {

            autoLayoutBtn.setText(running ? s(c(CANCEL), ELLIPSIS) : AUTO_LAYOUT);
            autoLayoutBtn.setEnabled(running || hasScenarios);
            aspectPresetCombo.setEnabled(!running && hasScenarios);
            if (running) {
                undoBtn.setEnabled(false);
                redoBtn.setEnabled(false);
                revertAllBtn.setEnabled(false);
                resetAllZeroBtn.setEnabled(false);
                saveBtn.setEnabled(false);
            }
        }

        private void updateState(
                final boolean solving, final boolean idle, final boolean hasScenarios,
                final boolean canUndo, final boolean canRedo, final int shifted, final int modified) {

            undoBtn.setEnabled(idle && canUndo);
            redoBtn.setEnabled(idle && canRedo);
            revertAllBtn.setEnabled(idle && 0 < modified);
            resetAllZeroBtn.setEnabled(idle && 0 < shifted);
            aspectPresetCombo.setEnabled(idle && hasScenarios);
            autoLayoutBtn.setEnabled(solving || hasScenarios);
            saveBtn.setEnabled(idle && 0 < modified);
            saveBtn.setText(0 < modified ? b(c(SAVE), e(ROUND, modified), TO, c(SOURCES)) : SAVE_TO_SOURCES);
        }
    }

    private static final class OffsetTableModel extends DefaultTableModel {

        @Serial
        private static final long serialVersionUID = 1L;

        private OffsetTableModel() {

            super(new Object[]{c(SCENARIO), b(c(COL), s(DELTA)), b(c(ROW), s(DELTA)), c(CLASS)}, 0);
        }

        @Override
        public boolean isCellEditable(final int row, final int column) {

            return COL_OFFSET == column || COL_ROW_OFFSET == column;
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {

            return (COL_OFFSET == columnIndex || COL_ROW_OFFSET == columnIndex) ? Integer.class : String.class;
        }
    }

    private final class KeyBindingAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = 1L;
        private final KeyCommand command;

        private KeyBindingAction(final KeyCommand command) {

            this.command = command;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {

            if (KeyCommand.UNDO == command) {
                performUndo();
            } else {
                performRedo();
            }
        }

        @Override
        public Object clone() throws CloneNotSupportedException {

            throw new CloneNotSupportedException(getClass().getName());
        }
    }

    private final class CanvasResizeListener extends ComponentAdapter {

        @Override
        public void componentResized(final ComponentEvent e) {

            final boolean hasSize = 0 < canvas.getWidth() && 0 < canvas.getHeight();
            if (state.isInitialFitPending() && hasSize && !model.isEmpty()) {
                state.setInitialFitPending(false);
                canvas.fitToView();
            }
        }
    }

    private final class PanelShownListener extends ComponentAdapter {

        private final boolean autoLoadFromPackage;

        private PanelShownListener(final boolean autoLoadFromPackage) {

            this.autoLoadFromPackage = autoLoadFromPackage;
        }

        @Override
        public void componentShown(final ComponentEvent e) {

            if (autoLoadFromPackage
                    && !Objects.equals(state.loadedRootPackage(), root)
                    && 0 == model.modifiedCount()) {
                reloadFromSources();
            }
        }
    }

    private final class SearchDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(final DocumentEvent e) {

            rebuildNavigatorViews();
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {

            rebuildNavigatorViews();
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {

            rebuildNavigatorViews();
        }
    }

    private final class ScenarioTreeCellRenderer extends DefaultTreeCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTreeCellRendererComponent(
                final JTree tree, final Object value, final boolean sel,
                final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {

            final Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof final DefaultMutableTreeNode node && node.getUserObject() instanceof final TreeItem item) {
                final ScenarioOffsetSourceCodec.Offsets off = model.currentOffsets(item.className());
                final boolean dirty = model.isModified(item.className());
                final StringBuilder text = new StringBuilder(INITIAL_SB_CAPACITY);
                if (dirty) text.append(DIRTY_MARKER);
                text.append(item.simpleName());
                if (!off.isZero()) {
                    text.append(_SPACE_).append(_SPACE_).append(e(SQUARE, formatOffsetTag(off)));
                }
                setText(text.toString());
                setFont(getFont().deriveFont(item.isLeaf() ? Font.PLAIN : Font.BOLD));
                if (!sel) {
                    applyNodeForeground(dirty, !off.isZero(), item.isLeaf());
                }
            }
            return component;
        }

        private void applyNodeForeground(final boolean dirty, final boolean shifted, final boolean leaf) {

            if (dirty) {
                setForeground(COLOR_DIRTY);
            } else if (shifted) {
                setForeground(COLOR_SHIFTED);
            } else if (leaf) {
                setForeground(COLOR_MUTED);
            }
        }

        private static String formatOffsetTag(final ScenarioOffsetSourceCodec.Offsets o) {

            final String tag;
            if (0 != o.colOffset() && 0 != o.rowOffset()) {
                tag = String.format(FMT_COL_ROW_TAG, o.colOffset(), o.rowOffset());
            } else if (0 != o.colOffset()) {
                tag = String.format(FMT_COL_TAG, o.colOffset());
            } else {
                tag = String.format(FMT_ROW_TAG, o.rowOffset());
            }
            return tag;
        }
    }

    private final class ScenarioTableCellRenderer extends DefaultTableCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(
                final JTable table, final Object value, final boolean isSelected,
                final boolean hasFocus, final int row, final int column) {

            final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final int modelRow = table.convertRowIndexToModel(row);
            final String className = tableNav.classNameAtModelRow(modelRow);
            final boolean dirty = null != className && model.isModified(className);
            final boolean shifted = null != className && model.isShifted(className);
            if (!isSelected) {
                if (dirty) {
                    setForeground(COLOR_DIRTY);
                } else if (shifted) {
                    setForeground(COLOR_SHIFTED);
                } else {
                    setForeground(table.getForeground());
                }
            }
            final boolean numericCol = COL_OFFSET == column || COL_ROW_OFFSET == column;
            setHorizontalAlignment(numericCol ? TRAILING : LEADING);
            return component;
        }
    }

    private final class AutoLayoutWorker
            extends SwingWorker<TreeLayoutAutoSolver.SolveResult, TreeLayoutAutoSolver.SolveProgress> {

        private final List<TreeLayoutEditorModel.ScenarioBlueprint> rootsSnapshot;
        private final Map<String, ScenarioOffsetSourceCodec.Offsets> startOffsets;
        private final TreeLayoutEditorModel.PreviewSettings settingsSnapshot;
        private final TreeLayoutAutoSolver.AspectPreset preset;

        private AutoLayoutWorker(
                final List<TreeLayoutEditorModel.ScenarioBlueprint> rootsSnapshot,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> startOffsets,
                final TreeLayoutEditorModel.PreviewSettings settingsSnapshot,
                final TreeLayoutAutoSolver.AspectPreset preset) {

            this.rootsSnapshot = rootsSnapshot;
            this.startOffsets = startOffsets;
            this.settingsSnapshot = settingsSnapshot;
            this.preset = preset;
        }

        @Override
        protected TreeLayoutAutoSolver.SolveResult doInBackground() {

            return TreeLayoutAutoSolver.solve(
                    rootsSnapshot,
                    startOffsets,
                    settingsSnapshot,
                    preset,
                    state::isSolverCancelled,
                    this::publish);
        }

        @Override
        protected void process(final List<TreeLayoutAutoSolver.SolveProgress> chunks) {

            if (!chunks.isEmpty() && !state.isSolverCancelled()) {
                final TreeLayoutAutoSolver.SolveProgress p = chunks.getLast();
                final int rows = Math.toIntExact(Math.round(Math.ceil(p.bestMetrics().rows())));
                statusMetricsLabel.setText(String.format(Locale.ROOT,
                        FMT_OPTIMIZING_PROGRESS,
                        preset.label(),
                        p.stage(),
                        p.bestMetrics().cols(),
                        rows,
                        p.bestMetrics().aspectRatio()));
            }
        }

        @Override
        protected void done() {

            state.clearActiveSolver();
            setSolverUiState(false);
            try {
                applySolveResult(get());
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                updateStatusAndButtons();
            } catch (final ExecutionException ex) {
                updateStatusAndButtons();
            }
        }

        private void applySolveResult(final TreeLayoutAutoSolver.SolveResult result) {

            if (result.cancelled()) {
                updateStatusAndButtons();
            } else if (result.improved() && model.applyOffsetsBatch(result.offsets())) {
                refreshAfterOffsetMutation();
                canvas.fitToView();
                statusMetricsLabel.setText(s(statusMetricsLabel.getText(),
                        SEP_MID_DOT, b(OPTIMIZED, e(ROUND, preset.label()))));
            } else {
                updateStatusAndButtons();
                statusMetricsLabel.setText(s(b(c(ALREADY), OPTIMAL, FOR, preset.label()),
                        SEP_MID_DOT, statusMetricsLabel.getText()));
            }
        }
    }

    private record TreeItem(String className, String simpleName, boolean isLeaf) {
    }
}
