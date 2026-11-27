package works.lysenko.base.output.sonifier;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.sonifier.NeonSynth.NeonEvent;
import works.lysenko.base.output.sonifier.NeonSynth.VoiceKind;
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

import static works.lysenko.util.data.strs.Swap.s;

/**
 * Kinetic midnight-synthwave &amp; analog-modular telemetry sonifier ({@code neon}).
 * <p>
 * Turns scenario tree execution into a driving true-stereo subtractive synth track rooted in
 * {@code F# minor} ({@code F#m9 → Dmaj9 → E6/9 → C#m7}, darkening into {@code F# Phrygian}
 * under raw regressions and lifting into a wide {@code F#maj9} sunrise swell on clean completion):
 * <ul>
 *     <li><b>Kinetic Rolling Bass &amp; Kick Pulse</b>: Active execution frames drive a PolyBLEP
 *     analog bassline whose resonant filter cutoff opens dynamically with execution velocity,
 *     anchored by sidechain-pumping 808 kicks on test cycle boundaries.</li>
 *     <li><b>Stereo Bank Panorama &amp; Resonant Arp Plucks</b>: Tree column depth pans across
 *     the stereo stage while vertical position shapes filter brightness. Branch steps play snappy
 *     PWM-square arp plucks with familiarity damping into tight cyber hi-hats on hot parents.</li>
 *     <li><b>Laser Leaf Discoveries &amp; Shimmer Arpeggios</b>: Partial leaves fire pitch-snapped
 *     laser chirps; newly covered leaves burst into 3-note octave-shimmering supersaw arpeggios;
 *     over-executions beyond target register as crisp filtered cyber-hat ticks.</li>
 *     <li><b>Color-Coded Status Semantics</b>: Electric-cyan harmonic bells for {@code NOTICE},
 *     detuned amber brass stabs for raw {@code WARNING}s, and unmistakable ring-modulated bitcrushed
 *     glitch alarms for raw {@code FAILED} regressions (with muted, cooldown-gated pulses for known issues).</li>
 *     <li><b>Leaf Disappearance &amp; Final Compaction Lock-In</b>: Hidden passed leaves and pruned
 *     branches gently sink and fade down in a warm low-register low-pass dissolve from their stereo
 *     column position, while post-test column-bank compaction ({@code .tree.allow.final.reordering})
 *     triggers a resonant band-pass lock sweep and cooldown camera shimmers.</li>
 * </ul>
 */
public final class NeonSonifier implements TreeSonifier {

    /**
     * Configuration property value activating the Neon sonifier.
     */
    public static final String MODE = "neon";

    // 4-bar midnight synthwave cycle in F# minor:
    // Bar 0: F#m9 (F#2=42), Bar 1: Dmaj9 (D2=38), Bar 2: E6/9 (E2=40), Bar 3: C#m7 (C#2=37)
    private static final int[] BASS_ROOTS_CLEAN = {42, 38, 40, 37};
    private static final int[] BASS_ROOTS_TENSE = {42, 38, 43, 40}; // Bar 2 shifts to G (Phrygian b2 tension)
    private static final int[][] CHORD_INTERVALS_CLEAN = {
            {0, 3, 7, 10, 14}, // F#m9
            {0, 4, 7, 11, 14}, // Dmaj9
            {0, 4, 7, 9, 14},  // E6/9
            {0, 3, 7, 10, 12}  // C#m7
    };
    private static final int[][] CHORD_INTERVALS_TENSE = {
            {0, 3, 7, 10, 14}, // F#m9
            {0, 4, 7, 11, 14}, // Dmaj9
            {0, 4, 7, 11, 14}, // Gmaj9 over F# Phrygian tension
            {0, 3, 7, 10, 13}  // Em(b9) pull back to F#
    };

    // F# natural minor / Dorian hybrid melody scale (2 octaves from F#3 = 54)
    private static final int[] NEON_MELODY_SCALE = {0, 2, 3, 7, 9, 10, 12, 14, 15, 19, 22};
    private static final int MIN_MELODY_MIDI = 49; // C#3 (~139 Hz)
    private static final int MAX_MELODY_MIDI = 73; // C#5 (~554 Hz)
    // Sunrise clean finale: F#maj9 (F#3=54, C#4=61, G#4=68, A#4=70, F5=77)
    private static final int[] SUNRISE_FINALE_MIDI = {54, 61, 68, 70, 77};
    // Brooding regression finale: F#m(add9) (F#3=54, C#4=61, A4=69, G#4=68, C#5=73)
    private static final int[] DARK_FINALE_MIDI = {54, 61, 68, 69, 73};

