package works.lysenko.base.parameters;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.Base;
import works.lysenko.base.Parameters;
import works.lysenko.base.TestProperties;
import works.lysenko.base.util.Platforms;
import works.lysenko.tree.CoverageEstimator;
import works.lysenko.tree.Ctrl;
import works.lysenko.util.apis.parameters._GUI;
import works.lysenko.util.apis.scenario._Ctrl;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.data.enums.Platform;
import works.lysenko.util.data.records.TestPropertiesDescriptor;
import works.lysenko.util.func.core.ClassLoader;
import works.lysenko.util.prop.tree.Include;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.prop.tree.Traverse;
import works.lysenko.util.spec.PropEnum;

import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static works.lysenko.base.Parameters.WIDTH;
import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.FULL;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.ExecutionParameter.ALL_LEAFS;
import static works.lysenko.util.data.enums.ExecutionParameter.ALL_LEAFS_COUNT;
import static works.lysenko.util.data.enums.ExecutionParameter.COMPLETION_WEIGHT;
import static works.lysenko.util.data.enums.ExecutionParameter.DOMAIN;
import static works.lysenko.util.data.enums.ExecutionParameter.FORBID_OVEREXECUTION;
import static works.lysenko.util.data.enums.ExecutionParameter.HEADLESS;
import static works.lysenko.util.data.enums.ExecutionParameter.PLATFORM;
import static works.lysenko.util.data.enums.ExecutionParameter.POOL;
import static works.lysenko.util.data.enums.ExecutionParameter.TEST;
import static works.lysenko.util.data.enums.ExecutionParameter.TRAVERSE_EXTENSIONS;
import static works.lysenko.util.data.enums.ExitCode.CLOSED_THROUGH_GUI;
import static works.lysenko.util.data.enums.ExitCode.PLATFORMS_RESET;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.TestProperties.readTestPropertiesFromFile;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static java.util.Objects.isNull;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.C.COVERAGE;
import static works.lysenko.util.lang.word.D.DELETE;
import static works.lysenko.util.lang.word.F.FIXED;
import static works.lysenko.util.lang.word.M.MODIFY;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.P.PLATFORMS;
import static works.lysenko.util.lang.word.P.PROPERTIES;
import static works.lysenko.util.lang.word.R.REQUIRED;
import static works.lysenko.util.lang.word.R.RESET;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.V.VERIFICATION;
import static works.lysenko.util.prop.core.Start.forcedPlatform;
import static works.lysenko.util.spec.Layout.Directories.ETC_;
import static works.lysenko.util.spec.Layout.Files.PLATFORMS_;
import static works.lysenko.util.spec.Layout.Parts.TEST_PROPERTIES_EXTENSION;
import static works.lysenko.util.spec.Layout.Parts.USERS_POOL_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._TESTS_;
import static works.lysenko.util.spec.Symbols._DASH_;

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
    public Gui(final Parameters parameters) {

        this.parameters = parameters;
    }

    /**
     * Adds a labeled row to a GridBagLayout container.
     *
     * @param label     the label for the component
     * @param comp      the component to add
     * @param container the container to add the label and component to
     * @param row       the grid row index
     */
    private static void addRow(final String label, final Component comp, final JPanel container, final int row) {

        final JLabel l = new JLabel(label);
        if (comp instanceof JCheckBox cb) {
            l.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(final java.awt.event.MouseEvent e) {
                    cb.setSelected(!cb.isSelected());
                }
            });
        }

        final GridBagConstraints cLabel = new GridBagConstraints();
        cLabel.gridx = 0;
        cLabel.gridy = row;
        cLabel.anchor = GridBagConstraints.LINE_START;
        cLabel.insets = new Insets(3, 4, 3, 12);
        container.add(l, cLabel);

        final GridBagConstraints cComp = new GridBagConstraints();
        cComp.gridx = 1;
        cComp.gridy = row;
        cComp.weightx = 1.0;
        cComp.anchor = GridBagConstraints.LINE_START;
        if (comp instanceof JButton) {
            cComp.fill = GridBagConstraints.NONE;
        } else {
            cComp.fill = GridBagConstraints.HORIZONTAL;
        }
        cComp.insets = new Insets(3, 0, 3, 4);
        container.add(comp, cComp);
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
     * This method adds standard parameters, adds additional parameters, displays a dialogue box,
     * propagates user input, and allows the user to exit the program.
     */
    public final void display() {

        addStandardParameters();
        addAdditionalParameters();
        
        final works.lysenko.base.ui.ControlPanel controlPanel = new works.lysenko.base.ui.ControlPanel(parameters, this);
        controlPanel.displayAndWait();
        
        // Final user input propagation happens in ControlPanel startRun
    }

    /**
     * Adds additional parameters based on the given params and types arrays.
     * Parameters with type "t" will create a JTextField and parameters with type "p" will create a JPasswordField.
     *
     * @throws NullPointerException if params is null
     */
    @SuppressWarnings({"ObjectAllocationInLoop", "SwitchStatement"})
    private void addAdditionalParameters() {

        // Additional parameters
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
     * Returns a JComboBox populated with entity names obtained from files in a given directory that match a specified filter.
     *
     * @param type      the type of entity to retrieve
     * @param directory the directory to search for files
     * @param filter    the filter to match file names against
     * @return a JComboBox containing the entity names obtained from the files
     */
    private JComboBox<Object> addFromFiles(final String type, final String directory, final String filter) {

        final List<String> entityList = new LinkedList<>();
        final String propVal = parameters.getProperty(type);
        if (isNotNull(propVal) && !propVal.isEmpty()) {
            entityList.add(propVal);
        }

        final File dir = new File(directory);
        final File[] files = dir.listFiles((dir1, name) -> name.endsWith(filter));
        if (isNotNull(files)) {
            for (final File file : files) {
                final String entity = removeEnd(file.getName(), filter);
                if (!entityList.contains(entity)) entityList.add(entity);
            }
            entityList.sort(null);
        }

        final JComboBox<Object> comboBox = new JComboBox<>(entityList.toArray());
        comboBox.setSelectedItem(parameters.getProperty(type));
        return comboBox;
    }

    /**
     * Adds platforms to the combo box and initializes the reset button.
     */
    @SuppressWarnings("VariableNotUsedInsideIf")
    private void addPlatforms() {

        final List<String> platformNames = platformNames();
        // Creating Browsers ComboBox
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
    void addStandardParameters() {
        // Standard parameters
        test = addFromFiles(TEST.name(), _TESTS_, TEST_PROPERTIES_EXTENSION);
        pool = addFromFiles(POOL.name(), ETC_, USERS_POOL_EXTENSION);
        addPlatforms();

        //noinspection StatementWithEmptyBody
        if (Platform.ANDROID.getString().equals(parameters.getProperty(PLATFORM.name()))) {
            /* Devices selection is commented out until implementation of automatic devices management
            List<String> devices = loadLinesFromFile(Paths.get(DEVICES_FILE));
            device = new JComboBox<>(devices.toArray());
            device.setSelectedItem(get(\"DEVICE\")); */
        } else {
            domain = new JTextField(parameters.getProperty(DOMAIN.name()), WIDTH);
            headless = new JCheckBox();
            headless.setSelected(Boolean.parseBoolean(parameters.getProperty(HEADLESS.name())));
        }
        allLeafs = new JCheckBox();
        allLeafs.setSelected(parameters.isAllLeafs());
        allLeafsCount = new JTextField(String.valueOf(parameters.getAllLeafsCount()), WIDTH);

        final String paramTests = parameters.getTests();
        final Integer propTests = PropEnum._TEST_TESTS.get();
        final String defaultTests = (isNotNull(paramTests) && !paramTests.isEmpty())
                ? paramTests
                : (isNotNull(propTests) ? propTests.toString() : EMPTY);
        testsCount = new JTextField(defaultTests, WIDTH);

        final Boolean paramForbid = parameters.getForbidOverexecution();
        final boolean defaultForbid = (isNotNull(paramForbid))
                ? paramForbid
                : Boolean.TRUE.equals(PropEnum._TREE_FORBID_OVEREXECUTION.get());
        forbidOverexecution = new JCheckBox();
        forbidOverexecution.setSelected(defaultForbid);

        final String paramWeight = parameters.getCompletionWeight();
        final String defaultWeight = (isNotNull(paramWeight) && !paramWeight.isEmpty())
                ? paramWeight
                : PropEnum._TREE_COMPLETION_WEIGHT.defaultValue();
        completionWeight = new JTextField(defaultWeight, WIDTH);

        final Boolean paramTraverse = parameters.getTraverseExtensions();
        final boolean defaultTraverse = (isNotNull(paramTraverse))
                ? paramTraverse
                : Boolean.TRUE.equals(PropEnum._TREE_TRAVERSE_EXTENSIONS.get());
        traverseExtensions = new JCheckBox();
        traverseExtensions.setSelected(defaultTraverse);

        initCyclesComponents();
        initPropertiesComponents();
    }

    /**
     * Initializes the cycles calculation button, progress bar, and result label.
     */
    private void initCyclesComponents() {

        calculateCycles = new JButton(b(c(REQUIRED), FOR, FULL, COVERAGE));
        calculateCycles.addActionListener(e -> onCalculateCycles());

        cyclesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cyclesPanel.add(calculateCycles);

        if (isNotNull(test)) {
            test.addActionListener(e -> onTestSelectionChanged());
        }
        if (isNotNull(allLeafs)) {
            allLeafs.addActionListener(e -> onTestSelectionChanged());
        }
        if (isNotNull(allLeafsCount)) {
            allLeafsCount.addActionListener(e -> onTestSelectionChanged());
            allLeafsCount.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onTestSelectionChanged(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onTestSelectionChanged(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onTestSelectionChanged(); }
            });
        }
        if (isNotNull(testsCount)) {
            testsCount.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onTestSelectionChanged(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onTestSelectionChanged(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onTestSelectionChanged(); }
            });
        }
        if (isNotNull(forbidOverexecution)) {
            forbidOverexecution.addActionListener(e -> onTestSelectionChanged());
        }
        if (isNotNull(completionWeight)) {
            completionWeight.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onTestSelectionChanged(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onTestSelectionChanged(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onTestSelectionChanged(); }
            });
        }
        if (isNotNull(traverseExtensions)) {
            traverseExtensions.addActionListener(e -> onTestSelectionChanged());
        }
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

    // Removed onPropertiesClicked as Properties panel is now visible alongside parameters

    /**
     * Calculates the estimated average cycles required for 100% leaf coverage
     * of the specified test suite.
     *
     * @param testName the name of the test to calculate cycles for
     * @return a formatted string with the result or an explanation of why calculation could not proceed
     */
    String calculateEstimatedCycles(final String testName) {

        return calculateEstimatedCycles(testName, null);
    }

    /**
     * Calculates the estimated average cycles required for 100% leaf coverage
     * of the specified test suite, reporting progress to a consumer.
     *
     * @param testName         the name of the test to calculate cycles for
     * @param progressConsumer consumer receiving progress percentage, nullable
     * @return a formatted string with the result or an explanation of why calculation could not proceed
     */
    String calculateEstimatedCycles(final String testName, final IntConsumer progressConsumer) {

        if (isNull(testName) || testName.isBlank()) {
            return "No test selected";
        }
        try {
            readTestPropertiesFromFile(new TestPropertiesDescriptor(_TESTS_, testName, TEST_PROPERTIES_EXTENSION));
        } catch (final RuntimeException e) {
            return "Test configuration not found";
        }

        final TestProperties tp = (isNotNull(Base.properties)) ? Base.properties : new TestProperties();
        Base.properties = tp;

        final boolean isHeadless = isNotNull(headless) && headless.isSelected();
        final boolean isAllLeafs = isNotNull(allLeafs) && allLeafs.isSelected();

        int leafsCount = 1;
        if (isNotNull(allLeafsCount) && !allLeafsCount.getText().isBlank()) {
            try {
                leafsCount = Math.max(1, Integer.parseInt(allLeafsCount.getText().trim()));
            } catch (final NumberFormatException ignored) {
                if (isNotNull(parameters)) leafsCount = parameters.getAllLeafsCount();
            }
        } else if (isNotNull(parameters)) {
            leafsCount = parameters.getAllLeafsCount();
        }

        tp.prepareTestConfiguration(testName, isHeadless, isAllLeafs, leafsCount);
        if (isNotNull(testsCount) && !testsCount.getText().isBlank()) {
            tp.setUserOverride(PropEnum._TEST_TESTS.getPropertyName(), testsCount.getText().trim());
        }
        if (isNotNull(forbidOverexecution)) {
            tp.setUserOverride(PropEnum._TREE_FORBID_OVEREXECUTION.getPropertyName(), String.valueOf(forbidOverexecution.isSelected()));
        }
        if (isNotNull(completionWeight) && !completionWeight.getText().isBlank()) {
            tp.setUserOverride(PropEnum._TREE_COMPLETION_WEIGHT.getPropertyName(), completionWeight.getText().trim());
        }
        if (isNotNull(traverseExtensions)) {
            tp.setUserOverride(PropEnum._TREE_TRAVERSE_EXTENSIONS.getPropertyName(), String.valueOf(traverseExtensions.isSelected()));
        }
        Scenario.refresh();
        Include.refresh();
        Traverse.refresh();

        final String rootPackage = (isNotNull(Scenario.root) && !Scenario.root.isBlank())
                ? Scenario.root
                : PropEnum._TEST_ROOT.get();

        if (isNull(rootPackage) || rootPackage.isBlank()) {
            return "No root scenario configured";
        }

        final Set<_Scenario> scenarios = ClassLoader.readFrom(rootPackage, false);
        if (isNull(scenarios) || scenarios.isEmpty()) {
            return "No scenarios found";
        }

        final _Ctrl rootCtrl = new Ctrl(null, scenarios);
        final int leafs = rootCtrl.getAccessibleLeafs().size();
        if (0 == leafs) {
            return "0 leafs found";
        }

        final int target = leafsCount;
        final int recommended = CoverageEstimator.estimateAverageCycles(rootCtrl, target, progressConsumer);
        return b(s1(recommended, "test"), REQUIRED, FOR, FULL, COVERAGE, e(ROUND, s1(leafs, "leaf")));
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
    public JPanel dialogueBox() {

        panel = new JPanel(new GridBagLayout());
        int row = 0;

        addRow(TEST.name(), test, panel, row++);
        addRow(POOL.name(), pool, panel, row++);
        addRow(PLATFORM.name(), platform, panel, row++);
        
        // Add additional special properties
        if (domain != null) {
            addRow(DOMAIN.name(), domain, panel, row++);
        }
        if (headless != null) {
            addRow(HEADLESS.name(), headless, panel, row++);
        }
        if (allLeafs != null) {
            addRow(ALL_LEAFS.name(), allLeafs, panel, row++);
        }
        if (allLeafsCount != null) {
            addRow(ALL_LEAFS_COUNT.name(), allLeafsCount, panel, row++);
        }
        if (testsCount != null) {
            addRow(works.lysenko.util.data.enums.ExecutionParameter.TESTS.name(), testsCount, panel, row++);
        }
        if (forbidOverexecution != null) {
            addRow(FORBID_OVEREXECUTION.name(), forbidOverexecution, panel, row++);
        }
        if (completionWeight != null) {
            addRow(COMPLETION_WEIGHT.name(), completionWeight, panel, row++);
        }
        if (traverseExtensions != null) {
            addRow(TRAVERSE_EXTENSIONS.name(), traverseExtensions, panel, row++);
        }

        // addRow(PROPERTIES, properties, panel, row++); // Moved to properties panel on right side

        // Additional parameters
        if (isNotNull(aParams)) {
            int i = 0;
            for (final JTextField aParam : aParams) {
                addRow(parameters.getAdditionalValues()[i++], aParam, panel, row++);
            }
        }

        addRow(EMPTY, reset, panel, row);

        final Dimension pref = panel.getPreferredSize();
        panel.setPreferredSize(new Dimension(Math.max(580, pref.width), pref.height));

        return panel;
    }

    /**
     * Propagates user input to the parameters object.
     */
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public void propagateUserInput() {
        // Propagation of the user input
        parameters.setTest(null == test.getSelectedItem() ? EMPTY : test.getSelectedItem().toString());
        parameters.setPool(null == pool.getSelectedItem() ? EMPTY : pool.getSelectedItem().toString());
        parameters.setPlatform(null == platform.getSelectedItem() ? EMPTY : platform.getSelectedItem().toString());
        
        //noinspection StatementWithEmptyBody
        if (parameters.getProperty(PLATFORM.name()).equals(Platform.ANDROID.getString())) {
            /* Devices selection is commented out until implementation of automatic devices management
            put("DEVICE", device.getSelectedItem() == null ? StringUtils.EMPTY : device.getSelectedItem().toString()); */
        } else {
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
        if (isNotNull(testsCount) && !testsCount.getText().isBlank()) {
            parameters.setProperty(works.lysenko.util.data.enums.ExecutionParameter.TESTS.name(), testsCount.getText().trim());
        }
        if (isNotNull(forbidOverexecution)) {
            parameters.setProperty(FORBID_OVEREXECUTION.name(), String.valueOf(forbidOverexecution.isSelected()));
        }
        if (isNotNull(completionWeight) && !completionWeight.getText().isBlank()) {
            parameters.setProperty(COMPLETION_WEIGHT.name(), completionWeight.getText().trim());
        }
        if (isNotNull(traverseExtensions)) {
            parameters.setProperty(TRAVERSE_EXTENSIONS.name(), String.valueOf(traverseExtensions.isSelected()));
        }

        // Additional parameters
        if (isNotNull(aParams)) {
            int i = 0;
            for (final JTextField aParam : aParams)
                parameters.setProperty(parameters.getAdditionalValues()[i++], aParam.getText());
        }
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
     * Creates and returns a panel containing the coverage estimate controls
     * (calculateCycles button, progressBar, and cycles label).
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
