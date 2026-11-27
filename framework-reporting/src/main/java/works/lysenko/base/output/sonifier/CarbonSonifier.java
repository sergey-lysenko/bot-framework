package works.lysenko.base.output.sonifier;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.sonifier.CarbonSynth.SynthEvent;
import works.lysenko.base.output.sonifier.CarbonSynth.VoiceKind;
import works.lysenko.base.output.tree.TreeCameraTransform;
import works.lysenko.base.output.tree.TreeProgressMetrics;
import works.lysenko.util.data.enums.ScenarioType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static works.lysenko.util.data.strs.Swap.s;

/**
 * Spatial physical-synthesis and polyrhythmic telemetry sonifier ({@code carbon}).
 * <p>
 * Treats the scenario execution tree as a two-dimensional stereo acoustic instrument governed by
 * continuous <i>harmonic buoyancy</i> (lifting from grounded D Dorian tension into weightless
 * D Lydian Maj9 #11 as leaf coverage rises and inactive branches are shed):
 * <ul>
 *     <li><b>True Stereo Topology Mapping</b>: Horizontal column depth pans continuously from Left to Right
 *     using constant-power stereo law, while vertical row position shapes voicing contour within an
 *     ergonomic register that never shrieks even on 20+ column trees.</li>
 *     <li><b>Adaptive Dual-Regime Rhythm</b>: Automatically adapts between single-node step traversal
 *     ({@code per.node = true}, gliding left-to-right with familiarity-damped marimba plucks and limbo
 *     sub-bass heartbeats) and macro-cycle frames ({@code per.node = false}, micro-strumming branch
 *     trajectories into luminous celesta leaf blooms while damping Monte-Carlo over-executions into
 *     delicate glass telemetry ticks).</li>
 *     <li><b>Nuanced Semantic Statuses</b>: Distinguishes {@code NOTICE} inflections, raw vs.
 *     {@code known-issue} warnings, contained {@code known-issue} failures, and unexpected raw
 *     {@code FAILED} regressions (rendered as visceral downward gravitational pitch drops).</li>
 *     <li><b>Weightless Pruning Updrafts &amp; Stereo Diffuser</b>: Renders upward harmonic sweeps when
 *     {@code hide.off} prunes dead branches, smooth camera-settling shimmers during cooldown frames,
 *     and a warm cross-feedback stereo delay tail.</li>
 * </ul>
 */
public final class CarbonSonifier implements TreeSonifier {

    /**
     * Configuration property value activating the Carbon sonifier.
     */
    public static final String MODE = "carbon";

    private static final int[][] BUOYANCY_SCALES = {
            {0, 2, 3, 7, 9, 10, 14},  // Field 0: D Dorian add9 (grounded)
            {0, 2, 4, 6, 7, 9, 11},   // Field 1: F Lydian (first lift)
            {0, 2, 4, 7, 9, 10, 14},  // Field 2: G Mixolydian add9 (forward flight)
            {0, 2, 4, 7, 9, 11, 14},  // Field 3: A Major 6/9 (open sky)
            {0, 2, 4, 6, 7, 9, 11}    // Field 4: D Lydian Maj9 #11 (weightless)
    };

    private static final int[] PAD_ROOTS = {50, 53, 55, 57, 50};
    private static final int[] MELODY_ROOTS = {50, 53, 55, 57, 57};
    private static final int[] CLEAN_FINALE_OFFSETS = {0, 4, 7, 11, 18}; // Maj9(#11)
    private static final int[] REGRESSION_FINALE_OFFSETS = {0, 3, 7, 10, 14}; // Dorian m9

    private static final int PAD_INTERVAL_FRAMES = 12;
    private static final int KNOWN_ISSUE_COOLDOWN_FRAMES = 4;

    private static final double PAD_ROOT_AMP = 0.068;
    private static final double PAD_FIFTH_AMP = 0.050;
    private static final double PAD_COLOR_AMP = 0.044;
    private static final double BRANCH_PLUCK_AMP = 0.115;
    private static final double PARTIAL_LEAF_AMP = 0.125;
    private static final double DISCOVERY_BLOOM_AMP = 0.155;
    private static final double TELEMETRY_TICK_AMP = 0.088;
    private static final double NOTICE_AMP = 0.082;
    private static final double WARNING_RAW_AMP = 0.118;
    private static final double WARNING_KNOWN_AMP = 0.062;
    private static final double FAILURE_KNOWN_AMP = 0.095;
    private static final double FAILURE_RAW_AMP = 0.265;
    private static final double UPDRAFT_AMP = 0.088;
    private static final double HEARTBEAT_AMP = 0.075;
    private static final double SHIMMER_AMP = 0.052;
    private static final double FINALE_AMP = 0.125;

    /**
     * Resolves the sidecar snapshot file corresponding to a rendered progression frame.
     *
     * @param frame rendered progression frame ({@code .svg} or {@code .png})
     * @return sidecar {@code .carbon} file inside {@code carbon_events/}
     */
    public static File snapshotFile(final File frame) {
        return new File(new File(frame.getParentFile(), "carbon_events"),
                frame.getName().replaceFirst("\\.(png|svg)$", ".carbon"));
    }

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public void capture(final File frame, final TreeLayout layout, final int target) throws IOException {
        capture(frame, layout, target, null);
    }

