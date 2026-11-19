package works.lysenko.base.ui;

import works.lysenko.base.util.Telemetry;
import works.lysenko.util.apis.core._EtaProvider;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.util._Dashboard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.Base.core;
import static works.lysenko.Base.timer;
import static works.lysenko.util.chrs.___.ETA;
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
import static works.lysenko.util.spec.Layout.Paths._DASH_;
import static works.lysenko.util.spec.Symbols.QUS_MRK;

/**
 * The Directory class represents a filesystem-backed dashboard in {@code dash/}.
 * It implements {@link _Dashboard} for CLI / headless execution, exposing stop/pause/halt/debug flags,
 * run title ({@code dash/title}), and throttled live ETA snapshots ({@code dash/eta}).
 */
@SuppressWarnings({"ClassWithoutLogger", "FinalClass", "NestedMethodCall", "ClassWithTooManyDependencies",
        "ClassWithTooManyTransitiveDependencies", "ClassWithTooManyTransitiveDependents", "CyclicClassDependency",
        "AutoBoxing",
        "ClassUnconnectedToPackage", "MismatchedQueryAndUpdateOfCollection", "BooleanParameter"})
public final class Directory implements _Dashboard {

    private static final String TITLE_PATH = s(_DASH_, TITLE);
    private static final String ETA_PATH = s(_DASH_, ETA);
    private static final String DEBUG_PATH = s(_DASH_, DEBUG);
    private static final String HALT_PATH = s(_DASH_, HALT);
    private static final String STOP_PATH = s(_DASH_, STOP);
    private static final String PAUSE_PATH = s(_DASH_, PAUSE);
    private static final long ETA_WRITE_INTERVAL_MS = 1000L;

    private volatile String currentTitle = "";
    private volatile long lastEtaWriteAtMs = 0L;

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
     * @return label for the given scenario.
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

        publishEta(true, true);
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
    public void setDebug(final boolean active) {

        setButton(DEBUG_PATH, active);
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

    @Override
    public void setPause(final boolean active) {

        setButton(PAUSE_PATH, active);
    }

    /**
     * Performs is stop.
     * @return the is stop result
     */
    public boolean isStop() {

        return isFilePresent(STOP_PATH);
    }

    @Override
    public void setStop(final boolean active) {

        setButton(STOP_PATH, active);
    }

    @SuppressWarnings("ForeachStatement")
    /**
     * Performs set breadcrumb.
     * @param content the content
     */
    public void setBreadcrumb(final List<? extends _Scenario> content) {

        final Collection<String> breadcrumb = new ArrayList<>(0);
        for (final _Scenario scenario : content)
            breadcrumb.add(createScenarioString(scenario));
    }

    /**
     * Refreshes the throttled {@code dash/eta} telemetry sidecar during CLI/headless execution.
     *
     * @param message   The information message to be displayed.
     * @param telemetry The telemetry data used to refresh the status board.
     */
    public void setInfo(final String message, final Telemetry telemetry) {

        publishEta(false, false);
    }

    @Override
    public void setTitle(final String title) {

        currentTitle = (null == title) ? "" : title;
        writeToFile(title, TITLE_PATH);
        publishEta(true, false);
    }

    private void publishEta(final boolean force, final boolean markDone) {

        if (!isNotNull(core) || !isNotNull(timer)) return;

        final long now = System.currentTimeMillis();
        if (!force && (now - lastEtaWriteAtMs) < ETA_WRITE_INTERVAL_MS) return;
        lastEtaWriteAtMs = now;

        final String json = _EtaProvider.resolveEtaJson(markDone, now, Math.max(0L, timer.msSinceStart()), currentTitle);
        if (null != json) {
            writeEtaJson(json);
        }
    }

    private static void writeEtaJson(final String json) {

        try {
            final Path target = Path.of(ETA_PATH);
            final Path parent = target.getParent();
            if (null != parent) {
                Files.createDirectories(parent);
            }
            final Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final IOException ignored) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException ignored) {
            // Never disrupt test execution if dash/eta cannot be written
        }
    }
}
