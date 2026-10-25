package works.lysenko.base.parameters;

import works.lysenko.Base;
import works.lysenko.base.TestProperties;
import works.lysenko.util.apis._PropEnum;
import works.lysenko.util.apis.properties._TestProperties;
import works.lysenko.base.ui.PropertyTablePanel;

import works.lysenko.base.ui.ValidatingCellEditor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;

import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.C.CONFIGURED;
import static works.lysenko.util.lang.word.D.DEFAULT;
import static works.lysenko.util.lang.word.M.MODIFIED;
import static works.lysenko.util.lang.word.P.PROPERTY;
import static works.lysenko.util.lang.word.R.RESET;
import static works.lysenko.util.lang.word.S.SELECTED;
import static works.lysenko.util.lang.word.S.STATUS;

/**
 * Dialog for previewing and modifying test configuration properties before test execution.
 */
@SuppressWarnings({"ClassWithTooManyFields", "CallToSuspiciousStringMethod", "MagicNumber"})
public class PropertiesPanel extends JPanel {

    static {
        works.lysenko.base.ui.ControlPanel.initializeLookAndFeel();
    }

    /** The status_default. */
    public static final String STATUS_DEFAULT = c(DEFAULT);
    /** The status_configured. */
    public static final String STATUS_CONFIGURED = c(CONFIGURED);
    /** The status_modified. */
    public static final String STATUS_MODIFIED = c(MODIFIED);
    /** The status_custom. */
    public static final String STATUS_CUSTOM = "Custom";
    /** The status_reset_to_default. */
    public static final String STATUS_RESET_TO_DEFAULT = "Reset to Default";

    private final _TestProperties testProperties;
    private final Map<String, String> baseline = new LinkedHashMap<>();
    private final Map<String, String> overrides = new LinkedHashMap<>();
    private final DefaultTableModel model;
    private final PropertyTablePanel propertyTablePanel;
    private final JButton resetSelectedBtn;
    private final JButton resetAllBtn;
    private final JButton copyPropertyNameBtn;
    private final JButton okBtn;
    private JButton cancelBtn; // Keeping variable around just in case to not break test, but we'll remove it from the panel
    private boolean confirmed = false;
    private boolean isUpdating = false;
    private final JLabel validationLabel = new JLabel(" ");

    /**
     * Constructs a new PropertiesDialog using Base.properties.
     *
     * @param testName      the selected test name
     * @param isHeadless    headless flag
     * @param isAllLeafs    all-leafs flag
     * @param allLeafsExecutions all-leafs count
     */
    public PropertiesPanel(final String testName, final boolean isHeadless,
                           final boolean isAllLeafs, final int allLeafsExecutions) {

        this((isNotNull(Base.properties)) ? Base.properties : new TestProperties(), testName, isHeadless, isAllLeafs, allLeafsExecutions);
    }

    public PropertiesPanel(final String testName, final boolean isHeadless,
                           final boolean isAllLeafs, final int allLeafsExecutions,
                           final JComponent extraSearchComponent) {

        this((isNotNull(Base.properties)) ? Base.properties : new TestProperties(), testName, isHeadless, isAllLeafs, allLeafsExecutions, extraSearchComponent);
    }

    /**
     * Constructs a new PropertiesDialog with the given test properties instance.
     *
     * @param testProps     the test properties instance
     * @param testName      the selected test name
     * @param isHeadless    headless flag
     * @param isAllLeafs    all-leafs flag
     * @param allLeafsExecutions all-leafs count
     */
    public PropertiesPanel(final _TestProperties testProps, final String testName,
                           final boolean isHeadless, final boolean isAllLeafs, final int allLeafsExecutions) {

        this(testProps, testName, isHeadless, isAllLeafs, allLeafsExecutions, null);
    }

