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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static works.lysenko.util.spec.Layout.Files.PLATFORMS_;

class ControlPanelTest {

    private Parameters previousParameters;
    private boolean createdPlatformsFile = false;

    @BeforeEach
    void setUp() throws IOException {
        previousParameters = Base.parameters;
        TreeTracker.reset();
        final File platformsFile = new File(PLATFORMS_);
        if (!platformsFile.exists()) {
            platformsFile.getParentFile().mkdirs();
            Files.writeString(Paths.get(PLATFORMS_), "Android,Chrome");
            createdPlatformsFile = true;
        }
    }

    @AfterEach
    void tearDown() {
        Base.parameters = previousParameters;
        TreeTracker.reset();
        if (createdPlatformsFile) {
            new File(PLATFORMS_).delete();
            new File("var").delete();
        }
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
            assertEquals(5, tabbedPane.getTabCount());
            assertEquals("Config & Tests", tabbedPane.getTitleAt(0));
            assertEquals("Run Setup", tabbedPane.getTitleAt(1));
            assertEquals("Execution", tabbedPane.getTitleAt(2));
            assertEquals("Tree Progression", tabbedPane.getTitleAt(3));
            assertEquals("Known Issues", tabbedPane.getTitleAt(4));
            assertEquals(1, tabbedPane.getSelectedIndex());
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

    @Test
    void startRunSwitchesToSelectedTab() throws Exception {
        final java.io.File suiteFile = new java.io.File("src/main/resources/suites/test.suite");
        try {
            suiteFile.getParentFile().mkdirs();
            java.nio.file.Files.writeString(suiteFile.toPath(), "");

            Base.properties = new works.lysenko.base.TestProperties();
            final Properties props = new Properties();
            props.setProperty("TEST", "test");
            props.setProperty("PLATFORM", "chrome");
            final Parameters parameters = new Parameters(props);
            Base.parameters = parameters;
            SwingUtilities.invokeAndWait(() -> {
                final Gui gui = new Gui(parameters);
                gui.addStandardParameters();
                final ControlPanel controlPanel = new ControlPanel(parameters, gui);

                final JTabbedPane tabbedPane = controlPanel.getTabbedPane();
                assertEquals(1, tabbedPane.getSelectedIndex());

                try {
                    final java.lang.reflect.Method startRunMethod = ControlPanel.class.getDeclaredMethod("startRun", int.class);
                    startRunMethod.setAccessible(true);
                    startRunMethod.invoke(controlPanel, 2);
                    assertEquals(2, tabbedPane.getSelectedIndex());
                    org.junit.jupiter.api.Assertions.assertFalse(tabbedPane.isEnabledAt(0));
                    org.junit.jupiter.api.Assertions.assertFalse(tabbedPane.isEnabledAt(1));
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            if (suiteFile.exists()) suiteFile.delete();
        }
    }
}
