package works.lysenko.base.ui.tree;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.tree.TreeLayoutOptimizer;
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
import java.util.Collection;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.spec.Symbols.UND_SCR;

/**
 * Pure data model, undo/redo history, and isolated preview layout engine for
 * {@link works.lysenko.base.ui.TreeLayoutEditorPanel}.
 */
public final class TreeLayoutEditorModel {

    private static final Lock OPTIMIZATION_LOCK = new ReentrantLock();
    private static final int DEFAULT_CHILD_CAPACITY = 4;
    private static final int DEFAULT_ROOT_CAPACITY = 8;
    private static final int DEFAULT_HISTORY_CAPACITY = 16;
    private static final int DEFAULT_MAP_CAPACITY = 32;
    private static final double OVERLAP_ROW_THRESHOLD = 1.0 - 1.0e-5;

    /**
     * Static scenario blueprint captured once on load so preview recomputations never rescan the classpath.
     */
    public static final class ScenarioBlueprint {

        private final String className;
        private final String simpleName;
        private final String displayLabel;
        private final _Scenario scenario;
        private final List<ScenarioBlueprint> children = new ArrayList<>(DEFAULT_CHILD_CAPACITY);
        private int subtreeSize = 1;

        /**
         * Creates a scenario blueprint node.
         *
         * @param className    fully qualified scenario class name
         * @param simpleName   simple scenario name
         * @param displayLabel formatted label shown in preview nodes
         * @param scenario     underlying scenario instance, or {@code null} in synthetic tests
         */
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

        /**
         * Returns the fully qualified scenario class name.
         *
         * @return class name
         */
        public String className() {
            return className;
        }

        /**
         * Returns the simple scenario name.
         *
         * @return simple name
         */
        public String simpleName() {
            return simpleName;
        }

        String displayLabel() {
            return displayLabel;
        }

        /**
         * Returns the backing scenario instance if available.
         *
         * @return scenario instance or {@code null}
         */
        public _Scenario scenario() {
            return scenario;
        }

        /**
         * Returns the mutable list of child blueprints.
         *
         * @return child blueprints
         */
        public List<ScenarioBlueprint> children() {
            return children;
        }

        /**
         * Returns whether this blueprint has no children.
         *
         * @return {@code true} if this blueprint is a leaf
         */
        public boolean isLeaf() {
            return children.isEmpty();
        }

        /**
         * Returns the total number of nodes in this blueprint's subtree, including itself.
         *
         * @return subtree node count
         */
        public int subtreeSize() {
            return subtreeSize;
        }

        void recomputeSubtreeSize() {
            int count = 1;
            for (final ScenarioBlueprint child : children) {
                child.recomputeSubtreeSize();
                count += child.subtreeSize();
            }
            subtreeSize = count;
        }
    }

    /**
     * Isolated preview compaction parameters that do not permanently mutate {@link Optimization}.
     */
    public static final class PreviewSettings {

        /** Whether post-layout compaction passes are applied in preview. */
        public boolean compacted = true;
        /** Whether horizontal visual offset reduction is enabled during compaction. */
        public boolean reduceHorizontalOffsets = Optimization.reduceHorizontalOffsets;
        /** Whether vertical visual offset reduction is enabled during compaction. */
        public boolean reduceVerticalOffsets = Optimization.reduceVerticalOffsets;
        /** Whether empty horizontal gap removal is enabled during compaction. */
        public boolean reduceHorizontalGaps = Optimization.reduceHorizontalGaps;
        /** Whether empty vertical gap removal is enabled during compaction. */
        public boolean reduceVerticalGaps = Optimization.reduceVerticalGaps;
        /** Minimum separation preserved when reducing visual offsets. */
        public int minOffsetSeparation = Optimization.minOffsetSeparation;

        /**
         * Creates an independent copy of these preview settings.
         *
         * @return copied preview settings
         */
        public PreviewSettings copy() {
            final PreviewSettings copy = new PreviewSettings();
            copy.compacted = compacted;
            copy.reduceHorizontalOffsets = reduceHorizontalOffsets;
            copy.reduceVerticalOffsets = reduceVerticalOffsets;
            copy.reduceHorizontalGaps = reduceHorizontalGaps;
            copy.reduceVerticalGaps = reduceVerticalGaps;
            copy.minOffsetSeparation = minOffsetSeparation;
            return copy;
        }
    }

