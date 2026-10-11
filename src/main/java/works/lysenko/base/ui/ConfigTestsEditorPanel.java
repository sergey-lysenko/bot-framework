package works.lysenko.base.ui;

import works.lysenko.base.parameters.PropertyValidator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.reflections.Reflections;
import java.util.Set;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Paths._CONFIG_;
import static works.lysenko.util.spec.Layout.Paths._TESTS_;

/**
 * Embedded Micro-IDE style panel for managing configuration files, parts, and test suites.
 * Integrates PropertyValidator for real-time validation and inline table cell editing.
 */
public class ConfigTestsEditorPanel extends JPanel {

    static {
        ControlPanel.initializeLookAndFeel();
    }

    public enum FileCategory {
        ROOT_DIR, CATEGORY_DIR, GLOBAL_CONFIG, CONFIG_PART, TEST_SUITE, TEST_PART
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

        final JScrollPane treeScrollPane = new JScrollPane(fileTree);
        treeScrollPane.setPreferredSize(new Dimension(360, 0));

        // Sidebar Toolbar
        final JPanel treeToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        final Font btnFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

        final JButton newFileBtn = new JButton("+ New File...");
        newFileBtn.setFont(btnFont);
        newFileBtn.setToolTipText("Create a new .test suite or .part file");
        newFileBtn.addActionListener(e -> createNewFile());

        final JButton deleteFileBtn = new JButton("Delete File");
        deleteFileBtn.setFont(btnFont);
        deleteFileBtn.setToolTipText("Delete selected file");
        deleteFileBtn.addActionListener(e -> deleteSelectedFile());

        final JButton refreshTreeBtn = new JButton("Refresh");
        refreshTreeBtn.setFont(btnFont);
        refreshTreeBtn.setToolTipText("Refresh file tree from disk");
        refreshTreeBtn.addActionListener(e -> refreshTreeFromDisk());

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
        reloadBtn.addActionListener(e -> loadSelectedFile());

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
        
        final JTable table = propertyTablePanel.getTable();
        table.getColumnModel().getColumn(0).setMaxWidth(110);
        table.getColumnModel().getColumn(0).setCellRenderer(new ScopeCellRenderer());
        table.getColumnModel().getColumn(2).setCellEditor(new ValidatingCellEditor(1, msg -> {
            if (msg != null) {
                scopeWarningLabel.setText("⚠️ " + msg);
                scopeWarningLabel.setVisible(true);
            } else {
                scopeWarningLabel.setVisible(false);
            }
        }));

