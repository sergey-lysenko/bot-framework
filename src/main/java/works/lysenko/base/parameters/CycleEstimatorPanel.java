package works.lysenko.base.parameters;

import works.lysenko.Base;
import works.lysenko.base.Parameters;
import works.lysenko.base.TestProperties;
import works.lysenko.tree.CoverageEstimator;
import works.lysenko.tree.Ctrl;
import works.lysenko.util.apis.scenario._Ctrl;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.records.TestPropertiesDescriptor;
import works.lysenko.util.func.core.ClassLoader;
import works.lysenko.util.prop.tree.Include;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.prop.tree.Traverse;
import works.lysenko.util.spec.PropEnum;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Set;
import java.util.function.IntConsumer;

import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.____.FULL;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.core.TestProperties.readTestPropertiesFromFile;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static java.util.Objects.isNull;
import static works.lysenko.util.lang.word.C.COVERAGE;
import static works.lysenko.util.lang.word.R.REQUIRED;
import static works.lysenko.util.spec.Layout.Parts.TEST_PROPERTIES_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._TESTS_;

/**
 * Handles coverage cycle estimations and cycle controls panel initialization.
 */
public final class CycleEstimatorPanel {

    private CycleEstimatorPanel() {
    }

    /**
     * Attaches a listener callback to a Swing component when value changes.
     *
     * @param comp     the component to observe
     * @param listener the Runnable to invoke on change
     */
    public static void attachChangeListener(final Component comp, final Runnable listener) {

        if (isNull(comp)) return;
        if (comp instanceof JComboBox<?> cb) {
            cb.addActionListener(e -> listener.run());
        } else if (comp instanceof JCheckBox cb) {
            cb.addActionListener(e -> listener.run());
        } else if (comp instanceof JTextField tf) {
            tf.addActionListener(e -> listener.run());
            tf.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(final DocumentEvent e) { listener.run(); }
                @Override public void removeUpdate(final DocumentEvent e) { listener.run(); }
                @Override public void changedUpdate(final DocumentEvent e) { listener.run(); }
            });
        }
    }

    /**
     * Creates a JPanel containing the cycle calculation button.
     *
     * @param calculateCycles the button component
     * @return a flow layout JPanel holding the button
     */
    public static JPanel createPanel(final JButton calculateCycles) {

        final JPanel cyclesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cyclesPanel.add(calculateCycles);
        return cyclesPanel;
    }

    /**
     * Calculates estimated average cycles required for 100% leaf coverage.
     *
     * @param parameters          execution parameters
     * @param testName            name of the selected test
     * @param headless            headless checkbox
     * @param allLeafs            all-leafs checkbox
     * @param allLeafsCount       all-leafs-count input
     * @param testsCount          tests-count input
     * @param forbidOverexecution forbid-overexecution checkbox
     * @param resilientMode       resilient-mode checkbox
     * @param completionWeight    completion-weight input
     * @param traverseExtensions traverse-extensions checkbox
     * @param progressConsumer    progress Consumer
     * @return formatted result string
     */
    public static String calculateEstimatedCycles(
            final Parameters parameters,
            final String testName,
            final JCheckBox headless,
            final JCheckBox allLeafs,
            final JTextField allLeafsCount,
            final JTextField testsCount,
            final JCheckBox forbidOverexecution,
            final JCheckBox resilientMode,
            final JTextField completionWeight,
            final JCheckBox traverseExtensions,
            final IntConsumer progressConsumer) {

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
        setUserOverrideOptional(tp, testsCount, PropEnum._TEST_TESTS.getPropertyName());
        setUserOverrideOptional(tp, forbidOverexecution, PropEnum._TREE_FORBID_OVEREXECUTION.getPropertyName());
        setUserOverrideOptional(tp, resilientMode, PropEnum._TEST_RESILIENT_MODE.getPropertyName());
        setUserOverrideOptional(tp, completionWeight, PropEnum._TREE_COMPLETION_WEIGHT.getPropertyName());
        setUserOverrideOptional(tp, traverseExtensions, PropEnum._TREE_TRAVERSE_EXTENSIONS.getPropertyName());
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

    private static void setUserOverrideOptional(final TestProperties tp, final JTextField tf, final String propertyName) {

        if (isNotNull(tf) && !tf.getText().isBlank()) {
            tp.setUserOverride(propertyName, tf.getText().trim());
        }
    }

    private static void setUserOverrideOptional(final TestProperties tp, final JCheckBox cb, final String propertyName) {

        if (isNotNull(cb)) {
            tp.setUserOverride(propertyName, String.valueOf(cb.isSelected()));
        }
    }
}
