package works.lysenko.base.output.tree;

import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.svg.Groups;
import works.lysenko.base.output.svg.Parts;
import works.lysenko.util.apis.*;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Node;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.prop.tree.HideOff;
import works.lysenko.util.prop.tree.VisualOffset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Files.writeToFile;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_TREE_HTML_;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Computes scenario tree layout topology, traversing scenarios/groups,
 * matching known issues, and pruning off-branches.
 */
public final class TreeLayoutComputer {

    private static long lastProgressLogTimestamp = 0;
    private static final long PROGRESS_LOG_INTERVAL_MS = 2000;

    private static final class NonExpectedEventsCursor {
        final works.lysenko.base.responses.ExpectedResponsesStore expStore;
        final int expResponsesSize;
        final List<_LogRecord> nonBrowserNonExpected = new ArrayList<>(4);
        int scannedUpTo;
        _LogRecord firstRecord;
        _LogRecord lastRecord;

        NonExpectedEventsCursor(final works.lysenko.base.responses.ExpectedResponsesStore expStore, final int expResponsesSize) {
            this.expStore = expStore;
            this.expResponsesSize = expResponsesSize;
        }
    }

    private static final Object NON_EXPECTED_CURSOR_LOCK = new Object();
    private static final Map<_Result, Map<String, NonExpectedEventsCursor>> NON_EXPECTED_CURSORS = new WeakHashMap<>();

    private static final Object OWN_LOGS_CACHE_LOCK = new Object();
    private static KnownIssuesStore ownLogsStore;
    private static int ownLogsStoreSize;
    private static works.lysenko.base.responses.ExpectedResponsesStore ownLogsExpStore;
    private static int ownLogsExpSize;
    private static int ownLogsScannedUpTo;
    private static _LogRecord ownLogsFirstRecord;
    private static _LogRecord ownLogsLastRecord;
    private static final Map<_Scenario, Set<KnownIssue>> ownLogsMatchedByScenario = new IdentityHashMap<>();

    private TreeLayoutComputer() {
    }