        tableModel.addTableModelListener(e -> {
            if (isUpdatingForm) return;
            final int r = e.getFirstRow();
            final int col = e.getColumn();
            if (r >= 0 && r < tableModel.getRowCount()) {
                isUpdatingForm = true;
                try {
                    if (col == 1) {
                        final String k = (String) tableModel.getValueAt(r, 1);
                        tableModel.setValueAt(determineScope(k), r, 0);
                    }
                    validateContainerScope();
                } finally {
                    isUpdatingForm = false;
                }
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

        // 2. tests/
        final Path testsDir = Path.of(_TESTS_);
        if (Files.exists(testsDir) && Files.isDirectory(testsDir)) {
            final DefaultMutableTreeNode testsFolder = new DefaultMutableTreeNode(
                    new FileNodeObject("tests", testsDir, FileCategory.CATEGORY_DIR, false)
            );
            try (final var stream = Files.list(testsDir)) {
                stream.filter(p -> p.toString().endsWith(".test"))
                        .sorted()
                        .forEach(p -> testsFolder.add(new DefaultMutableTreeNode(
                                new FileNodeObject(p.getFileName().toString(), p, FileCategory.TEST_SUITE, true)
                        )));
            } catch (final Exception ignored) {}

            // parts/ inside tests/ ("parts of tests")
            final Path testPartsDir = Path.of(_TESTS_, "parts");
            if (Files.exists(testPartsDir) && Files.isDirectory(testPartsDir)) {
                final DefaultMutableTreeNode testPartsFolder = new DefaultMutableTreeNode(
                        new FileNodeObject("parts", testPartsDir, FileCategory.CATEGORY_DIR, false)
                );
                try (final var stream = Files.list(testPartsDir)) {
                    stream.filter(p -> p.toString().endsWith(".part") || p.toString().endsWith(".test"))
                            .sorted()
                            .forEach(p -> testPartsFolder.add(new DefaultMutableTreeNode(
                                    new FileNodeObject(p.getFileName().toString(), p, FileCategory.TEST_PART, true)
                            )));
                } catch (final Exception ignored) {}
                testsFolder.add(testPartsFolder);
            }

            treeRootNode.add(testsFolder);
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
            currentSelectedNode = fileObj;
            updateActiveFileHeader(fileObj);
            loadSelectedFile();
        }
    }

    private void updateActiveFileHeader(final FileNodeObject fileObj) {
        activeFileLabel.setText(fileObj.path().toString());

        switch (fileObj.category()) {
            case GLOBAL_CONFIG -> {
                activeFileBadge.setText("GLOBAL CONFIG");
                activeFileBadge.setBackground(new Color(0xDB, 0xEA, 0xFE));
                activeFileBadge.setForeground(new Color(0x1E, 0x40, 0xAF));
                propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.CONFIG_ONLY);
            }
            case CONFIG_PART -> {
                activeFileBadge.setText("CONFIG PARTIAL");
                activeFileBadge.setBackground(new Color(0xDC, 0xFC, 0xE7));
                activeFileBadge.setForeground(new Color(0x16, 0x65, 0x34));
                propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.CONFIG_ONLY);
            }
            case TEST_SUITE -> {
                activeFileBadge.setText("TEST SUITE");
                activeFileBadge.setBackground(new Color(0xFE, 0xF3, 0xC7));
                activeFileBadge.setForeground(new Color(0x92, 0x40, 0x0E));
                propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.TESTS_ONLY);
            }
            case TEST_PART -> {
                activeFileBadge.setText("TEST PARTIAL");
                activeFileBadge.setBackground(new Color(0xF3, 0xE8, 0xFF));
                activeFileBadge.setForeground(new Color(0x6B, 0x21, 0xA8));
                propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.TESTS_ONLY);
            }
            default -> {
                activeFileBadge.setText("FILE");
                activeFileBadge.setBackground(Color.LIGHT_GRAY);
                activeFileBadge.setForeground(Color.BLACK);
                propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.BOTH);
            }
        }
    }

    private void saveCurrentFormEntry() {
        final String key = propertyTablePanel.getKeyField().getText().trim();
        final String val = propertyTablePanel.getValueField().getText().trim();
        if (key.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Key / Property name cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final String valErr = PropertyValidator.validate(key, val);
        if (isNotNull(valErr)) {
            final int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Validation warning for '" + key + "': " + valErr + "\n\nSave entry anyway?",
                    "Property Value Validation Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final String scope = determineScope(key);
        final Object[] rowData = {scope, key, val};

        final JTable table = propertyTablePanel.getTable();
        final int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            for (int col = 0; col < rowData.length; col++) {
                tableModel.setValueAt(rowData[col], table.convertRowIndexToModel(selectedRow), col);
            }
        } else {
            tableModel.addRow(rowData);
            final int modelRow = tableModel.getRowCount() - 1;
            final int viewRow = table.convertRowIndexToView(modelRow);
            if (viewRow >= 0) {
                table.setRowSelectionInterval(viewRow, viewRow);
            }
        }

        validateContainerScope();
    }

    private void deleteSelectedEntry() {
        final JTable table = propertyTablePanel.getTable();
        final int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            tableModel.removeRow(table.convertRowIndexToModel(selectedRow));
            propertyTablePanel.clearForm();
            validateContainerScope();
        }
    }

    private static String determineScope(final String key) {
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

        // 1. Container Scope Check
        final FileCategory category = currentSelectedNode.category();
        if (category == FileCategory.GLOBAL_CONFIG || category == FileCategory.CONFIG_PART) {
            final List<String> invalidKeys = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                final String scope = (String) tableModel.getValueAt(i, 0);
                if ("SCENARIO".equals(scope)) {
                    invalidKeys.add((String) tableModel.getValueAt(i, 1));
                }
            }

            if (!invalidKeys.isEmpty()) {
                scopeWarningLabel.setText("⚠️ Scope Violation: Un-dotted scenario keys " + invalidKeys +
                        " are disallowed in " + currentSelectedNode.displayName() + "!");
                scopeWarningLabel.setVisible(true);
                return;
            }
        }

        // 2. Property Value Type Validation (using PropertyValidator)
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            final String k = (String) tableModel.getValueAt(i, 1);
            final String v = (String) tableModel.getValueAt(i, 2);
            final String valErr = PropertyValidator.validate(k, v);
            if (isNotNull(valErr)) {
                scopeWarningLabel.setText("⚠️ Value Validation Error in '" + k + "': " + valErr);
                scopeWarningLabel.setVisible(true);
                return;
            }
        }

        scopeWarningLabel.setVisible(false);
    }

    public void loadSelectedFile() {
        if (null == currentSelectedNode) return;

        stopOrCancelTableEditing();
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
                        final String scope = determineScope(k);
                        tableModel.addRow(new Object[]{scope, k, v});
                    }
                }
            }
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            isUpdatingForm = false;
        }

        validateContainerScope();
    }

    public void saveSelectedFile() {
        if (null == currentSelectedNode) return;

        try {
            if (rawTextArea.isShowing()) {
                syncRawTextToTable();
            }

            validateContainerScope();
            if (scopeWarningLabel.isVisible()) {
                final int confirm = JOptionPane.showConfirmDialog(
                        this,
                        scopeWarningLabel.getText() + "\n\nSave file anyway?",
                        "Validation Warning",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            final String content = generateFileContentFromTable();
            Files.createDirectories(currentSelectedNode.path().getParent());
            Files.writeString(currentSelectedNode.path(), content, StandardCharsets.UTF_8);
            rawTextArea.setText(content);

            JOptionPane.showMessageDialog(this, "Saved " + currentSelectedNode.displayName() + " successfully.", "File Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void saveSilentlyIfModified() {
        if (null == currentSelectedNode) return;
        try {
            stopOrCancelTableEditing();
            if (rawTextArea.isShowing()) {
                syncRawTextToTable();
            }
            final String content = generateFileContentFromTable();
            Files.createDirectories(currentSelectedNode.path().getParent());
            Files.writeString(currentSelectedNode.path(), content, StandardCharsets.UTF_8);
            rawTextArea.setText(content);
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
                        final String scope = determineScope(k);
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
        final String[] types = {
                "Test Suite (*.test in tests/)",
                "Test Part (*.part in tests/parts/)",
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
        if (selectedType.contains("tests/parts/")) {
            extension = ".part";
            targetPath = Path.of(_TESTS_, "parts", cleanName + extension);
        } else if (selectedType.contains("tests/")) {
            extension = ".test";
            targetPath = Path.of(_TESTS_, cleanName + extension);
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

    // --- Validating Cell Editor for JTable Inline Editing ---


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
                        case GLOBAL_CONFIG -> label.setText("⚙️ " + fileObj.displayName());
                        case CONFIG_PART -> label.setText("🧩 " + fileObj.displayName());
                        case TEST_SUITE -> label.setText("🧪 " + fileObj.displayName());
                        case TEST_PART -> label.setText("🧩 " + fileObj.displayName());
                        case CATEGORY_DIR -> label.setText("📁 " + fileObj.displayName());
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

            if ("CONFIG".equals(str)) {
                label.setBackground(new Color(0xDB, 0xEA, 0xFE));
                label.setForeground(new Color(0x1E, 0x40, 0xAF));
            } else if ("SCENARIO".equals(str)) {
                label.setBackground(new Color(0xFE, 0xF3, 0xC7));
                label.setForeground(new Color(0x92, 0x40, 0x0E));
            } else if ("INCLUDE".equals(str)) {
                label.setBackground(new Color(0xDC, 0xFC, 0xE7));
                label.setForeground(new Color(0x16, 0x65, 0x34));
            } else {
                label.setBackground(Color.LIGHT_GRAY);
                label.setForeground(Color.BLACK);
            }
            return label;
        }
    }
}
