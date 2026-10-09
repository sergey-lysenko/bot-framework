package works.lysenko.base.parameters;

import works.lysenko.base.Parameters;
import works.lysenko.base.util.Platforms;
import works.lysenko.util.apis.parameters._GUI;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.data.enums.Platform;
import works.lysenko.util.spec.PropEnum;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.base.Parameters.WIDTH;
import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.FULL;
import static works.lysenko.util.data.enums.ExecutionParameter.*;
import static works.lysenko.util.data.enums.ExitCode.PLATFORMS_RESET;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static java.util.Objects.isNull;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.C.COVERAGE;
import static works.lysenko.util.lang.word.D.DELETE;
import static works.lysenko.util.lang.word.F.FIXED;
import static works.lysenko.util.lang.word.M.MODIFY;
import static works.lysenko.util.lang.word.P.PLATFORMS;
import static works.lysenko.util.lang.word.R.REQUIRED;
import static works.lysenko.util.lang.word.R.RESET;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.prop.core.Start.forcedPlatform;
import static works.lysenko.util.spec.Layout.Directories.ETC_;
import static works.lysenko.util.spec.Layout.Files.PLATFORMS_;
import static works.lysenko.util.spec.Layout.Parts.TEST_PROPERTIES_EXTENSION;
import static works.lysenko.util.spec.Layout.Parts.USERS_POOL_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._TESTS_;

/**
 * Represents a Graphical User Interface (GUI) for parameter input and user interaction.
 */
@SuppressWarnings({"UseOfConcreteClass", "AssignmentOrReturnOfFieldWithMutableType", "CallToSuspiciousStringMethod"})
public class Gui implements _GUI {

    private final Parameters parameters;
    private JPanel panel = null;
    private JTextField domain = null;
    private JCheckBox headless = null;
    private JCheckBox allLeafs = null;
    private JTextField allLeafsCount = null;
    private JTextField testsCount = null;
    private JCheckBox forbidOverexecution = null;
    private JCheckBox resilientMode = null;
    private JTextField completionWeight = null;
    private JCheckBox traverseExtensions = null;
    private JComboBox<Object> platform = null;
    private JComboBox<Object> test = null;
    private JComboBox<Object> pool = null;
    private JButton reset = null;
    private JButton calculateCycles = null;
    private JProgressBar progressBar = null;
    private JLabel cycles = null;
    private JPanel cyclesPanel = null;
    private JButton properties = null;
    private List<JTextField> aParams = null;

