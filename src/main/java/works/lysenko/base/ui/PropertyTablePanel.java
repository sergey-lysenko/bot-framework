package works.lysenko.base.ui;

import works.lysenko.base.parameters.PropertyHelp;
import works.lysenko.base.parameters.PropertyValidator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Paths._CONFIG_;
import static works.lysenko.util.spec.Layout.Paths._SUITES_;

/**
 * The PropertyTablePanel class.
 */
public class PropertyTablePanel extends JPanel {

    /**
     * The PartsMode enum.
     */
    public enum PartsMode {
        CONFIG_ONLY, SUITES_ONLY
    }

    private PartsMode partsMode = PartsMode.CONFIG_ONLY;

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> sorter;

    // Search and Filters
    private final JTextField searchField = new JTextField(12);

    // Form
    private final AutoCompleteComboBox keyField = new AutoCompleteComboBox(PropertyValidator.getAllKnownProperties());
    private final JTextField valueField = new JTextField();
    private final JButton selectPartsBtn = new JButton("Select Parts...");
    private final JLabel liveValidationErrorLabel = new JLabel();
    private final JLabel propertyHintLabel = new JLabel();
    
    private final JButton newEntryBtn = new JButton("New Entry");
    private final JButton saveEntryBtn = new JButton("Save Entry");
    private final JButton deleteEntryBtn = new JButton("Delete Entry");

    // Help
    private final JLabel helpTitle = new JLabel("Select a property to see help.");
    private final JTextArea helpText = new JTextArea();

    private final JPanel topContainer;
    private final int keyColIdx;
    private final int valColIdx;

    private boolean isUpdatingForm = false;

