package works.lysenko.base.parameters;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.Base;
import works.lysenko.base.Parameters;
import works.lysenko.base.TestProperties;
import works.lysenko.base.properties.Renderer;
import works.lysenko.util.data.records.PropertiesMeta;
import works.lysenko.util.data.records.TestPropertiesDescriptor;
import works.lysenko.util.spec.PropEnum;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.base.parameters.PropertiesDialog.*;
import static works.lysenko.util.spec.Layout.Files.PLATFORMS_;

class GuiTest {

    private Parameters previousParameters;
    private TestProperties previousProperties;
    private boolean createdPlatformsFile = false;

    @BeforeEach
    void setUp() throws IOException {
        previousParameters = Base.parameters;
        previousProperties = Base.properties;
        final File platformsFile = new File(PLATFORMS_);
        if (!platformsFile.exists()) {
            platformsFile.getParentFile().mkdirs();
            Files.writeString(Paths.get(PLATFORMS_), "chrome");
            createdPlatformsFile = true;
        }
    }

    @AfterEach
    void tearDown() {
        Base.parameters = previousParameters;
        Base.properties = previousProperties;
        if (createdPlatformsFile) {
            new File(PLATFORMS_).delete();
            new File("var").delete();
        }
    }

    @Test
    void testGuiComponentsInitialization() {
        final Properties props = new Properties();
        props.setProperty("TEST", "testSuite");
        props.setProperty("POOL", "");
        props.setProperty("PLATFORM", "chrome");
        props.setProperty("DOMAIN", "example.com");
        props.setProperty("HEADLESS", "false");
        props.setProperty("ALL_LEAFS", "false");

        final Parameters parameters = new Parameters(props);
        final Gui gui = new Gui(parameters);

        gui.addStandardParameters();

        final JButton calcButton = gui.getCalculateCycles();
        assertNotNull(calcButton, "Required for full coverage button should be initialized");
        assertEquals("Required for full coverage", calcButton.getText());

        final JLabel cyclesLabel = gui.getCycles();
        assertNotNull(cyclesLabel, "Cycles label should be initialized");
        assertEquals("-", cyclesLabel.getText());

        final JProgressBar progressBar = gui.getProgressBar();
        assertNotNull(progressBar, "Progress bar should be initialized");
        assertEquals(0, progressBar.getMinimum());
        assertEquals(100, progressBar.getMaximum());
        assertFalse(progressBar.isVisible());

        final JButton propButton = gui.getPropertiesButton();
        assertNotNull(propButton, "Modify button should be initialized");
        assertEquals("Modify...", propButton.getText());
    }

    @Test
    void testResetCycleEstimationOnSelectionChange() {
        final Properties props = new Properties();
        props.setProperty("TEST", "testSuite");
        props.setProperty("POOL", "");
        props.setProperty("PLATFORM", "chrome");
        props.setProperty("DOMAIN", "example.com");
        props.setProperty("HEADLESS", "false");
        props.setProperty("ALL_LEAFS", "false");

        final Parameters parameters = new Parameters(props);
        final Gui gui = new Gui(parameters);

        gui.addStandardParameters();

        final JLabel cyclesLabel = gui.getCycles();
        cyclesLabel.setText("42");
        final JProgressBar progressBar = gui.getProgressBar();
        progressBar.setVisible(true);
        progressBar.setValue(50);
        progressBar.setString("50%");

        final Method resetMethod;
        try {
            resetMethod = Gui.class.getDeclaredMethod("onTestSelectionChanged");
            resetMethod.setAccessible(true);
            resetMethod.invoke(gui);
        } catch (final Exception e) {
            fail("Failed to invoke onTestSelectionChanged: " + e.getMessage());
            return;
        }

        assertEquals("-", cyclesLabel.getText(), "Cycles label should be reset to '-'");
        assertEquals(0, progressBar.getValue(), "Progress bar value should be reset to 0");
        assertEquals("", progressBar.getString(), "Progress bar string should be reset to empty");
        assertFalse(progressBar.isVisible(), "Progress bar should be hidden");
    }

