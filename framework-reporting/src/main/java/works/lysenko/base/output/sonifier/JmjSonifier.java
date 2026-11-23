package works.lysenko.base.output.sonifier;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.sonifier.JmjSynth.JmjEvent;
import works.lysenko.base.output.sonifier.JmjSynth.VoiceKind;
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
 * Cosmic 1970s French analog synthesizer sonifier ({@code jmj}), crafted in tribute to
 * Jean-Michel Jarre's early classic albums (<i>Oxygène</i>, <i>Équinoxe</i>, <i>Les Chants Magnétiques</i>).
 * <p>
 * Rooted in classic <b>C minor</b> ({@code Cm9 → Fm9 → Abmaj7 → G7sus4}, darkening into
 * {@code C Phrygian / G7b9} tension under raw regressions and resolving into a breathing
 * phased {@code Cmaj9} sunrise on clean completion):
 * <ul>
 *     <li><b>Phased Eminent 310 String Ensemble</b>: Lush divide-down string chords swept through
 *     a stereo 4-stage Small Stone all-pass phaser across the 4-bar cosmic progression.</li>
 *     <li><b>Hypnotic Matrisequencer Bass &amp; MiniPops-7 Groove</b>: Octave-and-fifth analog
 *     sequencer ostinati paired with woody vintage Korg MiniPops-7 kicks and sandy maraca ticks.</li>
 *     <li><b>EMS VCS3 Droplets &amp; Singing Oxygène Lead</b>: Partial leaf hits emit resonant
 *     VCS3 cosmic water-drops into a Roland RE-201 Space Echo stereo ping-pong tape delay; newly
 *     completed leaves sing lyrical 3-note vibrato lead phrases across the stereo horizon.</li>
 *     <li><b>Analog Telemetry Status Semantics</b>: Airy RMI glass harmonics for {@code NOTICE},
 *     solemn detuned analog horns for {@code WARNING}, and vintage VCS3 ring-modulated dissonant
 *     alarms + thunder rumble for raw {@code FAILED} regressions (muted &amp; cooldown-gated for known issues).</li>
 *     <li><b>Cosmic Disappearance &amp; Oxygène Wind Compaction</b>: Hidden/pruned leaves gently
 *     sink and fade down in a low warm analog sigh from their stereo column, while post-test
 *     column-bank compaction ({@code .tree.allow.final.reordering}) sweeps filtered pink-noise
 *     space wind through the phaser.</li>
 * </ul>
 */
public final class JmjSonifier implements TreeSonifier {

    /**
     * Configuration property value activating the JMJ sonifier.
     */
    public static final String MODE = "jmj";

    // 4-bar Oxygène / Équinoxe cosmic cycle in C minor:
    // Bar 0: Cm9 (C2=36), Bar 1: Fm9 (F2=41), Bar 2: Abmaj7 (Ab2=44), Bar 3: G7sus4 (G2=43)
    private static final int[] BASS_ROOTS_CLEAN = {36, 41, 44, 43};
    private static final int[] BASS_ROOTS_TENSE = {36, 37, 41, 43}; // Bar 1 shifts to Db (Phrygian b2 darkness)
    private static final int[][] CHORD_INTERVALS_CLEAN = {
            {0, 3, 7, 10, 14}, // Cm9
            {0, 3, 7, 10, 14}, // Fm9
            {0, 4, 7, 11, 14}, // Abmaj9
            {0, 5, 7, 10, 14}  // G9sus4
    };
    private static final int[][] CHORD_INTERVALS_TENSE = {
            {0, 3, 7, 10, 14}, // Cm9
            {0, 4, 7, 11, 18}, // Dbmaj7(#11)
            {0, 3, 7, 10, 14}, // Fm9
            {0, 4, 7, 10, 13}  // G7(b9) pull back to Cm
    };