    public PropertiesPanel(final _TestProperties testProps, final String testName,
                           final boolean isHeadless, final boolean isAllLeafs, final int allLeafsExecutions,
                           final JComponent extraSearchComponent) {

        this.testProperties = testProps;

        setLayout(new BorderLayout(8, 8));

        // 2. Table and Model
        final String[] columns = {c(PROPERTY), "Scope", c(VALUE), c(DEFAULT), c(STATUS), "Execution parameter"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(final int r, final int c) {
                return 2 == c;
            }

            @Override
            public void setValueAt(final Object aValue, final int r, final int c) {
                if (r >= 0 && r < getRowCount() && c >= 0 && c < getColumnCount()) {
                    super.setValueAt(aValue, r, c);
                }
            }
        };

        populateProperties(testName, isHeadless, isAllLeafs, allLeafsExecutions);

        propertyTablePanel = new PropertyTablePanel(model, 0, 2, true, extraSearchComponent);
        propertyTablePanel.setPartsMode(PropertyTablePanel.PartsMode.CONFIG_ONLY);

        setupTableRenderingAndEditing();

        add(propertyTablePanel, BorderLayout.CENTER);
        propertyTablePanel.getTable().getSelectionModel().addListSelectionListener(this::onPropertySelected);

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
        leftActions.add(copyPropertyNameBtn);
        leftActions.add(resetSelectedBtn);
        leftActions.add(resetAllBtn);

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

    /**
     * Performs reload from disk operation.
     */
    public void reloadFromDisk(final String testName, final boolean isHeadless, final boolean isAllLeafs,
                               final int allLeafsExecutions) {

        if (isNotNull(testProperties)) {
            testProperties.readCommonConfiguration();
            testProperties.readTestConfiguration();
        }
        baseline.clear();
        model.setRowCount(0);
        populateProperties(testName, isHeadless, isAllLeafs, allLeafsExecutions);
    }

    private void populateProperties(final String testName, final boolean isHeadless, final boolean isAllLeafs,
                                    final int allLeafsExecutions) {

        if (isNotNull(testProperties)) {
            final Map<String, String> originalConfig = testProperties.getOriginalConfigProperties(testName);
            if (isNotNull(originalConfig)) {
                baseline.putAll(originalConfig);
            }
            final Map<String, String> defaults = testProperties.getDefaults();

            final Map<String, String> effective = testProperties.resolveEffectiveProperties(testName, isHeadless, isAllLeafs,
                    allLeafsExecutions, true);

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
                final String scope = PropertyValidator.determinePropertyScope(key);
                model.addRow(new Object[]{key, scope, safeVal, safeDef, st, executionParameterName(key)});
            }
        }
    }

    private static String executionParameterName(final String propertyName) {

        for (final _PropEnum property : PropertyValidator.getAllPropEnumConstants()) {
            if (property.executionParameter() && property.getPropertyName().equalsIgnoreCase(propertyName)) {
                return property.name().startsWith("_") ? property.name().substring(1) : property.name();
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
                    final String st = (String) model.getValueAt(modelRow, 4);
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

                    if (1 == c) {
                        final String scope = (String) model.getValueAt(modelRow, 1);
                        if ("Project".equals(scope)) {
                            comp.setForeground(new Color(130, 0, 150));
                            comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                        } else {
                            comp.setForeground(new Color(0, 100, 180));
                        }
                    }
                }
                return comp;
            }
        };

        final JTable table = propertyTablePanel.getTable();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        table.getColumnModel().getColumn(1).setMaxWidth(100);
        table.getColumnModel().getColumn(2).setCellEditor(new ValidatingCellEditor(0, msg -> {
            if (msg != null) {
                validationLabel.setText(msg);
            } else {
                validationLabel.setText(" ");
            }
        }));