    @Override
    public void capture(
            final File frame,
            final TreeLayout layout,
            final int target,
            final TreeHtml.AccessibleContext precomputedCtx) throws IOException {
        final File snapshot = snapshotFile(frame);
        Files.createDirectories(snapshot.toPath().getParent());

        final TreeHtml.AccessibleContext accessibleCtx = (null != precomputedCtx)
                ? precomputedCtx : new TreeHtml.AccessibleContext(Set.of());
        final Map<NodeData, Boolean> executableCache = new HashMap<>();
        final Map<NodeData, Boolean> childFailureCache = new HashMap<>();

        final int safeTarget = Math.max(1, target);
        final TreeCameraTransform.Transform camera = TreeCameraTransform.computeZoomTransform(
                layout, Set.of(), safeTarget, 1920, 1080);

        final List<NodeData> orderedNodes = new ArrayList<>(layout.nodes());
        orderedNodes.sort(Comparator.comparingInt(NodeData::col)
                .thenComparingDouble(NodeData::row)
                .thenComparing(NodeData::id));

        int executableLeaves = 0;
        int coveredLeaves = 0;
        for (final NodeData node : orderedNodes) {
            final int execs = (null != node.result()) ? Math.max(0, node.result().getExecutions()) : 0;
            if (node.children().isEmpty() && TreeProgressMetrics.isTerminalScenario(node)) {
                if (TreeHtml.isTheoreticallyExecutable(node, accessibleCtx, executableCache) || execs > 0) {
                    executableLeaves++;
                    if (execs >= TreeProgressMetrics.getTargetExecutions(node, safeTarget)) {
                        coveredLeaves++;
                    }
                }
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                snapshot.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(String.format(Locale.US, "#meta\t%.4f\t%.2f\t%.2f\t%d\t%d",
                    camera.zoom(), camera.translateX(), camera.translateY(), executableLeaves, coveredLeaves));
            writer.newLine();

            for (final NodeData node : orderedNodes) {
                final String encodedPath = Base64.getUrlEncoder().withoutPadding().encodeToString(
                        lineagePath(node).getBytes(StandardCharsets.UTF_8));
                final int col = Math.max(0, node.col());
                final int rowTenths = (int) Math.round(Math.max(0.0, node.row()) * 10.0);
                final int execs = (null != node.result()) ? Math.max(0, node.result().getExecutions()) : 0;
                final ScenarioType scenarioType = TreeProgressMetrics.getScenarioType(node);
                final boolean executable = TreeHtml.isTheoreticallyExecutable(node, accessibleCtx, executableCache) || execs > 0;
                final char typeCode = resolveTypeCode(node, scenarioType, executable);
                final int nodeTarget = (ScenarioType.MONO == scenarioType) ? 1 : safeTarget;
                final char statusCode = resolveStatusCode(node, execs, childFailureCache);
                final char knownFlag = (null != node.knownIssues() && !node.knownIssues().isEmpty()) ? '1' : '0';

                writer.write(encodedPath);
                writer.write('\t');
                writer.write(Integer.toString(col));
                writer.write('\t');
                writer.write(Integer.toString(rowTenths));
                writer.write('\t');
                writer.write(typeCode);
                writer.write('\t');
                writer.write(Integer.toString(execs));
                writer.write('\t');
                writer.write(Integer.toString(nodeTarget));
                writer.write('\t');
                writer.write(statusCode);
                writer.write('\t');
                writer.write(knownFlag);
                writer.newLine();
            }
        }
    }

    @Override
    public boolean canSonify(final List<File> frames) {
        return !frames.isEmpty() && frames.stream().allMatch(CarbonSonifier::hasReadableFrame);
    }

    @Override
    public void writeWav(final List<File> frames, final File output, final int frameRate) throws IOException {
        if (frames.isEmpty() || frameRate < 1) {
            throw new IllegalArgumentException("Audio requires frames and a positive frame rate");
        }
        if (CarbonSynth.SAMPLE_RATE % frameRate != 0) {
            throw new IllegalArgumentException("Frame rate must divide the audio sample rate");
        }
        final int samplesPerFrame = CarbonSynth.SAMPLE_RATE / frameRate;
        final long totalSamples = (long) frames.size() * samplesPerFrame + CarbonSynth.TAIL_SAMPLES;
        final Map<Integer, List<SynthEvent>> events = composeStereoEvents(frames, samplesPerFrame);
        CarbonSynth.writeStereoWav(output, totalSamples, samplesPerFrame, events);
    }

    private static boolean hasReadableFrame(final File frame) {
        if (snapshotFile(frame).isFile()) {
            return true;
        }
        return frame.isFile()
                && frame.getName().toLowerCase(Locale.ROOT).endsWith(".svg")
                && frame.length() > 0L;
    }

    private static Map<Integer, List<SynthEvent>> composeStereoEvents(
            final List<File> frames,
            final int samplesPerFrame) throws IOException {

        final List<FrameState> states = new ArrayList<>(frames.size());
        int maxCol = 0;
        int maxRowTenths = 10;
        int initialNodeCount = 0;

        for (int i = 0; i < frames.size(); i++) {
            final FrameState state = readFrameState(frames.get(i));
            states.add(state);
            if (0 == i) {
                initialNodeCount = state.nodes().size();
            }
            for (final NodeState node : state.nodes().values()) {
                if (node.col() > maxCol) {
                    maxCol = node.col();
                }
                if (node.rowTenths() > maxRowTenths) {
                    maxRowTenths = node.rowTenths();
                }
            }
        }

        final Map<Integer, List<SynthEvent>> eventsByFrame = new HashMap<>();
        final Map<String, NodeState> previousNodes = new HashMap<>();

        int previousField = -1;
        int lastPadFrame = -PAD_INTERVAL_FRAMES;
        int lastKnownWarnFrame = -KNOWN_ISSUE_COOLDOWN_FRAMES;
        int lastKnownFailFrame = -KNOWN_ISSUE_COOLDOWN_FRAMES;
        int previousNodeCount = -1;
        int previousTestCycle = -1;
        double previousZoom = Double.NaN;
        double previousTx = Double.NaN;
        double previousTy = Double.NaN;
        boolean hadUnexpectedFailure = false;

        final int padDuration = CarbonSynth.millisToSamples(1_380);
        final int pluckDuration = CarbonSynth.millisToSamples(165);
        final int bloomDuration = CarbonSynth.millisToSamples(680);
        final int tickDuration = CarbonSynth.millisToSamples(88);
        final int heartbeatDuration = CarbonSynth.millisToSamples(250);
        final int updraftDuration = CarbonSynth.millisToSamples(520);
        final int dropDuration = CarbonSynth.millisToSamples(540);
        final int finaleBloomDuration = CarbonSynth.millisToSamples(960);

        for (int frameIndex = 0; frameIndex < states.size(); frameIndex++) {
            final FrameState current = states.get(frameIndex);
            final List<SynthEvent> frameEvents = new ArrayList<>();
            final long frameStart = (long) frameIndex * samplesPerFrame;

            final int currentNodeCount = current.nodes().size();
            final double shedRatio = (initialNodeCount > 0)
                    ? clamp01(1.0 - (double) currentNodeCount / initialNodeCount)
                    : 0.0;
            final double coverage = current.coverageRatio();
            final double buoyancy = clamp01(0.78 * coverage + 0.22 * shedRatio);

            final int field = computeBuoyancyField(buoyancy);
            final int padRoot = PAD_ROOTS[field];
            final int melodyRoot = MELODY_ROOTS[field];
            final int[] scale = BUOYANCY_SCALES[field];

            // 1. Warm stereo Horizon Pad (open 3-voice chord, never dense column clusters)
            final boolean fieldAdvanced = (field != previousField && frameIndex - lastPadFrame >= 4);
            final boolean padDue = (0 == frameIndex) || (frameIndex - lastPadFrame >= PAD_INTERVAL_FRAMES) || fieldAdvanced;
            if (padDue && !current.nodes().isEmpty()) {
                frameEvents.add(new SynthEvent(
                        frameStart, padDuration, VoiceKind.HORIZON_PAD,
                        padRoot - 12, PAD_ROOT_AMP, 0.22 + 0.30 * buoyancy, 0.0));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 18), padDuration, VoiceKind.HORIZON_PAD,
                        padRoot - 5, PAD_FIFTH_AMP, 0.28 + 0.35 * buoyancy, -0.36));
                final int colorSemitone = scale[Math.min(scale.length - 1, field >= 3 ? 3 : 2)];
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 36), padDuration, VoiceKind.HORIZON_PAD,
                        padRoot + colorSemitone, PAD_COLOR_AMP * (0.65 + 0.35 * buoyancy), 0.35 + 0.40 * buoyancy, 0.36));
                previousField = field;
                lastPadFrame = frameIndex;
            }

            // 2. Weightless pruning updraft when inactive OFF branches are shed
            if (previousNodeCount > 0 && currentNodeCount < previousNodeCount) {
                final int pruned = previousNodeCount - currentNodeCount;
                final double intensity = Math.min(1.0, Math.sqrt(pruned / 10.0));
                frameEvents.add(new SynthEvent(
                        frameStart, updraftDuration, VoiceKind.UPDRAFT_SWEEP,
                        melodyRoot, UPDRAFT_AMP * (0.55 + 0.45 * intensity), 0.50 + 0.35 * buoyancy, 0.0));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 24), bloomDuration / 2, VoiceKind.CELESTA_BLOOM,
                        melodyRoot + scale[4], UPDRAFT_AMP * 0.65 * intensity, 0.72, -0.48));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 48), bloomDuration / 2, VoiceKind.CELESTA_BLOOM,
                        melodyRoot + 12 + scale[1], UPDRAFT_AMP * 0.60 * intensity, 0.80, 0.48));
            }
            previousNodeCount = currentNodeCount;

            // Classify node-level state deltas
            final List<NodeState> progressedBranches = new ArrayList<>();
            final List<NodeState> discoveredLeaves = new ArrayList<>();
            final List<NodeState> partialLeaves = new ArrayList<>();
            final List<NodeState> overExecutedLeaves = new ArrayList<>();
            final List<NodeState> newlyNoticeNodes = new ArrayList<>();
            final List<NodeState> newlyRawWarnNodes = new ArrayList<>();
            final List<NodeState> newlyKnownWarnNodes = new ArrayList<>();
            final List<NodeState> newlyRegressionWarnNodes = new ArrayList<>();
            final List<NodeState> newlyRawFailNodes = new ArrayList<>();
            final List<NodeState> newlyKnownFailNodes = new ArrayList<>();
            final List<NodeState> newlyRegressionNodes = new ArrayList<>();

            int totalDeltaExecs = 0;

            for (final NodeState node : current.nodes().values()) {
                if ('R' == node.statusCode() || ('F' == node.statusCode() && !node.knownIssue())) {
                    hadUnexpectedFailure = true;
                }
                final NodeState prev = previousNodes.get(node.identity());
                final int oldExec = (null != prev) ? prev.executions() : 0;
                final char oldStatus = (null != prev) ? prev.statusCode() : 'N';
                final int delta = Math.max(0, node.executions() - oldExec);
                totalDeltaExecs += delta;

                if ('R' == node.statusCode() && ('R' != oldStatus || delta > 0)) {
                    newlyRegressionNodes.add(node);
                } else if ('F' == node.statusCode() && ('F' != oldStatus || delta > 0)) {
                    if (node.knownIssue()) {
                        newlyKnownFailNodes.add(node);
                    } else {
                        newlyRawFailNodes.add(node);
                    }
                } else if ('M' == node.statusCode() && ('M' != oldStatus || delta > 0)) {
                    newlyRegressionWarnNodes.add(node);
                } else if ('W' == node.statusCode() && ('W' != oldStatus || delta > 0)) {
                    if (node.knownIssue()) {
                        newlyKnownWarnNodes.add(node);
                    } else {
                        newlyRawWarnNodes.add(node);
                    }
                } else if ('I' == node.statusCode() && ('I' != oldStatus || delta > 0)) {
                    newlyNoticeNodes.add(node);
                }

                final boolean triggered = delta > 0 || (null == prev && node.recentHighlight() && node.executions() > 0);
                if (triggered) {
                    if (node.isTerminal()) {
                        if (oldExec < node.target() && node.executions() >= node.target()) {
                            discoveredLeaves.add(node);
                        } else if (node.executions() < node.target()) {
                            partialLeaves.add(node);
                        } else {
                            overExecutedLeaves.add(node);
                        }
                    } else if ('X' != node.typeCode()) {
                        progressedBranches.add(node);
                    }
                }
            }

            // 3. Left-to-right Branch Trajectory Plucks (micro-strummed in macro-cycle frames)
            int trajectoryTailMillis = 0;
            if (!progressedBranches.isEmpty()) {
                progressedBranches.sort(Comparator.comparingInt(NodeState::col)
                        .thenComparingInt(NodeState::rowTenths));
                final List<NodeState> waypoints = selectWaypoints(progressedBranches, 3);
                for (int i = 0; i < waypoints.size(); i++) {
                    final NodeState branch = waypoints.get(i);
                    final int midi = pitchForNode(melodyRoot - 12, scale, branch.col(), maxCol, branch.rowTenths(), maxRowTenths);
                    final double pan = panForColumn(branch.col(), maxCol);
                    final double familiarity = 1.0 / (1.0 + 0.52 * Math.log(Math.max(1, branch.executions())));
                    final double amp = BRANCH_PLUCK_AMP * familiarity * (0.85 + 0.15 * ((double) (i + 1) / waypoints.size()));
                    final int offsetMs = i * 22;
                    trajectoryTailMillis = offsetMs + 22;
                    frameEvents.add(new SynthEvent(
                            frameStart + subFrameOffset(samplesPerFrame, offsetMs),
                            pluckDuration,
                            VoiceKind.MARIMBA_PLUCK,
                            midi,
                            amp,
                            0.30 + 0.35 * ((double) branch.col() / Math.max(1, maxCol)),
                            pan));
                }
            }

            // 4. Terminal Leaf Discovery Bloom (newly covered leaves)
            if (!discoveredLeaves.isEmpty()) {
                discoveredLeaves.sort(Comparator.comparingInt(NodeState::col)
                        .thenComparingInt(NodeState::rowTenths));
                final int count = Math.min(2, discoveredLeaves.size());
                final double multiGain = Math.min(1.25, 0.90 + 0.18 * discoveredLeaves.size());
                for (int i = 0; i < count; i++) {
                    final NodeState leaf = discoveredLeaves.get(i);
                    final int baseMidi = pitchForNode(melodyRoot, scale, leaf.col(), maxCol, leaf.rowTenths(), maxRowTenths);
                    final int upperMidi = Math.min(92, baseMidi + (buoyancy >= 0.65 ? 7 : 4));
                    final double pan = panForColumn(leaf.col(), maxCol);
                    final int startMs = Math.min(68, trajectoryTailMillis + i * 24);
                    final double bright = clamp01(0.62 + 0.35 * buoyancy);
                    frameEvents.add(new SynthEvent(
                            frameStart + subFrameOffset(samplesPerFrame, startMs),
                            bloomDuration,
                            VoiceKind.CELESTA_BLOOM,
                            baseMidi,
                            DISCOVERY_BLOOM_AMP * multiGain,
                            bright,
                            pan));
                    frameEvents.add(new SynthEvent(
                            frameStart + subFrameOffset(samplesPerFrame, startMs + 20),
                            bloomDuration,
                            VoiceKind.CELESTA_BLOOM,
                            upperMidi,
                            DISCOVERY_BLOOM_AMP * 0.78 * multiGain,
                             clamp01(bright + 0.12),
                            clampPan(pan + 0.08)));
                }
            } else if (!partialLeaves.isEmpty()) {
                // 5. Partial Leaf Progress (expectant rising fourth dyad)
                final NodeState leaf = partialLeaves.getFirst();
                final int firstMidi = pitchForNode(melodyRoot - 12, scale, leaf.col(), maxCol, leaf.rowTenths(), maxRowTenths);
                final int secondMidi = Math.min(88, firstMidi + 5);
                final double ratio = clamp01((double) leaf.executions() / Math.max(1, leaf.target()));
                final double pan = panForColumn(leaf.col(), maxCol);
                final int startMs = Math.min(55, trajectoryTailMillis);
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, startMs),
                        pluckDuration,
                        VoiceKind.MARIMBA_PLUCK,
                        firstMidi,
                        PARTIAL_LEAF_AMP * (0.65 + 0.35 * ratio),
                        0.40 + 0.40 * ratio,
                        pan));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, startMs + 26),
                        bloomDuration / 2,
                        VoiceKind.CELESTA_BLOOM,
                        secondMidi,
                        PARTIAL_LEAF_AMP * 0.85 * (0.65 + 0.35 * ratio),
                        0.50 + 0.40 * ratio,
                        pan));
            } else if (!overExecutedLeaves.isEmpty()) {
                // 6. Monte-Carlo Over-Execution Telemetry Tick (crisp, damped against ear fatigue)
                final NodeState leaf = overExecutedLeaves.getLast();
                final int overCount = Math.max(1, leaf.executions() - leaf.target());
                final double damping = Math.max(0.30, 1.0 / (1.0 + 0.32 * (overCount - 1)));
                final int midi = Math.min(91,
                        pitchForNode(melodyRoot, scale, leaf.col(), maxCol, leaf.rowTenths(), maxRowTenths) + 12);
                final double pan = panForColumn(leaf.col(), maxCol);
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, Math.min(60, trajectoryTailMillis)),
                        tickDuration,
                        VoiceKind.TELEMETRY_TICK,
                        midi,
                        TELEMETRY_TICK_AMP * damping,
                        0.55,
                        pan));
            }

            // 7. Semantic Telemetry Cues (Notice, Warning, Reg. Warning, Known Failure, Unexpected Failure, Regression)
            if (!newlyRegressionNodes.isEmpty()) {
                final NodeState reg = newlyRegressionNodes.getFirst();
                final double pan = panForColumn(reg.col(), maxCol);
                final double boost = Math.min(1.25, Math.sqrt(newlyRegressionNodes.size()));
                frameEvents.add(new SynthEvent(
                        frameStart,
                        dropDuration,
                        VoiceKind.GRAVITY_DROP,
                        Math.max(36, padRoot - 12),
                        FAILURE_RAW_AMP * 1.12 * boost,
                        0.94,
                        pan));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 24),
                        bloomDuration * 2 / 3,
                        VoiceKind.CELESTA_BLOOM,
                        melodyRoot + 6, // Tritone ghost against root
                        FAILURE_RAW_AMP * 0.56 * boost,
                        0.90,
                        pan));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 110),
                        bloomDuration * 2 / 3,
                        VoiceKind.CELESTA_BLOOM,
                        melodyRoot + 13, // High minor-9th opposite-pan echo
                        FAILURE_RAW_AMP * 0.50 * boost,
                        0.92,
                        -pan));
            } else if (!newlyRawFailNodes.isEmpty()) {
                final NodeState failed = newlyRawFailNodes.getFirst();
                final double pan = panForColumn(failed.col(), maxCol);
                final double boost = Math.min(1.25, Math.sqrt(newlyRawFailNodes.size()));
                frameEvents.add(new SynthEvent(
                        frameStart,
                        dropDuration,
                        VoiceKind.GRAVITY_DROP,
                        Math.max(36, padRoot - 12),
                        FAILURE_RAW_AMP * boost,
                        0.88,
                        pan));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 26),
                        bloomDuration * 2 / 3,
                        VoiceKind.CELESTA_BLOOM,
                        melodyRoot + 1, // Minor-9th tension against root
                        FAILURE_RAW_AMP * 0.48 * boost,
                        0.82,
                        pan));
            } else if (!newlyKnownFailNodes.isEmpty() && frameIndex - lastKnownFailFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                final NodeState knownFail = newlyKnownFailNodes.getFirst();
                final double pan = panForColumn(knownFail.col(), maxCol);
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 14),
                        pluckDuration * 3 / 2,
                        VoiceKind.MARIMBA_PLUCK,
                        melodyRoot,
                        FAILURE_KNOWN_AMP,
                        0.32,
                        pan));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 34),
                        pluckDuration * 3 / 2,
                        VoiceKind.MARIMBA_PLUCK,
                        melodyRoot + 3,
                        FAILURE_KNOWN_AMP * 0.85,
                        0.28,
                        pan));
                lastKnownFailFrame = frameIndex;
            }

            if (!newlyRegressionWarnNodes.isEmpty()) {
                final NodeState regWarn = newlyRegressionWarnNodes.getFirst();
                final double pan = panForColumn(regWarn.col(), maxCol);
                final int baseMidi = pitchForNode(melodyRoot, scale, regWarn.col(), maxCol, regWarn.rowTenths(), maxRowTenths);
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 16),
                        pluckDuration * 4 / 3,
                        VoiceKind.MARIMBA_PLUCK,
                        baseMidi,
                        WARNING_RAW_AMP * 0.96,
                        0.62,
                        pan));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 36),
                        pluckDuration * 4 / 3,
                        VoiceKind.MARIMBA_PLUCK,
                        baseMidi + 6, // Tritone ghost interval
                        WARNING_RAW_AMP * 0.88,
                        0.60,
                        pan));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 110),
                        bloomDuration / 2,
                        VoiceKind.CELESTA_BLOOM,
                        Math.min(92, baseMidi + 18),
                        WARNING_RAW_AMP * 0.72,
                        0.76,
                        -pan));
            } else if (!newlyRawWarnNodes.isEmpty()) {
                final NodeState warned = newlyRawWarnNodes.getFirst();
                final double pan = panForColumn(warned.col(), maxCol);
                final int baseMidi = pitchForNode(melodyRoot, scale, warned.col(), maxCol, warned.rowTenths(), maxRowTenths);
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 16),
                        pluckDuration * 4 / 3,
                        VoiceKind.MARIMBA_PLUCK,
                        baseMidi,
                        WARNING_RAW_AMP,
                        0.58,
                        pan));
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 36),
                        pluckDuration * 4 / 3,
                        VoiceKind.MARIMBA_PLUCK,
                        baseMidi + 5, // Suspended 4th
                        WARNING_RAW_AMP * 0.88,
                        0.52,
                        pan));
            } else if (!newlyKnownWarnNodes.isEmpty() && frameIndex - lastKnownWarnFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                final NodeState knownWarn = newlyKnownWarnNodes.getFirst();
                final double pan = panForColumn(knownWarn.col(), maxCol);
                final int baseMidi = pitchForNode(melodyRoot, scale, knownWarn.col(), maxCol, knownWarn.rowTenths(), maxRowTenths);
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 20),
                        pluckDuration,
                        VoiceKind.MARIMBA_PLUCK,
                        baseMidi + 5,
                        WARNING_KNOWN_AMP,
                        0.34,
                        pan));
                lastKnownWarnFrame = frameIndex;
            } else if (!newlyNoticeNodes.isEmpty()) {
                final NodeState noticed = newlyNoticeNodes.getFirst();
                final double pan = panForColumn(noticed.col(), maxCol);
                final int midi = pitchForNode(melodyRoot, scale, noticed.col(), maxCol, noticed.rowTenths(), maxRowTenths) + 7;
                frameEvents.add(new SynthEvent(
                        frameStart + subFrameOffset(samplesPerFrame, 18),
                        bloomDuration / 2,
                        VoiceKind.CELESTA_BLOOM,
                        Math.min(90, midi),
                        NOTICE_AMP,
                        0.38,
                        pan));
            }

            // 8. Limbo Test-Cycle Heartbeat & Camera Cooldown Glide when no execution deltas occurred
            final boolean advancedTestCycle = current.testCycle() > 0 && current.testCycle() != previousTestCycle;
            if (current.testCycle() > 0) {
                previousTestCycle = current.testCycle();
            }

            if (0 == totalDeltaExecs && frameIndex > 0) {
                if (advancedTestCycle) {
                    frameEvents.add(new SynthEvent(
                            frameStart,
                            heartbeatDuration,
                            VoiceKind.SUB_HEARTBEAT,
                            Math.max(38, padRoot - 12),
                            HEARTBEAT_AMP,
                            0.25,
                            0.0));
                } else if (!Double.isNaN(previousZoom)) {
                    final double dz = Math.abs(current.zoom() - previousZoom);
                    final double dPan = Math.hypot(current.translateX() - previousTx, current.translateY() - previousTy);
                    if ((dz > 0.008 || dPan > 5.0) && frameIndex % 2 == 0) {
                        final double motion = clamp01((dz * 9.0) + (dPan / 220.0));
                        final int degreeIdx = (frameIndex / 2) % scale.length;
                        final int lift = (current.zoom() >= previousZoom) ? 12 : 0;
                        final double shimmerPan = ((frameIndex / 2) % 2 == 0) ? -0.42 : 0.42;
                        frameEvents.add(new SynthEvent(
                                frameStart,
                                bloomDuration / 2,
                                VoiceKind.CELESTA_BLOOM,
                                melodyRoot + lift + scale[degreeIdx],
                                SHIMMER_AMP * (0.45 + 0.55 * motion),
                                0.30,
                                shimmerPan));
                    }
                }
            }

            previousZoom = current.zoom();
            previousTx = current.translateX();
            previousTy = current.translateY();

            previousNodes.clear();
            previousNodes.putAll(current.nodes());

            // 9. Final Frame Weightless Resolution Chord fanning across the stereo field
            if (frameIndex == states.size() - 1 && states.size() > 1 && !current.nodes().isEmpty()) {
                final int[] chord = hadUnexpectedFailure ? REGRESSION_FINALE_OFFSETS : CLEAN_FINALE_OFFSETS;
                final int finaleRoot = hadUnexpectedFailure ? 50 : 62;
                frameEvents.add(new SynthEvent(
                        frameStart,
                        CarbonSynth.millisToSamples(1_250),
                        VoiceKind.HORIZON_PAD,
                        finaleRoot - 12,
                        PAD_ROOT_AMP * 1.15,
                        hadUnexpectedFailure ? 0.25 : 0.55,
                        0.0));
                for (int i = 0; i < chord.length; i++) {
                    final double fanPan = -0.52 + (1.04 * i / Math.max(1, chord.length - 1));
                    frameEvents.add(new SynthEvent(
                            frameStart + subFrameOffset(samplesPerFrame, i * 18),
                            finaleBloomDuration,
                            VoiceKind.CELESTA_BLOOM,
                            finaleRoot + chord[i],
                            FINALE_AMP * (0.92 - 0.04 * i),
                            hadUnexpectedFailure ? 0.42 : 0.82,
                            fanPan));
                }
            }

            if (!frameEvents.isEmpty()) {
                eventsByFrame.put(frameIndex, frameEvents);
            }
        }

        return eventsByFrame;
    }

    private static List<NodeState> selectWaypoints(final List<NodeState> sorted, final int maxCount) {
        if (sorted.size() <= maxCount) {
            return sorted;
        }
        final List<NodeState> picked = new ArrayList<>(maxCount);
        for (int i = 0; i < maxCount; i++) {
            final int idx = (i * (sorted.size() - 1)) / (maxCount - 1);
            picked.add(sorted.get(idx));
        }
        return picked;
    }

    private static int computeBuoyancyField(final double buoyancy) {
        if (buoyancy >= 0.88) {
            return 4;
        }
        if (buoyancy >= 0.65) {
            return 3;
        }
        if (buoyancy >= 0.42) {
            return 2;
        }
        if (buoyancy >= 0.20) {
            return 1;
        }
        return 0;
    }

    private static int pitchForNode(
            final int baseMidi,
            final int[] scale,
            final int col,
            final int maxCol,
            final int rowTenths,
            final int maxRowTenths) {
        final double normCol = (maxCol > 0) ? clamp01((double) col / maxCol) : 0.0;
        final double normHeight = 1.0 - clamp01((double) rowTenths / Math.max(10.0, maxRowTenths));
        final int degree = (int) Math.round(normCol * 9.0) + (int) Math.round(normHeight * 2.0);
        final int octave = degree / scale.length;
        final int step = scale[degree % scale.length];
        return Math.clamp(baseMidi + octave * 12 + step, 36, 88);
    }

    private static double panForColumn(final int col, final int maxCol) {
        if (maxCol <= 0) {
            return 0.0;
        }
        return clampPan(-0.72 + 1.44 * clamp01((double) col / maxCol));
    }

    private static double clampPan(final double pan) {
        return Math.clamp(pan, -0.90, 0.90);
    }

    private static double clamp01(final double value) {
        return Math.clamp(value, 0.0, 1.0);
    }

    private static long subFrameOffset(final int samplesPerFrame, final int desiredMillis) {
        if (desiredMillis <= 0 || samplesPerFrame <= 4) {
            return 0L;
        }
        final long desired = (long) CarbonSynth.SAMPLE_RATE * desiredMillis / 1000L;
        return Math.min(desired, samplesPerFrame - 2L);
    }

    private static char resolveTypeCode(final NodeData node, final ScenarioType type, final boolean executable) {
        if (!executable) {
            return 'X';
        }
        if (node.children().isEmpty() && ScenarioType.MONO == type) {
            return 'M';
        }
        if (node.children().isEmpty() && ScenarioType.LEAF == type) {
            return 'L';
        }
        return 'N';
    }

    private static char resolveStatusCode(
            final NodeData node,
            final int executions,
            final Map<NodeData, Boolean> childFailureCache) {
        if (TreeHtml.hasRegression(node)) {
            return 'R';
        }
        if (TreeHtml.hasFailure(node)) {
            return 'F';
        }
        if (TreeHtml.hasChildFailure(node, childFailureCache)) {
            return 'U';
        }
        if (TreeHtml.hasRegressionWarning(node)) {
            return 'M';
        }
        if (TreeHtml.hasWarning(node)) {
            return 'W';
        }
        if (TreeHtml.hasNotice(node)) {
            return 'I';
        }
        return executions > 0 ? 'P' : 'N';
    }

    private static String lineagePath(final NodeData node) {
        final List<String> chain = new ArrayList<>();
        NodeData cursor = node;
        while (null != cursor) {
            chain.add(cursor.label());
            cursor = cursor.parent();
        }
        java.util.Collections.reverse(chain);
        final List<String> full = new ArrayList<>(chain.size() + 1);
        full.add(node.group());
        full.addAll(chain);
        return String.join("/", full);
    }

    private static FrameState readFrameState(final File frame) throws IOException {
        final File snapshot = snapshotFile(frame);
        if (snapshot.isFile()) {
            return readSnapshotFile(snapshot);
        }
        if (frame.isFile() && frame.getName().toLowerCase(Locale.ROOT).endsWith(".svg")) {
            return parseSvgFrameFast(frame);
        }
        throw new IOException(s("Missing carbon snapshot for frame: ", frame));
    }

    private static FrameState readSnapshotFile(final File file) throws IOException {
        final Map<String, NodeState> nodes = new LinkedHashMap<>();
        double zoom = 1.0;
        double tx = 0.0;
        double ty = 0.0;
        int headerExecLeaves = -1;
        int headerCovLeaves = -1;

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while (null != (line = reader.readLine())) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("#meta\t")) {
                    final String[] meta = line.split("\t", -1);
                    if (meta.length >= 6) {
                        try {
                            zoom = Double.parseDouble(meta[1]);
                            tx = Double.parseDouble(meta[2]);
                            ty = Double.parseDouble(meta[3]);
                            headerExecLeaves = Integer.parseInt(meta[4]);
                            headerCovLeaves = Integer.parseInt(meta[5]);
                        } catch (final NumberFormatException e) {
                            throw new IOException(s("Invalid metadata header in carbon snapshot: ", file), e);
                        }
                    }
                    continue;
                }
                final String[] fields = line.split("\t", -1);
                if (8 != fields.length) {
                    throw new IOException(s("Invalid carbon snapshot line in: ", file));
                }
                try {
                    final String identity = new String(Base64.getUrlDecoder().decode(fields[0]), StandardCharsets.UTF_8);
                    final int col = Math.max(0, Integer.parseInt(fields[1]));
                    final int rowTenths = Math.max(0, Integer.parseInt(fields[2]));
                    final char typeCode = fields[3].isEmpty() ? 'N' : fields[3].charAt(0);
                    final int execs = Math.max(0, Integer.parseInt(fields[4]));
                    final int target = Math.max(1, Integer.parseInt(fields[5]));
                    final char statusCode = fields[6].isEmpty() ? 'N' : fields[6].charAt(0);
                    final boolean knownIssue = "1".equals(fields[7]);
                    nodes.put(identity, new NodeState(
                            identity, col, rowTenths, typeCode, execs, target, statusCode, knownIssue, false));
                } catch (final IllegalArgumentException e) {
                    throw new IOException(s("Invalid carbon snapshot format in: ", file), e);
                }
            }
        }
        return new FrameState(nodes, zoom, tx, ty, headerExecLeaves, headerCovLeaves, -1);
    }

    /**
     * Linear-time, zero-backtracking SVG progression frame parser that reconstructs node lineage
     * from rendered cubic Bezier edges, preserving distinct identities for identically named sibling leaves.
     */
    private static FrameState parseSvgFrameFast(final File svgFile) throws IOException {
        final String svg = Files.readString(svgFile.toPath(), StandardCharsets.UTF_8);

        double zoom = 1.0;
        double tx = 0.0;
        double ty = 0.0;
        final int camIdx = svg.indexOf("<g transform=\"translate(");
        if (camIdx >= 0) {
            final int closeParen = svg.indexOf(')', camIdx + 24);
            final int scaleIdx = (closeParen > 0) ? svg.indexOf("scale(", closeParen) : -1;
            final int scaleEnd = (scaleIdx > 0) ? svg.indexOf(')', scaleIdx + 6) : -1;
            if (closeParen > 0 && scaleEnd > 0) {
                final String transPart = svg.substring(camIdx + 24, closeParen);
                final int comma = transPart.indexOf(',');
                if (comma > 0) {
                    tx = parseDoubleSafe(transPart.substring(0, comma), 0.0);
                    ty = parseDoubleSafe(transPart.substring(comma + 1), 0.0);
                }
                zoom = parseDoubleSafe(svg.substring(scaleIdx + 6, scaleEnd), 1.0);
            }
        }

        // 1. Collect directed edge endpoints (startX, startY) -> (endX, endY)
        final List<int[]> edges = new ArrayList<>();
        int edgePos = 0;
        while ((edgePos = svg.indexOf("<path class=\"edge\"", edgePos)) >= 0) {
            final int tagEnd = svg.indexOf("/>", edgePos);
            if (tagEnd < 0) {
                break;
            }
            final int dStart = svg.indexOf(" d=\"M ", edgePos);
            if (dStart > 0 && dStart < tagEnd) {
                final int dEnd = svg.indexOf('"', dStart + 6);
                if (dEnd > dStart && dEnd < tagEnd) {
                    final int[] coords = parseEdgeEndpoints(svg.substring(dStart + 6, dEnd));
                    if (null != coords) {
                        edges.add(coords);
                    }
                }
            }
            edgePos = tagEnd + 2;
        }

        // 2. Scan all <g id="..." class="node-card ..."> blocks linearly
        final List<ParsedSvgCard> cards = new ArrayList<>();
        int cardPos = 0;
        while ((cardPos = svg.indexOf("<g id=\"", cardPos)) >= 0) {
            final int openEnd = svg.indexOf('>', cardPos);
            if (openEnd < 0) {
                break;
            }
            final String openTag = svg.substring(cardPos, openEnd);
            if (!openTag.contains("class=\"node-card ")) {
                cardPos = openEnd + 1;
                continue;
            }
            final int groupEnd = svg.indexOf("</g>", openEnd);
            if (groupEnd < 0) {
                break;
            }
            final String body = svg.substring(openEnd + 1, groupEnd);
            final ParsedSvgCard parsed = parseCard(openTag, body);
            if (null != parsed) {
                cards.add(parsed);
            }
            cardPos = groupEnd + 4;
        }

        // 3. Reconstruct parent-child topology via socket coordinates
        final int cardCount = cards.size();
        final int[] parentIdx = new int[cardCount];
        final boolean[] hasOutgoing = new boolean[cardCount];
        java.util.Arrays.fill(parentIdx, -1);

        if (!edges.isEmpty() && cardCount > 0) {
            final Map<Long, Integer> rightSocketMap = new HashMap<>(cardCount * 2);
            final Map<Long, Integer> leftSocketMap = new HashMap<>(cardCount * 2);
            for (int i = 0; i < cardCount; i++) {
                final ParsedSvgCard c = cards.get(i);
                final int centerY = c.y + c.cardH / 2;
                for (int dy = -3; dy <= 3; dy++) {
                    rightSocketMap.put(packCoord(c.x + c.cardW, centerY + dy), i);
                    leftSocketMap.put(packCoord(c.x, centerY + dy), i);
                }
            }
            for (final int[] e : edges) {
                final Integer from = rightSocketMap.get(packCoord(e[0], e[1]));
                final Integer to = leftSocketMap.get(packCoord(e[2], e[3]));
                if (null != from) {
                    hasOutgoing[from] = true;
                    if (null != to && -1 == parentIdx[to]) {
                        parentIdx[to] = from;
                    }
                }
            }
        }

        // 4. Parse header badges (Leaves: C/T or legacy Leafs: C/T, and Test #N)
        int headerCovLeaves = -1;
        int headerExecLeaves = -1;
        int testCycle = -1;
        int hdrPos = Math.max(0, cardPos);
        while ((hdrPos = svg.indexOf("<text class=\"tx3\"", hdrPos)) >= 0) {
            final int gt = svg.indexOf('>', hdrPos);
            final int lt = (gt > 0) ? svg.indexOf("</text>", gt) : -1;
            if (gt < 0 || lt < 0) {
                break;
            }
            final String badge = svg.substring(gt + 1, lt).trim();
            if (badge.startsWith("Leaves:") || badge.startsWith("Leafs:")) {
                final String ratioStr = badge.substring(badge.indexOf(':') + 1).trim();
                final int slash = ratioStr.indexOf('/');
                if (slash > 0) {
                    headerCovLeaves = parseIntSafe(ratioStr.substring(0, slash), -1);
                    headerExecLeaves = parseIntSafe(ratioStr.substring(slash + 1), -1);
                }
            } else if (badge.startsWith("Test #")) {
                testCycle = parseIntSafe(badge.substring(6).trim(), -1);
            }
            hdrPos = lt + 7;
        }

        // 5. Materialize NodeStates with unique lineage paths
        final Map<String, NodeState> nodes = new LinkedHashMap<>(cardCount * 2);
        final Map<String, Integer> duplicateCounter = new HashMap<>();
        final boolean hasEdges = !edges.isEmpty();

        for (int i = 0; i < cardCount; i++) {
            final ParsedSvgCard c = cards.get(i);
            final String basePath = buildCardLineage(i, cards, parentIdx);
            final int seen = duplicateCounter.getOrDefault(basePath, 0);
            duplicateCounter.put(basePath, seen + 1);
            final String uniqueIdentity = (0 == seen) ? basePath : s(basePath, "#", seen);

            final int col = Math.max(0, (int) Math.round((c.x - 40.0) / 270.0));
            final int rowTenths = Math.max(0, (int) Math.round(((c.y - 92.0) / 60.0) * 10.0));
            final char typeCode = inferSvgTypeCode(c, hasEdges, hasOutgoing[i]);
            final char statusCode = inferSvgStatusCode(c.cssClasses, c.executions);
            final boolean knownIssue = c.cssClasses.contains("known-issue");

            nodes.put(uniqueIdentity, new NodeState(
                    uniqueIdentity,
                    col,
                    rowTenths,
                    typeCode,
                    c.executions,
                    c.target,
                    statusCode,
                    knownIssue,
                    c.recentHighlight));
        }

        return new FrameState(nodes, zoom, tx, ty, headerExecLeaves, headerCovLeaves, testCycle);
    }

    private static String buildCardLineage(
            final int index,
            final List<ParsedSvgCard> cards,
            final int[] parentIdx) {
        final List<String> segments = new ArrayList<>(8);
        int cursor = index;
        int guard = 0;
        while (cursor >= 0 && guard++ < 64) {
            segments.add(cards.get(cursor).cleanName);
            cursor = parentIdx[cursor];
        }
        if (segments.size() == 1) {
            final int col = Math.max(0, (int) Math.round((cards.get(index).x - 40.0) / 270.0));
            return s(col, ":", segments.getFirst());
        }
        java.util.Collections.reverse(segments);
        return String.join("/", segments);
    }

    private static ParsedSvgCard parseCard(final String openTag, final String body) {
        final String id = extractAttr(openTag, "id=\"");
        final String css = extractAttr(openTag, "class=\"node-card ");
        final String trans = extractAttr(openTag, "transform=\"translate(");
        if (null == id || null == css || null == trans) {
            return null;
        }
        int x = 40;
        int y = 92;
        final int comma = trans.indexOf(',');
        final int endParen = trans.indexOf(')');
        if (comma > 0 && endParen > comma) {
            x = (int) Math.round(parseDoubleSafe(trans.substring(0, comma), 40.0));
            y = (int) Math.round(parseDoubleSafe(trans.substring(comma + 1, endParen), 92.0));
        }

        int cardW = 220;
        int cardH = 44;
        boolean recent = false;
        final int bgIdx = body.indexOf("<rect class=\"card-bg\"");
        if (bgIdx >= 0) {
            final int bgEnd = body.indexOf("/>", bgIdx);
            if (bgEnd > bgIdx) {
                final String bgTag = body.substring(bgIdx, bgEnd);
                final String wStr = extractAttr(bgTag, "width=\"");
                final String hStr = extractAttr(bgTag, "height=\"");
                final String stroke = extractAttr(bgTag, "stroke=\"");
                if (null != wStr) cardW = parseIntSafe(wStr, 220);
                if (null != hStr) cardH = parseIntSafe(hStr, 44);
                if (null != stroke) recent = "#38bdf8".equalsIgnoreCase(stroke.trim());
            }
        }

        final String title = extractElementText(body, "<title>", "</title>");
        final String tx1 = extractClassText(body, "<text class=\"tx1\"");
        final int[] execAndTarget = parseBadgeExecutionsAndTarget(tx1);
        final String cleanName = stripScenarioPrefix(id);

        return new ParsedSvgCard(
                cleanName, css, x, y, cardW, cardH,
                null != title ? title : "", null != tx1 ? tx1 : "",
                execAndTarget[0], execAndTarget[1], recent);
    }

    private static String stripScenarioPrefix(final String nodeId) {
        if (nodeId.startsWith("scenario_")) {
            final int secondUnderscore = nodeId.indexOf('_', 9);
            if (secondUnderscore > 9 && secondUnderscore + 1 < nodeId.length()) {
                return nodeId.substring(secondUnderscore + 1);
            }
        }
        return nodeId;
    }

    private static char inferSvgTypeCode(
            final ParsedSvgCard card,
            final boolean hasEdges,
            final boolean hasOutgoing) {
        if (card.cssClasses.contains("non_executable") || "OFF".equalsIgnoreCase(card.badgeText)) {
            return 'X';
        }
        if (card.titleText.indexOf('◆') >= 0) {
            return 'M';
        }
        if (card.titleText.indexOf('◼') >= 0 || card.badgeText.indexOf('/') >= 0) {
            return 'L';
        }
        if (card.titleText.indexOf('▷') >= 0) {
            return 'N';
        }
        if (hasEdges) {
            return hasOutgoing ? 'N' : 'L';
        }
        return 'N';
    }

    private static char inferSvgStatusCode(final String cssClasses, final int executions) {
        if (cssClasses.contains("regression") && !cssClasses.contains("regression_warning")) {
            return 'R';
        }
        if (cssClasses.contains("failed") && !cssClasses.contains("child_failed")) {
            return 'F';
        }
        if (cssClasses.contains("child_failed")) {
            return 'U';
        }
        if (cssClasses.contains("regression_warning")) {
            return 'M';
        }
        if (cssClasses.contains("warning")) {
            return 'W';
        }
        if (cssClasses.contains("notice")) {
            return 'I';
        }
        return executions > 0 ? 'P' : 'N';
    }

    private static int[] parseBadgeExecutionsAndTarget(final String badge) {
        if (null == badge || badge.isBlank() || "OFF".equalsIgnoreCase(badge)) {
            return new int[]{0, 1};
        }
        final String cleaned = badge.replace("✓", "").trim();
        final int slash = cleaned.indexOf('/');
        if (slash > 0) {
            final int execs = Math.max(0, parseIntSafe(cleaned.substring(0, slash).trim(), 0));
            final String right = cleaned.substring(slash + 1).trim();
            final int sp = right.indexOf(' ');
            final int target = Math.max(1, parseIntSafe(sp > 0 ? right.substring(0, sp) : right, 1));
            return new int[]{execs, target};
        }
        final int sp = cleaned.indexOf(' ');
        if (sp > 0) {
            return new int[]{Math.max(0, parseIntSafe(cleaned.substring(0, sp).trim(), 0)), 1};
        }
        return new int[]{0, 1};
    }

    private static int[] parseEdgeEndpoints(final String dPath) {
        final int cIdx = dPath.indexOf(" C ");
        final int lastComma = dPath.lastIndexOf(',');
        if (cIdx <= 0 || lastComma <= cIdx) {
            return null;
        }
        final String startPart = dPath.substring(0, cIdx).trim();
        final String endPart = dPath.substring(lastComma + 1).trim();
        final int sp1 = startPart.indexOf(' ');
        final int sp2 = endPart.indexOf(' ');
        if (sp1 <= 0 || sp2 <= 0) {
            return null;
        }
        final int sx = parseIntSafe(startPart.substring(0, sp1), Integer.MIN_VALUE);
        final int sy = parseIntSafe(startPart.substring(sp1 + 1), Integer.MIN_VALUE);
        final int ex = parseIntSafe(endPart.substring(0, sp2), Integer.MIN_VALUE);
        final int ey = parseIntSafe(endPart.substring(sp2 + 1), Integer.MIN_VALUE);
        if (Integer.MIN_VALUE == sx || Integer.MIN_VALUE == sy
                || Integer.MIN_VALUE == ex || Integer.MIN_VALUE == ey) {
            return null;
        }
        return new int[]{sx, sy, ex, ey};
    }

    private static long packCoord(final int x, final int y) {
        return (((long) x) << 32) ^ (y & 0xFFFFFFFFL);
    }

    private static String extractAttr(final String tag, final String prefix) {
        final int start = tag.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        final int valStart = start + prefix.length();
        final int valEnd = tag.indexOf('"', valStart);
        return (valEnd >= valStart) ? tag.substring(valStart, valEnd) : null;
    }

    private static String extractElementText(final String body, final String open, final String close) {
        final int start = body.indexOf(open);
        if (start < 0) {
            return null;
        }
        final int valStart = start + open.length();
        final int valEnd = body.indexOf(close, valStart);
        return (valEnd >= valStart) ? body.substring(valStart, valEnd).trim() : null;
    }

    private static String extractClassText(final String body, final String prefix) {
        final int start = body.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        final int gt = body.indexOf('>', start + prefix.length());
        if (gt < 0) {
            return null;
        }
        final int lt = body.indexOf("</text>", gt + 1);
        return (lt >= gt + 1) ? body.substring(gt + 1, lt).trim() : null;
    }

    private static int parseIntSafe(final String text, final int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (final NumberFormatException _) {
            return fallback;
        }
    }

    private static double parseDoubleSafe(final String text, final double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (final NumberFormatException _) {
            return fallback;
        }
    }

    private record ParsedSvgCard(
            String cleanName,
            String cssClasses,
            int x,
            int y,
            int cardW,
            int cardH,
            String titleText,
            String badgeText,
            int executions,
            int target,
            boolean recentHighlight) {
    }

    private record NodeState(
            String identity,
            int col,
            int rowTenths,
            char typeCode,
            int executions,
            int target,
            char statusCode,
            boolean knownIssue,
            boolean recentHighlight) {

        boolean isTerminal() {
            return 'L' == typeCode || 'M' == typeCode;
        }
    }

    private record FrameState(
            Map<String, NodeState> nodes,
            double zoom,
            double translateX,
            double translateY,
            int headerExecutableLeaves,
            int headerCoveredLeaves,
            int testCycle) {

        double coverageRatio() {
            if (headerExecutableLeaves > 0 && headerCoveredLeaves >= 0) {
                return Math.clamp((double) headerCoveredLeaves / headerExecutableLeaves, 0.0, 1.0);
            }
            int total = 0;
            int covered = 0;
            for (final NodeState node : nodes.values()) {
                if (node.isTerminal()) {
                    total++;
                    if (node.executions() >= node.target()) {
                        covered++;
                    }
                }
            }
            return total > 0 ? (double) covered / total : 0.0;
        }
    }
}
