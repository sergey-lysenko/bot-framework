package works.lysenko.base.output;

import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.spec.Layout;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;

public final class SequenceTracker {

    private static final List<String> recordedSequence = Collections.synchronizedList(new ArrayList<>());
    private static List<String> loadedSequence = null;
    private static int sequencePointer = 0;
    private static boolean initialized = false;

    private SequenceTracker() {
    }

    /**
     * Initializes sequence reading from environment variable or system property "SEQUENCE".
     */
    public static synchronized void initIfNeeded() {
        if (initialized) return;
        initialized = true;

        String seqPath = System.getenv("SEQUENCE");
        if (seqPath == null || seqPath.isBlank()) {
            seqPath = System.getProperty("SEQUENCE");
        }
        if (seqPath == null || seqPath.isBlank()) {
            seqPath = System.getProperty("tree.sequence.file");
        }

        if (seqPath != null && !seqPath.isBlank()) {
            final File file = new File(seqPath.trim());
            if (file.exists() && file.isFile()) {
                try {
                    final List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                    final List<String> parsed = new ArrayList<>();
                    for (final String line : lines) {
                        final String trimmed = line.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                            parsed.add(trimmed);
                        }
                    }
                    loadedSequence = parsed;
                    sequencePointer = 0;
                    logDebug(String.format("Loaded scenario execution sequence script with %d scenarios from %s",
                            loadedSequence.size(), file.getAbsolutePath()));
                } catch (final Exception e) {
                    logEvent(S2, "Failed to load scenario sequence script from " + file.getAbsolutePath() + ": " + e.getMessage());
                }
            } else {
                logEvent(S2, "Configured scenario sequence script file does not exist: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Gets the canonical class key or name for a scenario (e.g. signIn.login.auditLogs.clearFilters.AllEmpty).
     */
    public static String getScenarioClassKey(final _Scenario scenario) {
        if (scenario == null) return "";
        final String fqn = scenario.getClass().getName();
        if (fqn.contains("$")) {
            return (scenario.getShortName() != null && !scenario.getShortName().isBlank()) ? scenario.getShortName() : scenario.getSimpleName();
        }
        String rootPackage = works.lysenko.util.prop.tree.Scenario.root;
        if (rootPackage == null || rootPackage.isBlank()) {
            rootPackage = works.lysenko.util.prop.PropEnum._TEST_ROOT.get();
        }
        if (rootPackage != null && !rootPackage.isBlank() && !rootPackage.equals(".") && fqn.startsWith(rootPackage + ".")) {
            return fqn.substring(rootPackage.length() + 1);
        }
        return fqn;
    }

    /**
     * Records a scenario execution name using canonical class key.
     */
    public static void recordScenarioExecution(final _Scenario scenario) {
        if (scenario == null) return;
        final String name = getScenarioClassKey(scenario);
        if (name != null && !name.isBlank()) {
            recordedSequence.add(name);
        }
    }

    /**
     * Writes recorded scenario sequence to {timestamp}.scenarios.sequence artifact file.
     */
    public static void writeSequenceFile() {
        if (recordedSequence.isEmpty()) return;
        try {
            final String filePath = Layout.Files.name(Layout.Templates.RUN_SEQUENCE_);
            final Path path = Path.of(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, recordedSequence, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logDebug(String.format("Saved scenario sequence artifact (%d steps) to %s",
                    recordedSequence.size(), path.toAbsolutePath()));
        } catch (final Exception e) {
            logDebug("Failed to save scenario sequence artifact: " + e.getMessage());
        }
    }

    public static boolean hasActiveScript() {
        initIfNeeded();
        return loadedSequence != null && sequencePointer < loadedSequence.size();
    }

    public static String peekNextScenario() {
        if (!hasActiveScript()) return null;
        return loadedSequence.get(sequencePointer);
    }

    public static void advanceSequence() {
        if (hasActiveScript()) {
            sequencePointer++;
        }
    }

    public static void reset() {
        recordedSequence.clear();
        loadedSequence = null;
        sequencePointer = 0;
        initialized = false;
    }
}
