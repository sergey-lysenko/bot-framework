package works.lysenko.base.ui;

import works.lysenko.base.util.Telemetry;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.util._Dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static works.lysenko.util.chrs.____.HALT;
import static works.lysenko.util.chrs.____.STOP;
import static works.lysenko.util.chrs.____.TRUE;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Files.deleteFile;
import static works.lysenko.util.func.type.Files.isFilePresent;
import static works.lysenko.util.func.type.Files.writeToFile;
import static works.lysenko.util.lang.word.D.DEBUG;
import static works.lysenko.util.lang.word.P.PAUSE;
import static works.lysenko.util.lang.word.T.TITLE;
import static works.lysenko.util.spec.Layout.Paths._TARGET_DASH_;
import static works.lysenko.util.spec.Symbols.QUS_MRK;

/**
 * The Directory class represents a dashboard in a directory.
 * It implements the ProvidesDashboard interface and provides methods to interact with the directory.
 */
@SuppressWarnings({"ClassWithoutLogger", "FinalClass", "NestedMethodCall", "ClassWithTooManyDependencies",
        "ClassWithTooManyTransitiveDependencies", "ClassWithTooManyTransitiveDependents", "CyclicClassDependency",
        "AutoBoxing",
        "ClassUnconnectedToPackage", "MismatchedQueryAndUpdateOfCollection", "BooleanParameter"})
public final class Directory implements _Dashboard {

    private static final String TITLE_PATH = s(_TARGET_DASH_, TITLE);
    private static final String DEBUG_PATH = s(_TARGET_DASH_, DEBUG);
    private static final String HALT_PATH = s(_TARGET_DASH_, HALT);
    private static final String STOP_PATH = s(_TARGET_DASH_, STOP);
    private static final String PAUSE_PATH = s(_TARGET_DASH_, PAUSE);

    /**
     * Instantiates a new Directory object.
     */
    private Directory() {

        setTitle(initialization);
    }

    /**
     * Instantiates a new Directory object.
     *
     * @param isDebug a boolean flag indicating if the debug mode is enabled
     */
    public Directory(final boolean isDebug) {

        this();
        if (isDebug) writeToFile(TRUE, DEBUG_PATH);
    }

    /**
     * Creates a label for a given scenario.
     *
     * @param scenario The scenario for which a label is to be created.
     * @return A1 label for the given scenario.
     */
    private static String createScenarioString(final _Scenario scenario) {

        return (null == scenario) ? s(QUS_MRK) : scenario.getSimpleName();
    }

    private static void setButton(final String path, final boolean active) {

        if (active) writeToFile(TRUE, path);
        else deleteFile(path);
    }

    @Override
    public void dispose() {

    }

    /**
     * Checks if the debug mode is enabled.
     *
     * @return true if the debug mode is enabled, false otherwise
     */
    @Override
    public boolean isDebug() {

        return isFilePresent(DEBUG_PATH);
    }

    @Override
    public boolean isHalt() {

        return isFilePresent(HALT_PATH);
    }

    @Override
    public void setHalt(final boolean active) {

        setButton(HALT_PATH, active);
    }

    @Override
    public boolean isPause() {

        return isFilePresent(PAUSE_PATH);
    }

    public boolean isStop() {

        return isFilePresent(STOP_PATH);
    }

    @Override
    public void setStop(final boolean active) {

        setButton(STOP_PATH, active);
    }

    @SuppressWarnings("ForeachStatement")
    public void setBreadcrumb(final List<? extends _Scenario> content) {

        final Collection<String> breadcrumb = new ArrayList<>(0);
        for (final _Scenario scenario : content)
            breadcrumb.add(createScenarioString(scenario));
    }

    /**
     * Sets the information message and refreshes the status board with the provided telemetry data.
     *
     * @param message   The information message to be displayed.
     * @param telemetry The telemetry data used to refresh the status board.
     */
    public void setInfo(final String message, final Telemetry telemetry) {
        // not implemented for console in order to conserve resources
    }

    @Override
    public void setTitle(final String title) {

        writeToFile(title, TITLE_PATH);
    }
}