    private static Map<_Scenario, List<KnownIssue>> resolveOwnLogKnownIssues(
            final List<_LogRecord> logRecords,
            final KnownIssuesStore store,
            final works.lysenko.base.responses.ExpectedResponsesStore expStore) {
        if (null == store || null == logRecords || logRecords.isEmpty()) {
            return Collections.emptyMap();
        }
        final int storeSize = store.getIssues().size();
        final int expSize = (null != expStore && null != expStore.getResponses()) ? expStore.getResponses().size() : 0;
        final int totalLogs = logRecords.size();
        final _LogRecord firstRecord = logRecords.getFirst();

        synchronized (OWN_LOGS_CACHE_LOCK) {
            final boolean valid = ownLogsStore == store
                    && ownLogsStoreSize == storeSize
                    && ownLogsExpStore == expStore
                    && ownLogsExpSize == expSize
                    && ownLogsScannedUpTo <= totalLogs
                    && (0 == ownLogsScannedUpTo
                    || (firstRecord == ownLogsFirstRecord && logRecords.get(ownLogsScannedUpTo - 1) == ownLogsLastRecord));
            if (!valid) {
                ownLogsStore = store;
                ownLogsStoreSize = storeSize;
                ownLogsExpStore = expStore;
                ownLogsExpSize = expSize;
                ownLogsScannedUpTo = 0;
                ownLogsFirstRecord = null;
                ownLogsLastRecord = null;
                ownLogsMatchedByScenario.clear();
            }

            if (ownLogsScannedUpTo < totalLogs) {
                for (int i = ownLogsScannedUpTo; i < totalLogs; i++) {
                    final _LogRecord lr = logRecords.get(i);
                    if (null == lr || null == lr.scenario()) continue;
                    final _Scenario sc = lr.scenario();
                    final List<KnownIssue> candidates = store.getForScenario(sc.getName(), sc.getSimpleName());
                    if (candidates.isEmpty()) continue;
                    final Set<KnownIssue> matched = ownLogsMatchedByScenario.computeIfAbsent(sc, _ -> new LinkedHashSet<>(4));
                    if (matched.size() >= candidates.size()) continue;
                    if (null != expStore && expStore.isExpected(sc, lr)) continue;
                    for (int c = 0; c < candidates.size(); c++) {
                        final KnownIssue ki = candidates.get(c);
                        if (matched.contains(ki)) continue;
                        if (works.lysenko.base.issues.KnownIssuesMatcher.isCatchAllRule(ki.scenario(), ki.pattern())) continue;
                        if (works.lysenko.base.issues.KnownIssuesMatcher.matchesPatternForRecord(ki.pattern(), lr)) {
                            matched.add(ki);
                        }
                    }
                }
                ownLogsScannedUpTo = totalLogs;
                ownLogsFirstRecord = firstRecord;
                ownLogsLastRecord = logRecords.get(totalLogs - 1);
            }

            if (ownLogsMatchedByScenario.isEmpty()) {
                return Collections.emptyMap();
            }
            final Map<_Scenario, List<KnownIssue>> snapshot = new IdentityHashMap<>(ownLogsMatchedByScenario.size() * 2);
            for (final Map.Entry<_Scenario, Set<KnownIssue>> entry : ownLogsMatchedByScenario.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
            return snapshot;
        }
    }

    private static List<_LogRecord> getNonBrowserNonExpectedEvents(
            final _Scenario scenario,
            final _Result result,
            final works.lysenko.base.responses.ExpectedResponsesStore expStore) {
        if (null == result || null == result.getEvents() || result.getEvents().isEmpty()) {
            return Collections.emptyList();
        }
        final List<_LogRecord> events = result.getEvents();
        final int eventCount = events.size();
        final int expSize = (null != expStore && null != expStore.getResponses()) ? expStore.getResponses().size() : 0;
        final String scenarioKey = (null != scenario)
                ? ((null != scenario.getName() ? scenario.getName() : "") + '\0' + (null != scenario.getSimpleName() ? scenario.getSimpleName() : ""))
                : "";
        final _LogRecord firstRecord = events.getFirst();

        synchronized (NON_EXPECTED_CURSOR_LOCK) {
            final Map<String, NonExpectedEventsCursor> perScenario = NON_EXPECTED_CURSORS.computeIfAbsent(result, _ -> new HashMap<>(2));
            NonExpectedEventsCursor cursor = perScenario.get(scenarioKey);
            final boolean valid = null != cursor
                    && cursor.expStore == expStore
                    && cursor.expResponsesSize == expSize
                    && cursor.scannedUpTo <= eventCount
                    && (0 == cursor.scannedUpTo
                    || (cursor.firstRecord == firstRecord && events.get(cursor.scannedUpTo - 1) == cursor.lastRecord));
            if (!valid) {
                cursor = new NonExpectedEventsCursor(expStore, expSize);
                perScenario.put(scenarioKey, cursor);
            }
            if (cursor.scannedUpTo < eventCount) {
                for (int i = cursor.scannedUpTo; i < eventCount; i++) {
                    final _LogRecord lr = events.get(i);
                    if (null == lr) continue;
                    final String text = lr.text();
                    if (null != text && text.contains("[BROWSER]")) continue;
                    if (null != expStore && expStore.isExpected(scenario, lr)) continue;
                    cursor.nonBrowserNonExpected.add(lr);
                }
                cursor.scannedUpTo = eventCount;
                cursor.firstRecord = firstRecord;
                cursor.lastRecord = events.get(eventCount - 1);
            }
            return cursor.nonBrowserNonExpected.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(cursor.nonBrowserNonExpected);
        }
    }

    private static Map<_Scenario, List<_LogRecord>> indexLogsByScenario(final List<_LogRecord> logRecords) {
        if (null == logRecords || logRecords.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<_Scenario, List<_LogRecord>> byScenario = new IdentityHashMap<>();
        for (final _LogRecord lr : logRecords) {
            if (null != lr && null != lr.scenario()) {
                byScenario.computeIfAbsent(lr.scenario(), _ -> new ArrayList<>(4)).add(lr);
            }
        }
        return byScenario;
    }

    public static TreeLayout computeLayout(final TreeMap<String, Result> sorted) {
        if (null != core) {
            final Set<_Scenario> roots = rootsOf(core.getRootScenarios());
            if (null != roots && !roots.isEmpty()) return computeLayout(roots, sorted);
        }
        final long tStart = System.currentTimeMillis();
        resetProgressTimer();
        final List<NodeData> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();
        final int[] dy = new int[100];
        Arrays.fill(dy, 1);

        final List<_LogRecord> logRecords = (!works.lysenko.util.prop.issues.KnownIssues.failedOnly && isNotNull(exec) && isNotNull(exec.issues()))
                ? exec.issues().latestCopy() : Collections.emptyList();
        final Map<_Scenario, List<_LogRecord>> logsByScenario = indexLogsByScenario(logRecords);

        final SortedMap<String, TreeMap<String, Result>> t = new Groups(TESTS, sorted);
        for (final Map.Entry<String, TreeMap<String, Result>> group : t.entrySet()) {
            processGroup(group, null, 0, dy, nodes, edges, logRecords, logsByScenario);
        }
        final long tProc = System.currentTimeMillis();

        final List<NodeData> unprunedNodes = new ArrayList<>(nodes);
        final TreeHtml.AccessibleContext accessibleCtx = new TreeHtml.AccessibleContext(getAccessibleScenarios());
        final Map<NodeData, Boolean> execCache = new HashMap<>();
        final Map<NodeData, Boolean> childFailCache = new HashMap<>();
        for (final NodeData n : unprunedNodes) {
            isTheoreticallyExecutable(n, accessibleCtx, execCache);
            hasChildFailure(n, childFailCache);
        }

        final long tPruneStart = System.currentTimeMillis();
        pruneOffBranches(nodes, edges, accessibleCtx, execCache, childFailCache);
        final Set<NodeData> hiddenNodes = prunePassedLeavesReturningHidden(nodes, edges, childFailCache);
        final long tPrune = System.currentTimeMillis();

        final works.lysenko.base.output.SummaryPlaque plaque = works.lysenko.base.output.SummaryPlaque.computeFromNodes(
                unprunedNodes, works.lysenko.base.output.SummaryPlaque.RunConfig.fromParameters(works.lysenko.Base.parameters),
                accessibleCtx, execCache, childFailCache, hiddenNodes);

        final long tOptStart = System.currentTimeMillis();
        TreeLayoutOptimizer.optimizeLayout(nodes);
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);
        TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);
        final long tOpt = System.currentTimeMillis();

        logDebug(s("Tree layout timing (groups): process = ", tProc - tStart, " ms (known issues checked against ", logRecords.size(), " logs), prune = ", tPrune - tPruneStart, " ms, optimize = ", tOpt - tOptStart, " ms"));
        return new TreeLayout(nodes, edges, plaque);
    }

