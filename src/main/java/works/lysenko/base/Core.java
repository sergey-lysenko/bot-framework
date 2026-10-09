package works.lysenko.base;

import org.openqa.selenium.WebDriver;
import works.lysenko.base.core.Routines;
import works.lysenko.base.output.PostProcessor;
import works.lysenko.base.output.ProgressionSettings;
import works.lysenko.base.output.RunLocks;
import java.awt.GraphicsEnvironment;
import java.io.File;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_HTML_;
import works.lysenko.base.ui.Directory;
import works.lysenko.base.ui.UserInterface;
import works.lysenko.tree.Root;
import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis._PropEnum;
import works.lysenko.util.apis.core._Core;
import works.lysenko.util.apis.core._Results;
import works.lysenko.util.apis.core._Tests;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.apis.util._Dashboard;
import works.lysenko.util.prop.core.Is;
import works.lysenko.util.prop.core.Start;
import works.lysenko.util.prop.logs.To;
import works.lysenko.util.spec.Level;

import javax.swing.SwingUtilities;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;
import static works.lysenko.Base.*;
import static works.lysenko.util.apis.test._Test.processCode;
import static works.lysenko.util.chrs.___.AND;
import static works.lysenko.util.chrs.___.LOG;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.ExitCode.EXECUTION_FAILURE;
import static works.lysenko.util.data.enums.ExitCode.SUCCESS;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Time.t;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.C.*;
import static works.lysenko.util.lang.word.D.DASHBOARD;
import static works.lysenko.util.lang.word.L.LOGGER;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.P.PROPERTIES;
import static works.lysenko.util.lang.word.R.READING;
import static works.lysenko.util.lang.word.R.RESULTS;
import static works.lysenko.util.lang.word.S.SPENT;
import static works.lysenko.util.lang.word.S.STARTING;
import static works.lysenko.util.lang.word.S.STOPWATCH;
import static works.lysenko.util.lang.word.T.TELEMETRY;
import static works.lysenko.util.lang.word.W.WRITER;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols.S;

/**
 * Represents the Core class, which is a crucial part in orchestrating and managing
 * test executions, configurations, and results.
 * This class integrates multiple subsystems, such as logging, dashboards, and test management,
 * and provides essential methods for managing the execution lifecycle.
 * <p>
 * Core extends the Root class and implements _Core and _Tests interfaces, encapsulating complex
 * logic to manage test scenarios, logs, dashboards, and test configurations.
 * <p>
 * Features:
 * - Provides access to test properties, results, and scenarios.
 * - Manages execution state, including stopping mechanisms and execution within a JAR context.
 * - Supports dashboard creation in both GUI and directory-based modes.
 * - Initialises and configures logging and test properties.
 */
@SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "UseOfSystemOutOrSystemErr", "MethodWithMultipleReturnPoints",
        "ClassWithTooManyMethods", "OverlyCoupledClass"})
public final class Core extends Root implements _Core, _Tests {

    private final _Test test;
    private final RunLocks.RunLock runLock;
    private final String parametersList;
    private _Results results = null;
    private _Logs logger = null;
    private _Dashboard dashboard = null;
    private long previousTickAt = System.currentTimeMillis();
    private Boolean inJar = null;
    private boolean stopFlag = false;
    private String stopReason = null;
    private _Scenario stopScenario = null;

    public boolean getStopFlag() {

        return stopFlag;
    }

    public void setStopFlag(final boolean stopFlag) {

        this.stopFlag = stopFlag;
        if (stopFlag && null == this.stopScenario && isNotNull(exec) && isNotNull(exec.scenarios())) {
            this.stopScenario = exec.scenarios().current();
        }
    }

    @Override
    public String getStopReason() {

        return stopReason;
    }

    @Override
    public void setStopReason(final String stopReason) {

        if (null == this.stopReason) {
            this.stopReason = stopReason;
            if (null == this.stopScenario && isNotNull(exec) && isNotNull(exec.scenarios())) {
                this.stopScenario = exec.scenarios().current();
            }
        }
    }

