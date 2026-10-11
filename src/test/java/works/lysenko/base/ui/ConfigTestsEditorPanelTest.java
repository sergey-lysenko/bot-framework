package works.lysenko.base.ui;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTestsEditorPanelTest {

    @Test
    void instantiatesAndLoadsFilesWithoutExceptions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertDoesNotThrow(() -> {
                final ConfigTestsEditorPanel panel = new ConfigTestsEditorPanel();
                assertNotNull(panel);
            });
        });
    }

    @Test
    void testSaveEntryDoesNotThrowIndexOutOfBoundsException() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertDoesNotThrow(() -> {
                final ConfigTestsEditorPanel panel = new ConfigTestsEditorPanel();
                final PropertyTablePanel propertyTablePanel = findPropertyTablePanel(panel);
                assertNotNull(propertyTablePanel);

                final JTable table = propertyTablePanel.getTable();
                final DefaultTableModel model = (DefaultTableModel) table.getModel();

                if (model.getRowCount() > 0) {
                    table.setRowSelectionInterval(0, 0);
                    propertyTablePanel.getKeyField().setText(".tests");
                    propertyTablePanel.getValueField().setText("999");
                    propertyTablePanel.performLiveValidation();
                }
            });
        });
    }

    @Test
    void testIsModifiedTracking() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final ConfigTestsEditorPanel panel = new ConfigTestsEditorPanel();
            assertFalse(panel.isModified(), "Initially should not be modified");

            final PropertyTablePanel propertyTablePanel = findPropertyTablePanel(panel);
            assertNotNull(propertyTablePanel);
            final JTable table = propertyTablePanel.getTable();
            final DefaultTableModel model = (DefaultTableModel) table.getModel();

            if (model.getRowCount() > 0) {
                model.setValueAt("modified_value", 0, 2);
                assertTrue(panel.isModified(), "Should be modified after table value change");
            }
        });
    }

    private static PropertyTablePanel findPropertyTablePanel(final JComponent comp) {
        for (final java.awt.Component child : comp.getComponents()) {
            if (child instanceof PropertyTablePanel p) {
                return p;
            } else if (child instanceof JComponent jc) {
                final PropertyTablePanel res = findPropertyTablePanel(jc);
                if (res != null) return res;
            }
        }
        return null;
    }
}
