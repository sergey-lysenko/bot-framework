package works.lysenko.base.ui;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.base.parameters.PropertyValidator;
import works.lysenko.util.apis.parameters._ConfigFileCategory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.A.ACTION;
import static works.lysenko.util.lang.word.C.*;
import static works.lysenko.util.lang.word.D.DELETE;
import static works.lysenko.util.lang.word.D.DUPLICATE;
import static works.lysenko.util.lang.word.D.DUPLICATED;
import static works.lysenko.util.lang.word.E.EMPTY;
import static works.lysenko.util.lang.word.F.FOUND;
import static works.lysenko.util.lang.word.I.INCLUDE;
import static works.lysenko.util.lang.word.I.INVALID;
import static works.lysenko.util.lang.word.L.LOADING;
import static works.lysenko.util.lang.word.M.MODIFIED;
import static works.lysenko.util.lang.word.N.NOTICE;
import static works.lysenko.util.lang.word.P.PARTIAL;
import static works.lysenko.util.lang.word.P.PROPERTY;
import static works.lysenko.util.lang.word.R.RELOAD;
import static works.lysenko.util.lang.word.R.RESOURCES;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.lang.word.U.UNSAVED;
import static works.lysenko.util.lang.word.U.UPDATED;
import static works.lysenko.util.lang.word.V.VALIDATION;
import static works.lysenko.util.lang.word.W.WEIGHT;
import static works.lysenko.util.spec.Layout.Files.COMMON_CONFIGURATION;
import static works.lysenko.util.spec.Layout.Parts.SUITE_PROPERTIES_EXTENSION;
import static works.lysenko.util.spec.Layout.Parts.TEST_PROPERTIES_PARTS_EXTENSION;
import static works.lysenko.util.spec.Layout.Parts._DOT_PROPERTIES;
import static works.lysenko.util.spec.Layout.Paths._CONFIG_;
import static works.lysenko.util.spec.Layout.Paths._SUITES_;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Embedded Micro-IDE style panel for managing configuration files, parts, and test suites.
 * Integrates PropertyValidator for real-time validation, invalid key/value indications, inline editing,
 * and prompting the user to save changes upon losing focus or switching files.
 */
