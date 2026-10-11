package works.lysenko.base.ui;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
