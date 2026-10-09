package works.lysenko.base.issues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Manages loading and matching of known issues against scenarios and execution results.
 */
public class KnownIssuesStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_RESOURCE = "/known_issues.json";

    private final List<KnownIssue> issues = new ArrayList<>();

    public KnownIssuesStore() {
        loadDefault();
    }

    public KnownIssuesStore(final InputStream inputStream) {
        if (isNotNull(inputStream)) {
            loadFromStream(inputStream);
        }
    }

    public KnownIssuesStore(final List<KnownIssue> issues) {
        if (isNotNull(issues)) {
            this.issues.addAll(issues);
        }
    }

    private void loadDefault() {
        InputStream is = null;
        try {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (isNotNull(contextClassLoader)) {
                is = contextClassLoader.getResourceAsStream("known_issues.json");
            }
            if (null == is) {
                is = KnownIssuesStore.class.getResourceAsStream(DEFAULT_RESOURCE);
            }
            if (null == is) {
                final java.io.File f1 = new java.io.File("src/main/resources/known_issues.json");
                if (f1.exists()) {
                    is = new java.io.FileInputStream(f1);
                } else {
                    final java.io.File f2 = new java.io.File("known_issues.json");
                    if (f2.exists()) {
                        is = new java.io.FileInputStream(f2);
                    }
                }
            }
            if (isNotNull(is)) {
                try (final InputStream stream = is) {
                    loadFromStream(stream);
                }
            }
        } catch (final Exception ignored) {
        }
    }

    private void loadFromStream(final InputStream is) {
        try {
            final KnownIssuesContainer container = MAPPER.readValue(is, KnownIssuesContainer.class);
            if (isNotNull(container) && isNotNull(container.issues())) {
                issues.addAll(container.issues());
            }
        } catch (final Exception ignored) {
        }
    }

    public List<KnownIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    /**
     * Finds known issues that match the given scenario and optional execution result.
     *
     * @param scenario the scenario to check (can be null)
     * @param result   the execution result to check for log patterns (can be null)
     * @return list of matching known issues
     */
    public List<KnownIssue> getForNode(final _Scenario scenario, final _Result result) {
        final List<KnownIssue> matches = new ArrayList<>();
        if (null == scenario) return matches;

        final String fqn = scenario.getName();
        final String simpleName = scenario.getSimpleName();

        for (final KnownIssue ki : issues) {
            if (matchesScenario(ki.scenario(), fqn, simpleName)) {
                if (matchesPattern(ki.pattern(), result)) {
                    matches.add(ki);
                }
            }
        }
        return matches;
    }

    /**
     * Finds known issues that match the given scenario and specific log record.
     *
     * @param scenario  the scenario to check (can be null)
     * @param logRecord the log record to check for log patterns (can be null)
     * @return list of matching known issues
     */
    public List<KnownIssue> getForLogRecord(final _Scenario scenario, final _LogRecord logRecord) {
        return getForLogRecord(scenario != null ? Collections.singletonList(scenario) : Collections.emptyList(), logRecord);
    }

    /**
     * Finds known issues that match any of the given scenarios and specific log record.
     *
     * @param scenarios collection of scenarios in the run (can be null or empty)
     * @param logRecord the log record to check for log patterns (can be null)
     * @return list of matching known issues
     */
    public List<KnownIssue> getForLogRecord(final java.util.Collection<_Scenario> scenarios, final _LogRecord logRecord) {
        final List<KnownIssue> matches = new ArrayList<>();
        if (null == logRecord) return matches;

        for (final KnownIssue ki : issues) {
            if (matchesPatternForRecord(ki.pattern(), logRecord)) {
                if (null == ki.scenario() || ki.scenario().isBlank()) {
                    matches.add(ki);
                } else if (null != scenarios && !scenarios.isEmpty()) {
                    boolean found = false;
                    for (final _Scenario scenario : scenarios) {
                        if (null != scenario && matchesScenario(ki.scenario(), scenario.getName(), scenario.getSimpleName())) {
                            matches.add(ki);
                            found = true;
                            break;
                        }
                    }
                    if (!found && matchesScenarioInText(ki.scenario(), logRecord.text())) {
                        matches.add(ki);
                    }
                } else if (matchesScenarioInText(ki.scenario(), logRecord.text())) {
                    matches.add(ki);
                }
            }
        }
        return matches;
    }

    private static boolean matchesScenarioInText(final String target, final String text) {
        if (null == target || target.isBlank() || null == text || text.isBlank()) return false;
        final String trimmed = target.trim().toLowerCase(java.util.Locale.ROOT);
        final String lowerText = text.toLowerCase(java.util.Locale.ROOT);
        return lowerText.contains(trimmed);
    }

    public static boolean matchesScenario(final String target, final String fqn, final String simpleName) {
        if (null == target || target.isBlank()) return false;
        final String trimmed = target.trim();
        if (trimmed.equals(fqn) || trimmed.equals(simpleName)) return true;
        if (isNotNull(fqn) && (fqn.endsWith("." + trimmed) || fqn.endsWith(trimmed))) return true;

        if (trimmed.contains("*")) {
            String patternStr = trimmed.replace(".", "\\.").replace("*", ".*");
            if (!patternStr.startsWith(".*")) {
                patternStr = ".*" + patternStr;
            }
            final String regex = "^" + patternStr + "$";
            if (isNotNull(fqn) && fqn.matches(regex)) return true;
            if (isNotNull(simpleName) && simpleName.matches(regex)) return true;
        }
        return false;
    }

    private static boolean matchesPattern(final String patternStr, final _Result result) {
        if (null == patternStr || patternStr.isBlank()) return true;
        if (null == result || null == result.getEvents() || result.getEvents().isEmpty()) return false;

        for (final _LogRecord lr : result.getEvents()) {
            if (matchesPatternForRecord(patternStr, lr)) return true;
        }
        return false;
    }

    private static boolean matchesPatternForRecord(final String patternStr, final _LogRecord logRecord) {
        if (null == patternStr || patternStr.isBlank()) return true;
        if (null == logRecord || null == logRecord.text()) return false;

        final String text = logRecord.text();

        // 1. Direct Regex match
        try {
            final Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            if (pattern.matcher(text).find()) {
                return true;
            }
        } catch (final Exception ignored) {
        }

        // 2. Direct Substring match (case-insensitive)
        final String lowerText = text.toLowerCase(java.util.Locale.ROOT);
        final String lowerPattern = patternStr.toLowerCase(java.util.Locale.ROOT);
        if (lowerText.contains(lowerPattern)) {
            return true;
        }

        // 3. Normalized alphanumeric tolerance match
        final String normText = lowerText.replaceAll("[^a-z0-9]", "");
        final String normPattern = lowerPattern.replaceAll("[^a-z0-9]", "");
        return !normPattern.isEmpty() && normText.contains(normPattern);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KnownIssuesContainer(List<KnownIssue> issues) {}
}