    public static Set<_Scenario> rootsOf(final Iterable<? extends _Scenario> scenarios) {
        final Set<_Scenario> roots = Collections.newSetFromMap(new IdentityHashMap<>());
        if (null == scenarios) return roots;
        for (final _Scenario scenario : scenarios)
            if (null != scenario) roots.add(scenario);
        for (final _Scenario scenario : roots.toArray(_Scenario[]::new))
            if (scenario instanceof _Node node)
                for (final _Scenario child : node.getPool().getPairList().stream().map(pair -> pair.k()).toList())
                    roots.remove(child);
        return roots;
    }

    public static TreeLayout computeLayout(final Iterable<? extends _Scenario> roots, final TreeMap<String, Result> sorted) {
        final long tStart = System.currentTimeMillis();
        resetProgressTimer();
        final List<NodeData> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();
        final int[] rows = {1};
        final Set<_Scenario> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<_Scenario> sortedRoots = new ArrayList<>();
        if (null != roots) {
            for (final _Scenario r : roots) {
                if (null != r) sortedRoots.add(r);
            }
            sortedRoots.sort(Comparator.comparing(_Scenario::getSimpleName).thenComparing(_Scenario::getName));
        }

        final List<_LogRecord> logRecords = (!works.lysenko.util.prop.issues.KnownIssues.failedOnly && isNotNull(exec) && isNotNull(exec.issues()))
                ? exec.issues().latestCopy() : Collections.emptyList();
        final KnownIssuesStore store = TreeHtml.getKnownIssuesStore();
        final works.lysenko.base.responses.ExpectedResponsesStore expStore = TreeHtml.getExpectedResponsesStore();
        final Map<_Scenario, List<KnownIssue>> ownLogKnownIssues = resolveOwnLogKnownIssues(logRecords, store, expStore);

        for (final _Scenario root : sortedRoots)
            processScenario(root, null, 0, rows, ancestors, sorted, nodes, edges, ownLogKnownIssues, store, expStore);
        final long tProc = System.currentTimeMillis();

        final List<NodeData> unprunedNodes = new ArrayList<>(nodes);
        final TreeHtml.AccessibleContext accessibleCtx = new TreeHtml.AccessibleContext(getAccessibleScenarios());
        final Map<NodeData, Boolean> execCache = new HashMap<>();
        final Map<NodeData, Boolean> childFailCache = new HashMap<>();
        for (final NodeData n : unprunedNodes) {
            isTheoreticallyExecutable(n, accessibleCtx, execCache);
            hasChildFailure(n, childFailCache);
        }

        final long tPruneStart = System.currentTimeMillis();
        pruneOffBranches(nodes, edges, accessibleCtx, execCache, childFailCache);
        final Set<NodeData> hiddenNodes = prunePassedLeavesReturningHidden(nodes, edges, childFailCache);
        final long tPrune = System.currentTimeMillis();

        final works.lysenko.base.output.SummaryPlaque plaque = works.lysenko.base.output.SummaryPlaque.computeFromNodes(
                unprunedNodes, works.lysenko.base.output.SummaryPlaque.RunConfig.fromParameters(works.lysenko.Base.parameters),
                accessibleCtx, execCache, childFailCache, hiddenNodes);

        final long tOptStart = System.currentTimeMillis();
        TreeLayoutOptimizer.optimizeLayout(nodes);
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);
        TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);
        final long tOpt = System.currentTimeMillis();

        logDebug(s("Tree layout timing (scenarios): process = ", tProc - tStart, " ms (known issues checked against ", logRecords.size(), " logs), prune = ", tPrune - tPruneStart, " ms, optimize = ", tOpt - tOptStart, " ms"));
        return new TreeLayout(nodes, edges, plaque);
    }
    public static final java.util.Map<String, Integer> offsetOverrides = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<String, Integer> rowOffsetOverrides = new java.util.concurrent.ConcurrentHashMap<>();

    private static void processScenario(final _Scenario scenario, final NodeData parent, final int col, final int[] rows,
                                        final Set<_Scenario> ancestors, final TreeMap<String, Result> sorted,
                                        final List<NodeData> nodes, final List<Edge> edges,
                                        final Map<_Scenario, List<KnownIssue>> ownLogKnownIssues,
                                        final KnownIssuesStore store,
                                        final works.lysenko.base.responses.ExpectedResponsesStore expStore) {

        if (null == scenario || !ancestors.add(scenario)) return;
        int offset = 0;
        if (offsetOverrides.containsKey(scenario.getClass().getName())) {
            offset = offsetOverrides.get(scenario.getClass().getName());
        } else if (offsetOverrides.containsKey(scenario.getSimpleName())) {
            offset = offsetOverrides.get(scenario.getSimpleName());
        } else {
            final VisualOffset vo = scenario.getClass().getAnnotation(VisualOffset.class);
            if (vo != null) offset = vo.value();
        }
        int rowOffset = 0;
        if (rowOffsetOverrides.containsKey(scenario.getClass().getName())) {
            rowOffset = rowOffsetOverrides.get(scenario.getClass().getName());
        } else if (rowOffsetOverrides.containsKey(scenario.getSimpleName())) {
            rowOffset = rowOffsetOverrides.get(scenario.getSimpleName());
        } else {
            final works.lysenko.util.prop.tree.VisualRowOffset vro = scenario.getClass().getAnnotation(works.lysenko.util.prop.tree.VisualRowOffset.class);
            if (vro != null) rowOffset = vro.value();
        }
        final int myCol = col + offset;
        final String label = b(scenario.getSimpleName(), scenario.type().tag());
        final String id = s("scenario_", nodes.size(), "_", scenario.getSimpleName());
        final String group = (null == parent) ? TESTS : parent.label();
        final NodeData node = new NodeData(id, label, group, myCol, rows[0]++, offset, rowOffset, resultFor(scenario, sorted), scenario);

        if (null != store) {
            final List<KnownIssue> matchedByNode = store.getForNode(scenario, node.result());
            if (null != matchedByNode && !matchedByNode.isEmpty()) {
                final boolean nodeHasExpected = null != expStore && null != node.result()
                        && expStore.matchesNode(scenario, node.result());
                if (!nodeHasExpected) {
                    for (final KnownIssue ki : matchedByNode) {
                        if (!node.knownIssues().contains(ki)) node.knownIssues().add(ki);
                    }
                } else {
                    final List<_LogRecord> nonExpectedEvents = getNonBrowserNonExpectedEvents(scenario, node.result(), expStore);
                    if (!nonExpectedEvents.isEmpty()) {
                        for (final KnownIssue ki : matchedByNode) {
                            if (matchesAnyRecord(ki.pattern(), nonExpectedEvents)) {
                                if (!node.knownIssues().contains(ki)) node.knownIssues().add(ki);
                            }
                        }
                    }
                }
            }
            final List<KnownIssue> ownMatches = ownLogKnownIssues.get(scenario);
            if (null != ownMatches && !ownMatches.isEmpty()) {
                for (final KnownIssue ki : ownMatches) {
                    if (!node.knownIssues().contains(ki)) {
                        node.knownIssues().add(ki);
                    }
                }
            }
        }

        nodes.add(node);
        if (null != parent) {
            node.setParent(parent);
            parent.children().add(node);
            edges.add(new Edge(parent, node));
        }
        if (scenario instanceof _Node parentNode) {
            final List<works.lysenko.util.data.records.KeyValue<_Scenario, org.apache.commons.math3.fraction.Fraction>> pairs =
                    new ArrayList<>(parentNode.getPool().getPairList());
            pairs.sort(Comparator.comparing((works.lysenko.util.data.records.KeyValue<_Scenario, org.apache.commons.math3.fraction.Fraction> p) -> p.k().getSimpleName())
                    .thenComparing(p -> p.k().getName()));
            for (final var child : pairs)
                processScenario(child.k(), node, myCol + 1, rows, ancestors, sorted, nodes, edges, ownLogKnownIssues, store, expStore);
        }
        ancestors.remove(scenario);
    }

    private static boolean matchesAnyRecord(final String pattern, final List<_LogRecord> records) {
        for (int i = 0; i < records.size(); i++) {
            if (works.lysenko.base.issues.KnownIssuesMatcher.matchesPatternForRecord(pattern, records.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static void processGroup(final Map.Entry<String, ? extends TreeMap<String, Result>> group,
                                     final NodeData parentNode,
                                     final int col, final int[] dy,
                                     final List<NodeData> nodes, final List<Edge> edges,
                                     final List<_LogRecord> logRecords,
                                     final Map<_Scenario, List<_LogRecord>> logsByScenario) {
        final Map<String, NodeData> myNodes = new HashMap<>();
        final Parts parts = Parts.process(group.getValue());

        for (final Map.Entry<String, Result> entry : parts.head.entrySet()) {
            final int myRow = dy[col]++;
            final String rawKey = entry.getKey();
            final String shortKey = rawKey.split(SPACE)[ZERO];
            final String nodeId = s("col_", col, "_row_", myRow, "_", shortKey.replace('.', '_'));
            final NodeData nd = new NodeData(nodeId, rawKey, group.getKey(), col, myRow, entry.getValue());

            final KnownIssuesStore store = TreeHtml.getKnownIssuesStore();
            if (null != store) {
                final String groupPath = group.getKey().replace(" ▷", "").replace(" ◼", "").replace(" ◆", "");
                final String fullScenarioKey = (nd.scenario() != null) ? nd.scenario().getName() : (groupPath.isEmpty() || groupPath.equals(TESTS) ? rawKey : groupPath + "." + rawKey);
                final String altScenarioKey = (nd.scenario() != null) ? nd.scenario().getShortName() : shortKey;
                if (isNotNull(nd.result())) {
                    for (final KnownIssue ki : store.getIssues()) {
                        if (works.lysenko.base.issues.KnownIssuesMatcher.isCatchAllRule(ki.scenario(), ki.pattern())) continue;
                        if (works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), fullScenarioKey, shortKey)
                                || works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), altScenarioKey, shortKey)
                                || works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), rawKey, shortKey)) {
                            if (works.lysenko.base.issues.KnownIssuesMatcher.matchesPattern(ki.pattern(), nd.result())) {
                                if (!nd.knownIssues().contains(ki)) nd.knownIssues().add(ki);
                            }
                        }
                    }
                }
                final List<_LogRecord> ownLogs = (null != nd.scenario()) ? logsByScenario.get(nd.scenario()) : null;
                if (null != ownLogs && !ownLogs.isEmpty()) {
                    for (final KnownIssue ki : store.getIssues()) {
                        if (nd.knownIssues().contains(ki)) continue;
                        if (works.lysenko.base.issues.KnownIssuesMatcher.isCatchAllRule(ki.scenario(), ki.pattern())) continue;
                        if (works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), fullScenarioKey, shortKey)
                                || works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), altScenarioKey, shortKey)
                                || works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), rawKey, shortKey)) {
                            for (final _LogRecord lr : ownLogs) {
                                if (works.lysenko.base.issues.KnownIssuesMatcher.matchesPatternForRecord(ki.pattern(), lr)) {
                                    nd.knownIssues().add(ki);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            nodes.add(nd);
            myNodes.put(shortKey.toLowerCase(), nd);

            if (null != parentNode) {
                nd.setParent(parentNode);
                edges.add(new Edge(parentNode, nd));
                parentNode.children().add(nd);
            }
        }

        final Groups groups = Groups.process(parts.body);
        for (final Map.Entry<String, TreeMap<String, Result>> subgroup : groups.entrySet()) {
            final String subKey = subgroup.getKey().toLowerCase();
            final NodeData subParent = myNodes.get(subKey);
            final NodeData nextParent = (null != subParent) ? subParent : parentNode;
            processGroup(subgroup, nextParent, col + 1, dy, nodes, edges, logRecords, logsByScenario);
        }
    }

    public static boolean hasFailure(final NodeData n) {
        return NodeEventClassifier.classify(n).hasFailure();
    }

    public static boolean hasChildFailure(final NodeData n) {
        return hasChildFailure(n, new HashMap<>());
    }

    public static boolean hasChildFailure(final NodeData n, final Map<NodeData, Boolean> cache) {
        if (null == n) return false;
        final Boolean precomputed = n.precomputedChildFailure();
        if (null != precomputed) return precomputed;
        final Boolean cached = cache.get(n);
        if (null != cached) return cached;

        boolean result = false;
        if (null != n.scenario()) {
            if (n.scenario().hasChildFailed() || n.scenario().getExecutionStatus() == works.lysenko.util.data.enums.ExecutionStatus.CHILD_FAILED) {
                result = true;
            }
        }
        if (!result && null != n.result()) {
            if (n.result().status() == works.lysenko.util.data.enums.ExecutionStatus.CHILD_FAILED) {
                result = true;
            }
        }
        if (!result && null != n.children() && !n.children().isEmpty()) {
            for (final NodeData child : n.children()) {
                if (hasFailure(child) || hasChildFailure(child, cache)) {
                    result = true;
                    break;
                }
            }
        }
        cache.put(n, result);
        return result;
    }

    public static boolean isTheoreticallyExecutable(final NodeData n, final Set<_Scenario> accessibleScenarios) {
        return isTheoreticallyExecutable(n, accessibleScenarios, new HashMap<>());
    }

    public static boolean isTheoreticallyExecutable(
            final NodeData n,
            final Set<_Scenario> accessibleScenarios,
            final Map<NodeData, Boolean> cache) {
        if (null == n) return false;
        final Boolean precomputed = n.precomputedExecutable();
        if (null != precomputed) return precomputed;
        final Boolean cached = cache.get(n);
        if (null != cached) return cached;
        return isTheoreticallyExecutable(n, new TreeHtml.AccessibleContext(accessibleScenarios), cache);
    }

    public static boolean isTheoreticallyExecutable(
            final NodeData n,
            final TreeHtml.AccessibleContext ctx,
            final Map<NodeData, Boolean> cache) {
        if (null == n) return false;
        final Boolean precomputed = n.precomputedExecutable();
        if (null != precomputed) return precomputed;
        final Boolean cached = cache.get(n);
        if (null != cached) return cached;

        final boolean res = computeIsTheoreticallyExecutable(n, ctx, cache);
        cache.put(n, res);
        return res;
    }

    private static boolean computeIsTheoreticallyExecutable(
            final NodeData n,
            final TreeHtml.AccessibleContext ctx,
            final Map<NodeData, Boolean> cache) {
        if (null != n.parent()) {
            final boolean parentIsStructural = null == n.parent().scenario() && null == n.parent().result();
            final boolean parentWasExecuted = null != n.parent().result() && n.parent().result().getExecutions() > 0;
            if (!parentIsStructural && !parentWasExecuted && !isTheoreticallyExecutable(n.parent(), ctx, cache)) {
                return false;
            }
        }
        if (null != n.result() && n.result().getExecutions() > 0) {
            return true;
        }
        if (null != n.scenario()) {
            final _Scenario sc = n.scenario();
            if (sc.hasFailed() || sc.isExhausted()) {
                return false;
            }
            final org.apache.commons.math3.fraction.Fraction configured = sc.weightConfigured();
            final double cw = (null != configured) ? configured.doubleValue()
                    : ((null != sc.weightCoded()) ? sc.weightCoded().doubleValue() : 1.0);
            if (cw <= 0.0) {
                final org.apache.commons.math3.fraction.Fraction downstream = sc.weightDownstream();
                final double dw = (null != downstream) ? downstream.doubleValue() : 0.0;
                if (dw <= 0.0) {
                    final org.apache.commons.math3.fraction.Fraction upstream = sc.weightUpstream();
                    final double uw = (null != upstream) ? upstream.doubleValue() : 0.0;
                    if (uw <= 0.0) {
                        return false;
                    }
                }
            }
        }
        if (null != ctx.scenarios && !ctx.scenarios.isEmpty()) {
            if (null != n.scenario()) {
                final _Scenario sc = n.scenario();
                if (ctx.scenarios.contains(sc)) return true;
                return ctx.names.contains(sc.getName())
                        || ctx.names.contains(sc.getSimpleName())
                        || ctx.names.contains(sc.getShortName())
                        || ctx.classes.contains(sc.getClass())
                        || ctx.classes.contains(sc.getClass().getSuperclass());
            }
            return ctx.names.contains(n.label());
        }
        if (null != n.scenario()) {
            final _Scenario s = n.scenario();
            return s.calculateCombinations(false) > 0;
        }
        if (null != n.result()) {
            final org.apache.commons.math3.fraction.Fraction cw = n.result().getConfiguredWeight();
            if (null != cw) {
                final double cwVal = cw.doubleValue();
                final double dwVal = (null != n.result().getDownstreamWeight()) ? n.result().getDownstreamWeight().doubleValue() : 0.0;
                final double uwVal = (null != n.result().getUpstreamWeight()) ? n.result().getUpstreamWeight().doubleValue() : 0.0;
                if (cwVal <= 0.0 && dwVal <= 0.0 && uwVal <= 0.0) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isTheoreticallyExecutable(final NodeData n) {
        return isTheoreticallyExecutable(n, getAccessibleScenarios());
    }

    public static Set<_Scenario> previewAccessibleScenarios = null;

    public static Set<_Scenario> getAccessibleScenarios() {
        if (previewAccessibleScenarios != null) {
            return previewAccessibleScenarios;
        }
        if (isNotNull(core)) {
            try {
                final Set<_Scenario> accessible = core.getAccessibleScenarios();
                if (isNotNull(accessible) && !accessible.isEmpty()) {
                    return accessible;
                }
            } catch (final Throwable _) {
            }
        }
        return Collections.emptySet();
    }

    private static Result resultFor(final _Scenario scenario, final TreeMap<String, Result> sorted) {
        if (previewAccessibleScenarios == null && null != core && null != core.getResults()) {
            final Result result = core.getResults().getResult(scenario);
            if (null != result) return result;
        }
        final Result result = sorted.get(b(scenario.getShortName(), scenario.type().tag()));
        if (null != result) return result;
        if (previewAccessibleScenarios != null) {
            return new Result(scenario, scenario.type(), scenario.weightConfigured(), scenario.weightUpstream(), scenario.weightDownstream(), Collections.emptyList(), 1, 0);
        }
        return new Result(scenario);
    }

    public static void resetProgressTimer() {
        lastProgressLogTimestamp = System.currentTimeMillis();
    }

    public static void logProgressIfNeeded(final String phase, final int current, final int total) {
        final long now = System.currentTimeMillis();
        if (0 == lastProgressLogTimestamp) {
            lastProgressLogTimestamp = now;
            return;
        }
        if (now - lastProgressLogTimestamp >= PROGRESS_LOG_INTERVAL_MS) {
            lastProgressLogTimestamp = now;
            if (total > 0) {
                logDebug(s("Tree HTML generation in progress... ", phase, ": ", current, "/", total, " (", (current * 100 / total), "%)"));
            } else {
                logDebug(s("Tree HTML generation in progress... ", phase, ": ", current, " processed"));
            }
        }
    }

    public static void treeStats() {
        try {
            resetProgressTimer();
            final long tStart = System.currentTimeMillis();
            logDebug("Generating tree.html report...");

            final TreeMap<String, Result> sorted = core.getResults().getSortedStrings(false);

            final long tLayoutStart = System.currentTimeMillis();
            TreeLayoutOptimizer.triggerFinalReoptimizationOnce();
            final TreeLayout layout = computeLayout(sorted);
            final long tLayout = System.currentTimeMillis();

            final long tRenderStart = System.currentTimeMillis();
            final String html = TreeHtmlRenderer.renderHtml(layout);
            final long tRender = System.currentTimeMillis();

            final long tWriteStart = System.currentTimeMillis();
            final String fileName = name(RUN_TREE_HTML_);
            writeToFile(html, fileName);
            final long tWrite = System.currentTimeMillis();

            logDebug(s("tree.html generated in ", tWrite - tStart, " ms (layout: ", tLayout - tLayoutStart, " ms, render: ", tRender - tRenderStart, " ms, write: ", tWrite - tWriteStart, " ms)"));
        } catch (final Throwable t) {
            logEvent(S2, s("Failed to generate tree.html: ", t.getMessage()));
        }
    }

    public static void pruneOffBranches(final List<NodeData> nodes, final List<Edge> edges) {
        final TreeHtml.AccessibleContext accessibleCtx = new TreeHtml.AccessibleContext(getAccessibleScenarios());
        pruneOffBranches(nodes, edges, accessibleCtx, new HashMap<>(), new HashMap<>());
    }

    private static void pruneOffBranches(
            final List<NodeData> nodes,
            final List<Edge> edges,
            final TreeHtml.AccessibleContext accessibleCtx,
            final Map<NodeData, Boolean> execCache,
            final Map<NodeData, Boolean> childFailCache) {
        if (!HideOff.branches) return;

        final Set<NodeData> toRemove = new HashSet<>();
        for (final NodeData n : nodes) {
            if (!isTheoreticallyExecutable(n, accessibleCtx, execCache)
                    && !hasFailure(n)
                    && !hasChildFailure(n, childFailCache)) {
                toRemove.add(n);
            }
        }

        if (toRemove.isEmpty()) return;

        nodes.removeAll(toRemove);
        edges.removeIf(e -> toRemove.contains(e.from()) || toRemove.contains(e.to()));
        for (final NodeData n : nodes) {
            n.children().removeAll(toRemove);
        }
    }

    public static int prunePassedLeaves(final List<NodeData> nodes, final List<Edge> edges) {
        return prunePassedLeavesReturningHidden(nodes, edges, new HashMap<>()).size();
    }

    private static Set<NodeData> prunePassedLeavesReturningHidden(
            final List<NodeData> nodes,
            final List<Edge> edges,
            final Map<NodeData, Boolean> childFailCache) {
        if (!works.lysenko.util.prop.tree.HideOff.passedLeaves) return Collections.emptySet();

        boolean changed;
        final Set<NodeData> toRemove = new HashSet<>();
        do {
            changed = false;
            for (final NodeData n : nodes) {
                if (toRemove.contains(n)) continue;

                // The first-root-level nodes should always be visible
                if (n.parent() == null) continue;

                boolean isLeaf = true;
                for (final NodeData child : n.children()) {
                    if (!toRemove.contains(child)) {
                        isLeaf = false;
                        break;
                    }
                }

                if (isLeaf) {
                    final _Result res = n.result();
                    final int execs = (res != null) ? res.getExecutions() : 0;
                    if (execs > 0
                            && !hasFailure(n)
                            && !hasChildFailure(n, childFailCache)
                            && !works.lysenko.base.output.TreeHtml.hasWarning(n)
                            && !works.lysenko.base.output.TreeHtml.hasNotice(n)) {
                        toRemove.add(n);
                        changed = true;
                    }
                }
            }
        } while (changed);

        if (toRemove.isEmpty()) return Collections.emptySet();

        nodes.removeAll(toRemove);
        edges.removeIf(e -> toRemove.contains(e.from()) || toRemove.contains(e.to()));
        for (final NodeData n : nodes) {
            n.children().removeAll(toRemove);
        }
        return toRemove;
    }
}
