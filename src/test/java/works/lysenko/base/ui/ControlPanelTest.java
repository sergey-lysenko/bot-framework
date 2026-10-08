package works.lysenko.base.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Parameters;
import works.lysenko.base.output.TreeTracker;
import works.lysenko.base.parameters.Gui;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ControlPanelTest {

    private Parameters previousParameters;

    @BeforeEach
    void setUp() {
        previousParameters = Base.parameters;
        TreeTracker.reset();
    }

    @AfterEach
    void tearDown() {
        Base.parameters = previousParameters;
        TreeTracker.reset();
    }

    @Test
    void controlPanelHasThreeTabsWithTreeProgression() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final Properties props = new Properties();
            props.setProperty("TEST", "testSuite");
            props.setProperty("PLATFORM", "chrome");
            final Parameters parameters = new Parameters(props);
            final Gui gui = new Gui(parameters);
            gui.addStandardParameters();
            final ControlPanel controlPanel = new ControlPanel(parameters, gui);

            final JTabbedPane tabbedPane = controlPanel.getTabbedPane();
            assertNotNull(tabbedPane);
            assertEquals(4, tabbedPane.getTabCount());
            assertEquals("Configuration", tabbedPane.getTitleAt(0));
            assertEquals("Execution", tabbedPane.getTitleAt(1));
            assertEquals("Tree Progression", tabbedPane.getTitleAt(2));
            assertEquals("Known Issues", tabbedPane.getTitleAt(3));
        });
    }

    @Test
    void setTreeProgressionUpdatesImageInControlPanel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final Properties props = new Properties();
            props.setProperty("TEST", "testSuite");
            props.setProperty("PLATFORM", "chrome");
            final Parameters parameters = new Parameters(props);
            final Gui gui = new Gui(parameters);
            gui.addStandardParameters();
            final ControlPanel controlPanel = new ControlPanel(parameters, gui);

            final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
            controlPanel.setTreeProgression(image);
            assertEquals(image, controlPanel.getTreeImagePanel().getImage());

            controlPanel.setTreeProgression(null);
        });
    }

    @Test
    void treeTrackerTracksLatestFrameImage() {
        assertNull(TreeTracker.getLatestFrameImage());

        TreeTracker.reset();
        assertNull(TreeTracker.getLatestFrameImage());
    }
}