    /**
     * Snapshot of a single layout preview pass (both raw post-optimizeLayout and final compacted coordinates).
     *
     * @param layout              computed tree layout containing nodes and edges
     * @param nodeById            lookup map from node id to node data
     * @param classNameByNodeId   lookup map from node id to scenario class name
     * @param nodesByClassName    lookup map from scenario class name to instantiated nodes
     * @param rawPositionByNodeId pre-compaction grid positions keyed by node id
     * @param totalCols           total column span of the layout
     * @param totalRows           maximum row coordinate of the layout
     * @param overlapCount        number of overlapping node pairs
     */
    public record PreviewResult(
            TreeHtml.TreeLayout layout,
            Map<String, TreeHtml.NodeData> nodeById,
            Map<String, String> classNameByNodeId,
            Map<String, List<TreeHtml.NodeData>> nodesByClassName,
            Map<String, Point2DGrid> rawPositionByNodeId,
            int totalCols,
            double totalRows,
            int overlapCount) {

        static final PreviewResult EMPTY = new PreviewResult(
                new TreeHtml.TreeLayout(List.of(), List.of()),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                0,
                0.0,
                0);
    }

    /**
     * Grid coordinate pair representing a node's column and row position.
     *
     * @param col zero-based column index
     * @param row one-based row coordinate
     */
    public record Point2DGrid(int col, double row) {
    }

    /**
     * Description of a scenario whose offsets differ from its saved baseline.
     *
     * @param className  fully qualified scenario class name
     * @param simpleName simple scenario name
     * @param initial    initial baseline offsets
     * @param current    current edited offsets
     */
    public record OffsetChange(
            String className,
            String simpleName,
            ScenarioOffsetSourceCodec.Offsets initial,
            ScenarioOffsetSourceCodec.Offsets current) {
    }

    private record HistoryEntry(
            Map<String, ScenarioOffsetSourceCodec.Offsets> before,
            Map<String, ScenarioOffsetSourceCodec.Offsets> after) {
    }

    private final List<ScenarioBlueprint> roots = new ArrayList<>(DEFAULT_ROOT_CAPACITY);
    private final Map<String, ScenarioBlueprint> blueprintByClassName = new TreeMap<>();
    private final Map<String, String> primaryParentByClassName = new HashMap<>(DEFAULT_MAP_CAPACITY);
    private final Map<String, ScenarioOffsetSourceCodec.Offsets> initialOffsets = new TreeMap<>();
    private final Map<String, ScenarioOffsetSourceCodec.Offsets> currentOffsets = new TreeMap<>();
    private final Deque<HistoryEntry> undoStack = new ArrayDeque<>(DEFAULT_HISTORY_CAPACITY);
    private final Deque<HistoryEntry> redoStack = new ArrayDeque<>(DEFAULT_HISTORY_CAPACITY);
    private final PreviewSettings previewSettings = new PreviewSettings();
    private PreviewResult latestPreview = PreviewResult.EMPTY;

    /**
     * Returns the mutable preview settings used when recomputing layouts.
     *
     * @return preview settings
     */
    public PreviewSettings previewSettings() {
        return previewSettings;
    }

    /**
     * Returns an unmodifiable view of the loaded root scenario blueprints.
     *
     * @return root blueprints
     */
    public List<ScenarioBlueprint> roots() {
        return Collections.unmodifiableList(roots);
    }

    /**
     * Returns an unmodifiable map of loaded blueprints keyed by class name.
     *
     * @return blueprints by class name
     */
    public Map<String, ScenarioBlueprint> blueprintsByClassName() {
        return Collections.unmodifiableMap(blueprintByClassName);
    }

    /**
     * Looks up a blueprint by its fully qualified class name.
     *
     * @param className scenario class name
     * @return matching blueprint, or {@code null} if not loaded
     */
    public ScenarioBlueprint blueprint(final String className) {
        return blueprintByClassName.get(className);
    }

    /**
     * Returns the primary parent class name for the given scenario class.
     *
     * @param className scenario class name
     * @return parent class name, or {@code null} for root scenarios
     */
    public String parentClassName(final String className) {
        return primaryParentByClassName.get(className);
    }

    /**
     * Returns whether no scenarios are currently loaded in the model.
     *
     * @return {@code true} if no scenarios are loaded
     */
    public boolean isEmpty() {
        return blueprintByClassName.isEmpty();
    }

    /**
     * Returns the most recently computed layout preview snapshot.
     *
     * @return latest preview result
     */
    public PreviewResult latestPreview() {
        return latestPreview;
    }

