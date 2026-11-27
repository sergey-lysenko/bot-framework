package works.lysenko.base.issues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static works.lysenko.Base.logDebug;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.FIXED_ISSUES;
import static works.lysenko.util.spec.Layout.Files.FIXED_ISSUES_;
import static works.lysenko.util.spec.Layout.Files.KNOWN_ISSUES;
import static works.lysenko.util.spec.Layout.Files.KNOWN_ISSUES_;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_FIXED_ISSUES_;
import static works.lysenko.util.spec.Layout.Templates.RUN_KNOWN_ISSUES_;
import static works.lysenko.util.spec.Symbols._SLASH_;

/**
 * Utility class for loading known and fixed issues from configuration streams.
 */
@SuppressWarnings("all")
public final class KnownIssuesLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_RESOURCE = s(_SLASH_, KNOWN_ISSUES);
    private static final String DEFAULT_FIXED_RESOURCE = s(_SLASH_, FIXED_ISSUES);
    private static final String LEGACY_RESOURCE = "known/issues";
    private static final String LEGACY_RESOURCE_ALT = "/known/issues";
    private static final String LEGACY_FILE_PATH = "src/main/resources/known/issues";

    private KnownIssuesLoader() {
        // Utility class
    }

    /**
     * Finds the default configuration input stream.
     *
     * @return the input stream, or null if not found
     */
    public static InputStream findInputStream() {
        InputStream is = null;
        try {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (isNotNull(contextClassLoader)) {
                is = contextClassLoader.getResourceAsStream(KNOWN_ISSUES);
                if (null == is) is = contextClassLoader.getResourceAsStream(LEGACY_RESOURCE);
            }
            if (null == is) is = KnownIssuesLoader.class.getResourceAsStream(DEFAULT_RESOURCE);
            if (null == is) is = KnownIssuesLoader.class.getResourceAsStream(LEGACY_RESOURCE_ALT);
            if (null == is) {
                final java.io.File f1 = new java.io.File(KNOWN_ISSUES_);
                if (f1.exists()) is = new java.io.FileInputStream(f1);
                else {
                    final java.io.File f2 = new java.io.File(KNOWN_ISSUES);
                    if (f2.exists()) is = new java.io.FileInputStream(f2);
                    else {
                        final java.io.File f3 = new java.io.File(LEGACY_FILE_PATH);
                        if (f3.exists()) is = new java.io.FileInputStream(f3);
                        else {
                            final java.io.File f4 = new java.io.File(LEGACY_RESOURCE);
                            if (f4.exists()) is = new java.io.FileInputStream(f4);
                        }
                    }
                }
            }
        } catch (final SecurityException | IOException _) {
        }
        return is;
    }

    /**
     * Finds the default fixed issues configuration input stream ({@code known/fixed.issues.json}).
     *
     * @return the input stream, or null if not found
     */
    public static InputStream findFixedInputStream() {
        InputStream is = null;
        try {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (isNotNull(contextClassLoader)) {
                is = contextClassLoader.getResourceAsStream(FIXED_ISSUES);
            }
            if (null == is) is = KnownIssuesLoader.class.getResourceAsStream(DEFAULT_FIXED_RESOURCE);
            if (null == is) {
                final java.io.File f1 = new java.io.File(FIXED_ISSUES_);
                if (f1.exists()) is = new java.io.FileInputStream(f1);
                else {
                    final java.io.File f2 = new java.io.File(FIXED_ISSUES);
                    if (f2.exists()) is = new java.io.FileInputStream(f2);
                }
            }
        } catch (final SecurityException | IOException _) {
        }
        return is;
    }

    /**
     * Loads known issues from the default location.
     *
     * @return a list of known issues
     */
    public static List<KnownIssue> loadDefault() {
        List<KnownIssue> issues = Collections.emptyList();
        try (final InputStream is = findInputStream()) {
            if (isNotNull(is)) issues = loadFromStream(is);
        } catch (final IOException _) {
        }
        return issues;
    }

    /**
     * Loads fixed issues (regression guards) from the default {@code known/fixed.issues.json} location.
     * Every returned entry has {@code decreaseToWarning == false}.
     *
     * @return a list of fixed issues, or empty list if none found
     */
    public static List<KnownIssue> loadDefaultFixed() {
        List<KnownIssue> issues = Collections.emptyList();
        try (final InputStream is = findFixedInputStream()) {
            if (isNotNull(is)) issues = loadFixedFromStream(is);
        } catch (final IOException _) {
        }
        return issues;
    }

    /**
     * Loads fixed issues (regression guards) from an input stream, ensuring {@code decreaseToWarning == false}.
     *
     * @param is the input stream
     * @return a list of fixed issues
     */
    public static List<KnownIssue> loadFixedFromStream(final InputStream is) {
        final List<KnownIssue> loaded = loadFromStream(is);
        if (loaded.isEmpty()) return loaded;
        final List<KnownIssue> guards = new ArrayList<>(loaded.size());
        for (final KnownIssue ki : loaded) {
            guards.add(ki.asFixedGuard());
        }
        return guards;
    }

    /**
     * Saves the current known/issues.json content into a specific file.
     *
     * @param outputFile the file to save the dataset to
     */
    public static void saveRunKnownIssues(final java.io.File outputFile) {
        saveStreamToFile(findInputStream(), outputFile, "known issues");
    }

    /**
     * Saves the current known/issues.json content into the run result dataset as {runId}.known.issues.json.
     */
    public static void saveRunKnownIssues() {
        saveRunKnownIssues(new java.io.File(name(RUN_KNOWN_ISSUES_)));
    }

    /**
     * Saves the current known/fixed.issues.json content into a specific file.
     *
     * @param outputFile the file to save the dataset to
     */
    public static void saveRunFixedIssues(final java.io.File outputFile) {
        saveStreamToFile(findFixedInputStream(), outputFile, "fixed issues");
    }

    /**
     * Saves the current known/fixed.issues.json content into the run result dataset as {runId}.fixed.issues.json.
     */
    public static void saveRunFixedIssues() {
        saveRunFixedIssues(new java.io.File(name(RUN_FIXED_ISSUES_)));
    }

    private static void saveStreamToFile(final InputStream source, final java.io.File outputFile, final String label) {
        try (final InputStream is = source) {
            if (isNotNull(is)) {
                final byte[] bytes = is.readAllBytes();
                if (bytes.length > 0) {
                    final java.io.File parent = outputFile.getAbsoluteFile().getParentFile();
                    if (null != parent && !parent.exists()) {
                        parent.mkdirs();
                    }
                    java.nio.file.Files.write(outputFile.toPath(), bytes);
                    logDebug(s("Saved ", label, " dataset to ", outputFile.getAbsolutePath()));
                }
            }
        } catch (final Throwable e) {
            logDebug(sn("Unable to save run ", label, " dataset: ", e.getMessage()));
        }
    }

    /**
     * Loads known issues from an input stream.
     *
     * @param is the input stream
     * @return a list of known issues
     */
    public static List<KnownIssue> loadFromStream(final InputStream is) {
        final List<KnownIssue> issues = new ArrayList<>();
        if (null == is) return issues;
        final byte[] bytes;
        try {
            bytes = is.readAllBytes();
        } catch (final IOException e) {
            return issues;
        }

        String content = new String(bytes, StandardCharsets.UTF_8).trim();
        if (!content.isEmpty() && '\uFEFF' == content.charAt(0)) {
            content = content.substring(1).trim();
        }
        if (content.isEmpty()) return issues;

        if (content.startsWith("{") || content.startsWith("[")) {
            try {
                if (content.startsWith("[")) {
                    final KnownIssueRaw[] rawArray = MAPPER.readValue(bytes, KnownIssueRaw[].class);
                    if (isNotNull(rawArray)) {
                        for (final KnownIssueRaw raw : rawArray) {
                            addRawIssue(issues, raw);
                        }
                    }
                } else {
                    final KnownIssuesContainer container = MAPPER.readValue(bytes, KnownIssuesContainer.class);
                    if (isNotNull(container)) {
                        if (isNotNull(container.issues())) {
                            for (final KnownIssueRaw raw : container.issues()) {
                                addRawIssue(issues, raw);
                            }
                        }
                        if (isNotNull(container.scenarios())) {
                            for (final KnownScenarioRaw scRaw : container.scenarios()) {
                                final List<String> targetScenarios = collectScenarios(scRaw.scenario(), scRaw.scenarios());
                                if (isNotNull(scRaw.issues())) {
                                    for (final KnownIssueRaw rawIssue : scRaw.issues()) {
                                        final boolean decrease = null == rawIssue.decreaseToWarning() || rawIssue.decreaseToWarning();
                                        for (final String scStr : targetScenarios) {
                                            issues.add(new KnownIssue(
                                                    scStr,
                                                    rawIssue.title(),
                                                    rawIssue.description(),
                                                    rawIssue.link(),
                                                    rawIssue.pattern(),
                                                    decrease,
                                                    rawIssue.resolvedAt(),
                                                    rawIssue.verifiedInRun()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (final IOException e) {
                works.lysenko.Base.logEvent(works.lysenko.util.data.enums.Severity.S0, sn("JSON Parsing Error in issues.json: ", e.getMessage()));
                throw new RuntimeException("Failed to parse issues.json", e);
            }
            return issues;
        }

        // Fallback: parse as Properties / Key-Value lines
        try {
            final Properties props = new Properties();
            props.load(new ByteArrayInputStream(bytes));
            for (final String key : props.stringPropertyNames()) {
                final String val = props.getProperty(key);
                if (isNotNull(key) && !key.isBlank() && isNotNull(val) && !val.isBlank()) {
                    if ("XXX".equalsIgnoreCase(key.trim()) && "YYY".equalsIgnoreCase(val.trim())) {
                        continue; // Skip dummy placeholder entries
                    }
                    issues.add(new KnownIssue(key.trim(), key.trim(), val.trim(), "", val.trim(), true));
                }
            }
        } catch (final IOException _) {
        }

        return issues;
    }

    private static void addRawIssue(final List<KnownIssue> issues, final KnownIssueRaw raw) {
        if (isNotNull(raw)) {
            final List<String> targetScenarios = collectScenarios(raw.scenario(), raw.scenarios());
            final boolean decrease = null == raw.decreaseToWarning() || raw.decreaseToWarning();
            for (final String scStr : targetScenarios) {
                issues.add(new KnownIssue(
                        scStr,
                        raw.title(),
                        raw.description(),
                        raw.link(),
                        raw.pattern(),
                        decrease,
                        raw.resolvedAt(),
                        raw.verifiedInRun()));
            }
        }
    }

    private static List<String> collectScenarios(final String singleScenario, final List<String> scenarioList) {
        final List<String> result = new ArrayList<>();
        if (isNotNull(scenarioList) && !scenarioList.isEmpty()) {
            for (final String s : scenarioList) {
                if (isNotNull(s) && !s.isBlank()) {
                    result.add(s.trim());
                }
            }
        }
        if (result.isEmpty() && isNotNull(singleScenario) && !singleScenario.isBlank()) {
            result.add(singleScenario.trim());
        }
        if (result.isEmpty()) {
            result.add("");
        }
        return result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KnownIssueRaw(
            @JsonProperty("scenario") String scenario,
            @JsonProperty("scenarios") List<String> scenarios,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("link") String link,
            @JsonProperty("pattern") String pattern,
            @JsonProperty("decrease-to-warning") Boolean decreaseToWarning,
            @JsonProperty("resolvedAt") String resolvedAt,
            @JsonProperty("verifiedInRun") String verifiedInRun
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KnownScenarioRaw(
            @JsonProperty("scenario") String scenario,
            @JsonProperty("scenarios") List<String> scenarios,
            @JsonProperty("issues") List<KnownIssueRaw> issues
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KnownIssuesContainer(
            @JsonProperty("issues") List<KnownIssueRaw> issues,
            @JsonProperty("scenarios") List<KnownScenarioRaw> scenarios
    ) {}
}