        model.addTableModelListener(e -> {
            if (isUpdating) return;
            final int col = e.getColumn();
            final int row = e.getFirstRow();
            if (2 == col && row >= 0 && row < model.getRowCount()) {
                isUpdating = true;
                try {
                    final String k = (String) model.getValueAt(row, 0);
                    final String v = (String) model.getValueAt(row, 2);
                    final String d = (String) model.getValueAt(row, 3);
                    final boolean isDefaultKnown = isNotNull(testProperties) && isNotNull(testProperties.getDefaults()) &&
                            testProperties.getDefaults().containsKey(k);
                    final String baseVal = baseline.get(k);
                    final String st = calculateStatus(k, (null == v) ? "" : v, (null == d) ? "" : d,
                            isDefaultKnown, baseVal);
                    model.setValueAt(st, row, 4);
                } finally {
                    isUpdating = false;
                }
            }
        });
    }


    private void onPropertySelected(final ListSelectionEvent event) {

        if (event.getValueIsAdjusting()) return;
        final JTable table = propertyTablePanel.getTable();
        final int viewRow = table.getSelectedRow();
        copyPropertyNameBtn.setEnabled(viewRow >= 0);
    }

    private void copySelectedPropertyName() {

        final JTable table = propertyTablePanel.getTable();
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

        final JTable table = propertyTablePanel.getTable();
        final int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        final int modelRow = table.convertRowIndexToModel(viewRow);
        final String k = (String) model.getValueAt(modelRow, 0);
        if (baseline.containsKey(k)) {
            final String base = baseline.get(k);
            model.setValueAt((null == base) ? "" : base, modelRow, 2);
        } else if (isNotNull(testProperties) && isNotNull(testProperties.getDefaults()) &&
                testProperties.getDefaults().containsKey(k)) {
            final String def = testProperties.getDefaults().get(k);
            model.setValueAt((null == def) ? "" : def, modelRow, 2);
        } else {
            model.setValueAt("", modelRow, 2);
        }
    }

    private void onResetAll() {

        for (int r = 0; r < model.getRowCount(); r++) {
            final String k = (String) model.getValueAt(r, 0);
            if (baseline.containsKey(k)) {
                final String base = baseline.get(k);
                model.setValueAt((null == base) ? "" : base, r, 2);
            } else if (isNotNull(testProperties) && isNotNull(testProperties.getDefaults()) &&
                    testProperties.getDefaults().containsKey(k)) {
                final String def = testProperties.getDefaults().get(k);
                model.setValueAt((null == def) ? "" : def, r, 2);
            } else {
                model.setValueAt("", r, 2);
            }
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
            final String v = (String) model.getValueAt(r, 2);
            final String val = (null == v) ? "" : v;
            final String baseVal = baseline.get(k);
            final boolean changed = (null == baseVal) ? !val.isEmpty() : !val.equals(baseVal);
            final String error = changed ? PropertyValidator.validate(k, val) : null;
            if (null != error) {
                final JTable table = propertyTablePanel.getTable();
                final int viewRow = table.convertRowIndexToView(r);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 2, true));
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

    public void applyChanges() {

        final JTable table = propertyTablePanel.getTable();
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
            return;
        }
        if (!validateModifiedValues()) {
            return;
        }
        overrides.clear();
        for (int r = 0; r < model.getRowCount(); r++) {
            final String k = (String) model.getValueAt(r, 0);
            final String v = (String) model.getValueAt(r, 2);
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

    /**
     * Performs is confirmed.
     * @return the is confirmed result
     */
    public boolean isConfirmed() {

        return confirmed;
    }

    /**
     * Performs get overrides.
     * @return the get overrides result
     */
    public Map<String, String> getOverrides() {

        return Collections.unmodifiableMap(overrides);
    }

    /**
     * Performs get baseline.
     * @return the get baseline result
     */
    public Map<String, String> getBaseline() {

        return Collections.unmodifiableMap(baseline);
    }

    /**
     * Performs get config defaults.
     * @return the get config defaults result
     */
    public Map<String, String> getConfigDefaults() {

        return Collections.unmodifiableMap(baseline);
    }

    /**
     * Performs get model.
     * @return the get model result
     */
    public DefaultTableModel getModel() {

        return model;
    }

    /**
     * Performs get table.
     * @return the get table result
     */
    public JTable getTable() {

        return propertyTablePanel.getTable();
    }

    /**
     * Performs get search field.
     * @return the get search field result
     */
    public JTextField getSearchField() {

        return propertyTablePanel.getSearchField();
    }

    /**
     * Performs get help text.
     * @return the get help text result
     */
    public JTextArea getHelpText() {

        return propertyTablePanel.getHelpText();
    }

    /**
     * Performs get help title.
     * @return the get help title result
     */
    public JLabel getHelpTitle() {

        return propertyTablePanel.getHelpTitle();
    }

    /**
     * Performs get reset selected button.
     * @return the get reset selected button result
     */
    public JButton getResetSelectedButton() {

        return resetSelectedBtn;
    }

    /**
     * Performs get copy property name button.
     * @return the get copy property name button result
     */
    public JButton getCopyPropertyNameButton() {

        return copyPropertyNameBtn;
    }

    /**
     * Performs get reset all button.
     * @return the get reset all button result
     */
    public JButton getResetAllButton() {

        return resetAllBtn;
    }

    /**
     * Performs get ok button.
     * @return the get ok button result
     */
    public JButton getOkButton() {

        return okBtn;
    }

    /**
     * Performs get cancel button.
     * @return the get cancel button result
     */
    public JButton getCancelButton() {

        return cancelBtn;
    }
}
