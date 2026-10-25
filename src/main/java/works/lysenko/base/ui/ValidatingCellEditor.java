package works.lysenko.base.ui;

import works.lysenko.base.parameters.PropertyValidator;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * The ValidatingCellEditor class.
 */
public class ValidatingCellEditor extends AbstractCellEditor implements TableCellEditor {

    private JTextField field;
    private JComboBox<String> comboBox;
    private javax.swing.border.Border normalBorder;
    private String propertyName;
    private final int keyColumnIndex;
    private final Consumer<String> validationErrorCallback;

    /**
     * Constructs a new ValidatingCellEditor.
     * @param keyColumnIndex the keyColumnIndex
     * @param validationErrorCallback the validationErrorCallback
     */
    public ValidatingCellEditor(final int keyColumnIndex, final Consumer<String> validationErrorCallback) {
        this.keyColumnIndex = keyColumnIndex;
        this.validationErrorCallback = validationErrorCallback;
    }

    @Override
    public Component getTableCellEditorComponent(final JTable tbl, final Object value, final boolean isSelected,
                                                 final int row, final int column) {
        final int modelRow = tbl.convertRowIndexToModel(row);
        propertyName = (String) tbl.getModel().getValueAt(modelRow, keyColumnIndex);
        final String currentValue = null == value ? "" : value.toString();
        final List<String> validValues = PropertyValidator.validValues(propertyName);

        if (!validValues.isEmpty() &&
                (currentValue.isEmpty() ||
                        validValues.stream().anyMatch(option -> option.equalsIgnoreCase(currentValue)))) {
            comboBox = new JComboBox<>(validValues.toArray(new String[0]));
            comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
            comboBox.setForeground(Color.BLACK);
            comboBox.setBackground(Color.WHITE);
            comboBox.setSelectedItem(validValues.stream()
                    .filter(option -> option.equalsIgnoreCase(currentValue))
                    .findFirst()
                    .orElse(validValues.get(0)));
            field = null;
            normalBorder = null;
            return comboBox;
        }

        field = new JTextField(currentValue);
        field.setFont(tbl.getFont());
        field.setForeground(Color.BLACK);
        field.setBackground(Color.WHITE);
        field.setCaretColor(Color.BLACK);
        
        normalBorder = UIManager.getBorder("TextField.border");
        if (normalBorder == null) {
            normalBorder = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x3B, 0x82, 0xF6), 1),
                    BorderFactory.createEmptyBorder(2, 4, 2, 4)
            );
        }
        field.setBorder(normalBorder);
        
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
        if (isNotNull(error)) {
            if (null != field) {
                field.setBorder(BorderFactory.createLineBorder(new Color(190, 0, 0), 2));
                field.setToolTipText(error);
            }
            if (isNotNull(validationErrorCallback)) {
                validationErrorCallback.accept(propertyName + ": " + error);
            }
            fireEditingStopped();
            return true;
        }
        if (null != field) {
            field.setBorder(normalBorder);
            field.setToolTipText(null);
        }
        if (isNotNull(validationErrorCallback)) {
            validationErrorCallback.accept(null); // Clear error
        }
        fireEditingStopped();
        return true;
    }

    @Override
    public void cancelCellEditing() {
        if (null != field) {
            field.setBorder(normalBorder);
            field.setToolTipText(null);
        }
        if (isNotNull(validationErrorCallback)) {
            validationErrorCallback.accept(null);
        }
        fireEditingCanceled();
    }
}