    @Test
    void testCalculateCyclesWorkerExecution() throws Exception {
        final Properties props = new Properties();
        props.setProperty("TEST", "testSuite");
        props.setProperty("POOL", "");
        props.setProperty("PLATFORM", "chrome");
        props.setProperty("DOMAIN", "example.com");
        props.setProperty("HEADLESS", "false");
        props.setProperty("ALL_LEAFS", "false");

        final Parameters parameters = new Parameters(props);
        final Gui gui = new Gui(parameters);

        gui.addStandardParameters();

        final JPanel box = gui.dialogueBox();
        assertNotNull(box);

        final Method onCalcMethod = Gui.class.getDeclaredMethod("onCalculateCycles");
        onCalcMethod.setAccessible(true);
        onCalcMethod.invoke(gui);

        final JProgressBar progressBar = gui.getProgressBar();
        assertTrue(progressBar.isVisible(), "Progress bar should become visible when calculation starts");

        int attempts = 0;
        while (progressBar.isVisible() && attempts < 50) {
            Thread.sleep(100);
            attempts++;
        }

        assertFalse(progressBar.isVisible(), "Progress bar should be hidden after calculation completes");
        assertNotEquals("-", gui.getCycles().getText(), "Cycles label should display estimated cycle count");
    }

    @Test
    void testWindowLayoutAfterCalculation() {
        final Properties props = new Properties();
        props.setProperty("TEST", "testSuite");
        props.setProperty("POOL", "");
        props.setProperty("PLATFORM", "chrome");
        props.setProperty("DOMAIN", "example.com");
        props.setProperty("HEADLESS", "false");
        props.setProperty("ALL_LEAFS", "false");

        final Parameters parameters = new Parameters(props);
        final Gui gui = new Gui(parameters);

        gui.addStandardParameters();
        final JPanel panel = gui.dialogueBox();

        assertNotNull(panel);
        assertTrue(panel.getLayout() instanceof GridBagLayout, "Panel layout should be GridBagLayout");
        assertNotNull(gui.getCycles().getParent(), "Cycles label should be added to container");
        assertNotNull(gui.getProgressBar().getParent(), "Progress bar should be added to container");
        assertNotNull(gui.getCalculateCycles().getParent(), "Calculate cycles button should be added to container");
        assertEquals(panel, gui.getPropertiesButton().getParent(), "Properties button should be added to panel");
    }

    @Test
    void testIncludeScenarioPropEnumsPresent() {
        assertEquals(".include.upstream", PropEnum._INCLUDE_UPSTREAM.getPropertyName());
        assertEquals("false", PropEnum._INCLUDE_UPSTREAM.defaultValue());
        assertFalse(PropEnum._INCLUDE_UPSTREAM.silent());

        assertEquals(".include.downstream", PropEnum._INCLUDE_DOWNSTREAM.getPropertyName());
        assertEquals("false", PropEnum._INCLUDE_DOWNSTREAM.defaultValue());
        assertFalse(PropEnum._INCLUDE_DOWNSTREAM.silent());

        assertEquals(".scenario.depth.safeguard", PropEnum._SCENARIO_DEPTH_SAFEGUARD.getPropertyName());
        assertEquals("20", PropEnum._SCENARIO_DEPTH_SAFEGUARD.defaultValue());
        assertFalse(PropEnum._SCENARIO_DEPTH_SAFEGUARD.silent());

        assertEquals(".scenario.history.depth.safeguard", PropEnum._SCENARIO_HISTORY_DEPTH_SAFEGUARD.getPropertyName());
        assertEquals("20", PropEnum._SCENARIO_HISTORY_DEPTH_SAFEGUARD.defaultValue());
        assertFalse(PropEnum._SCENARIO_HISTORY_DEPTH_SAFEGUARD.silent());
    }

