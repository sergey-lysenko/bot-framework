package works.lysenko.base.output.sonifier;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.sonifier.EdmSynth.EdmEvent;
import works.lysenko.base.output.sonifier.EdmSynth.VoiceKind;
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
import java.util.*;

/**
 * Progressive House &amp; deadmau5-inspired telemetry sonifier ({@code edm}).
 * <p>
 * Translates test execution telemetry into a clean, driving, and harmonically rich progressive house
 * track clocked at 128 BPM. Key features:
 * <ul>
 *     <li><b>128 BPM Clock &amp; Groove:</b> Tight 4-on-the-floor kick, snappy layered claps on beats 2 &amp; 4,
 *     driving offbeat open hats, and syncopated 16th rolling bass.</li>
 *     <li><b>Signature deadmau5 Plucked Chords:</b> Syncopated 3-3-2 chord patterns in D minor
 *     (Dm9 &rarr; Bbmaj7 &rarr; Fmaj9 &rarr; Cadd9) with resonant 2-pole low-pass filters that
 *     dynamically sweep open as testing coverage advances from 0% to 100%.</li>
 *     <li><b>Kick-Driven Sidechain Compression:</b> Pumping sidechain ducking on chords and bass
 *     whenever the kick hits, providing that hypnotic rolling pulse without mud or fatigue.</li>
 *     <li><b>Harmonic Telemetry Mapping:</b> Node executions trigger clockwork arpeggio plucks panned
 *     spatially across the stereo field; completed leaves celebrate with a soaring high-register chime crown;
 *     warnings introduce warm amber brass tension; and regressions trigger a dramatic analog tape-stop dive.</li>
 *     <li><b>5-State Verdict Outro:</b> The final 2.2 seconds resolve into an unmistakable musical cadence
 *     conveying the exact test outcome purely by ear: <i>Spotless 100% Pass</i>, <i>Clean Pass with Known Issues</i>,
 *     <i>Passed with Raw Warnings</i>, <i>Incomplete Coverage</i>, or <i>Raw Failure Regression</i>.</li>
 * </ul>
 */
public final class EdmSonifier implements TreeSonifier {

    /**
     * Configuration property value activating the EDM sonifier.
     */
    public static final String MODE = "edm";

    private static final int STEPS_PER_BAR = 16;

    // 4-bar progressive house chord cycles in D minor (roots: D2=38, Bb1=34, F2=41, C2=36)
    private static final int[] SUB_ROOTS_CLEAN = {38, 34, 41, 36};
    private static final int[] SUB_ROOTS_WARN = {38, 34, 31, 33}; // Dm, Bb, Gm, A
    private static final int[] SUB_ROOTS_FAIL = {38, 39, 34, 33}; // Dm, Eb(Phrygian b2), Bb, A

    private static final int[][] CHORD_INTERVALS_CLEAN = {
            {0, 3, 7, 10, 14}, // Dm9
            {0, 4, 7, 11, 14}, // Bbmaj9
            {0, 4, 7, 11, 14}, // Fmaj9
            {0, 4, 7, 10, 14}  // C9
    };
    private static final int[][] CHORD_INTERVALS_WARN = {
            {0, 3, 7, 10, 14}, // Dm9
            {0, 4, 7, 11, 14}, // Bbmaj9
            {0, 3, 7, 10, 14}, // Gm9
            {0, 4, 7, 10, 13}  // A7(b9)
    };
    private static final int[][] CHORD_INTERVALS_FAIL = {
            {0, 3, 7, 10, 14}, // Dm9
            {0, 4, 7, 11, 18}, // Ebmaj7(#11)
            {0, 4, 7, 14, 19}, // Bb(add9)
            {0, 4, 7, 10, 13}  // A7(b9)
    };

