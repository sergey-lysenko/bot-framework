package works.lysenko.base.ui;

import works.lysenko.base.output.AllLeafCompletions;
import works.lysenko.base.output.EtaCorrection;
import works.lysenko.base.util.Telemetry;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.util._BotButton;
import works.lysenko.util.apis.util._Dashboard;
import works.lysenko.util.data.enums.Brackets;

import works.lysenko.util.spec.PropEnum;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.parameters;
import static works.lysenko.Base.timer;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.MiB;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.l;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.lang.word.A.AFTER;
import static works.lysenko.util.lang.word.C.CLASSES;
import static works.lysenko.util.lang.word.C.CURRENT;
import static works.lysenko.util.lang.word.D.DEBUG;
import static works.lysenko.util.lang.word.D.DURING;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.O.OUTPUT;
import static works.lysenko.util.lang.word.P.PAUSE;
import static works.lysenko.util.lang.word.P.PAUSED;
import static works.lysenko.util.lang.word.R.RESUME;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.S.SECONDS;
import static works.lysenko.util.lang.word.T.TESTING;
import static works.lysenko.util.lang.word.T.THREAD;
import static works.lysenko.util.spec.Symbols.QUS_MRK;
import static works.lysenko.util.spec.Symbols.VRT_BAR;
import static works.lysenko.util.spec.Symbols._PRCNT_;


/**
 * The Dashboard class provides a graphical user interface for displaying system telemetry and logs.
 * It extends the JFrame class and implements the ProvidesDashboard interface.
 */
@SuppressWarnings({"ClassWithoutLogger", "FieldHasSetterButNoGetter", "FeatureEnvy", "ClassWithTooManyFields",
        "ImplicitNumericConversion",
        "UseOfConcreteClass", "ClassWithoutNoArgConstructor", "ClassWithTooManyMethods", "ClassHasNoToStringMethod",
        "FinalClass",
        "ChainedMethodCall", "NestedMethodCall", "ClassWithTooManyDependencies", "ClassWithTooManyTransitiveDependencies",
        "ClassWithTooManyTransitiveDependents", "CyclicClassDependency", "AutoBoxing", "AutoUnboxing", "LawOfDemeter"})
public final class UserInterface extends JPanel implements _Dashboard {

    private static final int bytesInMiB = 1 << 20;
    private static final float RED_HUE = 0.0F;
    private static final float GREEN_HUE = 0.333F;
    private static final float SATURATION = 1.0F;
    private static final float BRIGHTNESS = 0.75F;
    private final JPanel container = new JPanel();
    private final JPanel breadcrumb = new JPanel();
    private final JPanel statusboard = new JPanel();
    private final JPanel business = new JPanel();
    private final JLabel status = new JLabel(s(QUS_MRK));
    private final JLabel cores = new JLabel(s(QUS_MRK));
    private final JLabel threads = new JLabel(s(QUS_MRK));
    private final JLabel usage = new JLabel(s(QUS_MRK));
    private final JLabel classes = new JLabel(s(QUS_MRK));
    private final JLabel memory = new JLabel(s(QUS_MRK));
    private final JLabel runtime = new JLabel(s(QUS_MRK));
    private final JLabel eta = new JLabel(s(QUS_MRK));
    private final JPanel switchboard = new JPanel();
    private final AnsiTextPane log = new AnsiTextPane();
    private final JScrollPane logScrollPane = new JScrollPane(log);
    private double previous = 0.0;
    private JBotButton debug;
    private JBotButton pause;
    private JBotButton halt;
    private JBotButton stop;

    /**
     * Initializes and builds a Dashboard with various components such as Breadcrumbs, Statusboard, Switchboard, and Log.
     * The Dashboard is displayed as a JFrame with a specified size and title.
     *
     * @param screenToSpawnDashboard The index of the screen to center the Dashboard on.
     */
    @SuppressWarnings({"WeakerAccess", "ImplicitCallToSuper", "PublicConstructor", "MethodParameterNamingConvention"})
    public UserInterface(final Integer screenToSpawnDashboard) {

        setLayout(new BorderLayout());
        buildStatusBoard();
        buildBusiness();
        buildSwitchBoard();
        buildLog();
        buildContainer();
        // centerThisWindow(screenToSpawnDashboard);
    }

