package works.lysenko.base.ui.tree;

import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.tree.TreeLayoutOptimizer;
import works.lysenko.base.ui.tree.ScenarioOffsetSourceCodec.Offsets;
import works.lysenko.tree.Tree;
import works.lysenko.util.apis.scenario._Node;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.prop.tree.Optimization;
import works.lysenko.util.prop.tree.VisualOffset;
import works.lysenko.util.prop.tree.VisualRowOffset;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.T.TESTS;

/**
 * Pure data model, undo/redo history, and isolated preview layout engine for
 * {@link works.lysenko.base.ui.TreeLayoutEditorPanel}.
 */
public final class TreeLayoutEditorModel {

    private static final Object OPTIMIZATION_LOCK = new Object();

    /**
     * Static scenario blueprint captured once on load so preview recomputations never rescan the classpath.
     */
    public static final class ScenarioBlueprint {
        private final String className;
        private final String simpleName;
        private final String displayLabel;
        private final _Scenario scenario;
        private final List<ScenarioBlueprint> children = new ArrayList<>();
        private int subtreeSize = 1;

        public ScenarioBlueprint(
                final String className,
                final String simpleName,
                final String displayLabel,
                final _Scenario scenario) {
            this.className = className;
            this.simpleName = simpleName;
            this.displayLabel = displayLabel;
            this.scenario = scenario;
        }

        public String className() { return className; }
        public String simpleName() { return simpleName; }
        public String displayLabel() { return displayLabel; }
        public _Scenario scenario() { return scenario; }
        public List<ScenarioBlueprint> children() { return children; }
        public boolean isLeaf() { return children.isEmpty(); }
        public int subtreeSize() { return subtreeSize; }

        void recomputeSubtreeSize() {
            int count = 1;
            for (final ScenarioBlueprint child : children) {
                child.recomputeSubtreeSize();
                count += child.subtreeSize();
            }
            this.subtreeSize = count;
        }
    }

    /**
     * Isolated preview compaction parameters that do not permanently mutate {@link Optimization}.
     */
    public static final class PreviewSettings {
        public boolean compacted = true;
        public boolean reduceHorizontalOffsets = Optimization.reduceHorizontalOffsets;
        public boolean reduceVerticalOffsets = Optimization.reduceVerticalOffsets;
        public boolean reduceHorizontalGaps = Optimization.reduceHorizontalGaps;
        public boolean reduceVerticalGaps = Optimization.reduceVerticalGaps;
        public int minOffsetSeparation = Optimization.minOffsetSeparation;

        public PreviewSettings copy() {
            final PreviewSettings c = new PreviewSettings();
            c.compacted = this.compacted;
            c.reduceHorizontalOffsets = this.reduceHorizontalOffsets;
            c.reduceVerticalOffsets = this.reduceVerticalOffsets;
            c.reduceHorizontalGaps = this.reduceHorizontalGaps;
            c.reduceVerticalGaps = this.reduceVerticalGaps;
            c.minOffsetSeparation = this.minOffsetSeparation;
            return c;
        }
    }

    /**
     * Snapshot of a single layout preview pass (both raw post-optimizeLayout and final compacted coordinates).
     */
    public record PreviewResult(
            TreeLayout layout,
            Map<String, NodeData> nodeById,
            Map<String, String> classNameByNodeId,
            Map<String, List<NodeData>> nodesByClassName,
            Map<String, Point2DGrid> rawPositionByNodeId,
            int totalCols,
            double totalRows,
            int overlapCount) {
        public static final PreviewResult EMPTY = new PreviewResult(
                new TreeLayout(List.of(), List.of()),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                0,
                0.0,
                0);
    }

    public record Point2DGrid(int col, double row) {}

    public record OffsetChange(String className, String simpleName, Offsets initial, Offsets current) {}

    private record HistoryEntry(Map<String, Offsets> before, Map<String, Offsets> after) {}

    private final List<ScenarioBlueprint> roots = new ArrayList<>();
    private final Map<String, ScenarioBlueprint> blueprintByClassName = new TreeMap<>();
    private final Map<String, String> primaryParentByClassName = new HashMap<>();
    private final Map<String, Offsets> initialOffsets = new TreeMap<>();
    private final Map<String, Offsets> currentOffsets = new TreeMap<>();
    private final Deque<HistoryEntry> undoStack = new ArrayDeque<>();
    private final Deque<HistoryEntry> redoStack = new ArrayDeque<>();
    private final PreviewSettings previewSettings = new PreviewSettings();
    private PreviewResult latestPreview = PreviewResult.EMPTY;