    // D minor pentatonic melodic pitches spanning a warm progressive-house mid register (G3=55 .. C5=72)
    private static final int[] EDM_MELODY_NOTES = {55, 57, 60, 62, 65, 67, 69, 72};
    private static final int MIN_MELODY_MIDI = 55; // G3 (~196 Hz)
    private static final int MAX_MELODY_MIDI = 72; // C5 (~523 Hz)

    // Amplitudes
    private static final double KICK_AMP = 0.58;
    private static final double CLAP_AMP = 0.44;
    private static final double OPEN_HAT_AMP = 0.28;
    private static final double CLOSED_HAT_AMP = 0.16;
    private static final double ROLLING_BASS_AMP = 0.45;
    private static final double CHORD_VOICE_AMP = 0.22;
    private static final double PLUCK_AMP = 0.32;
    private static final double CHIME_CROWN_AMP = 0.42;
    private static final double NOTICE_AMP = 0.36;
    private static final double WARNING_RAW_AMP = 0.48;
    private static final double WARNING_KNOWN_AMP = 0.22;
    private static final double UPSET_AMP = 0.35;
    private static final double FAILURE_RAW_AMP = 0.58;
    private static final double FAILURE_KNOWN_AMP = 0.28;
    private static final double RISER_AMP = 0.38;
    private static final double FADE_AMP = 0.26;

    private static final int KNOWN_ISSUE_COOLDOWN_FRAMES = 12;
    private static final int UPSET_COOLDOWN_FRAMES = 8;

    /**
     * Resolves the sidecar snapshot file corresponding to a rendered progression frame.
     *
     * @param frame rendered progression frame
     * @return sidecar {@code .edm} file
     */
    public static File snapshotFile(final File frame) {
        return new File(new File(frame.getParentFile(), "edm_events"),
                frame.getName().replaceFirst("\\.(png|svg)$", ".edm"));
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
        return !frames.isEmpty() && frames.stream().allMatch(EdmSonifier::hasReadableFrame);
    }

    @Override
    public void writeWav(final List<File> frames, final File output, final int frameRate) throws IOException {
        if (frames.isEmpty() || frameRate < 1) {
            throw new IllegalArgumentException("Audio requires frames and a positive frame rate");
        }
        if (EdmSynth.SAMPLE_RATE % frameRate != 0) {
            throw new IllegalArgumentException("Frame rate must divide the audio sample rate");
        }
        final int samplesPerFrame = EdmSynth.SAMPLE_RATE / frameRate;
        final long totalSamples = (long) frames.size() * samplesPerFrame + EdmSynth.TAIL_SAMPLES;
        final Map<Integer, List<EdmEvent>> events = composeEdmEvents(frames, samplesPerFrame);
        EdmSynth.writeStereoWav(output, totalSamples, samplesPerFrame, events);
    }

    private static boolean hasReadableFrame(final File frame) {
        if (snapshotFile(frame).isFile()) {
            return true;
        }
        return frame.isFile()
                && frame.getName().toLowerCase(Locale.ROOT).endsWith(".svg")
                && frame.length() > 0L;
    }