    /**
     * Loads scenarios from {@code rootPackage}, reading initial offsets from Java sources under {@code sourceBaseDir}.
     *
     * @param rootPackage   root Java package to scan for scenarios
     * @param sourceBaseDir base directory containing scenario source files, or {@code null}
     */
    public void loadFromPackage(final String rootPackage, final Path sourceBaseDir) {
        clear();
        if (new BlueprintLoader(roots, blueprintByClassName, primaryParentByClassName, initialOffsets, currentOffsets)
                .isPackageLoaded(rootPackage, sourceBaseDir)) {
            recomputePreview();
        }
    }

    /**
     * Loads an already-constructed blueprint forest (useful for unit tests without classpath scanning).
     *
     * @param rootBlueprints root scenario blueprints to load
     * @param offsetsByClass initial offsets keyed by class name, or {@code null}
     */
    public void loadBlueprints(
            final Iterable<ScenarioBlueprint> rootBlueprints,
            final Map<String, ScenarioOffsetSourceCodec.Offsets> offsetsByClass) {
        clear();
        new BlueprintLoader(roots, blueprintByClassName, primaryParentByClassName, initialOffsets, currentOffsets)
                .loadBlueprints(rootBlueprints, offsetsByClass);
        recomputePreview();
    }

    private record BlueprintLoader(
            Collection<? super ScenarioBlueprint> roots,
            Map<? super String, ? super ScenarioBlueprint> blueprintByClassName,
            Map<? super String, ? super String> primaryParentByClassName,
            Map<? super String, ? super ScenarioOffsetSourceCodec.Offsets> initialOffsets,
            Map<? super String, ? super ScenarioOffsetSourceCodec.Offsets> currentOffsets) {

        private boolean isPackageLoaded(final String rootPackage, final Path sourceBaseDir) {
            boolean loaded = false;
            if (isNonBlank(rootPackage)) {
                loaded = isBranchesLoaded(rootPackage, sourceBaseDir);
            }
            return loaded;
        }

        private static boolean isNonBlank(final String value) {
            return null != value && !value.isBlank();
        }

        private boolean isBranchesLoaded(final String rootPackage, final Path sourceBaseDir) {
            boolean loaded = false;
            final Tree tree = new Tree(rootPackage);
            final Set<_Scenario> branches = tree.getBranches();
            if (null != branches && !branches.isEmpty()) {
                populatePackageRoots(branches, sourceBaseDir);
                loaded = true;
            }
            return loaded;
        }

        private void populatePackageRoots(final Collection<? extends _Scenario> branches, final Path sourceBaseDir) {
            final List<_Scenario> sortedRoots = new ArrayList<>(branches);
            sortedRoots.sort(Comparator.comparing(_Scenario::getSimpleName).thenComparing(_Scenario::getName));
            final Set<_Scenario> ancestors = Collections.newSetFromMap(new IdentityHashMap<>(DEFAULT_MAP_CAPACITY));
            for (final _Scenario root : sortedRoots) {
                final ScenarioBlueprint bp = buildBlueprint(root, null, ancestors, sourceBaseDir);
                if (null != bp) {
                    bp.recomputeSubtreeSize();
                    roots.add(bp);
                }
            }
        }

        private void loadBlueprints(
                final Iterable<ScenarioBlueprint> rootBlueprints,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> offsetsByClass) {
            for (final ScenarioBlueprint root : rootBlueprints) {
                root.recomputeSubtreeSize();
                roots.add(root);
                registerBlueprintTree(root, null, offsetsByClass);
            }
        }

        private void registerBlueprintTree(
                final ScenarioBlueprint node,
                final String parentClass,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> offsetsByClass) {
            blueprintByClassName.putIfAbsent(node.className(), node);
            if (null != parentClass) {
                primaryParentByClassName.putIfAbsent(node.className(), parentClass);
            }
            final ScenarioOffsetSourceCodec.Offsets off = (null == offsetsByClass)
                    ? ScenarioOffsetSourceCodec.Offsets.ZERO
                    : offsetsByClass.getOrDefault(node.className(), ScenarioOffsetSourceCodec.Offsets.ZERO);
            initialOffsets.putIfAbsent(node.className(), off);
            currentOffsets.putIfAbsent(node.className(), off);
            for (final ScenarioBlueprint child : node.children()) {
                registerBlueprintTree(child, node.className(), offsetsByClass);
            }
        }

        private ScenarioBlueprint buildBlueprint(
                final _Scenario scenario,
                final String parentClass,
                final Set<? super _Scenario> ancestors,
                final Path sourceBaseDir) {
            ScenarioBlueprint bp = null;
            if (null != scenario && ancestors.add(scenario)) {
                final String className = scenario.getClass().getName();
                final String simpleName = scenario.getSimpleName();
                final String displayLabel = b(simpleName, scenario.type().tag());
                bp = new ScenarioBlueprint(className, simpleName, displayLabel, scenario);
                registerScannedBlueprint(bp, scenario, parentClass, sourceBaseDir);
                populateBlueprintChildren(bp, scenario, ancestors, sourceBaseDir);
                ancestors.remove(scenario);
            }
            return bp;
        }

        private void registerScannedBlueprint(
                final ScenarioBlueprint bp,
                final _Scenario scenario,
                final String parentClass,
                final Path sourceBaseDir) {
            final String className = bp.className();
            blueprintByClassName.putIfAbsent(className, bp);
            if (null != parentClass) {
                primaryParentByClassName.putIfAbsent(className, parentClass);
            }
            if (!initialOffsets.containsKey(className)) {
                final ScenarioOffsetSourceCodec.Offsets resolved = resolveInitialOffsets(scenario, className, sourceBaseDir);
                initialOffsets.put(className, resolved);
                currentOffsets.put(className, resolved);
            }
        }

        private static ScenarioOffsetSourceCodec.Offsets resolveInitialOffsets(
                final _Scenario scenario,
                final String className,
                final Path sourceBaseDir) {
            final VisualOffset vo = scenario.getClass().getAnnotation(VisualOffset.class);
            final VisualRowOffset vro = scenario.getClass().getAnnotation(VisualRowOffset.class);
            final int colOff = (null == vo) ? 0 : vo.value();
            final int rowOff = (null == vro) ? 0 : vro.value();
            final ScenarioOffsetSourceCodec.Offsets annotationFallback = new ScenarioOffsetSourceCodec.Offsets(colOff, rowOff);
            final Path sourcePath = (null == sourceBaseDir)
                    ? null
                    : ScenarioOffsetSourceCodec.resolveSourcePath(sourceBaseDir, className);
            return ScenarioOffsetSourceCodec.readOffsets(sourcePath, annotationFallback);
        }

        private void populateBlueprintChildren(
                final ScenarioBlueprint bp,
                final _Scenario scenario,
                final Set<? super _Scenario> ancestors,
                final Path sourceBaseDir) {
            if (scenario instanceof final _Node parentNode) {
                final List<KeyValue<_Scenario, Fraction>> pairs = new ArrayList<>(parentNode.getPool().getPairList());
                pairs.sort(Comparator.comparing((final KeyValue<_Scenario, Fraction> pair) -> pair.k().getSimpleName())
                        .thenComparing(pair -> pair.k().getName()));
                for (final KeyValue<_Scenario, Fraction> pair : pairs) {
                    final ScenarioBlueprint childBp = buildBlueprint(pair.k(), bp.className(), ancestors, sourceBaseDir);
                    if (null != childBp) {
                        bp.children().add(childBp);
                    }
                }
            }
        }
    }