    @Override
    public _Scenario getStopScenario() {

        return stopScenario;
    }

    @Override
    public void setStopScenario(final _Scenario stopScenario) {

        if (null == this.stopScenario || null == stopScenario) {
            this.stopScenario = stopScenario;
        }
    }

    /**
     * Constructs a Core object with the provided parameters.
     *
     * @param test           The test manager responsible for handling execution tests.
     * @param additional     A1 collection of additional property enums that are used in the core configuration.
     * @param parametersList A1 string representing the list of parameters for initialising configurations.
     */
    public Core(final _Test test, final Collection<Class<? extends _PropEnum>> additional, final String parametersList) {

        runLock = RunLocks.acquireRunLock();
        this.test = test;
        this.parametersList = parametersList;

        try {
            Routines.starting();
            inJarOrNotInJar();
            create(additional);
            read();
            startStopwatch();
            build();
        } catch (final RuntimeException | Error e) {
            runLock.close();
            throw e;
        }
    }

    public int getActiveScenarioPaths() {

        if (isNotNull(test) && isNotNull(test.executor())) {
            if (isNotNull(test.executor().ctrl()))
                return test.executor().ctrl().getPathsCount(true);
        }
        return ZERO;
    }

    @Override
    public Set<_Scenario> getAccessibleLeafs() {

        if (isNotNull(test) && isNotNull(test.executor())) {
            if (isNotNull(test.executor().ctrl()))
                return test.executor().ctrl().getAccessibleLeafs();
        }
        return Set.of();
    }

    @Override
    public Set<_Scenario> getAccessibleNodes() {

        if (isNotNull(test) && isNotNull(test.executor())) {
            if (isNotNull(test.executor().ctrl()))
                return test.executor().ctrl().getAccessibleNodes();
        }
        return Set.of();
    }

    @Override
    public Set<_Scenario> getAccessibleScenarios() {

        if (isNotNull(test) && isNotNull(test.executor())) {
            if (isNotNull(test.executor().ctrl()))
                return test.executor().ctrl().getAccessibleScenarios();
        }
        return Set.of();
    }

    public static int resolveAllLeafsTarget() {
        if (isNotNull(parameters)) {
            return parameters.getAllLeafsCount();
        }
        final Integer prop = works.lysenko.util.spec.PropEnum._TEST_ALL_LEAFS_COUNT.get();
        return isNotNull(prop) ? Math.max(1, prop) : 1;
    }

    @Override
    public boolean areAllLeafsExecuted() {

        final Set<_Scenario> leafs = getAccessibleLeafs();
        if (leafs.isEmpty()) return true;
        final int target = resolveAllLeafsTarget();
        for (final _Scenario leaf : leafs) {
            if (!leaf.isExecutable()) continue;
            final int leafTarget = (leaf instanceof Mono) ? 1 : target;
            if (getResults().getExecutions(leaf) < leafTarget)
                return false;
        }
        return true;
    }

    @Override
    public int getExecutedLeafsCount() {

        final Set<_Scenario> leafs = getAccessibleLeafs();
        int count = 0;
        final int target = resolveAllLeafsTarget();
        for (final _Scenario leaf : leafs) {
            final int leafTarget = (leaf instanceof Mono) ? 1 : target;
            if (leafTarget <= getResults().getExecutions(leaf))
                count++;
        }
        return count;
    }

    public int getExecutedLeafExecutionsCount() {

        final Set<_Scenario> leafs = getAccessibleLeafs();
        int count = 0;
        final int target = resolveAllLeafsTarget();
        for (final _Scenario leaf : leafs) {
            final int leafTarget = (leaf instanceof Mono) ? 1 : target;
            count += Math.min(getResults().getExecutions(leaf), leafTarget);
        }
        return count;
    }

    public int getTotalLeafExecutionsCount() {

        final Set<_Scenario> leafs = getAccessibleLeafs();
        int count = 0;
        final int target = resolveAllLeafsTarget();
        for (final _Scenario leaf : leafs)
            count += (leaf instanceof Mono) ? 1 : target;
        return count;
    }

