package works.lysenko.base.responses;

import works.lysenko.base.issues.KnownIssuesMatcher;
import works.lysenko.util.apis.scenario._TreeNodeView;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store for managing expected browser responses during test execution.
 */
public class ExpectedResponsesStore {

    private static final String NULL_SCENARIO_KEY = "\0NULL";

    private static ExpectedResponsesStore globalStore = null;

    public static void setGlobalStore(final ExpectedResponsesStore store) {
        globalStore = store;
    }

    public static ExpectedResponsesStore getGlobalStore() {
        if (null == globalStore) {
            globalStore = new ExpectedResponsesStore();
        }
        return globalStore;
    }

    private static final class NodeMatchCursor {
        final List<ExpectedResponse> candidates;
        int scannedUpTo;
        _LogRecord firstRecord;
        _LogRecord lastRecord;
        boolean matched;

        NodeMatchCursor(final List<ExpectedResponse> candidates) {
            this.candidates = candidates;
        }
    }

    private final List<ExpectedResponse> responses;
    private final Map<String, List<ExpectedResponse>> scenarioNameCache = new ConcurrentHashMap<>();
    private final Map<String, List<ExpectedResponse>> scenarioCache = new ConcurrentHashMap<>();
    private final Map<String, List<ExpectedResponse>> nodeViewCache = new ConcurrentHashMap<>();
    private final Object nodeCursorLock = new Object();
    private final Map<_Result, Map<String, NodeMatchCursor>> nodeMatchCursors = new WeakHashMap<>();

    public ExpectedResponsesStore() {
        final List<ExpectedResponse> loaded = ExpectedResponsesLoader.loadDefault();
        this.responses = (null != loaded) ? new ArrayList<>(loaded) : new ArrayList<>(0);
    }

    public ExpectedResponsesStore(final List<ExpectedResponse> responses) {
        this.responses = (null != responses) ? new ArrayList<>(responses) : new ArrayList<>(0);
    }

    public List<ExpectedResponse> getResponses() {
        return responses;
    }

    private List<ExpectedResponse> getForScenarioName(final String scenarioName) {
        if (responses.isEmpty()) {
            return Collections.emptyList();
        }
        final String key = (null != scenarioName) ? scenarioName : NULL_SCENARIO_KEY;
        return scenarioNameCache.computeIfAbsent(key, k -> {
            final List<ExpectedResponse> matched = new ArrayList<>(4);
            for (final ExpectedResponse er : responses) {
                if (!KnownIssuesMatcher.isCatchAllRule(er.scenario(), er.pattern())
                        && ExpectedResponsesMatcher.matchesScenario(er.scenario(), scenarioName)) {
                    matched.add(er);
                }
            }
            return matched.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(matched);
        });
    }

    private static String scenarioKey(final _Scenario scenario) {
        final String fqn = scenario.getName();
        final String shortName = scenario.getShortName();
        final String simple = scenario.getSimpleName();
        return (null != fqn ? fqn : "") + '\0' + (null != shortName ? shortName : "") + '\0' + (null != simple ? simple : "");
    }

    private List<ExpectedResponse> getForScenario(final _Scenario scenario) {
        if (responses.isEmpty() || null == scenario) {
            return Collections.emptyList();
        }
        return scenarioCache.computeIfAbsent(scenarioKey(scenario), k -> {
            final List<ExpectedResponse> matched = new ArrayList<>(4);
            for (final ExpectedResponse er : responses) {
                if (!KnownIssuesMatcher.isCatchAllRule(er.scenario(), er.pattern())
                        && ExpectedResponsesMatcher.matchesScenario(er.scenario(), scenario)) {
                    matched.add(er);
                }
            }
            return matched.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(matched);
        });
    }

    private static String nodeViewKey(final _TreeNodeView node) {
        final String scen = (null != node.scenario()) ? scenarioKey(node.scenario()) : "";
        return scen + '\0' + (null != node.label() ? node.label() : "") + '\0' + (null != node.group() ? node.group() : "");
    }

    private List<ExpectedResponse> getForNodeView(final _TreeNodeView node) {
        if (responses.isEmpty() || null == node) {
            return Collections.emptyList();
        }
        return nodeViewCache.computeIfAbsent(nodeViewKey(node), k -> {
            final List<ExpectedResponse> matched = new ArrayList<>(4);
            for (final ExpectedResponse er : responses) {
                if (!KnownIssuesMatcher.isCatchAllRule(er.scenario(), er.pattern())
                        && ExpectedResponsesMatcher.matchesNodeScenario(er.scenario(), node)) {
                    matched.add(er);
                }
            }
            return matched.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(matched);
        });
    }