    private static final int CHORD_BAR_FRAMES = 8;
    private static final int KNOWN_ISSUE_COOLDOWN_FRAMES = 4;

    private static final double BASS_AMP = 0.145;
    private static final double KICK_AMP = 0.215;
    private static final double PAD_VOICE_AMP = 0.056;
    private static final double PLUCK_AMP = 0.125;
    private static final double LASER_AMP = 0.132;
    private static final double SHIMMER_AMP = 0.140;
    private static final double HAT_AMP = 0.088;
    private static final double NOTICE_AMP = 0.105;
    private static final double WARNING_RAW_AMP = 0.135;
    private static final double WARNING_KNOWN_AMP = 0.068;
    private static final double FAILURE_RAW_AMP = 0.270;
    private static final double FAILURE_KNOWN_AMP = 0.105;
    private static final double SWEEP_AMP = 0.105;
    private static final double FINALE_AMP = 0.105;

    /**
     * Resolves the sidecar snapshot file corresponding to a rendered progression frame.
     *
     * @param frame rendered progression frame ({@code .svg} or {@code .png})
     * @return sidecar {@code .neon} file inside {@code neon_events/}
     */
    public static File snapshotFile(final File frame) {
        return new File(new File(frame.getParentFile(), "neon_events"),
                frame.getName().replaceFirst("\\.(png|svg)$", ".neon"));
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
        return !frames.isEmpty() && frames.stream().allMatch(NeonSonifier::hasReadableFrame);
    }

    @Override
    public void writeWav(final List<File> frames, final File output, final int frameRate) throws IOException {
        if (frames.isEmpty() || frameRate < 1) {
            throw new IllegalArgumentException("Audio requires frames and a positive frame rate");
        }
        if (NeonSynth.SAMPLE_RATE % frameRate != 0) {
            throw new IllegalArgumentException("Frame rate must divide the audio sample rate");
        }
        final int samplesPerFrame = NeonSynth.SAMPLE_RATE / frameRate;
        final long totalSamples = (long) frames.size() * samplesPerFrame + NeonSynth.TAIL_SAMPLES;
        final Map<Integer, List<NeonEvent>> events = composeNeonEvents(frames, samplesPerFrame);
        NeonSynth.writeStereoWav(output, totalSamples, samplesPerFrame, events);
    }

    private static boolean hasReadableFrame(final File frame) {
        if (snapshotFile(frame).isFile()) {
            return true;
        }
        return frame.isFile()
                && frame.getName().toLowerCase(Locale.ROOT).endsWith(".svg")
                && frame.length() > 0L;
    }