    public Integer getCurrentTestNumber() {

        if (isNull(test)) return null;
        return test.repeater().getCurrent();
    }

    @Override
    public int getTestsCount() {

        if (isNull(test) || isNull(test.repeater())) return ZERO;
        return test.repeater().getTestsCount();
    }

    public _Dashboard getDashboard() {

        return dashboard;
    }

    public Exception getException() {

        return test.handler().getException();
    }

    public _Logs getLogger() {

        return logger;
    }

    @Override
    public Integer getTotalTests() {

        return test.repeater().getTotalTests();
    }

    public _Results getResults() {

        return results;
    }

    public Set<_Scenario> getRootScenarios() {

        return test.executor().getRootScenariosList();
    }

    public String getStatus() {

        return test.getStatus();
    }

    public _Test getTest() {

        return test;
    }

    public List<List<String>> getTestsSummary() {

        return test.repeater().getSummary();
    }

    public int getTotalScenarioPaths() {

        if (isNotNull(test))
            if (isNotNull(test.executor()))
                if (isNotNull(test.executor().ctrl()))
                    return test.executor().ctrl().getPathsCount(false);
        return ZERO;
    }

    public boolean isInJar() {

        return inJar;
    }

    public boolean isStopRequestedByDashboard() {

        return test.isStopRequestedByDashboard();
    }

    private void build() {

        buildLogger();
        buildDashboard();
    }

    /**
     * Creates the dashboard for the program.
     */
    private void buildDashboard() {

        System.out.print(b(c(CREATING), c(DASHBOARD), Routines.DOTS));
        createDashboard0();
        tick();
    }

    /**
     * Creates a logger instance and initialises it with the given set of logs to read.
     */
    private void buildLogger() {

        System.out.print(b(c(CREATING), c(LOGGER), Routines.DOTS));
        logger = new Logs(To.read);
        tick();
    }

    /**
     * Closes the WebDriver if required.
     */
    private void closeWebDriverIfRequired() {

        final WebDriver wd = exec.wd();
        if (isNull(wd)) return;
        try {
            wd.quit();
        } catch (final RuntimeException e) {
            throw new IllegalStateException("Unable to close WebDriver due to an exception", e);
        } finally {
            exec.clearDriver();
        }
    }

    /**
     * Closes resources and handles the exit code based on conditions.
     */
    void conditionalClose() {
        try {
            logEmptyLine();
            log(Level.none, b(c(CLOSING), WebDriver.class.getSimpleName()), false);
            closeWebDriverIfRequired();
            log(Level.none, b(c(CLOSING), c(DASHBOARD)), false);
            disposeDashboardIfExist();
            final boolean isSuccess = results.getFailures().isEmpty();
            final boolean isHeadless = (isNotNull(parameters) && parameters.isHeadless()) || GraphicsEnvironment.isHeadless();
            final boolean shouldShowPostProcessorWindow =
                    !GraphicsEnvironment.isHeadless() && !Routines.isInsideCI() && !Routines.isInsideDocker();
            final boolean shouldOpenBrowser = shouldShowPostProcessorWindow && !isHeadless;
            final File logFile = new File(name(RUN_LOG_));
            final File htmlFile = new File(name(RUN_LOG_HTML_));
            log(Level.none, b(c(CLOSING), LOG, AND, TELEMETRY, s(WRITER, S)), false);
            Routines.closeQuietly(logger.getLogWriter(), "Unable to close log writer"); //NON-NLS
            Routines.closeQuietly(logger.getTelemetryWriter(), "Unable to close telemetry writer"); //NON-NLS
            PostProcessor.launchDetached(logFile, htmlFile, shouldOpenBrowser, shouldShowPostProcessorWindow);
            runLock.close();
            processCode(isSuccess ? SUCCESS : EXECUTION_FAILURE);
        } finally {
            runLock.close();
        }
    }

    private void create(final Collection<Class<? extends _PropEnum>> additional) {

        createTestResultsContainer();
        createTestProperties(additional);
    }