    private boolean matchesResultIncremental(
            final String cursorKey,
            final List<ExpectedResponse> candidates,
            final _Result result) {
        if (candidates.isEmpty() || null == result) {
            return false;
        }
        final List<_LogRecord> events = result.getEvents();
        final int eventCount = (null != events) ? events.size() : 0;
        if (0 == eventCount) {
            return false;
        }
        final _LogRecord firstRecord = events.get(0);
        synchronized (nodeCursorLock) {
            final Map<String, NodeMatchCursor> perKey = nodeMatchCursors.computeIfAbsent(result, r -> new HashMap<>(2));
            NodeMatchCursor cursor = perKey.get(cursorKey);
            final boolean cursorValid = null != cursor
                    && cursor.candidates == candidates
                    && cursor.scannedUpTo <= eventCount
                    && (0 == cursor.scannedUpTo
                    || (cursor.firstRecord == firstRecord && events.get(cursor.scannedUpTo - 1) == cursor.lastRecord));
            if (!cursorValid) {
                cursor = new NodeMatchCursor(candidates);
                perKey.put(cursorKey, cursor);
            }
            if (cursor.matched) {
                if (cursor.scannedUpTo < eventCount) {
                    cursor.scannedUpTo = eventCount;
                    cursor.firstRecord = firstRecord;
                    cursor.lastRecord = events.get(eventCount - 1);
                }
                return true;
            }
            if (cursor.scannedUpTo < eventCount) {
                for (int i = cursor.scannedUpTo; i < eventCount; i++) {
                    final _LogRecord lr = events.get(i);
                    for (int c = 0; c < candidates.size(); c++) {
                        if (ExpectedResponsesMatcher.matchesPatternForRecord(candidates.get(c).pattern(), lr)) {
                            cursor.matched = true;
                            break;
                        }
                    }
                    if (cursor.matched) {
                        break;
                    }
                }
                cursor.scannedUpTo = eventCount;
                cursor.firstRecord = firstRecord;
                cursor.lastRecord = events.get(eventCount - 1);
            }
            return cursor.matched;
        }
    }

    /**
     * Checks if a log record matches any expected response for a scenario or lifecycle phase name.
     *
     * @param scenarioName current scenario or lifecycle phase name (may be null for global {@code "*"} rules)
     * @param lr           log record
     * @return true if expected
     */
    public boolean isExpected(final String scenarioName, final _LogRecord lr) {
        if (responses.isEmpty() || null == lr) return false;
        final List<ExpectedResponse> candidates = getForScenarioName(scenarioName);
        for (int i = 0; i < candidates.size(); i++) {
            if (ExpectedResponsesMatcher.matchesPatternForRecord(candidates.get(i).pattern(), lr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a log record matches any expected response for a scenario instance.
     *
     * @param scenario scenario instance
     * @param lr       log record
     * @return true if expected
     */
    public boolean isExpected(final _Scenario scenario, final _LogRecord lr) {
        if (responses.isEmpty() || null == scenario || null == lr) return false;
        final List<ExpectedResponse> candidates = getForScenario(scenario);
        for (int i = 0; i < candidates.size(); i++) {
            if (ExpectedResponsesMatcher.matchesPatternForRecord(candidates.get(i).pattern(), lr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a log record matches any expected response for a tree NodeData.
     *
     * @param node tree node
     * @param lr   log record
     * @return true if expected
     */
    public boolean isExpected(final _TreeNodeView node, final _LogRecord lr) {
        if (responses.isEmpty() || null == node || null == lr) return false;
        final List<ExpectedResponse> candidates = getForNodeView(node);
        for (int i = 0; i < candidates.size(); i++) {
            if (ExpectedResponsesMatcher.matchesPatternForRecord(candidates.get(i).pattern(), lr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given scenario and execution result match any expected response pattern.
     *
     * @param scenario the scenario
     * @param result   the execution result
     * @return true if the scenario's result events match an expected response pattern
     */
    public boolean matchesNode(final _Scenario scenario, final _Result result) {
        if (responses.isEmpty() || null == scenario || null == result) return false;
        return matchesResultIncremental("S:" + scenarioKey(scenario), getForScenario(scenario), result);
    }

    /**
     * Checks if the given tree node's events match any expected response pattern for that node.
     *
     * @param node the tree node
     * @return true if the node's result events match an expected response pattern
     */
    public boolean matchesNode(final _TreeNodeView node) {
        if (responses.isEmpty() || null == node || null == node.result()) return false;
        return matchesResultIncremental("N:" + nodeViewKey(node), getForNodeView(node), node.result());
    }

    /**
     * Returns all matching expected responses for a log record in a scenario.
     *
     * @param scenarioName current scenario name
     * @param lr           log record
     * @return list of matching expected responses
     */
    public List<ExpectedResponse> getForRecord(final String scenarioName, final _LogRecord lr) {
        final List<ExpectedResponse> result = new ArrayList<>();
        if (responses.isEmpty() || null == lr) return result;
        final List<ExpectedResponse> candidates = getForScenarioName(scenarioName);
        for (int i = 0; i < candidates.size(); i++) {
            final ExpectedResponse er = candidates.get(i);
            if (ExpectedResponsesMatcher.matchesPatternForRecord(er.pattern(), lr)) {
                result.add(er);
            }
        }
        return result;
    }
}