    static Map<Integer, List<NeonEvent>> composeNeonEvents(
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

        final Map<Integer, List<NeonEvent>> eventsByFrame = new HashMap<>();
        final Map<String, NodeState> previousNodes = new HashMap<>();

        int previousBar = -1;
        int previousNodeCount = -1;
        int previousFrameMaxCol = -1;
        int previousTestCycle = -1;
        int lastKnownWarnFrame = -KNOWN_ISSUE_COOLDOWN_FRAMES;
        int lastKnownFailFrame = -KNOWN_ISSUE_COOLDOWN_FRAMES;
        double previousZoom = Double.NaN;
        double previousTx = Double.NaN;
        double previousTy = Double.NaN;
        double smoothedVelocity = 0.25;
        boolean hadRawFailure = false;

        final int padDuration = NeonSynth.millisToSamples(1_150);
        final int bassDuration = Math.max(NeonSynth.millisToSamples(135), (int) (samplesPerFrame * 0.92));
        final int kickDuration = NeonSynth.millisToSamples(210);
        final int pluckDuration = NeonSynth.millisToSamples(175);
        final int laserDuration = NeonSynth.millisToSamples(210);
        final int shimmerDuration = NeonSynth.millisToSamples(560);
        final int hatDuration = NeonSynth.millisToSamples(62);
        final int noticeDuration = NeonSynth.millisToSamples(320);
        final int warningDuration = NeonSynth.millisToSamples(310);
        final int failureDuration = NeonSynth.millisToSamples(490);
        final int sweepDuration = NeonSynth.millisToSamples(420);
        final int finaleDuration = NeonSynth.millisToSamples(1_320);

        for (int frameIndex = 0; frameIndex < states.size(); frameIndex++) {
            final FrameState current = states.get(frameIndex);
            final long frameStart = (long) frameIndex * samplesPerFrame;
            final List<NeonEvent> frameEvents = new ArrayList<>();

            final int frameMaxCol = computeFrameMaxCol(current);
            final int panSpan = Math.max(1, Math.max(frameMaxCol, globalMaxCol));
            final double coverage = computeCoverage(current);

            // Detect active execution deltas on this frame
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
                if ('R' == node.status() || ('F' == node.status() && !node.knownIssue())) {
                    hadRawFailure = true;
                }
            }

            final boolean hasActivity = !deltas.isEmpty();
            final double instantVelocity = Math.min(1.0, totalExecDelta / 6.0);
            smoothedVelocity = 0.62 * smoothedVelocity + 0.38 * instantVelocity;

            // Harmonic bar progression (4-bar synthwave cycle advancing every CHORD_BAR_FRAMES)
            final int barIndex = (frameIndex / CHORD_BAR_FRAMES) % 4;
            final int bassRoot = hadRawFailure ? BASS_ROOTS_TENSE[barIndex] : BASS_ROOTS_CLEAN[barIndex];
            final int[] chordIntervals = hadRawFailure ? CHORD_INTERVALS_TENSE[barIndex] : CHORD_INTERVALS_CLEAN[barIndex];
            final boolean isFinalFrame = (frameIndex == states.size() - 1);

            // 1. Evolving Supersaw Chord Bed at each bar change (or opening frame)
            if (barIndex != previousBar || 0 == frameIndex) {
                previousBar = barIndex;
                final int padBase = bassRoot + 12;
                final double padBrightness = 0.22 + 0.42 * coverage + 0.22 * smoothedVelocity;
                final int[] chordVoicing = {chordIntervals[0], chordIntervals[1], chordIntervals[2], chordIntervals[4]};
                for (int v = 0; v < chordVoicing.length; v++) {
                    final double voicePan = -0.42 + 0.28 * v;
                    frameEvents.add(new NeonEvent(
                            frameStart + (long) v * (samplesPerFrame / 16),
                            padDuration,
                            VoiceKind.SUPERSAW_PAD,
                            padBase + chordVoicing[v],
                            PAD_VOICE_AMP,
                            padBrightness,
                            voicePan));
                }
            }

            // 2. Kinetic Rolling Bass Pulse & Test-Cycle 808 Kick
            final boolean newCycle = (current.testCycle() > 0 && current.testCycle() != previousTestCycle);
            if (newCycle || 0 == frameIndex || (hasActivity && 0 == (frameIndex % 4))) {
                frameEvents.add(new NeonEvent(
                        frameStart,
                        kickDuration,
                        VoiceKind.ANALOG_KICK,
                        46.0,
                        KICK_AMP * (newCycle ? 1.0 : 0.78),
                        0.45 + 0.35 * smoothedVelocity,
                        0.0));
            }
            if (hasActivity || 0 == (frameIndex % 2)) {
                final boolean octaveBounce = (1 == (frameIndex % 2));
                final double bassMidi = bassRoot + (octaveBounce ? 12.0 : 0.0);
                final double bassBright = hasActivity
                        ? (0.28 + 0.56 * smoothedVelocity)
                        : (0.12 + 0.16 * coverage);
                final double bassGain = hasActivity ? BASS_AMP : (BASS_AMP * 0.52);
                frameEvents.add(new NeonEvent(
                        frameStart,
                        bassDuration,
                        VoiceKind.ANALOG_BASS,
                        bassMidi,
                        bassGain,
                        bassBright,
                        octaveBounce ? 0.10 : -0.10));

                // Mid-frame rolling 16th-style sub-step on energetic frames
                if (hasActivity && samplesPerFrame >= NeonSynth.millisToSamples(70)) {
                    final int fifthOffset = (0 == (frameIndex % 3)) ? 7 : 12;
                    frameEvents.add(new NeonEvent(
                            frameStart + (samplesPerFrame / 2),
                            Math.max(NeonSynth.millisToSamples(75), samplesPerFrame / 2),
                            VoiceKind.ANALOG_BASS,
                            bassRoot + fifthOffset,
                            bassGain * 0.78,
                            bassBright * 0.90,
                            octaveBounce ? -0.12 : 0.12));
                }
            }

            // 3. Node Traversal, Leaf Discoveries, Over-Executions, and Semantic Statuses
            deltas.sort(Comparator.comparingInt((NodeDelta d) -> d.node().col())
                    .thenComparingInt(d -> d.node().rowTenths())
                    .thenComparing(d -> d.node().path()));

            final int maxVoicesThisFrame = Math.min(deltas.size(), 7);
            final long staggerStep = (maxVoicesThisFrame > 1)
                    ? Math.max(1L, (samplesPerFrame * 2L) / (3L * maxVoicesThisFrame))
                    : 0L;

            for (int i = 0; i < maxVoicesThisFrame; i++) {
                final NodeDelta delta = deltas.get(i);
                final NodeState node = delta.node();
                final long onset = frameStart + i * staggerStep;
                final double pan = computeStereoPan(node.col(), panSpan);
                final double rowRatio = 1.0 - Math.min(1.0, (double) node.rowTenths() / Math.max(10, globalMaxRowTenths));
                final double melodyMidi = resolveNeonPitch(bassRoot, chordIntervals, node, rowRatio);

                final boolean isLeaf = ('L' == node.type() || 'M' == node.type());
                if (delta.execDelta() > 0) {
                    if (isLeaf) {
                        final int target = Math.max(1, node.target());
                        if (delta.prevExec() < target && node.executions() >= target) {
                            // Newly completed leaf: luminous 3-note neon supersaw arpeggio!
                            final int[] arpOffsets = {0, 7, 12};
                            final long arpStep = Math.max(NeonSynth.millisToSamples(24), samplesPerFrame / 5);
                            for (int a = 0; a < arpOffsets.length; a++) {
                                frameEvents.add(new NeonEvent(
                                        onset + a * arpStep,
                                        shimmerDuration,
                                        VoiceKind.NEON_SHIMMER,
                                        melodyMidi + arpOffsets[a],
                                        SHIMMER_AMP * (1.0 - 0.12 * a),
                                        0.62 + 0.30 * rowRatio,
                                        clampPan(pan + (a - 1) * 0.14)));
                            }
                        } else if (node.executions() > target) {
                            // Monte-Carlo over-execution beyond target: crisp cyber-hat tick
                            frameEvents.add(new NeonEvent(
                                    onset,
                                    hatDuration,
                                    VoiceKind.CYBER_HAT,
                                    melodyMidi + 12.0,
                                    HAT_AMP,
                                    0.45 + 0.45 * rowRatio,
                                    pan));
                        } else {
                            // Partial leaf hit: pitch-snapped bright laser chirp
                            frameEvents.add(new NeonEvent(
                                    onset,
                                    laserDuration,
                                    VoiceKind.LASER_LEAD,
                                    melodyMidi,
                                    LASER_AMP,
                                    0.48 + 0.42 * rowRatio,
                                    pan));
                        }
                    } else {
                        // Branch node traversal: resonant PWM arp pluck with familiarity damping
                        final double familiarity = 1.0 / (1.0 + 0.32 * Math.log1p(delta.prevExec()));
                        if (delta.prevExec() >= 6 && familiarity < 0.52) {
                            frameEvents.add(new NeonEvent(
                                    onset,
                                    hatDuration,
                                    VoiceKind.CYBER_HAT,
                                    melodyMidi,
                                    HAT_AMP * 0.72,
                                    0.30 + 0.30 * rowRatio,
                                    pan));
                        } else {
                            frameEvents.add(new NeonEvent(
                                    onset,
                                    pluckDuration,
                                    VoiceKind.ARP_PLUCK,
                                    melodyMidi - 12.0,
                                    PLUCK_AMP * Math.max(0.45, familiarity),
                                    0.38 + 0.48 * rowRatio,
                                    pan));
                        }
                    }
                }

                // Semantic status cues
                if (delta.newNotice()) {
                    frameEvents.add(new NeonEvent(
                            onset,
                            noticeDuration,
                            VoiceKind.CYAN_BELL,
                            melodyMidi + 7.0,
                            NOTICE_AMP,
                            0.72,
                            pan));
                }
                if (delta.statusWorsened()) {
                    if ('W' == node.status()) {
                        if (node.knownIssue()) {
                            if (frameIndex - lastKnownWarnFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                                lastKnownWarnFrame = frameIndex;
                                frameEvents.add(new NeonEvent(
                                        onset,
                                        warningDuration,
                                        VoiceKind.AMBER_BRASS,
                                        bassRoot + 19.0,
                                        WARNING_KNOWN_AMP,
                                        0.24,
                                        pan));
                            }
                        } else {
                            frameEvents.add(new NeonEvent(
                                    onset,
                                    warningDuration,
                                    VoiceKind.AMBER_BRASS,
                                    bassRoot + 22.0, // minor 7th amber brass stab
                                    WARNING_RAW_AMP,
                                    0.78,
                                    pan));
                        }
                    } else if ('M' == node.status()) {
                        // Warning-tier regression (soft rose-magenta): amber brass + opposite-pan ghostly tritone bell echo
                        frameEvents.add(new NeonEvent(
                                onset,
                                warningDuration,
                                VoiceKind.AMBER_BRASS,
                                bassRoot + 22.0,
                                WARNING_RAW_AMP * 0.92,
                                0.80,
                                pan));
                        frameEvents.add(new NeonEvent(
                                onset + NeonSynth.millisToSamples(120),
                                noticeDuration,
                                VoiceKind.CYAN_BELL,
                                bassRoot + 30.0,
                                WARNING_RAW_AMP * 0.72,
                                0.88,
                                clampPan(-pan)));
                    } else if ('R' == node.status()) {
                        // Failure-tier regression (vivid electric magenta): double-strike glitch + piercing laser tritone ping
                        frameEvents.add(new NeonEvent(
                                onset,
                                failureDuration,
                                VoiceKind.GLITCH_FAULT,
                                bassRoot + 15.0,
                                FAILURE_RAW_AMP * 1.08,
                                0.96,
                                pan * 0.5));
                        frameEvents.add(new NeonEvent(
                                onset + NeonSynth.millisToSamples(115),
                                failureDuration * 3 / 4,
                                VoiceKind.GLITCH_FAULT,
                                bassRoot + 18.0,
                                FAILURE_RAW_AMP * 0.72,
                                0.94,
                                clampPan(-pan * 0.6)));
                        frameEvents.add(new NeonEvent(
                                onset + NeonSynth.millisToSamples(45),
                                laserDuration,
                                VoiceKind.LASER_LEAD,
                                bassRoot + 30.0,
                                FAILURE_RAW_AMP * 0.60,
                                0.95,
                                clampPan(-pan * 0.4)));
                    } else if ('F' == node.status()) {
                        if (node.knownIssue()) {
                            if (frameIndex - lastKnownFailFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                                lastKnownFailFrame = frameIndex;
                                frameEvents.add(new NeonEvent(
                                        onset,
                                        failureDuration,
                                        VoiceKind.GLITCH_FAULT,
                                        bassRoot + 12.0,
                                        FAILURE_KNOWN_AMP,
                                        0.22,
                                        pan));
                            }
                        } else {
                            frameEvents.add(new NeonEvent(
                                    onset,
                                    failureDuration,
                                    VoiceKind.GLITCH_FAULT,
                                    bassRoot + 13.0, // Phrygian b2 clash + tritone ring-mod glitch
                                    FAILURE_RAW_AMP,
                                    0.92,
                                    pan * 0.5));
                        }
                    }
                }
            }

            // 4. Leaf Disappearance (Fade-Down) & Post-Test Bank-Column Compaction Lock-In
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
                // Warm low-register downward fade-out as hidden passed leaves or pruned branches disappear
                final int removed = Math.max(vanishedCount, Math.max(1, previousNodeCount - currentNodeCount));
                final double sweepIntensity = Math.min(1.0, removed / 8.0);
                final double vanishPan = (vanishedCount > 0)
                        ? clampPan(Math.clamp((vanishedColSum / vanishedCount) / panSpan, 0.0, 1.0) * 1.56 - 0.78)
                        : 0.0;
                frameEvents.add(new NeonEvent(
                        frameStart,
                        sweepDuration,
                        VoiceKind.FADE_DISSOLVE,
                        bassRoot + 12.0,
                        bassRoot,
                        SWEEP_AMP * (0.62 + 0.32 * sweepIntensity),
                        0.34,
                        vanishPan));
            } else if (columnsCompacted) {
                // Signature Neon Lock-In Sweep when final re-optimization collapses column banks
                frameEvents.add(new NeonEvent(
                        frameStart,
                        sweepDuration + NeonSynth.millisToSamples(180),
                        VoiceKind.LOCK_SWEEP,
                        bassRoot + 12.0,
                        bassRoot + 31.0,
                        SWEEP_AMP * 1.32,
                        0.85,
                        0.0));
            }

