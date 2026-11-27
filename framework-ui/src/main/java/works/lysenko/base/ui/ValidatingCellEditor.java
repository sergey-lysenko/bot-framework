package works.lysenko.base.ui;

import works.lysenko.base.parameters.PropertyValidator;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * The ValidatingCellEditor class.
 */
public final class ValidatingCellEditor extends AbstractCellEditor implements TableCellEditor {

    private static final Color FOCUS_BORDER_COLOR = new Color(0x3B, 0x82, 0xF6);
    private static final Color ERROR_BORDER_COLOR = new Color(190, 0, 0);

    private JTextField field = null;
    private JComboBox<String> comboBox = null;
    private Border normalBorder = null;
    private String propertyName = null;
    private final int keyColumnIndex;
    private final Consumer<? super String> validationErrorCallback;

    /**
     * Constructs a new ValidatingCellEditor.
     *
     * @param keyColumnIndex          the keyColumnIndex
     * @param validationErrorCallback the validationErrorCallback
     */
    public ValidatingCellEditor(final int keyColumnIndex, final Consumer<? super String> validationErrorCallback) {
        this.keyColumnIndex = keyColumnIndex;
        this.validationErrorCallback = validationErrorCallback;
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
                                                 final int row, final int column) {
        final int modelRow = table.convertRowIndexToModel(row);
        propertyName = (String) table.getModel().getValueAt(modelRow, keyColumnIndex);
        final String currentValue = null == value ? "" : value.toString();
        final String normalizedCurrent = currentValue.toLowerCase(Locale.ROOT);
        final List<String> validValues = PropertyValidator.validValues(propertyName);
        final Component editor;

        if (!validValues.isEmpty() &&
                (currentValue.isEmpty() ||
                        validValues.stream().anyMatch(option -> matchesNormalized(option, normalizedCurrent)))) {
            comboBox = new JComboBox<>(validValues.toArray(String[]::new));
            comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
            comboBox.setForeground(Color.BLACK);
            comboBox.setBackground(Color.WHITE);
            comboBox.setSelectedItem(validValues.stream()
                    .filter(option -> matchesNormalized(option, normalizedCurrent))
                    .findFirst()
                    .orElse(validValues.getFirst()));
            field = null;
            normalBorder = null;
            editor = comboBox;
        } else {
            field = new JTextField(currentValue);
            field.setFont(table.getFont());
            field.setForeground(Color.BLACK);
            field.setBackground(Color.WHITE);
            field.setCaretColor(Color.BLACK);

            normalBorder = UIManager.getBorder("TextField.border");
            if (null == normalBorder) {
                normalBorder = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(FOCUS_BORDER_COLOR, 1),
                        BorderFactory.createEmptyBorder(2, 4, 2, 4)
                );
            }
            field.setBorder(normalBorder);

            comboBox = null;
            editor = field;
        }
        return editor;
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
                field.setBorder(BorderFactory.createLineBorder(ERROR_BORDER_COLOR, 2));
                field.setToolTipText(error);
            }
            if (isNotNull(validationErrorCallback)) {
                validationErrorCallback.accept(s(propertyName, ": ", error));
            }
        } else {
            if (null != field) {
                field.setBorder(normalBorder);
                field.setToolTipText(null);
            }
            if (isNotNull(validationErrorCallback)) {
                validationErrorCallback.accept(null); // Clear error
            }
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

    private static boolean matchesNormalized(final String option, final String normalizedTarget) {
        return Objects.equals(option.toLowerCase(Locale.ROOT), normalizedTarget);
    }
}