    private void clear() {
        roots.clear();
        blueprintByClassName.clear();
        primaryParentByClassName.clear();
        initialOffsets.clear();
        currentOffsets.clear();
        undoStack.clear();
        redoStack.clear();
        latestPreview = PreviewResult.EMPTY;
    }

    ScenarioOffsetSourceCodec.Offsets initialOffsets(final String className) {
        return initialOffsets.getOrDefault(className, ScenarioOffsetSourceCodec.Offsets.ZERO);
    }

    /**
     * Returns the current edited offsets for the given scenario class.
     *
     * @param className scenario class name
     * @return current offsets, or {@link ScenarioOffsetSourceCodec.Offsets#ZERO} if unknown
     */
    public ScenarioOffsetSourceCodec.Offsets currentOffsets(final String className) {
        return currentOffsets.getOrDefault(className, ScenarioOffsetSourceCodec.Offsets.ZERO);
    }

    /**
     * Returns whether the given scenario currently has a non-zero visual offset.
     *
     * @param className scenario class name
     * @return {@code true} if either column or row offset is non-zero
     */
    public boolean isShifted(final String className) {
        return !currentOffsets(className).isZero();
    }

    /**
     * Returns whether the given scenario's current offsets differ from its initial baseline.
     *
     * @param className scenario class name
     * @return {@code true} if the scenario has unsaved offset changes
     */
    public boolean isModified(final String className) {
        return !currentOffsets(className).equals(initialOffsets(className));
    }