    @Test
    void testTestPropertiesOverridesLifecycle() {
        final TestProperties tp = new TestProperties();
        final Map<String, String> defaults = tp.getDefaults();
        assertNotNull(defaults);
        assertFalse(defaults.isEmpty());
        assertTrue(defaults.containsKey(PropEnum._DEBUG.getPropertyName()));

        assertTrue(tp.getUserOverrides().isEmpty());

        tp.setUserOverride(PropEnum._DEBUG.getPropertyName(), "true");
        assertEquals("true", tp.getUserOverrides().get(PropEnum._DEBUG.getPropertyName()));

        final Map<String, String> newOverrides = new HashMap<>();
        newOverrides.put(PropEnum._PAUSE_LENGTH.getPropertyName(), "3000");
        tp.setUserOverrides(newOverrides);
        assertEquals(1, tp.getUserOverrides().size());
        assertEquals("3000", tp.getUserOverrides().get(PropEnum._PAUSE_LENGTH.getPropertyName()));
        assertNull(tp.getUserOverrides().get(PropEnum._DEBUG.getPropertyName()));

        tp.clearUserOverrides();
        assertTrue(tp.getUserOverrides().isEmpty());
    }

    @Test
    void testTestPropertiesResolveEffective() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        final Map<String, String> baseline = tp.resolveEffectiveProperties("some_test", false, false, 1, false);
        assertNotNull(baseline);
        assertEquals(PropEnum._HEADLESS.defaultValue(), baseline.get(PropEnum._HEADLESS.getPropertyName()));
        assertEquals(PropEnum._ALL_LEAFS.defaultValue(), baseline.get(PropEnum._ALL_LEAFS.getPropertyName()));

        final Map<String, String> effectiveWithHeadless = tp.resolveEffectiveProperties("some_test", true, true, 3, false);
        assertEquals("true", effectiveWithHeadless.get(PropEnum._HEADLESS.getPropertyName()));
        assertEquals("true", effectiveWithHeadless.get(PropEnum._ALL_LEAFS.getPropertyName()));
        assertEquals("3", effectiveWithHeadless.get(PropEnum._ALL_LEAFS_COUNT.getPropertyName()));

        // Test with user overrides on a property with default value
        tp.setUserOverride(PropEnum._PAUSE_LENGTH.getPropertyName(), "9999");
        final Map<String, String> withoutOverrides = tp.resolveEffectiveProperties("some_test", false, false, 1, false);
        assertEquals(PropEnum._PAUSE_LENGTH.defaultValue(), withoutOverrides.get(PropEnum._PAUSE_LENGTH.getPropertyName()));