    // Hypnotic 8-step Matrisequencer / ARP bass ostinato pattern (semitone offsets from bar root)
    private static final int[] SEQ_BASS_PATTERN = {0, 12, 7, 12, 0, 12, 10, 7};

    // C natural minor / melodic cosmic lead scale (2 octaves from C4 = 60)
    private static final int[] COSMIC_MELODY_SCALE = {0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 19, 22};

    // Clean cosmic sunrise finale: Cmaj9 (C3=48, G3=55, D4=62, E4=64, B4=71)
    private static final int[] SUNRISE_FINALE_MIDI = {48, 55, 62, 64, 71};
    // Wistful regression finale: Cm(add9) (C3=48, G3=55, D4=62, Eb4=63, G4=67)
    private static final int[] DARK_FINALE_MIDI = {48, 55, 62, 63, 67};

    private static final int CHORD_BAR_FRAMES = 8;
    private static final int KNOWN_ISSUE_COOLDOWN_FRAMES = 4;

    private static final double STRING_VOICE_AMP = 0.062;
    private static final double SEQ_BASS_AMP = 0.140;
    private static final double KICK_AMP = 0.165;
    private static final double TICK_AMP = 0.080;
    private static final double ARP_AMP = 0.118;
    private static final double DROP_AMP = 0.128;
    private static final double LEAD_AMP = 0.132;
    private static final double NOTICE_AMP = 0.105;
    private static final double WARNING_RAW_AMP = 0.128;
    private static final double WARNING_KNOWN_AMP = 0.064;
    private static final double FAILURE_RAW_AMP = 0.255;
    private static final double FAILURE_KNOWN_AMP = 0.100;
    private static final double WIND_AMP = 0.115;
    private static final double FADE_AMP = 0.102;
    private static final double FINALE_AMP = 0.098;

    /**
     * Resolves the sidecar snapshot file corresponding to a rendered progression frame.
     *
     * @param frame rendered progression frame ({@code .svg} or {@code .png})
     * @return sidecar {@code .jmj} file inside {@code jmj_events/}
     */
    public static File snapshotFile(final File frame) {
        return new File(new File(frame.getParentFile(), "jmj_events"),
                frame.getName().replaceFirst("\\.(png|svg)$", ".jmj"));
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
        return !frames.isEmpty() && frames.stream().allMatch(JmjSonifier::hasReadableFrame);
    }

    @Override
    public void writeWav(final List<File> frames, final File output, final int frameRate) throws IOException {
        if (frames.isEmpty() || frameRate < 1) {
            throw new IllegalArgumentException("Audio requires frames and a positive frame rate");
        }
        if (JmjSynth.SAMPLE_RATE % frameRate != 0) {
            throw new IllegalArgumentException("Frame rate must divide the audio sample rate");
        }
        final int samplesPerFrame = JmjSynth.SAMPLE_RATE / frameRate;
        final long totalSamples = (long) frames.size() * samplesPerFrame + JmjSynth.TAIL_SAMPLES;
        final Map<Integer, List<JmjEvent>> events = composeJmjEvents(frames, samplesPerFrame);
        JmjSynth.writeStereoWav(output, totalSamples, samplesPerFrame, events);
    }

    private static boolean hasReadableFrame(final File frame) {
        if (snapshotFile(frame).isFile()) {
            return true;
        }
        return frame.isFile()
                && frame.getName().toLowerCase(Locale.ROOT).endsWith(".svg")
                && frame.length() > 0L;
    }

