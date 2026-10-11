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
        scanDirectoryFiles(dir, "", filter, entityList);
        entityList.sort(null);

        final JComboBox<Object> comboBox = new JComboBox<>(entityList.toArray());
        comboBox.setSelectedItem(parameters.getProperty(type));
        return comboBox;
    }

    private static void scanDirectoryFiles(final File dir, final String prefix, final String filter, final List<String> entityList) {

        if (!dir.exists() || !dir.isDirectory()) return;

        final File[] files = dir.listFiles();
        if (isNotNull(files)) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    scanDirectoryFiles(file, prefix + file.getName() + "/", filter, entityList);
                } else if (file.getName().endsWith(filter) || file.getName().endsWith(".part")) {
                    final String rawName = file.getName();
                    final String entityName = rawName.contains(".") ? rawName.substring(0, rawName.lastIndexOf('.')) : rawName;
                    final String fullEntity = prefix + entityName;
                    if (!entityList.contains(fullEntity)) {
                        entityList.add(fullEntity);
                    }
                }
            }
        }
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
