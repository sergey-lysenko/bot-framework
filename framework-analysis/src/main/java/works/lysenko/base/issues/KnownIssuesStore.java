package works.lysenko.base.issues;

import works.lysenko.util.apis.data.*;
import works.lysenko.util.apis.log.*;
import works.lysenko.util.apis.scenario.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import static works.lysenko.util.func.type.Objects.*;

/**
 * Manages storage and retrieval of known issues against scenarios and execution results.
 */
@SuppressWarnings("all")
public final class KnownIssuesStore {

    private static KnownIssuesStore globalStore = null;

    public static void setGlobalStore(final KnownIssuesStore store) {
        globalStore = store;
    }

    public static KnownIssuesStore getGlobalStore() {
        if (null == globalStore) {
            globalStore = new KnownIssuesStore();
        }
        return globalStore;
    }

    private static final class NodeScanCursor {
        final List<KnownIssue> candidates;
        final boolean[] matched;
        int remainingUnmatched;
        int scannedUpTo;
        _LogRecord firstRecord;
        _LogRecord lastRecord;
        List<KnownIssue> cachedMatches;

        NodeScanCursor(final List<KnownIssue> candidates) {
            this.candidates = candidates;
            this.matched = new boolean[candidates.size()];
            int unmatched = 0;
            for (int i = 0; i < candidates.size(); i++) {
                final String pattern = candidates.get(i).pattern();
                if (null == pattern || pattern.isBlank()) {
                    this.matched[i] = true;
                } else {
                    unmatched++;
                }
            }
            this.remainingUnmatched = unmatched;
            this.cachedMatches = buildMatches();
        }

        List<KnownIssue> buildMatches() {
            final List<KnownIssue> result = new ArrayList<>(candidates.size());
            for (int i = 0; i < candidates.size(); i++) {
                if (matched[i]) {
                    result.add(candidates.get(i));
                }
            }
            return result.isEmpty() ? Collections.emptyList() : result;
        }
    }

    private final List<KnownIssue> issues = new ArrayList<>(16);
    private final Map<String, List<KnownIssue>> scenarioIssuesCache = new ConcurrentHashMap<>();
    private final Object nodeCursorLock = new Object();
    private final Map<_Result, Map<String, NodeScanCursor>> nodeScanCursors = new WeakHashMap<>();

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
     * Returns all known issues whose scenario target matches the given scenario FQN or simple name.
     * Results are cached per store instance since the underlying issue list is immutable after construction.
     *
     * @param fqn        fully qualified scenario name (may be null)
     * @param simpleName simple scenario name (may be null)
     * @return unmodifiable list of matching known issues
     */
    public List<KnownIssue> getForScenario(final String fqn, final String simpleName) {

        if (issues.isEmpty() || (null == fqn && null == simpleName)) {
            return Collections.emptyList();
        }
        final String key = (null != fqn ? fqn : "") + '\0' + (null != simpleName ? simpleName : "");
        return scenarioIssuesCache.computeIfAbsent(key, k -> {
            final List<KnownIssue> matched = new ArrayList<>(4);
            for (final KnownIssue ki : issues) {
                if (KnownIssuesMatcher.matchesScenario(ki.scenario(), fqn, simpleName)) {
                    matched.add(ki);
                }
            }
            return matched.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(matched);
        });
    }

    /**
     * Finds known issues that match the given scenario and optional execution result.
     * <p>
     * Uses per-{@link _Result} incremental scan cursors keyed weakly by result instance so repeated
     * lookups across tree layout frames scale with newly appended events rather than total run events.
     *
     * @param scenario the scenario to check (can be null)
     * @param result   the execution result to check for log patterns (can be null)
     * @return list of matching known issues
     */
    public List<KnownIssue> getForNode(final _Scenario scenario, final _Result result) {

        if (null == scenario) {
            return new ArrayList<>(0);
        }
        final String fqn = scenario.getName();
        final String simpleName = scenario.getSimpleName();
        final List<KnownIssue> candidates = getForScenario(fqn, simpleName);
        if (candidates.isEmpty()) {
            return new ArrayList<>(0);
        }
        if (null == result) {
            final List<KnownIssue> matches = new ArrayList<>(candidates.size());
            for (final KnownIssue ki : candidates) {
                if (null == ki.pattern() || ki.pattern().isBlank()) {
                    matches.add(ki);
                }
            }
            return matches;
        }
        final List<_LogRecord> events = result.getEvents();
        final int eventCount = (null != events) ? events.size() : 0;
        if (0 == eventCount) {
            final List<KnownIssue> matches = new ArrayList<>(candidates.size());
            for (final KnownIssue ki : candidates) {
                if (null == ki.pattern() || ki.pattern().isBlank()) {
                    matches.add(ki);
                }
            }
            return matches;
        }

        final String scenarioKey = (null != fqn ? fqn : "") + '\0' + (null != simpleName ? simpleName : "");
        final _LogRecord firstRecord = events.get(0);

        synchronized (nodeCursorLock) {
            final Map<String, NodeScanCursor> perScenario = nodeScanCursors.computeIfAbsent(result, r -> new HashMap<>(2));
            NodeScanCursor cursor = perScenario.get(scenarioKey);
            final boolean cursorValid = null != cursor
                    && cursor.candidates == candidates
                    && cursor.scannedUpTo <= eventCount
                    && (0 == cursor.scannedUpTo
                    || (cursor.firstRecord == firstRecord && events.get(cursor.scannedUpTo - 1) == cursor.lastRecord));
            if (!cursorValid) {
                cursor = new NodeScanCursor(candidates);
                perScenario.put(scenarioKey, cursor);
            }

            if (cursor.scannedUpTo < eventCount && cursor.remainingUnmatched > 0) {
                boolean changed = false;
                for (int i = cursor.scannedUpTo; i < eventCount; i++) {
                    final _LogRecord lr = events.get(i);
                    for (int c = 0; c < candidates.size(); c++) {
                        if (!cursor.matched[c] && KnownIssuesMatcher.matchesPatternForRecord(candidates.get(c).pattern(), lr)) {
                            cursor.matched[c] = true;
                            cursor.remainingUnmatched--;
                            changed = true;
                        }
                    }
                    if (0 == cursor.remainingUnmatched) {
                        break;
                    }
                }
                cursor.scannedUpTo = eventCount;
                cursor.firstRecord = firstRecord;
                cursor.lastRecord = events.get(eventCount - 1);
                if (changed) {
                    cursor.cachedMatches = cursor.buildMatches();
                }
            } else if (cursor.scannedUpTo < eventCount) {
                cursor.scannedUpTo = eventCount;
                cursor.firstRecord = firstRecord;
                cursor.lastRecord = events.get(eventCount - 1);
            }

            return new ArrayList<>(cursor.cachedMatches);
        }
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
                    } else if (KnownIssuesMatcher.matchesScenarioInText(scenarioTarget, logRecord.text())) matches.add(ki);
                }
            }
        }
        return matches;
    }
}