    /**
     * Creates a dashboard based on the execution environment.
     * If the execution is not inside a Docker container or CI environment,
     * it spawns a graphical user interface (GUI) dashboard using the provided screen number.
     * Otherwise, it creates a directory-based dashboard.
     */
    private void createDashboard0() {

        if (Routines.isInsideDocker() || Routines.isInsideCI()) {
            dashboard = new Directory(Is.debug);
        } else {
            dashboard = works.lysenko.base.ui.ControlPanel.getInstance();
        }
        if (isNotNull(dashboard)) dashboard.setDebug(Is.debug);
    }

    /**
     * Creates test properties by initialising the 'properties' field and printing a message to the console.
     */
    private void createTestProperties(final Collection<Class<? extends _PropEnum>> additional) {

        System.out.print(b(c(CREATING), c(TEST), PROPERTIES, Routines.DOTS));
        properties = new TestProperties(additional);
        tick();
    }

    /**
     * Creates a test results container.
     */
    private void createTestResultsContainer() {

        System.out.print(b(c(CREATING), c(TEST), c(RESULTS), CONTAINER, Routines.DOTS));
        results = new Results();
        tick();
    }

    /**
     * Disposes the dashboard if it exists.
     * <p>
     * This method checks if the dashboard object is not null, and if so, calls the dispose() method on it.
     * The dispose() method releases any resources or performs any clean-up operations associated with the dashboard object.
     * <p>
     * Note: This method is private and does not return any value.
     */
    private void disposeDashboardIfExist() {

        if (isNotNull(dashboard)) dashboard.dispose();
    }

    /**
     * Determines if the current execution is being performed in a JAR file or not.
     * <p>
     * This method checks the classpath and the location of the main class to determine if
     * the execution is inside a JAR file or not. The result is stored in the private field 'inJar'.
     * <p>
     * Note: This method is called internally and does not return any value.
     */
    @SuppressWarnings("AccessOfSystemProperties")
    private void inJarOrNotInJar() {

        final String classpath = System.getProperty("java.class.path");
        final String mainClassPath = Exec.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        inJar = mainClassPath.contains(classpath);
    }

    private void read() {

        readCommonConfiguration();
        /* The method calls to readParameters() now occurs after createTestProperties() and readCommonConfiguration().
        The rearrangement ensures that parameters and stopwatch are triggered after the test properties and common
        configuration are set up. */
        readParameters();
        readTestConfiguration();
    }

    /**
     * Reads the common configuration properties and performs necessary operations.
     */
    private void readCommonConfiguration() {

        System.out.println(b(c(READING), c(COMMON), c(CONFIGURATION), PROPERTIES, Routines.DOTS));
        System.out.println(gray(a(COMMON, properties.readCommonConfiguration())));
        tick();
    }

    /**
     * Reads the execution parameters and initialises the 'parameters' field.
     * This method prompts the user to enter the necessary parameters and
     * creates a new Parameters object based on the provided parametersList.
     */
    private void readParameters() {

        System.out.print(b(c(READING), c(PARAMETERS), Routines.DOTS));
        parameters = new Parameters(parametersList);
        tick();
    }

    /**
     * Reads the test configuration.
     */
    private void readTestConfiguration() {

        System.out.println((b(c(READING), yb(parameters.getTest()), c(TEST), c(PROPERTIES), Routines.DOTS)));
        properties.readTestConfiguration();
        ProgressionSettings.initialize();
        tick();
    }

    /**
     * Starts the stopwatch.
     */
    private void startStopwatch() {

        System.out.print(b(c(STARTING), c(STOPWATCH), Routines.DOTS));
        timer = new Stopwatch();
        tick();
    }

    /**
     * Executes the tick operation.
     * <p>
     * This method calculates the time elapsed since the previous tick operation,
     * formats it as a string, and stores it in the 'length' variable.
     * It then encloses the 'length' variable with optional spaces and stores the result in the 'str' variable.
     * </p>
     */
    private void tick() {

        final long now = System.currentTimeMillis();
        final long length = now - previousTickAt;
        previousTickAt = now;
        System.out.println(e(false, b(s(Routines.DOTS, SPENT), t(length)), true));
    }
}