    /**
     * Constructs a new PropertyTablePanel.
     * @param tableModel the tableModel
     * @param keyColIdx the keyColIdx
     * @param valColIdx the valColIdx
     * @param showHelp the showHelp
     * @param extraSearchComponent the extraSearchComponent
     */
    public PropertyTablePanel(final DefaultTableModel tableModel, final int keyColIdx, final int valColIdx, final boolean showHelp, final JComponent extraSearchComponent) {
        this.tableModel = tableModel;
        this.keyColIdx = keyColIdx;
        this.valColIdx = valColIdx;
        setLayout(new BorderLayout(8, 8));

        table = new JTable(tableModel);
        table.setRowHeight(20);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // 1. Search Panel
        topContainer = new JPanel(new BorderLayout());
        final JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        searchPanel.add(new JLabel("Search:"));
        final JButton clearBtn = new JButton("Clear");
        clearBtn.setMargin(new Insets(2, 6, 2, 6));
        clearBtn.addActionListener(e -> searchField.setText(""));
        searchPanel.add(searchField);
        searchPanel.add(clearBtn);
        if (null != extraSearchComponent) {
            searchPanel.add(Box.createHorizontalStrut(12));
            searchPanel.add(extraSearchComponent);
        }
        topContainer.add(searchPanel, BorderLayout.NORTH);

        final JPanel quickFiltersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        final String[] quickFilters = {".data", ".grid", ".logs", ".math", ".test", ".tree", ".webd", ".mobi", ".swipes", ".progression", ".screenshots"};
        for (final String filter : quickFilters) {
            final JButton filterBtn = new JButton(filter);
            filterBtn.setMargin(new Insets(2, 6, 2, 6));
            filterBtn.addActionListener(e -> searchField.setText(filter));
            quickFiltersPanel.add(filterBtn);
        }
        topContainer.add(quickFiltersPanel, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        setupSearchFiltering();

        // 2. Table and Splitters
        final JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(new EmptyBorder(0, 8, 0, 8));

        // Form Panel
        liveValidationErrorLabel.setForeground(new Color(0xDC, 0x26, 0x26));
        liveValidationErrorLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        liveValidationErrorLabel.setVisible(false);
        propertyHintLabel.setForeground(new Color(0x25, 0x63, 0xEB));
        propertyHintLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        propertyHintLabel.setVisible(false);

        final JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Property Details"));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Key / Property:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(keyField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Value / Weight:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        final JPanel valueContainer = new JPanel(new BorderLayout(6, 0));
        valueContainer.add(valueField, BorderLayout.CENTER);
        selectPartsBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        selectPartsBtn.setVisible(false);
        selectPartsBtn.addActionListener(e -> showPartsSelectionDialog());
        valueContainer.add(selectPartsBtn, BorderLayout.EAST);
        formPanel.add(valueContainer, gbc);
        row++;

        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0;
        final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        infoPanel.add(liveValidationErrorLabel);
        infoPanel.add(propertyHintLabel);
        formPanel.add(infoPanel, gbc);
        row++;

        final JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        newEntryBtn.addActionListener(e -> clearForm());
        formButtons.add(newEntryBtn);
        formButtons.add(saveEntryBtn);
        formButtons.add(deleteEntryBtn);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        formPanel.add(formButtons, gbc);

        setupLiveFieldValidation();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelectedRow();
                if (showHelp) {
                    updateHelpPanel();
                }
            }
        });

        tableModel.addTableModelListener(e -> {
            if (isUpdatingForm) return;
            final int r = e.getFirstRow();
            if (r >= 0 && r < tableModel.getRowCount()) {
                populateFormFromSelectedRow();
            }
        });

        final JPanel tableAndForm = new JPanel(new BorderLayout());
        tableAndForm.add(tableScrollPane, BorderLayout.CENTER);
        tableAndForm.add(formPanel, BorderLayout.SOUTH);

        if (showHelp) {
            helpText.setEditable(false);
            helpText.setLineWrap(true);
            helpText.setWrapStyleWord(true);
            helpText.setFocusable(false);
            helpText.setOpaque(false);
            final JPanel helpPanel = new JPanel(new BorderLayout(4, 4));
            helpPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Help"),
                    new EmptyBorder(4, 8, 8, 8)));
            helpPanel.add(helpTitle, BorderLayout.NORTH);
            helpPanel.add(new JScrollPane(helpText), BorderLayout.CENTER);

            final JSplitPane content = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableAndForm, helpPanel);
            content.setResizeWeight(0.78);
            content.setDividerLocation(0.78);
            content.setBorder(null);
            add(content, BorderLayout.CENTER);
        } else {
            add(tableAndForm, BorderLayout.CENTER);
        }
    }
    
    /**
     * Performs set form save action.
     * @param listener the listener
     */
    public void setFormSaveAction(ActionListener listener) {
        saveEntryBtn.addActionListener(listener);
    }
    
    /**
     * Performs set form delete action.
     * @param listener the listener
     */
    public void setFormDeleteAction(ActionListener listener) {
        deleteEntryBtn.addActionListener(listener);
    }

    /**
     * Performs set search and filters visible.
     * @param visible the visible
     */
    public void setSearchAndFiltersVisible(boolean visible) {
        topContainer.setVisible(visible);
    }

    /**
     * Performs get table.
     * @return the get table result
     */
    public JTable getTable() {
        return table;
    }

    /**
     * Performs get sorter.
     * @return the get sorter result
     */
    public TableRowSorter<DefaultTableModel> getSorter() {
        return sorter;
    }

    /**
     * Performs get search field.
     * @return the get search field result
     */
    public JTextField getSearchField() {
        return searchField;
    }
    
    /**
     * Performs get key field.
     * @return the get key field result
     */
    public AutoCompleteComboBox getKeyField() {
        return keyField;
    }

    /**
     * Performs get value field.
     * @return the get value field result
     */
    public JTextField getValueField() {
        return valueField;
    }
    
    /**
     * Performs get help title.
     * @return the get help title result
     */
    public JLabel getHelpTitle() {
        return helpTitle;
    }
    
    /**
     * Performs get help text.
     * @return the get help text result
     */
    public JTextArea getHelpText() {
        return helpText;
    }

    private void setupSearchFiltering() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(final DocumentEvent e) { filter(); }
            @Override public void removeUpdate(final DocumentEvent e) { filter(); }
            @Override public void changedUpdate(final DocumentEvent e) { filter(); }
            private void filter() {
                final String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    final Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(text));
                    sorter.setRowFilter(new RowFilter<>() {
                        @Override
                        public boolean include(final Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                            for (int column = 0; column < entry.getValueCount(); column++) {
                                if (pattern.matcher(String.valueOf(entry.getValue(column))).find()) {
                                    return true;
                                }
                            }
                            final String key = String.valueOf(entry.getValue(keyColIdx));
                            final String description = PropertyHelp.getDescription(key);
                            return null != description && pattern.matcher(description).find();
                        }
                    });
                }
            }
        });
    }

    private void setupLiveFieldValidation() {
        final DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(final DocumentEvent e) { performLiveValidation(); }
            @Override public void removeUpdate(final DocumentEvent e) { performLiveValidation(); }
            @Override public void changedUpdate(final DocumentEvent e) { performLiveValidation(); }
        };
        final JTextComponent editor = (JTextComponent) keyField.getEditor().getEditorComponent();
        editor.getDocument().addDocumentListener(listener);
        valueField.getDocument().addDocumentListener(listener);
    }

    /**
     * Performs perform live validation.
     */
    public void performLiveValidation() {
        final String key = keyField.getText().trim();
        final String val = valueField.getText().trim();

        if (selectPartsBtn != null) {
            final boolean isInclude = key.toLowerCase().startsWith(".include") || key.toLowerCase().equals("include");
            selectPartsBtn.setVisible(isInclude);
        }

        final java.util.List<String> allowedValues = PropertyValidator.validValues(key);
        if (!allowedValues.isEmpty()) {
            propertyHintLabel.setText(" Allowed: " + String.join(", ", allowedValues));
            propertyHintLabel.setVisible(true);
        } else {
            propertyHintLabel.setVisible(false);
        }

        if (!key.isEmpty()) {
            final String err = PropertyValidator.validateProperty(key, val, null);
            if (isNotNull(err)) {
                liveValidationErrorLabel.setText("\u26a0\ufe0f " + err);
                liveValidationErrorLabel.setVisible(true);
                valueField.setBackground(new Color(0xFE, 0xF2, 0xF2));
                return;
            }
        }
        liveValidationErrorLabel.setVisible(false);
        valueField.setBackground(UIManager.getColor("TextField.background"));
    }

    private static void scanPartFiles(final Path baseDir, final Set<String> relativePaths) {
        if (isNull(baseDir) || !Files.exists(baseDir) || !Files.isDirectory(baseDir)) return;
        try (final var stream = Files.walk(baseDir)) {
            stream.filter(p -> p.toString().endsWith(".part") && Files.isRegularFile(p))
                    .forEach(p -> {
                        final Path relative = baseDir.relativize(p);
                        String relStr = relative.toString().replace('\\', '/');
                        if (relStr.endsWith(".part")) {
                            relStr = relStr.substring(0, relStr.length() - 5);
                        }
                        relativePaths.add(relStr);
                    });
        } catch (final Exception ignored) {}
    }

    /**
     * Performs set parts mode.
     * @param mode the mode
     */
    public void setPartsMode(final PartsMode mode) {
        this.partsMode = isNotNull(mode) ? mode : PartsMode.CONFIG_ONLY;
    }

    /**
     * Performs get parts mode.
     * @return the get parts mode result
     */
    public PartsMode getPartsMode() {
        return partsMode;
    }

    private void showPartsSelectionDialog() {
        final Set<String> relevantParts = new TreeSet<>();
        if (partsMode == PartsMode.CONFIG_ONLY) {
            scanPartFiles(Path.of(_CONFIG_), relevantParts);
            if (relevantParts.isEmpty()) {
                scanPartFiles(Path.of("config"), relevantParts);
            }
        } else {
            scanPartFiles(Path.of(_SUITES_), relevantParts);
            if (relevantParts.isEmpty()) {
                scanPartFiles(Path.of("suites"), relevantParts);
            }
        }

        if (relevantParts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No .part files found.", "No Parts Found", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final String currentVal = valueField.getText().trim();
        final Set<String> currentlySelected = new HashSet<>();
        if (!currentVal.isEmpty()) {
            for (final String s : currentVal.split(",")) {
                currentlySelected.add(s.trim());
            }
        }

        final List<JCheckBox> allCheckBoxes = new ArrayList<>();
        final JComponent contentComponent = createTabPanel(relevantParts, currentlySelected, allCheckBoxes);
        contentComponent.setPreferredSize(new Dimension(380, 240));

        final int res = JOptionPane.showConfirmDialog(
                this,
                contentComponent,
                "Select .part Files to Include",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (JOptionPane.OK_OPTION == res) {
            final List<String> chosen = allCheckBoxes.stream()
                    .filter(AbstractButton::isSelected)
                    .map(AbstractButton::getText)
                    .toList();
            valueField.setText(String.join(",", chosen));
            performLiveValidation();
        }
    }

    private static JPanel createTabPanel(final Set<String> parts, final Set<String> selected, final List<JCheckBox> globalList) {
        final JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        final List<JCheckBox> localList = new ArrayList<>();

        for (final String part : parts) {
            final JCheckBox cb = new JCheckBox(part);
            cb.setSelected(selected.contains(part));
            localList.add(cb);
            globalList.add(cb);
            listPanel.add(cb);
        }

        final JScrollPane scrollPane = new JScrollPane(listPanel);
        final JPanel panel = new JPanel(new BorderLayout(0, 6));

        final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        final JButton selectAllBtn = new JButton("Select All");
        selectAllBtn.addActionListener(e -> localList.forEach(cb -> cb.setSelected(true)));
        final JButton deselectAllBtn = new JButton("Deselect All");
        deselectAllBtn.addActionListener(e -> localList.forEach(cb -> cb.setSelected(false)));
        toolbar.add(selectAllBtn);
        toolbar.add(deselectAllBtn);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Performs clear form.
     */
    public void clearForm() {
        table.clearSelection();
        keyField.setEnabled(true);
        keyField.setText("");
        valueField.setText("");
        performLiveValidation();
    }

    /**
     * Performs populate form from selected row.
     */
    public void populateFormFromSelectedRow() {
        final int viewRow = table.getSelectedRow();
        if (viewRow >= 0) {
            final int modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                isUpdatingForm = true;
                try {
                    keyField.setText((String) tableModel.getValueAt(modelRow, keyColIdx));
                    valueField.setText((String) tableModel.getValueAt(modelRow, valColIdx));
                    keyField.setEnabled(false);
                } finally {
                    isUpdatingForm = false;
                }
                performLiveValidation();
            }
        } else {
            keyField.setEnabled(true);
        }
    }

    /**
     * Performs update help panel.
     */
    public void updateHelpPanel() {
        final int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            helpTitle.setText("Select a property to see help.");
            helpText.setText("");
            return;
        }
        final int modelRow = table.convertRowIndexToModel(viewRow);
        final String key = (String) tableModel.getValueAt(modelRow, keyColIdx);
        final String description = PropertyHelp.getDescription(key);
        helpTitle.setText(key);
        helpText.setText((null == description || description.isBlank()) ? "No help is available for this property." : description);
        helpText.setCaretPosition(0);
    }
}
