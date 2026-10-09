package works.lysenko.base.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KnownIssuesPanelTest {

    @Test
    void testInputFieldsHaveContextMenus() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final KnownIssuesPanel panel = new KnownIssuesPanel();

            try {
                final Field scenarioField = KnownIssuesPanel.class.getDeclaredField("scenarioField");
                scenarioField.setAccessible(true);
                final JTextField tfScenario = (JTextField) scenarioField.get(panel);
                assertNotNull(tfScenario.getComponentPopupMenu(), "scenarioField should have popup menu");

                final Field titleField = KnownIssuesPanel.class.getDeclaredField("titleField");
                titleField.setAccessible(true);
                final JTextField tfTitle = (JTextField) titleField.get(panel);
                assertNotNull(tfTitle.getComponentPopupMenu(), "titleField should have popup menu");

                final Field descriptionField = KnownIssuesPanel.class.getDeclaredField("descriptionField");
                descriptionField.setAccessible(true);
                final JTextField tfDesc = (JTextField) descriptionField.get(panel);
                assertNotNull(tfDesc.getComponentPopupMenu(), "descriptionField should have popup menu");

                final Field linkField = KnownIssuesPanel.class.getDeclaredField("linkField");
                linkField.setAccessible(true);
                final JTextField tfLink = (JTextField) linkField.get(panel);
                assertNotNull(tfLink.getComponentPopupMenu(), "linkField should have popup menu");

                final Field patternField = KnownIssuesPanel.class.getDeclaredField("patternField");
                patternField.setAccessible(true);
                final JTextField tfPattern = (JTextField) patternField.get(panel);
                assertNotNull(tfPattern.getComponentPopupMenu(), "patternField should have popup menu");

                final Field rawJsonArea = KnownIssuesPanel.class.getDeclaredField("rawJsonArea");
                rawJsonArea.setAccessible(true);
                final JTextArea taJson = (JTextArea) rawJsonArea.get(panel);
                assertNotNull(taJson.getComponentPopupMenu(), "rawJsonArea should have popup menu");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
