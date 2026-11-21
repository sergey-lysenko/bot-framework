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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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

    private static final class LogScanCursor {
        int scannedUpTo;
        _LogRecord lastRecord;
        boolean matched;
    }

    private static final Object LOG_SCAN_LOCK = new Object();
    private static List<_LogRecord> cachedLogListRef = null;
    private static _LogRecord cachedLogFirstRecord = null;
    private static final Map<String, LogScanCursor> LOG_SCAN_CURSORS = new HashMap<>();

    private TreeLayoutComputer() {
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
                ? exec.issues().latest() : Collections.emptyList();

        final Map<String, Boolean> patternLogCache = new HashMap<>();
        final SortedMap<String, TreeMap<String, Result>> t = new Groups(TESTS, sorted);
        for (final Map.Entry<String, TreeMap<String, Result>> group : t.entrySet()) {
            processGroup(group, null, 0, dy, nodes, edges, logRecords, patternLogCache);
        }
        final long tProc = System.currentTimeMillis();

        final List<NodeData> unprunedNodes = new ArrayList<>(nodes);
        final TreeHtml.AccessibleContext accessibleCtx = new TreeHtml.AccessibleContext(getAccessibleScenarios());
        final Map<NodeData, Boolean> execCache = new HashMap<>();
        final Map<NodeData, Boolean> childFailCache = new HashMap<>();

        final long tPruneStart = System.currentTimeMillis();
        pruneOffBranches(nodes, edges);
        final int hiddenCount = prunePassedLeaves(nodes, edges);
        final long tPrune = System.currentTimeMillis();

        final works.lysenko.base.output.SummaryPlaque plaque = works.lysenko.base.output.SummaryPlaque.computeFromNodes(
                unprunedNodes, works.lysenko.base.output.SummaryPlaque.RunConfig.fromParameters(works.lysenko.Base.parameters),
                accessibleCtx, execCache, childFailCache, hiddenCount);

        final long tOptStart = System.currentTimeMillis();
        TreeLayoutOptimizer.optimizeLayout(nodes);
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
                ? exec.issues().latest() : Collections.emptyList();

        final Map<String, Boolean> patternLogCache = new HashMap<>();
        for (final _Scenario root : sortedRoots)
            processScenario(root, null, 0, rows, ancestors, sorted, nodes, edges, logRecords, patternLogCache);
        final long tProc = System.currentTimeMillis();

        final List<NodeData> unprunedNodes = new ArrayList<>(nodes);
        final TreeHtml.AccessibleContext accessibleCtx = new TreeHtml.AccessibleContext(getAccessibleScenarios());
        final Map<NodeData, Boolean> execCache = new HashMap<>();
        final Map<NodeData, Boolean> childFailCache = new HashMap<>();

        final long tPruneStart = System.currentTimeMillis();
        pruneOffBranches(nodes, edges);
        final int hiddenCount = prunePassedLeaves(nodes, edges);
        final long tPrune = System.currentTimeMillis();

        final works.lysenko.base.output.SummaryPlaque plaque = works.lysenko.base.output.SummaryPlaque.computeFromNodes(
                unprunedNodes, works.lysenko.base.output.SummaryPlaque.RunConfig.fromParameters(works.lysenko.Base.parameters),
                accessibleCtx, execCache, childFailCache, hiddenCount);

        final long tOptStart = System.currentTimeMillis();
        TreeLayoutOptimizer.optimizeLayout(nodes);
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
                                        final List<_LogRecord> logRecords,
                                        final Map<String, Boolean> patternLogCache) {

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
        final NodeData node = new NodeData(id, label, group, myCol, rows[0]++, rowOffset, resultFor(scenario, sorted), scenario);

        final KnownIssuesStore store = TreeHtml.getKnownIssuesStore();
        if (null != store) {
            final List<KnownIssue> matchedByNode = store.getForNode(scenario, node.result());
            if (null != matchedByNode && !matchedByNode.isEmpty()) {
                for (final KnownIssue ki : matchedByNode) {
                    if (!node.knownIssues().contains(ki)) node.knownIssues().add(ki);
                }
            }
            if (!works.lysenko.util.prop.issues.KnownIssues.failedOnly && null != logRecords && !logRecords.isEmpty()) {
                for (final KnownIssue ki : store.getForScenario(scenario.getName(), scenario.getSimpleName())) {
                    if (node.knownIssues().contains(ki)) continue;
                    final String scenarioTarget = ki.scenario();
                    final boolean isWildcard = null == scenarioTarget || scenarioTarget.isBlank() || "*".equals(scenarioTarget.trim());
                    if (isWildcard && !ki.pattern().isBlank()) continue;

                    if (matchesAnyLogRecord(ki.pattern(), logRecords, s("checking known issues for scenario ", scenario.getSimpleName()), patternLogCache)) {
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
                processScenario(child.k(), node, myCol + 1, rows, ancestors, sorted, nodes, edges, logRecords, patternLogCache);
        }
        ancestors.remove(scenario);
    }

    private static void processGroup(final Map.Entry<String, ? extends TreeMap<String, Result>> group,
                                     final NodeData parentNode,
                                     final int col, final int[] dy,
                                     final List<NodeData> nodes, final List<Edge> edges,
                                     final List<_LogRecord> logRecords,
                                     final Map<String, Boolean> patternLogCache) {
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
                        if (works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), fullScenarioKey, shortKey)
                                || works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), altScenarioKey, shortKey)
                                || works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), rawKey, shortKey)) {
                            if (works.lysenko.base.issues.KnownIssuesMatcher.matchesPattern(ki.pattern(), nd.result())) {
                                if (!nd.knownIssues().contains(ki)) nd.knownIssues().add(ki);
                            }
                        }
                    }
                }
                if (!works.lysenko.util.prop.issues.KnownIssues.failedOnly && null != logRecords && !logRecords.isEmpty()) {
                    for (final KnownIssue ki : store.getIssues()) {
                        if (nd.knownIssues().contains(ki)) continue;
                        final String scenarioTarget = ki.scenario();
                        final boolean isWildcard = null == scenarioTarget || scenarioTarget.isBlank() || "*".equals(scenarioTarget.trim());
                        if (isWildcard && !ki.pattern().isBlank()) continue;

                        if (works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), fullScenarioKey, shortKey)
                                || works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), altScenarioKey, shortKey)
                                || works.lysenko.base.issues.KnownIssuesMatcher.matchesScenario(ki.scenario(), rawKey, shortKey)) {
                            if (matchesAnyLogRecord(ki.pattern(), logRecords, s("checking known issues for ", shortKey), patternLogCache)) {
                                nd.knownIssues().add(ki);
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
            processGroup(subgroup, nextParent, col + 1, dy, nodes, edges, logRecords, patternLogCache);
        }
    }

    private static boolean matchesAnyLogRecord(
            final String pattern,
            final List<_LogRecord> logRecords,
            final String progressLabel,
            final Map<String, Boolean> perCallCache) {
        final String cacheKey = (null == pattern) ? "" : pattern;
        final Boolean perCall = perCallCache.get(cacheKey);
        if (null != perCall) return perCall;

        final int size = logRecords.size();
        if (0 == size) {
            perCallCache.put(cacheKey, Boolean.FALSE);
            return false;
        }

        final _LogRecord firstRecord = logRecords.get(0);
        int startIdx = 0;
        synchronized (LOG_SCAN_LOCK) {
            if (cachedLogListRef != logRecords || cachedLogFirstRecord != firstRecord) {
                cachedLogListRef = logRecords;
                cachedLogFirstRecord = firstRecord;
                LOG_SCAN_CURSORS.clear();
            } else {
                final LogScanCursor cursor = LOG_SCAN_CURSORS.get(cacheKey);
                if (null != cursor && cursor.scannedUpTo <= size) {
                    if (0 == cursor.scannedUpTo || logRecords.get(cursor.scannedUpTo - 1) == cursor.lastRecord) {
                        if (cursor.matched) {
                            perCallCache.put(cacheKey, Boolean.TRUE);
                            return true;
                        }
                        startIdx = cursor.scannedUpTo;
                    }
                }
            }
        }

        boolean matched = false;
        int scannedTo = size;
        for (int i = startIdx; i < size; i++) {
            final _LogRecord lr = logRecords.get(i);
            logProgressIfNeeded(progressLabel, i + 1, size);
            if (works.lysenko.base.issues.KnownIssuesMatcher.matchesPatternForRecord(pattern, lr)) {
                matched = true;
                scannedTo = i + 1;
                break;
            }
        }

        synchronized (LOG_SCAN_LOCK) {
            if (cachedLogListRef == logRecords && cachedLogFirstRecord == firstRecord) {
                final LogScanCursor cursor = LOG_SCAN_CURSORS.computeIfAbsent(cacheKey, k -> new LogScanCursor());
                cursor.scannedUpTo = scannedTo;
                cursor.lastRecord = (scannedTo > 0) ? logRecords.get(scannedTo - 1) : null;
                cursor.matched = matched;
            }
        }
        perCallCache.put(cacheKey, matched);
        return matched;
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
            } catch (final Throwable ignored) {
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
            final TreeLayout layout = computeLayout(sorted);
            final long tLayout = System.currentTimeMillis();

            final long tRenderStart = System.currentTimeMillis();
            final String html = TreeHtmlRenderer.renderHtml(layout.nodes(), layout.edges());
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
        if (!HideOff.branches) return;

        final Set<_Scenario> accessibleScenarios = getAccessibleScenarios();
        final TreeHtml.AccessibleContext accessibleCtx = new TreeHtml.AccessibleContext(accessibleScenarios);
        final Map<NodeData, Boolean> cache = new HashMap<>();
        final Set<NodeData> toRemove = new HashSet<>();
        for (final NodeData n : nodes) {
            if (!isTheoreticallyExecutable(n, accessibleCtx, cache)) {
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
        if (!works.lysenko.util.prop.tree.HideOff.passedLeaves) return 0;

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
                    final works.lysenko.util.data.type.Result res = (works.lysenko.util.data.type.Result) n.result();
                    final int execs = (res != null) ? res.getExecutions() : 0;
                    if (execs > 0 && !hasFailure(n) && !works.lysenko.base.output.TreeHtml.hasWarning(n) && !works.lysenko.base.output.TreeHtml.hasNotice(n)) {
                        toRemove.add(n);
                        changed = true;
                    }
                }
            }
        } while (changed);

        if (toRemove.isEmpty()) return 0;
        final int count = toRemove.size();

        nodes.removeAll(toRemove);
        edges.removeIf(e -> toRemove.contains(e.from()) || toRemove.contains(e.to()));
        for (final NodeData n : nodes) {
            n.children().removeAll(toRemove);
        }
        return count;
    }
}
