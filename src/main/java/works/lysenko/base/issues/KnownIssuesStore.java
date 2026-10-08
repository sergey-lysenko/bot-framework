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
        try (final InputStream is = KnownIssuesStore.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (isNotNull(is)) {
                loadFromStream(is);
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

    public static boolean matchesScenario(final String target, final String fqn, final String simpleName) {
        if (null == target || target.isBlank()) return false;
        final String trimmed = target.trim();
        if (trimmed.equals(fqn) || trimmed.equals(simpleName)) return true;

        if (trimmed.contains("*")) {
            final String regex = "^" + trimmed.replace(".", "\\.").replace("*", ".*") + "$";
            if (isNotNull(fqn) && fqn.matches(regex)) return true;
            if (isNotNull(simpleName) && simpleName.matches(regex)) return true;
        }
        return false;
    }

    private static boolean matchesPattern(final String patternStr, final _Result result) {
        if (null == patternStr || patternStr.isBlank()) return true;
        if (null == result || null == result.getEvents() || result.getEvents().isEmpty()) return false;

        try {
            final Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            for (final _LogRecord lr : result.getEvents()) {
                if (isNotNull(lr) && isNotNull(lr.text())) {
                    if (pattern.matcher(lr.text()).find()) return true;
                }
            }
        } catch (final Exception ignored) {
        }
        return false;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KnownIssuesContainer(List<KnownIssue> issues) {}
}
