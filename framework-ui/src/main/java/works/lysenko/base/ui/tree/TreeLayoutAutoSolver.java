package works.lysenko.base.ui.tree;

import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.ui.tree.ScenarioOffsetSourceCodec.Offsets;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.PreviewResult;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.PreviewSettings;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.ScenarioBlueprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Automatic layout solver that searches for clean, overlap-free {@code @VisualOffset} and
 * {@code @VisualRowOffset} combinations starting from the user's current offsets.
 * <p>
 * Evaluates candidate placements through the real {@link works.lysenko.base.output.tree.TreeLayoutOptimizer}
 * pipeline (via {@link TreeLayoutEditorModel#computePreview}) so the scored geometry matches
 * what the user sees on the canvas and in HTML/SVG reports.
 */
public final class TreeLayoutAutoSolver {

    static final double COL_WIDTH = 260.0;
    static final double ROW_HEIGHT = 56.0;
    static final double CARD_WIDTH = 220.0;
    static final double CARD_HEIGHT = 44.0;

    private static final double OVERLAP_PENALTY = 1_000_000_000.0;
    private static final double COL_OFFSET_ANNOTATION_PENALTY = 24.0;
    private static final double ROW_OFFSET_ANNOTATION_PENALTY = 18.0;
    private static final double OFFSET_MAGNITUDE_PENALTY = 0.6;
    private static final double OCCLUSION_PENALTY = 28.0;
    private static final double CROSSING_PENALTY = 22.0;
    private static final double LONG_COL_EDGE_PENALTY = 4.0;
    private static final double LONG_ROW_EDGE_PENALTY = 1.5;

    private static final int MAX_CANDIDATES = 26;
    private static final int MAX_COL_OFFSET = 20;
    private static final int MAX_ROW_OFFSET = 48;
    private static final int MAX_SWEEPS = 3;
    private static final long TIME_BUDGET_MS = 3_500L;
    private static final long SECONDARY_ORDER_CUTOFF_MS = 1_000L;

    private TreeLayoutAutoSolver() {
    }

    /**
     * Selectable target proportions for automatic layout optimization.
     */
    public enum AspectPreset {
        WIDESCREEN_16_9("16:9", 16.0 / 9.0),
        WIDESCREEN_16_10("16:10", 16.0 / 10.0),
        ULTRAWIDE_21_9("21:9", 21.0 / 9.0),
        CLASSIC_4_3("4:3", 4.0 / 3.0),
        SQUARE_1_1("1:1", 1.0),
        PORTRAIT_9_16("9:16", 9.0 / 16.0),
        MIN_AREA("Min Area", 0.0);

        private final String label;
        private final double targetRatio;

        AspectPreset(final String label, final double targetRatio) {
            this.label = label;
            this.targetRatio = targetRatio;
        }

        public String label() {
            return label;
        }

        /**
         * Target width/height pixel ratio, or {@code 0.0} when minimizing raw bounding area only.
         */
        public double targetRatio() {
            return targetRatio;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record LayoutMetrics(
            int cols,
            double rows,
            double pixelWidth,
            double pixelHeight,
            double aspectRatio,
            int overlaps,
            int shiftedCount,
            int crossings,
            int occlusions,
            double score) {
    }

    public record SolveProgress(
            int completedSteps,
            int totalSteps,
            String stage,
            LayoutMetrics bestMetrics) {
    }

    public record SolveResult(
            Map<String, Offsets> offsets,
            LayoutMetrics before,
            LayoutMetrics after,
            int evaluations,
            boolean improved,
            boolean cancelled) {
    }

    private record CandidateInfo(String className, int subtreeSize, int depth, boolean leaf) {
    }

    private static final class SearchState {
        final List<ScenarioBlueprint> roots;
        final PreviewSettings settings;
        final AspectPreset preset;
        final BooleanSupplier cancelled;
        final Consumer<SolveProgress> progressListener;
        long deadlineNanos;

        Map<String, Offsets> bestOffsets;
        LayoutMetrics bestMetrics;
        int evaluations;
        boolean wasCancelled;

        SearchState(
                final List<ScenarioBlueprint> roots,
                final Map<String, Offsets> startOffsets,
                final PreviewSettings settings,
                final AspectPreset preset,
                final BooleanSupplier cancelled,
                final Consumer<SolveProgress> progressListener,
                final long deadlineNanos) {
            this.roots = roots;
            this.settings = settings;
            this.preset = preset;
            this.cancelled = (null != cancelled) ? cancelled : () -> false;
            this.progressListener = progressListener;
            this.deadlineNanos = deadlineNanos;
            this.bestOffsets = new LinkedHashMap<>(startOffsets);
            this.bestMetrics = evaluate(this.bestOffsets);
        }

        boolean shouldStop() {
            if (cancelled.getAsBoolean()) {
                wasCancelled = true;
                return true;
            }
            return System.nanoTime() >= deadlineNanos;
        }

        LayoutMetrics evaluate(final Map<String, Offsets> offsets) {
            evaluations++;
            final PreviewResult preview = TreeLayoutEditorModel.computePreview(roots, offsets, settings, true);
            return computeMetrics(preview, offsets, preset);
        }

        void report(final int step, final int total, final String stage) {
            if (null != progressListener) {
                progressListener.accept(new SolveProgress(step, total, stage, bestMetrics));
            }
        }
    }

    /**
     * Convenience synchronous entry point operating on a live model's snapshot.
     */
    public static SolveResult solve(final TreeLayoutEditorModel model, final AspectPreset preset) {
        Objects.requireNonNull(model, "model");
        return solve(
                model.roots(),
                model.snapshotCurrentOffsets(),
                model.previewSettings().copy(),
                preset,
                () -> false,
                null);
    }

    /**
     * Searches for improved {@code @VisualOffset} and {@code @VisualRowOffset} values starting from
     * {@code startOffsets}, targeting {@code preset}. Safe to invoke on a background worker thread.
     */
    public static SolveResult solve(
            final List<ScenarioBlueprint> roots,
            final Map<String, Offsets> startOffsets,
            final PreviewSettings settings,
            final AspectPreset preset,
            final BooleanSupplier cancelled,
            final Consumer<SolveProgress> progressListener) {

        Objects.requireNonNull(preset, "preset");
        final PreviewSettings effectiveSettings = (null != settings) ? settings.copy() : new PreviewSettings();
        final Map<String, Offsets> initial = (null != startOffsets) ? new LinkedHashMap<>(startOffsets) : new LinkedHashMap<>();

        if (null == roots || roots.isEmpty()) {
            final LayoutMetrics empty = computeMetrics(PreviewResult.EMPTY, initial, preset);
            return new SolveResult(initial, empty, empty, 0, false, false);
        }

        final long startNanos = System.nanoTime();
        final long deadlineNanos = startNanos + TIME_BUDGET_MS * 1_000_000L;

        final SearchState primary = new SearchState(
                roots, initial, effectiveSettings, preset, cancelled, progressListener, deadlineNanos);
        final LayoutMetrics baseline = primary.bestMetrics;

        final List<CandidateInfo> allCandidates = collectCandidates(roots, initial);
        if (allCandidates.isEmpty()) {
            return new SolveResult(primary.bestOffsets, baseline, baseline, primary.evaluations, false, false);
        }

        // Primary ordering: largest subtrees first (macro branches claim the best pockets first)
        final List<CandidateInfo> largestFirst = new ArrayList<>(allCandidates);
        largestFirst.sort(Comparator.comparingInt(CandidateInfo::subtreeSize).reversed()
                .thenComparingInt(CandidateInfo::depth)
                .thenComparing(CandidateInfo::className));

        runCoordinateDescent(primary, largestFirst);

        // If we finished well inside budget, also test a shallowest-first ordering from the same
        // starting offsets and keep whichever layout scores strictly better.
        SearchState winner = primary;
        int totalEvals = primary.evaluations;
        final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        if (!primary.wasCancelled && elapsedMs < SECONDARY_ORDER_CUTOFF_MS && allCandidates.size() >= 3) {
            final List<CandidateInfo> shallowFirst = new ArrayList<>(allCandidates);
            shallowFirst.sort(Comparator.comparingInt(CandidateInfo::depth)
                    .thenComparing(Comparator.comparingInt(CandidateInfo::subtreeSize).reversed())
                    .thenComparing(CandidateInfo::className));
            if (!sameOrder(largestFirst, shallowFirst)) {
                final SearchState secondary = new SearchState(
                        roots, initial, effectiveSettings, preset, cancelled, progressListener, deadlineNanos);
                runCoordinateDescent(secondary, shallowFirst);
                totalEvals += secondary.evaluations;
                if (secondary.wasCancelled) {
                    winner.wasCancelled = true;
                } else if (secondary.bestMetrics.score() < primary.bestMetrics.score() - 1e-6) {
                    winner = secondary;
                }
            }
        }

        if (!winner.wasCancelled) {
            // Always grant a short dedicated cleanup window so inflated offsets get simplified
            winner.deadlineNanos = Math.max(winner.deadlineNanos, System.nanoTime() + 800_000_000L);
            final int evalsBeforePrune = winner.evaluations;
            pruneAndSimplify(winner);
            totalEvals += (winner.evaluations - evalsBeforePrune);
        }

        final boolean improved = !winner.wasCancelled
                && !winner.bestOffsets.equals(initial)
                && winner.bestMetrics.score() < baseline.score() - 1e-6;

        return new SolveResult(
                Collections.unmodifiableMap(winner.bestOffsets),
                baseline,
                winner.bestMetrics,
                totalEvals,
                improved,
                winner.wasCancelled);
    }

    /**
     * Computes visual aspect ratio ({@code pixelWidth / pixelHeight}) for a rendered preview.
     */
    public static double aspectRatio(final PreviewResult preview) {
        if (null == preview || preview.layout().nodes().isEmpty()) return 0.0;
        final double[] dims = pixelDimensions(preview.layout().nodes());
        return dims[1] > 0.0 ? (dims[0] / dims[1]) : 0.0;
    }

    /**
     * Scores a layout snapshot for the given offsets and target aspect preset (lower is better).
     */
    public static LayoutMetrics computeMetrics(
            final PreviewResult preview,
            final Map<String, Offsets> offsets,
            final AspectPreset preset) {
        final List<NodeData> nodes = (null != preview) ? preview.layout().nodes() : List.of();
        final List<Edge> edges = (null != preview) ? preview.layout().edges() : List.of();
        if (nodes.isEmpty()) {
            return new LayoutMetrics(0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0, 0.0);
        }

        final double[] dims = pixelDimensions(nodes);
        final double pixelW = dims[0];
        final double pixelH = dims[1];
        final double ratio = (pixelH > 0.0) ? (pixelW / pixelH) : 1.0;

        final int overlaps = preview.overlapCount();

        // Primary size objective (in pixel units — lower = larger legible zoom on screen)
        final double sqrtActualArea = Math.sqrt(pixelW * pixelH);
        final double primarySize;
        if (preset.targetRatio() > 0.0) {
            final double r = preset.targetRatio();
            final double enclosingH = Math.max(pixelW / r, pixelH);
            final double sqrtEnclosingArea = enclosingH * Math.sqrt(r);
            primarySize = 0.78 * sqrtEnclosingArea + 0.22 * sqrtActualArea;
        } else {
            primarySize = sqrtActualArea;
        }

        // Edge cleanliness: crossings, card occlusions, and excessive stretch
        final int occlusions = countEdgeOcclusions(nodes, edges);
        final int crossings = countEdgeCrossings(edges);
        double stretchPenalty = 0.0;
        for (final Edge e : edges) {
            final int dxCols = Math.abs(e.to().col() - e.from().col());
            final double dyRows = Math.abs(e.to().row() - e.from().row());
            if (dxCols > 1) {
                stretchPenalty += (dxCols - 1) * LONG_COL_EDGE_PENALTY;
            }
            if (dyRows > 2.0) {
                stretchPenalty += (dyRows - 2.0) * LONG_ROW_EDGE_PENALTY;
            }
        }

        // Annotation parsimony: prefer fewer, smaller annotations
        int shiftedCount = 0;
        double offsetPenalty = 0.0;
        if (null != offsets) {
            for (final Offsets off : offsets.values()) {
                if (null == off || off.isZero()) continue;
                shiftedCount++;
                if (off.colOffset() != 0) {
                    offsetPenalty += COL_OFFSET_ANNOTATION_PENALTY + off.colOffset() * OFFSET_MAGNITUDE_PENALTY;
                }
                if (off.rowOffset() != 0) {
                    offsetPenalty += ROW_OFFSET_ANNOTATION_PENALTY + Math.abs(off.rowOffset()) * OFFSET_MAGNITUDE_PENALTY;
                }
            }
        }

        final double score = (overlaps > 0 ? OVERLAP_PENALTY * overlaps : 0.0)
                + primarySize
                + occlusions * OCCLUSION_PENALTY
                + crossings * CROSSING_PENALTY
                + stretchPenalty
                + offsetPenalty;

        return new LayoutMetrics(
                preview.totalCols(),
                preview.totalRows(),
                pixelW,
                pixelH,
                ratio,
                overlaps,
                shiftedCount,
                crossings,
                occlusions,
                score);
    }

    private static void runCoordinateDescent(
            final SearchState state,
            final List<CandidateInfo> orderedCandidates) {

        final List<CandidateInfo> macroCandidates = new ArrayList<>();
        for (final CandidateInfo c : orderedCandidates) {
            final Offsets cur = state.bestOffsets.getOrDefault(c.className(), Offsets.ZERO);
            if (c.subtreeSize() >= 4 || !cur.isZero()) {
                macroCandidates.add(c);
            }
        }
        final List<CandidateInfo> pass0List = (macroCandidates.size() >= 4) ? macroCandidates : orderedCandidates;

        final int estimatedSteps = pass0List.size() + orderedCandidates.size();
        int step = 0;

        for (int sweep = 0; sweep < MAX_SWEEPS; sweep++) {
            if (state.shouldStop()) return;
            final List<CandidateInfo> activeList = (sweep == 0) ? pass0List : orderedCandidates;
            boolean sweepImproved = false;

            for (final CandidateInfo cand : activeList) {
                if (state.shouldStop()) return;
                step++;
                if (optimizeSingleCandidate(state, cand, sweep == 0)) {
                    sweepImproved = true;
                }
                state.report(Math.min(step, estimatedSteps), Math.max(estimatedSteps, step),
                        sweep == 0 ? "Macro placement" : "Refining layout");
            }

            if (!sweepImproved && sweep >= 1) {
                break;
            }
        }
    }

    private static boolean optimizeSingleCandidate(
            final SearchState state,
            final CandidateInfo cand,
            final boolean macroSweep) {

        final String className = cand.className();
        final Offsets startOff = state.bestOffsets.getOrDefault(className, Offsets.ZERO);
        final double startScore = state.bestMetrics.score();

        final int rowSpan = Math.max(4, (int) Math.ceil(state.bestMetrics.rows()));
        final int maxColProbe = Math.min(MAX_COL_OFFSET, Math.max(8, state.bestMetrics.cols() + 3));

        final Map<String, Offsets> trial = new LinkedHashMap<>(state.bestOffsets);
        Offsets localBestOff = startOff;
        LayoutMetrics localBestMetrics = state.bestMetrics;

        // 1a. Coarse column sweep at current row (and row 0) to pick the top 3 promising columns
        final List<Integer> coarseCols = buildCoarseCols(startOff.colOffset(), maxColProbe);
        final Map<Integer, Double> bestScoreByCol = new LinkedHashMap<>();
        bestScoreByCol.put(startOff.colOffset(), startScore);

        final Set<Integer> probeBaseRows = new LinkedHashSet<>();
        probeBaseRows.add(startOff.rowOffset());
        probeBaseRows.add(0);

        for (final int col : coarseCols) {
            if (state.shouldStop()) break;
            for (final int baseRow : probeBaseRows) {
                if (state.shouldStop()) break;
                final Offsets next = new Offsets(col, baseRow);
                if (next.equals(localBestOff)) continue;
                trial.put(className, next);
                final LayoutMetrics m = state.evaluate(trial);
                bestScoreByCol.merge(col, m.score(), Math::min);
                if (m.score() < localBestMetrics.score() - 1e-6) {
                    localBestOff = next;
                    localBestMetrics = m;
                }
            }
        }

        // 1b. Joint 2D vertical band probe on the top 3 columns (plus col 0)
        if (!state.shouldStop()) {
            final List<Integer> topCols = new ArrayList<>(bestScoreByCol.keySet());
            topCols.sort(Comparator.comparingDouble(c -> bestScoreByCol.getOrDefault(c, Double.POSITIVE_INFINITY)));
            final Set<Integer> rowProbeCols = new LinkedHashSet<>();
            rowProbeCols.add(localBestOff.colOffset());
            for (final int c : topCols) {
                if (rowProbeCols.size() >= 3) break;
                rowProbeCols.add(c);
            }
            rowProbeCols.add(0);

            final List<Integer> macroRowBands = buildCoarseRows(startOff.rowOffset(), rowSpan, true);
            final List<Integer> zeroColRows = buildCoarseRows(startOff.rowOffset(), rowSpan, false);

            for (final int col : rowProbeCols) {
                if (state.shouldStop()) break;
                final List<Integer> rowsToTest = (col == 0) ? zeroColRows : macroRowBands;
                for (final int row : rowsToTest) {
                    if (state.shouldStop()) break;
                    final Offsets next = new Offsets(col, row);
                    if (next.equals(localBestOff)) continue;
                    trial.put(className, next);
                    final LayoutMetrics m = state.evaluate(trial);
                    if (m.score() < localBestMetrics.score() - 1e-6) {
                        localBestOff = next;
                        localBestMetrics = m;
                    }
                }
            }
        }

        // 2. Fine 1D column polish around the best coarse column
        if (!state.shouldStop()) {
            final int baseCol = localBestOff.colOffset();
            final int baseRow = localBestOff.rowOffset();
            for (int dc = -2; dc <= 2; dc++) {
                if (dc == 0) continue;
                final int col = baseCol + dc;
                if (col < 0 || col > MAX_COL_OFFSET) continue;
                if (state.shouldStop()) break;
                final Offsets next = new Offsets(col, baseRow);
                trial.put(className, next);
                final LayoutMetrics m = state.evaluate(trial);
                if (m.score() < localBestMetrics.score() - 1e-6) {
                    localBestOff = next;
                    localBestMetrics = m;
                }
            }
        }

        // 3. Fine 1D row polish around the best (col, row)
        if (!state.shouldStop()) {
            final int baseCol = localBestOff.colOffset();
            final int baseRow = localBestOff.rowOffset();
            final int[] deltas = macroSweep
                    ? new int[]{-6, -3, -1, 1, 3, 6}
                    : new int[]{-8, -4, -2, -1, 1, 2, 4, 8};
            final Set<Integer> fineRows = new LinkedHashSet<>();
            fineRows.add(0);
            for (final int d : deltas) {
                final int r = clampRow(baseRow + d);
                if (r != baseRow) fineRows.add(r);
            }
            for (final int r : fineRows) {
                if (state.shouldStop()) break;
                final Offsets next = new Offsets(baseCol, r);
                if (next.equals(localBestOff)) continue;
                trial.put(className, next);
                final LayoutMetrics m = state.evaluate(trial);
                if (m.score() < localBestMetrics.score() - 1e-6) {
                    localBestOff = next;
                    localBestMetrics = m;
                }
            }
        }

        if (localBestMetrics.score() < startScore - 1e-6) {
            state.bestOffsets.put(className, localBestOff);
            state.bestMetrics = localBestMetrics;
            return true;
        }
        return false;
    }

    /**
     * Drops redundant shifts and shrinks inflated offset values when the resulting layout
     * stays equally good or better.
     */
    private static void pruneAndSimplify(final SearchState state) {
        final List<String> shifted = new ArrayList<>();
        for (final Map.Entry<String, Offsets> e : state.bestOffsets.entrySet()) {
            if (!e.getValue().isZero()) {
                shifted.add(e.getKey());
            }
        }
        if (shifted.isEmpty()) return;

        final Map<String, Offsets> trial = new LinkedHashMap<>(state.bestOffsets);
        int step = 0;
        for (final String className : shifted) {
            if (state.shouldStop()) return;
            step++;
            Offsets cur = state.bestOffsets.getOrDefault(className, Offsets.ZERO);

            // Try removing row offset completely
            if (cur.rowOffset() != 0) {
                final Offsets noRow = new Offsets(cur.colOffset(), 0);
                trial.put(className, noRow);
                final LayoutMetrics m = state.evaluate(trial);
                if (m.score() <= state.bestMetrics.score() + 1.0) {
                    cur = noRow;
                    state.bestOffsets.put(className, cur);
                    state.bestMetrics = m;
                } else {
                    trial.put(className, cur);
                }
            }

            // Try removing col offset completely
            if (cur.colOffset() != 0 && !state.shouldStop()) {
                final Offsets noCol = new Offsets(0, cur.rowOffset());
                trial.put(className, noCol);
                final LayoutMetrics m = state.evaluate(trial);
                if (m.score() <= state.bestMetrics.score() + 1.0) {
                    cur = noCol;
                    state.bestOffsets.put(className, cur);
                    state.bestMetrics = m;
                } else {
                    trial.put(className, cur);
                }
            }

            // Try shrinking col offset toward 0
            while (cur.colOffset() > 1 && !state.shouldStop()) {
                final int smaller = cur.colOffset() - 1;
                final Offsets next = new Offsets(smaller, cur.rowOffset());
                trial.put(className, next);
                final LayoutMetrics m = state.evaluate(trial);
                if (m.score() <= state.bestMetrics.score() + 0.25) {
                    cur = next;
                    state.bestOffsets.put(className, cur);
                    state.bestMetrics = m;
                } else {
                    trial.put(className, cur);
                    break;
                }
            }

            // Try shrinking row offset toward 0
            while (Math.abs(cur.rowOffset()) > 1 && !state.shouldStop()) {
                final int stepTowardZero = cur.rowOffset() > 0
                        ? Math.max(0, cur.rowOffset() - Math.max(1, cur.rowOffset() / 3))
                        : Math.min(0, cur.rowOffset() + Math.max(1, (-cur.rowOffset()) / 3));
                final Offsets next = new Offsets(cur.colOffset(), stepTowardZero);
                trial.put(className, next);
                final LayoutMetrics m = state.evaluate(trial);
                if (m.score() <= state.bestMetrics.score() + 0.25) {
                    cur = next;
                    state.bestOffsets.put(className, cur);
                    state.bestMetrics = m;
                } else {
                    trial.put(className, cur);
                    break;
                }
            }

            state.report(step, shifted.size(), "Simplifying offsets");
        }
    }

    private static List<Integer> buildCoarseCols(final int currentCol, final int maxCol) {
        final TreeSet<Integer> cols = new TreeSet<>();
        cols.add(0);
        cols.add(Math.max(0, Math.min(MAX_COL_OFFSET, currentCol)));
        cols.add(1);
        cols.add(2);
        if (maxCol >= 4) cols.add(4);
        if (maxCol >= 6) cols.add(6);
        if (maxCol >= 9) cols.add(9);
        if (maxCol >= 12) cols.add(12);
        if (maxCol >= 16) cols.add(16);
        return new ArrayList<>(cols);
    }

    private static List<Integer> buildCoarseRows(final int currentRow, final int rowSpan, final boolean shiftedCol) {
        final Set<Integer> rows = new LinkedHashSet<>();
        rows.add(0);
        rows.add(clampRow(currentRow));
        if (shiftedCol) {
            final int half = Math.max(4, Math.min(MAX_ROW_OFFSET, rowSpan / 2));
            final int quarter = Math.max(2, half / 2);
            rows.add(-half);
            rows.add(-quarter);
            rows.add(quarter);
            rows.add(half);
        } else {
            final int mild = Math.max(2, Math.min(12, rowSpan / 5));
            rows.add(-mild);
            rows.add(mild);
            if (rowSpan >= 16) {
                final int wide = Math.min(MAX_ROW_OFFSET, rowSpan / 2);
                rows.add(-wide);
                rows.add(wide);
            }
        }
        return new ArrayList<>(rows);
    }

    private static int clampRow(final int r) {
        return Math.max(-MAX_ROW_OFFSET, Math.min(MAX_ROW_OFFSET, r));
    }

    private static List<CandidateInfo> collectCandidates(
            final List<ScenarioBlueprint> roots,
            final Map<String, Offsets> startOffsets) {

        final Map<String, CandidateInfo> byClass = new LinkedHashMap<>();
        final boolean singleRoot = roots.size() == 1;
        for (final ScenarioBlueprint root : roots) {
            collectRecursive(root, 0, singleRoot, byClass);
        }

        final List<CandidateInfo> shifted = new ArrayList<>();
        final List<CandidateInfo> branches = new ArrayList<>();
        final List<CandidateInfo> leaves = new ArrayList<>();

        for (final CandidateInfo info : byClass.values()) {
            final Offsets off = startOffsets.getOrDefault(info.className(), Offsets.ZERO);
            if (!off.isZero()) {
                shifted.add(info);
            } else if (!info.leaf()) {
                branches.add(info);
            } else {
                leaves.add(info);
            }
        }

        final Comparator<CandidateInfo> bySizeDesc = Comparator.comparingInt(CandidateInfo::subtreeSize).reversed()
                .thenComparingInt(CandidateInfo::depth)
                .thenComparing(CandidateInfo::className);
        shifted.sort(bySizeDesc);
        branches.sort(bySizeDesc);
        leaves.sort(bySizeDesc);

        final List<CandidateInfo> selected = new ArrayList<>(shifted);
        for (final CandidateInfo b : branches) {
            if (selected.size() >= MAX_CANDIDATES) break;
            selected.add(b);
        }
        if (selected.size() < 10) {
            for (final CandidateInfo l : leaves) {
                if (selected.size() >= Math.min(16, MAX_CANDIDATES)) break;
                selected.add(l);
            }
        }
        return selected;
    }

    private static void collectRecursive(
            final ScenarioBlueprint bp,
            final int depth,
            final boolean skipSingleRoot,
            final Map<String, CandidateInfo> out) {
        final boolean skip = skipSingleRoot && depth == 0;
        if (!skip) {
            final CandidateInfo existing = out.get(bp.className());
            if (null == existing || bp.subtreeSize() > existing.subtreeSize()) {
                out.put(bp.className(), new CandidateInfo(bp.className(), bp.subtreeSize(), depth, bp.isLeaf()));
            }
        }
        for (final ScenarioBlueprint child : bp.children()) {
            collectRecursive(child, depth + 1, skipSingleRoot, out);
        }
    }

    private static boolean sameOrder(final List<CandidateInfo> a, final List<CandidateInfo> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).className().equals(b.get(i).className())) return false;
        }
        return true;
    }

    private static double[] pixelDimensions(final List<NodeData> nodes) {
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        double minRow = Double.POSITIVE_INFINITY;
        double maxRow = Double.NEGATIVE_INFINITY;
        for (final NodeData n : nodes) {
            if (n.col() < minCol) minCol = n.col();
            if (n.col() > maxCol) maxCol = n.col();
            if (n.row() < minRow) minRow = n.row();
            if (n.row() > maxRow) maxRow = n.row();
        }
        final double pixelW = Math.max(CARD_WIDTH, (maxCol - minCol) * COL_WIDTH + CARD_WIDTH);
        final double pixelH = Math.max(CARD_HEIGHT, (maxRow - minRow) * ROW_HEIGHT + CARD_HEIGHT);
        return new double[]{pixelW, pixelH};
    }

    private static int countEdgeOcclusions(final List<NodeData> nodes, final List<Edge> edges) {
        if (edges.isEmpty() || nodes.size() <= 2) return 0;
        final Map<Integer, List<NodeData>> byCol = new HashMap<>();
        for (final NodeData n : nodes) {
            byCol.computeIfAbsent(n.col(), k -> new ArrayList<>()).add(n);
        }

        int occlusions = 0;
        final double hitHalfHeight = CARD_HEIGHT * 0.62;
        for (final Edge e : edges) {
            final NodeData p = e.from();
            final NodeData c = e.to();
            final int colSpan = c.col() - p.col();
            if (colSpan <= 1) continue;

            final double x1 = p.col() * COL_WIDTH + CARD_WIDTH;
            final double y1 = p.row() * ROW_HEIGHT;
            final double x2 = c.col() * COL_WIDTH;
            final double y2 = c.row() * ROW_HEIGHT;
            final double dx = x2 - x1;
            if (dx <= 1e-6) continue;

            for (int col = p.col() + 1; col < c.col(); col++) {
                final List<NodeData> colNodes = byCol.get(col);
                if (null == colNodes) continue;
                final double xMid = col * COL_WIDTH + CARD_WIDTH * 0.5;
                final double u = Math.max(0.0, Math.min(1.0, (xMid - x1) / dx));
                // Smoothstep approximates the horizontal-tangent cubic Bezier rendered in SVG/Canvas
                final double smooth = u * u * (3.0 - 2.0 * u);
                final double yAtCol = y1 + (y2 - y1) * smooth;
                for (final NodeData n : colNodes) {
                    if (n == p || n == c) continue;
                    if (Math.abs(n.row() * ROW_HEIGHT - yAtCol) < hitHalfHeight) {
                        occlusions++;
                    }
                }
            }
        }
        return occlusions;
    }

    private static int countEdgeCrossings(final List<Edge> edges) {
        final int n = edges.size();
        if (n < 2) return 0;

        final double[] x1 = new double[n];
        final double[] y1 = new double[n];
        final double[] x2 = new double[n];
        final double[] y2 = new double[n];
        final double[] minX = new double[n];
        final double[] maxX = new double[n];
        final double[] minY = new double[n];
        final double[] maxY = new double[n];
        final NodeData[] parents = new NodeData[n];
        final NodeData[] children = new NodeData[n];

        for (int i = 0; i < n; i++) {
            final Edge e = edges.get(i);
            final NodeData p = e.from();
            final NodeData c = e.to();
            parents[i] = p;
            children[i] = c;
            x1[i] = p.col() * COL_WIDTH + CARD_WIDTH;
            y1[i] = p.row() * ROW_HEIGHT;
            x2[i] = c.col() * COL_WIDTH;
            y2[i] = c.row() * ROW_HEIGHT;
            minX[i] = Math.min(x1[i], x2[i]);
            maxX[i] = Math.max(x1[i], x2[i]);
            minY[i] = Math.min(y1[i], y2[i]);
            maxY[i] = Math.max(y1[i], y2[i]);
        }

        int crossings = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (parents[i] == parents[j] || parents[i] == children[j]
                        || children[i] == parents[j] || children[i] == children[j]) {
                    continue;
                }
                if (maxX[i] <= minX[j] || maxX[j] <= minX[i]
                        || maxY[i] <= minY[j] || maxY[j] <= minY[i]) {
                    continue;
                }
                if (segmentsProperlyIntersect(x1[i], y1[i], x2[i], y2[i], x1[j], y1[j], x2[j], y2[j])) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean segmentsProperlyIntersect(
            final double ax, final double ay, final double bx, final double by,
            final double cx, final double cy, final double dx, final double dy) {
        final double o1 = orient(ax, ay, bx, by, cx, cy);
        final double o2 = orient(ax, ay, bx, by, dx, dy);
        if ((o1 >= 0.0 && o2 >= 0.0) || (o1 <= 0.0 && o2 <= 0.0)) return false;
        final double o3 = orient(cx, cy, dx, dy, ax, ay);
        final double o4 = orient(cx, cy, dx, dy, bx, by);
        return (o3 > 0.0 && o4 < 0.0) || (o3 < 0.0 && o4 > 0.0);
    }

    private static double orient(
            final double ax, final double ay, final double bx, final double by, final double cx, final double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }
}