    public PreviewSettings previewSettings() {
        return previewSettings;
    }

    public List<ScenarioBlueprint> roots() {
        return Collections.unmodifiableList(roots);
    }

    public Map<String, ScenarioBlueprint> blueprintsByClassName() {
        return Collections.unmodifiableMap(blueprintByClassName);
    }

    public ScenarioBlueprint blueprint(final String className) {
        return blueprintByClassName.get(className);
    }

    public String parentClassName(final String className) {
        return primaryParentByClassName.get(className);
    }

    public boolean isEmpty() {
        return blueprintByClassName.isEmpty();
    }

    public PreviewResult latestPreview() {
        return latestPreview;
    }

    /**
     * Loads scenarios from {@code rootPackage}, reading initial offsets from Java sources under {@code sourceBaseDir}.
     */
    public void loadFromPackage(final String rootPackage, final Path sourceBaseDir) {
        clear();
        if (null == rootPackage || rootPackage.isBlank()) {
            latestPreview = PreviewResult.EMPTY;
            return;
        }
        final Tree tree = new Tree(rootPackage);
        final Set<_Scenario> branches = tree.getBranches();
        if (null == branches || branches.isEmpty()) {
            latestPreview = PreviewResult.EMPTY;
            return;
        }
        final List<_Scenario> sortedRoots = new ArrayList<>(branches);
        sortedRoots.sort(Comparator.comparing(_Scenario::getSimpleName).thenComparing(_Scenario::getName));

        final Set<_Scenario> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final _Scenario root : sortedRoots) {
            final ScenarioBlueprint bp = buildBlueprint(root, null, ancestors, sourceBaseDir);
            if (null != bp) {
                bp.recomputeSubtreeSize();
                roots.add(bp);
            }
        }
        recomputePreview();
    }

    /**
     * Loads an already-constructed blueprint forest (useful for unit tests without classpath scanning).
     */
    public void loadBlueprints(final List<ScenarioBlueprint> rootBlueprints, final Map<String, Offsets> offsetsByClass) {
        clear();
        for (final ScenarioBlueprint root : rootBlueprints) {
            root.recomputeSubtreeSize();
            roots.add(root);
            registerBlueprintTree(root, null, offsetsByClass);
        }
        recomputePreview();
    }

    private void registerBlueprintTree(
            final ScenarioBlueprint node,
            final String parentClass,
            final Map<String, Offsets> offsetsByClass) {
        blueprintByClassName.putIfAbsent(node.className(), node);
        if (null != parentClass) {
            primaryParentByClassName.putIfAbsent(node.className(), parentClass);
        }
        final Offsets off = (null != offsetsByClass)
                ? offsetsByClass.getOrDefault(node.className(), Offsets.ZERO)
                : Offsets.ZERO;
        initialOffsets.putIfAbsent(node.className(), off);
        currentOffsets.putIfAbsent(node.className(), off);
        for (final ScenarioBlueprint child : node.children()) {
            registerBlueprintTree(child, node.className(), offsetsByClass);
        }
    }

    private ScenarioBlueprint buildBlueprint(
            final _Scenario scenario,
            final String parentClass,
            final Set<_Scenario> ancestors,
            final Path sourceBaseDir) {
        if (null == scenario || !ancestors.add(scenario)) {
            return null;
        }
        final String className = scenario.getClass().getName();
        final String simpleName = scenario.getSimpleName();
        final String displayLabel = b(simpleName, scenario.type().tag());
        final ScenarioBlueprint bp = new ScenarioBlueprint(className, simpleName, displayLabel, scenario);

        blueprintByClassName.putIfAbsent(className, bp);
        if (null != parentClass) {
            primaryParentByClassName.putIfAbsent(className, parentClass);
        }

        if (!initialOffsets.containsKey(className)) {
            final VisualOffset vo = scenario.getClass().getAnnotation(VisualOffset.class);
            final VisualRowOffset vro = scenario.getClass().getAnnotation(VisualRowOffset.class);
            final Offsets annotationFallback = new Offsets(
                    null != vo ? vo.value() : 0,
                    null != vro ? vro.value() : 0);
            final Path sourcePath = (null != sourceBaseDir)
                    ? ScenarioOffsetSourceCodec.resolveSourcePath(sourceBaseDir, className)
                    : null;
            final Offsets resolved = ScenarioOffsetSourceCodec.readOffsets(sourcePath, annotationFallback);
            initialOffsets.put(className, resolved);
            currentOffsets.put(className, resolved);
        }

        if (scenario instanceof _Node parentNode) {
            final List<KeyValue<_Scenario, org.apache.commons.math3.fraction.Fraction>> pairs =
                    new ArrayList<>(parentNode.getPool().getPairList());
            pairs.sort(Comparator.comparing((KeyValue<_Scenario, org.apache.commons.math3.fraction.Fraction> p) -> p.k().getSimpleName())
                    .thenComparing(p -> p.k().getName()));
            for (final var pair : pairs) {
                final ScenarioBlueprint childBp = buildBlueprint(pair.k(), className, ancestors, sourceBaseDir);
                if (null != childBp) {
                    bp.children().add(childBp);
                }
            }
        }
        ancestors.remove(scenario);
        return bp;
    }

    public void clear() {
        roots.clear();
        blueprintByClassName.clear();
        primaryParentByClassName.clear();
        initialOffsets.clear();
        currentOffsets.clear();
        undoStack.clear();
        redoStack.clear();
        latestPreview = PreviewResult.EMPTY;
    }

    public Offsets initialOffsets(final String className) {
        return initialOffsets.getOrDefault(className, Offsets.ZERO);
    }

    public Offsets currentOffsets(final String className) {
        return currentOffsets.getOrDefault(className, Offsets.ZERO);
    }

    public boolean isShifted(final String className) {
        return !currentOffsets(className).isZero();
    }

    public boolean isModified(final String className) {
        return !currentOffsets(className).equals(initialOffsets(className));
    }

    public int shiftedCount() {
        int count = 0;
        for (final Offsets o : currentOffsets.values()) {
            if (!o.isZero()) count++;
        }
        return count;
    }

    public int modifiedCount() {
        int count = 0;
        for (final String className : currentOffsets.keySet()) {
            if (isModified(className)) count++;
        }
        return count;
    }

    public List<OffsetChange> modifiedChanges() {
        final List<OffsetChange> changes = new ArrayList<>();
        for (final Map.Entry<String, Offsets> entry : currentOffsets.entrySet()) {
            final String className = entry.getKey();
            final Offsets initial = initialOffsets(className);
            final Offsets current = entry.getValue();
            if (!current.equals(initial)) {
                final ScenarioBlueprint bp = blueprintByClassName.get(className);
                final String simple = (null != bp) ? bp.simpleName() : className;
                changes.add(new OffsetChange(className, simple, initial, current));
            }
        }
        return changes;
    }

    /**
     * Updates a scenario's offsets and records an undoable history step.
     *
     * @return {@code true} if the offsets actually changed
     */
    public boolean setOffsets(final String className, final int colOffset, final int rowOffset) {
        if (!currentOffsets.containsKey(className)) return false;
        final Offsets next = new Offsets(Math.max(0, colOffset), rowOffset);
        final Offsets prev = currentOffsets(className);
        if (next.equals(prev)) return false;

        undoStack.push(new HistoryEntry(Map.of(className, prev), Map.of(className, next)));
        redoStack.clear();
        currentOffsets.put(className, next);
        recomputePreview();
        return true;
    }

    public boolean nudgeOffsets(final String className, final int deltaCol, final int deltaRow) {
        final Offsets cur = currentOffsets(className);
        return setOffsets(className, Math.max(0, cur.colOffset() + deltaCol), cur.rowOffset() + deltaRow);
    }

    public boolean revertNode(final String className) {
        final Offsets initial = initialOffsets(className);
        return setOffsets(className, initial.colOffset(), initial.rowOffset());
    }

    public boolean revertAll() {
        final Map<String, Offsets> before = new LinkedHashMap<>();
        final Map<String, Offsets> after = new LinkedHashMap<>();
        for (final String className : currentOffsets.keySet()) {
            final Offsets cur = currentOffsets(className);
            final Offsets init = initialOffsets(className);
            if (!cur.equals(init)) {
                before.put(className, cur);
                after.put(className, init);
            }
        }
        if (before.isEmpty()) return false;
        undoStack.push(new HistoryEntry(before, after));
        redoStack.clear();
        currentOffsets.putAll(after);
        recomputePreview();
        return true;
    }

    /**
     * Resets every non-zero shift in the tree to {@link Offsets#ZERO} as a single undoable step.
     *
     * @return {@code true} if at least one scenario had a non-zero offset
     */
    public boolean resetAllToZero() {
        final Map<String, Offsets> before = new LinkedHashMap<>();
        final Map<String, Offsets> after = new LinkedHashMap<>();
        for (final String className : currentOffsets.keySet()) {
            final Offsets cur = currentOffsets(className);
            if (!cur.isZero()) {
                before.put(className, cur);
                after.put(className, Offsets.ZERO);
            }
        }
        if (before.isEmpty()) return false;
        undoStack.push(new HistoryEntry(before, after));
        redoStack.clear();
        currentOffsets.putAll(after);
        recomputePreview();
        return true;
    }

    /**
     * Returns an independent snapshot of all current scenario offsets.
     */
    public Map<String, Offsets> snapshotCurrentOffsets() {
        return new LinkedHashMap<>(currentOffsets);
    }

    /**
     * Applies a batch of target offsets as a single undoable history entry.
     * Unknown class names are ignored; negative column offsets are clamped to {@code 0}.
     *
     * @return {@code true} if at least one scenario's offsets changed
     */
    public boolean applyOffsetsBatch(final Map<String, Offsets> targetOffsets) {
        if (null == targetOffsets || targetOffsets.isEmpty()) return false;
        final Map<String, Offsets> before = new LinkedHashMap<>();
        final Map<String, Offsets> after = new LinkedHashMap<>();
        for (final String className : currentOffsets.keySet()) {
            if (!targetOffsets.containsKey(className)) continue;
            final Offsets raw = targetOffsets.get(className);
            final Offsets next = (null == raw) ? Offsets.ZERO : new Offsets(Math.max(0, raw.colOffset()), raw.rowOffset());
            final Offsets cur = currentOffsets(className);
            if (!cur.equals(next)) {
                before.put(className, cur);
                after.put(className, next);
            }
        }
        if (before.isEmpty()) return false;
        undoStack.push(new HistoryEntry(before, after));
        redoStack.clear();
        currentOffsets.putAll(after);
        recomputePreview();
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        final HistoryEntry entry = undoStack.pop();
        currentOffsets.putAll(entry.before());
        redoStack.push(entry);
        recomputePreview();
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        final HistoryEntry entry = redoStack.pop();
        currentOffsets.putAll(entry.after());
        undoStack.push(entry);
        recomputePreview();
        return true;
    }

    /**
     * Marks a saved class as clean (updating its baseline to its current offsets).
     */
    public void markSaved(final String className) {
        if (currentOffsets.containsKey(className)) {
            initialOffsets.put(className, currentOffsets.get(className));
        }
    }

    /**
     * Recomputes the preview layout strictly from cached blueprints without touching
     * {@link works.lysenko.base.output.tree.TreeLayoutComputer#offsetOverrides}.
     */
    public PreviewResult recomputePreview() {
        latestPreview = computePreview(roots, currentOffsets, previewSettings, false);
        return latestPreview;
    }

    /**
     * Computes a layout snapshot for the supplied blueprints, offsets, and preview settings.
     * Safe to invoke from background worker threads when {@code lightweight} is {@code true}
     * (skips constructing {@link Result} wrappers around {@link _Scenario} instances).
     */
    static PreviewResult computePreview(
            final List<ScenarioBlueprint> roots,
            final Map<String, Offsets> offsetsByClass,
            final PreviewSettings settings,
            final boolean lightweight) {
        if (null == roots || roots.isEmpty()) {
            return PreviewResult.EMPTY;
        }

        final List<NodeData> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();
        final Map<String, NodeData> nodeById = new LinkedHashMap<>();
        final Map<String, String> classNameByNodeId = new HashMap<>();
        final Map<String, List<NodeData>> nodesByClassName = new HashMap<>();
        final int[] rowCounter = {1};

        for (final ScenarioBlueprint root : roots) {
            instantiateNodes(root, null, 0, rowCounter, offsetsByClass, lightweight,
                    nodes, edges, nodeById, classNameByNodeId, nodesByClassName);
        }

        final Map<String, Point2DGrid> rawPos = lightweight ? Map.of() : new HashMap<>(nodes.size() * 2);

        synchronized (OPTIMIZATION_LOCK) {
            final boolean origHOff = Optimization.reduceHorizontalOffsets;
            final boolean origVOff = Optimization.reduceVerticalOffsets;
            final boolean origHGaps = Optimization.reduceHorizontalGaps;
            final boolean origVGaps = Optimization.reduceVerticalGaps;
            final int origMinSep = Optimization.minOffsetSeparation;
            final boolean origDynamicReordering = Optimization.allowDynamicReordering;
            try {
                Optimization.reduceHorizontalOffsets = settings.reduceHorizontalOffsets;
                Optimization.reduceVerticalOffsets = settings.reduceVerticalOffsets;
                Optimization.reduceHorizontalGaps = settings.reduceHorizontalGaps;
                Optimization.reduceVerticalGaps = settings.reduceVerticalGaps;
                Optimization.minOffsetSeparation = Math.max(0, settings.minOffsetSeparation);
                Optimization.allowDynamicReordering = true;

                TreeLayoutOptimizer.optimizeLayout(nodes);
                if (!lightweight) {
                    for (final NodeData n : nodes) {
                        rawPos.put(n.id(), new Point2DGrid(n.col(), n.row()));
                    }
                }

                if (settings.compacted) {
                    TreeLayoutOptimizer.reduceVisualOffsets(nodes);
                    TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);
                }
            } finally {
                Optimization.reduceHorizontalOffsets = origHOff;
                Optimization.reduceVerticalOffsets = origVOff;
                Optimization.reduceHorizontalGaps = origHGaps;
                Optimization.reduceVerticalGaps = origVGaps;
                Optimization.minOffsetSeparation = origMinSep;
                Optimization.allowDynamicReordering = origDynamicReordering;
            }
        }

        int maxCol = 0;
        double maxRow = 0.0;
        for (final NodeData n : nodes) {
            if (n.col() > maxCol) maxCol = n.col();
            if (n.row() > maxRow) maxRow = n.row();
        }
        final int overlaps = countOverlaps(nodes);

        return new PreviewResult(
                new TreeLayout(nodes, edges),
                Collections.unmodifiableMap(nodeById),
                Collections.unmodifiableMap(classNameByNodeId),
                Collections.unmodifiableMap(nodesByClassName),
                lightweight ? Map.of() : Collections.unmodifiableMap(rawPos),
                nodes.isEmpty() ? 0 : (maxCol + 1),
                maxRow,
                overlaps);
    }

    private static void instantiateNodes(
            final ScenarioBlueprint bp,
            final NodeData parent,
            final int col,
            final int[] rowCounter,
            final Map<String, Offsets> offsetsByClass,
            final boolean lightweight,
            final List<NodeData> nodes,
            final List<Edge> edges,
            final Map<String, NodeData> nodeById,
            final Map<String, String> classNameByNodeId,
            final Map<String, List<NodeData>> nodesByClassName) {

        final Offsets off = offsetsByClass.getOrDefault(bp.className(), Offsets.ZERO);
        final int myCol = col + off.colOffset();
        final String id = s("scenario_", nodes.size(), "_", bp.simpleName());
        final String group = (null == parent) ? TESTS : parent.label();
        final Result previewResult = (!lightweight && null != bp.scenario())
                ? new Result(bp.scenario(), bp.scenario().type(), bp.scenario().weightConfigured(),
                bp.scenario().weightUpstream(), bp.scenario().weightDownstream(), Collections.emptyList(), 1, 0)
                : null;
        final NodeData node = new NodeData(
                id,
                bp.displayLabel(),
                group,
                myCol,
                rowCounter[0]++,
                off.colOffset(),
                off.rowOffset(),
                previewResult,
                lightweight ? null : bp.scenario());

        nodes.add(node);
        nodeById.put(id, node);
        classNameByNodeId.put(id, bp.className());
        nodesByClassName.computeIfAbsent(bp.className(), ignored -> new ArrayList<>()).add(node);

        if (null != parent) {
            node.setParent(parent);
            parent.children().add(node);
            edges.add(new Edge(parent, node));
        }

        for (final ScenarioBlueprint child : bp.children()) {
            instantiateNodes(child, node, myCol + 1, rowCounter, offsetsByClass, lightweight,
                    nodes, edges, nodeById, classNameByNodeId, nodesByClassName);
        }
    }

    private static int countOverlaps(final List<NodeData> nodes) {
        int overlaps = 0;
        for (int i = 0; i < nodes.size(); i++) {
            final NodeData a = nodes.get(i);
            for (int j = i + 1; j < nodes.size(); j++) {
                final NodeData b = nodes.get(j);
                if (a.col() == b.col() && Math.abs(a.row() - b.row()) < 1.0 - 1.0e-5) {
                    overlaps++;
                }
            }
        }
        return overlaps;
    }
}
