package works.lysenko.base.parameters;

import works.lysenko.base.Parameters;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Utility class for loading, populating and propagating GUI parameter fields.
 */
public final class GuiParameters {

    private GuiParameters() {
    }

    /**
     * Populates a JComboBox from files in directory matching a filter.
     *
     * @param type       parameter type key
     * @param directory  directory path
     * @param filter     file suffix filter
     * @param parameters Parameters object
     * @return populated JComboBox
     */
    public static JComboBox<Object> addFromFiles(final String type, final String directory, final String filter, final Parameters parameters) {

        final List<String> entityList = new LinkedList<>();
        final String propVal = parameters.getProperty(type);
        if (isNotNull(propVal) && !propVal.isEmpty()) {
            entityList.add(propVal);
        }

        final File dir = new File(directory);
        final File[] files = dir.listFiles((dir1, name) -> name.endsWith(filter));
        if (isNotNull(files)) {
            for (final File file : files) {
                final String entity = org.apache.commons.lang3.StringUtils.removeEnd(file.getName(), filter);
                if (!entityList.contains(entity)) entityList.add(entity);
            }
            entityList.sort(null);
        }

        final JComboBox<Object> comboBox = new JComboBox<>(entityList.toArray());
        comboBox.setSelectedItem(parameters.getProperty(type));
        return comboBox;
    }

    /**
     * Propagates text field property value if non-blank.
     *
     * @param parameters Parameters instance
     * @param tf         JTextField component
     * @param key        property key name
     */
    public static void propagatePropertyOptional(final Parameters parameters, final JTextField tf, final String key) {

        if (isNotNull(tf) && !tf.getText().isBlank()) {
            parameters.setProperty(key, tf.getText().trim());
        }
    }

    /**
     * Propagates check box selection state.
     *
     * @param parameters Parameters instance
     * @param cb         JCheckBox component
     * @param key        property key name
     */
    public static void propagatePropertyOptional(final Parameters parameters, final JCheckBox cb, final String key) {

        if (isNotNull(cb)) {
            parameters.setProperty(key, String.valueOf(cb.isSelected()));
        }
    }
}
