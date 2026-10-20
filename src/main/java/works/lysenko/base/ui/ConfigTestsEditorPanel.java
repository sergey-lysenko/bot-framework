package works.lysenko.base.ui;

import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.issues.KnownIssuesMatcher;
import works.lysenko.util.prop.PropEnum;
import works.lysenko.base.parameters.PropertyValidator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Parts.SUITE_PROPERTIES_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._CONFIG_;
import static works.lysenko.util.spec.Layout.Paths._SUITES_;

/**
 * Embedded Micro-IDE style panel for managing configuration files, parts, and test suites.
 * Integrates PropertyValidator for real-time validation, invalid key/value indications, inline editing,
 * and prompting the user to save changes upon losing focus or switching files.
 */
public class ConfigTestsEditorPanel extends JPanel {

    static {
        ControlPanel.initializeLookAndFeel();
    }

    public enum FileCategory {
        ROOT_DIR, CATEGORY_DIR, GLOBAL_CONFIG, CONFIG_PART, TEST_SUITE, SUITE_PART
    }

    public record FileNodeObject(String displayName, Path path, FileCategory category, boolean deletable) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    private final JTree fileTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode treeRootNode;

    private final JLabel activeFileLabel = new JLabel("No file selected");
    private final JLabel activeFileBadge = new JLabel();

    private final DefaultTableModel tableModel;
    private final PropertyTablePanel propertyTablePanel;
    private final JLabel scopeWarningLabel = new JLabel();

    private final JTextArea rawTextArea = new JTextArea();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel centerPanel = new JPanel(cardLayout);
    private final JToggleButton toggleViewBtn = new JToggleButton("Raw Text View");

    private FileNodeObject currentSelectedNode = null;
    private String originalFileContent = "";
    private boolean isUpdatingTree = false;
    private boolean isUpdatingForm = false;

    public ConfigTestsEditorPanel() {
        setLayout(new BorderLayout(0, 0));

        // ---------------------------------------------------------------------
        // LEFT SIDEBAR: File Tree Explorer (Micro-IDE)
        // ---------------------------------------------------------------------
        treeRootNode = new DefaultMutableTreeNode("resources");
        treeModel = new DefaultTreeModel(treeRootNode);
        fileTree = new JTree(treeModel);
        fileTree.setCellRenderer(new FileTreeCellRenderer());
        fileTree.getSelectionModel().addTreeSelectionListener(e -> {
            if (!isUpdatingTree) {
                onTreeSelectionChanged();
            }
        });

        final JPopupMenu treePopupMenu = new JPopupMenu();
        final JMenuItem renameItem = new JMenuItem("Rename...");
        final JMenuItem duplicateItem = new JMenuItem("Duplicate...");
        final JMenuItem deleteItem = new JMenuItem("Delete");

        renameItem.addActionListener(e -> renameSelectedFile());
        duplicateItem.addActionListener(e -> duplicateSelectedFile());
        deleteItem.addActionListener(e -> deleteSelectedFile());

        treePopupMenu.add(renameItem);
        treePopupMenu.add(duplicateItem);
        treePopupMenu.addSeparator();
        treePopupMenu.add(deleteItem);

        treePopupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
                boolean allow = false;
                if (node != null && node.getUserObject() instanceof FileNodeObject fileObj) {
                    if (fileObj.category() != FileCategory.CATEGORY_DIR && fileObj.category() != FileCategory.ROOT_DIR
                            && !fileObj.displayName().equals("common.configuration.properties")) {
                        allow = true;
                    }
                }
                renameItem.setEnabled(allow);
                duplicateItem.setEnabled(allow);
                deleteItem.setEnabled(allow);
            }

