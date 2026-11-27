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

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.*;

/**
 * Manages storage and retrieval of known issues against scenarios and execution results.
 */
@SuppressWarnings("all")
public final class KnownIssuesStore {

    private static KnownIssuesStore globalStore = null;
    private static KnownIssuesStore fixedGlobalStore = null;

    public static void setGlobalStore(final KnownIssuesStore store) {
        globalStore = store;
    }

    public static KnownIssuesStore getGlobalStore() {
        if (null == globalStore) {
            globalStore = new KnownIssuesStore();
        }
        return globalStore;
    }

    public static void setFixedGlobalStore(final KnownIssuesStore store) {
        fixedGlobalStore = store;
    }

    public static KnownIssuesStore getFixedGlobalStore() {
        if (null == fixedGlobalStore) {
            fixedGlobalStore = new KnownIssuesStore(KnownIssuesLoader.loadDefaultFixed());
        }
        return fixedGlobalStore;
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
                final KnownIssue ki = candidates.get(i);
                if (!KnownIssuesMatcher.isCatchAllRule(ki.scenario(), ki.pattern())) {
                    unmatched++;
                }
            }
            this.remainingUnmatched = unmatched;
            this.cachedMatches = Collections.emptyList();
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
        final String key = s(null != fqn ? fqn : "", '\0', null != simpleName ? simpleName : "");
        return scenarioIssuesCache.computeIfAbsent(key, _ -> {
            final List<KnownIssue> matched = new ArrayList<>(4);
            for (final KnownIssue ki : issues) {
                if (!KnownIssuesMatcher.isCatchAllRule(ki.scenario(), ki.pattern())
                        && KnownIssuesMatcher.matchesScenario(ki.scenario(), fqn, simpleName)) {
                    matched.add(ki);
                }
            }
            return matched.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(matched);
        });
    }

    /**
     * Finds known issues that match the given scenario and execution result events.
     * Never matches scenarios with no events or rules with blank/catch-all patterns.
     * <p>
     * Uses per-{@link _Result} incremental scan cursors keyed weakly by result instance so repeated
     * lookups across tree layout frames scale with newly appended events rather than total run events.
     *
     * @param scenario the scenario to check (can be null)
     * @param result   the execution result to check for log patterns (can be null)
     * @return list of matching known issues
     */
    public List<KnownIssue> getForNode(final _Scenario scenario, final _Result result) {

        if (null == scenario || null == result) {
            return new ArrayList<>(0);
        }
        final List<_LogRecord> events = result.getEvents();
        final int eventCount = (null != events) ? events.size() : 0;
        if (0 == eventCount) {
            return new ArrayList<>(0);
        }
        final String fqn = scenario.getName();
        final String simpleName = scenario.getSimpleName();
        final List<KnownIssue> candidates = getForScenario(fqn, simpleName);
        if (candidates.isEmpty()) {
            return new ArrayList<>(0);
        }

        final String scenarioKey = s(null != fqn ? fqn : "", '\0', null != simpleName ? simpleName : "");
        final _LogRecord firstRecord = events.getFirst();

        synchronized (nodeCursorLock) {
            final Map<String, NodeScanCursor> perScenario = nodeScanCursors.computeIfAbsent(result, _ -> new HashMap<>(2));
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
                        final KnownIssue ki = candidates.get(c);
                        if (!cursor.matched[c]
                                && !KnownIssuesMatcher.isCatchAllRule(ki.scenario(), ki.pattern())
                                && KnownIssuesMatcher.matchesPatternForRecord(ki.pattern(), lr)) {
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
     * Finds known issues matching a log record using its embedded runtime provenance
     * ({@link _LogRecord#scenario()} when emitted inside a scenario, or {@link _LogRecord#context()}
     * when emitted inside a lifecycle phase such as {@code "Pretest"}).
     *
     * @param logRecord the log record to check (can be null)
     * @return list of matching known issues
     */
    public List<KnownIssue> getForLogRecord(final _LogRecord logRecord) {

        if (null == logRecord) {
            return Collections.emptyList();
        }
        if (null != logRecord.scenario()) {
            return getForLogRecord(logRecord.scenario(), logRecord);
        }
        return getForContext(logRecord.context(), logRecord);
    }

    /**
     * Finds known issues matching a non-scenario execution context (such as {@code "Pretest"}, {@code "Preflight"},
     * or {@code "Postflight"}) and a specific log record. Global {@code "*"} rules with specific patterns also match.
     *
     * @param contextName lifecycle phase or context name (may be null)
     * @param logRecord   the log record to check (can be null)
     * @return list of matching known issues
     */
    public List<KnownIssue> getForContext(final String contextName, final _LogRecord logRecord) {

        final List<KnownIssue> matches = new ArrayList<>(4);
        if (null == logRecord) {
            return matches;
        }
        final boolean hasContext = null != contextName && !contextName.isBlank();
        final String trimmedContext = hasContext ? contextName.trim() : null;
        final String simpleContext = hasContext ? extractSimpleName(trimmedContext) : null;
        for (final KnownIssue ki : issues) {
            if (KnownIssuesMatcher.isCatchAllRule(ki.scenario(), ki.pattern())) {
                continue;
            }
            if (!KnownIssuesMatcher.matchesPatternForRecord(ki.pattern(), logRecord)) {
                continue;
            }
            final String scenarioTarget = ki.scenario();
            if (KnownIssuesMatcher.isWildcardScenario(scenarioTarget)) {
                matches.add(ki);
            } else if (hasContext && KnownIssuesMatcher.matchesScenario(scenarioTarget, trimmedContext, simpleContext)) {
                matches.add(ki);
            }
        }
        return matches;
    }

    /**
     * Finds known issues that match the given scenario and specific log record.
     * If {@code scenario} is {@code null}, falls back to the record's provenance ({@link _LogRecord#scenario()} or
     * {@link _LogRecord#context()}).
     *
     * @param scenario  the scenario to check (can be null)
     * @param logRecord the log record to check for log patterns (can be null)
     * @return list of matching known issues
     */
    public List<KnownIssue> getForLogRecord(final _Scenario scenario, final _LogRecord logRecord) {

        if (null == logRecord) {
            return new ArrayList<>(0);
        }
        final _Scenario effectiveScenario = (null != scenario) ? scenario : logRecord.scenario();
        if (null != effectiveScenario) {
            return getForLogRecord(Collections.singletonList(effectiveScenario), logRecord);
        }
        return getForContext(logRecord.context(), logRecord);
    }

    /**
     * Finds known issues that match any of the given scenarios and specific log record.
     * When {@code scenarios} is empty/null, delegates to the record's own provenance context.
     *
     * @param scenarios collection of scenarios (can be null or empty)
     * @param logRecord the log record to check for log patterns (can be null)
     * @return list of matching known issues
     */
    public List<KnownIssue> getForLogRecord(final Collection<? extends _Scenario> scenarios, final _LogRecord logRecord) {

        final List<KnownIssue> matches = new ArrayList<>(4);
        if (null == logRecord) {
            return matches;
        }
        final boolean hasScenarios = null != scenarios && !scenarios.isEmpty();
        final String ctx = !hasScenarios ? logRecord.context() : null;
        final boolean hasContext = null != ctx && !ctx.isBlank();
        final String trimmedContext = hasContext ? ctx.trim() : null;
        final String simpleContext = hasContext ? extractSimpleName(trimmedContext) : null;

        for (final KnownIssue ki : issues) {
            if (KnownIssuesMatcher.isCatchAllRule(ki.scenario(), ki.pattern())) {
                continue;
            }
            if (!KnownIssuesMatcher.matchesPatternForRecord(ki.pattern(), logRecord)) {
                continue;
            }
            final String scenarioTarget = ki.scenario();
            if (KnownIssuesMatcher.isWildcardScenario(scenarioTarget)) {
                matches.add(ki);
            } else if (hasScenarios) {
                if (KnownIssuesMatcher.matchesAnyScenario(scenarioTarget, scenarios)) {
                    matches.add(ki);
                }
            } else if (hasContext && KnownIssuesMatcher.matchesScenario(scenarioTarget, trimmedContext, simpleContext)) {
                matches.add(ki);
            }
        }
        return matches;
    }

    private static String extractSimpleName(final String name) {
        final int lastDot = name.lastIndexOf('.');
        return (lastDot >= 0 && lastDot + 1 < name.length()) ? name.substring(lastDot + 1) : name;
    }
}
