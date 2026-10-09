package works.lysenko.base.parameters;

import works.lysenko.Base;
import works.lysenko.base.TestProperties;
import works.lysenko.util.apis.properties._TestProperties;
import works.lysenko.util.spec.PropEnum;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.C.CLEAR;
import static works.lysenko.util.lang.word.C.CONFIGURED;
import static works.lysenko.util.lang.word.D.DEFAULT;
import static works.lysenko.util.lang.word.M.MODIFIED;
import static works.lysenko.util.lang.word.P.PROPERTY;
import static works.lysenko.util.lang.word.R.RESET;
import static works.lysenko.util.lang.word.S.SEARCH;
import static works.lysenko.util.lang.word.S.SELECTED;
import static works.lysenko.util.lang.word.S.STATUS;
import static works.lysenko.util.spec.Symbols._COLON_;

/**
 * Dialog for previewing and modifying test configuration properties before test execution.
 */
@SuppressWarnings({"ClassWithTooManyFields", "CallToSuspiciousStringMethod", "MagicNumber"})
public class PropertiesPanel extends JPanel {

    static {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            works.lysenko.base.ui.ControlPanel.applyMenuShortcutKeyMask();
        } catch (final Exception ignored) {}
    }

    public static final String STATUS_DEFAULT = c(DEFAULT);
    public static final String STATUS_CONFIGURED = c(CONFIGURED);
    public static final String STATUS_MODIFIED = c(MODIFIED);
    public static final String STATUS_CUSTOM = "Custom";
    public static final String STATUS_RESET_TO_DEFAULT = "Reset to Default";

    private final _TestProperties testProperties;
    private final Map<String, String> baseline = new LinkedHashMap<>();
    private final Map<String, String> overrides = new LinkedHashMap<>();
    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField searchField;
    private final JTextArea helpText;
    private final JLabel helpTitle;
    private final JButton resetSelectedBtn;
    private final JButton resetAllBtn;
    private final JButton addPropertyBtn;
    private final JButton copyPropertyNameBtn;
    private final JButton okBtn;
    private JButton cancelBtn; // Keeping variable around just in case to not break test, but we'll remove it from the panel
    private boolean confirmed = false;
    private boolean isUpdating = false;
    private final JLabel validationLabel = new JLabel(" ");

    /**
     * Constructs a new PropertiesDialog using Base.properties.
     *
     * @param owner         the parent window
     * @param testName      the selected test name
     * @param isHeadless    headless flag
     * @param isAllLeafs    all-leafs flag
     * @param allLeafsCount all-leafs count
     */
    public PropertiesPanel(final String testName, final boolean isHeadless,
                           final boolean isAllLeafs, final int allLeafsCount) {

        this((isNotNull(Base.properties)) ? Base.properties : new TestProperties(), testName, isHeadless, isAllLeafs, allLeafsCount);
    }

    public PropertiesPanel(final String testName, final boolean isHeadless,
                           final boolean isAllLeafs, final int allLeafsCount,
                           final JComponent extraSearchComponent) {

        this((isNotNull(Base.properties)) ? Base.properties : new TestProperties(), testName, isHeadless, isAllLeafs, allLeafsCount, extraSearchComponent);
    }

    /**
     * Constructs a new PropertiesDialog with the given test properties instance.
     *
     * @param owner         the parent window
     * @param testProps     the test properties instance
     * @param testName      the selected test name
     * @param isHeadless    headless flag
     * @param isAllLeafs    all-leafs flag
     * @param allLeafsCount all-leafs count
     */
    public PropertiesPanel(final _TestProperties testProps, final String testName,
                           final boolean isHeadless, final boolean isAllLeafs, final int allLeafsCount) {

        this(testProps, testName, isHeadless, isAllLeafs, allLeafsCount, null);
    }

    public PropertiesPanel(final _TestProperties testProps, final String testName,
                           final boolean isHeadless, final boolean isAllLeafs, final int allLeafsCount,
                           final JComponent extraSearchComponent) {

        this.testProperties = testProps;

        setLayout(new BorderLayout(8, 8));

        // 1. Top Panel (Search/Filter & Quick Filters)
        final JPanel topContainer = new JPanel(new BorderLayout());

        final JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        searchPanel.add(new JLabel(b(c(SEARCH), s(_COLON_))));
        searchField = new JTextField(12);
        final JButton clearBtn = new JButton(c(CLEAR));
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

        // 2. Table and Model
        final String[] columns = {c(PROPERTY), c(VALUE), c(DEFAULT), c(STATUS), "Execution parameter"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(final int r, final int c) {
                return 1 == c;
            }
        };

        populateProperties(testName, isHeadless, isAllLeafs, allLeafsCount);

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        setupTableRenderingAndEditing();
        setupSearchFiltering();

        final JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(0, 8, 0, 8));

        helpTitle = new JLabel("Select a property to see help.");
        helpText = new JTextArea();
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

        final JSplitPane content = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, helpPanel);
        content.setResizeWeight(0.78);
        content.setDividerLocation(0.78);
        content.setBorder(null);
        add(content, BorderLayout.CENTER);
        table.getSelectionModel().addListSelectionListener(this::onPropertySelected);

        // 3. Bottom Panel (Actions)
        final JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        final JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        copyPropertyNameBtn = new JButton("Copy Property Name");
        copyPropertyNameBtn.setToolTipText("Copy the selected property name to the clipboard");
        copyPropertyNameBtn.setEnabled(false);
        copyPropertyNameBtn.addActionListener(e -> copySelectedPropertyName());
        resetSelectedBtn = new JButton(b(c(RESET), c(SELECTED)));
        resetSelectedBtn.addActionListener(e -> onResetSelected());
        resetAllBtn = new JButton(b(c(RESET), "All"));
        resetAllBtn.addActionListener(e -> onResetAll());
        addPropertyBtn = new JButton("+ " + c(PROPERTY));
        addPropertyBtn.addActionListener(e -> onAddProperty());
        leftActions.add(copyPropertyNameBtn);
        leftActions.add(resetSelectedBtn);
        leftActions.add(resetAllBtn);
        leftActions.add(addPropertyBtn);

        final JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        // cancelBtn = new JButton("Cancel");
        // cancelBtn.addActionListener(e -> onCancel());
        okBtn = new JButton("Apply");
        okBtn.addActionListener(e -> applyChanges());
        // rightActions.add(cancelBtn);
        rightActions.add(okBtn);

        bottomPanel.add(leftActions, BorderLayout.WEST);
        validationLabel.setForeground(new Color(190, 0, 0));
        validationLabel.setBorder(new EmptyBorder(0, 8, 0, 8));
        bottomPanel.add(validationLabel, BorderLayout.CENTER);
        bottomPanel.add(rightActions, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Computes the status for a property row with an explicitly known default status.
     *
     * @param key            property key
     * @param value          current property value
     * @param defaultValue   default value (may be null or empty)
     * @param isDefaultKnown true if the property is defined in the default properties compendium
     * @param baseVal        baseline value before user overrides
     * @return status description string
     */
    public static String calculateStatus(final String key, final String value, final String defaultValue,
                                         final boolean isDefaultKnown, final String baseVal) {

        final String val = (null == value) ? "" : value;
        final String def = (null == defaultValue) ? "" : defaultValue;
        final boolean isValDefault = isDefaultKnown && val.equals(def);
        final boolean isChangedFromBaseline = (null == baseVal) ? !val.isEmpty() : !val.equals(baseVal);

        if (isChangedFromBaseline) {
            return isValDefault ? STATUS_RESET_TO_DEFAULT : STATUS_MODIFIED;
        }
        if (isValDefault) {
            return STATUS_DEFAULT;
        }
        if (isDefaultKnown) {
            return STATUS_CONFIGURED;
        }
        return STATUS_CUSTOM;
    }

    /**
     * Computes the status for a property row.
     *
     * @param key          property key
     * @param value        current property value
     * @param defaultValue default value (may be null or empty)
     * @param baseVal      baseline value before user overrides
     * @return status description string
     */
    public static String calculateStatus(final String key, final String value, final String defaultValue, final String baseVal) {

        final boolean isDefaultKnown = isNotNull(defaultValue);
        return calculateStatus(key, value, defaultValue, isDefaultKnown, baseVal);
    }

    private void populateProperties(final String testName, final boolean isHeadless, final boolean isAllLeafs,
                                    final int allLeafsCount) {

        if (isNotNull(testProperties)) {
            final Map<String, String> originalConfig = testProperties.getOriginalConfigProperties(testName);
            if (isNotNull(originalConfig)) {
                baseline.putAll(originalConfig);
            }
            final Map<String, String> defaults = testProperties.getDefaults();

            final Map<String, String> effective = testProperties.resolveEffectiveProperties(testName, isHeadless, isAllLeafs,
                    allLeafsCount, true);

            final Set<String> allKeys = new TreeSet<>(Comparator.naturalOrder());
            if (isNotNull(effective)) allKeys.addAll(effective.keySet());
            if (isNotNull(defaults)) allKeys.addAll(defaults.keySet());
            allKeys.addAll(baseline.keySet());

            for (final String key : allKeys) {
                final boolean isDefaultKnown = isNotNull(defaults) && defaults.containsKey(key);
                final String val = (isNotNull(effective) && effective.containsKey(key)) ? effective.get(key) : "";
                final String def = isDefaultKnown ? defaults.get(key) : "";
                final String safeVal = (null == val) ? "" : val;
                final String safeDef = (null == def) ? "" : def;
                final String baseVal = baseline.get(key);
                final String st = calculateStatus(key, safeVal, safeDef, isDefaultKnown, baseVal);
                model.addRow(new Object[]{key, safeVal, safeDef, st, executionParameterName(key)});
            }
        }
    }

    private static String executionParameterName(final String propertyName) {

        for (final PropEnum property : PropEnum.values()) {
            if (property.executionParameter() && property.getPropertyName().equals(propertyName)) {
                return property.name().substring(1);
            }
        }
        return "";
    }

    private void setupTableRenderingAndEditing() {

        final DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable tbl, final Object val,
                                                           final boolean isSelected, final boolean hasFocus,
                                                           final int r, final int c) {
                final Component comp = super.getTableCellRendererComponent(tbl, val, isSelected, hasFocus, r, c);
                if (!isSelected) {
                    final int modelRow = tbl.convertRowIndexToModel(r);
                    final String st = (String) model.getValueAt(modelRow, 3);
                    if (STATUS_MODIFIED.equals(st)) {
                        comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                        comp.setForeground(new Color(180, 80, 0));
                    } else if (STATUS_RESET_TO_DEFAULT.equals(st)) {
                        comp.setFont(comp.getFont().deriveFont(Font.ITALIC | Font.BOLD));
                        comp.setForeground(new Color(0, 130, 0));
                    } else if (STATUS_CONFIGURED.equals(st)) {
                        comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                        comp.setForeground(new Color(0, 80, 180));
                    } else if (STATUS_CUSTOM.equals(st)) {
                        comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                        comp.setForeground(new Color(120, 0, 120));
                    } else {
                        comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
                        comp.setForeground(tbl.getForeground());
                    }
                }
                return comp;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        table.getColumnModel().getColumn(1).setCellEditor(new ValidatingCellEditor());

        model.addTableModelListener(e -> {
            if (isUpdating) return;
            final int col = e.getColumn();
            final int row = e.getFirstRow();
            if (1 == col && row >= 0 && row < model.getRowCount()) {
                isUpdating = true;
                try {
                    final String k = (String) model.getValueAt(row, 0);
                    final String v = (String) model.getValueAt(row, 1);
                    final String d = (String) model.getValueAt(row, 2);
                    final boolean isDefaultKnown = isNotNull(testProperties) && isNotNull(testProperties.getDefaults()) &&
                            testProperties.getDefaults().containsKey(k);
                    final String baseVal = baseline.get(k);
                    final String st = calculateStatus(k, (null == v) ? "" : v, (null == d) ? "" : d,
                            isDefaultKnown, baseVal);
                    model.setValueAt(st, row, 3);
                } finally {
                    isUpdating = false;
                }
            }
        });
    }

    private void setupSearchFiltering() {

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                filter();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                filter();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                filter();
            }

            private void filter() {
                final String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    final Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(text));
                    final RowFilter<DefaultTableModel, Integer> rowFilter = new RowFilter<>() {
                        @Override
                        public boolean include(final Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                            for (int column = 0; column < entry.getValueCount(); column++) {
                                if (pattern.matcher(String.valueOf(entry.getValue(column))).find()) {
                                    return true;
                                }
                            }
                            final String key = String.valueOf(entry.getValue(0));
                            final String description = PropertyHelp.getDescription(key);
                            return null != description && pattern.matcher(description).find();
                        }
                    };
                    sorter.setRowFilter(rowFilter);
                }
            }
        });
    }

    private void onPropertySelected(final ListSelectionEvent event) {

        if (event.getValueIsAdjusting()) return;
        final int viewRow = table.getSelectedRow();
        copyPropertyNameBtn.setEnabled(viewRow >= 0);
        if (viewRow < 0) {
            helpTitle.setText("Select a property to see help.");
            helpText.setText("");
            return;
        }
        final int modelRow = table.convertRowIndexToModel(viewRow);
        final String key = (String) model.getValueAt(modelRow, 0);
        final String description = PropertyHelp.getDescription(key);
        helpTitle.setText(key);
        helpText.setText((null == description || description.isBlank())
                ? "No help is available for this property."
                : description);
        helpText.setCaretPosition(0);
    }

    private void copySelectedPropertyName() {

        final int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        final int modelRow = table.convertRowIndexToModel(viewRow);
        final String propertyName = (String) model.getValueAt(modelRow, 0);
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(propertyName), null);
            validationLabel.setText(" ");
        } catch (final IllegalStateException e) {
            validationLabel.setText("Unable to copy property name: clipboard is unavailable.");
        }
    }

    private void onResetSelected() {

        final int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        final int modelRow = table.convertRowIndexToModel(viewRow);
        final String k = (String) model.getValueAt(modelRow, 0);
        if (baseline.containsKey(k)) {
            final String base = baseline.get(k);
            model.setValueAt((null == base) ? "" : base, modelRow, 1);
        } else if (isNotNull(testProperties) && isNotNull(testProperties.getDefaults()) &&
                testProperties.getDefaults().containsKey(k)) {
            final String def = testProperties.getDefaults().get(k);
            model.setValueAt((null == def) ? "" : def, modelRow, 1);
        } else {
            model.setValueAt("", modelRow, 1);
        }
    }

    private void onResetAll() {

        for (int r = 0; r < model.getRowCount(); r++) {
            final String k = (String) model.getValueAt(r, 0);
            if (baseline.containsKey(k)) {
                final String base = baseline.get(k);
                model.setValueAt((null == base) ? "" : base, r, 1);
            } else if (isNotNull(testProperties) && isNotNull(testProperties.getDefaults()) &&
                    testProperties.getDefaults().containsKey(k)) {
                final String def = testProperties.getDefaults().get(k);
                model.setValueAt((null == def) ? "" : def, r, 1);
            } else {
                model.setValueAt("", r, 1);
            }
        }
    }

    private void onAddProperty() {

        final JTextField propNameField = new JTextField(20);
        final JTextField propValueField = new JTextField(20);
        final JPanel addPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        addPanel.add(new JLabel(b(c(PROPERTY), s(_COLON_))));
        addPanel.add(propNameField);
        addPanel.add(new JLabel(b(c(VALUE), s(_COLON_))));
        addPanel.add(propValueField);

        while (true) {
            final int res = JOptionPane.showConfirmDialog(this, addPanel, "Add Custom Property",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (JOptionPane.OK_OPTION != res) return;
            final String name = propNameField.getText().trim();
            final String value = propValueField.getText().trim();
            if (name.isEmpty()) return;
            final String error = PropertyValidator.validate(name, value);
            if (null != error) {
                JOptionPane.showMessageDialog(this, error, "Invalid value for " + name, JOptionPane.ERROR_MESSAGE);
                continue;
            }
            boolean found = false;
            for (int r = 0; r < model.getRowCount(); r++) {
                if (name.equals(model.getValueAt(r, 0))) {
                    model.setValueAt(value, r, 1);
                    found = true;
                    break;
                }
            }
            if (!found) {
                model.addRow(new Object[]{name, value, "", STATUS_CUSTOM, ""});
            }
            return;
        }
    }

    /**
     * Checks all modified values; selects the first invalid row and reports the problem.
     *
     * @return true if every modified value is valid
     */
    private boolean validateModifiedValues() {

        for (int r = 0; r < model.getRowCount(); r++) {
            final String k = (String) model.getValueAt(r, 0);
            final String v = (String) model.getValueAt(r, 1);
            final String val = (null == v) ? "" : v;
            final String baseVal = baseline.get(k);
            final boolean changed = (null == baseVal) ? !val.isEmpty() : !val.equals(baseVal);
            final String error = changed ? PropertyValidator.validate(k, val) : null;
            if (null != error) {
                final int viewRow = table.convertRowIndexToView(r);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 1, true));
                }
                validationLabel.setText(k + ": " + error);
                JOptionPane.showMessageDialog(this, error, "Invalid value for " + k, JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    /**
     * Text cell editor that refuses to commit a value failing {@link PropertyValidator}.
     */
    private final class ValidatingCellEditor extends AbstractCellEditor implements TableCellEditor {

        private JTextField field;
        private JComboBox<String> comboBox;
        private javax.swing.border.Border normalBorder;
        private String propertyName;

        @Override
        public Component getTableCellEditorComponent(final JTable tbl, final Object value, final boolean isSelected,
                                                     final int row, final int column) {
            final int modelRow = tbl.convertRowIndexToModel(row);
            propertyName = (String) model.getValueAt(modelRow, 0);
            final String currentValue = null == value ? "" : value.toString();
            final List<String> validValues = PropertyValidator.validValues(propertyName);
            if (!validValues.isEmpty() &&
                    (currentValue.isEmpty() ||
                            validValues.stream().anyMatch(option -> option.equalsIgnoreCase(currentValue)))) {
                comboBox = new JComboBox<>(validValues.toArray(new String[0]));
                comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
                comboBox.setSelectedItem(validValues.stream()
                        .filter(option -> option.equalsIgnoreCase(currentValue))
                        .findFirst()
                        .orElse(validValues.get(0)));
                field = null;
                normalBorder = null;
                return comboBox;
            }
            field = new JTextField(currentValue);
            normalBorder = field.getBorder();
            comboBox = null;
            return field;
        }

        @Override
        public Object getCellEditorValue() {
            return null != comboBox ? comboBox.getSelectedItem() : field.getText();
        }

        @Override
        public boolean stopCellEditing() {
            final String value = String.valueOf(getCellEditorValue());
            final String error = PropertyValidator.validate(propertyName, value);
            if (null != error) {
                if (null != field) {
                    field.setBorder(BorderFactory.createLineBorder(new Color(190, 0, 0), 2));
                    field.setToolTipText(error);
                }
                validationLabel.setText(propertyName + ": " + error);
                return false;
            }
            if (null != field) {
                field.setBorder(normalBorder);
                field.setToolTipText(null);
            }
            validationLabel.setText(" ");
            fireEditingStopped();
            return true;
        }

        @Override
        public void cancelCellEditing() {
            if (null != field) {
                field.setBorder(normalBorder);
                field.setToolTipText(null);
            }
            validationLabel.setText(" ");
            fireEditingCanceled();
        }
    }

    public void applyChanges() {

        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
            return;
        }
        if (!validateModifiedValues()) {
            return;
        }
        overrides.clear();
        for (int r = 0; r < model.getRowCount(); r++) {
            final String k = (String) model.getValueAt(r, 0);
            final String v = (String) model.getValueAt(r, 1);
            final String val = (null == v) ? "" : v;
            final String baseVal = baseline.get(k);
            final boolean changed = (null == baseVal) ? !val.isEmpty() : !val.equals(baseVal);
            if (changed) {
                overrides.put(k, val);
            }
        }
        if (isNotNull(testProperties)) {
            testProperties.setUserOverrides(overrides);
        }
        confirmed = true;
    }

    // Removed onCancel() as it's no longer necessary

    public boolean isConfirmed() {

        return confirmed;
    }

    public Map<String, String> getOverrides() {

        return Collections.unmodifiableMap(overrides);
    }

    public Map<String, String> getBaseline() {

        return Collections.unmodifiableMap(baseline);
    }

    public Map<String, String> getConfigDefaults() {

        return Collections.unmodifiableMap(baseline);
    }

    public DefaultTableModel getModel() {

        return model;
    }

    public JTable getTable() {

        return table;
    }

    public JTextField getSearchField() {

        return searchField;
    }

    public JTextArea getHelpText() {

        return helpText;
    }

    public JLabel getHelpTitle() {

        return helpTitle;
    }

    public JButton getResetSelectedButton() {

        return resetSelectedBtn;
    }

    public JButton getCopyPropertyNameButton() {

        return copyPropertyNameBtn;
    }

    public JButton getResetAllButton() {

        return resetAllBtn;
    }

    public JButton getOkButton() {

        return okBtn;
    }

    public JButton getCancelButton() {

        return cancelBtn;
    }
}
