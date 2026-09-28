package works.lysenko.base.ui;

import works.lysenko.base.util.Telemetry;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.util._BotButton;
import works.lysenko.util.apis.util._Dashboard;
import works.lysenko.util.data.enums.Brackets;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static works.lysenko.Base.exec;
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
public final class UserInterface extends JFrame implements _Dashboard {

    private static final int bytesInMiB = 1 << 20;
    private static final float RED_HUE = 0.0F;
    private static final float GREEN_HUE = 0.333F;
    private static final float SATURATION = 1.0F;
    private static final float BRIGHTNESS = 0.75F;
    private static final int DASHBOARD_WIDTH = 1024;
    private static final int DASHBOARD_HEIGHT = 256;
    private static final float DOT_TWENTY_SEVEN = 0.27f;
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
    private final JPanel switchboard = new JPanel();
    private final AnsiTextPane log = new AnsiTextPane();
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

        setTitle(initialization);
        setSize(DASHBOARD_WIDTH, DASHBOARD_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        add(container);
        buildStatusBoard();
        buildBusiness();
        buildSwitchBoard();
        buildLog();
        buildContainer();
        centerThisWindow(screenToSpawnDashboard);
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

        if (active) button.activate();
        else button.deactivate();
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

        breadcrumb.removeAll();
        for (final _Scenario scenario : content) {
            final JLabel scenarioLabel = createScenarioLabel(scenario);
            breadcrumb.add(scenarioLabel);
        }
        container.repaint();
    }

    /**
     * Sets the information message and refreshes the status board with the provided telemetry data.
     *
     * @param message   The information message to be displayed.
     * @param telemetry The telemetry data used to refresh the status board.
     */
    public void setInfo(final String message, final Telemetry telemetry) {

        refreshStatusBoard(telemetry);
        log.ansiSetText(message);
    }

    /**
     * Adds a label and a component to the status board.
     */
    private void addLabelAndComponent(final JLabel label, final JComponent component) {

        statusboard.add(label);
        statusboard.add(component);
    }

    private void buildBusiness() {

        business.add(status);
    }

    /**
     * Builds the container by adding various components.
     * The components added are Breadcrumbs, Statusboard,
     * Switchboard, and Log.
     */
    private void buildContainer() {

        container.add(breadcrumb);
        container.add(statusboard);
        container.add(business);
        container.add(switchboard);
        container.add(log);
    }

    /**
     * This method is used to set the background color of the log component in the Dashboard.
     */
    private void buildLog() {

        log.setBackground(Color.getHSBColor(0.00f, 0.00f, DOT_TWENTY_SEVEN));
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
        addLabelAndComponent(new JLabel(l(SECONDS, true)), new JLabel());
    }

    /**
     * Builds the switchboard component of the dashboard.
     * The switchboard is built by creating JBotButton objects
     * for each desired button and adding them to the switchboard.
     */
    @SuppressWarnings("NestedMethodCall")
    private void buildSwitchBoard() {

        debug = new JBotButton(b(c(DEBUG), OUTPUT));
        pause = new JBotButton(b(c(PAUSE), CURRENT, c(TEST), EXECUTION));
        halt = new JBotButton(b(c(HALT), c(TEST), AFTER, CURRENT, c(SCENARIO)));
        stop = new JBotButton(b(c(STOP), TESTING, AFTER, CURRENT, c(TEST)));
        switchboard.add(debug);
        switchboard.add(pause);
        switchboard.add(halt);
        switchboard.add(stop);
    }

    /**
     * Centers the window on the specified screen.
     *
     * @param screen The index of the screen to center the window on.
     */
    @SuppressWarnings({"SameParameterValue", "NestedMethodCall"})
    private void centerThisWindow(final int screen) {

        final GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        final int i = (screen >= screens.length || 0 > screen) ? 0 : screen;
        final Rectangle r = screens[i].getDefaultConfiguration().getBounds();
        setLocation(((r.width - getWidth()) / 2) + r.x, ((r.height - getHeight()) / 2) + r.y);
    }

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
}