    private static Map<Integer, List<JmjEvent>> composeJmjEvents(
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

        final Map<Integer, List<JmjEvent>> eventsByFrame = new HashMap<>();
        final Map<String, NodeState> previousNodes = new HashMap<>();

        int previousBar = -1;
        int previousNodeCount = -1;
        int previousFrameMaxCol = -1;
        int previousTestCycle = -1;
        int seqStep = 0;
        int lastKnownWarnFrame = -KNOWN_ISSUE_COOLDOWN_FRAMES;
        int lastKnownFailFrame = -KNOWN_ISSUE_COOLDOWN_FRAMES;
        double previousZoom = Double.NaN;
        double previousTx = Double.NaN;
        double previousTy = Double.NaN;
        double smoothedVelocity = 0.22;
        boolean hadRawFailure = false;

        final int stringDuration = JmjSynth.millisToSamples(1_380);
        final int bassDuration = Math.max(JmjSynth.millisToSamples(125), (int) (samplesPerFrame * 0.88));
        final int kickDuration = JmjSynth.millisToSamples(150);
        final int tickDuration = JmjSynth.millisToSamples(58);
        final int arpDuration = JmjSynth.millisToSamples(165);
        final int dropDuration = JmjSynth.millisToSamples(230);
        final int leadDuration = JmjSynth.millisToSamples(540);
        final int noticeDuration = JmjSynth.millisToSamples(380);
        final int warningDuration = JmjSynth.millisToSamples(380);
        final int failureDuration = JmjSynth.millisToSamples(540);
        final int windDuration = JmjSynth.millisToSamples(780);
        final int fadeDuration = JmjSynth.millisToSamples(420);
        final int finaleDuration = JmjSynth.millisToSamples(1_550);

        for (int frameIndex = 0; frameIndex < states.size(); frameIndex++) {
            final FrameState current = states.get(frameIndex);
            final long frameStart = (long) frameIndex * samplesPerFrame;
            final List<JmjEvent> frameEvents = new ArrayList<>();

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
                if ('F' == node.status() && !node.knownIssue()) {
                    hadRawFailure = true;
                }
            }

            final boolean hasActivity = !deltas.isEmpty();
            final double instantVelocity = Math.min(1.0, totalExecDelta / 6.0);
            smoothedVelocity = 0.65 * smoothedVelocity + 0.35 * instantVelocity;

            final int barIndex = (frameIndex / CHORD_BAR_FRAMES) % 4;
            final int bassRoot = hadRawFailure ? BASS_ROOTS_TENSE[barIndex] : BASS_ROOTS_CLEAN[barIndex];
            final int[] chordIntervals = hadRawFailure ? CHORD_INTERVALS_TENSE[barIndex] : CHORD_INTERVALS_CLEAN[barIndex];
            final boolean isFinalFrame = (frameIndex == states.size() - 1);

            // 1. Opening Oxygène Cosmic Wind Breath + Phased Eminent 310 String Ensemble Bed
            if (0 == frameIndex) {
                frameEvents.add(new JmjEvent(
                        frameStart,
                        windDuration,
                        VoiceKind.WIND_SWELL,
                        50.0,
                        69.0,
                        WIND_AMP * 0.78,
                        0.52,
                        -0.22));
            }
            if (barIndex != previousBar || 0 == frameIndex) {
                previousBar = barIndex;
                final int padBase = bassRoot + 12;
                final double stringBright = 0.28 + 0.38 * coverage + 0.16 * smoothedVelocity;
                final int[] chordVoicing = {chordIntervals[0], chordIntervals[1], chordIntervals[2], chordIntervals[4]};
                for (int v = 0; v < chordVoicing.length; v++) {
                    final double voicePan = -0.46 + 0.30 * v;
                    frameEvents.add(new JmjEvent(
                            frameStart + (long) v * (samplesPerFrame / 14),
                            stringDuration,
                            VoiceKind.EMINENT_STRINGS,
                            padBase + chordVoicing[v],
                            STRING_VOICE_AMP,
                            stringBright,
                            voicePan));
                }
            }

            // 2. Hypnotic Matrisequencer Bass Ostinato & Korg MiniPops-7 Vintage Rhythm
            final boolean newCycle = (current.testCycle() > 0 && current.testCycle() != previousTestCycle);
            if (newCycle || 0 == frameIndex || (hasActivity && 0 == (frameIndex % 4))) {
                frameEvents.add(new JmjEvent(
                        frameStart,
                        kickDuration,
                        VoiceKind.MINIPOPS_KICK,
                        48.0,
                        KICK_AMP * (newCycle ? 1.0 : 0.78),
                        0.45,
                        -0.06));
            }
            if (hasActivity && 1 == (frameIndex % 2)) {
                // Classic MiniPops-7 off-beat sandy maraca tick
                frameEvents.add(new JmjEvent(
                        frameStart,
                        tickDuration,
                        VoiceKind.MINIPOPS_TICK,
                        72.0,
                        TICK_AMP * 0.72,
                        0.48 + 0.25 * smoothedVelocity,
                        0.28));
            }
            if (hasActivity || 0 == (frameIndex % 2)) {
                final int stepOffset = SEQ_BASS_PATTERN[seqStep % SEQ_BASS_PATTERN.length];
                seqStep++;
                final double bassBright = hasActivity
                        ? (0.28 + 0.50 * smoothedVelocity)
                        : (0.14 + 0.16 * coverage);
                final double bassGain = hasActivity ? SEQ_BASS_AMP : (SEQ_BASS_AMP * 0.54);
                final double bassPan = (0 == (seqStep % 2)) ? -0.14 : 0.14;
                frameEvents.add(new JmjEvent(
                        frameStart,
                        bassDuration,
                        VoiceKind.SEQ_BASS,
                        bassRoot + stepOffset,
                        bassGain,
                        bassBright,
                        bassPan));

                // Mid-frame 16th sequencer pulse on energetic frames
                if (hasActivity && samplesPerFrame >= JmjSynth.millisToSamples(70)) {
                    final int subOffset = SEQ_BASS_PATTERN[seqStep % SEQ_BASS_PATTERN.length];
                    seqStep++;
                    frameEvents.add(new JmjEvent(
                            frameStart + (samplesPerFrame / 2),
                            Math.max(JmjSynth.millisToSamples(72), samplesPerFrame / 2),
                            VoiceKind.SEQ_BASS,
                            bassRoot + subOffset,
                            bassGain * 0.76,
                            bassBright * 0.88,
                            -bassPan));
                }
            }

            // 3. Branch Sequencer Notes, VCS3 Droplets, Singing Oxygène Leads & Semantic Statuses
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
                final double melodyMidi = resolveCosmicPitch(bassRoot, chordIntervals, node, rowRatio);

                final boolean isLeaf = ('L' == node.type() || 'M' == node.type());
                if (delta.execDelta() > 0) {
                    if (isLeaf) {
                        final int target = Math.max(1, node.target());
                        if (delta.prevExec() < target && node.executions() >= target) {
                            // Newly covered leaf: lyrical singing Oxygène lead motif into Space Echo!
                            final int[] motif = {0, 3, 7};
                            final long noteStep = Math.max(JmjSynth.millisToSamples(38), samplesPerFrame / 4);
                            for (int m = 0; m < motif.length; m++) {
                                final double startPitch = (m == 0) ? (melodyMidi - 2.0) : (melodyMidi + motif[m - 1]);
                                final double targetPitch = melodyMidi + motif[m];
                                frameEvents.add(new JmjEvent(
                                        onset + m * noteStep,
                                        leadDuration,
                                        VoiceKind.OXYGENE_LEAD,
                                        startPitch,
                                        targetPitch,
                                        LEAD_AMP * (1.0 - 0.10 * m),
                                        0.56 + 0.32 * rowRatio,
                                        clampPan(pan + (m - 1) * 0.16)));
                            }
                        } else if (node.executions() > target) {
                            // Over-execution beyond target: delicate MiniPops-7 maraca tick
                            frameEvents.add(new JmjEvent(
                                    onset,
                                    tickDuration,
                                    VoiceKind.MINIPOPS_TICK,
                                    melodyMidi + 12.0,
                                    TICK_AMP,
                                    0.42 + 0.45 * rowRatio,
                                    pan));
                        } else {
                            // Partial leaf hit: iconic EMS VCS3 cosmic water-drop ping
                            final double glideDir = (0 == (node.executions() % 2)) ? 3.0 : -2.0;
                            frameEvents.add(new JmjEvent(
                                    onset,
                                    dropDuration,
                                    VoiceKind.VCS3_DROP,
                                    melodyMidi,
                                    melodyMidi + glideDir,
                                    DROP_AMP,
                                    0.60,
                                    pan));
                        }
                    } else {
                        // Branch node traversal: ARP 2600 sequencer pluck with familiarity damping
                        final double familiarity = 1.0 / (1.0 + 0.34 * Math.log1p(delta.prevExec()));
                        if (delta.prevExec() >= 6 && familiarity < 0.50) {
                            frameEvents.add(new JmjEvent(
                                    onset,
                                    tickDuration,
                                    VoiceKind.MINIPOPS_TICK,
                                    melodyMidi,
                                    TICK_AMP * 0.68,
                                    0.32 + 0.30 * rowRatio,
                                    pan));
                        } else {
                            frameEvents.add(new JmjEvent(
                                    onset,
                                    arpDuration,
                                    VoiceKind.ARP_SEQ_NOTE,
                                    melodyMidi - 12.0,
                                    ARP_AMP * Math.max(0.45, familiarity),
                                    0.36 + 0.48 * rowRatio,
                                    pan));
                        }
                    }
                }

                // Semantic status cues
                if (delta.newNotice()) {
                    frameEvents.add(new JmjEvent(
                            onset,
                            noticeDuration,
                            VoiceKind.GLASS_HARMONIC,
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
                                frameEvents.add(new JmjEvent(
                                        onset,
                                        warningDuration,
                                        VoiceKind.ANALOG_HORN,
                                        bassRoot + 19.0,
                                        WARNING_KNOWN_AMP,
                                        0.24,
                                        pan));
                            }
                        } else {
                            frameEvents.add(new JmjEvent(
                                    onset,
                                    warningDuration,
                                    VoiceKind.ANALOG_HORN,
                                    bassRoot + 22.0,
                                    WARNING_RAW_AMP,
                                    0.72,
                                    pan));
                        }
                    } else if ('F' == node.status()) {
                        if (node.knownIssue()) {
                            if (frameIndex - lastKnownFailFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                                lastKnownFailFrame = frameIndex;
                                frameEvents.add(new JmjEvent(
                                        onset,
                                        failureDuration,
                                        VoiceKind.VCS3_RING_ALARM,
                                        bassRoot + 12.0,
                                        FAILURE_KNOWN_AMP,
                                        0.22,
                                        pan));
                            }
                        } else {
                            frameEvents.add(new JmjEvent(
                                    onset,
                                    failureDuration,
                                    VoiceKind.VCS3_RING_ALARM,
                                    bassRoot + 13.0,
                                    FAILURE_RAW_AMP,
                                    0.88,
                                    pan * 0.5));
                        }
                    }
                }
            }

            // 4. Leaf Disappearance (Warm Low Fade-Down) & Post-Test Bank Compaction Space Wind
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
                final double fadeIntensity = Math.min(1.0, removed / 8.0);
                final double vanishPan = (vanishedCount > 0)
                        ? clampPan((Math.max(0.0, Math.min(1.0, (vanishedColSum / vanishedCount) / panSpan))) * 1.56 - 0.78)
                        : 0.0;
                frameEvents.add(new JmjEvent(
                        frameStart,
                        fadeDuration,
                        VoiceKind.COSMIC_FADE,
                        bassRoot + 12.0,
                        bassRoot,
                        FADE_AMP * (0.62 + 0.32 * fadeIntensity),
                        0.32,
                        vanishPan));
            } else if (columnsCompacted) {
                // Signature Oxygène resonant pink-noise space wind sweep on final column-bank compaction
                frameEvents.add(new JmjEvent(
                        frameStart,
                        windDuration,
                        VoiceKind.WIND_SWELL,
                        bassRoot + 16.0,
                        bassRoot + 34.0,
                        WIND_AMP * 1.22,
                        0.76,
                        0.0));
            }

            // 5. Cooldown Camera Glide: Distant VCS3 Echo Droplets
            if (!hasActivity && !Double.isNaN(previousZoom)) {
                final double cameraDelta = Math.abs(current.zoom() - previousZoom) * 18.0
                        + (Math.abs(current.tx() - previousTx) + Math.abs(current.ty() - previousTy)) / 180.0;
                if (cameraDelta > 0.08 && 0 == (frameIndex % 2)) {
                    final double glidePitch = (bassRoot + 24.0) + chordIntervals[frameIndex % chordIntervals.length];
                    frameEvents.add(new JmjEvent(
                            frameStart,
                            dropDuration,
                            VoiceKind.VCS3_DROP,
                            glidePitch,
                            glidePitch + 2.0,
                            DROP_AMP * Math.min(0.55, 0.24 + 0.28 * cameraDelta),
                            0.50,
                            ((frameIndex % 4) < 2) ? -0.42 : 0.42));
                }
            }

            // 6. Cosmic Finale: Phased Eminent 310 Cmaj9 Sunrise (clean) or Wistful Cm(add9) (failures)
            if (isFinalFrame) {
                final int[] finaleNotes = hadRawFailure ? DARK_FINALE_MIDI : SUNRISE_FINALE_MIDI;
                final double finaleBright = hadRawFailure ? 0.34 : 0.80;
                for (int f = 0; f < finaleNotes.length; f++) {
                    final double pan = -0.66 + (1.32 * f / Math.max(1, finaleNotes.length - 1));
                    frameEvents.add(new JmjEvent(
                            frameStart + (long) f * JmjSynth.millisToSamples(34),
                            finaleDuration,
                            VoiceKind.EMINENT_STRINGS,
                            finaleNotes[f],
                            FINALE_AMP,
                            finaleBright,
                            pan));
                }
                if (!hadRawFailure) {
                    // Singing high Oxygène crown note drifting into Space Echo
                    frameEvents.add(new JmjEvent(
                            frameStart + JmjSynth.millisToSamples(140),
                            JmjSynth.millisToSamples(920),
                            VoiceKind.OXYGENE_LEAD,
                            76.0, // E5 -> G5 lyrical sunrise bend
                            79.0,
                            LEAD_AMP * 0.78,
                            0.78,
                            0.0));
                }
                // Soft cosmic wind exhale
                frameEvents.add(new JmjEvent(
                        frameStart + JmjSynth.millisToSamples(90),
                        JmjSynth.millisToSamples(1_150),
                        VoiceKind.WIND_SWELL,
                        66.0,
                        48.0,
                        WIND_AMP * 0.62,
                        0.42,
                        0.15));
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

    private static double resolveCosmicPitch(
            final int bassRoot,
            final int[] chordIntervals,
            final NodeState node,
            final double rowRatio) {
        final int pathHash = Math.abs(stableHash(node.path()));
        final int degree = (node.col() * 2 + (int) Math.round(rowRatio * 4.0) + (pathHash % 3)) % COSMIC_MELODY_SCALE.length;
        final int chordAnchor = chordIntervals[(node.col() + pathHash) % chordIntervals.length];
        final int rawMidi = (0 == (pathHash % 3))
                ? (bassRoot + 24 + chordAnchor)
                : (60 + COSMIC_MELODY_SCALE[degree]);
        // Keep melodies in a singing analog lead register [60..84] (C4..C6)
        int midi = rawMidi;
        while (midi < 60) midi += 12;
        while (midi > 84) midi -= 12;
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
        return clampPan(normalized * 1.56 - 0.78);
    }

    private static double clampPan(final double pan) {
        return Math.max(-0.86, Math.min(0.86, pan));
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