    /**
     * This class represents a Dashboard object that is used to display various components such as Breadcrumbs,
     * Statusboard, Switchboard, and Log. The Dashboard is displayed as a JFrame with a specified size and title.
     *
     * @param screenToSpawnDashboard index of screen to spawn Dashboard on
     * @param isDebug                initial value for debug state
     */
    @SuppressWarnings({"BooleanParameter", "PublicConstructor", "WeakerAccess", "MethodParameterNamingConvention"})
    public UserInterface(final Integer screenToSpawnDashboard, final boolean isDebug) {

        this(screenToSpawnDashboard);
        if (isDebug) debug.activate();
    }

    /**
     * Converts a long value to a string representation in Mebibytes (MiB).
     *
     * @param l the long value to convert
     * @return the string representation of the value in Mebibytes
     */
    @SuppressWarnings({"ImplicitNumericConversion", "AutoBoxing"})
    private static String bytesToMiB(final long l) {

        return b(s((l / bytesInMiB)), s(MiB));
    }

    /**
     * Calculates the color to use for usage based on the previous usage and current usage values.
     *
     * @param previousUsage the previous usage value
     * @param currentUsage  the current usage value
     * @return the color to use for usage (red if the current usage is higher, green otherwise)
     */
    private static Color calculateUsageColor(final double previousUsage, final double currentUsage) {

        return previousUsage < currentUsage ? getRedColor() : getGreenColor();
    }

    /**
     * Creates a label for a given scenario.
     *
     * @param scenario The scenario for which a label is to be created.
     * @return A1 label for the given scenario.
     */
    @SuppressWarnings({"NestedMethodCall", "AutoBoxing"})
    private static JLabel createScenarioLabel(final _Scenario scenario) {

        final JLabel label;
        if (isNull(scenario)) label = new JLabel(s(QUS_MRK));
        else label = new JLabel(scenario.getSimpleName());
        return label;
    }

    /**
     * Retrieves the green color using the HSB color model.
     *
     * @return the green color
     */
    private static Color getGreenColor() {

        return Color.getHSBColor(GREEN_HUE, SATURATION, BRIGHTNESS);
    }

    /**
     * Retrieves the red color using the HSB color model.
     *
     * @return the red color
     */
    private static Color getRedColor() {

        return Color.getHSBColor(RED_HUE, SATURATION, BRIGHTNESS);
    }