public final class ConfigTestsEditorPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    static {
        ControlPanel.initializeLookAndFeel();
    }

    private static final int SIDEBAR_WIDTH = 360;
    private static final double SPLIT_RESIZE_WEIGHT = 0.32;
    private static final int SCOPE_COLUMN_MAX_WIDTH = 110;
    private static final int COL_SCOPE = 0;
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int TOOLBAR_HGAP = 6;
    private static final int TOOLBAR_VGAP = 4;
    private static final int HEADER_HGAP = 8;
    private static final int HEADER_VGAP = 4;
    private static final int FORM_VGAP = 8;
    private static final int EDITOR_GAP = 6;
    private static final int EDITOR_PADDING = 6;
    private static final int BADGE_VPAD = 2;
    private static final int BADGE_HPAD = 6;
    private static final int WARNING_VPAD = 6;
    private static final int WARNING_HPAD = 10;
    private static final int INITIAL_CONTENT_CAPACITY = 256;
    private static final int INITIAL_INVALID_LIST_CAPACITY = 8;

    private static final Font FONT_SANS_12 = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Font FONT_SANS_BOLD_10 = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    private static final Font FONT_SANS_BOLD_12 = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font FONT_SANS_BOLD_13 = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font FONT_MONO_12 = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private static final Color COLOR_ERROR_BG = new Color(0xFE, 0xF2, 0xF2);
    private static final Color COLOR_ERROR_ROW_BG = new Color(0xFF, 0xF1, 0xF1);
    private static final Color COLOR_ERROR_FG = new Color(0x99, 0x1B, 0x1B);
    private static final Color COLOR_ERROR_BORDER = new Color(0xF8, 0x71, 0x71);
    private static final Color COLOR_AMBER_BG = new Color(0xFE, 0xF3, 0xC7);
    private static final Color COLOR_AMBER_FG = new Color(0x92, 0x40, 0x0E);
    private static final Color COLOR_BLUE_BG = new Color(0xDB, 0xEA, 0xFE);
    private static final Color COLOR_BLUE_FG = new Color(0x1E, 0x40, 0xAF);
    private static final Color COLOR_GREEN_BG = new Color(0xDC, 0xFC, 0xE7);
    private static final Color COLOR_GREEN_FG = new Color(0x16, 0x65, 0x34);
    private static final Color COLOR_INDIGO_BG = new Color(0xEE, 0xF2, 0xFF);
    private static final Color COLOR_INDIGO_FG = new Color(0x37, 0x30, 0xA3);
    private static final Border ERROR_LINE_BORDER = BorderFactory.createLineBorder(COLOR_ERROR_BORDER);

    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9_\\-]");

    private static final String WORD_ERROR = works.lysenko.util.lang.word.E.ERROR;
    private static final String WORD_PROPERTIES = works.lysenko.util.lang.word.P.PROPERTIES;
    private static final String WORD_SAVING = "saving";
    private static final String EMPTY_STRING = StringUtils.EMPTY;
    private static final String ELLIPSIS = s(_DOT_, _DOT_, _DOT_);
    private static final String COMMON_CONFIG_FILE = s(COMMON_CONFIGURATION, _DOT_PROPERTIES);
    private static final String PROPERTY_HELP_FILE = "property-help.properties";
    private static final String PARTS_DIR_NAME = s(PART, S);
    private static final String CARD_FORM = u(FORM);
    private static final String CARD_RAW = u(RAW);
    private static final String SAVE_ACTION_KEY = b(UND_SCR, u(SAVE), u(FILE), u(ACTION));
    private static final String PREFIX_DOT_INCLUDE = s(_DOT_, INCLUDE);
    private static final String SUITES_PREFIX = s(SUITES, _SLASH_);
    private static final String SUITES_PARTS_PREFIX = s(SUITES, _SLASH_, PARTS_DIR_NAME, _SLASH_);
    private static final String CONFIG_SEGMENT = s(_SLASH_, CONFIG, _SLASH_);
    private static final String CONFIG_SUFFIX = s(_SLASH_, CONFIG);

    private static final String LABEL_NO_FILE_SELECTED = b(c(NO), FILE, SELECTED);
    private static final String LABEL_RAW_TEXT_VIEW = b(c(RAW), c(TEXT), c(VIEW));
    private static final String LABEL_SAVE_BUTTON = e(c(SAVE));
    private static final String LABEL_NEW_FILE_BUTTON = s(_PLUS_, _SPACE_, b(c(NEW), c(FILE)), ELLIPSIS);
    private static final String LABEL_DELETE_FILE_BUTTON = b(c(DELETE), c(FILE));
    private static final String LABEL_REFRESH_BUTTON = "Refresh";
    private static final String LABEL_MENU_RENAME = "Rename...";
    private static final String LABEL_MENU_DUPLICATE = s(c(DUPLICATE), ELLIPSIS);
    private static final String LABEL_PROJECT_EXPLORER = "Project Explorer";
    private static final String TOOLTIP_NEW_FILE = "Create a new .suite or .part file";
    private static final String TOOLTIP_DELETE_FILE = b(c(DELETE), SELECTED, FILE);
    private static final String TOOLTIP_REFRESH = "Refresh file tree from disk";
    private static final String TOOLTIP_SAVE_LEFT = "Save current file (Ctrl+S";
    private static final String TOOLTIP_SAVE_RIGHT = "Cmd+S)";
    private static final String TOOLTIP_SAVE_FILE = b(TOOLTIP_SAVE_LEFT, s(_SLASH_), TOOLTIP_SAVE_RIGHT);

    private static final String COL_HEADER_SCOPE = "Scope";
    private static final String COL_HEADER_KEY = b(c(KEY), s(_SLASH_), c(PROPERTY));
    private static final String COL_HEADER_VALUE = b(c(VALUE), s(_SLASH_), c(WEIGHT));

    private static final String BADGE_GLOBAL_CONFIG = "GLOBAL CONFIG";
    private static final String BADGE_CONFIG_PART = b(u(CONFIG), u(PARTIAL));
    private static final String BADGE_TEST_SUITE = b(u(TEST), u(SUITE));
    private static final String BADGE_SUITE_PART = b(u(SUITE), u(PARTIAL));
    private static final String BADGE_FILE = u(FILE);
    private static final String SUFFIX_MODIFIED_BADGE = s(_SPACE_, e(ROUND, u(MODIFIED)));
    private static final String SUFFIX_MODIFIED_STAR = s(_SPACE_, _ASTRS_);

    private static final String SCOPE_INVALID = u(INVALID);
    private static final String SCOPE_INCLUDE = u(INCLUDE);
    private static final String SCOPE_CONFIG = u(CONFIG);
    private static final String SCOPE_COMMENT = "COMMENT";
    private static final String SCOPE_SCENARIO = u(SCENARIO);

    private static final String ICON_WARNING = "⚠️ ";
    private static final String ICON_GEAR = "⚙️ ";
    private static final String ICON_PUZZLE = "🧩 ";
    private static final String ICON_TEST_TUBE = "🧪 ";
    private static final String ICON_FOLDER = "📁 ";

    private static final String TITLE_ERROR = c(WORD_ERROR);
    private static final String TITLE_FILE_ERROR = b(c(FILE), c(WORD_ERROR));
    private static final String TITLE_SAVE_ERROR = b(c(SAVE), c(WORD_ERROR));
    private static final String TITLE_FILE_SAVED = b(c(FILE), c(SAVED));
    private static final String TITLE_VALIDATION_ERROR = b(c(VALIDATION), c(WORD_ERROR));
    private static final String TITLE_VALIDATION_NOTICE = b(c(PROPERTY), c(VALIDATION), c(NOTICE));
    private static final String TITLE_NEW_FILE = b(c(NEW), c(FILE));
    private static final String TITLE_CONFIRM_DELETE = b(c(CONFIRM), c(DELETE));
    private static final String TITLE_DELETED = "Deleted";
    private static final String TITLE_PROTECTED_FILE = "Protected File";
    private static final String TITLE_RENAME_FILE = "Rename File";
    private static final String TITLE_RENAMED = "Renamed";
    private static final String TITLE_DUPLICATE_FILE = b(c(DUPLICATE), c(FILE));
    private static final String TITLE_DUPLICATED = c(DUPLICATED);

    private static final String MSG_UNSAVED_PREFIX = "The file '";
    private static final String MSG_UNSAVED_SUFFIX = "' has unsaved changes.";
    private static final String MSG_UNSAVED_PROMPT = "Do you want to save them before proceeding?";
    private static final String MSG_KEY_EMPTY = s(b(c(KEY), s(_SLASH_), c(PROPERTY), NAME, CANNOT, BE, EMPTY), _DOT_);
    private static final String MSG_VALIDATION_PREFIX = "Validation notice for '";
    private static final String MSG_VALIDATION_SAVE_PROMPT = "Save entry as invalid anyway?";
    private static final String MSG_INVALID_PRESERVED = ". Invalid properties are preserved when saving.";
    private static final String MSG_ERR_LOADING = s(b(c(WORD_ERROR), LOADING, FILE), _COLON_, _SPACE_);
    private static final String MSG_ERR_SAVING = s(b(c(WORD_ERROR), WORD_SAVING, FILE), _COLON_, _SPACE_);
    private static final String MSG_ERR_CREATING = s(b(c(WORD_ERROR), CREATING, FILE), _COLON_, _SPACE_);
    private static final String MSG_ERR_DELETING = "Error deleting file: ";
    private static final String MSG_ERR_RENAMING = "Error renaming file: ";
    private static final String MSG_ERR_DUPLICATING = "Error duplicating file: ";
    private static final String MSG_FILE_EXISTS = "File already exists: ";
    private static final String MSG_INVALID_FILENAME = s(b(c(INVALID), FILE, NAME), _DOT_);
    private static final String MSG_SELECT_FILE_TYPE = s(b(c(SELECT), FILE, TYPE), _COLON_);
    private static final String MSG_ENTER_FILE_NAME = "Enter file name (without extension):";
    private static final String MSG_ENTER_NEW_NAME = "Enter new file name (without extension):";
    private static final String MSG_ENTER_DUPLICATE_NAME = "Enter name for duplicate file (without extension):";
    private static final String MSG_CANNOT_DELETE_GLOBAL = "Cannot delete global system file: ";
    private static final String MSG_CONFIRM_DELETE_PREFIX = "Are you sure you want to delete file ";
    private static final String MSG_FILE_DELETED = "File deleted successfully.";
    private static final String MSG_CANNOT_RENAME_COMMON = "Cannot rename the common configuration file.";
    private static final String MSG_CANNOT_DUPLICATE_COMMON = "Cannot duplicate the common configuration file.";
    private static final String MSG_FILE_RENAMED = "File renamed successfully to ";
    private static final String MSG_FILE_DUPLICATED = s(b(c(FILE), DUPLICATED, SUCCESSFULLY, AS), _SPACE_);
    private static final String SUFFIX_COPY = "_copy";

    private static final String HEADER_GENERATED_LEFT = "# Generated";
    private static final String HEADER_MICRO_IDE = "Micro-IDE";
    private static final String GENERATED_FILE_HEADER = b(
            HEADER_GENERATED_LEFT, s(_SLASH_), c(UPDATED), BY, c(CONFIG), s(_AMPRS_), c(TESTS), HEADER_MICRO_IDE
    );

    private static final String TYPE_TEST_SUITE = b(
            c(TEST), c(SUITE), e(ROUND, b(s(_ASTRS_, SUITE_PROPERTIES_EXTENSION), IN, SUITES_PREFIX))
    );
    private static final String TYPE_SUITE_PART = b(
            c(SUITE), c(PART), e(ROUND, b(s(_ASTRS_, TEST_PROPERTIES_PARTS_EXTENSION), IN, SUITES_PARTS_PREFIX))
    );
    private static final String TYPE_CONFIG_PART = b(
            c(CONFIG), c(PART), e(ROUND, b(s(_ASTRS_, TEST_PROPERTIES_PARTS_EXTENSION), IN, s(CONFIG, _SLASH_, PARTS_DIR_NAME, _SLASH_)))
    );

    private static final Map<String, ScopeStyle> SCOPE_STYLES = Map.of(
            SCOPE_INVALID, new ScopeStyle(s(ICON_WARNING, SCOPE_INVALID), COLOR_ERROR_BG, COLOR_ERROR_FG, true),
            SCOPE_CONFIG, new ScopeStyle(SCOPE_CONFIG, COLOR_BLUE_BG, COLOR_BLUE_FG, false),
            SCOPE_SCENARIO, new ScopeStyle(SCOPE_SCENARIO, COLOR_AMBER_BG, COLOR_AMBER_FG, false),
            SCOPE_INCLUDE, new ScopeStyle(SCOPE_INCLUDE, COLOR_GREEN_BG, COLOR_GREEN_FG, false)
    );

    /**
     * The FileCategory enum.
     */
    public enum FileCategory implements _ConfigFileCategory {
        /** Root directory node in the explorer tree. */
        ROOT_DIR,
        /** Logical category folder node (e.g. config, suites, parts). */
        CATEGORY_DIR,
        /** Global configuration properties file. */
        GLOBAL_CONFIG,
        /** Reusable configuration partial file under config/parts. */
        CONFIG_PART,
        /** Executable test suite file under suites. */
        TEST_SUITE,
        /** Reusable suite partial file under suites/parts. */
        SUITE_PART;

        @Override
        public boolean isConfigOnlyScope() {
            return GLOBAL_CONFIG == this || CONFIG_PART == this;
        }
    }

    /**
     * The FileNodeObject record.
     *
     * @param displayName the file or folder display name shown in the explorer tree
     * @param path        the filesystem path represented by this node
     * @param category    the logical category of this node
     * @param deletable   whether this file may be deleted from the UI
     */
    public record FileNodeObject(String displayName, Path path, FileCategory category, boolean deletable) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    private record BadgeStyle(String label, Color background, Color foreground) {
    }

    private record ScopeStyle(String text, Color background, Color foreground, boolean bordered) {
    }

    private record SplitFileName(String baseName, String extension) {
    }

    private final EditorState state = new EditorState();
    private final TreeExplorerControls treeExplorer;
    private final EditorHeaderControls headerControls;
    private final EditorViews editorViews;

    /**
     * Constructs a new ConfigTestsEditorPanel.
     */
    public ConfigTestsEditorPanel() {
        setLayout(new BorderLayout(0, 0));

        editorViews = new EditorViews(
                state::currentSelectedNode,
                this::onCellValidationMessage,
                this::onTableModelChanged,
                this::saveCurrentFormEntry,
                this::deleteSelectedEntry,
                this::onRawTextChanged
        );
        headerControls = new EditorHeaderControls(
                this::onReloadRequested,
                this::onToggleView,
                this::isSelectedFileSaved
        );
        final FileOperations fileOperations = new FileOperations(
                editorViews.centerPanel(),
                state,
                this::refreshTreeFromDisk,
                this::selectFileByName,
                this::promptToSaveIfModified,
                this::isSelectedFileSaved,
                this::isModified
        );
        treeExplorer = TreeExplorerControls.create(
                this::onTreeSelectionChanged,
                fileOperations::createNewFile,
                fileOperations::deleteSelectedFile,
                this::onRefreshTreeRequested,
                fileOperations::renameSelectedFile,
                fileOperations::duplicateSelectedFile
        );

        final JPanel sidebarPanel = treeExplorer.buildSidebarPanel();
        final JPanel editorAreaPanel = buildEditorAreaPanel();

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, editorAreaPanel);
        splitPane.setDividerLocation(SIDEBAR_WIDTH);
        splitPane.setResizeWeight(SPLIT_RESIZE_WEIGHT);
        add(splitPane, BorderLayout.CENTER);

        setupSaveKeyboardShortcut();
        refreshTreeFromDisk();
        treeExplorer.selectByFileName(COMMON_CONFIG_FILE);
    }

    private JPanel buildEditorAreaPanel() {
        final JPanel editorAreaPanel = new JPanel(new BorderLayout(0, EDITOR_GAP));
        editorAreaPanel.setBorder(BorderFactory.createEmptyBorder(EDITOR_PADDING, EDITOR_PADDING, EDITOR_PADDING, EDITOR_PADDING));
        editorAreaPanel.add(headerControls.buildTopHeaderPanel(), BorderLayout.PAGE_START);
        editorAreaPanel.add(editorViews.centerPanel(), BorderLayout.CENTER);
        return editorAreaPanel;
    }

    /**
     * Checks if the currently open file has unsaved modifications.
     *
     * @return true if modified, false otherwise
     */
    boolean isModified() {
        final boolean modified;
        if (null == state.currentSelectedNode()) {
            modified = false;
        } else {
            final String currentContent = editorViews.isRawViewShowing()
                    ? editorViews.getRawText()
                    : editorViews.generateFileContent();
            modified = !Objects.equals(state.originalFileContent(), currentContent);
        }
        return modified;
    }

    /**
     * Prompts the user to save changes if the currently open file has unsaved modifications.
     *
     * @return true if navigation/switching should proceed (saved or discarded), false if action was cancelled
     */
    boolean promptToSaveIfModified() {
        final boolean proceed;
        final FileNodeObject currentNode = state.currentSelectedNode();
        if (!isModified() || null == currentNode) {
            proceed = true;
        } else {
            final int result = JOptionPane.showConfirmDialog(
                    this,
                    s(MSG_UNSAVED_PREFIX, currentNode.displayName(), MSG_UNSAVED_SUFFIX, _LFD_, MSG_UNSAVED_PROMPT),
                    s(b(c(UNSAVED), c(CHANGES), IN), _SPACE_, currentNode.displayName()),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            proceed = JOptionPane.YES_OPTION == result ? isSelectedFileSaved() : JOptionPane.NO_OPTION == result;
        }
        return proceed;
    }

    /**
     * Performs refresh tree from disk.
     */
    void refreshTreeFromDisk() {
        state.setTreeSelectionEventsEnabled(false);
        try {
            treeExplorer.refreshFromDisk();
        } finally {
            state.setTreeSelectionEventsEnabled(true);
        }
    }

    /**
     * Performs get property table panel.
     *
     * @return the property table panel
     */
    PropertyTablePanel getPropertyTablePanel() {
        return editorViews.propertyTablePanel();
    }

    private void selectFileByName(final String fileName) {
        treeExplorer.selectByFileName(fileName);
    }

    private void setupSaveKeyboardShortcut() {
        final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        final KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, mask);

        final InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        final ActionMap actionMap = getActionMap();

        inputMap.put(saveKeyStroke, SAVE_ACTION_KEY);
        actionMap.put(SAVE_ACTION_KEY, new SaveFileAction());
    }

    private void onRefreshTreeRequested() {
        if (promptToSaveIfModified()) {
            refreshTreeFromDisk();
        }
    }

    private void onReloadRequested() {
        if (promptToSaveIfModified()) {
            loadSelectedFile();
        }
    }

    private void onToggleView() {
        if (headerControls.isRawViewSelected()) {
            syncTableToRawText();
            editorViews.showRawView();
        } else {
            syncRawTextToTable();
            editorViews.showFormView();
        }
    }

    private void onCellValidationMessage(final String message) {
        if (null == message) {
            editorViews.validateContainerScope(state.currentSelectedNode());
        } else {
            editorViews.showValidationWarning(s(ICON_WARNING, message));
        }
    }

    private void onTableModelChanged(final TableModelEvent event) {
        if (state.isFormEventsEnabled()) {
            final int row = event.getFirstRow();
            final int col = event.getColumn();
            if (COL_KEY == col || COL_VALUE == col) {
                recalculateRowScope(row);
            }
            updateHeaderForCurrentNode();
        }
    }

    private void recalculateRowScope(final int row) {
        final DefaultTableModel model = editorViews.tableModel();
        if (0 <= row && row < model.getRowCount()) {
            state.setFormEventsEnabled(false);
            try {
                final String key = (String) model.getValueAt(row, COL_KEY);
                final String val = (String) model.getValueAt(row, COL_VALUE);
                final FileNodeObject currentNode = state.currentSelectedNode();
                final _ConfigFileCategory cat = isNotNull(currentNode) ? currentNode.category() : null;
                final String newScope = EditorViews.determineScope(key, val, cat);
                final String oldScope = (String) model.getValueAt(row, COL_SCOPE);
                if (!Objects.equals(newScope, oldScope)) {
                    model.setValueAt(newScope, row, COL_SCOPE);
                }
                editorViews.validateContainerScope(currentNode);
            } finally {
                state.setFormEventsEnabled(true);
            }
        }
    }

    private void onRawTextChanged() {
        if (state.isFormEventsEnabled()) {
            updateHeaderForCurrentNode();
        }
    }

    private void updateHeaderForCurrentNode() {
        final FileNodeObject currentNode = state.currentSelectedNode();
        if (isNotNull(currentNode)) {
            headerControls.updateHeader(currentNode, isModified());
        }
    }

    private void onTreeSelectionChanged() {
        if (state.isTreeSelectionEventsEnabled()) {
            editorViews.stopOrCancelTableEditing();
            final FileNodeObject selected = treeExplorer.getSelectedFileNode();
            if (isNotNull(selected) && isFileNodeCategory(selected.category())
                    && !Objects.equals(state.currentSelectedNode(), selected)) {
                switchToFileNode(selected);
            }
        }
    }

    private void switchToFileNode(final FileNodeObject targetNode) {
        if (promptToSaveIfModified()) {
            state.setCurrentSelectedNode(targetNode);
            headerControls.updateHeader(targetNode, isModified());
            loadSelectedFile();
        } else {
            restoreTreeSelectionToCurrentNode();
        }
    }

    private void restoreTreeSelectionToCurrentNode() {
        state.setTreeSelectionEventsEnabled(false);
        try {
            final FileNodeObject currentNode = state.currentSelectedNode();
            if (isNotNull(currentNode)) {
                treeExplorer.selectByFileName(currentNode.displayName());
            }
        } finally {
            state.setTreeSelectionEventsEnabled(true);
        }
    }

    private void saveCurrentFormEntry() {
        final PropertyTablePanel panel = editorViews.propertyTablePanel();
        final String key = panel.getKeyField().getText().strip();
        final String val = panel.getValueField().getText().strip();
        if (key.isEmpty()) {
            JOptionPane.showMessageDialog(this, MSG_KEY_EMPTY, TITLE_VALIDATION_ERROR, JOptionPane.ERROR_MESSAGE);
        } else if (isFormEntryConfirmedForSave(key, val)) {
            applyFormEntryToTable(key, val);
        }
    }

    private boolean isFormEntryConfirmedForSave(final String key, final String val) {
        final FileNodeObject currentNode = state.currentSelectedNode();
        final _ConfigFileCategory category = isNotNull(currentNode) ? currentNode.category() : null;
        final String valErr = PropertyValidator.validateProperty(key, val, category);
        final boolean confirmed;
        if (null == valErr) {
            confirmed = true;
        } else {
            final int confirm = JOptionPane.showConfirmDialog(
                    this,
                    s(MSG_VALIDATION_PREFIX, key, _QUOTE_, _COLON_, _LFD_, valErr, _LFD_, _LFD_, MSG_VALIDATION_SAVE_PROMPT),
                    TITLE_VALIDATION_NOTICE,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            confirmed = JOptionPane.YES_OPTION == confirm;
        }
        return confirmed;
    }

    private void applyFormEntryToTable(final String key, final String val) {
        final FileNodeObject currentNode = state.currentSelectedNode();
        final _ConfigFileCategory category = isNotNull(currentNode) ? currentNode.category() : null;
        final String scope = EditorViews.determineScope(key, val, category);

        state.setFormEventsEnabled(false);
        try {
            editorViews.saveFormEntry(scope, key, val);
        } finally {
            state.setFormEventsEnabled(true);
        }

        editorViews.validateContainerScope(currentNode);
        updateHeaderForCurrentNode();
    }

    private void deleteSelectedEntry() {
        if (editorViews.isSelectedRowDeleted()) {
            editorViews.validateContainerScope(state.currentSelectedNode());
            updateHeaderForCurrentNode();
        }
    }

    private void loadSelectedFile() {
        final FileNodeObject currentNode = state.currentSelectedNode();
        if (isNotNull(currentNode)) {
            editorViews.stopOrCancelTableEditing();
            editorViews.updatePartsMode(currentNode);
            state.setFormEventsEnabled(false);
            try {
                editorViews.clearTableAndForm();
                if (Files.exists(currentNode.path())) {
                    final List<String> lines = Files.readAllLines(currentNode.path(), StandardCharsets.UTF_8);
                    editorViews.setRawText(String.join(s(_LFD_), lines));
                    editorViews.loadLinesIntoTable(lines, currentNode.category());
                }
            } catch (final IOException e) {
                JOptionPane.showMessageDialog(this, sn(MSG_ERR_LOADING, e.getMessage()), TITLE_FILE_ERROR, JOptionPane.ERROR_MESSAGE);
            } finally {
                state.setFormEventsEnabled(true);
            }

            state.setOriginalFileContent(editorViews.generateFileContent());
            headerControls.updateHeader(currentNode, isModified());
            editorViews.validateContainerScope(currentNode);
        }
    }

    private boolean isSelectedFileSaved() {
        boolean saved = false;
        final FileNodeObject currentNode = state.currentSelectedNode();
        if (isNotNull(currentNode)) {
            if (editorViews.isRawViewShowing()) {
                syncRawTextToTable();
            }
            editorViews.validateContainerScope(currentNode);
            final String content = editorViews.generateFileContent();
            saved = isContentWrittenToFile(currentNode, content);
            if (saved) {
                JOptionPane.showMessageDialog(
                        this,
                        s(c(SAVED), _SPACE_, currentNode.displayName(), _SPACE_, SUCCESSFULLY, _DOT_),
                        TITLE_FILE_SAVED,
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        }
        return saved;
    }

    private boolean isContentWrittenToFile(final FileNodeObject currentNode, final String content) {
        boolean written = false;
        try {
            final Path parent = currentNode.path().getParent();
            if (isNotNull(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(currentNode.path(), content, StandardCharsets.UTF_8);
            editorViews.setRawText(content);
            state.setOriginalFileContent(content);
            headerControls.updateHeader(currentNode, isModified());
            written = true;
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(this, sn(MSG_ERR_SAVING, e.getMessage()), TITLE_SAVE_ERROR, JOptionPane.ERROR_MESSAGE);
        }
        return written;
    }

    private void syncTableToRawText() {
        editorViews.setRawText(editorViews.generateFileContent());
    }

    private void syncRawTextToTable() {
        editorViews.stopOrCancelTableEditing();
        final FileNodeObject currentNode = state.currentSelectedNode();
        state.setFormEventsEnabled(false);
        try {
            final _ConfigFileCategory category = isNotNull(currentNode) ? currentNode.category() : null;
            editorViews.loadRawTextIntoTable(category);
        } finally {
            state.setFormEventsEnabled(true);
        }
        editorViews.validateContainerScope(currentNode);
    }

    private static boolean isFileNodeCategory(final FileCategory category) {
        return FileCategory.CATEGORY_DIR != category && FileCategory.ROOT_DIR != category;
    }

    private static final class EditorState {

        private FileNodeObject currentSelectedNode = null;
        private String originalFileContent = EMPTY_STRING;
        private boolean treeSelectionEventsEnabled = true;
        private boolean formEventsEnabled = true;

        private FileNodeObject currentSelectedNode() {
            return currentSelectedNode;
        }

        private void setCurrentSelectedNode(final FileNodeObject node) {
            currentSelectedNode = node;
        }

        private String originalFileContent() {
            return originalFileContent;
        }

        private void setOriginalFileContent(final String content) {
            originalFileContent = content;
        }

        private boolean isTreeSelectionEventsEnabled() {
            return treeSelectionEventsEnabled;
        }

        private void setTreeSelectionEventsEnabled(final boolean enabled) {
            treeSelectionEventsEnabled = enabled;
        }

        private boolean isFormEventsEnabled() {
            return formEventsEnabled;
        }

        private void setFormEventsEnabled(final boolean enabled) {
            formEventsEnabled = enabled;
        }
    }

    private record FileOperations(
            Component parentComponent,
            EditorState state,
            Runnable onRefresh,
            Consumer<? super String> onSelectFile,
            BooleanSupplier onPromptSave,
            BooleanSupplier onSave,
            BooleanSupplier isModified
    ) {

        private void createNewFile() {
            if (onPromptSave.getAsBoolean()) {
                final String[] types = {TYPE_TEST_SUITE, TYPE_SUITE_PART, TYPE_CONFIG_PART};
                final String selectedType = (String) JOptionPane.showInputDialog(
                        parentComponent,
                        MSG_SELECT_FILE_TYPE,
                        TITLE_NEW_FILE,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        types,
                        types[0]
                );
                if (isNotNull(selectedType)) {
                    promptAndCreateFileOfType(selectedType);
                }
            }
        }

        private void promptAndCreateFileOfType(final String selectedType) {
            final String rawName = JOptionPane.showInputDialog(
                    parentComponent, MSG_ENTER_FILE_NAME, TITLE_NEW_FILE, JOptionPane.PLAIN_MESSAGE
            );
            if (isNotNull(rawName) && !rawName.isBlank()) {
                final String cleanName = sanitizeFileName(rawName);
                final Path targetPath = resolveNewFilePath(selectedType, cleanName);
                if (Files.exists(targetPath)) {
                    JOptionPane.showMessageDialog(
                            parentComponent, s(MSG_FILE_EXISTS, targetPath.getFileName()), TITLE_ERROR, JOptionPane.ERROR_MESSAGE
                    );
                } else {
                    writeNewFileAndSelect(targetPath, cleanName);
                }
            }
        }

        private void writeNewFileAndSelect(final Path targetPath, final String cleanName) {
            try {
                final Path parent = targetPath.getParent();
                if (isNotNull(parent)) {
                    Files.createDirectories(parent);
                }
                Files.writeString(targetPath, s(_NUMBR_, _SPACE_, cleanName, _LFD_), StandardCharsets.UTF_8);
                onRefresh.run();
                onSelectFile.accept(targetPath.getFileName().toString());
            } catch (final IOException e) {
                JOptionPane.showMessageDialog(
                        parentComponent, sn(MSG_ERR_CREATING, e.getMessage()), TITLE_ERROR, JOptionPane.ERROR_MESSAGE
                );
            }
        }

        private void deleteSelectedFile() {
            final FileNodeObject currentNode = state.currentSelectedNode();
            if (isNotNull(currentNode)) {
                if (currentNode.deletable()) {
                    confirmAndDeleteFile(currentNode);
                } else {
                    JOptionPane.showMessageDialog(
                            parentComponent,
                            s(MSG_CANNOT_DELETE_GLOBAL, currentNode.displayName()),
                            TITLE_PROTECTED_FILE,
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        }

        private void confirmAndDeleteFile(final FileNodeObject currentNode) {
            final int confirm = JOptionPane.showConfirmDialog(
                    parentComponent,
                    s(MSG_CONFIRM_DELETE_PREFIX, currentNode.displayName(), QUS_MRK),
                    TITLE_CONFIRM_DELETE,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (JOptionPane.YES_OPTION == confirm) {
                boolean deleted = false;
                try {
                    Files.deleteIfExists(currentNode.path());
                    onRefresh.run();
                    onSelectFile.accept(COMMON_CONFIG_FILE);
                    deleted = true;
                } catch (final IOException e) {
                    JOptionPane.showMessageDialog(
                            parentComponent, sn(MSG_ERR_DELETING, e.getMessage()), TITLE_ERROR, JOptionPane.ERROR_MESSAGE
                    );
                }
                if (deleted) {
                    JOptionPane.showMessageDialog(
                            parentComponent, MSG_FILE_DELETED, TITLE_DELETED, JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        }

        private void renameSelectedFile() {
            final FileNodeObject currentNode = state.currentSelectedNode();
            if (isNotNull(currentNode)) {
                if (isProtectedConfigFile(currentNode)) {
                    JOptionPane.showMessageDialog(
                            parentComponent, MSG_CANNOT_RENAME_COMMON, TITLE_PROTECTED_FILE, JOptionPane.WARNING_MESSAGE
                    );
                } else if (onPromptSave.getAsBoolean()) {
                    executeRename(currentNode);
                }
            }
        }

        private void executeRename(final FileNodeObject currentNode) {
            final SplitFileName parts = splitFileName(currentNode.displayName());
            final Path newPath = promptForValidatedSiblingPath(
                    currentNode.path(), parts.extension(), MSG_ENTER_NEW_NAME, TITLE_RENAME_FILE, parts.baseName()
            );
            if (isNotNull(newPath)) {
                final String newFileName = newPath.getFileName().toString();
                boolean renamed = false;
                try {
                    Files.move(currentNode.path(), newPath);
                    onRefresh.run();
                    onSelectFile.accept(newFileName);
                    renamed = true;
                } catch (final IOException e) {
                    JOptionPane.showMessageDialog(
                            parentComponent, sn(MSG_ERR_RENAMING, e.getMessage()), TITLE_ERROR, JOptionPane.ERROR_MESSAGE
                    );
                }
                if (renamed) {
                    JOptionPane.showMessageDialog(
                            parentComponent, s(MSG_FILE_RENAMED, newFileName), TITLE_RENAMED, JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        }

        private void duplicateSelectedFile() {
            final FileNodeObject currentNode = state.currentSelectedNode();
            if (isNotNull(currentNode)) {
                if (isProtectedConfigFile(currentNode)) {
                    JOptionPane.showMessageDialog(
                            parentComponent, MSG_CANNOT_DUPLICATE_COMMON, TITLE_PROTECTED_FILE, JOptionPane.WARNING_MESSAGE
                    );
                } else {
                    if (isModified.getAsBoolean()) {
                        onSave.getAsBoolean();
                    }
                    executeDuplicate(currentNode);
                }
            }
        }

        private void executeDuplicate(final FileNodeObject currentNode) {
            final SplitFileName parts = splitFileName(currentNode.displayName());
            final Path newPath = promptForValidatedSiblingPath(
                    currentNode.path(),
                    parts.extension(),
                    MSG_ENTER_DUPLICATE_NAME,
                    TITLE_DUPLICATE_FILE,
                    s(parts.baseName(), SUFFIX_COPY)
            );
            if (isNotNull(newPath)) {
                final String newFileName = newPath.getFileName().toString();
                boolean duplicated = false;
                try {
                    Files.copy(currentNode.path(), newPath);
                    onRefresh.run();
                    onSelectFile.accept(newFileName);
                    duplicated = true;
                } catch (final IOException e) {
                    JOptionPane.showMessageDialog(
                            parentComponent, sn(MSG_ERR_DUPLICATING, e.getMessage()), TITLE_ERROR, JOptionPane.ERROR_MESSAGE
                    );
                }
                if (duplicated) {
                    JOptionPane.showMessageDialog(
                            parentComponent, s(MSG_FILE_DUPLICATED, newFileName), TITLE_DUPLICATED, JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        }

        private Path promptForValidatedSiblingPath(final Path existingPath, final String extension,
                                                   final String promptMessage, final String dialogTitle,
                                                   final String initialBaseName) {
            Path validatedPath = null;
            final String rawName = (String) JOptionPane.showInputDialog(
                    parentComponent, promptMessage, dialogTitle, JOptionPane.PLAIN_MESSAGE, null, null, initialBaseName
            );
            if (isNotNull(rawName) && !rawName.isBlank()) {
                final String cleanName = sanitizeFileName(rawName);
                if (cleanName.isBlank()) {
                    JOptionPane.showMessageDialog(
                            parentComponent, MSG_INVALID_FILENAME, TITLE_ERROR, JOptionPane.ERROR_MESSAGE
                    );
                } else {
                    final String newFileName = s(cleanName, extension);
                    final Path candidatePath = existingPath.resolveSibling(newFileName);
                    if (Files.exists(candidatePath)) {
                        JOptionPane.showMessageDialog(
                                parentComponent, s(MSG_FILE_EXISTS, newFileName), TITLE_ERROR, JOptionPane.ERROR_MESSAGE
                        );
                    } else {
                        validatedPath = candidatePath;
                    }
                }
            }
            return validatedPath;
        }

        private static Path resolveNewFilePath(final String selectedType, final String cleanName) {
            final Path targetPath;
            if (selectedType.contains(SUITES_PARTS_PREFIX)) {
                targetPath = Path.of(_SUITES_, PARTS_DIR_NAME, s(cleanName, TEST_PROPERTIES_PARTS_EXTENSION));
            } else if (selectedType.contains(SUITES_PREFIX)) {
                targetPath = Path.of(_SUITES_, s(cleanName, SUITE_PROPERTIES_EXTENSION));
            } else {
                targetPath = Path.of(_CONFIG_, PARTS_DIR_NAME, s(cleanName, TEST_PROPERTIES_PARTS_EXTENSION));
            }
            return targetPath;
        }

        private static SplitFileName splitFileName(final String fileName) {
            final int dotIdx = fileName.lastIndexOf(_DOT_);
            final SplitFileName result;
            if (0 <= dotIdx) {
                result = new SplitFileName(fileName.substring(0, dotIdx), fileName.substring(dotIdx));
            } else {
                result = new SplitFileName(fileName, EMPTY_STRING);
            }
            return result;
        }

        private static String sanitizeFileName(final String rawName) {
            return INVALID_FILENAME_CHARS.matcher(rawName.strip()).replaceAll(EMPTY_STRING);
        }

        private static boolean isProtectedConfigFile(final FileNodeObject fileObj) {
            return Objects.equals(COMMON_CONFIG_FILE, fileObj.displayName());
        }
    }

    private record TreeExplorerControls(
            DefaultMutableTreeNode treeRootNode,
            DefaultTreeModel treeModel,
            JTree fileTree,
            Runnable onNewFile,
            Runnable onDeleteFile,
            Runnable onRefresh
    ) {

        private static TreeExplorerControls create(final Runnable onSelectionChanged,
                                                   final Runnable onNewFile,
                                                   final Runnable onDeleteFile,
                                                   final Runnable onRefresh,
                                                   final Runnable onRename,
                                                   final Runnable onDuplicate) {
            final DefaultMutableTreeNode root = new DefaultMutableTreeNode(RESOURCES);
            final DefaultTreeModel model = new DefaultTreeModel(root);
            final JTree tree = new JTree(model);
            tree.setCellRenderer(new FileTreeCellRenderer());
            tree.getSelectionModel().addTreeSelectionListener(_ -> onSelectionChanged.run());
            tree.setComponentPopupMenu(buildTreePopupMenu(tree, onRename, onDuplicate, onDeleteFile));
            tree.addMouseListener(new TreeRightClickListener(tree));
            return new TreeExplorerControls(root, model, tree, onNewFile, onDeleteFile, onRefresh);
        }

        private static JPopupMenu buildTreePopupMenu(final JTree tree,
                                                     final Runnable onRename,
                                                     final Runnable onDuplicate,
                                                     final Runnable onDelete) {
            final JPopupMenu popupMenu = new JPopupMenu();
            final JMenuItem renameItem = new JMenuItem(LABEL_MENU_RENAME);
            final JMenuItem duplicateItem = new JMenuItem(LABEL_MENU_DUPLICATE);
            final JMenuItem deleteItem = new JMenuItem(c(DELETE));

            renameItem.addActionListener(_ -> onRename.run());
            duplicateItem.addActionListener(_ -> onDuplicate.run());
            deleteItem.addActionListener(_ -> onDelete.run());

            popupMenu.add(renameItem);
            popupMenu.add(duplicateItem);
            popupMenu.addSeparator();
            popupMenu.add(deleteItem);
            popupMenu.addPopupMenuListener(new TreePopupMenuListener(tree, renameItem, duplicateItem, deleteItem));
            return popupMenu;
        }

        private JPanel buildSidebarPanel() {
            final JScrollPane treeScrollPane = new JScrollPane(fileTree);
            treeScrollPane.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));

            final JPanel sidebarPanel = new JPanel(new BorderLayout());
            sidebarPanel.setBorder(BorderFactory.createTitledBorder(LABEL_PROJECT_EXPLORER));
            sidebarPanel.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
            sidebarPanel.add(buildTreeToolbar(), BorderLayout.PAGE_START);
            sidebarPanel.add(treeScrollPane, BorderLayout.CENTER);
            return sidebarPanel;
        }

        private JPanel buildTreeToolbar() {
            final JPanel treeToolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, TOOLBAR_HGAP, TOOLBAR_VGAP));
            treeToolbar.add(createToolbarButton(LABEL_NEW_FILE_BUTTON, TOOLTIP_NEW_FILE, onNewFile));
            treeToolbar.add(createToolbarButton(LABEL_DELETE_FILE_BUTTON, TOOLTIP_DELETE_FILE, onDeleteFile));
            treeToolbar.add(createToolbarButton(LABEL_REFRESH_BUTTON, TOOLTIP_REFRESH, onRefresh));
            return treeToolbar;
        }

        private static JButton createToolbarButton(final String text, final String tooltip, final Runnable action) {
            final JButton button = new JButton(text);
            button.setFont(FONT_SANS_12);
            button.setToolTipText(tooltip);
            button.addActionListener(_ -> action.run());
            return button;
        }

        private FileNodeObject getSelectedFileNode() {
            FileNodeObject result = null;
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
            if (isNotNull(node) && node.getUserObject() instanceof final FileNodeObject fileObj) {
                result = fileObj;
            }
            return result;
        }

        private void refreshFromDisk() {
            treeRootNode.removeAllChildren();
            treeRootNode.add(buildConfigFolderNode());

            final Path suitesDir = Path.of(_SUITES_);
            if (Files.isDirectory(suitesDir)) {
                treeRootNode.add(buildSuitesFolderNode(suitesDir));
            }

            treeModel.reload();
            for (int i = 0; i < fileTree.getRowCount(); i++) {
                fileTree.expandRow(i);
            }
        }

        private static DefaultMutableTreeNode buildConfigFolderNode() {
            final DefaultMutableTreeNode configFolder = new DefaultMutableTreeNode(
                    new FileNodeObject(CONFIG, Path.of(_CONFIG_), FileCategory.CATEGORY_DIR, false)
            );
            addGlobalConfigIfPresent(configFolder, COMMON_CONFIG_FILE);
            addGlobalConfigIfPresent(configFolder, PROPERTY_HELP_FILE);

            final Path partsDir = Path.of(_CONFIG_, PARTS_DIR_NAME);
            if (Files.isDirectory(partsDir)) {
                final DefaultMutableTreeNode partsFolder = new DefaultMutableTreeNode(
                        new FileNodeObject(PARTS_DIR_NAME, partsDir, FileCategory.CATEGORY_DIR, false)
                );
                addMatchingFiles(partsDir, partsFolder, FileCategory.CONFIG_PART, TEST_PROPERTIES_PARTS_EXTENSION);
                configFolder.add(partsFolder);
            }
            return configFolder;
        }

        private static void addGlobalConfigIfPresent(final DefaultMutableTreeNode parent, final String fileName) {
            final Path path = Path.of(_CONFIG_, fileName);
            if (Files.exists(path)) {
                parent.add(new DefaultMutableTreeNode(
                        new FileNodeObject(fileName, path, FileCategory.GLOBAL_CONFIG, false)
                ));
            }
        }

        private static DefaultMutableTreeNode buildSuitesFolderNode(final Path suitesDir) {
            final DefaultMutableTreeNode suitesFolder = new DefaultMutableTreeNode(
                    new FileNodeObject(SUITES, suitesDir, FileCategory.CATEGORY_DIR, false)
            );
            addMatchingFiles(suitesDir, suitesFolder, FileCategory.TEST_SUITE, SUITE_PROPERTIES_EXTENSION);

            final Path suitePartsDir = Path.of(_SUITES_, PARTS_DIR_NAME);
            if (Files.isDirectory(suitePartsDir)) {
                final DefaultMutableTreeNode suitePartsFolder = new DefaultMutableTreeNode(
                        new FileNodeObject(PARTS_DIR_NAME, suitePartsDir, FileCategory.CATEGORY_DIR, false)
                );
                addMatchingFiles(suitePartsDir, suitePartsFolder, FileCategory.SUITE_PART,
                        TEST_PROPERTIES_PARTS_EXTENSION, SUITE_PROPERTIES_EXTENSION);
                suitesFolder.add(suitePartsFolder);
            }
            return suitesFolder;
        }

        private static void addMatchingFiles(final Path dir, final DefaultMutableTreeNode folderNode,
                                             final FileCategory category, final String... extensions) {
            try (final Stream<Path> stream = Files.list(dir)) {
                stream.filter(path -> hasAnyExtension(path, extensions))
                        .sorted()
                        .forEach(path -> folderNode.add(new DefaultMutableTreeNode(
                                new FileNodeObject(path.getFileName().toString(), path, category, true)
                        )));
            } catch (final IOException _) {
                // Directory listing failures leave folder empty
            }
        }

        private static boolean hasAnyExtension(final Path path, final String... extensions) {
            final String pathStr = path.toString();
            return Stream.of(extensions).anyMatch(pathStr::endsWith);
        }

        private void selectByFileName(final String targetFileName) {
            final DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            isNodeFoundAndSelected(root, targetFileName);
        }

        private boolean isNodeFoundAndSelected(final DefaultMutableTreeNode node, final String targetFileName) {
            boolean found = false;
            if (node.getUserObject() instanceof final FileNodeObject obj && Objects.equals(targetFileName, obj.displayName())) {
                final TreePath path = new TreePath(node.getPath());
                fileTree.setSelectionPath(path);
                fileTree.scrollPathToVisible(path);
                found = true;
            } else {
                for (int i = 0; i < node.getChildCount() && !found; i++) {
                    found = isNodeFoundAndSelected((DefaultMutableTreeNode) node.getChildAt(i), targetFileName);
                }
            }
            return found;
        }
    }

    private static final class EditorHeaderControls {

        private final JLabel activeFileLabel = new JLabel(LABEL_NO_FILE_SELECTED);
        private final JLabel activeFileBadge = new JLabel();
        private final JToggleButton toggleViewBtn = new JToggleButton(LABEL_RAW_TEXT_VIEW);
        private final Runnable onReload;
        private final Runnable onSave;

        private EditorHeaderControls(final Runnable onReload, final Runnable onToggleView, final Runnable onSave) {
            this.onReload = onReload;
            this.onSave = onSave;
            toggleViewBtn.addActionListener(_ -> onToggleView.run());
        }

        private JPanel buildTopHeaderPanel() {
            activeFileLabel.setFont(FONT_SANS_BOLD_13);
            activeFileBadge.setOpaque(true);
            activeFileBadge.setFont(FONT_SANS_BOLD_10);
            activeFileBadge.setBorder(BorderFactory.createEmptyBorder(BADGE_VPAD, BADGE_HPAD, BADGE_VPAD, BADGE_HPAD));

            final JPanel activeFileHeader = new JPanel(new FlowLayout(FlowLayout.LEADING, HEADER_HGAP, HEADER_VGAP));
            activeFileHeader.add(activeFileBadge);
            activeFileHeader.add(activeFileLabel);

            final JPanel topHeaderPanel = new JPanel(new BorderLayout());
            topHeaderPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            topHeaderPanel.add(activeFileHeader, BorderLayout.LINE_START);
            topHeaderPanel.add(buildEditorToolbar(), BorderLayout.LINE_END);
            return topHeaderPanel;
        }

        private JPanel buildEditorToolbar() {
            final JPanel editorToolbar = new JPanel(new FlowLayout(FlowLayout.TRAILING, TOOLBAR_HGAP, TOOLBAR_VGAP));

            final JButton saveFileBtn = new JButton(LABEL_SAVE_BUTTON);
            saveFileBtn.setFont(FONT_SANS_BOLD_12);
            saveFileBtn.setToolTipText(TOOLTIP_SAVE_FILE);
            saveFileBtn.addActionListener(_ -> onSave.run());

            final JButton reloadBtn = new JButton(c(RELOAD));
            reloadBtn.addActionListener(_ -> onReload.run());

            editorToolbar.add(reloadBtn);
            editorToolbar.add(toggleViewBtn);
            editorToolbar.add(saveFileBtn);
            return editorToolbar;
        }

        private boolean isRawViewSelected() {
            return toggleViewBtn.isSelected();
        }

        private void updateHeader(final FileNodeObject fileObj, final boolean modified) {
            if (null == fileObj) {
                activeFileLabel.setText(LABEL_NO_FILE_SELECTED);
                activeFileBadge.setText(EMPTY_STRING);
                activeFileBadge.setVisible(false);
            } else {
                activeFileLabel.setText(s(fileObj.path().toString(), modified ? SUFFIX_MODIFIED_STAR : EMPTY_STRING));
                final BadgeStyle style = resolveBadgeStyle(fileObj.category(), modified);
                activeFileBadge.setBackground(style.background());
                activeFileBadge.setForeground(style.foreground());
                activeFileBadge.setText(modified ? s(style.label(), SUFFIX_MODIFIED_BADGE) : style.label());
                activeFileBadge.setVisible(true);
            }
        }

        private static BadgeStyle resolveBadgeStyle(final FileCategory category, final boolean modified) {
            return switch (category) {
                case GLOBAL_CONFIG -> new BadgeStyle(
                        BADGE_GLOBAL_CONFIG,
                        modified ? COLOR_AMBER_BG : COLOR_BLUE_BG,
                        modified ? COLOR_AMBER_FG : COLOR_BLUE_FG
                );
                case CONFIG_PART -> new BadgeStyle(
                        BADGE_CONFIG_PART,
                        modified ? COLOR_AMBER_BG : COLOR_GREEN_BG,
                        modified ? COLOR_AMBER_FG : COLOR_GREEN_FG
                );
                case TEST_SUITE -> new BadgeStyle(BADGE_TEST_SUITE, COLOR_AMBER_BG, COLOR_AMBER_FG);
                case SUITE_PART -> new BadgeStyle(
                        BADGE_SUITE_PART,
                        modified ? COLOR_AMBER_BG : COLOR_INDIGO_BG,
                        modified ? COLOR_AMBER_FG : COLOR_INDIGO_FG
                );
                case ROOT_DIR, CATEGORY_DIR -> new BadgeStyle(BADGE_FILE, Color.LIGHT_GRAY, Color.BLACK);
            };
        }
    }

    private static final class EditorViews {

        private final CardLayout cardLayout = new CardLayout();
        private final JPanel centerPanel = new JPanel(cardLayout);
        private final JTextArea rawTextArea = new JTextArea();
        private final JLabel scopeWarningLabel = new JLabel();
        private final DefaultTableModel tableModel;
        private final PropertyTablePanel propertyTablePanel;

        private EditorViews(final Supplier<FileNodeObject> currentNodeSupplier,
                            final Consumer<String> cellValidationConsumer,
                            final TableModelListener tableModelListener,
                            final Runnable onFormSave,
                            final Runnable onFormDelete,
                            final Runnable onRawTextChanged) {
            final String[] columns = {COL_HEADER_SCOPE, COL_HEADER_KEY, COL_HEADER_VALUE};
            tableModel = new PropertiesTableModel(columns);
            propertyTablePanel = new PropertyTablePanel(tableModel, COL_KEY, COL_VALUE, false, null);
            propertyTablePanel.setSearchAndFiltersVisible(false);
            propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.CONFIG_ONLY);
            propertyTablePanel.setFormSaveAction(_ -> onFormSave.run());
            propertyTablePanel.setFormDeleteAction(_ -> onFormDelete.run());

            configureTable(currentNodeSupplier, new ValidatingCellEditor(COL_KEY, cellValidationConsumer));
            tableModel.addTableModelListener(tableModelListener);
            configureScopeWarningBanner();
            configureRawTextArea(onRawTextChanged);

            final JPanel formViewPanel = new JPanel(new BorderLayout(0, FORM_VGAP));
            formViewPanel.add(scopeWarningLabel, BorderLayout.PAGE_START);
            formViewPanel.add(propertyTablePanel, BorderLayout.CENTER);

            ContextMenu.attach(
                    (JTextComponent) propertyTablePanel.getKeyField().getEditor().getEditorComponent(),
                    propertyTablePanel.getValueField(),
                    rawTextArea
            );

            centerPanel.add(formViewPanel, CARD_FORM);
            centerPanel.add(new JScrollPane(rawTextArea), CARD_RAW);
        }

        private void configureTable(final Supplier<FileNodeObject> currentNodeSupplier,
                                    final TableCellEditor valueCellEditor) {
            final JTable table = propertyTablePanel.getTable();
            table.getColumnModel().getColumn(COL_SCOPE).setMaxWidth(SCOPE_COLUMN_MAX_WIDTH);
            table.getColumnModel().getColumn(COL_SCOPE).setCellRenderer(new ScopeCellRenderer());
            final TableCellRenderer propRenderer = new PropertyCellRenderer(tableModel, currentNodeSupplier);
            table.getColumnModel().getColumn(COL_KEY).setCellRenderer(propRenderer);
            table.getColumnModel().getColumn(COL_VALUE).setCellRenderer(propRenderer);
            table.getColumnModel().getColumn(COL_VALUE).setCellEditor(valueCellEditor);
        }

        private void configureScopeWarningBanner() {
            scopeWarningLabel.setOpaque(true);
            scopeWarningLabel.setBackground(COLOR_ERROR_BG);
            scopeWarningLabel.setForeground(COLOR_ERROR_FG);
            scopeWarningLabel.setBorder(BorderFactory.createCompoundBorder(
                    ERROR_LINE_BORDER,
                    BorderFactory.createEmptyBorder(WARNING_VPAD, WARNING_HPAD, WARNING_VPAD, WARNING_HPAD)
            ));
            scopeWarningLabel.setFont(FONT_SANS_BOLD_12);
            scopeWarningLabel.setVisible(false);
        }

        private void configureRawTextArea(final Runnable onRawTextChanged) {
            rawTextArea.setFont(FONT_MONO_12);
            rawTextArea.getDocument().addDocumentListener(new RawTextDocumentListener(onRawTextChanged));
        }

        private JPanel centerPanel() {
            return centerPanel;
        }

        private DefaultTableModel tableModel() {
            return tableModel;
        }

        private PropertyTablePanel propertyTablePanel() {
            return propertyTablePanel;
        }

        private void showRawView() {
            cardLayout.show(centerPanel, CARD_RAW);
        }

        private void showFormView() {
            cardLayout.show(centerPanel, CARD_FORM);
        }

        private boolean isRawViewShowing() {
            return rawTextArea.isShowing();
        }

        private String getRawText() {
            return rawTextArea.getText();
        }

        private void setRawText(final String text) {
            rawTextArea.setText(text);
        }

        private void showValidationWarning(final String text) {
            scopeWarningLabel.setText(text);
            scopeWarningLabel.setVisible(true);
        }

        private void clearTableAndForm() {
            tableModel.setRowCount(0);
            propertyTablePanel.clearForm();
        }

        private void stopOrCancelTableEditing() {
            final JTable table = propertyTablePanel.getTable();
            if (isNotNull(table) && table.isEditing()) {
                final TableCellEditor editor = table.getCellEditor();
                if (isNotNull(editor)) {
                    editor.stopCellEditing();
                    if (table.isEditing()) {
                        editor.cancelCellEditing();
                    }
                }
            }
        }

        private void saveFormEntry(final String scope, final String key, final String val) {
            final JTable table = propertyTablePanel.getTable();
            final int selectedViewRow = table.getSelectedRow();
            if (0 <= selectedViewRow) {
                final int modelRow = table.convertRowIndexToModel(selectedViewRow);
                if (0 <= modelRow && modelRow < tableModel.getRowCount()) {
                    tableModel.setValueAt(scope, modelRow, COL_SCOPE);
                    tableModel.setValueAt(key, modelRow, COL_KEY);
                    tableModel.setValueAt(val, modelRow, COL_VALUE);
                }
            } else {
                addPropertyRow(scope, key, val);
                final int modelRow = tableModel.getRowCount() - 1;
                final int viewRow = table.convertRowIndexToView(modelRow);
                if (0 <= viewRow) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }
            }
        }

        private boolean isSelectedRowDeleted() {
            boolean deleted = false;
            final JTable table = propertyTablePanel.getTable();
            final int selectedRow = table.getSelectedRow();
            if (0 <= selectedRow && selectedRow < tableModel.getRowCount()) {
                tableModel.removeRow(table.convertRowIndexToModel(selectedRow));
                propertyTablePanel.clearForm();
                deleted = true;
            }
            return deleted;
        }

        private void validateContainerScope(final FileNodeObject currentNode) {
            if (isNotNull(currentNode)) {
                final FileCategory category = currentNode.category();
                final List<String> invalidDetails = new ArrayList<>(INITIAL_INVALID_LIST_CAPACITY);

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    final String key = (String) tableModel.getValueAt(i, COL_KEY);
                    final String val = (String) tableModel.getValueAt(i, COL_VALUE);
                    final String err = PropertyValidator.validateProperty(key, val, category);
                    if (isNotNull(err)) {
                        invalidDetails.add(s(c(ROW), _SPACE_, i + 1, _SPACE_, OPN_BRK, _QUOTE_, key, _QUOTE_, CLS_BRK, _COLON_, _SPACE_, err));
                    }
                }

                if (invalidDetails.isEmpty()) {
                    scopeWarningLabel.setVisible(false);
                } else {
                    final String countStr = 1 == invalidDetails.size()
                            ? b(s(_1_), INVALID, PROPERTY)
                            : s(invalidDetails.size(), _SPACE_, b(INVALID, WORD_PROPERTIES));
                    scopeWarningLabel.setText(s(
                            ICON_WARNING, countStr, _SPACE_, b(FOUND, IN), _SPACE_, currentNode.displayName(),
                            _COLON_, _SPACE_, invalidDetails.getFirst(), MSG_INVALID_PRESERVED
                    ));
                    scopeWarningLabel.setVisible(true);
                }
            }
        }

        private String generateFileContent() {
            final StringBuilder sb = new StringBuilder(INITIAL_CONTENT_CAPACITY);
            sb.append(GENERATED_FILE_HEADER).append(_LFD_);
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                final String key = (String) tableModel.getValueAt(i, COL_KEY);
                final String val = (String) tableModel.getValueAt(i, COL_VALUE);
                sb.append(key).append(_EQUAL_).append(val).append(_LFD_);
            }
            return sb.toString();
        }

        private void loadRawTextIntoTable(final _ConfigFileCategory category) {
            tableModel.setRowCount(0);
            final String text = rawTextArea.getText();
            if (isNotNull(text)) {
                loadLinesIntoTable(List.of(text.split(s(_LFD_))), category);
            }
        }

        private void loadLinesIntoTable(final Iterable<String> lines, final _ConfigFileCategory category) {
            for (final String line : lines) {
                parseAndAddLine(line, category);
            }
        }

        private void parseAndAddLine(final String line, final _ConfigFileCategory category) {
            final String trimmed = line.strip();
            if (!trimmed.isEmpty() && _NUMBR_ != trimmed.charAt(0)) {
                final int eqIdx = trimmed.indexOf(_EQUAL_);
                if (0 < eqIdx) {
                    final String key = trimmed.substring(0, eqIdx).strip();
                    final String val = trimmed.substring(eqIdx + 1).strip();
                    final String scope = determineScope(key, val, category);
                    addPropertyRow(scope, key, val);
                }
            }
        }

        private void addPropertyRow(final String scope, final String key, final String val) {
            tableModel.addRow(new Object[]{scope, key, val});
        }

        private void updatePartsMode(final FileNodeObject currentNode) {
            final PropertyTablePanel.PartsMode mode;
            if (null == currentNode) {
                mode = PropertyTablePanel.PartsMode.CONFIG_ONLY;
            } else {
                mode = switch (currentNode.category()) {
                    case GLOBAL_CONFIG, CONFIG_PART -> PropertyTablePanel.PartsMode.CONFIG_ONLY;
                    case TEST_SUITE, SUITE_PART -> PropertyTablePanel.PartsMode.SUITES_ONLY;
                    case ROOT_DIR, CATEGORY_DIR -> resolvePartsModeByPath(currentNode.path());
                };
            }
            propertyTablePanel.setPartsMode(mode);
        }

        private static PropertyTablePanel.PartsMode resolvePartsModeByPath(final Path path) {
            final String pathStr = path.toString().replace(REW_SOL, _SLASH_);
            final boolean isConfigPath = pathStr.contains(CONFIG_SEGMENT)
                    || pathStr.endsWith(CONFIG_SUFFIX)
                    || path.startsWith(Path.of(_CONFIG_));
            return isConfigPath ? PropertyTablePanel.PartsMode.CONFIG_ONLY : PropertyTablePanel.PartsMode.SUITES_ONLY;
        }

        private static String determineScope(final String key, final String val, final _ConfigFileCategory category) {
            final String result;
            if (null == key || key.isBlank() || isNotNull(PropertyValidator.validateProperty(key, val, category))) {
                result = SCOPE_INVALID;
            } else if (key.startsWith(PREFIX_DOT_INCLUDE) || key.startsWith(INCLUDE)) {
                result = SCOPE_INCLUDE;
            } else {
                result = classifyValidScope(key);
            }
            return result;
        }

        private static String classifyValidScope(final CharSequence key) {
            final String result;
            if (_DOT_ == key.charAt(0)) {
                result = SCOPE_CONFIG;
            } else if (_NUMBR_ == key.charAt(0)) {
                result = SCOPE_COMMENT;
            } else {
                result = SCOPE_SCENARIO;
            }
            return result;
        }
    }

    private static final class PropertiesTableModel extends DefaultTableModel {

        @Serial
        private static final long serialVersionUID = 1L;

        private PropertiesTableModel(final Object[] columnNames) {
            super(columnNames, 0);
        }

        @Override
        public boolean isCellEditable(final int row, final int column) {
            return COL_VALUE == column;
        }

        @Override
        public void setValueAt(final Object aValue, final int row, final int column) {
            final boolean validRow = 0 <= row && row < getRowCount();
            final boolean validColumn = 0 <= column && column < getColumnCount();
            if (validRow && validColumn) {
                super.setValueAt(aValue, row, column);
            }
        }
    }

    private record TreePopupMenuListener(
            JTree fileTree,
            JMenuItem renameItem,
            JMenuItem duplicateItem,
            JMenuItem deleteItem
    ) implements PopupMenuListener {

        @Override
        public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
            final boolean allow = isNotNull(node)
                    && node.getUserObject() instanceof final FileNodeObject fileObj
                    && isEditableFileNode(fileObj);
            renameItem.setEnabled(allow);
            duplicateItem.setEnabled(allow);
            deleteItem.setEnabled(allow);
        }

        @Override
        public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            // No action required
        }

        @Override
        public void popupMenuCanceled(final PopupMenuEvent e) {
            // No action required
        }

        private static boolean isEditableFileNode(final FileNodeObject fileObj) {
            return isFileNodeCategory(fileObj.category()) && !FileOperations.isProtectedConfigFile(fileObj);
        }
    }

    private static final class TreeRightClickListener extends MouseAdapter {

        private final JTree fileTree;

        private TreeRightClickListener(final JTree fileTree) {
            this.fileTree = fileTree;
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            maybeSelectOnRightClick(e);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            maybeSelectOnRightClick(e);
        }

        private void maybeSelectOnRightClick(final MouseEvent e) {
            if (e.isPopupTrigger()) {
                final TreePath path = fileTree.getClosestPathForLocation(e.getX(), e.getY());
                if (null != path) {
                    fileTree.setSelectionPath(path);
                }
            }
        }
    }

    private record RawTextDocumentListener(Runnable onChanged) implements DocumentListener {

        @Override
        public void insertUpdate(final DocumentEvent e) {
            onChanged.run();
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            onChanged.run();
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
            onChanged.run();
        }
    }

    private final class SaveFileAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(final ActionEvent e) {
            isSelectedFileSaved();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException(getClass().getName());
        }
    }

    private static final class FileTreeCellRenderer extends DefaultTreeCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
                                                      final boolean expanded, final boolean leaf, final int row,
                                                      final boolean hasFocus) {
            final JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof final DefaultMutableTreeNode node
                    && node.getUserObject() instanceof final FileNodeObject fileObj) {
                final String prefix = switch (fileObj.category()) {
                    case GLOBAL_CONFIG -> ICON_GEAR;
                    case CONFIG_PART, SUITE_PART -> ICON_PUZZLE;
                    case TEST_SUITE -> ICON_TEST_TUBE;
                    case CATEGORY_DIR -> ICON_FOLDER;
                    case ROOT_DIR -> EMPTY_STRING;
                };
                label.setText(s(prefix, fileObj.displayName()));
            }
            return label;
        }
    }

    private static final class ScopeCellRenderer extends DefaultTableCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                       final boolean hasFocus, final int row, final int column) {
            final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final String scope = (String) value;
            label.setFont(FONT_SANS_BOLD_10);
            label.setHorizontalAlignment(SwingConstants.CENTER);

            final ScopeStyle style = null == scope ? null : SCOPE_STYLES.get(scope);
            if (isNotNull(style)) {
                label.setText(style.text());
                label.setBackground(style.background());
                label.setForeground(style.foreground());
                label.setBorder(style.bordered() ? ERROR_LINE_BORDER : null);
            } else {
                label.setText(scope);
                label.setBackground(Color.LIGHT_GRAY);
                label.setForeground(Color.BLACK);
                label.setBorder(null);
            }
            return label;
        }
    }

    private static final class PropertyCellRenderer extends DefaultTableCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        private final DefaultTableModel tableModel;
        private final Supplier<FileNodeObject> currentNodeSupplier;

        private PropertyCellRenderer(final DefaultTableModel tableModel,
                                     final Supplier<FileNodeObject> currentNodeSupplier) {
            this.tableModel = tableModel;
            this.currentNodeSupplier = currentNodeSupplier;
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                       final boolean hasFocus, final int row, final int column) {
            final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final int modelRow = table.convertRowIndexToModel(row);
            if (0 <= modelRow && modelRow < tableModel.getRowCount()) {
                final String key = (String) tableModel.getValueAt(modelRow, COL_KEY);
                final String val = (String) tableModel.getValueAt(modelRow, COL_VALUE);
                final FileNodeObject currentNode = currentNodeSupplier.get();
                final _ConfigFileCategory cat = isNotNull(currentNode) ? currentNode.category() : null;
                final String error = PropertyValidator.validateProperty(key, val, cat);
                applyValidationStyle(label, table, isSelected, error);
            }
            return label;
        }

        private static void applyValidationStyle(final JLabel label, final JTable table,
                                                 final boolean isSelected, final String error) {
            if (isNotNull(error)) {
                if (!isSelected) {
                    label.setBackground(COLOR_ERROR_ROW_BG);
                    label.setForeground(COLOR_ERROR_FG);
                }
                label.setToolTipText(s(ICON_WARNING, error));
            } else {
                if (!isSelected) {
                    label.setBackground(table.getBackground());
                    label.setForeground(table.getForeground());
                }
                label.setToolTipText(null);
            }
        }
    }
}