            // 5. Cooldown Camera Glide Shimmer (when no nodes executed but camera is zooming/panning into rest)
            if (!hasActivity && !Double.isNaN(previousZoom)) {
                final double cameraDelta = Math.abs(current.zoom() - previousZoom) * 18.0
                        + (Math.abs(current.tx() - previousTx) + Math.abs(current.ty() - previousTy)) / 180.0;
                if (cameraDelta > 0.08 && 0 == (frameIndex % 2)) {
                    final double glidePitch = (bassRoot + 24.0) + chordIntervals[frameIndex % chordIntervals.length];
                    frameEvents.add(new NeonEvent(
                            frameStart,
                            NeonSynth.millisToSamples(240),
                            VoiceKind.CYAN_BELL,
                            glidePitch,
                            NOTICE_AMP * Math.min(0.62, 0.25 + 0.30 * cameraDelta),
                            0.55,
                            ((frameIndex % 4) < 2) ? -0.35 : 0.35));
                }
            }

            // 6. Finale Swell: Wide Neon Sunrise F#maj9 (clean) or Brooding F#m(add9) (failures present)
            if (isFinalFrame) {
                final int[] finaleNotes = hadRawFailure ? DARK_FINALE_MIDI : SUNRISE_FINALE_MIDI;
                final double finaleBright = hadRawFailure ? 0.32 : 0.84;
                for (int f = 0; f < finaleNotes.length; f++) {
                    final double pan = -0.68 + (1.36 * f / Math.max(1, finaleNotes.length - 1));
                    frameEvents.add(new NeonEvent(
                            frameStart + (long) f * NeonSynth.millisToSamples(28),
                            finaleDuration,
                            VoiceKind.SUPERSAW_PAD,
                            finaleNotes[f],
                            FINALE_AMP,
                            finaleBright,
                            pan));
                }
                if (!hadRawFailure) {
                    // Celebratory high octave sunrise sparkle
                    frameEvents.add(new NeonEvent(
                            frameStart + NeonSynth.millisToSamples(120),
                            NeonSynth.millisToSamples(820),
                            VoiceKind.NEON_SHIMMER,
                            82.0, // A#5 (radiant major 3rd crown)
                            SHIMMER_AMP * 0.82,
                            0.90,
                            0.0));
                }
            }

