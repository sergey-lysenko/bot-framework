package works.lysenko.base.output.sonifier;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.sonifier.DnbSynth.DnbEvent;
import works.lysenko.base.output.sonifier.DnbSynth.VoiceKind;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Liquid Drum &amp; Bass telemetry sonifier ({@code dnb}) engineered to be genuinely pleasant
 * across multi-minute test runs while conveying every scenario tree event and the final run
 * outcome purely by ear.
 * <p>
 * Runs on an exact sample-accurate <b>175 BPM</b> two-step breakbeat clock ({@code 3,780} samples
 * per 16th note at 44.1 kHz) rooted in warm <b>F minor</b> ({@code Fm9 → Dbmaj9 → Abmaj9 → Eb6/9},
 * tinting into suspended {@code Fm9 → Bbm9 → Dbmaj7 → C7sus4} under raw warnings, and darkening
 * into Phrygian Techstep {@code Fm9 → Gbmaj7#11 → Db(add9) → C7(b9)} on raw regressions):
 * <ul>
 *     <li><b>Breathing Liquid Breakbeat &amp; Sub-Bass</b>: Smooth kick-ducked Fender Rhodes
 *     chords and deep mono-centered F-minor sub-bass roll continuously; the two-step breakbeat
 *     (round kick, crisp woody snare, feather-light ghost snares, silky ride hats) dynamically
 *     intensifies with execution velocity, adds subtle 4-bar fills, and drops into an atmospheric
 *     half-time breakdown during camera cooldown/idle frames to prevent listening fatigue.</li>
 *     <li><b>Stereo Topology &amp; Execution Lifecycle</b>: Column depth pans left-to-right across
 *     the stereo stage while row position shapes ergonomic mid-register pitch. Branches play warm
 *     Rhodes plucks (softening into brushed ticks on hot revisited parents); partial leaves play
 *     ascending double-taps; newly covered leaves bloom into 3-note dub-delayed liquid cadences;
 *     {@code MONO} completions ring with a crystalline octave+9th chime; over-executions register
 *     as delicate ghost-hat ticks.</li>
 *     <li><b>Complete Acoustic Event Semantics</b>:
 *     <ul>
 *         <li>{@code NOTICE}: Airy aqua dub-delayed bell ping.</li>
 *         <li>{@code WARNING} (raw vs. known issue): Warm muted amber horn stab + rim double-tap
 *         for raw warnings; soft, low-passed, cooldown-gated muted pulse for known issues.</li>
 *         <li>{@code UPSET} ({@code child_failed}): Hollow downward dub-chord fifth (cooldown-gated
 *         across cascading ancestors).</li>
 *         <li>{@code FAILED} (raw vs. known issue): Visceral Reese bass pitch-dive + sub impact +
 *         harmonic shift into dark Phrygian Techstep for raw regressions; contained dark low-passed
 *         thump (cooldown-gated) for known issues.</li>
 *     </ul>
 *     </li>
 *     <li><b>Structural Events &amp; 5-State Verdict Outro</b>: Hidden/pruned leaves gently exhale
 *     downward from their stereo column; post-test column-bank compaction triggers a classic DnB
 *     band-pass air riser into a lock-in rim click; and the final 1.85s outro resolves into one of
 *     five unmistakable cadences: <i>Spotless 100% Pass</i> (wide {@code Abmaj9/Fmaj9} sunrise +
 *     soaring bell crown), <i>Clean Pass with Known Issues</i> ({@code Fmaj9} + single muted amber
 *     echo), <i>Passed with Raw Warnings</i> (suspended {@code Fm11/Eb9sus4} + warm amber horn),
 *     <i>Incomplete Coverage</i> (open-ended {@code Fm9} half-cadence), or <i>Raw Failure
 *     Regression</i> (dark {@code Fm(b9)} + deep {@code F1} Reese power-down drop).</li>
 * </ul>
 */
public final class DnbSonifier implements TreeSonifier {

    /**
     * Configuration property value activating the Liquid Drum &amp; Bass sonifier.
     */
    public static final String MODE = "dnb";

    // Exact 175 BPM 16th-note groove timing (1 bar = 16 steps = 60,480 samples = 1.3714 s)
    private static final int STEPS_PER_BAR = 16;
    private static final int MICRO_SWING_SAMPLES = 180; // ~4.1 ms silky off-16th groove swing

    // 4-bar Liquid DnB chord cycles in F minor (sub-bass pocket F1=29 .. Eb2=39 -> 43.7..77.8 Hz)
    private static final int[] SUB_ROOTS_CLEAN = {29, 37, 32, 39};   // F1, Db2, Ab1, Eb2
    private static final int[] SUB_ROOTS_WARN = {29, 34, 37, 36};    // F1, Bb1, Db2, C2
    private static final int[] SUB_ROOTS_FAIL = {29, 30, 37, 36};    // F1, Gb1 (Phrygian b2!), Db2, C2

    private static final int[][] CHORD_INTERVALS_CLEAN = {
            {0, 3, 7, 10, 14}, // Fm9
            {0, 4, 7, 11, 14}, // Dbmaj9
            {0, 4, 7, 11, 14}, // Abmaj9
            {0, 4, 7, 9, 14}   // Eb6/9
    };
    private static final int[][] CHORD_INTERVALS_WARN = {
            {0, 3, 7, 10, 14}, // Fm9
            {0, 3, 7, 10, 14}, // Bbm9
            {0, 4, 7, 11, 14}, // Dbmaj9
            {0, 5, 7, 10, 14}  // C9sus4
    };
    private static final int[][] CHORD_INTERVALS_FAIL = {
            {0, 3, 7, 10, 14}, // Fm9
            {0, 4, 7, 11, 18}, // Gbmaj7(#11)
            {0, 4, 7, 14, 19}, // Db(add9)
            {0, 4, 7, 10, 13}  // C7(b9)
    };

    // F minor / Ab major liquid melody scale (C4=60 anchor, F4=65)
    private static final int[] LIQUID_MELODY_SCALE = {0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 19, 22};

    // Five unmistakable verdict outro chord voicings (rooted around F3=53 / Ab3=56)
    private static final int[] OUTRO_SPOTLESS_MIDI = {53, 60, 64, 67, 72};  // Fmaj9 radiant sunrise
    private static final int[] OUTRO_KNOWN_OK_MIDI = {53, 60, 64, 67, 69};  // Fmaj9(6) warm resolved
    private static final int[] OUTRO_WARNING_MIDI = {53, 58, 63, 67, 70};   // Fm11 / Eb9sus4 amber question
    private static final int[] OUTRO_PARTIAL_MIDI = {53, 60, 63, 67};       // Fm9 open half-cadence
    private static final int[] OUTRO_FAILURE_MIDI = {53, 54, 60, 63, 66};   // Fm(b9) dark brooding regression

    private static final int KNOWN_ISSUE_COOLDOWN_FRAMES = 4;
    private static final int UPSET_COOLDOWN_FRAMES = 3;

    // Carefully balanced non-fatiguing mix gains
    private static final double KICK_AMP = 0.255;
    private static final double SNARE_AMP = 0.205;
    private static final double GHOST_SNARE_AMP = 0.072;
    private static final double HAT_AMP = 0.068;
    private static final double RIM_AMP = 0.112;
    private static final double SUB_AMP = 0.235;
    private static final double REESE_BED_AMP = 0.088;
    private static final double RHODES_VOICE_AMP = 0.064;
    private static final double PLUCK_AMP = 0.112;
    private static final double LEAF_ARP_AMP = 0.132;
    private static final double MONO_CHIME_AMP = 0.128;
    private static final double OVER_TICK_AMP = 0.056;
    private static final double NOTICE_AMP = 0.105;
    private static final double WARNING_RAW_AMP = 0.148;
    private static final double WARNING_KNOWN_AMP = 0.062;
    private static final double UPSET_AMP = 0.102;
    private static final double FAILURE_RAW_AMP = 0.295;
    private static final double FAILURE_KNOWN_AMP = 0.105;
    private static final double SWEEP_AMP = 0.098;
    private static final double OUTRO_CHORD_AMP = 0.096;

    /**
     * Resolves the sidecar snapshot file corresponding to a rendered progression frame.
     *
     * @param frame rendered progression frame ({@code .svg} or {@code .png})
     * @return sidecar {@code .dnb} file inside {@code dnb_events/}
     */
    public static File snapshotFile(final File frame) {
        return new File(new File(frame.getParentFile(), "dnb_events"),
                frame.getName().replaceFirst("\\.(png|svg)$", ".dnb"));
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
        return !frames.isEmpty() && frames.stream().allMatch(DnbSonifier::hasReadableFrame);
    }

    @Override
    public void writeWav(final List<File> frames, final File output, final int frameRate) throws IOException {
        if (frames.isEmpty() || frameRate < 1) {
            throw new IllegalArgumentException("Audio requires frames and a positive frame rate");
        }
        if (DnbSynth.SAMPLE_RATE % frameRate != 0) {
            throw new IllegalArgumentException("Frame rate must divide the audio sample rate");
        }
        final int samplesPerFrame = DnbSynth.SAMPLE_RATE / frameRate;
        final long totalSamples = (long) frames.size() * samplesPerFrame + DnbSynth.TAIL_SAMPLES;
        final Map<Integer, List<DnbEvent>> events = composeDnbEvents(frames, samplesPerFrame);
        DnbSynth.writeStereoWav(output, totalSamples, samplesPerFrame, events);
    }

    private static boolean hasReadableFrame(final File frame) {
        if (snapshotFile(frame).isFile()) {
            return true;
        }
        return frame.isFile()
                && frame.getName().toLowerCase(Locale.ROOT).endsWith(".svg")
                && frame.length() > 0L;
    }

    private static Map<Integer, List<DnbEvent>> composeDnbEvents(
            final List<File> frames,
            final int samplesPerFrame) throws IOException {

        final List<FrameState> states = new ArrayList<>(frames.size());
        int globalMaxCol = 0;
        int globalMaxRowTenths = 10;

        for (final File frame : frames) {
            final FrameState state = readFrameState(frame);
            states.add(state);
            for (final NodeState node : state.nodes().values()) {
                if (node.col() > globalMaxCol) {
                    globalMaxCol = node.col();
                }
                if (node.rowTenths() > globalMaxRowTenths) {
                    globalMaxRowTenths = node.rowTenths();
                }
            }
        }

        final Map<Integer, List<DnbEvent>> eventsByFrame = new HashMap<>();
        final int frameCount = states.size();
        final long contentSamples = (long) frameCount * samplesPerFrame;

        // Per-frame telemetry summaries computed in Pass 1 so the 175 BPM groove clock in Pass 2
        // breathes smoothly with real execution energy, harmonic mood, and idle cooldowns.
        final boolean[] frameActive = new boolean[frameCount];
        final double[] frameVelocity = new double[frameCount];
        final double[] frameCoverage = new double[frameCount];
        final int[] frameMood = new int[frameCount]; // 0 = clean, 1 = raw warning, 2 = raw failure

        final Map<String, NodeState> previousNodes = new HashMap<>();
        int previousNodeCount = -1;
        int previousFrameMaxCol = -1;
        int previousTestCycle = -1;
        int lastKnownWarnFrame = -KNOWN_ISSUE_COOLDOWN_FRAMES;
        int lastKnownFailFrame = -KNOWN_ISSUE_COOLDOWN_FRAMES;
        int lastUpsetFrame = -UPSET_COOLDOWN_FRAMES;
        int lastMilestoneBucket = 0; // 0, 1 (50%), 2 (100%)
        double previousZoom = Double.NaN;
        double previousTx = Double.NaN;
        double previousTy = Double.NaN;
        double smoothedVelocity = 0.25;

        boolean hadRawFailure = false;
        boolean hadRawWarning = false;
        boolean hadReproducedKnownIssue = false;
        double maxCoverageSeen = 0.0;

        final int pluckDuration = DnbSynth.millisToSamples(195);
        final int arpDuration = DnbSynth.millisToSamples(420);
        final int monoDuration = DnbSynth.millisToSamples(390);
        final int overTickDuration = DnbSynth.millisToSamples(52);
        final int noticeDuration = DnbSynth.millisToSamples(360);
        final int warningDuration = DnbSynth.millisToSamples(390);
        final int upsetDuration = DnbSynth.millisToSamples(320);
        final int failureDuration = DnbSynth.millisToSamples(620);
        final int sweepDuration = DnbSynth.millisToSamples(460);
        final int riserDuration = DnbSynth.millisToSamples(560);
        final int rimDuration = DnbSynth.millisToSamples(65);
        final int kickDuration = DnbSynth.millisToSamples(175);

        // Pass 1: Frame-aligned semantic tree events & telemetry timeline
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            final FrameState current = states.get(frameIndex);
            final long frameStart = (long) frameIndex * samplesPerFrame;
            final int frameMaxCol = computeFrameMaxCol(current);
            final int panSpan = Math.max(1, Math.max(frameMaxCol, globalMaxCol));
            final double coverage = computeCoverage(current);

            final List<NodeDelta> deltas = new ArrayList<>();
            int totalExecDelta = 0;
            for (final NodeState node : current.nodes().values()) {
                final NodeState prev = previousNodes.get(node.path());
                final int prevExec = (null != prev) ? prev.executions() : 0;
                final char prevStatus = (null != prev) ? prev.status() : 'N';
                final int execDelta = Math.max(0, node.executions() - prevExec);
                final boolean statusWorsened = statusSeverity(node.status()) > statusSeverity(prevStatus);
                final boolean newNotice = ('I' == node.status() && 'I' != prevStatus);
                if (execDelta > 0 || statusWorsened || newNotice) {
                    deltas.add(new NodeDelta(node, prevExec, execDelta, statusWorsened, newNotice));
                    totalExecDelta += execDelta;
                }
                if (node.knownIssue() && ('W' == node.status() || 'F' == node.status())) {
                    hadReproducedKnownIssue = true;
                }
                if ('F' == node.status() && !node.knownIssue()) {
                    hadRawFailure = true;
                } else if ('W' == node.status() && !node.knownIssue()) {
                    hadRawWarning = true;
                }
            }
            if (coverage > maxCoverageSeen) {
                maxCoverageSeen = coverage;
            }

            final boolean hasActivity = !deltas.isEmpty();
            final double instantVelocity = Math.min(1.0, totalExecDelta / 6.0);
            smoothedVelocity = 0.65 * smoothedVelocity + 0.35 * instantVelocity;

            final int mood = hadRawFailure ? 2 : (hadRawWarning ? 1 : 0);
            frameActive[frameIndex] = hasActivity;
            frameVelocity[frameIndex] = smoothedVelocity;
            frameCoverage[frameIndex] = coverage;
            frameMood[frameIndex] = mood;

            final int barIndex = (int) ((frameStart / (long) (DnbSynth.STEP_SAMPLES * STEPS_PER_BAR)) % 4L);
            final int subRoot = resolveSubRoot(mood, barIndex);
            final int[] chordIntervals = resolveChordIntervals(mood, barIndex);

            // Cycle boundary subtle accent
            final boolean newCycle = (current.testCycle() > 0 && current.testCycle() != previousTestCycle);
            if (newCycle && frameIndex > 0) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        frameStart,
                        rimDuration,
                        VoiceKind.RIM_CLICK,
                        69.0,
                        RIM_AMP * 0.58,
                        0.45,
                        -0.18));
            }

            // Coverage milestones (50% and 100%) -> subtle liquid cymbal air lift
            final int milestoneBucket = (coverage >= 0.999) ? 2 : ((coverage >= 0.50) ? 1 : 0);
            if (milestoneBucket > lastMilestoneBucket && frameIndex > 0) {
                lastMilestoneBucket = milestoneBucket;
                final double liftAmp = (2 == milestoneBucket) ? (SWEEP_AMP * 1.05) : (SWEEP_AMP * 0.72);
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        frameStart,
                        sweepDuration,
                        VoiceKind.AIR_SWEEP,
                        56.0,
                        78.0,
                        liftAmp,
                        0.62,
                        0.0));
            }

            // Node execution & status events (sorted spatially left-to-right, top-to-bottom)
            deltas.sort(Comparator.comparingInt((NodeDelta d) -> d.node().col())
                    .thenComparingInt(d -> d.node().rowTenths())
                    .thenComparing(d -> d.node().path()));

            final int maxVoicesThisFrame = Math.min(deltas.size(), 6);
            final long staggerStep = (maxVoicesThisFrame > 1)
                    ? Math.max(1L, (samplesPerFrame * 2L) / (3L * maxVoicesThisFrame))
                    : 0L;

            for (int i = 0; i < maxVoicesThisFrame; i++) {
                final NodeDelta delta = deltas.get(i);
                final NodeState node = delta.node();
                final long onset = frameStart + i * staggerStep;
                final double pan = computeStereoPan(node.col(), panSpan);
                final double rowRatio = 1.0 - Math.min(1.0, (double) node.rowTenths() / Math.max(10, globalMaxRowTenths));
                final double melodyMidi = resolveLiquidPitch(subRoot, chordIntervals, node, rowRatio);

                final boolean isLeaf = ('L' == node.type() || 'M' == node.type());
                final boolean isMono = ('M' == node.type());
                if (delta.execDelta() > 0) {
                    if (isLeaf) {
                        final int target = Math.max(1, node.target());
                        if (delta.prevExec() < target && node.executions() >= target) {
                            if (isMono) {
                                // Newly completed MONO (◆): crisp crystalline octave + 9th chime
                                final long step = Math.max(DnbSynth.millisToSamples(32), samplesPerFrame / 5);
                                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                        onset,
                                        monoDuration,
                                        VoiceKind.LIQUID_ARP,
                                        melodyMidi,
                                        melodyMidi + 2.0,
                                        MONO_CHIME_AMP,
                                        0.72,
                                        pan));
                                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                        onset + step,
                                        monoDuration,
                                        VoiceKind.LIQUID_ARP,
                                        melodyMidi + 12.0,
                                        melodyMidi + 14.0,
                                        MONO_CHIME_AMP * 0.78,
                                        0.80,
                                        clampPan(pan + 0.14)));
                            } else {
                                // Newly completed LEAF (◼): lush 3-note liquid rising cadence into dub delay
                                final int[] cadence = {0, 7, 14}; // root -> 5th -> 9th
                                final long noteStep = Math.max(DnbSynth.millisToSamples(36), samplesPerFrame / 4);
                                for (int m = 0; m < cadence.length; m++) {
                                    final double startPitch = melodyMidi + cadence[m] - (0 == m ? 2.0 : 0.0);
                                    final double endPitch = melodyMidi + cadence[m];
                                    scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                            onset + m * noteStep,
                                            arpDuration,
                                            VoiceKind.LIQUID_ARP,
                                            startPitch,
                                            endPitch,
                                            LEAF_ARP_AMP * (1.0 - 0.12 * m),
                                            0.54 + 0.30 * rowRatio,
                                            clampPan(pan + (m - 1) * 0.14)));
                                }
                            }
                        } else if (node.executions() > target) {
                            // Over-execution beyond target: delicate ghost-hat tick
                            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                    onset,
                                    overTickDuration,
                                    VoiceKind.LIQUID_HAT,
                                    melodyMidi + 12.0,
                                    OVER_TICK_AMP,
                                    0.38 + 0.35 * rowRatio,
                                    pan));
                        } else {
                            // Partial leaf hit (e.g. 1/2): warm ascending electric-piano double-tap
                            final long tapGap = Math.max(DnbSynth.millisToSamples(34), samplesPerFrame / 4);
                            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                    onset,
                                    pluckDuration,
                                    VoiceKind.LIQUID_PLUCK,
                                    melodyMidi,
                                    PLUCK_AMP * 0.92,
                                    0.48 + 0.25 * rowRatio,
                                    pan));
                            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                    onset + tapGap,
                                    pluckDuration,
                                    VoiceKind.LIQUID_PLUCK,
                                    melodyMidi + 7.0,
                                    PLUCK_AMP * 0.78,
                                    0.60 + 0.22 * rowRatio,
                                    clampPan(pan + 0.10)));
                        }
                    } else {
                        // Branch node traversal: warm Rhodes pluck softening into brushed tick on hot parents
                        final double familiarity = 1.0 / (1.0 + 0.38 * Math.log1p(delta.prevExec()));
                        if (delta.prevExec() >= 6 && familiarity < 0.48) {
                            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                    onset,
                                    overTickDuration,
                                    VoiceKind.LIQUID_HAT,
                                    melodyMidi,
                                    OVER_TICK_AMP * 0.72,
                                    0.25 + 0.22 * rowRatio,
                                    pan));
                        } else {
                            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                    onset,
                                    pluckDuration,
                                    VoiceKind.LIQUID_PLUCK,
                                    melodyMidi - 12.0,
                                    PLUCK_AMP * Math.max(0.42, familiarity),
                                    0.35 + 0.38 * rowRatio,
                                    pan));
                        }
                    }
                }

                // Semantic status cues
                if (delta.newNotice()) {
                    scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                            onset,
                            noticeDuration,
                            VoiceKind.AQUA_NOTICE,
                            melodyMidi + 7.0,
                            NOTICE_AMP,
                            0.68,
                            pan));
                }

                if (delta.statusWorsened()) {
                    if ('W' == node.status()) {
                        if (node.knownIssue()) {
                            if (frameIndex - lastKnownWarnFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                                lastKnownWarnFrame = frameIndex;
                                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                        onset,
                                        warningDuration,
                                        VoiceKind.AMBER_HORN,
                                        subRoot + 24.0 + 7.0,
                                        WARNING_KNOWN_AMP,
                                        0.20,
                                        pan));
                            }
                        } else {
                            // Raw warning: warm amber horn stab + crisp rim double-tap
                            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                    onset,
                                    warningDuration,
                                    VoiceKind.AMBER_HORN,
                                    subRoot + 24.0 + 10.0,
                                    WARNING_RAW_AMP,
                                    0.72,
                                    pan));
                            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                    onset,
                                    rimDuration,
                                    VoiceKind.RIM_CLICK,
                                    69.0,
                                    RIM_AMP * 0.78,
                                    0.65,
                                    clampPan(pan * 0.7)));
                        }
                    } else if ('U' == node.status()) {
                        // UPSET (parent passed, child failed downstream): hollow downward dub fifth
                        if (frameIndex - lastUpsetFrame >= UPSET_COOLDOWN_FRAMES) {
                            lastUpsetFrame = frameIndex;
                            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                    onset,
                                    upsetDuration,
                                    VoiceKind.UPSET_DROP,
                                    melodyMidi,
                                    melodyMidi - 7.0,
                                    UPSET_AMP,
                                    0.45,
                                    pan));
                        }
                    } else if ('F' == node.status()) {
                        if (node.knownIssue()) {
                            if (frameIndex - lastKnownFailFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                                lastKnownFailFrame = frameIndex;
                                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                        onset,
                                        failureDuration,
                                        VoiceKind.FAULT_IMPACT,
                                        subRoot + 12.0,
                                        subRoot + 5.0,
                                        FAILURE_KNOWN_AMP,
                                        0.22,
                                        pan * 0.5));
                            }
                        } else {
                            // Raw regression: visceral DnB Reese pitch-dive + deep sub impact
                            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                    onset,
                                    failureDuration,
                                    VoiceKind.FAULT_IMPACT,
                                    subRoot + 18.0, // tritone above root diving down 11 semitones
                                    subRoot + 7.0,
                                    FAILURE_RAW_AMP,
                                    0.90,
                                    pan * 0.35));
                            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                                    onset,
                                    kickDuration,
                                    VoiceKind.DNB_KICK,
                                    48.0,
                                    KICK_AMP * 1.05,
                                    0.65,
                                    0.0));
                        }
                    }
                }
            }

            // Structural events: hidden/pruned leaves & final column-bank compaction
            final int currentNodeCount = current.nodes().size();
            double vanishedColSum = 0.0;
            int vanishedCount = 0;
            if (!previousNodes.isEmpty()) {
                for (final Map.Entry<String, NodeState> prevEntry : previousNodes.entrySet()) {
                    if (!current.nodes().containsKey(prevEntry.getKey())) {
                        vanishedColSum += prevEntry.getValue().col();
                        vanishedCount++;
                    }
                }
            }
            final boolean prunedMass = (vanishedCount > 0)
                    || (previousNodeCount > 0 && currentNodeCount < previousNodeCount);
            final boolean columnsCompacted = (previousFrameMaxCol > 0 && frameMaxCol < previousFrameMaxCol);

            if (prunedMass) {
                final int removed = Math.max(vanishedCount, Math.max(1, previousNodeCount - currentNodeCount));
                final double fadeIntensity = Math.min(1.0, removed / 8.0);
                final double vanishPan = (vanishedCount > 0)
                        ? clampPan((Math.max(0.0, Math.min(1.0, (vanishedColSum / vanishedCount) / panSpan))) * 1.50 - 0.75)
                        : 0.0;
                // Silky downward liquid low-pass exhale panned to vanished leaves
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        frameStart,
                        sweepDuration,
                        VoiceKind.AIR_SWEEP,
                        82.0,
                        56.0,
                        SWEEP_AMP * (0.62 + 0.32 * fadeIntensity),
                        0.38,
                        vanishPan));
            } else if (columnsCompacted) {
                // Signature DnB upward band-pass air riser into a crisp lock-in rim click
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        frameStart,
                        riserDuration,
                        VoiceKind.AIR_SWEEP,
                        56.0,
                        94.0,
                        SWEEP_AMP * 1.18,
                        0.76,
                        0.0));
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        frameStart + (riserDuration * 3L) / 4L,
                        rimDuration,
                        VoiceKind.RIM_CLICK,
                        72.0,
                        RIM_AMP,
                        0.75,
                        0.0));
            }

            // Cooldown camera glide shimmer: soft dub-delayed liquid chime
            if (!hasActivity && !Double.isNaN(previousZoom)) {
                final double cameraDelta = Math.abs(current.zoom() - previousZoom) * 18.0
                        + (Math.abs(current.tx() - previousTx) + Math.abs(current.ty() - previousTy)) / 180.0;
                if (cameraDelta > 0.08 && 0 == (frameIndex % 3)) {
                    final double shimmerPitch = 65.0 + chordIntervals[frameIndex % chordIntervals.length];
                    scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                            frameStart,
                            noticeDuration,
                            VoiceKind.AQUA_NOTICE,
                            shimmerPitch,
                            NOTICE_AMP * Math.min(0.42, 0.18 + 0.22 * cameraDelta),
                            0.42,
                            ((frameIndex % 6) < 3) ? -0.45 : 0.45));
                }
            }

            previousNodes.clear();
            previousNodes.putAll(current.nodes());
            previousNodeCount = currentNodeCount;
            previousFrameMaxCol = frameMaxCol;
            if (current.testCycle() > 0) {
                previousTestCycle = current.testCycle();
            }
            previousZoom = current.zoom();
            previousTx = current.tx();
            previousTy = current.ty();
        }

        // Pass 2: Sample-accurate 175 BPM Liquid Drum & Bass groove engine (kick, snare, ghosts,
        // ride hats, deep sub-bass, warm Reese bed, and lush Rhodes chord progression)
        composeLiquidGroove(
                eventsByFrame,
                samplesPerFrame,
                contentSamples,
                frameActive,
                frameVelocity,
                frameCoverage,
                frameMood);

        // Pass 3: Unmistakable 5-state Final Verdict Outro
        final FrameState finalState = states.get(frameCount - 1);
        final double finalCoverage = computeCoverage(finalState);
        final boolean fullCoverage = (finalCoverage >= 0.999)
                || (0 == finalState.executableLeaves() && maxCoverageSeen >= 0.999 && !hasUncoveredLeaf(finalState));
        final long outroStart = Math.max(0L, contentSamples - samplesPerFrame);
        composeVerdictOutro(
                eventsByFrame,
                samplesPerFrame,
                outroStart,
                hadRawFailure,
                hadRawWarning,
                hadReproducedKnownIssue,
                fullCoverage);

        return eventsByFrame;
    }

    private static void composeLiquidGroove(
            final Map<Integer, List<DnbEvent>> eventsByFrame,
            final int samplesPerFrame,
            final long contentSamples,
            final boolean[] frameActive,
            final double[] frameVelocity,
            final double[] frameCoverage,
            final int[] frameMood) {

        final int kickDur = DnbSynth.millisToSamples(170);
        final int snareDur = DnbSynth.millisToSamples(155);
        final int ghostDur = DnbSynth.millisToSamples(68);
        final int hatDur = DnbSynth.millisToSamples(78);
        final int openHatDur = DnbSynth.millisToSamples(145);
        final int rimDur = DnbSynth.millisToSamples(58);
        final int rhodesDur = DnbSynth.millisToSamples(1_450);

        final int totalSteps = Math.max(1, (int) ((contentSamples - 1L) / DnbSynth.STEP_SAMPLES) + 1);
        final int maxFrameIdx = frameActive.length - 1;

        for (int step = 0; step < totalSteps; step++) {
            final long baseSample = (long) step * DnbSynth.STEP_SAMPLES;
            if (baseSample >= contentSamples) {
                break;
            }
            final int frameIdx = Math.min(maxFrameIdx, (int) (baseSample / samplesPerFrame));
            final boolean active = frameActive[frameIdx];
            final double vel = frameVelocity[frameIdx];
            final double cov = frameCoverage[frameIdx];
            final int mood = frameMood[frameIdx];

            final int barNum = step / STEPS_PER_BAR;
            final int barIndex = barNum % 4;
            final int pos = step % STEPS_PER_BAR; // 0..15 within bar
            final boolean isTurnaroundBar = (3 == barIndex);
            final boolean isIntroBar = (0 == barNum && totalSteps > STEPS_PER_BAR * 2);

            final int subRoot = resolveSubRoot(mood, barIndex);
            final int[] chordIntervals = resolveChordIntervals(mood, barIndex);

            // Subtle micro-swing on odd 16th steps for human breakbeat feel
            final long stepSample = baseSample + ((1 == (pos & 1)) ? MICRO_SWING_SAMPLES : 0L);

            // 1. Lush Fender Rhodes Chord Bed on every bar downbeat (pos == 0)
            if (0 == pos) {
                final int rhodesBase = subRoot + 24; // Warm F3..Eb4 center
                final double rhodesBright = (2 == mood)
                        ? (0.28 + 0.16 * vel)
                        : (0.38 + 0.34 * cov + 0.14 * vel);
                final int[] voicing = {chordIntervals[0], chordIntervals[1], chordIntervals[2], chordIntervals[4]};
                for (int v = 0; v < voicing.length; v++) {
                    final double voicePan = -0.48 + 0.32 * v;
                    scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                            baseSample + (long) v * DnbSynth.millisToSamples(14),
                            rhodesDur,
                            VoiceKind.RHODES_CHORD,
                            rhodesBase + voicing[v],
                            RHODES_VOICE_AMP * (active ? 1.0 : 0.85),
                            rhodesBright,
                            voicePan));
                }
            }

            // 2. Deep Mono-Centered Rolling Sub-Bass (+ warm Reese layer under high energy or failure mood)
            if (0 == pos || 10 == pos) {
                final int subSteps = (0 == pos) ? 10 : 6;
                final int subDur = subSteps * DnbSynth.STEP_SAMPLES;
                final int noteMidi = (10 == pos)
                        ? (subRoot + ((2 == mood) ? 1 : (0 == (barIndex % 2) ? 7 : 5)))
                        : subRoot;
                final double subAmp = SUB_AMP * (active ? (0.86 + 0.14 * vel) : 0.64);
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        baseSample,
                        subDur,
                        VoiceKind.SUB_BASS,
                        noteMidi,
                        subAmp,
                        0.30 + 0.35 * vel,
                        0.0));

                // Layer classic detuned Reese bass when energetic or in raw-failure Techstep mood
                if ((2 == mood) || (active && vel > 0.35 && 0 == pos)) {
                    final double reeseBright = (2 == mood) ? (0.52 + 0.24 * vel) : (0.22 + 0.25 * vel);
                    final double reeseAmp = REESE_BED_AMP * ((2 == mood) ? 1.15 : 0.78);
                    scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                            baseSample,
                            subDur,
                            VoiceKind.REESE_BASS,
                            noteMidi + 12.0,
                            reeseAmp,
                            reeseBright,
                            0.0));
                }
            }

            // 3. Breathing 175 BPM Two-Step Liquid Breakbeat
            //    Standard DnB skeleton: Kick on 0 & 10, Snare on 4 & 12, Ghost Snares on 7 & 9,
            //    Silky Ride/Hat on 8ths (and soft 16th shuffles when active).
            //    During idle/cooldown frames, drums pull back gracefully to half-time breath!
            if (0 == pos) {
                // Main downbeat kick (always grounded, softer in intro/idle)
                final double kGain = isIntroBar ? 0.62 : (active ? 1.0 : 0.68);
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        baseSample,
                        kickDur,
                        VoiceKind.DNB_KICK,
                        48.0,
                        KICK_AMP * kGain,
                        0.50,
                        0.0));
            } else if (10 == pos && active && !isIntroBar) {
                // Classic syncopated two-step DnB second kick on beat 3.5!
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        baseSample,
                        kickDur,
                        VoiceKind.DNB_KICK,
                        48.0,
                        KICK_AMP * (0.86 + 0.12 * vel),
                        0.48,
                        0.0));
            }

            // Two-step backbeat snares (4 & 12 when active; gentle half-time on 8 only when idle)
            if (active && !isIntroBar) {
                if (4 == pos || 12 == pos) {
                    scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                            baseSample,
                            snareDur,
                            VoiceKind.DNB_SNARE,
                            60.0,
                            SNARE_AMP * ((12 == pos) ? 0.94 : 1.0),
                            0.44 + 0.26 * vel,
                            (4 == pos) ? -0.05 : 0.05));
                } else if (7 == pos || 9 == pos || (isTurnaroundBar && 15 == pos)) {
                    // Feather-light rolling Amen ghost snares!
                    scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                            stepSample,
                            ghostDur,
                            VoiceKind.GHOST_SNARE,
                            62.0,
                            GHOST_SNARE_AMP * (0.78 + 0.30 * vel),
                            0.40 + 0.20 * vel,
                            (7 == pos) ? -0.14 : 0.14));
                } else if (isTurnaroundBar && 14 == pos && vel > 0.25) {
                    // Subtle 4-bar liquid turnaround rim accent
                    scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                            baseSample,
                            rimDur,
                            VoiceKind.RIM_CLICK,
                            69.0,
                            RIM_AMP * 0.62,
                            0.50,
                            0.22));
                }
            } else if (!active && 8 == pos) {
                // Half-time atmospheric breakdown rim/ghost on beat 3 during camera cooldown
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        baseSample,
                        ghostDur,
                        VoiceKind.GHOST_SNARE,
                        62.0,
                        GHOST_SNARE_AMP * 0.72,
                        0.32,
                        0.08));
            }

            // Silky brushed ride / hi-hat motion (8ths always, gentle off-16ths on active frames)
            if (0 == (pos % 2)) {
                final boolean accent = (2 == pos || 6 == pos || 10 == pos || 14 == pos);
                final double hatGain = (active ? 1.0 : 0.56) * (accent ? 1.0 : 0.62);
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        baseSample,
                        accent ? openHatDur : hatDur,
                        VoiceKind.LIQUID_HAT,
                        78.0,
                        HAT_AMP * hatGain,
                        accent ? (0.48 + 0.24 * vel) : 0.28,
                        accent ? 0.24 : -0.18));
            } else if (active && vel > 0.20) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        stepSample,
                        hatDur,
                        VoiceKind.LIQUID_HAT,
                        80.0,
                        HAT_AMP * 0.42 * vel,
                        0.26,
                        -0.26));
            }
        }
    }

    private static void composeVerdictOutro(
            final Map<Integer, List<DnbEvent>> eventsByFrame,
            final int samplesPerFrame,
            final long outroStart,
            final boolean hadRawFailure,
            final boolean hadRawWarning,
            final boolean hadAnyKnownIssue,
            final boolean fullCoverage) {

        final int chordDur = DnbSynth.millisToSamples(1_650);
        final int subDur = DnbSynth.millisToSamples(1_450);

        if (hadRawFailure) {
            // Verdict 5: RAW FAILURE REGRESSION -> brooding Fm(b9) + deep F1 Reese power-down drop
            for (int i = 0; i < OUTRO_FAILURE_MIDI.length; i++) {
                final double pan = -0.62 + (1.24 * i / Math.max(1, OUTRO_FAILURE_MIDI.length - 1));
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        outroStart + (long) i * DnbSynth.millisToSamples(28),
                        chordDur,
                        VoiceKind.RHODES_CHORD,
                        OUTRO_FAILURE_MIDI[i],
                        OUTRO_CHORD_AMP * 0.92,
                        0.24,
                        pan));
            }
            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                    outroStart,
                    subDur,
                    VoiceKind.FAULT_IMPACT,
                    41.0, // F2 diving down an octave to F1 (29)
                    29.0,
                    FAILURE_RAW_AMP * 0.88,
                    0.78,
                    0.0));
            return;
        }

        if (hadRawWarning) {
            // Verdict 3: PASSED WITH RAW WARNINGS -> suspended Fm11 / Eb9sus4 + warm amber horn fifth
            for (int i = 0; i < OUTRO_WARNING_MIDI.length; i++) {
                final double pan = -0.62 + (1.24 * i / Math.max(1, OUTRO_WARNING_MIDI.length - 1));
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        outroStart + (long) i * DnbSynth.millisToSamples(32),
                        chordDur,
                        VoiceKind.RHODES_CHORD,
                        OUTRO_WARNING_MIDI[i],
                        OUTRO_CHORD_AMP,
                        0.48,
                        pan));
            }
            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                    outroStart + DnbSynth.millisToSamples(110),
                    DnbSynth.millisToSamples(920),
                    VoiceKind.AMBER_HORN,
                    63.0, // Eb4 warm suspended amber horn
                    WARNING_RAW_AMP * 0.82,
                    0.56,
                    0.18));
            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                    outroStart,
                    subDur,
                    VoiceKind.SUB_BASS,
                    39.0, // Eb2 grounded suspended bass
                    SUB_AMP * 0.85,
                    0.32,
                    0.0));
            return;
        }

        if (!fullCoverage) {
            // Verdict 4: INCOMPLETE COVERAGE (no failures, no warnings, <100% leaves) -> open Fm9 half-cadence
            for (int i = 0; i < OUTRO_PARTIAL_MIDI.length; i++) {
                final double pan = -0.54 + (1.08 * i / Math.max(1, OUTRO_PARTIAL_MIDI.length - 1));
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        outroStart + (long) i * DnbSynth.millisToSamples(35),
                        chordDur,
                        VoiceKind.RHODES_CHORD,
                        OUTRO_PARTIAL_MIDI[i],
                        OUTRO_CHORD_AMP * 0.90,
                        0.42,
                        pan));
            }
            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                    outroStart,
                    subDur,
                    VoiceKind.SUB_BASS,
                    29.0,
                    SUB_AMP * 0.72,
                    0.24,
                    0.0));
            return;
        }

        if (hadAnyKnownIssue) {
            // Verdict 2: 100% CLEAN PASS WITH KNOWN ISSUES ONLY -> warm Fmaj9 + single soft muted amber echo
            for (int i = 0; i < OUTRO_KNOWN_OK_MIDI.length; i++) {
                final double pan = -0.64 + (1.28 * i / Math.max(1, OUTRO_KNOWN_OK_MIDI.length - 1));
                scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                        outroStart + (long) i * DnbSynth.millisToSamples(30),
                        chordDur,
                        VoiceKind.RHODES_CHORD,
                        OUTRO_KNOWN_OK_MIDI[i],
                        OUTRO_CHORD_AMP,
                        0.68,
                        pan));
            }
            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                    outroStart,
                    subDur,
                    VoiceKind.SUB_BASS,
                    29.0,
                    SUB_AMP * 0.85,
                    0.35,
                    0.0));
            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                    outroStart + DnbSynth.millisToSamples(150),
                    DnbSynth.millisToSamples(700),
                    VoiceKind.AMBER_HORN,
                    60.0,
                    WARNING_KNOWN_AMP * 1.10,
                    0.24,
                    -0.22));
            return;
        }

        // Verdict 1: SPOTLESS 100% CLEAN PASS -> wide radiant Fmaj9 sunrise + soaring 3-note crown
        for (int i = 0; i < OUTRO_SPOTLESS_MIDI.length; i++) {
            final double pan = -0.68 + (1.36 * i / Math.max(1, OUTRO_SPOTLESS_MIDI.length - 1));
            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                    outroStart + (long) i * DnbSynth.millisToSamples(28),
                    chordDur,
                    VoiceKind.RHODES_CHORD,
                    OUTRO_SPOTLESS_MIDI[i],
                    OUTRO_CHORD_AMP * 1.06,
                    0.82,
                    pan));
        }
        scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                outroStart,
                subDur,
                VoiceKind.SUB_BASS,
                29.0,
                SUB_AMP * 0.90,
                0.42,
                0.0));
        // Soaring 3-note sunrise bell crown drifting into tempo-locked dub delay
        final int[] crown = {72, 76, 79}; // C5 -> E5 -> G5
        for (int c = 0; c < crown.length; c++) {
            scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                    outroStart + DnbSynth.millisToSamples(120 + c * 95),
                    DnbSynth.millisToSamples(900),
                    VoiceKind.LIQUID_ARP,
                    crown[c],
                    crown[c] + (2 == c ? 2.0 : 0.0),
                    LEAF_ARP_AMP * (0.85 - 0.10 * c),
                    0.82,
                    -0.24 + 0.24 * c));
        }
        // Soft silky ride shimmer exhale
        scheduleEvent(eventsByFrame, samplesPerFrame, new DnbEvent(
                outroStart + DnbSynth.millisToSamples(60),
                DnbSynth.millisToSamples(1_050),
                VoiceKind.AIR_SWEEP,
                74.0,
                56.0,
                SWEEP_AMP * 0.52,
                0.44,
                0.12));
    }

    private static void scheduleEvent(
            final Map<Integer, List<DnbEvent>> eventsByFrame,
            final int samplesPerFrame,
            final DnbEvent event) {
        final int frameBucket = (int) Math.max(0L, event.startSample() / samplesPerFrame);
        eventsByFrame.computeIfAbsent(frameBucket, ignored -> new ArrayList<>()).add(event);
    }

    private static int resolveSubRoot(final int mood, final int barIndex) {
        return switch (mood) {
            case 2 -> SUB_ROOTS_FAIL[barIndex];
            case 1 -> SUB_ROOTS_WARN[barIndex];
            default -> SUB_ROOTS_CLEAN[barIndex];
        };
    }

    private static int[] resolveChordIntervals(final int mood, final int barIndex) {
        return switch (mood) {
            case 2 -> CHORD_INTERVALS_FAIL[barIndex];
            case 1 -> CHORD_INTERVALS_WARN[barIndex];
            default -> CHORD_INTERVALS_CLEAN[barIndex];
        };
    }

    private static double resolveLiquidPitch(
            final int subRoot,
            final int[] chordIntervals,
            final NodeState node,
            final double rowRatio) {
        final int pathHash = Math.abs(stableHash(node.path()));
        final int degree = (node.col() * 2 + (int) Math.round(rowRatio * 4.0) + (pathHash % 3)) % LIQUID_MELODY_SCALE.length;
        final int chordAnchor = chordIntervals[(node.col() + pathHash) % chordIntervals.length];
        final int rawMidi = (0 == (pathHash % 3))
                ? (subRoot + 36 + chordAnchor)
                : (65 + LIQUID_MELODY_SCALE[degree]); // F4 anchor
        // Keep melodies in an ergonomic warm mid-high register [60..81] (C4..A5, never shrieking)
        int midi = rawMidi;
        while (midi < 60) {
            midi += 12;
        }
        while (midi > 81) {
            midi -= 12;
        }
        return midi;
    }

    private static int stableHash(final String value) {
        int h = 0x811C9DC5;
        for (int i = 0; i < value.length(); i++) {
            h ^= value.charAt(i);
            h *= 0x01000193;
        }
        return h;
    }

    private static double computeStereoPan(final int col, final int span) {
        if (span <= 0) {
            return 0.0;
        }
        final double normalized = Math.max(0.0, Math.min(1.0, (double) col / span));
        return clampPan(normalized * 1.52 - 0.76);
    }

    private static double clampPan(final double pan) {
        return Math.max(-0.84, Math.min(0.84, pan));
    }

    private static int computeFrameMaxCol(final FrameState state) {
        int maxCol = 0;
        for (final NodeState node : state.nodes().values()) {
            if (node.col() > maxCol) {
                maxCol = node.col();
            }
        }
        return maxCol;
    }

    private static double computeCoverage(final FrameState state) {
        if (state.executableLeaves() > 0) {
            return Math.max(0.0, Math.min(1.0, (double) state.coveredLeaves() / state.executableLeaves()));
        }
        int leaves = 0;
        int covered = 0;
        for (final NodeState node : state.nodes().values()) {
            if ('L' == node.type() || 'M' == node.type()) {
                leaves++;
                if (node.executions() >= Math.max(1, node.target())) {
                    covered++;
                }
            }
        }
        return (leaves > 0) ? ((double) covered / leaves) : 0.0;
    }

    private static boolean hasUncoveredLeaf(final FrameState state) {
        for (final NodeState node : state.nodes().values()) {
            if (('L' == node.type() || 'M' == node.type()) && node.executions() < Math.max(1, node.target())) {
                return true;
            }
        }
        return false;
    }

    private static int statusSeverity(final char status) {
        return switch (status) {
            case 'F' -> 4;
            case 'U' -> 3;
            case 'W' -> 2;
            case 'I' -> 1;
            default -> 0;
        };
    }

    private static String lineagePath(final NodeData node) {
        final List<String> segments = new ArrayList<>(8);
        NodeData cursor = node;
        int guard = 0;
        while (null != cursor && guard++ < 64) {
            final String label = (null != cursor.label() && !cursor.label().isBlank())
                    ? cursor.label()
                    : cursor.id();
            segments.add(label);
            cursor = cursor.parent();
        }
        Collections.reverse(segments);
        return String.join("/", segments);
    }

    private static char resolveTypeCode(
            final NodeData node,
            final ScenarioType scenarioType,
            final boolean executable) {
        if (!executable) {
            return 'X';
        }
        if (ScenarioType.MONO == scenarioType) {
            return 'M';
        }
        if (node.children().isEmpty() && TreeProgressMetrics.isTerminalScenario(node)) {
            return 'L';
        }
        return 'N';
    }

    private static char resolveStatusCode(
            final NodeData node,
            final int executions,
            final Map<NodeData, Boolean> childFailureCache) {
        if (TreeHtml.hasFailure(node)) {
            return 'F';
        }
        if (TreeHtml.hasChildFailure(node, childFailureCache)) {
            return 'U';
        }
        if (TreeHtml.hasWarning(node)) {
            return 'W';
        }
        if (TreeHtml.hasNotice(node)) {
            return 'I';
        }
        return (executions > 0) ? 'P' : 'N';
    }

    private static FrameState readFrameState(final File frame) throws IOException {
        final File snapshot = snapshotFile(frame);
        if (snapshot.isFile()) {
            return readSnapshotFile(snapshot);
        }
        return readSvgFallback(frame);
    }

    private static FrameState readSnapshotFile(final File snapshot) throws IOException {
        final Map<String, NodeState> nodes = new LinkedHashMap<>();
        double zoom = 1.0;
        double tx = 0.0;
        double ty = 0.0;
        int executableLeaves = 0;
        int coveredLeaves = 0;

        try (BufferedReader reader = Files.newBufferedReader(snapshot.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while (null != (line = reader.readLine())) {
                if (line.isEmpty()) {
                    continue;
                }
                final String[] parts = line.split("\t", -1);
                if ("#meta".equals(parts[0]) && parts.length >= 6) {
                    zoom = parseDoubleSafe(parts[1], 1.0);
                    tx = parseDoubleSafe(parts[2], 0.0);
                    ty = parseDoubleSafe(parts[3], 0.0);
                    executableLeaves = parseIntSafe(parts[4], 0);
                    coveredLeaves = parseIntSafe(parts[5], 0);
                    continue;
                }
                if (parts.length < 8) {
                    continue;
                }
                final String path = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                final int col = parseIntSafe(parts[1], 0);
                final int rowTenths = parseIntSafe(parts[2], 0);
                final char type = parts[3].isEmpty() ? 'N' : parts[3].charAt(0);
                final int execs = parseIntSafe(parts[4], 0);
                final int target = Math.max(1, parseIntSafe(parts[5], 1));
                final char status = parts[6].isEmpty() ? 'N' : parts[6].charAt(0);
                final boolean knownIssue = "1".equals(parts[7]);
                nodes.put(path, new NodeState(path, col, rowTenths, type, execs, target, status, knownIssue));
            }
        }
        return new FrameState(nodes, zoom, tx, ty, executableLeaves, coveredLeaves, -1);
    }

    private static FrameState readSvgFallback(final File svgFile) throws IOException {
        final String content = Files.readString(svgFile.toPath(), StandardCharsets.UTF_8);
        double zoom = 1.0;
        double tx = 0.0;
        double ty = 0.0;

        final int camIdx = content.indexOf("<g transform=\"translate(");
        if (camIdx >= 0) {
            final int tagEnd = content.indexOf('>', camIdx);
            if (tagEnd > camIdx) {
                final String camTag = content.substring(camIdx, tagEnd);
                final String tStr = extractAttr(camTag, "translate(");
                if (null != tStr) {
                    final int comma = tStr.indexOf(',');
                    final int end = tStr.indexOf(')');
                    if (comma > 0 && end > comma) {
                        tx = parseDoubleSafe(tStr.substring(0, comma), 0.0);
                        ty = parseDoubleSafe(tStr.substring(comma + 1, end), 0.0);
                    }
                }
                final int scaleIdx = camTag.indexOf("scale(");
                if (scaleIdx >= 0) {
                    final int scaleEnd = camTag.indexOf(')', scaleIdx + 6);
                    if (scaleEnd > scaleIdx + 6) {
                        zoom = parseDoubleSafe(camTag.substring(scaleIdx + 6, scaleEnd), 1.0);
                    }
                }
            }
        }

        int headerCovered = 0;
        int headerExecutable = 0;
        int testCycle = -1;
        for (final String leafLabel : new String[]{"Leaves:", "Leafs:"}) {
            final int lIdx = content.indexOf(leafLabel);
            if (lIdx >= 0) {
                final int endTag = content.indexOf('<', lIdx);
                if (endTag > lIdx) {
                    final int[] parsed = parseSlashPair(content.substring(lIdx + leafLabel.length(), endTag));
                    headerCovered = parsed[0];
                    headerExecutable = parsed[1];
                    break;
                }
            }
        }
        final int testIdx = content.indexOf("Test #");
        if (testIdx >= 0) {
            final int endTag = content.indexOf('<', testIdx);
            if (endTag > testIdx) {
                testCycle = parseLeadingInt(content.substring(testIdx + 6, endTag), -1);
            }
        }

        final List<ParsedSvgCard> cards = new ArrayList<>();
        int cursor = 0;
        while (true) {
            final int gStart = content.indexOf("<g id=\"", cursor);
            if (gStart < 0) {
                break;
            }
            final int openEnd = content.indexOf('>', gStart);
            if (openEnd < 0) {
                break;
            }
            final String openTag = content.substring(gStart, openEnd + 1);
            final int gEnd = content.indexOf("</g>", openEnd);
            if (gEnd < 0) {
                break;
            }
            cursor = gEnd + 4;
            if (!openTag.contains("class=\"node-card ")) {
                continue;
            }
            final ParsedSvgCard card = parseSvgCard(openTag, content.substring(openEnd + 1, gEnd));
            if (null != card) {
                cards.add(card);
            }
        }

        final Map<String, NodeState> nodes = new LinkedHashMap<>();
        final Map<String, Integer> dupCounter = new HashMap<>();
        for (final ParsedSvgCard c : cards) {
            final int col = Math.max(0, (int) Math.round((c.x - 40.0) / 270.0));
            final int rowTenths = Math.max(0, (int) Math.round(((c.y - 92.0) / 60.0) * 10.0));
            final String baseKey = col + ":" + c.cleanName;
            final int dup = dupCounter.getOrDefault(baseKey, 0);
            dupCounter.put(baseKey, dup + 1);
            final String path = (0 == dup) ? baseKey : (baseKey + "#" + dup);
            final char type = inferSvgType(c);
            final char status = inferSvgStatus(c.cssClasses, c.executions);
            final boolean knownIssue = c.cssClasses.contains("known-issue")
                    || c.titleText.toLowerCase(Locale.ROOT).contains("known issue");
            nodes.put(path, new NodeState(path, col, rowTenths, type, c.executions, c.target, status, knownIssue));
        }

        return new FrameState(nodes, zoom, tx, ty, headerExecutable, headerCovered, testCycle);
    }

    private static ParsedSvgCard parseSvgCard(final String openTag, final String body) {
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
        final String title = extractElementText(body, "<title>", "</title>");
        final String tx1 = extractClassText(body, "<text class=\"tx1\"");
        final int[] execAndTarget = parseBadgeExecs(tx1);
        final String cleanName = stripScenarioPrefix(id);
        return new ParsedSvgCard(
                cleanName,
                css,
                x,
                y,
                (null != title) ? title : "",
                (null != tx1) ? tx1 : "",
                execAndTarget[0],
                execAndTarget[1]);
    }

    private static char inferSvgType(final ParsedSvgCard card) {
        if (card.cssClasses.contains("non_executable") || "OFF".equalsIgnoreCase(card.badgeText.trim())) {
            return 'X';
        }
        if (card.titleText.indexOf('◆') >= 0) {
            return 'M';
        }
        if (card.titleText.indexOf('◼') >= 0 || card.badgeText.indexOf('/') >= 0) {
            return 'L';
        }
        return 'N';
    }

    private static char inferSvgStatus(final String cssClasses, final int executions) {
        if (cssClasses.contains("failed") && !cssClasses.contains("child_failed")) {
            return 'F';
        }
        if (cssClasses.contains("child_failed")) {
            return 'U';
        }
        if (cssClasses.contains("warning")) {
            return 'W';
        }
        if (cssClasses.contains("notice")) {
            return 'I';
        }
        return (executions > 0) ? 'P' : 'N';
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

    private static String extractAttr(final String tag, final String prefix) {
        final int idx = tag.indexOf(prefix);
        if (idx < 0) {
            return null;
        }
        final int start = idx + prefix.length();
        final int end = tag.indexOf('"', start);
        return (end > start) ? tag.substring(start, end) : null;
    }

    private static String extractElementText(final String body, final String open, final String close) {
        final int start = body.indexOf(open);
        if (start < 0) {
            return null;
        }
        final int end = body.indexOf(close, start + open.length());
        return (end > start) ? body.substring(start + open.length(), end) : null;
    }

    private static String extractClassText(final String body, final String openPrefix) {
        final int idx = body.indexOf(openPrefix);
        if (idx < 0) {
            return null;
        }
        final int gt = body.indexOf('>', idx);
        if (gt < 0) {
            return null;
        }
        final int end = body.indexOf("</text>", gt + 1);
        return (end > gt) ? body.substring(gt + 1, end) : null;
    }

    private static int[] parseBadgeExecs(final String badge) {
        if (null == badge || badge.isBlank()) {
            return new int[]{0, 1};
        }
        final int slash = badge.indexOf('/');
        if (slash > 0) {
            final int[] pair = parseSlashPair(badge);
            return new int[]{pair[0], Math.max(1, pair[1])};
        }
        return new int[]{Math.max(0, parseLeadingInt(badge, 0)), 1};
    }

    private static int[] parseSlashPair(final String text) {
        final int slash = text.indexOf('/');
        if (slash < 0) {
            return new int[]{0, 0};
        }
        final int left = parseTrailingInt(text.substring(0, slash), 0);
        final int right = parseLeadingInt(text.substring(slash + 1), 0);
        return new int[]{Math.max(0, left), Math.max(0, right)};
    }

    private static int parseLeadingInt(final String text, final int fallback) {
        int i = 0;
        while (i < text.length() && !Character.isDigit(text.charAt(i))) {
            i++;
        }
        final int start = i;
        while (i < text.length() && Character.isDigit(text.charAt(i))) {
            i++;
        }
        return (i > start) ? parseIntSafe(text.substring(start, i), fallback) : fallback;
    }

    private static int parseTrailingInt(final String text, final int fallback) {
        int end = text.length();
        while (end > 0 && !Character.isDigit(text.charAt(end - 1))) {
            end--;
        }
        int start = end;
        while (start > 0 && Character.isDigit(text.charAt(start - 1))) {
            start--;
        }
        return (end > start) ? parseIntSafe(text.substring(start, end), fallback) : fallback;
    }

    private static int parseIntSafe(final String text, final int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (final NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDoubleSafe(final String text, final double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (final NumberFormatException ignored) {
            return fallback;
        }
    }

    private record NodeState(
            String path,
            int col,
            int rowTenths,
            char type,
            int executions,
            int target,
            char status,
            boolean knownIssue) {
    }

    private record NodeDelta(
            NodeState node,
            int prevExec,
            int execDelta,
            boolean statusWorsened,
            boolean newNotice) {
    }

    private record FrameState(
            Map<String, NodeState> nodes,
            double zoom,
            double tx,
            double ty,
            int executableLeaves,
            int coveredLeaves,
            int testCycle) {
    }

    private record ParsedSvgCard(
            String cleanName,
            String cssClasses,
            int x,
            int y,
            String titleText,
            String badgeText,
            int executions,
            int target) {
    }
}