    static Map<Integer, List<EdmEvent>> composeEdmEvents(
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

        final Map<Integer, List<EdmEvent>> eventsByFrame = new HashMap<>();
        final int frameCount = states.size();
        final long contentSamples = (long) frameCount * samplesPerFrame;

        final boolean[] frameActive = new boolean[frameCount];
        final double[] frameVelocity = new double[frameCount];
        final double[] frameCoverage = new double[frameCount];
        final int[] frameMood = new int[frameCount]; // 0=clean, 1=warn, 2=fail

        final Map<String, NodeState> previousNodes = new HashMap<>();
        int previousNodeCount = -1;
        int previousFrameMaxCol = -1;
        int lastKnownWarnFrame = -KNOWN_ISSUE_COOLDOWN_FRAMES;
        int lastKnownFailFrame = -KNOWN_ISSUE_COOLDOWN_FRAMES;
        int lastUpsetFrame = -UPSET_COOLDOWN_FRAMES;
        int lastMilestoneBucket = 0;
        double smoothedVelocity = 0.25;

        boolean hadRawFailure = false;
        boolean hadRawWarning = false;
        boolean hadReproducedKnownIssue = false;

        final int pluckDuration = EdmSynth.millisToSamples(180);
        final int crownDuration = EdmSynth.millisToSamples(580);
        final int noticeDuration = EdmSynth.millisToSamples(320);
        final int warningDuration = EdmSynth.millisToSamples(420);
        final int upsetDuration = EdmSynth.millisToSamples(320);
        final int failureDuration = EdmSynth.millisToSamples(680);
        final int riserDuration = EdmSynth.millisToSamples(850);
        final int fadeDuration = EdmSynth.millisToSamples(480);

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

            final boolean hasActivity = !deltas.isEmpty();
            final double instantVelocity = Math.min(1.0, totalExecDelta / 6.0);
            smoothedVelocity = 0.65 * smoothedVelocity + 0.35 * instantVelocity;

            final int mood = hadRawFailure ? 2 : (hadRawWarning ? 1 : 0);
            frameActive[frameIndex] = hasActivity;
            frameVelocity[frameIndex] = smoothedVelocity;
            frameCoverage[frameIndex] = coverage;
            frameMood[frameIndex] = mood;

            final int barIndex = (int) ((frameStart / (long) EdmSynth.BAR_SAMPLES) % 4L);
            final int subRoot = resolveSubRoot(mood, barIndex);
            final int[] chordIntervals = resolveChordIntervals(mood, barIndex);

            // Coverage milestone sweeps (50% and 100%)
            final int milestoneBucket = (coverage >= 0.999) ? 2 : ((coverage >= 0.50) ? 1 : 0);
            if (milestoneBucket > lastMilestoneBucket && frameIndex > 0) {
                lastMilestoneBucket = milestoneBucket;
                final double sweepAmp = (2 == milestoneBucket) ? (RISER_AMP * 1.25) : RISER_AMP;
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        frameStart,
                        riserDuration,
                        VoiceKind.NOISE_RISER,
                        220.0,
                        (2 == milestoneBucket) ? 6_500.0 : 4_500.0,
                        sweepAmp,
                        0.85,
                        0.0));
            }

            // Node execution & status events
            deltas.sort(Comparator.comparingInt((NodeDelta d) -> d.node().col())
                    .thenComparingInt(d -> d.node().rowTenths())
                    .thenComparing(d -> d.node().path()));

            final int maxVoices = Math.min(deltas.size(), 6);
            final long staggerStep = (maxVoices > 1)
                    ? Math.max(1L, (samplesPerFrame * 2L) / (3L * maxVoices))
                    : 0L;

            for (int i = 0; i < maxVoices; i++) {
                final NodeDelta delta = deltas.get(i);
                final NodeState node = delta.node();
                final long onset = frameStart + i * staggerStep;
                final double pan = computeStereoPan(node.col(), panSpan);
                final double rowRatio = 1.0 - Math.min(1.0, (double) node.rowTenths() / Math.max(10, globalMaxRowTenths));
                final double melodyMidi = resolveEdmPitch(subRoot, chordIntervals, node, rowRatio);

                final boolean isLeaf = ('L' == node.type() || 'M' == node.type());
                if (delta.execDelta() > 0) {
                    if (isLeaf) {
                        final int target = Math.max(1, node.target());
                        if (delta.prevExec() < target && node.executions() >= target) {
                            // Completed leaf: glorious high-octave chime crown (D5/F5/A5/C6)
                            final int[] flourish = {0, 7, 12, 14};
                            final long flourishStep = Math.max(EdmSynth.millisToSamples(35), samplesPerFrame / 4);
                            for (int f = 0; f < flourish.length; f++) {
                                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                                        onset + f * flourishStep,
                                        crownDuration,
                                        VoiceKind.CHIME_CROWN,
                                        melodyMidi + flourish[f],
                                        CHIME_CROWN_AMP * (1.0 - 0.10 * f),
                                        0.65 + 0.30 * rowRatio,
                                        clampPan(pan + (f - 1.5) * 0.12)));
                            }
                        } else {
                            // In-progress / repeated leaf execution: snappy deadmau5 arp pluck
                            scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                                    onset,
                                    pluckDuration,
                                    VoiceKind.PLUCK_LEAD,
                                    melodyMidi,
                                    PLUCK_AMP,
                                    0.45 + 0.40 * rowRatio,
                                    pan));
                        }
                    } else {
                        // Branch traversal
                        scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                                onset,
                                pluckDuration,
                                VoiceKind.PLUCK_LEAD,
                                melodyMidi - 12.0,
                                PLUCK_AMP * 0.85,
                                0.35 + 0.35 * rowRatio,
                                pan));
                    }
                }

                // Status cues
                if (delta.newNotice()) {
                    scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                            onset,
                            noticeDuration,
                            VoiceKind.CYAN_NOTICE,
                            melodyMidi + 7.0,
                            NOTICE_AMP,
                            0.70,
                            pan));
                }

                if (delta.statusWorsened()) {
                    if ('W' == node.status()) {
                        if (node.knownIssue()) {
                            if (frameIndex - lastKnownWarnFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                                lastKnownWarnFrame = frameIndex;
                                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                                        onset,
                                        warningDuration,
                                        VoiceKind.AMBER_BRASS,
                                        subRoot + 24.0 + 7.0,
                                        WARNING_KNOWN_AMP,
                                        0.25,
                                        pan));
                            }
                        } else {
                            scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                                    onset,
                                    warningDuration,
                                    VoiceKind.AMBER_BRASS,
                                    subRoot + 24.0 + 10.0,
                                    WARNING_RAW_AMP,
                                    0.75,
                                    pan));
                        }
                    } else if ('U' == node.status()) {
                        if (frameIndex - lastUpsetFrame >= UPSET_COOLDOWN_FRAMES) {
                            lastUpsetFrame = frameIndex;
                            scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                                    onset,
                                    upsetDuration,
                                    VoiceKind.UPSET_DIVE,
                                    melodyMidi,
                                    melodyMidi - 3.0,
                                    UPSET_AMP,
                                    0.50,
                                    pan));
                        }
                    } else if ('F' == node.status()) {
                        if (node.knownIssue()) {
                            if (frameIndex - lastKnownFailFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                                lastKnownFailFrame = frameIndex;
                                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                                        onset,
                                        failureDuration,
                                        VoiceKind.FAULT_DROP,
                                        subRoot + 12.0,
                                        subRoot + 5.0,
                                        FAILURE_KNOWN_AMP,
                                        0.30,
                                        pan * 0.5));
                            }
                        } else {
                            // Raw regression: dramatic analog tape stop dive
                            scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                                    onset,
                                    failureDuration,
                                    VoiceKind.FAULT_DROP,
                                    subRoot + 18.0,
                                    subRoot + 4.0,
                                    FAILURE_RAW_AMP,
                                    0.90,
                                    pan * 0.3));
                        }
                    }
                }
            }

            // Pruned / hidden leaves & compaction sweeps
            final int currentNodeCount = current.nodes().size();
            int vanishedCount = 0;
            if (!previousNodes.isEmpty()) {
                for (final String prevKey : previousNodes.keySet()) {
                    if (!current.nodes().containsKey(prevKey)) {
                        vanishedCount++;
                    }
                }
            }
            final boolean prunedMass = (vanishedCount > 0)
                    || (previousNodeCount > 0 && currentNodeCount < previousNodeCount);
            final boolean columnsCompacted = (previousFrameMaxCol > 0 && frameMaxCol < previousFrameMaxCol);

            if (prunedMass) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        frameStart,
                        fadeDuration,
                        VoiceKind.FILTER_FADE,
                        1200.0,
                        80.0,
                        FADE_AMP,
                        0.40,
                        0.0));
            } else if (columnsCompacted) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        frameStart,
                        riserDuration,
                        VoiceKind.NOISE_RISER,
                        300.0,
                        5_500.0,
                        RISER_AMP * 1.10,
                        0.80,
                        0.0));
            }

            previousNodes.clear();
            previousNodes.putAll(current.nodes());
            previousNodeCount = currentNodeCount;
            previousFrameMaxCol = frameMaxCol;
        }

        // Pass 2: Progressive House 128 BPM continuous groove engine
        composeEdmGroove(
                eventsByFrame,
                samplesPerFrame,
                contentSamples,
                frameActive,
                frameVelocity,
                frameCoverage,
                frameMood);

        // Pass 3: 5-state final verdict outro
        final FrameState finalState = states.get(frameCount - 1);
        final double finalCoverage = computeCoverage(finalState);
        final boolean fullCoverage = (finalCoverage >= 0.999);
        final long outroStart = contentSamples;

        composeOutro(
                eventsByFrame,
                samplesPerFrame,
                outroStart,
                hadRawFailure,
                hadRawWarning,
                hadReproducedKnownIssue,
                fullCoverage);

        return eventsByFrame;
    }

    private static void composeEdmGroove(
            final Map<Integer, List<EdmEvent>> eventsByFrame,
            final int samplesPerFrame,
            final long contentSamples,
            final boolean[] frameActive,
            final double[] frameVelocity,
            final double[] frameCoverage,
            final int[] frameMood) {

        final int kickDur = EdmSynth.millisToSamples(170);
        final int clapDur = EdmSynth.millisToSamples(180);
        final int openHatDur = EdmSynth.millisToSamples(140);
        final int closedHatDur = EdmSynth.millisToSamples(40);
        final int bassDur = EdmSynth.millisToSamples(115);
        final int chordDur = EdmSynth.millisToSamples(340);

        final int totalSteps = Math.max(1, (int) ((contentSamples - 1L) / EdmSynth.STEP_SAMPLES) + 1);
        final int maxFrameIdx = frameActive.length - 1;

        for (int step = 0; step < totalSteps; step++) {
            final long baseSample = (long) step * EdmSynth.STEP_SAMPLES;
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
            final int pos = step % STEPS_PER_BAR; // 0..15

            final int subRoot = resolveSubRoot(mood, barIndex);
            final int[] chordIntervals = resolveChordIntervals(mood, barIndex);

            // 1. Four-on-the-floor kick on beats 1, 2, 3, 4 (pos == 0, 4, 8, 12)
            if (0 == (pos % 4)) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        baseSample,
                        kickDur,
                        VoiceKind.EDM_KICK,
                        48.0,
                        KICK_AMP,
                        0.75,
                        0.0));
            }

            // 2. Layered clap on beats 2 and 4 (pos == 4, 12)
            if (4 == pos || 12 == pos) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        baseSample,
                        clapDur,
                        VoiceKind.EDM_CLAP,
                        220.0,
                        CLAP_AMP * (active ? 1.0 : 0.85),
                        0.65 + 0.25 * cov,
                        0.0));
            }

            // 3. Offbeat open hi-hat on the "&" of every beat (pos == 2, 6, 10, 14)
            if (2 == (pos % 4)) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        baseSample,
                        openHatDur,
                        VoiceKind.OPEN_HAT,
                        7_200.0,
                        OPEN_HAT_AMP * (active ? 1.0 : 0.75),
                        0.70 + 0.25 * cov,
                        0.08));
            }

            // 4. Ticking 16th closed hi-hat / shaker
            scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                    baseSample,
                    closedHatDur,
                    VoiceKind.CLOSED_HAT,
                    8_500.0,
                    CLOSED_HAT_AMP * (0 == (pos & 1) ? 0.90 : 0.60),
                    0.50,
                    -0.08));

            // 5. Signature deadmau5 syncopated plucked chords (3-3-2 groove: steps 0, 3, 6, 8, 11, 14)
            final boolean isChordStep = (0 == pos || 3 == pos || 6 == pos || 8 == pos || 11 == pos || 14 == pos);
            if (isChordStep) {
                final int chordBase = subRoot + 12; // D3/Bb2/F3/C3
                // Filter cutoff dynamically sweeps open as coverage and velocity rise
                final double chordBrightness = Math.clamp(0.12 + 0.72 * cov + 0.16 * vel, 0.10, 1.0);
                final int[] voicing = {chordIntervals[0], chordIntervals[1], chordIntervals[2], chordIntervals[3], chordIntervals[4]};

                for (int v = 0; v < voicing.length; v++) {
                    final double voicePan = -0.50 + 0.25 * v;
                    scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                            baseSample,
                            chordDur,
                            VoiceKind.MAU5_CHORD,
                            chordBase + voicing[v],
                            CHORD_VOICE_AMP * (active ? 1.0 : 0.80),
                            chordBrightness,
                            voicePan));
                }
            }

            // 6. 16th-note rolling bass (plays offbeat 16ths, ducked by kick)
            final boolean isBassStep = (2 == pos || 3 == pos || 6 == pos || 7 == pos || 10 == pos || 11 == pos || 14 == pos || 15 == pos);
            if (isBassStep) {
                final int bassNote = subRoot;
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        baseSample,
                        bassDur,
                        VoiceKind.ROLLING_BASS,
                        bassNote,
                        ROLLING_BASS_AMP * (active ? 1.0 : 0.85),
                        0.40 + 0.35 * cov,
                        0.0));
            }
        }
    }

    private static void composeOutro(
            final Map<Integer, List<EdmEvent>> eventsByFrame,
            final int samplesPerFrame,
            final long outroStart,
            final boolean hadRawFailure,
            final boolean hadRawWarning,
            final boolean hadReproducedKnownIssue,
            final boolean fullCoverage) {

        final int chordDur = EdmSynth.millisToSamples(1_600);
        final int chimeDur = EdmSynth.millisToSamples(1_400);

        if (hadRawFailure) {
            // Verdict 5: Raw Failure Regression (dark Dm(b9) + tape stop sub-drop)
            final int[] failChord = {38, 45, 50, 51, 57}; // D2, A2, D3, Eb3 (b9), A3
            for (final int pitch : failChord) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        outroStart,
                        chordDur,
                        VoiceKind.MAU5_CHORD,
                        pitch,
                        CHORD_VOICE_AMP * 1.25,
                        0.25,
                        0.0));
            }
            scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                    outroStart,
                    EdmSynth.millisToSamples(1_100),
                    VoiceKind.FAULT_DROP,
                    50.0,
                    28.0,
                    FAILURE_RAW_AMP * 1.15,
                    0.80,
                    0.0));

        } else if (hadRawWarning) {
            // Verdict 3: Passed with Raw Warnings (suspended Dm11 + amber brass)
            final int[] warnChord = {38, 45, 50, 53, 55, 60}; // Dm11
            for (final int pitch : warnChord) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        outroStart,
                        chordDur,
                        VoiceKind.MAU5_CHORD,
                        pitch,
                        CHORD_VOICE_AMP,
                        0.45,
                        0.0));
            }
            scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                    outroStart + EdmSynth.millisToSamples(80),
                    EdmSynth.millisToSamples(950),
                    VoiceKind.AMBER_BRASS,
                    62.0,
                    WARNING_RAW_AMP * 0.75,
                    0.40,
                    0.0));

        } else if (!fullCoverage) {
            // Verdict 4: Incomplete Coverage (open Dm9 half-cadence)
            final int[] incChord = {38, 50, 53, 57, 64}; // Dm9
            for (final int pitch : incChord) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        outroStart,
                        chordDur,
                        VoiceKind.MAU5_CHORD,
                        pitch,
                        CHORD_VOICE_AMP * 0.90,
                        0.50,
                        0.0));
            }

        } else if (hadReproducedKnownIssue) {
            // Verdict 2: Clean Pass with Known Issues (warm Fmaj9 + soft amber chime)
            final int[] knownChord = {41, 48, 53, 57, 60, 64}; // Fmaj9
            for (final int pitch : knownChord) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        outroStart,
                        chordDur,
                        VoiceKind.MAU5_CHORD,
                        pitch,
                        CHORD_VOICE_AMP,
                        0.65,
                        0.0));
            }
            scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                    outroStart + EdmSynth.millisToSamples(120),
                    chimeDur,
                    VoiceKind.CHIME_CROWN,
                    72.0,
                    CHIME_CROWN_AMP * 0.75,
                    0.60,
                    0.15));

        } else {
            // Verdict 1: Spotless 100% Pass (Strobe-style glorious Dm9 -> Fmaj9 -> Dsus2 with soaring chime arpeggio)
            final int[] passChord = {38, 45, 50, 57, 62, 64, 69}; // Dm9 wide
            for (final int pitch : passChord) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        outroStart,
                        chordDur,
                        VoiceKind.MAU5_CHORD,
                        pitch,
                        CHORD_VOICE_AMP * 1.10,
                        0.88,
                        0.0));
            }

            // Soaring Strobe-style arpeggio flourish (D5 - F5 - A5 - C6 - D6 - E6)
            final int[] flourish = {74, 77, 81, 84, 86, 88};
            final long step = EdmSynth.millisToSamples(85);
            for (int i = 0; i < flourish.length; i++) {
                final double pan = ((i & 1) == 0) ? -0.35 : 0.35;
                scheduleEvent(eventsByFrame, samplesPerFrame, new EdmEvent(
                        outroStart + i * step,
                        chimeDur,
                        VoiceKind.CHIME_CROWN,
                        flourish[i],
                        CHIME_CROWN_AMP * (0.95 - 0.05 * i),
                        0.85,
                        pan));
            }
        }
    }

    private static int resolveSubRoot(final int mood, final int barIndex) {
        return switch (mood) {
            case 2 -> SUB_ROOTS_FAIL[barIndex % SUB_ROOTS_FAIL.length];
            case 1 -> SUB_ROOTS_WARN[barIndex % SUB_ROOTS_WARN.length];
            default -> SUB_ROOTS_CLEAN[barIndex % SUB_ROOTS_CLEAN.length];
        };
    }

    private static int[] resolveChordIntervals(final int mood, final int barIndex) {
        return switch (mood) {
            case 2 -> CHORD_INTERVALS_FAIL[barIndex % CHORD_INTERVALS_FAIL.length];
            case 1 -> CHORD_INTERVALS_WARN[barIndex % CHORD_INTERVALS_WARN.length];
            default -> CHORD_INTERVALS_CLEAN[barIndex % CHORD_INTERVALS_CLEAN.length];
        };
    }

    static double resolveEdmPitch(
            final int subRoot,
            final int[] chordIntervals,
            final NodeState node,
            final double rowRatio) {
        final double clampedRow = Math.clamp(rowRatio, 0.0, 1.0);
        final int selector = Math.abs(node.col() * 3 + node.rowTenths());
        int midi;
        if (0 == (selector % 3)) {
            final int chordAnchor = chordIntervals[selector % chordIntervals.length];
            midi = subRoot + 12 + chordAnchor + ((clampedRow > 0.50) ? 12 : 0);
        } else {
            final int rowBias = (int) Math.round(clampedRow * 4.0);
            final int noteIdx = Math.min(EDM_MELODY_NOTES.length - 1, rowBias + (selector % 4));
            midi = EDM_MELODY_NOTES[noteIdx];
        }
        // Keep regular execution melodies in a warm progressive-house mid register [55..72] (G3..C5)
        while (midi < MIN_MELODY_MIDI) {
            midi += 12;
        }
        while (midi > MAX_MELODY_MIDI) {
            midi -= 12;
        }
        return midi;
    }

    private static double computeStereoPan(final int col, final int panSpan) {
        final double norm = Math.clamp((double) col / (double) panSpan, 0.0, 1.0);
        return clampPan(norm * 1.60 - 0.80);
    }

    private static double clampPan(final double pan) {
        return Math.clamp(pan, -0.92, 0.92);
    }

    private static int computeFrameMaxCol(final FrameState state) {
        int max = 0;
        for (final NodeState n : state.nodes().values()) {
            if (n.col() > max) max = n.col();
        }
        return max;
    }

    private static double computeCoverage(final FrameState state) {
        if (state.executableLeaves() <= 0) return 0.0;
        return Math.clamp((double) state.coveredLeaves() / (double) state.executableLeaves(), 0.0, 1.0);
    }

    private static void scheduleEvent(
            final Map<Integer, List<EdmEvent>> eventsByFrame,
            final int samplesPerFrame,
            final EdmEvent event) {
        final int frameIndex = (int) (event.startSample() / samplesPerFrame);
        eventsByFrame.computeIfAbsent(frameIndex, _ -> new ArrayList<>()).add(event);
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
        int executableLeaves = 0;
        int coveredLeaves = 0;

        try (BufferedReader reader = Files.newBufferedReader(snapshot.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while (null != (line = reader.readLine())) {
                if (line.isEmpty()) continue;
                if (line.startsWith("#meta\t")) {
                    final String[] parts = line.split("\t");
                    if (parts.length >= 6) {
                        executableLeaves = Integer.parseInt(parts[4]);
                        coveredLeaves = Integer.parseInt(parts[5]);
                    }
                    continue;
                }
                final String[] parts = line.split("\t");
                if (parts.length < 8) continue;
                final String path = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                final int col = Integer.parseInt(parts[1]);
                final int rowTenths = Integer.parseInt(parts[2]);
                final char type = parts[3].charAt(0);
                final int execs = Integer.parseInt(parts[4]);
                final int target = Integer.parseInt(parts[5]);
                final char status = parts[6].charAt(0);
                final boolean known = "1".equals(parts[7]);

                nodes.put(path, new NodeState(path, col, rowTenths, type, execs, target, status, known));
            }
        }
        return new FrameState(nodes, executableLeaves, coveredLeaves);
    }

    private static FrameState readSvgFallback(final File svgFile) throws IOException {
        final String content = Files.readString(svgFile.toPath(), StandardCharsets.UTF_8);

        int headerCovered = 0;
        int headerExecutable = 0;
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

        return new FrameState(nodes, Math.max(1, headerExecutable), Math.max(1, headerCovered));
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

    private static String lineagePath(final NodeData node) {
        return (null == node.parent()) ? node.id() : lineagePath(node.parent()) + "/" + node.id();
    }

    private static char resolveTypeCode(final NodeData node, final ScenarioType scenarioType, final boolean executable) {
        if (!node.children().isEmpty()) return 'B';
        if (ScenarioType.MONO == scenarioType) return 'M';
        if (TreeProgressMetrics.isTerminalScenario(node)) return executable ? 'L' : 'H';
        return 'X';
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

    private static int statusSeverity(final char status) {
        return switch (status) {
            case 'F' -> 4;
            case 'W' -> 3;
            case 'U' -> 2;
            case 'I' -> 1;
            default -> 0;
        };
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

    record FrameState(
            Map<String, NodeState> nodes,
            int executableLeaves,
            int coveredLeaves) {
    }

    record NodeDelta(
            NodeState node,
            int prevExec,
            int execDelta,
            boolean statusWorsened,
            boolean newNotice) {
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
