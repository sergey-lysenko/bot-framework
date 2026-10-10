package works.lysenko.base.issues;

import works.lysenko.util.apis.data.*;
import works.lysenko.util.apis.log.*;
import works.lysenko.util.apis.scenario.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static works.lysenko.util.func.type.Objects.*;

/**
 * Manages storage and retrieval of known issues against scenarios and execution results.
 */
@SuppressWarnings("all")
public final class KnownIssuesStore {

    private final List<KnownIssue> issues = new ArrayList<>(16);

    /**
     * Instantiates a new Known issues store with default resources.
     */
    public KnownIssuesStore() {

        this.issues.addAll(KnownIssuesLoader.loadDefault());
    }

    /**
     * Instantiates a new Known issues store from an input stream.
     *
     * @param inputStream the input stream to load from
     */
    public KnownIssuesStore(final InputStream inputStream) {

        if (isNotNull(inputStream)) {
            this.issues.addAll(KnownIssuesLoader.loadFromStream(inputStream));
        }
    }

    /**
     * Instantiates a new Known issues store from a collection of issues.
     *
     * @param issues the collection of known issues
     */
    public KnownIssuesStore(final Collection<KnownIssue> issues) {

        if (isNotNull(issues)) {
            this.issues.addAll(issues);
        }
    }

    /**
     * Gets the list of known issues.
     *
     * @return the unmodifiable list of known issues
     */
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

        final List<KnownIssue> matches = new ArrayList<>(10);
        if (null != scenario) {
            final String fqn = scenario.getName();
            final String simpleName = scenario.getSimpleName();

            for (final KnownIssue ki : issues) {
                if (KnownIssuesMatcher.matchesScenario(ki.scenario(), fqn, simpleName)) {
                    if (KnownIssuesMatcher.matchesPattern(ki.pattern(), result)) matches.add(ki);
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

        return getForLogRecord(null != scenario ? Collections.singletonList(scenario) : Collections.emptyList(), logRecord);
    }

    /**
     * Finds known issues that match any of the given scenarios and specific log record.
     *
     * @param scenarios collection of scenarios in the run (can be null or empty)
     * @param logRecord the log record to check for log patterns (can be null)
     * @return list of matching known issues
     */
    public List<KnownIssue> getForLogRecord(final Collection<? extends _Scenario> scenarios, final _LogRecord logRecord) {

        final List<KnownIssue> matches = new ArrayList<>(10);
        if (null != logRecord) {
            for (final KnownIssue ki : issues) {
                if (KnownIssuesMatcher.matchesPatternForRecord(ki.pattern(), logRecord)) {
                    final String scenarioTarget = ki.scenario();
                    if (null == scenarioTarget || scenarioTarget.isBlank()) matches.add(ki);
                    else if (null != scenarios && !scenarios.isEmpty()) {
                        if (KnownIssuesMatcher.matchesAnyScenario(scenarioTarget, scenarios)) matches.add(ki);
                        else if (KnownIssuesMatcher.matchesScenarioInText(scenarioTarget, logRecord.text())) matches.add(ki);
                    } else if (KnownIssuesMatcher.matchesScenarioInText(scenarioTarget, logRecord.text())) matches.add(ki);
                }
            }
        }
        return matches;
    }
}
