package works.lysenko.base.output.sonifier;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.sonifier.AdpSynth.AdpEvent;
import works.lysenko.base.output.sonifier.AdpSynth.VoiceKind;
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
 * Angine de Poitrine-inspired microtonal math-rock telemetry sonifier ({@code adp}).
 * <p>
 * Translates test execution telemetry into a hypnotic, angular, odd-meter "Dada Pythago-Cubist
 * Mantra-Rock" composition clocked at 136 BPM in 7/8 time and 24-tone equal temperament (24-TET):
 * <ul>
 *     <li><b>24-TET Quarter-Tone Fretting:</b> Built on G Bayati ({@code G, A&#9837;&frac12;, B&#9837;, C, D, E&#9837;&frac12;, F}),
 *     G Hijaz (warnings), and G Saba (failures), preserving Khn de Poitrine's signature "accessible
 *     dissonance" where harmonized guitar overdubs track quarter-tone roots at consonant 5ths and octaves.</li>
 *     <li><b>Odd-Meter 7/8 Groove &amp; Cloth-Dampened Kit:</b> Klek de Poitrine's restrained, pocket-driven
 *     4-piece acoustic kit (muffled single-pedal kick, woody piccolo snare with ghost notes, dampened
 *     toms, and dark extra-dry cymbals) locked to a crooked {@code 4 + 4 + 6} (2+2+3) pulse.</li>
 *     <li><b>Live Multi-Track Looper Buildup:</b> Independent P-bass and Stratocaster rigs stack
 *     interlocking melodic cells as test coverage rises from 0% to 100%, like live pedalboard looping.</li>
 *     <li><b>Semantic Rig Telemetry:</b> Regular executions pluck deep bass-guitar / low-baritone
 *     microtonal riffs and palm-muted branch chugs; completed leaves erupt in angular quarter-tone-to-fifth
 *     harmonized phrases; warnings engage Khn's foot-controlled ring modulator; and raw failures trigger a
 *     fuzzed microtonal meltdown.</li>
 *     <li><b>5-State Verdict Outro:</b> Resolves into an unmistakable live-band cadence conveying the
 *     exact test outcome purely by ear.</li>
 * </ul>
 */
public final class AdpSonifier implements TreeSonifier {

    /**
     * Configuration property value activating the Angine de Poitrine microtonal rock sonifier.
     */
    public static final String MODE = "adp";

    private static final int STEPS_PER_BAR = AdpSynth.STEPS_PER_BAR; // 14 steps (7/8)

    // 4-bar 24-TET microtonal Precision Bass cycles rooted on G1 (31.0)
    // Neutral 2nd = +1.5 semitones (A-half-flat = 32.5)
    private static final double[] BASS_ROOTS_CLEAN = {31.0, 31.0, 32.5, 36.0}; // G1, G1, Ab½1, C2
    private static final double[] BASS_ROOTS_WARN = {31.0, 32.5, 34.0, 37.5};  // G1, Ab½1, Bb1, C#½2
    private static final double[] BASS_ROOTS_FAIL = {31.0, 32.5, 34.0, 36.5};  // G1, Ab½1, Bb1, C+¼2

    // 24-TET microtonal scales spanning bass-guitar / low-baritone register [G2=43.0 .. Bb3=58.0]
    // Clean: G Bayati (neutral 2nd 44.5 & neutral 6th 51.5)
    private static final double[] SCALE_BAYATI = {43.0, 44.5, 46.0, 48.0, 50.0, 51.5, 53.0, 55.0, 56.5, 58.0};
    // Warning: G Hijaz (augmented 2nd + neutral 6th)
    private static final double[] SCALE_HIJAZ = {43.0, 44.0, 47.0, 48.0, 50.0, 51.5, 53.0, 55.0, 56.0, 58.0};
    // Failure: G Saba (crushed diminished 4th + quarter-flat 5th)
    private static final double[] SCALE_SABA = {43.0, 44.5, 46.0, 47.0, 49.5, 51.0, 53.0, 55.0, 56.5, 58.0};

    private static final double MIN_MELODY_MIDI = 43.0; // G2 (~98 Hz, upper bass guitar / low guitar string)
    private static final double MAX_MELODY_MIDI = 58.0; // Bb3 (~233 Hz)

    // Interlocking 7/8 (14-step) microtonal Stratocaster looper cell intervals (in quarter-tone-capable semitones)
    private static final double[] LOOP_RIFF_BAYATI = {0.0, 1.5, 5.0, 7.0, 3.0, 1.5};
    private static final double[] LOOP_RIFF_HIJAZ = {0.0, 1.0, 4.0, 7.0, 5.0, 1.0};
    private static final double[] LOOP_RIFF_SABA = {0.0, 1.5, 4.0, 6.5, 3.0, 1.5};

    // Dual-amp stereo placement (bass amp left, guitar amp right, dry drums tight center)
    private static final double BASS_AMP_PAN = -0.18;
    private static final double GUITAR_AMP_PAN = 0.26;
    private static final double HARMONY_LOOP_PAN = -0.34;
    private static final double MUTE_LOOP_PAN = 0.46;

    // Amplitudes
    private static final double KICK_AMP = 0.56;
    private static final double SNARE_AMP = 0.46;
    private static final double GHOST_SNARE_AMP = 0.16;
    private static final double TOM_AMP = 0.42;
    private static final double HAT_AMP = 0.24;
    private static final double RIDE_AMP = 0.28;
    private static final double BASS_AMP = 0.50;
    private static final double LOOP_RIFF_AMP = 0.26;
    private static final double LOOP_HARMONY_AMP = 0.20;
    private static final double LOOP_MUTE_AMP = 0.17;
    private static final double EXEC_RIFF_AMP = 0.34;
    private static final double EXEC_MUTE_AMP = 0.28;
    private static final double LEAF_FLOURISH_AMP = 0.38;
    private static final double NOTICE_AMP = 0.32;
    private static final double WARN_RAW_AMP = 0.46;
    private static final double WARN_KNOWN_AMP = 0.22;
    private static final double UPSET_AMP = 0.34;
    private static final double FAIL_RAW_AMP = 0.54;
    private static final double FAIL_KNOWN_AMP = 0.26;
    private static final double SCRAPE_AMP = 0.24;

    private static final int KNOWN_ISSUE_COOLDOWN_FRAMES = 12;
    private static final int UPSET_COOLDOWN_FRAMES = 8;

    /**
     * Resolves the sidecar snapshot file corresponding to a rendered progression frame.
     *
     * @param frame rendered progression frame
     * @return sidecar {@code .adp} file
     */
    public static File snapshotFile(final File frame) {
        return new File(new File(frame.getParentFile(), "adp_events"),
                frame.getName().replaceFirst("\\.(png|svg)$", ".adp"));
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
        return !frames.isEmpty() && frames.stream().allMatch(AdpSonifier::hasReadableFrame);
    }

    @Override
    public void writeWav(final List<File> frames, final File output, final int frameRate) throws IOException {
        if (frames.isEmpty() || frameRate < 1) {
            throw new IllegalArgumentException("Audio requires frames and a positive frame rate");
        }
        if (AdpSynth.SAMPLE_RATE % frameRate != 0) {
            throw new IllegalArgumentException("Frame rate must divide the audio sample rate");
        }
        final int samplesPerFrame = AdpSynth.SAMPLE_RATE / frameRate;
        final long totalSamples = (long) frames.size() * samplesPerFrame + AdpSynth.TAIL_SAMPLES;
        final Map<Integer, List<AdpEvent>> events = composeAdpEvents(frames, samplesPerFrame);
        AdpSynth.writeStereoWav(output, totalSamples, samplesPerFrame, events);
    }

    private static boolean hasReadableFrame(final File frame) {
        if (snapshotFile(frame).isFile()) {
            return true;
        }
        return frame.isFile()
                && frame.getName().toLowerCase(Locale.ROOT).endsWith(".svg")
                && frame.length() > 0L;
    }

    static Map<Integer, List<AdpEvent>> composeAdpEvents(
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

        final Map<Integer, List<AdpEvent>> eventsByFrame = new HashMap<>();
        final int frameCount = states.size();
        final long contentSamples = (long) frameCount * samplesPerFrame;

        final boolean[] frameActive = new boolean[frameCount];
        final double[] frameVelocity = new double[frameCount];
        final double[] frameCoverage = new double[frameCount];
        final int[] frameMood = new int[frameCount]; // 0=Bayati(clean), 1=Hijaz(warn), 2=Saba(fail)

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

        final int riffDur = AdpSynth.millisToSamples(250);
        final int muteDur = AdpSynth.millisToSamples(135);
        final int flourishDur = AdpSynth.millisToSamples(380);
        final int noticeDur = AdpSynth.millisToSamples(340);
        final int warnDur = AdpSynth.millisToSamples(420);
        final int upsetDur = AdpSynth.millisToSamples(320);
        final int failDur = AdpSynth.millisToSamples(650);
        final int tomDur = AdpSynth.millisToSamples(220);
        final int rideDur = AdpSynth.millisToSamples(520);
        final int scrapeDur = AdpSynth.millisToSamples(175);

        // Pass 1: Frame-aligned semantic tree telemetry mapped onto the dual-neck microtonal rig
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

            final int barIndex = (int) ((frameStart / (long) AdpSynth.BAR_SAMPLES) % 4L);
            final double bassRoot = resolveBassRoot(mood, barIndex);

            // Coverage milestones (50% and 100%) -> Klek dry tom + extra-dry ride bell turnaround
            final int milestoneBucket = (coverage >= 0.999) ? 2 : ((coverage >= 0.50) ? 1 : 0);
            if (milestoneBucket > lastMilestoneBucket && frameIndex > 0) {
                lastMilestoneBucket = milestoneBucket;
                final long fillGap = AdpSynth.millisToSamples(75);
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        frameStart,
                        tomDur,
                        VoiceKind.DRY_TOM,
                        51.0, // ~155 Hz muffled rack tom
                        TOM_AMP,
                        0.60,
                        0.10));
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        frameStart + fillGap,
                        tomDur,
                        VoiceKind.DRY_TOM,
                        45.0, // ~110 Hz muffled floor tom
                        TOM_AMP * 1.05,
                        0.55,
                        0.22));
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        frameStart + fillGap * 2L,
                        rideDur,
                        VoiceKind.DRY_RIDE_BELL,
                        (2 == milestoneBucket) ? 74.0 : 69.0,
                        RIDE_AMP * 1.35,
                        0.80,
                        0.18));
            }

            // Node execution & status events (sorted spatially left-to-right, top-to-bottom)
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
                final double melodyMidi = resolveAdpPitch(mood, bassRoot, node, rowRatio);

                final boolean isLeaf = ('L' == node.type() || 'M' == node.type());
                if (delta.execDelta() > 0) {
                    if (isLeaf) {
                        final int target = Math.max(1, node.target());
                        if (delta.prevExec() < target && node.executions() >= target) {
                            // Completed leaf: angular 3-note microtonal phrase (root -> neutral 2nd -> 5th)
                            // with Khn's consonant 5th/octave looper harmony on the peak
                            final double[] phrase = (1 == mood)
                                    ? new double[]{0.0, 1.0, 7.0}
                                    : new double[]{0.0, 1.5, 7.0};
                            final long step = Math.max(AdpSynth.millisToSamples(38), samplesPerFrame / 4);
                            for (int p = 0; p < phrase.length; p++) {
                                final double noteMidi = melodyMidi + phrase[p];
                                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                        onset + p * step,
                                        flourishDur,
                                        VoiceKind.STRAT_HARMONY,
                                        noteMidi,
                                        LEAF_FLOURISH_AMP * (1.0 - 0.08 * p),
                                        0.62 + 0.28 * rowRatio,
                                        clampPan(pan + (p - 1) * 0.14)));
                            }
                            // Consonant octave harmony overdub on the resolving hit (accessible microtonal dissonance!)
                            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                    onset + 2L * step,
                                    flourishDur,
                                    VoiceKind.STRAT_HARMONY,
                                    Math.min(70.0, melodyMidi + 12.0),
                                    LEAF_FLOURISH_AMP * 0.72,
                                    0.72,
                                    clampPan(-pan * 0.45)));
                        } else {
                            // Regular leaf execution: deep microtonal bass-guitar / low-string riff pluck
                            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                    onset,
                                    riffDur,
                                    VoiceKind.STRAT_RIFF,
                                    melodyMidi,
                                    EXEC_RIFF_AMP,
                                    0.45 + 0.38 * rowRatio,
                                    pan));
                        }
                    } else {
                        // Branch traversal: tight palm-muted bass-guitar chug an octave down
                        scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                onset,
                                muteDur,
                                VoiceKind.STRAT_MUTE,
                                melodyMidi - 12.0,
                                EXEC_MUTE_AMP,
                                0.35 + 0.32 * rowRatio,
                                pan));
                    }
                }

                // Semantic status cues
                if (delta.newNotice()) {
                    scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                            onset,
                            noticeDur,
                            VoiceKind.NOTICE_HARMONIC,
                            melodyMidi + 12.0,
                            NOTICE_AMP,
                            0.72,
                            pan));
                }

                if (delta.statusWorsened()) {
                    if ('W' == node.status()) {
                        if (node.knownIssue()) {
                            if (frameIndex - lastKnownWarnFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                                lastKnownWarnFrame = frameIndex;
                                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                        onset,
                                        warnDur,
                                        VoiceKind.RING_MOD_WARN,
                                        bassRoot + 12.0 + 1.5,
                                        WARN_KNOWN_AMP,
                                        0.28,
                                        pan));
                            }
                        } else {
                            // Raw warning: Khn steps on the ring-modulator pedal (neutral-tritone dyad)
                            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                    onset,
                                    warnDur,
                                    VoiceKind.RING_MOD_WARN,
                                    bassRoot + 12.0 + 1.5,
                                    WARN_RAW_AMP,
                                    0.80,
                                    pan));
                            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                    onset,
                                    warnDur,
                                    VoiceKind.RING_MOD_WARN,
                                    bassRoot + 12.0 + 6.5,
                                    WARN_RAW_AMP * 0.72,
                                    0.85,
                                    clampPan(pan + 0.20)));
                        }
                    } else if ('U' == node.status()) {
                        if (frameIndex - lastUpsetFrame >= UPSET_COOLDOWN_FRAMES) {
                            lastUpsetFrame = frameIndex;
                            // Weeping three-quarter-tone downward string bend
                            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                    onset,
                                    upsetDur,
                                    VoiceKind.UPSET_BEND,
                                    melodyMidi,
                                    melodyMidi - 1.5,
                                    UPSET_AMP,
                                    0.55,
                                    pan));
                        }
                    } else if ('F' == node.status()) {
                        if (node.knownIssue()) {
                            if (frameIndex - lastKnownFailFrame >= KNOWN_ISSUE_COOLDOWN_FRAMES) {
                                lastKnownFailFrame = frameIndex;
                                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                        onset,
                                        failDur,
                                        VoiceKind.FAULT_MELTDOWN,
                                        bassRoot + 12.0,
                                        bassRoot + 7.0,
                                        FAIL_KNOWN_AMP,
                                        0.35,
                                        pan * 0.4));
                            }
                        } else {
                            // Raw regression: fuzzed microtonal ring-mod breakdown + dry floor-tom slam
                            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                    onset,
                                    failDur,
                                    VoiceKind.FAULT_MELTDOWN,
                                    bassRoot + 14.5,
                                    bassRoot + 7.0,
                                    FAIL_RAW_AMP,
                                    0.92,
                                    pan * 0.3));
                            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                                    onset,
                                    tomDur,
                                    VoiceKind.DRY_TOM,
                                    43.0,
                                    TOM_AMP * 1.15,
                                    0.75,
                                    0.0));
                        }
                    }
                }
            }

            // Pruned / hidden leaves & column compaction cues
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
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        frameStart,
                        scrapeDur,
                        VoiceKind.MUTE_SCRAPE,
                        60.0,
                        SCRAPE_AMP,
                        0.45,
                        GUITAR_AMP_PAN));
            } else if (columnsCompacted) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        frameStart,
                        tomDur,
                        VoiceKind.DRY_TOM,
                        50.0,
                        TOM_AMP * 0.90,
                        0.65,
                        0.14));
            }

            previousNodes.clear();
            previousNodes.putAll(current.nodes());
            previousNodeCount = currentNodeCount;
            previousFrameMaxCol = frameMaxCol;
        }

        // Pass 2: 136 BPM 7/8 Odd-Meter Mantra Groove & Multi-Track Looper Engine
        composeAdpGroove(
                eventsByFrame,
                samplesPerFrame,
                contentSamples,
                frameActive,
                frameVelocity,
                frameCoverage,
                frameMood);

        // Pass 3: 5-State Live-Duo Verdict Outro
        final FrameState finalState = states.get(frameCount - 1);
        final double finalCoverage = computeCoverage(finalState);
        final boolean fullCoverage = (finalCoverage >= 0.999);
        composeOutro(
                eventsByFrame,
                samplesPerFrame,
                contentSamples,
                hadRawFailure,
                hadRawWarning,
                hadReproducedKnownIssue,
                fullCoverage);

        return eventsByFrame;
    }

    private static void composeAdpGroove(
            final Map<Integer, List<AdpEvent>> eventsByFrame,
            final int samplesPerFrame,
            final long contentSamples,
            final boolean[] frameActive,
            final double[] frameVelocity,
            final double[] frameCoverage,
            final int[] frameMood) {

        final int kickDur = AdpSynth.millisToSamples(135);
        final int snareDur = AdpSynth.millisToSamples(130);
        final int ghostDur = AdpSynth.millisToSamples(70);
        final int tomDur = AdpSynth.millisToSamples(180);
        final int hatClosedDur = AdpSynth.millisToSamples(52);
        final int hatBarkDur = AdpSynth.millisToSamples(125);
        final int rideDur = AdpSynth.millisToSamples(360);
        final int bassDur = AdpSynth.millisToSamples(205);
        final int loopRiffDur = AdpSynth.millisToSamples(230);
        final int loopHarmDur = AdpSynth.millisToSamples(260);
        final int loopMuteDur = AdpSynth.millisToSamples(110);

        final int totalSteps = Math.max(1, (int) ((contentSamples - 1L) / AdpSynth.STEP_SAMPLES) + 1);
        final int maxFrameIdx = frameActive.length - 1;

        for (int step = 0; step < totalSteps; step++) {
            final long baseSample = (long) step * AdpSynth.STEP_SAMPLES;
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
            final int pos = step % STEPS_PER_BAR; // 0..13 (4 + 4 + 6 = 7/8!)

            final double bassRoot = resolveBassRoot(mood, barIndex);
            final double[] riffCell = resolveLoopRiff(mood);

            // 1. Klek's Dry Single-Pedal Kick (4 + 4 + 6 pocket: steps 0, 6, 8, 12)
            if (0 == pos || 6 == pos || 8 == pos || 12 == pos) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        baseSample,
                        kickDur,
                        VoiceKind.DRY_KICK,
                        48.0,
                        KICK_AMP * (0 == pos ? 1.05 : 0.92),
                        0.62,
                        0.0));
            }

            // 2. Klek's Cloth-Muffled Piccolo Snare (crisp backbeats on 4 & 10; tight ghost notes on 3, 7, 13)
            final boolean isTurnaroundFill = (3 == barIndex && pos >= 11);
            if (!isTurnaroundFill) {
                if (4 == pos || 10 == pos) {
                    scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                            baseSample,
                            snareDur,
                            VoiceKind.PICCOLO_SNARE,
                            62.0,
                            SNARE_AMP * (active ? 1.0 : 0.80),
                            0.60 + 0.25 * cov,
                            0.04));
                } else if (active && (3 == pos || 7 == pos || 13 == pos)) {
                    scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                            baseSample,
                            ghostDur,
                            VoiceKind.PICCOLO_SNARE,
                            62.0,
                            GHOST_SNARE_AMP * (0.75 + 0.40 * vel),
                            0.42,
                            0.04));
                }
            } else {
                // Bar-4 odd-meter turnaround fill (rack tom -> floor tom -> piccolo snare snap)
                if (11 == pos) {
                    scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                            baseSample,
                            tomDur,
                            VoiceKind.DRY_TOM,
                            51.0,
                            TOM_AMP,
                            0.58,
                            0.12));
                } else if (12 == pos) {
                    scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                            baseSample,
                            tomDur,
                            VoiceKind.DRY_TOM,
                            45.0,
                            TOM_AMP * 1.04,
                            0.54,
                            0.22));
                } else {
                    scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                            baseSample,
                            snareDur,
                            VoiceKind.PICCOLO_SNARE,
                            62.0,
                            SNARE_AMP * 1.06,
                            0.78,
                            0.04));
                }
            }

            // 3. Meinl Extra Dry Hi-Hat & Ride Bell (restrained on pauses, driving when active)
            if (0 == (pos & 1)) {
                final boolean bark = (12 == pos && active);
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        baseSample,
                        bark ? hatBarkDur : hatClosedDur,
                        VoiceKind.EXTRA_DRY_HAT,
                        72.0,
                        HAT_AMP * (bark ? 1.15 : (active ? 0.92 : 0.58)),
                        bark ? 0.78 : (0.38 + 0.18 * cov),
                        -0.16));
            } else if (active && cov >= 0.45 && (5 == pos || 9 == pos)) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        baseSample,
                        hatClosedDur,
                        VoiceKind.EXTRA_DRY_HAT,
                        72.0,
                        HAT_AMP * 0.55,
                        0.32,
                        -0.16));
            }
            if (0 == pos && cov >= 0.35) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        baseSample,
                        rideDur,
                        VoiceKind.DRY_RIDE_BELL,
                        69.0,
                        RIDE_AMP * (0.65 + 0.35 * cov),
                        0.55 + 0.30 * cov,
                        0.18));
            }

            // 4. Khn's Bottom Neck — 24-TET Microtonal Precision Bass Ostinato (steps 0, 2, 3, 6, 8, 10, 12)
            final double bassInterval = switch (pos) {
                case 0, 6 -> 0.0;
                case 2 -> 7.0; // punchy 5th pop inside deep bass range
                case 3 -> (1 == mood) ? 1.0 : 1.5; // iconic neutral-2nd quarter-tone hook!
                case 8 -> (3 == barIndex) ? 5.0 : 7.0;
                case 10 -> (2 == mood) ? 4.0 : 3.0;
                case 12 -> (1 == mood) ? 8.5 : 10.0;
                default -> Double.NaN;
            };
            if (!Double.isNaN(bassInterval)) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        baseSample,
                        bassDur,
                        VoiceKind.P_BASS,
                        bassRoot + bassInterval,
                        BASS_AMP * (0 == pos ? 1.05 : 0.90),
                        0.42 + 0.34 * cov,
                        BASS_AMP_PAN));
            }

            // 5. Khn's Top Neck — Live Multi-Track Looper Layer 1 (Core angular 7/8 microtonal riff in bass-guitar range)
            final int riffSlot = switch (pos) {
                case 0 -> 0;
                case 3 -> 1;
                case 5 -> 2;
                case 8 -> 3;
                case 11 -> 4;
                case 13 -> 5;
                default -> -1;
            };
            if (riffSlot >= 0) {
                final double guitarNote = clampRegister(bassRoot + 12.0 + riffCell[riffSlot], 43.0, 55.0);
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        baseSample,
                        loopRiffDur,
                        VoiceKind.STRAT_RIFF,
                        guitarNote,
                        LOOP_RIFF_AMP * (active ? 1.0 : 0.78),
                        0.45 + 0.35 * cov,
                        GUITAR_AMP_PAN));
            }

            // 6. Live Looper Layer 2 — Harmonized Counterpoint (+7.0 consonant 5th over microtonal cell!)
            // Enters progressively like Khn stamping a looper pedal as coverage builds
            if (cov >= 0.28 || barNum >= 2) {
                final int harmSlot = switch (pos) {
                    case 1 -> 0;
                    case 4 -> 2;
                    case 6 -> 1;
                    case 9 -> 3;
                    case 12 -> 4;
                    default -> -1;
                };
                if (harmSlot >= 0) {
                    // Consonant 5th (+7.0) above the microtonal root preserves Angine's "accessible dissonance"
                    final double harmNote = clampRegister(
                            bassRoot + 12.0 + riffCell[harmSlot] + 7.0,
                            48.0,
                            60.0);
                    scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                            baseSample,
                            loopHarmDur,
                            VoiceKind.STRAT_HARMONY,
                            harmNote,
                            LOOP_HARMONY_AMP * (active ? 1.0 : 0.72),
                            0.52 + 0.30 * cov,
                            HARMONY_LOOP_PAN));
                }
            }

            // 7. Live Looper Layer 3 — Interlocking palm-muted offbeat pulse at high coverage
            if (active && cov >= 0.65 && (2 == pos || 7 == pos || 10 == pos)) {
                final double muteNote = clampRegister(bassRoot + 12.0 + riffCell[pos % riffCell.length], 36.0, 48.0);
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        baseSample,
                        loopMuteDur,
                        VoiceKind.STRAT_MUTE,
                        muteNote,
                        LOOP_MUTE_AMP * (0.75 + 0.35 * vel),
                        0.48,
                        MUTE_LOOP_PAN));
            }
        }
    }

    private static void composeOutro(
            final Map<Integer, List<AdpEvent>> eventsByFrame,
            final int samplesPerFrame,
            final long outroStart,
            final boolean hadRawFailure,
            final boolean hadRawWarning,
            final boolean hadReproducedKnownIssue,
            final boolean fullCoverage) {

        final int chordDur = AdpSynth.millisToSamples(1_550);
        final int riffStep = AdpSynth.millisToSamples(95);

        if (hadRawFailure) {
            // Verdict 5: Raw Failure Regression — Dada fuzzed microtonal dive + ring-mod clang + tom thud
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    outroStart,
                    AdpSynth.millisToSamples(1_300),
                    VoiceKind.FAULT_MELTDOWN,
                    43.5, // quarter-tone dissonant G-Saba
                    36.0, // drops to C2
                    FAIL_RAW_AMP * 1.12,
                    0.92,
                    0.0));
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    outroStart,
                    AdpSynth.millisToSamples(1_200),
                    VoiceKind.P_BASS,
                    31.0,
                    BASS_AMP * 1.15,
                    0.50,
                    BASS_AMP_PAN));
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    outroStart,
                    AdpSynth.millisToSamples(280),
                    VoiceKind.DRY_TOM,
                    43.0,
                    TOM_AMP * 1.20,
                    0.65,
                    0.15));

        } else if (hadRawWarning) {
            // Verdict 3: Passed with Raw Warnings — Hijaz neutral-tritone chord + lingering ring-mod stab
            final double[] warnChord = {43.0, 44.0, 49.5, 55.0};
            for (int i = 0; i < warnChord.length; i++) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        outroStart,
                        chordDur,
                        VoiceKind.STRAT_RIFF,
                        warnChord[i],
                        EXEC_RIFF_AMP * 0.85,
                        0.55,
                        -0.25 + 0.18 * i));
            }
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    outroStart + AdpSynth.millisToSamples(90),
                    AdpSynth.millisToSamples(1_100),
                    VoiceKind.RING_MOD_WARN,
                    49.5,
                    WARN_RAW_AMP * 0.82,
                    0.65,
                    GUITAR_AMP_PAN));

        } else if (!fullCoverage) {
            // Verdict 4: Incomplete Coverage — abrupt looper cut leaving an unresolved G + Ab½ neutral-2nd dyad
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    outroStart,
                    chordDur,
                    VoiceKind.STRAT_RIFF,
                    43.0,
                    EXEC_RIFF_AMP * 0.90,
                    0.48,
                    -0.20));
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    outroStart + AdpSynth.millisToSamples(45),
                    chordDur,
                    VoiceKind.STRAT_RIFF,
                    44.5, // hanging quarter-tone neutral 2nd!
                    EXEC_RIFF_AMP * 0.85,
                    0.52,
                    0.24));
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    outroStart,
                    AdpSynth.millisToSamples(900),
                    VoiceKind.P_BASS,
                    31.0,
                    BASS_AMP * 0.90,
                    0.40,
                    BASS_AMP_PAN));

        } else if (hadReproducedKnownIssue) {
            // Verdict 2: Clean Pass with Known Issues — warm G5 + neutral-6th guitar chord + subtle harmonic chime
            final double[] knownChord = {43.0, 50.0, 51.5, 55.0};
            for (int i = 0; i < knownChord.length; i++) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        outroStart,
                        chordDur,
                        VoiceKind.STRAT_HARMONY,
                        knownChord[i],
                        EXEC_RIFF_AMP * 0.85,
                        0.60,
                        -0.30 + 0.20 * i));
            }
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    outroStart + AdpSynth.millisToSamples(120),
                    AdpSynth.millisToSamples(1_150),
                    VoiceKind.NOTICE_HARMONIC,
                    62.0,
                    NOTICE_AMP * 0.85,
                    0.68,
                    0.18));

        } else {
            // Verdict 1: Spotless 100% Pass — tight unison G Bayati math-rock triumph riff + crisp final power-chord hit!
            final double[] unisonRiff = {43.0, 44.5, 46.0, 48.0, 50.0};
            for (int i = 0; i < unisonRiff.length; i++) {
                final long t = outroStart + (long) i * riffStep;
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        t,
                        AdpSynth.millisToSamples(220),
                        VoiceKind.STRAT_HARMONY,
                        unisonRiff[i],
                        EXEC_RIFF_AMP * 0.95,
                        0.72,
                        (0 == (i & 1)) ? -0.22 : 0.28));
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        t,
                        AdpSynth.millisToSamples(200),
                        VoiceKind.P_BASS,
                        unisonRiff[i] - 12.0,
                        BASS_AMP * 0.88,
                        0.65,
                        BASS_AMP_PAN));
            }

            // Final tight band hit on G5 power chord + dry kick + piccolo snare + ride bell!
            final long hitTime = outroStart + (long) unisonRiff.length * riffStep;
            final double[] finalPowerChord = {43.0, 50.0, 55.0, 62.0}; // G2, D3, G3, D4
            for (int i = 0; i < finalPowerChord.length; i++) {
                scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                        hitTime,
                        AdpSynth.millisToSamples(1_350),
                        VoiceKind.STRAT_HARMONY,
                        finalPowerChord[i],
                        LEAF_FLOURISH_AMP * 0.92,
                        0.85,
                        -0.36 + 0.24 * i));
            }
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    hitTime,
                    AdpSynth.millisToSamples(1_250),
                    VoiceKind.P_BASS,
                    31.0,
                    BASS_AMP * 1.15,
                    0.75,
                    BASS_AMP_PAN));
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    hitTime,
                    AdpSynth.millisToSamples(150),
                    VoiceKind.DRY_KICK,
                    48.0,
                    KICK_AMP * 1.10,
                    0.72,
                    0.0));
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    hitTime,
                    AdpSynth.millisToSamples(150),
                    VoiceKind.PICCOLO_SNARE,
                    62.0,
                    SNARE_AMP * 1.05,
                    0.82,
                    0.04));
            scheduleEvent(eventsByFrame, samplesPerFrame, new AdpEvent(
                    hitTime,
                    AdpSynth.millisToSamples(950),
                    VoiceKind.DRY_RIDE_BELL,
                    74.0,
                    RIDE_AMP * 1.30,
                    0.85,
                    0.18));
        }
    }

    private static double resolveBassRoot(final int mood, final int barIndex) {
        return switch (mood) {
            case 2 -> BASS_ROOTS_FAIL[barIndex % BASS_ROOTS_FAIL.length];
            case 1 -> BASS_ROOTS_WARN[barIndex % BASS_ROOTS_WARN.length];
            default -> BASS_ROOTS_CLEAN[barIndex % BASS_ROOTS_CLEAN.length];
        };
    }

    private static double[] resolveLoopRiff(final int mood) {
        return switch (mood) {
            case 2 -> LOOP_RIFF_SABA;
            case 1 -> LOOP_RIFF_HIJAZ;
            default -> LOOP_RIFF_BAYATI;
        };
    }

    private static double[] resolveScale(final int mood) {
        return switch (mood) {
            case 2 -> SCALE_SABA;
            case 1 -> SCALE_HIJAZ;
            default -> SCALE_BAYATI;
        };
    }

    /**
     * Resolves a deterministic 24-TET microtonal pitch strictly inside the deep bass-guitar /
     * low-baritone register [{@code G2=43.0 .. Bb3=58.0}], preserving smooth vertical row mapping
     * and consonant fifths/unisons over quarter-tone bass roots.
     */
    static double resolveAdpPitch(
            final int mood,
            final double bassRoot,
            final NodeState node,
            final double rowRatio) {
        final double clampedRow = Math.clamp(rowRatio, 0.0, 1.0);
        final int selector = Math.abs(node.col() * 3 + node.rowTenths());
        final double[] scale = resolveScale(mood);

        final double rawMidi;
        if (0 == (selector % 4)) {
            // Lock onto active quarter-tone bass root (+0 or +7 consonant 5th) for accessible microtonal groove
            final double chordOffset = (0 == ((selector / 4) & 1)) ? 0.0 : 7.0;
            rawMidi = bassRoot + 12.0 + chordOffset + ((clampedRow > 0.55) ? 12.0 : 0.0);
        } else {
            final int maxBias = Math.max(0, scale.length - 4);
            final int rowBias = (int) Math.round(clampedRow * maxBias);
            final int idx = Math.min(scale.length - 1, rowBias + (selector % 4));
            rawMidi = scale[idx];
        }
        return clampRegister(rawMidi, MIN_MELODY_MIDI, MAX_MELODY_MIDI);
    }

    private static double clampRegister(final double rawMidi, final double minMidi, final double maxMidi) {
        double midi = rawMidi;
        while (midi < minMidi) {
            midi += 12.0;
        }
        while (midi > maxMidi) {
            midi -= 12.0;
        }
        return midi;
    }

    private static double computeStereoPan(final int col, final int panSpan) {
        final double norm = Math.clamp((double) col / (double) panSpan, 0.0, 1.0);
        return clampPan(norm * 1.56 - 0.78);
    }

    private static double clampPan(final double pan) {
        return Math.clamp(pan, -0.90, 0.90);
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
            final Map<Integer, List<AdpEvent>> eventsByFrame,
            final int samplesPerFrame,
            final AdpEvent event) {
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