    /**
     * Sets the active state of a JBotButton.
     *
     * @param button The JBotButton to set the active state for
     * @param active The active state to set (true = active, false = inactive)
     */
    private static void setButtonActiveState(final _BotButton button, final boolean active) {

        final Runnable r = () -> {
            if (active) button.activate();
            else button.deactivate();
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    /**
     * Retrieves the debug button.
     *
     * @return the debug button
     */
    @Override
    public boolean isDebug() {

        return debug.isActive();
    }

    @Override
    public void setDebug(final boolean active) {

        setButtonActiveState(debug, active);
    }

    /**
     * Retrieves the halt button from the dashboard.
     *
     * @return the halt button
     */
    @Override
    public boolean isHalt() {

        return halt.isActive();
    }

    /**
     * Sets the state of the halt button.
     *
     * @param active true to activate the halt button, false to deactivate it
     */
    @Override
    public void setHalt(final boolean active) {

        setButtonActiveState(halt, active);
    }

    /**
     * Retrieves the pause button from the dashboard.
     *
     * @return the pause button from the dashboard
     */
    @Override
    public boolean isPause() {

        return pause.isActive();
    }

    @Override
    public void setPause(final boolean active) {

        setButtonActiveState(pause, active);
    }

    /**
     * Retrieves the stop button from the dashboard.
     *
     * @return the stop button
     */
    public boolean isStop() {

        return stop.isActive();
    }

    /**
     * Sets the state of the stop button in the dashboard.
     *
     * @param active true to activate the stop button, false to deactivate it
     */
    @Override
    public void setStop(final boolean active) {

        setButtonActiveState(stop, active);
    }

    /**
     * Sets the breadcrumb component with the provided content.
     *
     * @param content The list of scenarios to display in the breadcrumb.
     *                Each scenario must implement the ProvidesScenario interface.
     */
    @SuppressWarnings("ForeachStatement")
    public void setBreadcrumb(final List<? extends _Scenario> content) {

        final List<_Scenario> safeContent = (null == content) ? List.of() : new ArrayList<>(content);
        final Runnable r = () -> {
            breadcrumb.removeAll();
            for (int i = 0; i < safeContent.size(); i++) {
                if (i > 0) {
                    final JLabel separator = new JLabel(" \u2192 ");
                    separator.setForeground(Color.GRAY);
                    breadcrumb.add(separator);
                }
                final JLabel scenarioLabel = createScenarioLabel(safeContent.get(i));
                breadcrumb.add(scenarioLabel);
            }
            breadcrumb.revalidate();
            breadcrumb.repaint();
            container.revalidate();
            container.repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    /**
     * Sets the information message and refreshes the status board with the provided telemetry data.
     *
     * @param message   The information message to be displayed.
     * @param telemetry The telemetry data used to refresh the status board.
     */
    public void setInfo(final String message, final Telemetry telemetry) {

        final int depth = works.lysenko.Base.exec == null ? 0 : works.lysenko.Base.exec.scenarios().depth();
        final String indent = "  ".repeat(Math.max(0, depth));
        SwingUtilities.invokeLater(() -> {
            final JScrollBar scrollBar = logScrollPane.getVerticalScrollBar();
            final boolean followOutput = scrollBar.getValue() + scrollBar.getVisibleAmount() >= scrollBar.getMaximum() - 2;
            refreshStatusBoard(telemetry);
            log.appendOutput(indent + message);
            if (followOutput) SwingUtilities.invokeLater(() -> scrollBar.setValue(scrollBar.getMaximum()));
        });
    }

    /**
     * Adds a label and a component to the status board.
     */
    private void addLabelAndComponent(final JLabel label, final JComponent component) {

        statusboard.add(label);
        statusboard.add(component);
    }

    private void buildBusiness() {

        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        status.setForeground(new Color(0x02, 0x84, 0xC7));
        business.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCB, 0xD5, 0xE1)),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        business.add(status);
    }

    /**
     * Builds the container by adding various components.
     * The components added are Breadcrumbs, Statusboard,
     * Switchboard, and Log.
     */
    private void buildContainer() {

        breadcrumb.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));
        breadcrumb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCB, 0xD5, 0xE1)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        statusboard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCB, 0xD5, 0xE1)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        switchboard.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        container.add(breadcrumb);
        container.add(statusboard);
        container.add(business);
        container.add(switchboard);
        add(container, BorderLayout.NORTH);
        add(logScrollPane, BorderLayout.CENTER);
    }

    /**
     * This method is used to set the background color of the log component in the Dashboard.
     */
    private void buildLog() {

        logScrollPane.setMinimumSize(new Dimension());
        logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        logScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * Builds the status board component of the dashboard.
     * The status board is built by adding various labels and components to display information such as classes, threads,
     * usage, cores, memory,
     * and runtime.
     */
    private void buildStatusBoard() {

        statusboard.setLayout(new FlowLayout());
        addLabelAndComponent(new JLabel(), classes);
        addLabelAndComponent(new JLabel(IN), threads);
        addLabelAndComponent(new JLabel(USED), usage);
        addLabelAndComponent(new JLabel(OF), cores);
        addLabelAndComponent(new JLabel(IN), memory);
        addLabelAndComponent(new JLabel(DURING), runtime);
        addLabelAndComponent(new JLabel(l(SECONDS, true)), eta);
    }

    /**
     * Builds the switchboard component of the dashboard.
     * The switchboard is built by creating JBotButton objects
     * for each desired button and adding them to the switchboard.
     */
    @SuppressWarnings("NestedMethodCall")
    private void buildSwitchBoard() {

        debug = new JBotButton(b(c(DEBUG), OUTPUT));
        pause = new JBotButton(b(c(PAUSE), CURRENT, c(TEST), EXECUTION), b(c(RESUME), TEST, EXECUTION, e(Brackets.ROUND, c(PAUSED))));
        halt = new JBotButton(b(c(HALT), c(TEST), AFTER, CURRENT, c(SCENARIO)));
        stop = new JBotButton(b(c(STOP), TESTING, AFTER, CURRENT, c(TEST)));
        switchboard.add(debug);
        switchboard.add(pause);
        switchboard.add(halt);
        switchboard.add(stop);
    }

    // Removed centerThisWindow since it is a JPanel

    @SuppressWarnings("NestedMethodCall")
    private void refreshBusiness() {

        status.setText(exec.getStatus());
    }

    @SuppressWarnings({"NestedMethodCall", "AutoBoxing"})
    private void refreshClassStatus(final Telemetry telemetry) {

        classes.setText(
                b(
                        e(Brackets.SQUARE,
                                b(VRT_BAR,
                                        s(telemetry.data().loadedC()),
                                        s(telemetry.data().unloadedC()),
                                        s(telemetry.data().totalC())
                                )
                        ), CLASSES
                )
        );
    }

    @SuppressWarnings("NestedMethodCall")
    private void refreshCoreStatus(final Telemetry telemetry) {

        cores.setText(s1(telemetry.data().cores(), CORE));
    }

    @SuppressWarnings("NestedMethodCall")
    private void refreshMemoryStatus(final Telemetry telemetry) {

        memory.setText(
                e(Brackets.SQUARE,
                        b(VRT_BAR,
                                s(bytesToMiB(telemetry.data().freeM())),
                                s(bytesToMiB(telemetry.data().totalM())),
                                s(bytesToMiB(telemetry.data().maxM()))
                        )
                )
        );
    }

    /**
     * Refreshes the status board component with the provided telemetry data.
     * Updates the core status, thread status, usage status, class status, memory status,
     * and runtime status.
     *
     * @param telemetry The telemetry data used to refresh the status board.
     */
    @SuppressWarnings("NestedMethodCall")
    private void refreshStatusBoard(final Telemetry telemetry) {

        refreshBusiness();
        refreshCoreStatus(telemetry);
        refreshThreadStatus(telemetry);
        refreshUsageStatus(telemetry);
        refreshClassStatus(telemetry);
        refreshMemoryStatus(telemetry);
        runtime.setText(s(timer.msSinceStart() / 1000));
        eta.setText(calculateEtaString());
    }

    @SuppressWarnings("NestedMethodCall")
    private void refreshThreadStatus(final Telemetry telemetry) {

        threads.setText(s1(telemetry.data().threads(), THREAD));
    }

    /**
     * Refreshes the usage status component with the provided telemetry data.
     * Updates the usage value, sets the text color based on the comparison between previous and current usage,
     * and updates the previous usage value.
     *
     * @param telemetry The telemetry data used to refresh the usage status component.
     */
    @SuppressWarnings({"NestedMethodCall", "AutoBoxing"})
    private void refreshUsageStatus(final Telemetry telemetry) {

        final double currentUsage = telemetry.data().spent();
        usage.setText(b(s(format("%.2f", telemetry.data().spent()), _PRCNT_))); //NON-NLS
        usage.setForeground(calculateUsageColor(previous, currentUsage));
        previous = currentUsage;
    }

    @Override
    public void setTitle(String title) {
        // Ignored for JPanel, handled by ControlPanel
    }

    /**
     * Calculates the estimated time remaining for current execution across different execution modes
     * (e.g. all-leaf coverage mode, fixed test count mode, or fallback estimation).
     *
     * @return Formatted ETA string (e.g. "(ETA 02:15)", "(ETA --:--)", or "(ETA Done)")
     */
    public static long calculateEtaMs() {

        if (isNull(core) || isNull(timer)) return 0L;

        final long elapsedMs = timer.msSinceStart();
        if (elapsedMs < 1000L) return 0L;

        long rawEtaMs = 0L;

        final boolean allLeafsMode = (isNotNull(parameters) && parameters.isAllLeafs())
                || Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get());

        if (allLeafsMode) {
            final boolean multipleExecutionsPerLeaf = isNotNull(parameters) && parameters.getAllLeafsCount() > 1;
            final int executed = multipleExecutionsPerLeaf
                    ? core.getExecutedLeafExecutionsCount()
                    : core.getExecutedLeafsCount();
            final int total = multipleExecutionsPerLeaf
                    ? core.getTotalLeafExecutionsCount()
                    : core.getAccessibleLeafs().size();

            if (0 == total || (executed >= total && core.areAllLeafsExecuted())) {
                return 0L;
            }
            if (executed > 0 && total > executed) {
                final double avgMsPerExecution = (double) elapsedMs / executed;
                final int remainingExecutions = total - executed;
                rawEtaMs = Math.round(remainingExecutions * avgMsPerExecution);
            }
        } else {
            // Fixed test count mode (.test.tests specified)
            final Integer totalTests = core.getTotalTests();
            if (isNotNull(totalTests) && totalTests > 0) {
                final int completedTests = (isNotNull(core.getTest()) && isNotNull(core.getTest().repeater()) && isNotNull(core.getTest().repeater().getHistory()))
                        ? core.getTest().repeater().getHistory().size()
                        : 0;
                if (completedTests >= totalTests) {
                    return 0L;
                }
                if (completedTests > 0) {
                    final double avgMsPerTest = (double) elapsedMs / completedTests;
                    final int remainingTests = totalTests - completedTests;
                    rawEtaMs = Math.round(remainingTests * avgMsPerTest);
                }
            } else {
                // Fallback: general leaf-based progress
                final int totalLeafs = (isNotNull(core.getAccessibleLeafs())) ? core.getAccessibleLeafs().size() : 0;
                final int executedLeafs = core.getExecutedLeafsCount();

                if (totalLeafs > 0 && executedLeafs > 0 && totalLeafs > executedLeafs) {
                    final double avgMsPerLeaf = (double) elapsedMs / executedLeafs;
                    final int remainingLeafs = totalLeafs - executedLeafs;
                    rawEtaMs = Math.round(remainingLeafs * avgMsPerLeaf);
                }
            }
        }

        if (rawEtaMs <= 0L) return 0L;

        EtaCorrection.recordInitialPrediction(elapsedMs + rawEtaMs);
        return rawEtaMs;
    }

    private static String calculateEtaString() {

        final long remainingMs = calculateEtaMs();
        if (remainingMs > 0L) {
            AllLeafCompletions.recordEtaSample(remainingMs);
            return b("(ETA", s(formatMs(remainingMs)) + ")");
        }

        if (isNull(core) || isNull(timer)) return "(ETA --:--)";
        final long elapsedMs = timer.msSinceStart();
        if (elapsedMs < 1000L) return "(ETA --:--)";

        final boolean allLeafsMode = (isNotNull(parameters) && parameters.isAllLeafs())
                || Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get());
        if (allLeafsMode) {
            final boolean multipleExecutionsPerLeaf = isNotNull(parameters) && parameters.getAllLeafsCount() > 1;
            final int executed = multipleExecutionsPerLeaf
                    ? core.getExecutedLeafExecutionsCount()
                    : core.getExecutedLeafsCount();
            final int total = multipleExecutionsPerLeaf
                    ? core.getTotalLeafExecutionsCount()
                    : core.getAccessibleLeafs().size();
            if (executed >= total && core.areAllLeafsExecuted()) return "(ETA Done)";
            return "(ETA --:--)";
        }

        final Integer totalTests = core.getTotalTests();
        if (isNotNull(totalTests) && totalTests > 0) {
            final int completedTests = (isNotNull(core.getTest()) && isNotNull(core.getTest().repeater()) && isNotNull(core.getTest().repeater().getHistory()))
                    ? core.getTest().repeater().getHistory().size()
                    : 0;
            if (completedTests >= totalTests) return "(ETA Done)";
            return "(ETA --:--)";
        }

        return "(ETA --:--)";
    }

    private static String formatMs(final long ms) {

        final long totalSec = Math.max(0L, ms / 1000L);
        final long hours = totalSec / 3600L;
        final long minutes = (totalSec % 3600L) / 60L;
        final long seconds = totalSec % 60L;

        if (hours > 0) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        }
    }

    @Override
    public void dispose() {
        // Ignored for JPanel, handled by ControlPanel
    }
}
