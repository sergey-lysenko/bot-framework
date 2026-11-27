package works.lysenko.base.ui.tree;

import works.lysenko.base.output.TreeHtml;

import java.util.ArrayList;
import java.util.Collection;
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

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.ING;
import static works.lysenko.util.chrs.___.MEN;
import static works.lysenko.util.chrs.___.MIN;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.AREA;
import static works.lysenko.util.lang.word.L.LAYOUT;
import static works.lysenko.util.lang.word.M.MODEL;
import static works.lysenko.util.lang.word.O.OFFSETS;
import static works.lysenko.util.lang.word.P.PRESET;
import static works.lysenko.util.spec.Symbols.C;
import static works.lysenko.util.spec.Symbols.E;
import static works.lysenko.util.spec.Symbols.T;

/**
 * Automatic layout solver that searches for clean, overlap-free {@code @VisualOffset} and
 * {@code @VisualRowOffset} combinations starting from the user's current offsets.
 * <p>
 * Evaluates candidate placements through the real {@link works.lysenko.base.output.tree.TreeLayoutOptimizer}
 * pipeline (via {@link TreeLayoutEditorModel#computePreview}) so the scored geometry matches
 * what the user sees on the canvas and in HTML/SVG reports.
 */
public enum TreeLayoutAutoSolver {
    ;

    private static final double COL_WIDTH = 260.0;
    private static final double ROW_HEIGHT = 56.0;
    private static final double CARD_WIDTH = 220.0;
    private static final double CARD_HEIGHT = 44.0;

    private static final double OVERLAP_PENALTY = 1_000_000_000.0;
    private static final double COL_OFFSET_ANNOTATION_PENALTY = 24.0;
    private static final double ROW_OFFSET_ANNOTATION_PENALTY = 18.0;
    private static final double OFFSET_MAGNITUDE_PENALTY = 0.6;
    private static final double OCCLUSION_PENALTY = 28.0;
    private static final double CROSSING_PENALTY = 22.0;
    private static final double LONG_COL_EDGE_PENALTY = 4.0;
    private static final double LONG_ROW_EDGE_PENALTY = 1.5;
    private static final double LONG_ROW_EDGE_THRESHOLD = 2.0;

    private static final double ENCLOSING_AREA_WEIGHT = 0.78;
    private static final double ACTUAL_AREA_WEIGHT = 0.22;
    private static final double SCORE_EPSILON = 1.0e-6;
    private static final double PRUNE_DROP_TOLERANCE = 1.0;
    private static final double PRUNE_SHRINK_TOLERANCE = 0.25;
    private static final double OCCLUSION_HALF_HEIGHT_FACTOR = 0.62;
    private static final double OCCLUSION_HIT_HALF_HEIGHT = CARD_HEIGHT * OCCLUSION_HALF_HEIGHT_FACTOR;
    private static final double HALF = 0.5;
    private static final double SMOOTHSTEP_A = 3.0;
    private static final double SMOOTHSTEP_B = 2.0;

    private static final int MAX_CANDIDATES = 26;
    private static final int MAX_COL_OFFSET = 20;
    private static final int MAX_ROW_OFFSET = 48;
    private static final int MAX_SWEEPS = 3;
    private static final int SECONDARY_MIN_CANDIDATES = 3;
    private static final int MACRO_SUBTREE_MIN = 4;
    private static final int MACRO_PASS_MIN_COUNT = 4;
    private static final int MIN_ROW_SPAN = 4;
    private static final int COL_PROBE_MARGIN = 3;
    private static final int MIN_COL_PROBE = 8;
    private static final int TOP_COL_COUNT = 3;
    private static final int FINE_COL_RADIUS = 2;
    private static final int ROW_SHRINK_DIVISOR = 3;
    private static final int MIN_HALF_ROW = 4;
    private static final int MIN_QUARTER_ROW = 2;
    private static final int MILD_ROW_DIVISOR = 5;
    private static final int MILD_ROW_MIN = 2;
    private static final int MILD_ROW_MAX = 12;
    private static final int WIDE_ROW_SPAN_MIN = 16;
    private static final int LEAF_FILL_THRESHOLD = 10;
    private static final int MAX_WITH_LEAVES = 16;
    private static final int MIN_NODES_FOR_OCCLUSION = 2;
    private static final int MIN_EDGES_FOR_CROSSING = 2;
    private static final int INITIAL_CAPACITY_SMALL = 8;
    private static final int INITIAL_CAPACITY_MEDIUM = 16;
    private static final int INITIAL_CAPACITY_LARGE = 32;

    private static final int[] COARSE_COL_STEPS = {4, 6, 9, 12, 16};
    private static final int[] MACRO_ROW_DELTAS = {-6, -3, -1, 1, 3, 6};
    private static final int[] REFINE_ROW_DELTAS = {-8, -4, -2, -1, 1, 2, 4, 8};

    private static final long TIME_BUDGET_MS = 3_500L;
    private static final long SECONDARY_ORDER_CUTOFF_MS = 1_000L;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final long PRUNE_WINDOW_NANOS = 800_000_000L;

    private static final String MIN_AREA_LABEL = b(c(MIN), c(AREA));
    private static final String STAGE_MACRO_PLACEMENT = b(c(s(MA, C, RO)), s(PL, AC, E, MEN, T));
    private static final String STAGE_REFINING_LAYOUT = b(c(s(RE, FI, NI, NG)), LAYOUT);
    private static final String STAGE_SIMPLIFYING_OFFSETS = b(c(s(SI, MP, LI, FY, ING)), OFFSETS);

    private static final LayoutMetrics EMPTY_METRICS =
            new LayoutMetrics(0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0, 0.0);

    /**
     * Selectable target proportions for automatic layout optimization.
     */
    public enum AspectPreset {
        /** Widescreen 16:9 target aspect ratio. */
        WIDESCREEN_16_9("16:9", 16.0 / 9.0),
        /** Widescreen 16:10 target aspect ratio. */
        WIDESCREEN_16_10("16:10", 16.0 / 10.0),
        /** Ultrawide 21:9 target aspect ratio. */
        ULTRAWIDE_21_9("21:9", 21.0 / 9.0),
        /** Classic 4:3 target aspect ratio. */
        CLASSIC_4_3("4:3", 4.0 / 3.0),
        /** Square 1:1 target aspect ratio. */
        SQUARE_1_1("1:1", 1.0),
        /** Portrait 9:16 target aspect ratio. */
        PORTRAIT_9_16("9:16", 9.0 / 16.0),
        /** Minimize raw bounding area without targeting a fixed aspect ratio. */
        MIN_AREA(MIN_AREA_LABEL, 0.0);

        private final String label;
        private final double targetRatio;