        final Map<String, String> withOverrides = tp.resolveEffectiveProperties("some_test", false, false, 1, true);
        assertEquals("9999", withOverrides.get(PropEnum._PAUSE_LENGTH.getPropertyName()));
    }

    @Test
    void testPropertiesDialogStatusCalculation() {
        assertEquals(STATUS_DEFAULT, calculateStatus("_debug", "false", "false", true, "false"));
        assertEquals(STATUS_CONFIGURED, calculateStatus("_debug", "true", "false", true, "true"));
        assertEquals(STATUS_MODIFIED, calculateStatus("_debug", "true", "false", true, "false"));
        assertEquals(STATUS_RESET_TO_DEFAULT, calculateStatus("_debug", "false", "false", true, "true"));
        assertEquals(STATUS_CUSTOM, calculateStatus("custom.prop", "val", "", false, "val"));
        assertEquals(STATUS_MODIFIED, calculateStatus("custom.prop", "val2", "", false, "val"));

        // Properties with empty string default (such as .app, .root, .ud.id)
        assertEquals(STATUS_DEFAULT, calculateStatus(".app", "", "", true, ""));
        assertEquals(STATUS_CONFIGURED, calculateStatus(".app", "test.apk", "", true, "test.apk"));
        assertEquals(STATUS_MODIFIED, calculateStatus(".app", "test.apk", "", true, ""));
        assertEquals(STATUS_RESET_TO_DEFAULT, calculateStatus(".app", "", "", true, "test.apk"));
    }

    @Test
    void testPropertiesDialogTableAndSearch() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        final PropertiesDialog dialog = new PropertiesDialog(null, tp, "default", false, false, 1);
        final JTable table = dialog.getTable();
        final DefaultTableModel model = dialog.getModel();

        assertNotNull(table);
        assertTrue(model.getRowCount() >= tp.getDefaultsSize());

        // Verify .app has status Default (unconfigured) and .root has status Configured (configured in project.part)
        for (int r = 0; r < model.getRowCount(); r++) {
            final String name = (String) model.getValueAt(r, 0);
            if (PropEnum._APP.getPropertyName().equals(name)) {
                assertEquals(STATUS_DEFAULT, model.getValueAt(r, 3), name + " should have status Default");
            }
            if (PropEnum._ROOT.getPropertyName().equals(name)) {
                assertEquals(STATUS_CONFIGURED, model.getValueAt(r, 3), name + " should have status Configured");
            }
        }

        // Search test
        final JTextField searchField = dialog.getSearchField();
        searchField.setText("debug");
        assertTrue(table.getRowCount() >= 1, "Should filter to rows containing debug");
        assertTrue(table.getRowCount() < model.getRowCount(), "Filtered view should be smaller than full model");

        searchField.setText("");
        assertEquals(model.getRowCount(), table.getRowCount(), "Clearing search restores all rows");
    }

    @Test
    void testPropertiesDialogShowsAndSearchesPropertyHelp() {
        final TestProperties tp = new TestProperties();
        final PropertiesDialog dialog = new PropertiesDialog(null, tp, "default", false, false, 1);
        final JTable table = dialog.getTable();
        final DefaultTableModel model = dialog.getModel();

        int waitRow = -1;
        for (int row = 0; row < model.getRowCount(); row++) {
            if (PropEnum._EWAIT.getPropertyName().equals(model.getValueAt(row, 0))) {
                waitRow = row;
                break;
            }
        }
        assertTrue(waitRow >= 0);

        table.setRowSelectionInterval(table.convertRowIndexToView(waitRow), table.convertRowIndexToView(waitRow));
        assertEquals(PropEnum._EWAIT.getPropertyName(), dialog.getHelpTitle().getText());
        assertTrue(dialog.getHelpText().getText().contains("explicit condition"));

        dialog.getSearchField().setText("explicit condition");
        assertEquals(1, table.getRowCount(), "Help text should participate in search");
        assertEquals(PropEnum._EWAIT.getPropertyName(), table.getValueAt(0, 0));

        dialog.getSearchField().setText("");
        final int customRow = model.getRowCount();
        model.addRow(new Object[]{"custom.help.less.property", "", "", STATUS_CUSTOM});
        table.setRowSelectionInterval(table.convertRowIndexToView(customRow), table.convertRowIndexToView(customRow));
        assertEquals("No help is available for this property.", dialog.getHelpText().getText());
    }

    @Test
    void testPropertyHelpOnlyReferencesKnownProperties() {
        final TestProperties tp = new TestProperties();
        for (final String propertyName : PropertyHelp.getPropertyNames()) {
            assertTrue(tp.getDefaults().containsKey(propertyName), propertyName + " is not a known property");
        }
        for (final String propertyName : tp.getDefaults().keySet()) {
            assertNotNull(PropertyHelp.getDescription(propertyName), propertyName + " is missing help");
            assertFalse(PropertyHelp.getDescription(propertyName).isBlank(), propertyName + " has blank help");
        }
    }

    @Test
    void testProgressionPropertiesUseGroupedNamesAndReadLegacyAliases(@TempDir final Path tempDir) throws IOException {
        assertEquals(".progression.scenario", PropEnum._SCENARIO_PROGRESSION.getPropertyName());
        assertEquals(".progression.scenario.mp4", PropEnum._SCENARIO_PROGRESSION_MP4.getPropertyName());
        assertEquals(".progression.tree", PropEnum._TREE_PROGRESSION.getPropertyName());
        assertEquals(".progression.tree.mp4", PropEnum._TREE_PROGRESSION_MP4.getPropertyName());

        final Path config = tempDir.resolve("legacy.properties");
        Files.writeString(config, String.join(System.lineSeparator(),
                ".scenario.progression=false",
                ".scenario.progression.mp4=true",
                ".tree.progression=false",
                ".tree.progression.mp4=true",
                ".progression.scenario=true"));

        final works.lysenko.util.func.core.TestProperties.Result result =
                works.lysenko.util.func.core.TestProperties.readTestPropertiesFromFile(
                        new TestPropertiesDescriptor(tempDir + File.separator, "legacy", ".properties"));
        final Properties properties = result.properties();
        assertEquals("true", properties.getProperty(".progression.scenario"),
                "The new property name should take precedence over its legacy alias");
        assertEquals("true", properties.getProperty(".progression.scenario.mp4"));
        assertEquals("false", properties.getProperty(".progression.tree"));
        assertEquals("true", properties.getProperty(".progression.tree.mp4"));
        assertFalse(properties.containsKey(".scenario.progression"));

        final TestProperties overrides = new TestProperties();
        overrides.setUserOverride(".scenario.progression", "false");
        assertEquals("false", overrides.getUserOverrides().get(".progression.scenario"));
    }

    @Test
    void testSwipeMarkerPropertiesShareSwipeNamespaceAndReadLegacyAliases(@TempDir final Path tempDir) throws IOException {
        assertEquals(".swipes.marker.line.colour", PropEnum._SWIPE_LINE_MARKER_COLOUR.getPropertyName());
        assertEquals(".swipes.marker.start.colour", PropEnum._SWIPE_START_MARKER_COLOUR.getPropertyName());
        assertEquals(".swipes.marker.stop.colour", PropEnum._SWIPE_STOP_MARKER_COLOUR.getPropertyName());

        final Path config = tempDir.resolve("legacy-swipe.properties");
        Files.writeString(config, String.join(System.lineSeparator(),
                ".swipe.line.marker.colour=1,2,3,4",
                ".swipe.start.marker.colour=5,6,7,8",
                ".swipe.stop.marker.colour=9,10,11,12",
                ".swipes.marker.line.colour=13,14,15,16"));

        final works.lysenko.util.func.core.TestProperties.Result result =
                works.lysenko.util.func.core.TestProperties.readTestPropertiesFromFile(
                        new TestPropertiesDescriptor(tempDir + File.separator, "legacy-swipe", ".properties"));
        final Properties properties = result.properties();
        assertEquals("13,14,15,16", properties.getProperty(".swipes.marker.line.colour"),
                "The new property name should take precedence over its legacy alias");
        assertEquals("5,6,7,8", properties.getProperty(".swipes.marker.start.colour"));
        assertEquals("9,10,11,12", properties.getProperty(".swipes.marker.stop.colour"));
        assertFalse(properties.containsKey(".swipe.start.marker.colour"));

        final TestProperties overrides = new TestProperties();
        overrides.setUserOverride(".swipe.line.marker.colour", "1,2,3,4");
        assertEquals("1,2,3,4", overrides.getUserOverrides().get(".swipes.marker.line.colour"));
    }

    @Test
    void testPropertiesDialogEditAndOkCommit() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        final PropertiesDialog dialog = new PropertiesDialog(null, tp, "default", false, false, 1);
        final DefaultTableModel model = dialog.getModel();

        // Find _pause.length row
        int pauseRow = -1;
        for (int r = 0; r < model.getRowCount(); r++) {
            if (PropEnum._PAUSE_LENGTH.getPropertyName().equals(model.getValueAt(r, 0))) {
                pauseRow = r;
                break;
            }
        }
        assertTrue(pauseRow >= 0);

        // Edit value in model
        model.setValueAt("1234", pauseRow, 1);
        assertEquals(STATUS_MODIFIED, model.getValueAt(pauseRow, 3));

        // Click OK button
        final JButton okBtn = dialog.getOkButton();
        okBtn.doClick();

        assertTrue(dialog.isConfirmed());
        assertEquals("1234", tp.getUserOverrides().get(PropEnum._PAUSE_LENGTH.getPropertyName()));
    }

    @Test
    void testPropertiesDialogResetSelected() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        final PropertiesDialog dialog = new PropertiesDialog(null, tp, "default", false, false, 1);
        final JTable table = dialog.getTable();
        final DefaultTableModel model = dialog.getModel();

        int pauseRow = -1;
        for (int r = 0; r < model.getRowCount(); r++) {
            if (PropEnum._PAUSE_LENGTH.getPropertyName().equals(model.getValueAt(r, 0))) {
                pauseRow = r;
                break;
            }
        }
        assertTrue(pauseRow >= 0);

        // Modify value
        model.setValueAt("9999", pauseRow, 1);
        assertEquals(STATUS_MODIFIED, model.getValueAt(pauseRow, 3));

        // Select that row in table
        final int viewRow = table.convertRowIndexToView(pauseRow);
        table.setRowSelectionInterval(viewRow, viewRow);

        // Click Reset Selected
        dialog.getResetSelectedButton().doClick();
        assertEquals(PropEnum._PAUSE_LENGTH.defaultValue(), model.getValueAt(pauseRow, 1));
        assertEquals(STATUS_DEFAULT, model.getValueAt(pauseRow, 3));
    }

    @Test
    void testPropertiesDialogCancelDoesNotCommit() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        final PropertiesDialog dialog = new PropertiesDialog(null, tp, "default", false, false, 1);
        final DefaultTableModel model = dialog.getModel();

        int pauseRow = -1;
        for (int r = 0; r < model.getRowCount(); r++) {
            if (PropEnum._PAUSE_LENGTH.getPropertyName().equals(model.getValueAt(r, 0))) {
                pauseRow = r;
                break;
            }
        }
        assertTrue(pauseRow >= 0);

        model.setValueAt("9999", pauseRow, 1);
        dialog.getCancelButton().doClick();

        assertFalse(dialog.isConfirmed());
        assertTrue(tp.getUserOverrides().isEmpty(), "Cancel must not save user overrides");
    }

    @Test
    void testUserOverridesAppliedToTestPropertiesTheAndValidatePasses() throws Exception {
        final TestProperties tp = new TestProperties();
        final Field ccField = TestProperties.class.getDeclaredField("commonConfiguration");
        ccField.setAccessible(true);
        ccField.set(tp, new Properties());

        final Field theField = TestProperties.class.getDeclaredField("the");
        theField.setAccessible(true);
        theField.set(tp, new Properties());

        Base.properties = tp;

        final Properties props = new Properties();
        props.setProperty("TEST", "dummy");
        Base.parameters = new Parameters(props);

        // Set user override for non-default value
        tp.setUserOverride(PropEnum._DEBUG.getPropertyName(), "true");

        final Method applyMethod = TestProperties.class.getDeclaredMethod("applyUserOverrides");
        applyMethod.setAccessible(true);
        applyMethod.invoke(tp);

        assertEquals(Boolean.TRUE, tp.get(Boolean.class, PropEnum._DEBUG.getPropertyName(), true));

        // Output and validate must pass with the modified parameter!
        final Map<String, String> defaults = tp.getDefaults();
        final PropertiesMeta meta = new PropertiesMeta(new HashMap<>(defaults), new ArrayList<>(0));
        assertDoesNotThrow(() -> Renderer.outputAndValidate(tp.getSorted(), meta));

        // Now set user override resetting _DEBUG back to its default value "false"
        tp.setUserOverride(PropEnum._DEBUG.getPropertyName(), "false");
        applyMethod.invoke(tp);

        // Key should be removed from 'the' because it matches default, and validation must pass!
        final PropertiesMeta meta2 = new PropertiesMeta(new HashMap<>(defaults), new ArrayList<>(0));
        assertDoesNotThrow(() -> Renderer.outputAndValidate(tp.getSorted(), meta2));
    }

    @Test
    void testReopeningPropertiesDialogPreservesModifiedValueAndStatus() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        // 1. First dialog opening: modify .all.leafs.count from 1 to 2
        final PropertiesDialog dialog1 = new PropertiesDialog(null, tp, "default", false, false, 1);
        final DefaultTableModel model1 = dialog1.getModel();
        int targetRow1 = -1;
        int rootRow1 = -1;
        for (int r = 0; r < model1.getRowCount(); r++) {
            final String name = (String) model1.getValueAt(r, 0);
            if (PropEnum._ALL_LEAFS_COUNT.getPropertyName().equals(name)) {
                targetRow1 = r;
            }
            if (PropEnum._ROOT.getPropertyName().equals(name)) {
                rootRow1 = r;
            }
        }
        assertTrue(targetRow1 >= 0);
        assertEquals("1", model1.getValueAt(targetRow1, 1));
        assertEquals("1", model1.getValueAt(targetRow1, 2));
        assertEquals(STATUS_DEFAULT, model1.getValueAt(targetRow1, 3));

        if (rootRow1 >= 0) {
            // .root is configured in common config
            assertEquals(STATUS_CONFIGURED, model1.getValueAt(rootRow1, 3));
        }

        // Change value to 2 and click OK
        model1.setValueAt("2", targetRow1, 1);
        assertEquals(STATUS_MODIFIED, model1.getValueAt(targetRow1, 3));
        dialog1.getOkButton().doClick();
        assertTrue(dialog1.isConfirmed());
        assertEquals("2", tp.getUserOverrides().get(PropEnum._ALL_LEAFS_COUNT.getPropertyName()));

        // 2. Second dialog opening: verify value is 2, default is 1, and status is Modified!
        final PropertiesDialog dialog2 = new PropertiesDialog(null, tp, "default", false, false, 2);
        final DefaultTableModel model2 = dialog2.getModel();
        int targetRow2 = -1;
        int rootRow2 = -1;
        for (int r = 0; r < model2.getRowCount(); r++) {
            final String name = (String) model2.getValueAt(r, 0);
            if (PropEnum._ALL_LEAFS_COUNT.getPropertyName().equals(name)) {
                targetRow2 = r;
            }
            if (PropEnum._ROOT.getPropertyName().equals(name)) {
                rootRow2 = r;
            }
        }
        assertTrue(targetRow2 >= 0);
        assertEquals("2", model2.getValueAt(targetRow2, 1), "Value must remain 2 on reopen, not revert to default");
        assertEquals("1", model2.getValueAt(targetRow2, 2), "Default must remain 1 from enum default");
        assertEquals(STATUS_MODIFIED, model2.getValueAt(targetRow2, 3), "Status must remain Modified, not Default");

        if (rootRow2 >= 0) {
            assertEquals(STATUS_CONFIGURED, model2.getValueAt(rootRow2, 3), ".root must remain Configured, not Default");
        }

        // Click OK on second opening without editing
        dialog2.getOkButton().doClick();
        assertTrue(dialog2.isConfirmed());
        assertEquals("2", tp.getUserOverrides().get(PropEnum._ALL_LEAFS_COUNT.getPropertyName()),
                "Override must not be lost or cleared on second OK");

        // 3. Third dialog opening: Reset Selected back to default
        final PropertiesDialog dialog3 = new PropertiesDialog(null, tp, "default", false, false, 2);
        final DefaultTableModel model3 = dialog3.getModel();
        int targetRow3 = -1;
        for (int r = 0; r < model3.getRowCount(); r++) {
            if (PropEnum._ALL_LEAFS_COUNT.getPropertyName().equals(model3.getValueAt(r, 0))) {
                targetRow3 = r;
                break;
            }
        }
        assertTrue(targetRow3 >= 0);
        final int viewRow = dialog3.getTable().convertRowIndexToView(targetRow3);
        dialog3.getTable().setRowSelectionInterval(viewRow, viewRow);
        dialog3.getResetSelectedButton().doClick();

        assertEquals("1", model3.getValueAt(targetRow3, 1), "Value should be reset to default 1");
        assertEquals(STATUS_DEFAULT, model3.getValueAt(targetRow3, 3), "Status should be Default");

        dialog3.getOkButton().doClick();
        assertTrue(dialog3.isConfirmed());
        assertNull(tp.getUserOverrides().get(PropEnum._ALL_LEAFS_COUNT.getPropertyName()),
                "Override should be cleared when reset to default");
    }
}