    /**
     * Counts scenarios that currently have non-zero visual offsets.
     *
     * @return number of shifted scenarios
     */
    public int shiftedCount() {
        int count = 0;
        for (final ScenarioOffsetSourceCodec.Offsets offsets : currentOffsets.values()) {
            if (!offsets.isZero()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts scenarios whose current offsets differ from their initial baseline.
     *
     * @return number of modified scenarios
     */
    public int modifiedCount() {
        int count = 0;
        for (final String className : currentOffsets.keySet()) {
            if (isModified(className)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns all scenarios whose offsets currently differ from their initial baseline.
     *
     * @return list of offset changes
     */
    public List<OffsetChange> modifiedChanges() {
        final List<OffsetChange> changes = new ArrayList<>(DEFAULT_HISTORY_CAPACITY);
        for (final Map.Entry<String, ScenarioOffsetSourceCodec.Offsets> entry : currentOffsets.entrySet()) {
            final String className = entry.getKey();
            final ScenarioOffsetSourceCodec.Offsets initial = initialOffsets(className);
            final ScenarioOffsetSourceCodec.Offsets current = entry.getValue();
            if (!current.equals(initial)) {
                changes.add(createOffsetChange(className, initial, current));
            }
        }
        return changes;
    }

    private OffsetChange createOffsetChange(
            final String className,
            final ScenarioOffsetSourceCodec.Offsets initial,
            final ScenarioOffsetSourceCodec.Offsets current) {
        final ScenarioBlueprint bp = blueprintByClassName.get(className);
        final String simple = (null == bp) ? className : bp.simpleName();
        return new OffsetChange(className, simple, initial, current);
    }

    /**
     * Updates a scenario's offsets and records an undoable history step.
     *
     * @param className scenario class name
     * @param colOffset new column offset (clamped to {@code >= 0})
     * @param rowOffset new row offset
     * @return {@code true} if the offsets actually changed
     */
    public boolean setOffsets(final String className, final int colOffset, final int rowOffset) {
        boolean changed = false;
        if (currentOffsets.containsKey(className)) {
            final ScenarioOffsetSourceCodec.Offsets next =
                    new ScenarioOffsetSourceCodec.Offsets(Math.max(0, colOffset), rowOffset);
            final ScenarioOffsetSourceCodec.Offsets prev = currentOffsets(className);
            if (!next.equals(prev)) {
                undoStack.push(new HistoryEntry(Map.of(className, prev), Map.of(className, next)));
                redoStack.clear();
                currentOffsets.put(className, next);
                recomputePreview();
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Adjusts a scenario's offsets by the given column and row deltas.
     *
     * @param className scenario class name
     * @param deltaCol  column delta to add
     * @param deltaRow  row delta to add
     * @return {@code true} if the offsets actually changed
     */
    public boolean nudgeOffsets(final String className, final int deltaCol, final int deltaRow) {
        final ScenarioOffsetSourceCodec.Offsets cur = currentOffsets(className);
        return setOffsets(className, Math.max(0, cur.colOffset() + deltaCol), cur.rowOffset() + deltaRow);
    }

    /**
     * Reverts a single scenario back to its initial baseline offsets.
     *
     * @param className scenario class name
     * @return {@code true} if the offsets actually changed
     */
    public boolean revertNode(final String className) {
        final ScenarioOffsetSourceCodec.Offsets initial = initialOffsets(className);
        return setOffsets(className, initial.colOffset(), initial.rowOffset());
    }

    /**
     * Reverts all modified scenarios back to their initial baseline offsets as a single undoable step.
     *
     * @return {@code true} if at least one scenario changed
     */
    public boolean revertAll() {
        final int capacity = Math.max(DEFAULT_HISTORY_CAPACITY, currentOffsets.size());
        final Map<String, ScenarioOffsetSourceCodec.Offsets> before = new LinkedHashMap<>(capacity);
        final Map<String, ScenarioOffsetSourceCodec.Offsets> after = new LinkedHashMap<>(capacity);
        for (final String className : currentOffsets.keySet()) {
            final ScenarioOffsetSourceCodec.Offsets cur = currentOffsets(className);
            final ScenarioOffsetSourceCodec.Offsets init = initialOffsets(className);
            if (!cur.equals(init)) {
                before.put(className, cur);
                after.put(className, init);
            }
        }
        return isBatchCommitted(before, after);
    }

    /**
     * Resets every non-zero shift in the tree to {@link ScenarioOffsetSourceCodec.Offsets#ZERO} as a single undoable step.
     *
     * @return {@code true} if at least one scenario had a non-zero offset
     */
    public boolean resetAllToZero() {
        final int capacity = Math.max(DEFAULT_HISTORY_CAPACITY, currentOffsets.size());
        final Map<String, ScenarioOffsetSourceCodec.Offsets> before = new LinkedHashMap<>(capacity);
        final Map<String, ScenarioOffsetSourceCodec.Offsets> after = new LinkedHashMap<>(capacity);
        for (final String className : currentOffsets.keySet()) {
            final ScenarioOffsetSourceCodec.Offsets cur = currentOffsets(className);
            if (!cur.isZero()) {
                before.put(className, cur);
                after.put(className, ScenarioOffsetSourceCodec.Offsets.ZERO);
            }
        }
        return isBatchCommitted(before, after);
    }

    /**
     * Returns an independent snapshot of all current scenario offsets.
     *
     * @return copy of current offsets keyed by class name
     */
    public Map<String, ScenarioOffsetSourceCodec.Offsets> snapshotCurrentOffsets() {
        return new LinkedHashMap<>(currentOffsets);
    }

    /**
     * Applies a batch of target offsets as a single undoable history entry.
     * Unknown class names are ignored; negative column offsets are clamped to {@code 0}.
     *
     * @param targetOffsets target offsets keyed by scenario class name
     * @return {@code true} if at least one scenario's offsets changed
     */
    public boolean applyOffsetsBatch(final Map<String, ScenarioOffsetSourceCodec.Offsets> targetOffsets) {
        boolean changed = false;
        if (null != targetOffsets && !targetOffsets.isEmpty()) {
            final int capacity = Math.max(DEFAULT_HISTORY_CAPACITY, currentOffsets.size());
            final Map<String, ScenarioOffsetSourceCodec.Offsets> before = new LinkedHashMap<>(capacity);
            final Map<String, ScenarioOffsetSourceCodec.Offsets> after = new LinkedHashMap<>(capacity);
            for (final String className : currentOffsets.keySet()) {
                if (targetOffsets.containsKey(className)) {
                    final ScenarioOffsetSourceCodec.Offsets next = normalizeTargetOffset(targetOffsets.get(className));
                    final ScenarioOffsetSourceCodec.Offsets cur = currentOffsets(className);
                    if (!cur.equals(next)) {
                        before.put(className, cur);
                        after.put(className, next);
                    }
                }
            }
            changed = isBatchCommitted(before, after);
        }
        return changed;
    }

    private static ScenarioOffsetSourceCodec.Offsets normalizeTargetOffset(final ScenarioOffsetSourceCodec.Offsets raw) {
        return (null == raw)
                ? ScenarioOffsetSourceCodec.Offsets.ZERO
                : new ScenarioOffsetSourceCodec.Offsets(Math.max(0, raw.colOffset()), raw.rowOffset());
    }

    private boolean isBatchCommitted(
            final Map<String, ScenarioOffsetSourceCodec.Offsets> before,
            final Map<String, ScenarioOffsetSourceCodec.Offsets> after) {
        boolean committed = false;
        if (!before.isEmpty()) {
            undoStack.push(new HistoryEntry(before, after));
            redoStack.clear();
            currentOffsets.putAll(after);
            recomputePreview();
            committed = true;
        }
        return committed;
    }

    /**
     * Returns whether at least one edit can be undone.
     *
     * @return {@code true} if undo history is non-empty
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Returns whether at least one undone edit can be redone.
     *
     * @return {@code true} if redo history is non-empty
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Undoes the most recent offset edit.
     *
     * @return {@code true} if an edit was undone
     */
    public boolean undo() {
        boolean undone = false;
        if (!undoStack.isEmpty()) {
            final HistoryEntry entry = undoStack.pop();
            currentOffsets.putAll(entry.before());
            redoStack.push(entry);
            recomputePreview();
            undone = true;
        }
        return undone;
    }

    /**
     * Reapplies the most recently undone offset edit.
     *
     * @return {@code true} if an edit was redone
     */
    public boolean redo() {
        boolean redone = false;
        if (!redoStack.isEmpty()) {
            final HistoryEntry entry = redoStack.pop();
            currentOffsets.putAll(entry.after());
            undoStack.push(entry);
            recomputePreview();
            redone = true;
        }
        return redone;
    }

    /**
     * Marks a saved class as clean (updating its baseline to its current offsets).
     *
     * @param className scenario class name whose baseline should be updated
     */
    public void markSaved(final String className) {
        if (currentOffsets.containsKey(className)) {
            initialOffsets.put(className, currentOffsets.get(className));
        }
    }

    /**
     * Recomputes the preview layout strictly from cached blueprints without touching
     * {@link works.lysenko.base.output.tree.TreeLayoutComputer#offsetOverrides}.
     *
     * @return newly computed preview result
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
            final Collection<ScenarioBlueprint> roots,
            final Map<String, ScenarioOffsetSourceCodec.Offsets> offsetsByClass,
            final PreviewSettings settings,
            final boolean lightweight) {
        PreviewResult result = PreviewResult.EMPTY;
        if (null != roots && !roots.isEmpty()) {
            final PreviewBuilder builder = new PreviewBuilder(offsetsByClass, lightweight);
            builder.instantiateRoots(roots);
            result = builder.optimizeAndBuild(settings);
        }
        return result;
    }

    private static final class PreviewBuilder {

        private final List<TreeHtml.NodeData> nodes = new ArrayList<>(DEFAULT_MAP_CAPACITY);
        private final List<TreeHtml.Edge> edges = new ArrayList<>(DEFAULT_MAP_CAPACITY);
        private final Map<String, TreeHtml.NodeData> nodeById = new LinkedHashMap<>(DEFAULT_MAP_CAPACITY);
        private final Map<String, String> classNameByNodeId = new HashMap<>(DEFAULT_MAP_CAPACITY);
        private final Map<String, List<TreeHtml.NodeData>> nodesByClassName = new HashMap<>(DEFAULT_MAP_CAPACITY);
        private final Map<String, ScenarioOffsetSourceCodec.Offsets> offsetsByClass;
        private final boolean lightweight;
        private int nextRow = 1;

        private PreviewBuilder(
                final Map<String, ScenarioOffsetSourceCodec.Offsets> offsetsByClass,
                final boolean lightweight) {
            this.offsetsByClass = offsetsByClass;
            this.lightweight = lightweight;
        }

        private void instantiateRoots(final Iterable<ScenarioBlueprint> rootBlueprints) {
            for (final ScenarioBlueprint root : rootBlueprints) {
                instantiateNode(root, null, 0);
            }
        }

        private void instantiateNode(
                final ScenarioBlueprint bp,
                final TreeHtml.NodeData parent,
                final int col) {
            final ScenarioOffsetSourceCodec.Offsets off =
                    offsetsByClass.getOrDefault(bp.className(), ScenarioOffsetSourceCodec.Offsets.ZERO);
            final int myCol = col + off.colOffset();
            final String id = s(SCENARIO, UND_SCR, nodes.size(), UND_SCR, bp.simpleName());
            final String group = (null == parent) ? TESTS : parent.label();
            final Result previewResult = createPreviewResult(bp.scenario());
            final int row = nextRow;
            nextRow++;
            final TreeHtml.NodeData node = new TreeHtml.NodeData(
                    id,
                    bp.displayLabel(),
                    group,
                    myCol,
                    row,
                    off.colOffset(),
                    off.rowOffset(),
                    previewResult,
                    lightweight ? null : bp.scenario());

            nodes.add(node);
            nodeById.put(id, node);
            classNameByNodeId.put(id, bp.className());
            nodesByClassName.computeIfAbsent(bp.className(), _ -> createNodeBucket()).add(node);

            if (null != parent) {
                node.setParent(parent);
                parent.children().add(node);
                edges.add(new TreeHtml.Edge(parent, node));
            }

            for (final ScenarioBlueprint child : bp.children()) {
                instantiateNode(child, node, myCol + 1);
            }
        }

        private Result createPreviewResult(final _Scenario scenario) {
            final Result result;
            if (!lightweight && null != scenario) {
                result = new Result(
                        scenario,
                        scenario.type(),
                        scenario.weightConfigured(),
                        scenario.weightUpstream(),
                        scenario.weightDownstream(),
                        Collections.emptyList(),
                        1,
                        0);
            } else {
                result = null;
            }
            return result;
        }

        private static List<TreeHtml.NodeData> createNodeBucket() {
            return new ArrayList<>(DEFAULT_CHILD_CAPACITY);
        }

        private PreviewResult optimizeAndBuild(final PreviewSettings settings) {
            final Map<String, Point2DGrid> rawPos = lightweight ? Map.of() : new HashMap<>(nodes.size() << 1);
            runUnderOptimizationLock(nodes, lightweight, settings, rawPos);
            final int totalCols = computeTotalCols();
            final double maxRow = computeMaxRow();
            final int overlaps = countOverlaps(nodes);
            return new PreviewResult(
                    new TreeHtml.TreeLayout(nodes, edges),
                    Collections.unmodifiableMap(nodeById),
                    Collections.unmodifiableMap(classNameByNodeId),
                    Collections.unmodifiableMap(nodesByClassName),
                    lightweight ? Map.of() : Collections.unmodifiableMap(rawPos),
                    totalCols,
                    maxRow,
                    overlaps);
        }

        private static void runUnderOptimizationLock(
                final List<TreeHtml.NodeData> nodes,
                final boolean lightweight,
                final PreviewSettings settings,
                final Map<? super String, ? super Point2DGrid> rawPos) {
            OPTIMIZATION_LOCK.lock();
            try {
                runOptimizationPass(nodes, lightweight, settings, rawPos);
            } finally {
                OPTIMIZATION_LOCK.unlock();
            }
        }

        private static void runOptimizationPass(
                final List<TreeHtml.NodeData> nodes,
                final boolean lightweight,
                final PreviewSettings settings,
                final Map<? super String, ? super Point2DGrid> rawPos) {
            final boolean origHOff = Optimization.reduceHorizontalOffsets;
            final boolean origVOff = Optimization.reduceVerticalOffsets;
            final boolean origHGaps = Optimization.reduceHorizontalGaps;
            final boolean origVGaps = Optimization.reduceVerticalGaps;
            final int origMinSep = Optimization.minOffsetSeparation;
            final boolean origDynamicOrder = Optimization.allowDynamicReordering;
            try {
                Optimization.reduceHorizontalOffsets = settings.reduceHorizontalOffsets;
                Optimization.reduceVerticalOffsets = settings.reduceVerticalOffsets;
                Optimization.reduceHorizontalGaps = settings.reduceHorizontalGaps;
                Optimization.reduceVerticalGaps = settings.reduceVerticalGaps;
                Optimization.minOffsetSeparation = Math.max(0, settings.minOffsetSeparation);
                Optimization.allowDynamicReordering = true;

                TreeLayoutOptimizer.optimizeLayout(nodes);
                if (!lightweight) {
                    captureRawPositions(nodes, rawPos);
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
                Optimization.allowDynamicReordering = origDynamicOrder;
            }
        }

        private static void captureRawPositions(
                final Iterable<TreeHtml.NodeData> nodes,
                final Map<? super String, ? super Point2DGrid> rawPos) {
            nodes.forEach(node -> rawPos.put(node.id(), new Point2DGrid(node.col(), node.row())));
        }

        private int computeTotalCols() {
            int maxCol = 0;
            for (final TreeHtml.NodeData node : nodes) {
                if (maxCol < node.col()) {
                    maxCol = node.col();
                }
            }
            return nodes.isEmpty() ? 0 : (maxCol + 1);
        }

        private double computeMaxRow() {
            double maxRow = 0.0;
            for (final TreeHtml.NodeData node : nodes) {
                if (maxRow < node.row()) {
                    maxRow = node.row();
                }
            }
            return maxRow;
        }

        private static int countOverlaps(final List<TreeHtml.NodeData> nodes) {
            int overlaps = 0;
            for (int i = 0; i < nodes.size(); i++) {
                overlaps += countOverlapsFrom(nodes, i);
            }
            return overlaps;
        }

        private static int countOverlapsFrom(final List<TreeHtml.NodeData> nodes, final int firstIndex) {
            int overlaps = 0;
            final TreeHtml.NodeData first = nodes.get(firstIndex);
            for (int j = firstIndex + 1; j < nodes.size(); j++) {
                final TreeHtml.NodeData second = nodes.get(j);
                if (first.col() == second.col() && OVERLAP_ROW_THRESHOLD > Math.abs(first.row() - second.row())) {
                    overlaps++;
                }
            }
            return overlaps;
        }
    }
}