    /**
     * Represents a graphical user interface for parameter input and user interaction.
     *
     * @param parameters the Parameters object that holds and manages various parameters for execution
     */
    static {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            works.lysenko.base.ui.ControlPanel.applyMenuShortcutKeyMask();
            javax.swing.ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        } catch (final Exception ignored) {}
    }

    public Gui(final Parameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Adds a labeled row to a GridBagLayout container.
     */
    private static void addRow(final String label, final Component comp, final JPanel container, final int row) {
        GuiLayoutBuilder.addRow(label, comp, container, row);
    }

    /**
     * Adds a labeled row to a GridBagLayout container if the component is non-null.
     */
    static int addRowOptional(final String label, final Component comp, final JPanel container, final int row) {
        return GuiLayoutBuilder.addRowOptional(label, comp, container, row);
    }

    /**
     * Adds a horizontal separator divider spanning across the GridBagLayout columns.
     */
    static int addDivider(final JPanel container, final int row) {
        return GuiLayoutBuilder.addDivider(container, row);
    }

    /**
     * Returns a sorted list of platform names.
     *
     * @return a list of platform names
     */
    private static List<String> platformNames() {

        final List<String> platformNames = new LinkedList<>();
        for (final Platform platform : Platforms.available())
            platformNames.add(platform.getString());
        platformNames.sort(null);
        return platformNames;
    }

    /**
     * Resets the platforms file by deleting it.
     *
     * @throws RuntimeException if unable to delete the platforms file
     */
    private static void reset() {

        if (!(new File(PLATFORMS_).delete()))
            throw new IllegalStateException(b(c(UNABLE), TO, DELETE, PLATFORMS, FILE, q(PLATFORMS_)));
    }

    /**
     * Displays the graphical user interface for parameter input and user interaction.
     */
    public final void display() {

        final works.lysenko.base.ui.ControlPanel[] cp = new works.lysenko.base.ui.ControlPanel[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                addStandardParameters();
                addAdditionalParameters();
                cp[0] = new works.lysenko.base.ui.ControlPanel(parameters, this);
            });
        } catch (Exception e) {
            System.err.println("Failed to initialize GUI on EDT: " + e.getMessage());
            addStandardParameters();
            addAdditionalParameters();
            cp[0] = new works.lysenko.base.ui.ControlPanel(parameters, this);
        }
        
        cp[0].displayAndWait();
    }

    /**
     * Adds additional parameters based on the given params and types arrays.
     */
    @SuppressWarnings({"ObjectAllocationInLoop", "SwitchStatement"})
    private void addAdditionalParameters() {

        if (isNotNull(parameters.getAdditionalValues())) {
            aParams = new LinkedList<>();
            int i = 0;
            for (final String param : parameters.getAdditionalValues())
                switch (parameters.getAdditionalTypes()[i++]) {
                    case "t" -> aParams.add(new JTextField(parameters.getProperty(param), WIDTH));
                    case "p" -> aParams.add(new JPasswordField(parameters.getProperty(param), WIDTH)); //NON-NLS
                    default -> {
                    }
                }
        }
    }

    /**
     * Returns a JComboBox populated with entity names obtained from files matching a filter.
     */
    private JComboBox<Object> addFromFiles(final String type, final String directory, final String filter) {
        return GuiParameters.addFromFiles(type, directory, filter, parameters);
    }

    /**
     * Adds platforms to the combo box and initializes the reset button.
     */
    @SuppressWarnings("VariableNotUsedInsideIf")
    private void addPlatforms() {

        final List<String> platformNames = platformNames();
        platform = new JComboBox<>(platformNames.toArray());
        platform.setSelectedItem(parameters.getProperty(PLATFORM.name()));
        reset = new JButton((null == forcedPlatform) ? RESET : b(FIXED, BY, CONFIGURATION));
        reset.setEnabled(null == forcedPlatform);
        reset.addActionListener(e -> {
            reset();
            _Test.processCode(PLATFORMS_RESET);
        });
    }

    /**
     * Adds the standard parameters to the Parameters object.
     */
    public void addStandardParameters() {

        test = addFromFiles(TEST.name(), _TESTS_, TEST_PROPERTIES_EXTENSION);
        pool = addFromFiles(POOL.name(), ETC_, USERS_POOL_EXTENSION);
        addPlatforms();

        if (!Platform.ANDROID.getString().equals(parameters.getProperty(PLATFORM.name()))) {
            domain = new JTextField(parameters.getProperty(DOMAIN.name()), WIDTH);
            headless = new JCheckBox();
            headless.setSelected(Boolean.parseBoolean(parameters.getProperty(HEADLESS.name())));
        }
        allLeafs = new JCheckBox();
        allLeafs.setSelected(parameters.isAllLeafs());
        allLeafsCount = new JTextField(String.valueOf(parameters.getAllLeafsCount()), WIDTH);

        final String defaultTests = defaultIfEmpty(
                parameters.getTests(),
                Objects.toString(PropEnum._TEST_TESTS.get(), EMPTY)
        );
        testsCount = new JTextField(defaultTests, WIDTH);

        forbidOverexecution = createCheckBox(parameters.getForbidOverexecution(), PropEnum._TREE_FORBID_OVEREXECUTION);
        resilientMode = createCheckBox(parameters.getResilientMode(), PropEnum._TEST_RESILIENT_MODE);

        final String defaultWeight = defaultIfEmpty(
                parameters.getCompletionWeight(),
                PropEnum._TREE_COMPLETION_WEIGHT.defaultValue()
        );
        completionWeight = new JTextField(defaultWeight, WIDTH);

        traverseExtensions = createCheckBox(parameters.getTraverseExtensions(), PropEnum._TREE_TRAVERSE_EXTENSIONS);

        initCyclesComponents();
        initPropertiesComponents();
    }

    private static JCheckBox createCheckBox(final Boolean paramValue, final works.lysenko.util.apis._PropEnum propEnum) {
        final boolean selected = (paramValue != null)
                ? paramValue
                : Boolean.TRUE.equals(propEnum.get());
        final JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(selected);
        return checkBox;
    }

    private static String defaultIfEmpty(final String value, final String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }

    /**
     * Initializes the cycles calculation button, progress bar, and result label.
     */
    private void initCyclesComponents() {

        calculateCycles = new JButton(b(c(REQUIRED), FOR, FULL, COVERAGE));
        calculateCycles.addActionListener(e -> onCalculateCycles());

        cyclesPanel = CycleEstimatorPanel.createPanel(calculateCycles);

        attachChangeListener(test);
        attachChangeListener(allLeafs);
        attachChangeListener(allLeafsCount);
        attachChangeListener(completionWeight);
        attachChangeListener(testsCount);
        attachChangeListener(forbidOverexecution);
        attachChangeListener(resilientMode);
        attachChangeListener(traverseExtensions);
    }

    private void attachChangeListener(final Component comp) {
        CycleEstimatorPanel.attachChangeListener(comp, this::onTestSelectionChanged);
    }

    /**
     * Resets the cycle estimation display when test parameters change.
     */
    private void onTestSelectionChanged() {

        calculateCycles.setText(b(c(REQUIRED), FOR, FULL, COVERAGE));
        calculateCycles.setEnabled(true);
        updateWindowLayout();
    }

    /**
     * Initializes the properties preview and editing button.
     */
    private void initPropertiesComponents() {

        properties = new JButton(s(c(MODIFY), DOTS));
        properties.addActionListener(e -> {}); // Handled by unified Control Panel now
    }

    /**
     * Calculates the estimated average cycles required for 100% leaf coverage.
     */
    String calculateEstimatedCycles(final String testName) {

        return calculateEstimatedCycles(testName, null);
    }

    /**
     * Calculates the estimated average cycles required for 100% leaf coverage.
     */
    String calculateEstimatedCycles(final String testName, final IntConsumer progressConsumer) {

        return CycleEstimatorPanel.calculateEstimatedCycles(
                parameters, testName, headless, allLeafs, allLeafsCount,
                testsCount, forbidOverexecution, resilientMode, completionWeight,
                traverseExtensions, progressConsumer);
    }

    /**
     * Handles clicking the calculate cycles button.
     */
    private void onCalculateCycles() {

        calculateCycles.setEnabled(false);
        calculateCycles.setText("Calculating...");
        updateWindowLayout();

        final Object selected = (isNotNull(test)) ? test.getSelectedItem() : null;
        final String testName = (isNotNull(selected)) ? selected.toString() : parameters.getTest();

        final Thread thread = new Thread(() -> {
            try {
                final String text = calculateEstimatedCycles(testName, null);
                SwingUtilities.invokeLater(() -> {
                    calculateCycles.setText(text);
                    calculateCycles.setEnabled(true);
                    updateWindowLayout();
                });
            } catch (final RuntimeException e) {
                SwingUtilities.invokeLater(() -> {
                    calculateCycles.setText("Calculation failed");
                    calculateCycles.setEnabled(true);
                    updateWindowLayout();
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Revalidates and updates the panel layout to ensure components fit smoothly.
     */
    private void updateWindowLayout() {

        if (isNotNull(panel)) {
            panel.revalidate();
            panel.repaint();
        }
        if (isNotNull(cyclesPanel)) {
            cyclesPanel.revalidate();
            cyclesPanel.repaint();
        }
    }

    /**
     * Builds a dialogue box panel with various input components.
     *
     * @return a JPanel containing the dialogue box
     */
    @SuppressWarnings({"StatementWithEmptyBody", "ForeachStatement", "ObjectAllocationInLoop",
            "ValueOfIncrementOrDecrementUsed"})
    public JPanel parametersPanel() {

        if (test == null) addStandardParameters();
        panel = new JPanel(new GridBagLayout());
        addRows();

        final Dimension pref = panel.getPreferredSize();
        panel.setPreferredSize(new Dimension(Math.max(580, pref.width), pref.height));

        return panel;
    }

    private void addRows() {

        int rows = 0;

        rows = addRowOptional(TEST.name(), test, panel, rows);
        rows = addRowOptional(POOL.name(), pool, panel, rows);
        rows = addRowOptional(DOMAIN.name(), domain, panel, rows);
        // Add platform with reset button
        rows = addRowOptional(PLATFORM.name(), platform, panel, rows);
        rows = addRowOptional(EMPTY, reset, panel, rows);

        // Divider 1: between main parameters and special parameters
        rows = addDivider(panel, rows);

        // Add additional special properties
        rows = addRowOptional(TESTS.name(), testsCount, panel, rows);
        rows = addRowOptional(ALL_LEAFS_COUNT.name(), allLeafsCount, panel, rows);
        rows = addRowOptional(COMPLETION_WEIGHT.name(), completionWeight, panel, rows);
        rows = addDivider(panel, rows);
        rows = addRowOptional(ALL_LEAFS.name(), allLeafs, panel, rows);
        rows = addRowOptional(FORBID_OVEREXECUTION.name(), forbidOverexecution, panel, rows);
        rows = addRowOptional(RESILIENT_MODE.name(), resilientMode, panel, rows);
        rows = addRowOptional(TRAVERSE_EXTENSIONS.name(), traverseExtensions, panel, rows);
        rows = addRowOptional(HEADLESS.name(), headless, panel, rows);

        // Additional parameters
        rows = addRowsAdditional(rows);

    }

    private int addRowsAdditional(int rows) {
        if (isNotNull(aParams) && !aParams.isEmpty()) {
            rows = addDivider(panel, rows);
            int i = 0;
            for (final JTextField aParam : aParams) {
                rows = addRowOptional(parameters.getAdditionalValues()[i++], aParam, panel, rows);
            }
        }
        return rows;
    }

    /**
     * Propagates user input to the parameters object.
     */
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public void propagateUserInput() {

        parameters.setTest(null == test.getSelectedItem() ? EMPTY : test.getSelectedItem().toString());
        parameters.setPool(null == pool.getSelectedItem() ? EMPTY : pool.getSelectedItem().toString());
        parameters.setPlatform(null == platform.getSelectedItem() ? EMPTY : platform.getSelectedItem().toString());
        
        if (!parameters.getProperty(PLATFORM.name()).equals(Platform.ANDROID.getString())) {
            if (domain != null) {
                parameters.setProperty(DOMAIN.name(), domain.getText());
            }
            if (isNotNull(headless)) {
                parameters.setHeadless(headless.isSelected());
            }
        }
        
        if (isNotNull(allLeafs)) {
            parameters.setAllLeafs(allLeafs.isSelected());
        }
        if (isNotNull(allLeafsCount)) {
            try {
                parameters.setAllLeafsCount(Integer.parseInt(allLeafsCount.getText()));
            } catch (final NumberFormatException ignored) {
            }
        }
        if (parameters.getAllLeafsCount() > 1) {
            parameters.setAllLeafs(true);
            parameters.setAllLeafsCount(parameters.getAllLeafsCount());
        }
        propagatePropertyOptional(testsCount, TESTS.name());
        propagatePropertyOptional(forbidOverexecution, FORBID_OVEREXECUTION.name());
        propagatePropertyOptional(resilientMode, RESILIENT_MODE.name());
        propagatePropertyOptional(completionWeight, COMPLETION_WEIGHT.name());
        propagatePropertyOptional(traverseExtensions, TRAVERSE_EXTENSIONS.name());

        // Additional parameters
        if (isNotNull(aParams)) {
            int i = 0;
            for (final JTextField aParam : aParams)
                parameters.setProperty(parameters.getAdditionalValues()[i++], aParam.getText());
        }
    }

    private void propagatePropertyOptional(final JTextField tf, final String key) {
        GuiParameters.propagatePropertyOptional(parameters, tf, key);
    }

    private void propagatePropertyOptional(final JCheckBox cb, final String key) {
        GuiParameters.propagatePropertyOptional(parameters, cb, key);
    }

    public JButton getCalculateCycles() {

        return calculateCycles;
    }

    public JProgressBar getProgressBar() {

        return progressBar;
    }

    public JLabel getCycles() {

        return cycles;
    }

    public JButton getPropertiesButton() {

        return properties;
    }

    /**
     * Creates and returns a panel containing the coverage estimate controls.
     *
     * @return JPanel containing coverage estimation UI components
     */
    public JPanel createCoverageEstimatePanel() {

        if (isNull(cyclesPanel)) {
            initCyclesComponents();
        }
        return cyclesPanel;
    }
}