            if (!frameEvents.isEmpty()) {
                eventsByFrame.put(frameIndex, frameEvents);
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

        return eventsByFrame;
    }

    static double resolveNeonPitch(
            final int bassRoot,
            final int[] chordIntervals,
            final NodeState node,
            final double rowRatio) {
        final int pathHash = Math.abs(stableHash(node.path()));
        final int degree = (node.col() * 2 + (int) Math.round(rowRatio * 4.0) + (pathHash % 3)) % NEON_MELODY_SCALE.length;
        final int chordAnchor = chordIntervals[(node.col() + pathHash) % chordIntervals.length];
        final int rawMidi = (0 == (pathHash % 3))
                ? (bassRoot + 12 + chordAnchor)
                : (54 + NEON_MELODY_SCALE[degree]);
        // Keep melodies in an ergonomic synthwave register [49..73] (C#3..C#5, lowered one octave)
        int midi = rawMidi;
        while (midi < MIN_MELODY_MIDI) midi += 12;
        while (midi > MAX_MELODY_MIDI) midi -= 12;
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
        final double normalized = Math.clamp((double) col / span, 0.0, 1.0);
        return clampPan(normalized * 1.56 - 0.78);
    }

    private static double clampPan(final double pan) {
        return Math.clamp(pan, -0.86, 0.86);
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
            return Math.clamp((double) state.coveredLeaves() / state.executableLeaves(), 0.0, 1.0);
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

    private static int statusSeverity(final char status) {
        return switch (status) {
            case 'R' -> 6;
            case 'F' -> 5;
            case 'U' -> 4;
            case 'M' -> 3;
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
            final String baseKey = s(col, ":", c.cleanName);
            final int dup = dupCounter.getOrDefault(baseKey, 0);
            dupCounter.put(baseKey, dup + 1);
            final String path = (0 == dup) ? baseKey : s(baseKey, "#", dup);
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
        int start = i;
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

    record NodeState(
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