        AspectPreset(final String label, final double targetRatio) {
            this.label = label;
            this.targetRatio = targetRatio;
        }

        /**
         * Returns the human-readable preset label.
         *
         * @return preset display label
         */
        public String label() {
            return label;
        }

        /**
         * Target width/height pixel ratio, or {@code 0.0} when minimizing raw bounding area only.
         *
         * @return target aspect ratio, or {@code 0.0} for area-only optimization
         */
        double targetRatio() {
            return targetRatio;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Computed visual and structural quality metrics for a candidate layout.
     *
     * @param cols         total grid columns
     * @param rows         maximum grid row
     * @param pixelWidth   rendered pixel width
     * @param pixelHeight  rendered pixel height
     * @param aspectRatio  rendered width-to-height ratio
     * @param overlaps     overlapping card pairs
     * @param shiftedCount number of scenarios with non-zero offsets
     * @param crossings    proper edge crossings
     * @param occlusions   long edges passing through intermediate node cards
     * @param score        composite objective score (lower is better)
     */
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

    /**
     * Progress snapshot reported while automatic optimization is running.
     *
     * @param completedSteps completed optimization steps
     * @param totalSteps     estimated total optimization steps
     * @param stage          human-readable description of the active optimization stage
     * @param bestMetrics    metrics of the best layout discovered so far
     */
    public record SolveProgress(
            int completedSteps,
            int totalSteps,
            String stage,
            LayoutMetrics bestMetrics) {
    }

    /**
     * Final outcome returned by an automatic layout optimization run.
     *
     * @param offsets     best discovered scenario offsets
     * @param before      metrics before optimization
     * @param after       metrics after optimization
     * @param evaluations total number of layout previews evaluated
     * @param improved    whether a strictly better offset assignment was found
     * @param cancelled   whether optimization was stopped early by cancellation
     */
    public record SolveResult(
            Map<String, ScenarioOffsetSourceCodec.Offsets> offsets,
            LayoutMetrics before,
            LayoutMetrics after,
            int evaluations,
            boolean improved,
            boolean cancelled) {
    }

    private record CandidateInfo(String className, int subtreeSize, int depth, boolean leaf) {
    }

    private record SolveContext(
            List<TreeLayoutEditorModel.ScenarioBlueprint> roots,
            TreeLayoutEditorModel.PreviewSettings settings,
            AspectPreset preset,
            BooleanSupplier cancelled,
            Consumer<? super SolveProgress> progressListener) {
    }

    private static final class SearchState {

        private final SolveContext context;
        private long deadlineNanos;
        private final Map<String, ScenarioOffsetSourceCodec.Offsets> bestOffsets;
        private LayoutMetrics bestMetrics;
        private int evaluations;
        private boolean cancelled;

        private SearchState(
                final SolveContext context,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> startOffsets,
                final long deadlineNanos) {
            this.context = context;
            this.deadlineNanos = deadlineNanos;
            bestOffsets = new LinkedHashMap<>(startOffsets);
            evaluations = 0;
            cancelled = false;
            bestMetrics = EMPTY_METRICS;
        }

        private static SearchState create(
                final SolveContext context,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> startOffsets,
                final long deadlineNanos) {
            final SearchState state = new SearchState(context, startOffsets, deadlineNanos);
            state.initializeBaseline();
            return state;
        }

        private void initializeBaseline() {
            bestMetrics = evaluate(bestOffsets);
        }

        private boolean isActive() {
            final boolean active;
            if (context.cancelled().getAsBoolean()) {
                cancelled = true;
                active = false;
            } else {
                active = System.nanoTime() < deadlineNanos;
            }
            return active;
        }

        private LayoutMetrics evaluate(final Map<String, ScenarioOffsetSourceCodec.Offsets> offsets) {
            evaluations++;
            final TreeLayoutEditorModel.PreviewResult preview =
                    TreeLayoutEditorModel.computePreview(context.roots(), offsets, context.settings(), true);
            return GeometryScorer.computeMetrics(preview, offsets, context.preset());
        }

        private void report(final int step, final int total, final String stage) {
            if (null != context.progressListener()) {
                context.progressListener().accept(new SolveProgress(step, total, stage, bestMetrics));
            }
        }

        private Map<String, ScenarioOffsetSourceCodec.Offsets> bestOffsets() {
            return bestOffsets;
        }

        private Map<String, ScenarioOffsetSourceCodec.Offsets> copyBestOffsets() {
            return new LinkedHashMap<>(bestOffsets);
        }

        private ScenarioOffsetSourceCodec.Offsets offsetFor(final String className) {
            return bestOffsets.getOrDefault(className, ScenarioOffsetSourceCodec.Offsets.ZERO);
        }

        private LayoutMetrics bestMetrics() {
            return bestMetrics;
        }

        private int evaluations() {
            return evaluations;
        }

        private boolean isCancelled() {
            return cancelled;
        }

        private void markCancelled() {
            cancelled = true;
        }

        private void extendDeadlineAtLeast(final long minDeadlineNanos) {
            deadlineNanos = Math.max(deadlineNanos, minDeadlineNanos);
        }

        private void acceptBest(
                final String className,
                final ScenarioOffsetSourceCodec.Offsets offsets,
                final LayoutMetrics metrics) {
            bestOffsets.put(className, offsets);
            bestMetrics = metrics;
        }
    }

    /**
     * Convenience synchronous entry point operating on a live model's snapshot.
     *
     * @param model  live layout editor model
     * @param preset target aspect preset
     * @return optimization result
     */
    public static SolveResult solve(final TreeLayoutEditorModel model, final AspectPreset preset) {
        Objects.requireNonNull(model, MODEL);
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
     *
     * @param roots            root scenario blueprints to lay out
     * @param startOffsets     initial offsets keyed by scenario class name, or {@code null}
     * @param settings         preview compaction settings, or {@code null} for defaults
     * @param preset           target aspect preset
     * @param cancelled        cancellation predicate polled during search, or {@code null}
     * @param progressListener optional progress callback, or {@code null}
     * @return optimization result
     */
    public static SolveResult solve(
            final List<TreeLayoutEditorModel.ScenarioBlueprint> roots,
            final Map<String, ScenarioOffsetSourceCodec.Offsets> startOffsets,
            final TreeLayoutEditorModel.PreviewSettings settings,
            final AspectPreset preset,
            final BooleanSupplier cancelled,
            final Consumer<? super SolveProgress> progressListener) {
        Objects.requireNonNull(preset, PRESET);
        final TreeLayoutEditorModel.PreviewSettings effectiveSettings =
                (null == settings) ? new TreeLayoutEditorModel.PreviewSettings() : settings.copy();
        final Map<String, ScenarioOffsetSourceCodec.Offsets> initial =
                (null == startOffsets) ? new LinkedHashMap<>(INITIAL_CAPACITY_MEDIUM) : new LinkedHashMap<>(startOffsets);

        final SolveResult result;
        if (null == roots || roots.isEmpty()) {
            final LayoutMetrics empty =
                    GeometryScorer.computeMetrics(TreeLayoutEditorModel.PreviewResult.EMPTY, initial, preset);
            result = new SolveResult(initial, empty, empty, 0, false, false);
        } else {
            final BooleanSupplier effectiveCancel = (null == cancelled) ? () -> false : cancelled;
            final SolveContext context =
                    new SolveContext(roots, effectiveSettings, preset, effectiveCancel, progressListener);
            result = solveNonEmpty(context, initial);
        }
        return result;
    }

    private static SolveResult solveNonEmpty(
            final SolveContext context,
            final Map<String, ScenarioOffsetSourceCodec.Offsets> initial) {
        final long startNanos = System.nanoTime();
        final long deadlineNanos = startNanos + TIME_BUDGET_MS * NANOS_PER_MILLI;
        final SearchState primary = SearchState.create(context, initial, deadlineNanos);
        final LayoutMetrics baseline = primary.bestMetrics();
        final List<CandidateInfo> allCandidates = CandidateSelector.collectCandidates(context.roots(), initial);

        final SolveResult result;
        if (allCandidates.isEmpty()) {
            result = new SolveResult(primary.bestOffsets(), baseline, baseline, primary.evaluations(), false, false);
        } else {
            result = runOptimizationPipeline(context, initial, primary, baseline, allCandidates, startNanos, deadlineNanos);
        }
        return result;
    }

    private static SolveResult runOptimizationPipeline(
            final SolveContext context,
            final Map<String, ScenarioOffsetSourceCodec.Offsets> initial,
            final SearchState primary,
            final LayoutMetrics baseline,
            final List<CandidateInfo> allCandidates,
            final long startNanos,
            final long deadlineNanos) {
        final List<CandidateInfo> largestFirst = new ArrayList<>(allCandidates);
        largestFirst.sort(Comparator.comparingInt(CandidateInfo::subtreeSize).reversed()
                .thenComparingInt(CandidateInfo::depth)
                .thenComparing(CandidateInfo::className));

        runCoordinateDescent(primary, largestFirst);

        final int[] totalEvals = {primary.evaluations()};
        final SearchState winner = maybeRunSecondaryOrder(
                context, initial, primary, allCandidates, largestFirst, startNanos, deadlineNanos, totalEvals);

        if (!winner.isCancelled()) {
            winner.extendDeadlineAtLeast(System.nanoTime() + PRUNE_WINDOW_NANOS);
            final int evalsBeforePrune = winner.evaluations();
            OffsetPruner.pruneAndSimplify(winner);
            totalEvals[0] += winner.evaluations() - evalsBeforePrune;
        }

        final boolean improved = isImprovedResult(winner, initial, baseline);
        return new SolveResult(
                Collections.unmodifiableMap(winner.bestOffsets()),
                baseline,
                winner.bestMetrics(),
                totalEvals[0],
                improved,
                winner.isCancelled());
    }

    private static boolean isImprovedResult(
            final SearchState winner,
            final Map<String, ScenarioOffsetSourceCodec.Offsets> initial,
            final LayoutMetrics baseline) {
        final boolean active = !winner.isCancelled();
        final boolean changed = !winner.bestOffsets().equals(initial);
        final boolean betterScore = winner.bestMetrics().score() < baseline.score() - SCORE_EPSILON;
        return active && changed && betterScore;
    }

    private static SearchState maybeRunSecondaryOrder(
            final SolveContext context,
            final Map<String, ScenarioOffsetSourceCodec.Offsets> initial,
            final SearchState primary,
            final List<CandidateInfo> allCandidates,
            final List<CandidateInfo> largestFirst,
            final long startNanos,
            final long deadlineNanos,
            final int[] totalEvals) {
        SearchState winner = primary;
        final long elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI;
        final boolean eligible = !primary.isCancelled()
                && SECONDARY_ORDER_CUTOFF_MS > elapsedMs
                && SECONDARY_MIN_CANDIDATES <= allCandidates.size();
        if (eligible) {
            final List<CandidateInfo> shallowFirst = new ArrayList<>(allCandidates);
            shallowFirst.sort(Comparator.comparingInt(CandidateInfo::depth)
                    .thenComparing(Comparator.comparingInt(CandidateInfo::subtreeSize).reversed())
                    .thenComparing(CandidateInfo::className));
            if (!CandidateSelector.isSameOrder(largestFirst, shallowFirst)) {
                final SearchState secondary = SearchState.create(context, initial, deadlineNanos);
                runCoordinateDescent(secondary, shallowFirst);
                totalEvals[0] += secondary.evaluations();
                if (secondary.isCancelled()) {
                    winner.markCancelled();
                } else if (secondary.bestMetrics().score() < primary.bestMetrics().score() - SCORE_EPSILON) {
                    winner = secondary;
                }
            }
        }
        return winner;
    }

    /**
     * Computes visual aspect ratio ({@code pixelWidth / pixelHeight}) for a rendered preview.
     *
     * @param preview layout preview snapshot
     * @return visual aspect ratio, or {@code 0.0} if the preview is empty
     */
    public static double aspectRatio(final TreeLayoutEditorModel.PreviewResult preview) {
        return GeometryScorer.aspectRatio(preview);
    }

    private static void runCoordinateDescent(
            final SearchState state,
            final List<CandidateInfo> orderedCandidates) {
        final List<CandidateInfo> macroCandidates = selectMacroCandidates(state, orderedCandidates);
        final List<CandidateInfo> pass0List =
                (MACRO_PASS_MIN_COUNT <= macroCandidates.size()) ? macroCandidates : orderedCandidates;

        final int estimatedSteps = pass0List.size() + orderedCandidates.size();
        final int[] stepCounter = {0};
        boolean keepGoing = true;

        for (int sweep = 0; MAX_SWEEPS > sweep && keepGoing && state.isActive(); sweep++) {
            final List<CandidateInfo> activeList = (0 == sweep) ? pass0List : orderedCandidates;
            final boolean sweepImproved = isSweepImproved(state, activeList, 0 == sweep, estimatedSteps, stepCounter);
            keepGoing = sweepImproved || 0 == sweep;
        }
    }

    private static List<CandidateInfo> selectMacroCandidates(
            final SearchState state,
            final Collection<CandidateInfo> orderedCandidates) {
        final List<CandidateInfo> macroCandidates = new ArrayList<>(orderedCandidates.size());
        for (final CandidateInfo candidate : orderedCandidates) {
            final ScenarioOffsetSourceCodec.Offsets cur = state.offsetFor(candidate.className());
            if (MACRO_SUBTREE_MIN <= candidate.subtreeSize() || !cur.isZero()) {
                macroCandidates.add(candidate);
            }
        }
        return macroCandidates;
    }

    private static boolean isSweepImproved(
            final SearchState state,
            final Iterable<CandidateInfo> activeList,
            final boolean macroSweep,
            final int estimatedSteps,
            final int[] stepCounter) {
        boolean sweepImproved = false;
        final String stageLabel = macroSweep ? STAGE_MACRO_PLACEMENT : STAGE_REFINING_LAYOUT;
        for (final CandidateInfo candidate : activeList) {
            if (state.isActive()) {
                stepCounter[0]++;
                if (SingleCandidateSearch.isCandidateImproved(state, candidate, macroSweep)) {
                    sweepImproved = true;
                }
                final int step = stepCounter[0];
                state.report(Math.min(step, estimatedSteps), Math.max(estimatedSteps, step), stageLabel);
            }
        }
        return sweepImproved;
    }

    private static final class SingleCandidateSearch {

        private final SearchState state;
        private final String className;
        private final ScenarioOffsetSourceCodec.Offsets startOff;
        private final Map<String, ScenarioOffsetSourceCodec.Offsets> trial;
        private ScenarioOffsetSourceCodec.Offsets localBestOff;
        private LayoutMetrics localBestMetrics;

        private SingleCandidateSearch(final SearchState state, final String className) {
            this.state = state;
            this.className = className;
            startOff = state.offsetFor(className);
            trial = state.copyBestOffsets();
            localBestOff = startOff;
            localBestMetrics = state.bestMetrics();
        }

        private static boolean isCandidateImproved(
                final SearchState state,
                final CandidateInfo candidate,
                final boolean macroSweep) {
            final SingleCandidateSearch search = new SingleCandidateSearch(state, candidate.className());
            return search.isImproved(macroSweep);
        }

        private boolean isImproved(final boolean macroSweep) {
            final double startScore = state.bestMetrics().score();
            final int rowSpan = Math.max(MIN_ROW_SPAN, Math.toIntExact(Math.round(Math.ceil(state.bestMetrics().rows()))));
            final int maxColProbe = Math.clamp(state.bestMetrics().cols() + COL_PROBE_MARGIN, MIN_COL_PROBE, MAX_COL_OFFSET);

            final Map<Integer, Double> bestScoreByCol = probeCoarseColumns(startScore, maxColProbe);
            if (state.isActive()) {
                probeVerticalBands(bestScoreByCol, rowSpan);
            }
            if (state.isActive()) {
                polishColumns();
            }
            if (state.isActive()) {
                polishRows(macroSweep);
            }

            final boolean improved = localBestMetrics.score() < startScore - SCORE_EPSILON;
            if (improved) {
                state.acceptBest(className, localBestOff, localBestMetrics);
            }
            return improved;
        }

        private Map<Integer, Double> probeCoarseColumns(final double startScore, final int maxColProbe) {
            final List<Integer> coarseCols = buildCoarseCols(startOff.colOffset(), maxColProbe);
            final Map<Integer, Double> bestScoreByCol = new LinkedHashMap<>(coarseCols.size() << 1);
            bestScoreByCol.put(startOff.colOffset(), startScore);

            final Collection<Integer> probeBaseRows = new LinkedHashSet<>(INITIAL_CAPACITY_SMALL);
            probeBaseRows.add(startOff.rowOffset());
            probeBaseRows.add(0);

            int idx = 0;
            while (idx < coarseCols.size() && state.isActive()) {
                probeColumnAtBaseRows(coarseCols.get(idx), probeBaseRows, bestScoreByCol);
                idx++;
            }
            return bestScoreByCol;
        }

        private void probeColumnAtBaseRows(
                final int col,
                final Iterable<Integer> probeBaseRows,
                final Map<? super Integer, Double> bestScoreByCol) {
            for (final int baseRow : probeBaseRows) {
                if (state.isActive()) {
                    final double score = tryCandidate(col, baseRow);
                    if (!Double.isNaN(score)) {
                        bestScoreByCol.merge(col, score, Math::min);
                    }
                }
            }
        }

        private void probeVerticalBands(final Map<Integer, Double> bestScoreByCol, final int rowSpan) {
            final Collection<Integer> rowProbeCols = selectRowProbeColumns(bestScoreByCol);
            final List<Integer> macroRowBands = buildCoarseRows(startOff.rowOffset(), rowSpan, true);
            final List<Integer> zeroColRows = buildCoarseRows(startOff.rowOffset(), rowSpan, false);

            for (final int col : rowProbeCols) {
                if (state.isActive()) {
                    final List<Integer> rowsToTest = (0 == col) ? zeroColRows : macroRowBands;
                    probeColumnRows(col, rowsToTest);
                }
            }
        }

        private Collection<Integer> selectRowProbeColumns(final Map<Integer, Double> bestScoreByCol) {
            final List<Integer> topCols = new ArrayList<>(bestScoreByCol.keySet());
            topCols.sort(Comparator.comparingDouble(col -> bestScoreByCol.getOrDefault(col, Double.POSITIVE_INFINITY)));
            final Collection<Integer> rowProbeCols = new LinkedHashSet<>(INITIAL_CAPACITY_SMALL);
            rowProbeCols.add(localBestOff.colOffset());
            int idx = 0;
            while (idx < topCols.size() && TOP_COL_COUNT > rowProbeCols.size()) {
                rowProbeCols.add(topCols.get(idx));
                idx++;
            }
            rowProbeCols.add(0);
            return rowProbeCols;
        }

        private void probeColumnRows(final int col, final List<Integer> rowsToTest) {
            int idx = 0;
            while (idx < rowsToTest.size() && state.isActive()) {
                tryCandidate(col, rowsToTest.get(idx));
                idx++;
            }
        }

        private void polishColumns() {
            final int baseCol = localBestOff.colOffset();
            final int baseRow = localBestOff.rowOffset();
            for (int dc = -FINE_COL_RADIUS; FINE_COL_RADIUS >= dc && state.isActive(); dc++) {
                final int col = baseCol + dc;
                if (0 != dc && 0 <= col && MAX_COL_OFFSET >= col) {
                    evaluateAndAdopt(col, baseRow);
                }
            }
        }

        private void polishRows(final boolean macroSweep) {
            final int baseCol = localBestOff.colOffset();
            final int baseRow = localBestOff.rowOffset();
            final int[] deltas = macroSweep ? MACRO_ROW_DELTAS : REFINE_ROW_DELTAS;
            final List<Integer> fineRows = buildFineRows(baseRow, deltas);
            int idx = 0;
            while (idx < fineRows.size() && state.isActive()) {
                tryCandidate(baseCol, fineRows.get(idx));
                idx++;
            }
        }

        private static List<Integer> buildFineRows(final int baseRow, final int[] deltas) {
            final Collection<Integer> fineRows = new LinkedHashSet<>(INITIAL_CAPACITY_MEDIUM);
            fineRows.add(0);
            for (final int delta : deltas) {
                final int clamped = clampRow(baseRow + delta);
                if (clamped != baseRow) {
                    fineRows.add(clamped);
                }
            }
            return new ArrayList<>(fineRows);
        }

        private double tryCandidate(final int col, final int row) {
            double score = Double.NaN;
            if (col != localBestOff.colOffset() || row != localBestOff.rowOffset()) {
                score = evaluateAndAdopt(col, row);
            }
            return score;
        }

        private double evaluateAndAdopt(final int col, final int row) {
            final ScenarioOffsetSourceCodec.Offsets next = new ScenarioOffsetSourceCodec.Offsets(col, row);
            trial.put(className, next);
            final LayoutMetrics metrics = state.evaluate(trial);
            if (metrics.score() < localBestMetrics.score() - SCORE_EPSILON) {
                localBestOff = next;
                localBestMetrics = metrics;
            }
            return metrics.score();
        }

        private static List<Integer> buildCoarseCols(final int currentCol, final int maxCol) {
            final Set<Integer> cols = new TreeSet<>();
            cols.add(0);
            cols.add(Math.clamp(currentCol, 0, MAX_COL_OFFSET));
            cols.add(1);
            cols.add(2);
            for (final int step : COARSE_COL_STEPS) {
                if (step <= maxCol) {
                    cols.add(step);
                }
            }
            return new ArrayList<>(cols);
        }

        private static List<Integer> buildCoarseRows(
                final int currentRow,
                final int rowSpan,
                final boolean shiftedCol) {
            final Collection<Integer> rows = new LinkedHashSet<>(INITIAL_CAPACITY_SMALL);
            rows.add(0);
            rows.add(clampRow(currentRow));
            if (shiftedCol) {
                final int half = Math.clamp(rowSpan / 2, MIN_HALF_ROW, MAX_ROW_OFFSET);
                final int quarter = Math.max(MIN_QUARTER_ROW, half / 2);
                rows.add(-half);
                rows.add(-quarter);
                rows.add(quarter);
                rows.add(half);
            } else {
                final int mild = Math.clamp(rowSpan / MILD_ROW_DIVISOR, MILD_ROW_MIN, MILD_ROW_MAX);
                rows.add(-mild);
                rows.add(mild);
                if (WIDE_ROW_SPAN_MIN <= rowSpan) {
                    final int wide = Math.min(MAX_ROW_OFFSET, rowSpan / 2);
                    rows.add(-wide);
                    rows.add(wide);
                }
            }
            return new ArrayList<>(rows);
        }

        private static int clampRow(final int row) {
            return Math.clamp(row, -MAX_ROW_OFFSET, MAX_ROW_OFFSET);
        }
    }

    private enum OffsetPruner {
        ;

        /**
         * Drops redundant shifts and shrinks inflated offset values when the resulting layout
         * stays equally good or better.
         */
        private static void pruneAndSimplify(final SearchState state) {
            final List<String> shifted = collectShiftedClasses(state.bestOffsets());
            if (!shifted.isEmpty()) {
                final Map<String, ScenarioOffsetSourceCodec.Offsets> trial = state.copyBestOffsets();
                int idx = 0;
                while (idx < shifted.size() && state.isActive()) {
                    simplifyCandidate(state, trial, shifted.get(idx));
                    idx++;
                    state.report(idx, shifted.size(), STAGE_SIMPLIFYING_OFFSETS);
                }
            }
        }

        private static List<String> collectShiftedClasses(
                final Map<String, ScenarioOffsetSourceCodec.Offsets> offsets) {
            final List<String> shifted = new ArrayList<>(INITIAL_CAPACITY_MEDIUM);
            for (final Map.Entry<String, ScenarioOffsetSourceCodec.Offsets> entry : offsets.entrySet()) {
                if (!entry.getValue().isZero()) {
                    shifted.add(entry.getKey());
                }
            }
            return shifted;
        }

        private static void simplifyCandidate(
                final SearchState state,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> trial,
                final String className) {
            ScenarioOffsetSourceCodec.Offsets cur = state.offsetFor(className);
            cur = tryDropRowOffset(state, trial, className, cur);
            if (state.isActive()) {
                cur = tryDropColOffset(state, trial, className, cur);
            }
            cur = shrinkColOffset(state, trial, className, cur);
            shrinkRowOffset(state, trial, className, cur);
        }

        private static ScenarioOffsetSourceCodec.Offsets tryDropRowOffset(
                final SearchState state,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> trial,
                final String className,
                final ScenarioOffsetSourceCodec.Offsets cur) {
            ScenarioOffsetSourceCodec.Offsets updated = cur;
            if (0 != cur.rowOffset()) {
                updated = tryReplaceOffset(state, trial, className, cur, cur.colOffset(), 0, PRUNE_DROP_TOLERANCE);
            }
            return updated;
        }

        private static ScenarioOffsetSourceCodec.Offsets tryDropColOffset(
                final SearchState state,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> trial,
                final String className,
                final ScenarioOffsetSourceCodec.Offsets cur) {
            ScenarioOffsetSourceCodec.Offsets updated = cur;
            if (0 != cur.colOffset()) {
                updated = tryReplaceOffset(state, trial, className, cur, 0, cur.rowOffset(), PRUNE_DROP_TOLERANCE);
            }
            return updated;
        }

        private static ScenarioOffsetSourceCodec.Offsets shrinkColOffset(
                final SearchState state,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> trial,
                final String className,
                final ScenarioOffsetSourceCodec.Offsets initial) {
            ScenarioOffsetSourceCodec.Offsets cur = initial;
            boolean shrinking = true;
            while (1 < cur.colOffset() && shrinking && state.isActive()) {
                final ScenarioOffsetSourceCodec.Offsets next = tryReplaceOffset(
                        state, trial, className, cur, cur.colOffset() - 1, cur.rowOffset(), PRUNE_SHRINK_TOLERANCE);
                shrinking = !next.equals(cur);
                cur = next;
            }
            return cur;
        }

        private static void shrinkRowOffset(
                final SearchState state,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> trial,
                final String className,
                final ScenarioOffsetSourceCodec.Offsets initial) {
            ScenarioOffsetSourceCodec.Offsets cur = initial;
            boolean shrinking = true;
            while (1 < Math.abs(cur.rowOffset()) && shrinking && state.isActive()) {
                final int stepTowardZero = computeRowShrinkTarget(cur.rowOffset());
                final ScenarioOffsetSourceCodec.Offsets next = tryReplaceOffset(
                        state, trial, className, cur, cur.colOffset(), stepTowardZero, PRUNE_SHRINK_TOLERANCE);
                shrinking = !next.equals(cur);
                cur = next;
            }
        }

        private static int computeRowShrinkTarget(final int rowOffset) {
            final int target;
            if (0 < rowOffset) {
                target = Math.max(0, rowOffset - Math.max(1, rowOffset / ROW_SHRINK_DIVISOR));
            } else {
                target = Math.min(0, rowOffset + Math.max(1, -rowOffset / ROW_SHRINK_DIVISOR));
            }
            return target;
        }

        private static ScenarioOffsetSourceCodec.Offsets tryReplaceOffset(
                final SearchState state,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> trial,
                final String className,
                final ScenarioOffsetSourceCodec.Offsets cur,
                final int nextCol,
                final int nextRow,
                final double tolerance) {
            final ScenarioOffsetSourceCodec.Offsets candidate =
                    new ScenarioOffsetSourceCodec.Offsets(nextCol, nextRow);
            trial.put(className, candidate);
            final LayoutMetrics metrics = state.evaluate(trial);
            final ScenarioOffsetSourceCodec.Offsets result;
            if (metrics.score() <= state.bestMetrics().score() + tolerance) {
                state.acceptBest(className, candidate, metrics);
                result = candidate;
            } else {
                trial.put(className, cur);
                result = cur;
            }
            return result;
        }
    }

    private enum CandidateSelector {
        ;

        private static List<CandidateInfo> collectCandidates(
                final Collection<TreeLayoutEditorModel.ScenarioBlueprint> roots,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> startOffsets) {
            final Map<String, CandidateInfo> byClass = new LinkedHashMap<>(INITIAL_CAPACITY_LARGE);
            final boolean singleRoot = 1 == roots.size();
            for (final TreeLayoutEditorModel.ScenarioBlueprint root : roots) {
                collectRecursive(root, 0, singleRoot, byClass);
            }
            return prioritizeCandidates(byClass.values(), startOffsets);
        }

        private static List<CandidateInfo> prioritizeCandidates(
                final Iterable<CandidateInfo> allInfos,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> startOffsets) {
            final List<CandidateInfo> shifted = new ArrayList<>(INITIAL_CAPACITY_MEDIUM);
            final List<CandidateInfo> branches = new ArrayList<>(INITIAL_CAPACITY_LARGE);
            final List<CandidateInfo> leaves = new ArrayList<>(INITIAL_CAPACITY_LARGE);
            partitionCandidates(allInfos, startOffsets, shifted, branches, leaves);

            final Comparator<CandidateInfo> bySizeDesc = Comparator.comparingInt(CandidateInfo::subtreeSize).reversed()
                    .thenComparingInt(CandidateInfo::depth)
                    .thenComparing(CandidateInfo::className);
            shifted.sort(bySizeDesc);
            branches.sort(bySizeDesc);
            leaves.sort(bySizeDesc);

            final List<CandidateInfo> selected = new ArrayList<>(shifted);
            appendUpToLimit(selected, branches, MAX_CANDIDATES);
            if (LEAF_FILL_THRESHOLD > selected.size()) {
                appendUpToLimit(selected, leaves, Math.min(MAX_WITH_LEAVES, MAX_CANDIDATES));
            }
            return selected;
        }

        private static void partitionCandidates(
                final Iterable<CandidateInfo> allInfos,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> startOffsets,
                final Collection<? super CandidateInfo> shifted,
                final Collection<? super CandidateInfo> branches,
                final Collection<? super CandidateInfo> leaves) {
            for (final CandidateInfo info : allInfos) {
                final ScenarioOffsetSourceCodec.Offsets off =
                        startOffsets.getOrDefault(info.className(), ScenarioOffsetSourceCodec.Offsets.ZERO);
                if (!off.isZero()) {
                    shifted.add(info);
                } else if (!info.leaf()) {
                    branches.add(info);
                } else {
                    leaves.add(info);
                }
            }
        }

        private static void appendUpToLimit(
                final Collection<? super CandidateInfo> target,
                final List<CandidateInfo> source,
                final int limit) {
            int idx = 0;
            while (idx < source.size() && limit > target.size()) {
                target.add(source.get(idx));
                idx++;
            }
        }

        private static void collectRecursive(
                final TreeLayoutEditorModel.ScenarioBlueprint bp,
                final int depth,
                final boolean skipSingleRoot,
                final Map<? super String, CandidateInfo> out) {
            final boolean include = !skipSingleRoot || 0 != depth;
            if (include) {
                final CandidateInfo existing = out.get(bp.className());
                if (null == existing || bp.subtreeSize() > existing.subtreeSize()) {
                    out.put(bp.className(), new CandidateInfo(bp.className(), bp.subtreeSize(), depth, bp.isLeaf()));
                }
            }
            for (final TreeLayoutEditorModel.ScenarioBlueprint child : bp.children()) {
                collectRecursive(child, depth + 1, skipSingleRoot, out);
            }
        }

        private static boolean isSameOrder(final List<CandidateInfo> first, final List<CandidateInfo> second) {
            boolean same = first.size() == second.size();
            int idx = 0;
            while (same && idx < first.size()) {
                same = Objects.equals(first.get(idx).className(), second.get(idx).className());
                idx++;
            }
            return same;
        }
    }

    private record OffsetStats(int shiftedCount, double offsetPenalty) {
    }

    private enum GeometryScorer {
        ;

        private static double aspectRatio(final TreeLayoutEditorModel.PreviewResult preview) {
            double ratio = 0.0;
            if (null != preview && !preview.layout().nodes().isEmpty()) {
                final double[] dims = pixelDimensions(preview.layout().nodes());
                ratio = (0.0 < dims[1]) ? (dims[0] / dims[1]) : 0.0;
            }
            return ratio;
        }

        private static LayoutMetrics computeMetrics(
                final TreeLayoutEditorModel.PreviewResult preview,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> offsets,
                final AspectPreset preset) {
            final List<TreeHtml.NodeData> nodes = (null == preview) ? List.of() : preview.layout().nodes();
            final List<TreeHtml.Edge> edges = (null == preview) ? List.of() : preview.layout().edges();
            final LayoutMetrics result;
            if (nodes.isEmpty()) {
                result = EMPTY_METRICS;
            } else {
                result = scoreNonEmptyLayout(preview, nodes, edges, offsets, preset);
            }
            return result;
        }

        private static LayoutMetrics scoreNonEmptyLayout(
                final TreeLayoutEditorModel.PreviewResult preview,
                final Collection<TreeHtml.NodeData> nodes,
                final List<TreeHtml.Edge> edges,
                final Map<String, ScenarioOffsetSourceCodec.Offsets> offsets,
                final AspectPreset preset) {
            final double[] dims = pixelDimensions(nodes);
            final double pixelW = dims[0];
            final double pixelH = dims[1];
            final double ratio = (0.0 < pixelH) ? (pixelW / pixelH) : 1.0;
            final int overlaps = preview.overlapCount();
            final double primarySize = computePrimarySize(pixelW, pixelH, preset.targetRatio());
            final int occlusions = countEdgeOcclusions(nodes, edges);
            final int crossings = countEdgeCrossings(edges);
            final double stretchPenalty = computeStretchPenalty(edges);
            final OffsetStats offsetStats = computeOffsetStats(offsets);

            final double overlapCost = (0 < overlaps) ? (OVERLAP_PENALTY * overlaps) : 0.0;
            final double edgeCost = occlusions * OCCLUSION_PENALTY + crossings * CROSSING_PENALTY + stretchPenalty;
            final double score = overlapCost + primarySize + edgeCost + offsetStats.offsetPenalty();

            return new LayoutMetrics(
                    preview.totalCols(),
                    preview.totalRows(),
                    pixelW,
                    pixelH,
                    ratio,
                    overlaps,
                    offsetStats.shiftedCount(),
                    crossings,
                    occlusions,
                    score);
        }

        private static double computePrimarySize(
                final double pixelW,
                final double pixelH,
                final double targetRatio) {
            final double sqrtActualArea = Math.sqrt(pixelW * pixelH);
            final double primarySize;
            if (0.0 < targetRatio) {
                final double enclosingH = Math.max(pixelW / targetRatio, pixelH);
                final double sqrtEnclosingArea = enclosingH * Math.sqrt(targetRatio);
                primarySize = ENCLOSING_AREA_WEIGHT * sqrtEnclosingArea + ACTUAL_AREA_WEIGHT * sqrtActualArea;
            } else {
                primarySize = sqrtActualArea;
            }
            return primarySize;
        }

        private static double computeStretchPenalty(final Iterable<TreeHtml.Edge> edges) {
            double stretchPenalty = 0.0;
            for (final TreeHtml.Edge edge : edges) {
                final int dxCols = Math.abs(edge.to().col() - edge.from().col());
                final double dyRows = Math.abs(edge.to().row() - edge.from().row());
                if (1 < dxCols) {
                    stretchPenalty += (dxCols - 1) * LONG_COL_EDGE_PENALTY;
                }
                if (LONG_ROW_EDGE_THRESHOLD < dyRows) {
                    stretchPenalty += (dyRows - LONG_ROW_EDGE_THRESHOLD) * LONG_ROW_EDGE_PENALTY;
                }
            }
            return stretchPenalty;
        }

        private static OffsetStats computeOffsetStats(
                final Map<String, ScenarioOffsetSourceCodec.Offsets> offsets) {
            int shiftedCount = 0;
            double offsetPenalty = 0.0;
            if (null != offsets) {
                for (final ScenarioOffsetSourceCodec.Offsets off : offsets.values()) {
                    if (null != off && !off.isZero()) {
                        shiftedCount++;
                        offsetPenalty += singleOffsetPenalty(off);
                    }
                }
            }
            return new OffsetStats(shiftedCount, offsetPenalty);
        }

        private static double singleOffsetPenalty(final ScenarioOffsetSourceCodec.Offsets off) {
            double penalty = 0.0;
            if (0 != off.colOffset()) {
                penalty += COL_OFFSET_ANNOTATION_PENALTY + off.colOffset() * OFFSET_MAGNITUDE_PENALTY;
            }
            if (0 != off.rowOffset()) {
                penalty += ROW_OFFSET_ANNOTATION_PENALTY + Math.abs(off.rowOffset()) * OFFSET_MAGNITUDE_PENALTY;
            }
            return penalty;
        }

        private static double[] pixelDimensions(final Iterable<TreeHtml.NodeData> nodes) {
            int minCol = Integer.MAX_VALUE;
            int maxCol = Integer.MIN_VALUE;
            double minRow = Double.POSITIVE_INFINITY;
            double maxRow = Double.NEGATIVE_INFINITY;
            for (final TreeHtml.NodeData node : nodes) {
                if (node.col() < minCol) {
                    minCol = node.col();
                }
                if (node.col() > maxCol) {
                    maxCol = node.col();
                }
                if (node.row() < minRow) {
                    minRow = node.row();
                }
                if (node.row() > maxRow) {
                    maxRow = node.row();
                }
            }
            final double pixelW = Math.max(CARD_WIDTH, (maxCol - minCol) * COL_WIDTH + CARD_WIDTH);
            final double pixelH = Math.max(CARD_HEIGHT, (maxRow - minRow) * ROW_HEIGHT + CARD_HEIGHT);
            return new double[]{pixelW, pixelH};
        }

        private static int countEdgeOcclusions(
                final Collection<TreeHtml.NodeData> nodes,
                final Collection<TreeHtml.Edge> edges) {
            int occlusions = 0;
            if (!edges.isEmpty() && MIN_NODES_FOR_OCCLUSION < nodes.size()) {
                final Map<Integer, List<TreeHtml.NodeData>> byCol = groupNodesByCol(nodes);
                for (final TreeHtml.Edge edge : edges) {
                    occlusions += countOcclusionsForEdge(edge, byCol);
                }
            }
            return occlusions;
        }

        private static Map<Integer, List<TreeHtml.NodeData>> groupNodesByCol(
                final Iterable<TreeHtml.NodeData> nodes) {
            final Map<Integer, List<TreeHtml.NodeData>> byCol = new HashMap<>(INITIAL_CAPACITY_MEDIUM);
            nodes.forEach(node -> byCol.computeIfAbsent(node.col(), _ -> new ArrayList<>(INITIAL_CAPACITY_SMALL)).add(node));
            return byCol;
        }

        private static int countOcclusionsForEdge(
                final TreeHtml.Edge edge,
                final Map<Integer, ? extends List<TreeHtml.NodeData>> byCol) {
            int occlusions = 0;
            final TreeHtml.NodeData parent = edge.from();
            final TreeHtml.NodeData child = edge.to();
            final int colSpan = child.col() - parent.col();
            if (1 < colSpan) {
                final double x1 = parent.col() * COL_WIDTH + CARD_WIDTH;
                final double y1 = parent.row() * ROW_HEIGHT;
                final double x2 = child.col() * COL_WIDTH;
                final double y2 = child.row() * ROW_HEIGHT;
                final double dx = x2 - x1;
                if (SCORE_EPSILON < dx) {
                    occlusions = countSpanOcclusions(parent, child, byCol, x1, y1, y2, dx);
                }
            }
            return occlusions;
        }

        private static int countSpanOcclusions(
                final TreeHtml.NodeData parent,
                final TreeHtml.NodeData child,
                final Map<Integer, ? extends List<TreeHtml.NodeData>> byCol,
                final double x1,
                final double y1,
                final double y2,
                final double dx) {
            int occlusions = 0;
            for (int col = parent.col() + 1; col < child.col(); col++) {
                final List<TreeHtml.NodeData> colNodes = byCol.get(col);
                if (null != colNodes) {
                    final double xMid = col * COL_WIDTH + CARD_WIDTH * HALF;
                    final double ratio = Math.clamp((xMid - x1) / dx, 0.0, 1.0);
                    final double smooth = ratio * ratio * (SMOOTHSTEP_A - SMOOTHSTEP_B * ratio);
                    final double yAtCol = y1 + (y2 - y1) * smooth;
                    occlusions += countColumnOcclusions(colNodes, parent, child, yAtCol);
                }
            }
            return occlusions;
        }

        private static int countColumnOcclusions(
                final Iterable<TreeHtml.NodeData> colNodes,
                final TreeHtml.NodeData parent,
                final TreeHtml.NodeData child,
                final double yAtCol) {
            int occlusions = 0;
            for (final TreeHtml.NodeData node : colNodes) {
                if (node != parent && node != child && OCCLUSION_HIT_HALF_HEIGHT > Math.abs(node.row() * ROW_HEIGHT - yAtCol)) {
                    occlusions++;
                }
            }
            return occlusions;
        }

        private static int countEdgeCrossings(final List<TreeHtml.Edge> edges) {
            int crossings = 0;
            final int count = edges.size();
            if (MIN_EDGES_FOR_CROSSING <= count) {
                final EdgeSegments segments = EdgeSegments.fromEdges(edges);
                for (int i = 0; i < count; i++) {
                    crossings += segments.countCrossingsFrom(i);
                }
            }
            return crossings;
        }
    }

    private static final class EdgeSegments {

        private final int count;
        private final double[] x1;
        private final double[] y1;
        private final double[] x2;
        private final double[] y2;
        private final double[] minX;
        private final double[] maxX;
        private final double[] minY;
        private final double[] maxY;
        private final TreeHtml.NodeData[] endpoints;

        private EdgeSegments(final int count) {
            this.count = count;
            x1 = new double[count];
            y1 = new double[count];
            x2 = new double[count];
            y2 = new double[count];
            minX = new double[count];
            maxX = new double[count];
            minY = new double[count];
            maxY = new double[count];
            endpoints = new TreeHtml.NodeData[count << 1];
        }

        private static EdgeSegments fromEdges(final List<TreeHtml.Edge> edges) {
            final int count = edges.size();
            final EdgeSegments segments = new EdgeSegments(count);
            for (int i = 0; i < count; i++) {
                segments.populateEdge(i, edges.get(i));
            }
            return segments;
        }

        private void populateEdge(final int index, final TreeHtml.Edge edge) {
            final TreeHtml.NodeData parent = edge.from();
            final TreeHtml.NodeData child = edge.to();
            endpoints[index << 1] = parent;
            endpoints[(index << 1) + 1] = child;
            x1[index] = parent.col() * COL_WIDTH + CARD_WIDTH;
            y1[index] = parent.row() * ROW_HEIGHT;
            x2[index] = child.col() * COL_WIDTH;
            y2[index] = child.row() * ROW_HEIGHT;
            minX[index] = Math.min(x1[index], x2[index]);
            maxX[index] = Math.max(x1[index], x2[index]);
            minY[index] = Math.min(y1[index], y2[index]);
            maxY[index] = Math.max(y1[index], y2[index]);
        }

        private int countCrossingsFrom(final int first) {
            int crossings = 0;
            for (int second = first + 1; second < count; second++) {
                if (isCrossingPair(first, second)) {
                    crossings++;
                }
            }
            return crossings;
        }

        private boolean isCrossingPair(final int first, final int second) {
            final boolean crossing;
            if (isSharedEndpoint(first, second) || isDisjointBounds(first, second)) {
                crossing = false;
            } else {
                crossing = isProperlyIntersecting(
                        x1[first], y1[first], x2[first], y2[first],
                        x1[second], y1[second], x2[second], y2[second]);
            }
            return crossing;
        }

        private boolean isSharedEndpoint(final int first, final int second) {
            final TreeHtml.NodeData p1 = endpoints[first << 1];
            final TreeHtml.NodeData c1 = endpoints[(first << 1) + 1];
            final TreeHtml.NodeData p2 = endpoints[second << 1];
            final TreeHtml.NodeData c2 = endpoints[(second << 1) + 1];
            final boolean sharesFirst = p1 == p2 || p1 == c2;
            final boolean sharesSecond = c1 == p2 || c1 == c2;
            return sharesFirst || sharesSecond;
        }

        private boolean isDisjointBounds(final int first, final int second) {
            final boolean disjointX = maxX[first] <= minX[second] || maxX[second] <= minX[first];
            final boolean disjointY = maxY[first] <= minY[second] || maxY[second] <= minY[first];
            return disjointX || disjointY;
        }

        private static boolean isProperlyIntersecting(
                final double ax, final double ay, final double bx, final double by,
                final double cx, final double cy, final double dx, final double dy) {
            final double o1 = orient(ax, ay, bx, by, cx, cy);
            final double o2 = orient(ax, ay, bx, by, dx, dy);
            final boolean intersects;
            if (isSameSideOrCollinear(o1, o2)) {
                intersects = false;
            } else {
                final double o3 = orient(cx, cy, dx, dy, ax, ay);
                final double o4 = orient(cx, cy, dx, dy, bx, by);
                intersects = isOppositeSides(o3, o4);
            }
            return intersects;
        }

        private static boolean isSameSideOrCollinear(final double o1, final double o2) {
            final boolean bothAtLeastZero = 0.0 <= o1 && 0.0 <= o2;
            final boolean bothAtMostZero = 0.0 >= o1 && 0.0 >= o2;
            return bothAtLeastZero || bothAtMostZero;
        }

        private static boolean isOppositeSides(final double o3, final double o4) {
            final boolean posNeg = 0.0 < o3 && 0.0 > o4;
            final boolean negPos = 0.0 > o3 && 0.0 < o4;
            return posNeg || negPos;
        }

        private static double orient(
                final double ax, final double ay, final double bx, final double by,
                final double cx, final double cy) {
            return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        }
    }
}