            @Override public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(final PopupMenuEvent e) {}
        });

        fileTree.setComponentPopupMenu(treePopupMenu);
        fileTree.addMouseListener(new MouseAdapter() {
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
                    if (path != null) {
                        fileTree.setSelectionPath(path);
                    }
                }
            }
        });

        final JScrollPane treeScrollPane = new JScrollPane(fileTree);
        treeScrollPane.setPreferredSize(new Dimension(360, 0));

        // Sidebar Toolbar
        final JPanel treeToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        final Font btnFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

        final JButton newFileBtn = new JButton("+ New File...");
        newFileBtn.setFont(btnFont);
        newFileBtn.setToolTipText("Create a new .suite or .part file");
        newFileBtn.addActionListener(e -> createNewFile());

        final JButton deleteFileBtn = new JButton("Delete File");
        deleteFileBtn.setFont(btnFont);
        deleteFileBtn.setToolTipText("Delete selected file");
        deleteFileBtn.addActionListener(e -> deleteSelectedFile());

        final JButton refreshTreeBtn = new JButton("Refresh");
        refreshTreeBtn.setFont(btnFont);
        refreshTreeBtn.setToolTipText("Refresh file tree from disk");
        refreshTreeBtn.addActionListener(e -> {
            if (promptToSaveIfModified()) {
                refreshTreeFromDisk();
            }
        });

        treeToolbar.add(newFileBtn);
        treeToolbar.add(deleteFileBtn);
        treeToolbar.add(refreshTreeBtn);

        final JPanel sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setBorder(BorderFactory.createTitledBorder("Project Explorer"));
        sidebarPanel.setPreferredSize(new Dimension(360, 0));
        sidebarPanel.add(treeToolbar, BorderLayout.NORTH);
        sidebarPanel.add(treeScrollPane, BorderLayout.CENTER);

        // ---------------------------------------------------------------------
        // RIGHT SIDE: Editor Area
        // ---------------------------------------------------------------------
        // Top Active File Header
        activeFileLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        activeFileBadge.setOpaque(true);
        activeFileBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        activeFileBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        final JPanel activeFileHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        activeFileHeader.add(activeFileBadge);
        activeFileHeader.add(activeFileLabel);

        // Action Toolbar
        final JPanel editorToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        final JButton saveFileBtn = new JButton(" Save ");
        saveFileBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        saveFileBtn.setToolTipText("Save current file (Ctrl+S / Cmd+S)");
        saveFileBtn.addActionListener(e -> saveSelectedFile());

        final JButton reloadBtn = new JButton("Reload");
        reloadBtn.addActionListener(e -> {
            if (promptToSaveIfModified()) {
                loadSelectedFile();
            }
        });

        toggleViewBtn.addActionListener(e -> {
            if (toggleViewBtn.isSelected()) {
                syncTableToRawText();
                cardLayout.show(centerPanel, "RAW");
            } else {
                syncRawTextToTable();
                cardLayout.show(centerPanel, "FORM");
            }
        });

        editorToolbar.add(reloadBtn);
        editorToolbar.add(toggleViewBtn);
        editorToolbar.add(saveFileBtn);

        final JPanel topHeaderPanel = new JPanel(new BorderLayout());
        topHeaderPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        topHeaderPanel.add(activeFileHeader, BorderLayout.WEST);
        topHeaderPanel.add(editorToolbar, BorderLayout.EAST);

        // --- Form View setup ---
        final String[] columns = {"Scope", "Key / Property", "Value / Weight"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return column == 2;
            }

            @Override
            public void setValueAt(final Object aValue, final int row, final int column) {
                if (row >= 0 && row < getRowCount() && column >= 0 && column < getColumnCount()) {
                    super.setValueAt(aValue, row, column);
                }
            }
        };
        propertyTablePanel = new PropertyTablePanel(tableModel, 1, 2, false, null);
        propertyTablePanel.setSearchAndFiltersVisible(false);
        propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.CONFIG_ONLY);
        
        final JTable table = propertyTablePanel.getTable();
        table.getColumnModel().getColumn(0).setMaxWidth(110);
        table.getColumnModel().getColumn(0).setCellRenderer(new ScopeCellRenderer());
        final PropertyCellRenderer propRenderer = new PropertyCellRenderer();
        table.getColumnModel().getColumn(1).setCellRenderer(propRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(propRenderer);

        table.getColumnModel().getColumn(2).setCellEditor(new ValidatingCellEditor(1, msg -> {
            if (msg != null) {
                scopeWarningLabel.setText("\u26a0\ufe0f " + msg);
                scopeWarningLabel.setVisible(true);
            } else {
                validateContainerScope();
            }
        }));

        tableModel.addTableModelListener(e -> {
            if (isUpdatingForm) return;
            final int r = e.getFirstRow();
            final int col = e.getColumn();
            if (col == 1 || col == 2) {
                if (r >= 0 && r < tableModel.getRowCount()) {
                    isUpdatingForm = true;
                    try {
                        final String k = (String) tableModel.getValueAt(r, 1);
                        final String v = (String) tableModel.getValueAt(r, 2);
                        final FileCategory cat = isNotNull(currentSelectedNode) ? currentSelectedNode.category() : null;
                        final String newScope = determineScope(k, v, cat);
                        final String oldScope = (String) tableModel.getValueAt(r, 0);
                        if (!newScope.equals(oldScope)) {
                            tableModel.setValueAt(newScope, r, 0);
                        }
                        validateContainerScope();
                    } finally {
                        isUpdatingForm = false;
                    }
                }
            }
            if (isNotNull(currentSelectedNode)) {
                updateActiveFileHeader(currentSelectedNode);
            }
        });

        // Scope / Value warning banner
        scopeWarningLabel.setOpaque(true);
        scopeWarningLabel.setBackground(new Color(0xFE, 0xF2, 0xF2));
        scopeWarningLabel.setForeground(new Color(0x99, 0x1B, 0x1B));
        scopeWarningLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xF8, 0x71, 0x71)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        scopeWarningLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        scopeWarningLabel.setVisible(false);

        propertyTablePanel.setFormSaveAction(e -> saveCurrentFormEntry());
        propertyTablePanel.setFormDeleteAction(e -> deleteSelectedEntry());

        final JPanel formViewPanel = new JPanel(new BorderLayout(0, 8));
        formViewPanel.add(scopeWarningLabel, BorderLayout.NORTH);
        formViewPanel.add(propertyTablePanel, BorderLayout.CENTER);

        // Attach context menu
        ContextMenu.attach((JTextComponent) propertyTablePanel.getKeyField().getEditor().getEditorComponent(), propertyTablePanel.getValueField(), rawTextArea);

        rawTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rawTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(final DocumentEvent e) { if (!isUpdatingForm && isNotNull(currentSelectedNode)) updateActiveFileHeader(currentSelectedNode); }
            @Override public void removeUpdate(final DocumentEvent e) { if (!isUpdatingForm && isNotNull(currentSelectedNode)) updateActiveFileHeader(currentSelectedNode); }
            @Override public void changedUpdate(final DocumentEvent e) { if (!isUpdatingForm && isNotNull(currentSelectedNode)) updateActiveFileHeader(currentSelectedNode); }
        });
        final JScrollPane rawScrollPane = new JScrollPane(rawTextArea);

        centerPanel.add(formViewPanel, "FORM");
        centerPanel.add(rawScrollPane, "RAW");

        final JPanel editorAreaPanel = new JPanel(new BorderLayout(0, 6));
        editorAreaPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        editorAreaPanel.add(topHeaderPanel, BorderLayout.NORTH);
        editorAreaPanel.add(centerPanel, BorderLayout.CENTER);

        // --- Split Pane: Sidebar Tree + Editor Area ---
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, editorAreaPanel);
        splitPane.setDividerLocation(360);
        splitPane.setResizeWeight(0.32);

        add(splitPane, BorderLayout.CENTER);

        // Setup Cmd+S / Ctrl+S Save Keyboard Shortcut
        setupSaveKeyboardShortcut();

        refreshTreeFromDisk();
        selectDefaultFileNode();
    }

    /**
     * Checks if the currently open file has unsaved modifications.
     *
     * @return true if modified, false otherwise
     */
    public boolean isModified() {
        if (null == currentSelectedNode) return false;
        final String currentContent;
        if (rawTextArea.isShowing()) {
            currentContent = rawTextArea.getText();
        } else {
            currentContent = generateFileContentFromTable();
        }
        return !currentContent.equals(originalFileContent);
    }

    /**
     * Prompts the user to save changes if the currently open file has unsaved modifications.
     *
     * @return true if navigation/switching should proceed (saved or discarded), false if action was cancelled
     */
    public boolean promptToSaveIfModified() {
        if (!isModified() || null == currentSelectedNode) {
            return true;
        }

        final int result = JOptionPane.showConfirmDialog(
                this,
                "The file '" + currentSelectedNode.displayName() + "' has unsaved changes.\nDo you want to save them before proceeding?",
                "Unsaved Changes in " + currentSelectedNode.displayName(),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            return saveSelectedFile();
        } else if (result == JOptionPane.NO_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    private void setupSaveKeyboardShortcut() {
        final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        final KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, mask);

        final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap actionMap = getActionMap();

        inputMap.put(saveKeyStroke, "SAVE_FILE_ACTION");
        actionMap.put("SAVE_FILE_ACTION", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                saveSelectedFile();
            }
        });
    }

    public void refreshTreeFromDisk() {
        isUpdatingTree = true;
        treeRootNode.removeAllChildren();

        // 1. config/
        final DefaultMutableTreeNode configFolder = new DefaultMutableTreeNode(
                new FileNodeObject("config", Path.of(_CONFIG_), FileCategory.CATEGORY_DIR, false)
        );

        final Path commonConfigPath = Path.of(_CONFIG_, "common.configuration.properties");
        if (Files.exists(commonConfigPath)) {
            configFolder.add(new DefaultMutableTreeNode(
                    new FileNodeObject("common.configuration.properties", commonConfigPath, FileCategory.GLOBAL_CONFIG, false)
            ));
        }

        final Path propertyHelpPath = Path.of(_CONFIG_, "property-help.properties");
        if (Files.exists(propertyHelpPath)) {
            configFolder.add(new DefaultMutableTreeNode(
                    new FileNodeObject("property-help.properties", propertyHelpPath, FileCategory.GLOBAL_CONFIG, false)
            ));
        }

        // parts/ inside config/
        final Path partsDir = Path.of(_CONFIG_, "parts");
        if (Files.exists(partsDir) && Files.isDirectory(partsDir)) {
            final DefaultMutableTreeNode partsFolder = new DefaultMutableTreeNode(
                    new FileNodeObject("parts", partsDir, FileCategory.CATEGORY_DIR, false)
            );
            try (final var stream = Files.list(partsDir)) {
                stream.filter(p -> p.toString().endsWith(".part"))
                        .sorted()
                        .forEach(p -> partsFolder.add(new DefaultMutableTreeNode(
                                new FileNodeObject(p.getFileName().toString(), p, FileCategory.CONFIG_PART, true)
                        )));
            } catch (final Exception ignored) {}
            configFolder.add(partsFolder);
        }

        treeRootNode.add(configFolder);

        // 2. suites/
        final Path suitesDir = Path.of(_SUITES_);
        if (Files.exists(suitesDir) && Files.isDirectory(suitesDir)) {
            final DefaultMutableTreeNode suitesFolder = new DefaultMutableTreeNode(
                    new FileNodeObject("suites", suitesDir, FileCategory.CATEGORY_DIR, false)
            );
            try (final var stream = Files.list(suitesDir)) {
                stream.filter(p -> p.toString().endsWith(SUITE_PROPERTIES_EXTENSION))
                        .sorted()
                        .forEach(p -> suitesFolder.add(new DefaultMutableTreeNode(
                                new FileNodeObject(p.getFileName().toString(), p, FileCategory.TEST_SUITE, true)
                        )));
            } catch (final Exception ignored) {}

            // parts/ inside suites/
            final Path suitePartsDir = Path.of(_SUITES_, "parts");
            if (Files.exists(suitePartsDir) && Files.isDirectory(suitePartsDir)) {
                final DefaultMutableTreeNode suitePartsFolder = new DefaultMutableTreeNode(
                        new FileNodeObject("parts", suitePartsDir, FileCategory.CATEGORY_DIR, false)
                );
                try (final var stream = Files.list(suitePartsDir)) {
                    stream.filter(p -> p.toString().endsWith(".part") || p.toString().endsWith(SUITE_PROPERTIES_EXTENSION))
                            .sorted()
                            .forEach(p -> suitePartsFolder.add(new DefaultMutableTreeNode(
                                    new FileNodeObject(p.getFileName().toString(), p, FileCategory.SUITE_PART, true)
                            )));
                } catch (final Exception ignored) {}
                suitesFolder.add(suitePartsFolder);
            }

            treeRootNode.add(suitesFolder);
        }

        treeModel.reload();

        // Expand all root nodes
        for (int i = 0; i < fileTree.getRowCount(); i++) {
            fileTree.expandRow(i);
        }

        isUpdatingTree = false;
    }

    private void selectDefaultFileNode() {
        findAndSelectNodeByFileName("common.configuration.properties");
    }

    private void findAndSelectNodeByFileName(final String targetFileName) {
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        findNodeAndSelect(root, targetFileName);
    }

    private boolean findNodeAndSelect(final DefaultMutableTreeNode node, final String targetFileName) {
        if (node.getUserObject() instanceof FileNodeObject obj) {
            if (obj.displayName().equals(targetFileName)) {
                final TreePath path = new TreePath(node.getPath());
                fileTree.setSelectionPath(path);
                fileTree.scrollPathToVisible(path);
                return true;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (findNodeAndSelect((DefaultMutableTreeNode) node.getChildAt(i), targetFileName)) {
                return true;
            }
        }
        return false;
    }

    public void stopOrCancelTableEditing() {
        if (isNotNull(propertyTablePanel) && isNotNull(propertyTablePanel.getTable()) && propertyTablePanel.getTable().isEditing()) {
            final TableCellEditor editor = propertyTablePanel.getTable().getCellEditor();
            if (isNotNull(editor)) {
                editor.stopCellEditing();
                if (propertyTablePanel.getTable().isEditing()) {
                    editor.cancelCellEditing();
                }
            }
        }
    }

    private void onTreeSelectionChanged() {
        stopOrCancelTableEditing();
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
        if (null == node) return;

        final Object userObj = node.getUserObject();
        if (userObj instanceof FileNodeObject fileObj) {
            if (fileObj.category() == FileCategory.CATEGORY_DIR || fileObj.category() == FileCategory.ROOT_DIR) {
                return;
            }
            if (isNotNull(currentSelectedNode) && currentSelectedNode.equals(fileObj)) {
                return;
            }
            if (!promptToSaveIfModified()) {
                isUpdatingTree = true;
                try {
                    if (isNotNull(currentSelectedNode)) {
                        findAndSelectNodeByFileName(currentSelectedNode.displayName());
                    }
                } finally {
                    isUpdatingTree = false;
                }
                return;
            }
            currentSelectedNode = fileObj;
            updateActiveFileHeader(fileObj);
            loadSelectedFile();
        }
    }

    private void updateActiveFileHeader(final FileNodeObject fileObj) {
        if (null == fileObj) {
            activeFileLabel.setText("No file selected");
            activeFileBadge.setText("");
            activeFileBadge.setVisible(false);
            return;
        }

        final boolean modified = isModified();
        final String pathStr = fileObj.path().toString() + (modified ? " *" : "");
        activeFileLabel.setText(pathStr);

        final String baseBadge;
        switch (fileObj.category()) {
            case GLOBAL_CONFIG -> {
                baseBadge = "GLOBAL CONFIG";
                activeFileBadge.setBackground(modified ? new Color(0xFE, 0xF3, 0xC7) : new Color(0xDB, 0xEA, 0xFE));
                activeFileBadge.setForeground(modified ? new Color(0x92, 0x40, 0x0E) : new Color(0x1E, 0x40, 0xAF));
            }
            case CONFIG_PART -> {
                baseBadge = "CONFIG PARTIAL";
                activeFileBadge.setBackground(modified ? new Color(0xFE, 0xF3, 0xC7) : new Color(0xDC, 0xFC, 0xE7));
                activeFileBadge.setForeground(modified ? new Color(0x92, 0x40, 0x0E) : new Color(0x16, 0x65, 0x34));
            }
            case TEST_SUITE -> {
                baseBadge = "TEST SUITE";
                activeFileBadge.setBackground(new Color(0xFE, 0xF3, 0xC7));
                activeFileBadge.setForeground(new Color(0x92, 0x40, 0x0E));
            }
            case SUITE_PART -> {
                activeFileBadge.setText("SUITE PARTIAL");
                activeFileBadge.setBackground(modified ? new Color(0xFE, 0xF3, 0xC7) : new Color(0xEE, 0xF2, 0xFF));
                activeFileBadge.setForeground(modified ? new Color(0x92, 0x40, 0x0E) : new Color(0x37, 0x30, 0xA3));
                baseBadge = "SUITE PARTIAL";
            }
            default -> {
                baseBadge = "FILE";
                activeFileBadge.setBackground(Color.LIGHT_GRAY);
                activeFileBadge.setForeground(Color.BLACK);
            }
        }
        activeFileBadge.setText(modified ? baseBadge + " (MODIFIED)" : baseBadge);
        activeFileBadge.setVisible(true);
    }

    private void saveCurrentFormEntry() {
        final String key = propertyTablePanel.getKeyField().getText().trim();
        final String val = propertyTablePanel.getValueField().getText().trim();
        if (key.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Key / Property name cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final FileCategory category = isNotNull(currentSelectedNode) ? currentSelectedNode.category() : null;
        final String valErr = PropertyValidator.validateProperty(key, val, category);
        if (isNotNull(valErr)) {
            final int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Validation notice for '" + key + "':\n" + valErr + "\n\nSave entry as invalid anyway?",
                    "Property Validation Notice",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final String scope = determineScope(key, val, category);
        final JTable table = propertyTablePanel.getTable();
        final int selectedViewRow = table.getSelectedRow();

        isUpdatingForm = true;
        try {
            if (selectedViewRow >= 0) {
                final int modelRow = table.convertRowIndexToModel(selectedViewRow);
                if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                    tableModel.setValueAt(scope, modelRow, 0);
                    tableModel.setValueAt(key, modelRow, 1);
                    tableModel.setValueAt(val, modelRow, 2);
                }
            } else {
                final Object[] rowData = {scope, key, val};
                tableModel.addRow(rowData);
                final int modelRow = tableModel.getRowCount() - 1;
                final int viewRow = table.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }
            }
        } finally {
            isUpdatingForm = false;
        }

        validateContainerScope();
        if (isNotNull(currentSelectedNode)) {
            updateActiveFileHeader(currentSelectedNode);
        }
    }

    private void deleteSelectedEntry() {
        final JTable table = propertyTablePanel.getTable();
        final int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            tableModel.removeRow(table.convertRowIndexToModel(selectedRow));
            propertyTablePanel.clearForm();
            validateContainerScope();
            if (isNotNull(currentSelectedNode)) {
                updateActiveFileHeader(currentSelectedNode);
            }
        }
    }

    private static String determineScope(final String key, final String val, final FileCategory category) {
        if (null == key || key.isBlank()) return "INVALID";
        final String err = PropertyValidator.validateProperty(key, val, category);
        if (isNotNull(err)) {
            return "INVALID";
        }
        if (key.startsWith(".include") || key.startsWith("include")) {
            return "INCLUDE";
        } else if (key.startsWith(".")) {
            return "CONFIG";
        } else if (key.startsWith("#")) {
            return "COMMENT";
        } else {
            return "SCENARIO";
        }
    }

    private void validateContainerScope() {
        if (null == currentSelectedNode) return;

        final FileCategory category = currentSelectedNode.category();
        final List<String> invalidDetails = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            final String k = (String) tableModel.getValueAt(i, 1);
            final String v = (String) tableModel.getValueAt(i, 2);
            final String err = PropertyValidator.validateProperty(k, v, category);
            if (isNotNull(err)) {
                invalidDetails.add("Row " + (i + 1) + " ('" + k + "'): " + err);
            }
        }

        if (!invalidDetails.isEmpty()) {
            final String countStr = invalidDetails.size() == 1 ? "1 invalid property" : invalidDetails.size() + " invalid properties";
            scopeWarningLabel.setText("\u26a0\ufe0f " + countStr + " found in " + currentSelectedNode.displayName() +
                    ": " + invalidDetails.get(0) + ". Invalid properties are preserved when saving.");
            scopeWarningLabel.setVisible(true);
        } else {
            scopeWarningLabel.setVisible(false);
        }
    }

    public void loadSelectedFile() {
        if (null == currentSelectedNode) return;

        stopOrCancelTableEditing();
        updatePartsModeForCurrentNode();
        isUpdatingForm = true;
        try {
            tableModel.setRowCount(0);
            propertyTablePanel.clearForm();

            if (Files.exists(currentSelectedNode.path())) {
                final List<String> lines = Files.readAllLines(currentSelectedNode.path(), StandardCharsets.UTF_8);
                rawTextArea.setText(String.join("\n", lines));

                for (final String line : lines) {
                    final String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                    final int eqIdx = trimmed.indexOf('=');
                    if (eqIdx > 0) {
                        final String k = trimmed.substring(0, eqIdx).trim();
                        final String v = trimmed.substring(eqIdx + 1).trim();
                        final String scope = determineScope(k, v, currentSelectedNode.category());
                        tableModel.addRow(new Object[]{scope, k, v});
                    }
                }
            }
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            isUpdatingForm = false;
        }

        originalFileContent = generateFileContentFromTable();
        updateActiveFileHeader(currentSelectedNode);
        validateContainerScope();
    }

    /**
     * Saves the currently selected file.
     *
     * @return true if save succeeded, false otherwise
     */
    public boolean saveSelectedFile() {
        if (null == currentSelectedNode) return false;

        try {
            if (rawTextArea.isShowing()) {
                syncRawTextToTable();
            }

            validateContainerScope();

            final String content = generateFileContentFromTable();
            Files.createDirectories(currentSelectedNode.path().getParent());
            Files.writeString(currentSelectedNode.path(), content, StandardCharsets.UTF_8);
            rawTextArea.setText(content);
            originalFileContent = content;
            updateActiveFileHeader(currentSelectedNode);

            JOptionPane.showMessageDialog(this, "Saved " + currentSelectedNode.displayName() + " successfully.", "File Saved", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public void saveSilentlyIfModified() {
        if (null == currentSelectedNode || !isModified()) return;
        try {
            stopOrCancelTableEditing();
            if (rawTextArea.isShowing()) {
                syncRawTextToTable();
            }
            final String content = generateFileContentFromTable();
            Files.createDirectories(currentSelectedNode.path().getParent());
            Files.writeString(currentSelectedNode.path(), content, StandardCharsets.UTF_8);
            rawTextArea.setText(content);
            originalFileContent = content;
            updateActiveFileHeader(currentSelectedNode);
        } catch (final Exception ignored) {
        }
    }

    private String generateFileContentFromTable() {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Generated / Updated by Config & Tests Micro-IDE\n");
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            final String k = (String) tableModel.getValueAt(i, 1);
            final String v = (String) tableModel.getValueAt(i, 2);
            sb.append(k).append("=").append(v).append("\n");
        }
        return sb.toString();
    }

    private void syncTableToRawText() {
        rawTextArea.setText(generateFileContentFromTable());
    }

    private void syncRawTextToTable() {
        stopOrCancelTableEditing();
        isUpdatingForm = true;
        try {
            tableModel.setRowCount(0);
            final String text = rawTextArea.getText();
            if (isNotNull(text)) {
                final String[] lines = text.split("\n");
                for (final String line : lines) {
                    final String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                    final int eqIdx = trimmed.indexOf('=');
                    if (eqIdx > 0) {
                        final String k = trimmed.substring(0, eqIdx).trim();
                        final String v = trimmed.substring(eqIdx + 1).trim();
                        final String scope = determineScope(k, v, isNotNull(currentSelectedNode) ? currentSelectedNode.category() : null);
                        tableModel.addRow(new Object[]{scope, k, v});
                    }
                }
            }
        } finally {
            isUpdatingForm = false;
        }
        validateContainerScope();
    }

    private void createNewFile() {
        if (!promptToSaveIfModified()) return;

        final String[] types = {
                "Test Suite (*.suite in suites/)",
                "Suite Part (*.part in suites/parts/)",
                "Config Part (*.part in config/parts/)"
        };
        final String selectedType = (String) JOptionPane.showInputDialog(
                this,
                "Select file type:",
                "New File",
                JOptionPane.QUESTION_MESSAGE,
                null,
                types,
                types[0]
        );

        if (null == selectedType) return;

        final String rawName = JOptionPane.showInputDialog(this, "Enter file name (without extension):", "New File", JOptionPane.PLAIN_MESSAGE);
        if (null == rawName || rawName.isBlank()) return;

        final String cleanName = rawName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "");
        final Path targetPath;
        final String extension;
        if (selectedType.contains("suites/parts/")) {
            extension = ".part";
            targetPath = Path.of(_SUITES_, "parts", cleanName + extension);
        } else if (selectedType.contains("suites/")) {
            extension = SUITE_PROPERTIES_EXTENSION;
            targetPath = Path.of(_SUITES_, cleanName + extension);
        } else {
            extension = ".part";
            targetPath = Path.of(_CONFIG_, "parts", cleanName + extension);
        }

        if (Files.exists(targetPath)) {
            JOptionPane.showMessageDialog(this, "File already exists: " + targetPath.getFileName(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, "# " + cleanName + "\n", StandardCharsets.UTF_8);

            refreshTreeFromDisk();
            findAndSelectNodeByFileName(cleanName + extension);
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(this, "Error creating file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedFile() {
        if (null == currentSelectedNode) return;

        if (!currentSelectedNode.deletable()) {
            JOptionPane.showMessageDialog(this, "Cannot delete global system file: " + currentSelectedNode.displayName(), "Protected File", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete file " + currentSelectedNode.displayName() + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Files.deleteIfExists(currentSelectedNode.path());
                refreshTreeFromDisk();
                selectDefaultFileNode();
                JOptionPane.showMessageDialog(this, "File deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } catch (final Exception e) {
                JOptionPane.showMessageDialog(this, "Error deleting file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void renameSelectedFile() {
        if (null == currentSelectedNode) return;
        if (currentSelectedNode.displayName().equals("common.configuration.properties")) {
            JOptionPane.showMessageDialog(this, "Cannot rename the common configuration file.", "Protected File", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!promptToSaveIfModified()) return;

        final String oldName = currentSelectedNode.displayName();
        final String baseName = oldName.contains(".") ? oldName.substring(0, oldName.lastIndexOf('.')) : oldName;
        final String extension = oldName.contains(".") ? oldName.substring(oldName.lastIndexOf('.')) : "";

        final String rawName = (String) JOptionPane.showInputDialog(
                this,
                "Enter new file name (without extension):",
                "Rename File",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                baseName
        );

        if (null == rawName || rawName.isBlank()) return;

        final String cleanName = rawName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "");
        if (cleanName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Invalid file name.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final String newFileName = cleanName + extension;
        final Path oldPath = currentSelectedNode.path();
        final Path newPath = oldPath.resolveSibling(newFileName);

        if (Files.exists(newPath)) {
            JOptionPane.showMessageDialog(this, "File already exists: " + newFileName, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Files.move(oldPath, newPath);
            refreshTreeFromDisk();
            findAndSelectNodeByFileName(newFileName);
            JOptionPane.showMessageDialog(this, "File renamed successfully to " + newFileName, "Renamed", JOptionPane.INFORMATION_MESSAGE);
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(this, "Error renaming file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void duplicateSelectedFile() {
        if (null == currentSelectedNode) return;
        if (currentSelectedNode.displayName().equals("common.configuration.properties")) {
            JOptionPane.showMessageDialog(this, "Cannot duplicate the common configuration file.", "Protected File", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (isModified()) {
            saveSelectedFile();
        }

        final String oldName = currentSelectedNode.displayName();
        final String baseName = oldName.contains(".") ? oldName.substring(0, oldName.lastIndexOf('.')) : oldName;
        final String extension = oldName.contains(".") ? oldName.substring(oldName.lastIndexOf('.')) : "";

        final String rawName = (String) JOptionPane.showInputDialog(
                this,
                "Enter name for duplicate file (without extension):",
                "Duplicate File",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                baseName + "_copy"
        );

        if (null == rawName || rawName.isBlank()) return;

        final String cleanName = rawName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "");
        if (cleanName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Invalid file name.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final String newFileName = cleanName + extension;
        final Path oldPath = currentSelectedNode.path();
        final Path newPath = oldPath.resolveSibling(newFileName);

        if (Files.exists(newPath)) {
            JOptionPane.showMessageDialog(this, "File already exists: " + newFileName, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Files.copy(oldPath, newPath);
            refreshTreeFromDisk();
            findAndSelectNodeByFileName(newFileName);
            JOptionPane.showMessageDialog(this, "File duplicated successfully as " + newFileName, "Duplicated", JOptionPane.INFORMATION_MESSAGE);
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(this, "Error duplicating file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Cell Renderers ---

    private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(final JTree t, final Object val, final boolean sel,
                                                      final boolean exp, final boolean leaf, final int r, final boolean foc) {
            final JLabel label = (JLabel) super.getTreeCellRendererComponent(t, val, sel, exp, leaf, r, foc);
            if (val instanceof DefaultMutableTreeNode node) {
                final Object obj = node.getUserObject();
                if (obj instanceof FileNodeObject fileObj) {
                    label.setText(fileObj.displayName());
                    switch (fileObj.category()) {
                        case GLOBAL_CONFIG -> label.setText("\u2699\ufe0f " + fileObj.displayName());
                        case CONFIG_PART, SUITE_PART -> label.setText("\ud83e\udde9 " + fileObj.displayName());
                        case TEST_SUITE -> label.setText("\ud83e\uddea " + fileObj.displayName());
                        case CATEGORY_DIR -> label.setText("\ud83d\udcc1 " + fileObj.displayName());
                        default -> {}
                    }
                }
            }
            return label;
        }
    }

    private static class ScopeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable t, final Object val, final boolean sel, final boolean foc, final int r, final int c) {
            final JLabel label = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, r, c);
            final String str = (String) val;
            label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            label.setHorizontalAlignment(CENTER);

            if ("INVALID".equals(str)) {
                label.setText("\u26a0\ufe0f INVALID");
                label.setBackground(new Color(0xFE, 0xF2, 0xF2));
                label.setForeground(new Color(0x99, 0x1B, 0x1B));
                label.setBorder(BorderFactory.createLineBorder(new Color(0xF8, 0x71, 0x71)));
            } else if ("CONFIG".equals(str)) {
                label.setText("CONFIG");
                label.setBackground(new Color(0xDB, 0xEA, 0xFE));
                label.setForeground(new Color(0x1E, 0x40, 0xAF));
                label.setBorder(null);
            } else if ("SCENARIO".equals(str)) {
                label.setText("SCENARIO");
                label.setBackground(new Color(0xFE, 0xF3, 0xC7));
                label.setForeground(new Color(0x92, 0x40, 0x0E));
                label.setBorder(null);
            } else if ("INCLUDE".equals(str)) {
                label.setText("INCLUDE");
                label.setBackground(new Color(0xDC, 0xFC, 0xE7));
                label.setForeground(new Color(0x16, 0x65, 0x34));
                label.setBorder(null);
            } else {
                label.setText(str);
                label.setBackground(Color.LIGHT_GRAY);
                label.setForeground(Color.BLACK);
                label.setBorder(null);
            }
            return label;
        }
    }

    private class PropertyCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable t, final Object val, final boolean sel,
                                                      final boolean foc, final int r, final int c) {
            final JLabel label = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, r, c);
            final int modelRow = t.convertRowIndexToModel(r);
            if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                final String key = (String) tableModel.getValueAt(modelRow, 1);
                final String value = (String) tableModel.getValueAt(modelRow, 2);
                final FileCategory cat = isNotNull(currentSelectedNode) ? currentSelectedNode.category() : null;

                final String error = PropertyValidator.validateProperty(key, value, cat);

                if (isNotNull(error)) {
                    if (!sel) {
                        label.setBackground(new Color(0xFF, 0xF1, 0xF1));
                        label.setForeground(new Color(0x99, 0x1B, 0x1B));
                    }
                    label.setToolTipText("\u26a0\ufe0f " + error);
                } else {
                    if (!sel) {
                        label.setBackground(t.getBackground());
                        label.setForeground(t.getForeground());
                    }
                    label.setToolTipText(null);
                }
            }
            return label;
        }
    }

    public PropertyTablePanel getPropertyTablePanel() {
        return propertyTablePanel;
    }

    public void updatePartsModeForCurrentNode() {
        if (null == currentSelectedNode) {
            propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.CONFIG_ONLY);
            return;
        }

        switch (currentSelectedNode.category()) {
            case GLOBAL_CONFIG, CONFIG_PART ->
                    propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.CONFIG_ONLY);
            case TEST_SUITE, SUITE_PART ->
                    propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.SUITES_ONLY);
            default -> {
                final String pathStr = currentSelectedNode.path().toString().replace('\\', '/');
                if (pathStr.contains("/config/") || pathStr.endsWith("/config") || currentSelectedNode.path().startsWith(Path.of(_CONFIG_))) {
                    propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.CONFIG_ONLY);
                } else {
                    propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.SUITES_ONLY);
                }
            }
        }
    }
}
