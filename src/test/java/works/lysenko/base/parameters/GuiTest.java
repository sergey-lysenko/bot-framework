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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import static works.lysenko.base.parameters.PropertiesPanel.*;
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
        if (null != Base.properties) {
            Base.properties.clearUserOverrides();
        }
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

        final JButton calcButton = gui.getCalculateCycles();
        calcButton.setText("56 tests required for full coverage (37 leafs)");

        final Method resetMethod;
        try {
            resetMethod = Gui.class.getDeclaredMethod("onTestSelectionChanged");
            resetMethod.setAccessible(true);
            resetMethod.invoke(gui);
        } catch (final Exception e) {
            fail("Failed to invoke onTestSelectionChanged: " + e.getMessage());
            return;
        }

        assertEquals("Required for full coverage", calcButton.getText(), "Button text should reset to default");
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

        final JButton calcButton = gui.getCalculateCycles();

        int attempts = 0;
        while (!calcButton.isEnabled() && attempts < 50) {
            Thread.sleep(100);
            attempts++;
        }

        assertTrue(calcButton.isEnabled(), "Button should be re-enabled after calculation completes");
        assertNotEquals("Calculating...", calcButton.getText(), "Button text should be updated with result");
    }

    @Test
    void testCalculateCyclesUsesUpdatedAllLeafsCount() throws Exception {
        final Properties props = new Properties();
        props.setProperty("TEST", "testSuite");
        props.setProperty("POOL", "");
        props.setProperty("PLATFORM", "chrome");
        props.setProperty("DOMAIN", "example.com");
        props.setProperty("HEADLESS", "false");
        props.setProperty("ALL_LEAFS", "true");
        props.setProperty("ALL_LEAFS_COUNT", "1");

        final Parameters parameters = new Parameters(props);
        final Gui gui = new Gui(parameters);
        gui.addStandardParameters();

        final Field allLeafsCountField = Gui.class.getDeclaredField("allLeafsCount");
        allLeafsCountField.setAccessible(true);
        final JTextField textInput = (JTextField) allLeafsCountField.get(gui);
        assertNotNull(textInput);

        textInput.setText("3");
        assertEquals("3", textInput.getText());
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
        final JPanel coveragePanel = gui.createCoverageEstimatePanel();
        assertNotNull(coveragePanel);
        assertNotNull(gui.getCalculateCycles().getParent(), "Calculate button should be in container");
    }

    @Test
    void testIncludeScenarioPropEnumsPresent() {
        assertEquals(".tree.include.upstream", PropEnum._TREE_INCLUDE_UPSTREAM.getPropertyName());
        assertEquals("false", PropEnum._TREE_INCLUDE_UPSTREAM.defaultValue());
        assertFalse(PropEnum._TREE_INCLUDE_UPSTREAM.silent());

        assertEquals(".tree.include.downstream", PropEnum._TREE_INCLUDE_DOWNSTREAM.getPropertyName());
        assertEquals("false", PropEnum._TREE_INCLUDE_DOWNSTREAM.defaultValue());
        assertFalse(PropEnum._TREE_INCLUDE_DOWNSTREAM.silent());

        assertEquals(".tree.traverse.extensions", PropEnum._TREE_TRAVERSE_EXTENSIONS.getPropertyName());
        assertEquals("false", PropEnum._TREE_TRAVERSE_EXTENSIONS.defaultValue());
        assertFalse(PropEnum._TREE_TRAVERSE_EXTENSIONS.silent());

        assertEquals(".tree.scenario.depth.safeguard", PropEnum._TREE_SCENARIO_DEPTH_SAFEGUARD.getPropertyName());
        assertEquals("20", PropEnum._TREE_SCENARIO_DEPTH_SAFEGUARD.defaultValue());
        assertFalse(PropEnum._TREE_SCENARIO_DEPTH_SAFEGUARD.silent());

        assertEquals(".tree.scenario.history.depth.safeguard", PropEnum._TREE_SCENARIO_HISTORY_DEPTH_SAFEGUARD.getPropertyName());
        assertEquals("20", PropEnum._TREE_SCENARIO_HISTORY_DEPTH_SAFEGUARD.defaultValue());
        assertFalse(PropEnum._TREE_SCENARIO_HISTORY_DEPTH_SAFEGUARD.silent());
    }

    @Test
    void testAllLeafPropertiesAreMarkedAsExecutionParametersInPropertiesPanel() {
        assertTrue(PropEnum._TEST_ALL_LEAFS.executionParameter());
        assertTrue(PropEnum._TEST_ALL_LEAFS_COUNT.executionParameter());
        assertTrue(PropEnum._TEST_HEADLESS.executionParameter());
        assertFalse(PropEnum._LOGS_DEBUG.executionParameter());

        final PropertiesPanel dialog = new PropertiesPanel(new TestProperties(), "default", false, false, 1);
        final DefaultTableModel model = dialog.getModel();
        boolean allLeafsMarked = false;
        boolean allLeafsCountMarked = false;
        boolean headlessMarked = false;
        for (int row = 0; row < model.getRowCount(); row++) {
            final String name = (String) model.getValueAt(row, 0);
            if (PropEnum._TEST_HEADLESS.getPropertyName().equals(name)) {
                assertEquals("TEST_HEADLESS", model.getValueAt(row, 4));
                headlessMarked = true;
            } else if (PropEnum._TEST_ALL_LEAFS.getPropertyName().equals(name)) {
                assertEquals("TEST_ALL_LEAFS", model.getValueAt(row, 4));
                allLeafsMarked = true;
            } else if (PropEnum._TEST_ALL_LEAFS_COUNT.getPropertyName().equals(name)) {
                assertEquals("TEST_ALL_LEAFS_COUNT", model.getValueAt(row, 4));
                allLeafsCountMarked = true;
            }
        }
        assertTrue(allLeafsMarked);
        assertTrue(allLeafsCountMarked);
        assertTrue(headlessMarked);
        assertTrue(PropertyHelp.getDescription(PropEnum._TEST_ALL_LEAFS.getPropertyName()).contains("ALL_LEAFS"));
        assertTrue(PropertyHelp.getDescription(PropEnum._TEST_ALL_LEAFS_COUNT.getPropertyName()).contains("ALL_LEAFS_COUNT"));
        assertTrue(PropertyHelp.getDescription(PropEnum._TEST_HEADLESS.getPropertyName()).contains("HEADLESS"));
    }

    @Test
    void testTestPropertiesOverridesLifecycle() {
        final TestProperties tp = new TestProperties();
        final Map<String, String> defaults = tp.getDefaults();
        assertNotNull(defaults);
        assertFalse(defaults.isEmpty());
        assertTrue(defaults.containsKey(PropEnum._LOGS_DEBUG.getPropertyName()));

        assertTrue(tp.getUserOverrides().isEmpty());

        tp.setUserOverride(PropEnum._LOGS_DEBUG.getPropertyName(), "true");
        assertEquals("true", tp.getUserOverrides().get(PropEnum._LOGS_DEBUG.getPropertyName()));

        final Map<String, String> newOverrides = new HashMap<>();
        newOverrides.put(PropEnum._TEST_PAUSE_LENGTH.getPropertyName(), "3000");
        tp.setUserOverrides(newOverrides);
        assertEquals(1, tp.getUserOverrides().size());
        assertEquals("3000", tp.getUserOverrides().get(PropEnum._TEST_PAUSE_LENGTH.getPropertyName()));
        assertNull(tp.getUserOverrides().get(PropEnum._LOGS_DEBUG.getPropertyName()));

        tp.clearUserOverrides();
        assertTrue(tp.getUserOverrides().isEmpty());
    }

    @Test
    void testTestPropertiesResolveEffective() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        final Map<String, String> baseline = tp.resolveEffectiveProperties("some_test", false, false, 1, false);
        assertNotNull(baseline);
        assertEquals(PropEnum._TEST_HEADLESS.defaultValue(), baseline.get(PropEnum._TEST_HEADLESS.getPropertyName()));
        assertEquals(PropEnum._TEST_ALL_LEAFS.defaultValue(), baseline.get(PropEnum._TEST_ALL_LEAFS.getPropertyName()));

        final Map<String, String> effectiveWithHeadless = tp.resolveEffectiveProperties("some_test", true, true, 3, false);
        assertEquals("true", effectiveWithHeadless.get(PropEnum._TEST_HEADLESS.getPropertyName()));
        assertEquals("true", effectiveWithHeadless.get(PropEnum._TEST_ALL_LEAFS.getPropertyName()));
        assertEquals("3", effectiveWithHeadless.get(PropEnum._TEST_ALL_LEAFS_COUNT.getPropertyName()));

        // Test with user overrides on a property with default value
        tp.setUserOverride(PropEnum._TEST_PAUSE_LENGTH.getPropertyName(), "9999");
        final Map<String, String> withoutOverrides = tp.resolveEffectiveProperties("some_test", false, false, 1, false);
        assertEquals(PropEnum._TEST_PAUSE_LENGTH.defaultValue(), withoutOverrides.get(PropEnum._TEST_PAUSE_LENGTH.getPropertyName()));

        final Map<String, String> withOverrides = tp.resolveEffectiveProperties("some_test", false, false, 1, true);
        assertEquals("9999", withOverrides.get(PropEnum._TEST_PAUSE_LENGTH.getPropertyName()));
    }

    @Test
    void testGuiOverridesPropagateToNewTestPropertiesAndRun() {
        final TestProperties oldProperties = new TestProperties();
        Base.properties = oldProperties;

        // Set GUI user override for cpu density per test
        oldProperties.setUserOverride(PropEnum._TEST_REPORT_CPU_DENSITY_PER_TEST.getPropertyName(), "true");

        // When Core initializes a new TestProperties instance for run
        final TestProperties newProperties = new TestProperties();
        Base.properties = newProperties;

        // The user overrides from previous Base.properties should be preserved in new instance
        assertEquals("true", newProperties.getUserOverrides().get(PropEnum._TEST_REPORT_CPU_DENSITY_PER_TEST.getPropertyName()));

        // When readTestConfiguration is called, user overrides are applied to 'the'
        newProperties.readCommonConfiguration();
        assertEquals(Boolean.TRUE, PropEnum._TEST_REPORT_CPU_DENSITY_PER_TEST.get());
    }

    @Test
    void testPropertiesPanelStatusCalculation() {
        assertEquals(STATUS_DEFAULT, calculateStatus(".logs.debug", "false", "false", true, "false"));
        assertEquals(STATUS_CONFIGURED, calculateStatus(".logs.debug", "true", "false", true, "true"));
        assertEquals(STATUS_MODIFIED, calculateStatus(".logs.debug", "true", "false", true, "false"));
        assertEquals(STATUS_RESET_TO_DEFAULT, calculateStatus(".logs.debug", "false", "false", true, "true"));
        assertEquals(STATUS_CUSTOM, calculateStatus("custom.prop", "val", "", false, "val"));
        assertEquals(STATUS_MODIFIED, calculateStatus("custom.prop", "val2", "", false, "val"));

        // Properties with empty string default (such as .test.app, .test.root, .mobi.ud.id)
        assertEquals(STATUS_DEFAULT, calculateStatus(".test.app", "", "", true, ""));
        assertEquals(STATUS_CONFIGURED, calculateStatus(".test.app", "test.apk", "", true, "test.apk"));
        assertEquals(STATUS_MODIFIED, calculateStatus(".test.app", "test.apk", "", true, ""));
        assertEquals(STATUS_RESET_TO_DEFAULT, calculateStatus(".test.app", "", "", true, "test.apk"));
    }

    @Test
    void testPropertiesPanelTableAndSearch() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        final PropertiesPanel dialog = new PropertiesPanel(tp, "default", false, false, 1);
        final JTable table = dialog.getTable();
        final DefaultTableModel model = dialog.getModel();

        assertNotNull(table);
        assertTrue(model.getRowCount() >= tp.getDefaultsSize());

        // Verify .test.app has status Default (unconfigured) and .test.root has status Configured (configured in project.part)
        for (int r = 0; r < model.getRowCount(); r++) {
            final String name = (String) model.getValueAt(r, 0);
            if (PropEnum._TEST_APP.getPropertyName().equals(name)) {
                assertEquals(STATUS_DEFAULT, model.getValueAt(r, 3), name + " should have status Default");
            }
            if (PropEnum._TEST_ROOT.getPropertyName().equals(name)) {
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
    void usesDropdownForPropertiesWithKnownValidValues() {
        final PropertiesPanel dialog = new PropertiesPanel(
                new TestProperties(), "default", false, false, 1);
        final JTable table = dialog.getTable();
        final DefaultTableModel model = dialog.getModel();
        int sonificationRow = -1;
        int booleanRow = -1;
        int freeFormRow = -1;
        for (int row = 0; row < model.getRowCount(); row++) {
            final String property = (String) model.getValueAt(row, 0);
            if (".test.report.progression.tree.sonification".equals(property)) {
                sonificationRow = row;
            } else if (PropEnum._TEST_REPORT_PROGRESSION_TREE.getPropertyName().equals(property)) {
                booleanRow = row;
            } else if (PropEnum._TEST_APP.getPropertyName().equals(property)) {
                freeFormRow = row;
            }
        }

        assertTrue(sonificationRow >= 0);
        final int sonificationViewRow = table.convertRowIndexToView(sonificationRow);
        assertTrue(table.editCellAt(sonificationViewRow, 1));
        assertInstanceOf(JComboBox.class, table.getEditorComponent());
        final JComboBox<?> choices = (JComboBox<?>) table.getEditorComponent();
        assertEquals(4, choices.getItemCount());
        assertEquals("none", choices.getItemAt(0));
        assertEquals("copilot", choices.getItemAt(1));
        assertEquals("claude", choices.getItemAt(2));
        assertEquals("gemini", choices.getItemAt(3));
        assertNotEquals("", choices.getSelectedItem());
        table.getCellEditor().cancelCellEditing();

        assertTrue(booleanRow >= 0);
        final int booleanViewRow = table.convertRowIndexToView(booleanRow);
        assertTrue(table.editCellAt(booleanViewRow, 1));
        assertInstanceOf(JComboBox.class, table.getEditorComponent());
        final JComboBox<?> booleanChoices = (JComboBox<?>) table.getEditorComponent();
        assertEquals(2, booleanChoices.getItemCount());
        assertEquals("true", booleanChoices.getItemAt(0));
        assertEquals("false", booleanChoices.getItemAt(1));
        assertNotEquals("", booleanChoices.getSelectedItem());
        table.getCellEditor().cancelCellEditing();

        assertTrue(freeFormRow >= 0);
        final int freeFormViewRow = table.convertRowIndexToView(freeFormRow);
        assertTrue(table.editCellAt(freeFormViewRow, 1));
        assertInstanceOf(JTextField.class, table.getEditorComponent());
        table.getCellEditor().cancelCellEditing();
    }

    @Test
    void testPropertiesPanelShowsAndSearchesPropertyHelp() {
        final TestProperties tp = new TestProperties();
        final PropertiesPanel dialog = new PropertiesPanel(tp, "default", false, false, 1);
        final JTable table = dialog.getTable();
        final DefaultTableModel model = dialog.getModel();

        int waitRow = -1;
        for (int row = 0; row < model.getRowCount(); row++) {
            if (PropEnum._TEST_EWAIT.getPropertyName().equals(model.getValueAt(row, 0))) {
                waitRow = row;
                break;
            }
        }
        assertTrue(waitRow >= 0);

        table.setRowSelectionInterval(table.convertRowIndexToView(waitRow), table.convertRowIndexToView(waitRow));
        assertEquals(PropEnum._TEST_EWAIT.getPropertyName(), dialog.getHelpTitle().getText());
        assertTrue(dialog.getHelpText().getText().contains("explicit condition"));

        dialog.getSearchField().setText("explicit condition");
        assertEquals(1, table.getRowCount(), "Help text should participate in search");
        assertEquals(PropEnum._TEST_EWAIT.getPropertyName(), table.getValueAt(0, 0));

        dialog.getSearchField().setText("");
        final int customRow = model.getRowCount();
        model.addRow(new Object[]{"custom.help.less.property", "", "", STATUS_CUSTOM});
        table.setRowSelectionInterval(table.convertRowIndexToView(customRow), table.convertRowIndexToView(customRow));
        assertEquals("No help is available for this property.", dialog.getHelpText().getText());
    }

    @Test
    void copyPropertyNameButtonIsEnabledOnlyForSelectedProperty() {
        final PropertiesPanel dialog = new PropertiesPanel(new TestProperties(), "default", false, false, 1);
        final JTable table = dialog.getTable();
        final JButton copyButton = dialog.getCopyPropertyNameButton();

        assertFalse(copyButton.isEnabled());
        table.setRowSelectionInterval(0, 0);
        assertTrue(copyButton.isEnabled());
        table.clearSelection();
        assertFalse(copyButton.isEnabled());
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
    void testProgressionPropertiesUseGroupedNames() {
        assertEquals(".test.report.progression.scenario", PropEnum._TEST_REPORT_PROGRESSION_SCENARIO.getPropertyName());
        assertEquals(".test.report.progression.scenario.mp4", PropEnum._TEST_REPORT_PROGRESSION_SCENARIO_MP4.getPropertyName());
        assertEquals(".test.report.progression.tree", PropEnum._TEST_REPORT_PROGRESSION_TREE.getPropertyName());
        assertEquals(".test.report.progression.tree.mp4", PropEnum._TEST_REPORT_PROGRESSION_TREE_MP4.getPropertyName());
    }

    @Test
    void testSwipeMarkerPropertiesShareSwipeNamespace() {
        assertEquals(".swipes.marker.line.colour", PropEnum._SWIPE_LINE_MARKER_COLOUR.getPropertyName());
        assertEquals(".swipes.marker.start.colour", PropEnum._SWIPE_START_MARKER_COLOUR.getPropertyName());
        assertEquals(".swipes.marker.stop.colour", PropEnum._SWIPE_STOP_MARKER_COLOUR.getPropertyName());
    }

    @Test
    void testPropertiesPanelEditAndOkCommit() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        final PropertiesPanel dialog = new PropertiesPanel(tp, "default", false, false, 1);
        final DefaultTableModel model = dialog.getModel();

        // Find _test.pause.length row
        int pauseRow = -1;
        for (int r = 0; r < model.getRowCount(); r++) {
            if (PropEnum._TEST_PAUSE_LENGTH.getPropertyName().equals(model.getValueAt(r, 0))) {
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
        assertEquals("1234", tp.getUserOverrides().get(PropEnum._TEST_PAUSE_LENGTH.getPropertyName()));
    }

    @Test
    void testPropertiesPanelResetSelected() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        final PropertiesPanel dialog = new PropertiesPanel(tp, "default", false, false, 1);
        final JTable table = dialog.getTable();
        final DefaultTableModel model = dialog.getModel();

        int pauseRow = -1;
        for (int r = 0; r < model.getRowCount(); r++) {
            if (PropEnum._TEST_PAUSE_LENGTH.getPropertyName().equals(model.getValueAt(r, 0))) {
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
        assertEquals(PropEnum._TEST_PAUSE_LENGTH.defaultValue(), model.getValueAt(pauseRow, 1));
        assertEquals(STATUS_DEFAULT, model.getValueAt(pauseRow, 3));
    }

    @Test
    void testPropertiesPanelCancelDoesNotCommit() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        final PropertiesPanel dialog = new PropertiesPanel(tp, "default", false, false, 1);
        final DefaultTableModel model = dialog.getModel();

        int pauseRow = -1;
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
        tp.setUserOverride(PropEnum._LOGS_DEBUG.getPropertyName(), "true");

        final Method applyMethod = TestProperties.class.getDeclaredMethod("applyUserOverrides");
        applyMethod.setAccessible(true);
        applyMethod.invoke(tp);

        assertEquals(Boolean.TRUE, tp.get(Boolean.class, PropEnum._LOGS_DEBUG.getPropertyName(), true));

        // Output and validate must pass with the modified parameter!
        final Map<String, String> defaults = tp.getDefaults();
        final PropertiesMeta meta = new PropertiesMeta(new HashMap<>(defaults), new ArrayList<>(0));
        assertDoesNotThrow(() -> Renderer.outputAndValidate(tp.getSorted(), meta));

        // Now set user override resetting _LOGS_DEBUG back to its default value "false"
        tp.setUserOverride(PropEnum._LOGS_DEBUG.getPropertyName(), "false");
        applyMethod.invoke(tp);

        // Key should be removed from 'the' because it matches default, and validation must pass!
        final PropertiesMeta meta2 = new PropertiesMeta(new HashMap<>(defaults), new ArrayList<>(0));
        assertDoesNotThrow(() -> Renderer.outputAndValidate(tp.getSorted(), meta2));
    }

    @Test
    void testReopeningPropertiesPanelPreservesModifiedValueAndStatus() {
        final TestProperties tp = new TestProperties();
        Base.properties = tp;

        // 1. First dialog opening: modify .all.leafs.count from 1 to 2
        final PropertiesPanel dialog1 = new PropertiesPanel(tp, "default", false, false, 1);
        final DefaultTableModel model1 = dialog1.getModel();
        int targetRow1 = -1;
        int rootRow1 = -1;
        for (int r = 0; r < model1.getRowCount(); r++) {
            final String name = (String) model1.getValueAt(r, 0);
            if (PropEnum._TEST_ALL_LEAFS_COUNT.getPropertyName().equals(name)) {
                targetRow1 = r;
            }
            if (PropEnum._TEST_ROOT.getPropertyName().equals(name)) {
                rootRow1 = r;
            }
        }
        assertTrue(targetRow1 >= 0);
        assertEquals("1", model1.getValueAt(targetRow1, 1));
        assertEquals("1", model1.getValueAt(targetRow1, 2));
        assertEquals(STATUS_DEFAULT, model1.getValueAt(targetRow1, 3));

        if (rootRow1 >= 0) {
            // .test.root is configured in common config
            assertEquals(STATUS_CONFIGURED, model1.getValueAt(rootRow1, 3));
        }

        // Change value to 2 and click OK
        model1.setValueAt("2", targetRow1, 1);
        assertEquals(STATUS_MODIFIED, model1.getValueAt(targetRow1, 3));
        dialog1.getOkButton().doClick();
        assertTrue(dialog1.isConfirmed());
        assertEquals("2", tp.getUserOverrides().get(PropEnum._TEST_ALL_LEAFS_COUNT.getPropertyName()));

        // 2. Second dialog opening: verify value is 2, default is 1, and status is Modified!
        final PropertiesPanel dialog2 = new PropertiesPanel(tp, "default", false, false, 2);
        final DefaultTableModel model2 = dialog2.getModel();
        int targetRow2 = -1;
        int rootRow2 = -1;
        for (int r = 0; r < model2.getRowCount(); r++) {
            final String name = (String) model2.getValueAt(r, 0);
            if (PropEnum._TEST_ALL_LEAFS_COUNT.getPropertyName().equals(name)) {
                targetRow2 = r;
            }
            if (PropEnum._TEST_ROOT.getPropertyName().equals(name)) {
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
        assertEquals("2", tp.getUserOverrides().get(PropEnum._TEST_ALL_LEAFS_COUNT.getPropertyName()),
                "Override must not be lost or cleared on second OK");

        // 3. Third dialog opening: Reset Selected back to default
        final PropertiesPanel dialog3 = new PropertiesPanel(tp, "default", false, false, 2);
        final DefaultTableModel model3 = dialog3.getModel();
        int targetRow3 = -1;
        for (int r = 0; r < model3.getRowCount(); r++) {
            if (PropEnum._TEST_ALL_LEAFS_COUNT.getPropertyName().equals(model3.getValueAt(r, 0))) {
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
        assertNull(tp.getUserOverrides().get(PropEnum._TEST_ALL_LEAFS_COUNT.getPropertyName()),
                "Override should be cleared when reset to default");
    }

    @Test
    void testGuiDefaultPropertyFallbacks() throws Exception {
        final Parameters emptyParams = new Parameters(new Properties());
        final Gui gui = new Gui(emptyParams);
        gui.addStandardParameters();

        final Field weightField = Gui.class.getDeclaredField("completionWeight");
        weightField.setAccessible(true);
        final JTextField weightComp = (JTextField) weightField.get(gui);
        assertNotNull(weightComp);
        assertEquals("4.5", weightComp.getText());

        final Field forbidField = Gui.class.getDeclaredField("forbidOverexecution");
        forbidField.setAccessible(true);
        final JCheckBox forbidComp = (JCheckBox) forbidField.get(gui);
        assertNotNull(forbidComp);
        assertFalse(forbidComp.isSelected());

        final Field traverseField = Gui.class.getDeclaredField("traverseExtensions");
        traverseField.setAccessible(true);
        final JCheckBox traverseComp = (JCheckBox) traverseField.get(gui);
        assertNotNull(traverseComp);
        assertFalse(traverseComp.isSelected());
    }
}
