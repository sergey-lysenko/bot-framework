package works.lysenko.base.output.sonifier;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.tree.TreeCameraTransform;
import works.lysenko.base.output.tree.TreeProgressMetrics;
import works.lysenko.util.data.enums.ExecutionStatus;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Polyphonic narrative sonifier ({@code orpheus}): translates every phase of scenario tree progression
 * into a deterministic, multi-voice musical narrative.
 * <p>
 * Combines structural tree state, node-level execution deltas, semantic execution status,
 * and camera dynamics into six coordinated voices:
 * <ul>
 *     <li><b>5-Stage Ascending Modal Progression</b>: Harmonic mode brightens and rises from
 *     D Dorian to A Major 9th Pentatonic as leaf coverage progresses from 0% to 100%.</li>
 *     <li><b>Branch Traversal Melody</b>: Intermediate {@code NODE} executions play soft marimba-like
 *     steps mapped to tree column depth and vertical row contour, with logarithmic familiarity
 *     attenuation so frequently visited root branches recede into a gentle pulse.</li>
 *     <li><b>Leaf Completion Arpeggios &amp; Over-Execution Pings</b>: Newly completed leaves trigger
 *     a micro-staggered 3-note harp bloom; partial leaf progress plays a rising dyad; over-executions
 *     beyond target emit a high-register crystalline ping.</li>
 *     <li><b>Semantic Status Harmony</b>: Warnings and known issues add a warm suspended-fourth color tone,
 *     while failures trigger an unmistakable low-mid minor/tritone cadence.</li>
 *     <li><b>Deterministic Pruning Waterfall</b>: When {@code onLimbo} prunes inactive branches,
 *     a descending pentatonic grace-note cascade plays instead of random noise.</li>
 *     <li><b>Camera Cooldown Shimmer &amp; Finale Resolution</b>: Tracks smooth camera zoom/pan settling
 *     during cooldown frames and concludes on the final frame with a strummed 5-voice chord
 *     (Major 9th on clean runs, Dorian minor-add9 when failures are present).</li>
 * </ul>
 */
public final class OrpheusSonifier extends AbstractTreeSonifier {

    /**
     * Configuration mode identifier for the Orpheus sonifier.
     */
    public static final String MODE = "orpheus";

    private static final int[][] MODAL_SCALES = {
            {0, 2, 3, 5, 7, 9, 10},  // Stage 0: D Dorian
            {0, 2, 4, 5, 7, 9, 10},  // Stage 1: E Mixolydian
            {0, 2, 4, 5, 7, 9, 11},  // Stage 2: F Major (Ionian)
            {0, 2, 4, 6, 7, 9, 11},  // Stage 3: G Lydian
            {0, 2, 4, 7, 9, 11, 14}  // Stage 4: A Major 9th Pentatonic
    };

    private static final int[] MODAL_ROOTS = {50, 52, 53, 55, 57};
    private static final int[] PENTATONIC_CASCADE = {14, 11, 9, 7, 4, 2, 0};
    private static final int[] CLEAN_FINALE_CHORD = {0, 4, 7, 11, 14};
    private static final int[] FAILED_FINALE_CHORD = {0, 3, 7, 10, 14};

    private static final double BASS_AMPLITUDE = 0.085;
    private static final double BRANCH_AMPLITUDE = 0.115;
    private static final double LEAF_PARTIAL_AMPLITUDE = 0.125;
    private static final double LEAF_BLOOM_AMPLITUDE = 0.155;
    private static final double OVER_EXEC_AMPLITUDE = 0.095;
    private static final double WARNING_AMPLITUDE = 0.095;
    private static final double FAILURE_AMPLITUDE = 0.560;
    private static final double PRUNE_AMPLITUDE = 0.095;
    private static final double CAMERA_SHIMMER_AMPLITUDE = 0.065;
    private static final double FINALE_AMPLITUDE = 0.135;

    private static final Pattern SVG_CAMERA_PATTERN = Pattern.compile(
            "<g transform=\"translate\\(([-0-9.]+),\\s*([-0-9.]+)\\)\\s*scale\\(([-0-9.]+)\\)\"");
    private static final Pattern SVG_NODE_PATTERN = Pattern.compile(
            "<g id=\"([^\"]+)\" class=\"node-card ([^\"]+)\" transform=\"translate\\(([-0-9.]+),\\s*([-0-9.]+)\\)\"[\\s\\S]*?</g>");
    private static final Pattern SVG_TITLE_PATTERN = Pattern.compile(
            "<title>([^<]*)</title>");
    private static final Pattern SVG_CARD_BG_PATTERN = Pattern.compile(
            "<rect class=\"card-bg\"[^>]*stroke=\"([^\"]+)\"");
    private static final Pattern SVG_BADGE_PATTERN = Pattern.compile(
            "<text class=\"tx1\"[^>]*>([^<]*)</text>");

    /**
     * Resolves the sidecar snapshot file corresponding to a rendered progression frame.
     *
     * @param frame rendered frame file ({@code .svg} or {@code .png})
     * @return sidecar {@code .orpheus} snapshot file inside {@code orpheus_events/}
     */
    public static File snapshotFile(final File frame) {
        return new File(new File(frame.getParentFile(), "orpheus_events"),
                frame.getName().replaceFirst("\\.(png|svg)$", ".orpheus"));
    }

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public void capture(final File frame, final TreeLayout layout, final int target) throws IOException {
        final File snapshot = snapshotFile(frame);
        Files.createDirectories(snapshot.toPath().getParent());

        final Map<NodeData, Boolean> childFailureCache = new HashMap<>();
        final TreeCameraTransform.Transform camera = TreeCameraTransform.computeZoomTransform(
                layout, Set.of(), Math.max(1, target), 1920, 1080);

        final List<NodeData> orderedNodes = new ArrayList<>(layout.nodes());
        orderedNodes.sort(Comparator.comparingInt(NodeData::col)
                .thenComparingDouble(NodeData::row)
                .thenComparing(NodeData::id));

        try (BufferedWriter writer = Files.newBufferedWriter(
                snapshot.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(String.format(Locale.US, "#camera\t%.4f\t%.2f\t%.2f",
                    camera.zoom(), camera.translateX(), camera.translateY()));
            writer.newLine();

            for (final NodeData node : orderedNodes) {
                final String identity = Base64.getUrlEncoder().withoutPadding().encodeToString(
                        path(node).getBytes(StandardCharsets.UTF_8));
                final int col = Math.max(0, node.col());
                final int rowTenths = (int) Math.round(Math.max(0.0, node.row()) * 10.0);
                final ScenarioType scenarioType = TreeProgressMetrics.getScenarioType(node);
                final char typeCode = typeCodeOf(node, scenarioType);
                final int executions = (null != node.result()) ? Math.max(0, node.result().getExecutions()) : 0;
                final int leafTarget = (ScenarioType.MONO == scenarioType) ? 1 : Math.max(1, target);
                final char statusCode = statusCodeOf(node, executions, childFailureCache);

                writer.write(identity);
                writer.write('\t');
                writer.write(Integer.toString(col));
                writer.write('\t');
                writer.write(Integer.toString(rowTenths));
                writer.write('\t');
                writer.write(typeCode);
                writer.write('\t');
                writer.write(Integer.toString(executions));
                writer.write('\t');
                writer.write(Integer.toString(leafTarget));
                writer.write('\t');
                writer.write(statusCode);
                writer.newLine();
            }
        }
    }

    @Override
    public boolean canSonify(final List<File> frames) {
        return !frames.isEmpty() && frames.stream().allMatch(OrpheusSonifier::hasReadableState);
    }

    private static boolean hasReadableState(final File frame) {
        if (snapshotFile(frame).isFile()) {
            return true;
        }
        return frame.isFile()
                && frame.getName().toLowerCase(Locale.ROOT).endsWith(".svg")
                && frame.length() > 0L;
    }

    @Override
    protected Map<Integer, List<AudioNote>> compose(final List<File> frames, final int samplesPerFrame)
            throws IOException {
        final Map<Integer, List<AudioNote>> notesByFrame = new HashMap<>();
        final Map<String, NodeSnapshot> previousNodes = new HashMap<>();

        int previousStage = -1;
        int previousTotalNodes = -1;
        double previousZoom = Double.NaN;
        double previousTx = Double.NaN;
        double previousTy = Double.NaN;
        boolean everHadFailure = false;

        for (int frameIndex = 0; frameIndex < frames.size(); frameIndex++) {
            final FrameSnapshot current = readFrameState(frames.get(frameIndex));
            final List<AudioNote> frameNotes = new ArrayList<>();
            final long frameStartSample = (long) frameIndex * samplesPerFrame;

            final int stage = computeModalStage(current.coverageRatio());
            final int rootMidi = MODAL_ROOTS[stage];
            final int[] scale = MODAL_SCALES[stage];

            // 1. Harmonic foundation: play warm low root when modal stage advances or periodically during activity
            if (stage != previousStage && (!current.nodes().isEmpty())) {
                frameNotes.add(new AudioNote(frameStartSample, rootMidi - 12, BASS_AMPLITUDE, 0.15));
                if (stage > previousStage && previousStage >= 0) {
                    final long fifthOffset = subFrameOffset(samplesPerFrame, 28, 1);
                    frameNotes.add(new AudioNote(
                            frameStartSample + fifthOffset,
                            rootMidi - 5,
                            BASS_AMPLITUDE * 0.85,
                            0.22));
                }
                previousStage = stage;
            }

            // 2. Deterministic pruning waterfall when onLimbo removes OFF branches
            final int currentTotalNodes = current.nodes().size();
            if (previousTotalNodes > 0 && currentTotalNodes < previousTotalNodes) {
                final int prunedCount = previousTotalNodes - currentTotalNodes;
                final double pruneScale = Math.min(1.0, Math.sqrt(prunedCount / 8.0));
                final int cascadeNotes = Math.min(4, Math.max(2, 1 + prunedCount / 4));
                for (int i = 0; i < cascadeNotes; i++) {
                    final long offset = subFrameOffset(samplesPerFrame, i * 18, cascadeNotes);
                    final int cascadeMidi = rootMidi + PENTATONIC_CASCADE[i % PENTATONIC_CASCADE.length];
                    final double amp = PRUNE_AMPLITUDE * pruneScale * (1.0 - 0.15 * i);
                    frameNotes.add(new AudioNote(frameStartSample + offset, cascadeMidi, amp, 0.35));
                }
            }
            previousTotalNodes = currentTotalNodes;

            // Classify node-level transitions in the current frame
            final List<NodeSnapshot> progressedBranches = new ArrayList<>();
            final List<NodeSnapshot> completedLeaves = new ArrayList<>();
            final List<NodeSnapshot> partialLeaves = new ArrayList<>();
            final List<NodeSnapshot> overExecutedLeaves = new ArrayList<>();
            final List<NodeSnapshot> newlyFailedNodes = new ArrayList<>();
            final List<NodeSnapshot> newlyWarnedNodes = new ArrayList<>();

            for (final NodeSnapshot node : current.nodes().values()) {
                if ('F' == node.statusCode() || 'U' == node.statusCode()) {
                    everHadFailure = true;
                }
                final NodeSnapshot prev = previousNodes.get(node.identity());
                final int oldExec = (null != prev) ? prev.executions() : 0;
                final char oldStatus = (null != prev) ? prev.statusCode() : 'N';
                final int delta = node.executions() - oldExec;

                if ('F' == node.statusCode() && ('F' != oldStatus || delta > 0)) {
                    newlyFailedNodes.add(node);
                } else if (('W' == node.statusCode() || 'K' == node.statusCode())
                        && (oldStatus != node.statusCode() || delta > 0)) {
                    newlyWarnedNodes.add(node);
                }

                if (delta > 0 || (node.recentHighlight() && null == prev && node.executions() > 0)) {
                    if (node.isTerminal()) {
                        if (oldExec < node.target() && node.executions() >= node.target()) {
                            completedLeaves.add(node);
                        } else if (node.executions() < node.target()) {
                            partialLeaves.add(node);
                        } else {
                            overExecutedLeaves.add(node);
                        }
                    } else {
                        progressedBranches.add(node);
                    }
                }
            }

            // 3. Branch traversal melody (intermediate NODE steps, sorted by column depth)
            if (!progressedBranches.isEmpty()) {
                progressedBranches.sort(Comparator.comparingInt(NodeSnapshot::col)
                        .thenComparingInt(NodeSnapshot::rowTenths));
                final int maxBranchNotes = Math.min(2, progressedBranches.size());
                for (int i = 0; i < maxBranchNotes; i++) {
                    final NodeSnapshot branch = progressedBranches.get(progressedBranches.size() - maxBranchNotes + i);
                    final int degreeIndex = branch.col() + Math.floorMod(branch.rowTenths() / 10, 3);
                    final int midi = scalePitch(rootMidi - 12, scale, degreeIndex);
                    final double familiarity = 1.0 / (1.0 + 0.35 * Math.log(Math.max(1, branch.executions())));
                    final double amp = BRANCH_AMPLITUDE * familiarity;
                    final long offset = subFrameOffset(samplesPerFrame, i * 24, maxBranchNotes);
                    frameNotes.add(new AudioNote(frameStartSample + offset, midi, amp, 0.22));
                }
            }

            // 4. Terminal leaf partial progress (rising dyad)
            if (!partialLeaves.isEmpty()) {
                final NodeSnapshot leaf = partialLeaves.get(0);
                final int baseDegree = leaf.col() + Math.floorMod(leaf.identity().hashCode(), 3);
                final int firstMidi = scalePitch(rootMidi - 12, scale, baseDegree);
                final int secondMidi = scalePitch(rootMidi - 12, scale, baseDegree + 2);
                final double progressRatio = Math.min(1.0, (double) leaf.executions() / Math.max(1, leaf.target()));
                final double amp = LEAF_PARTIAL_AMPLITUDE * (0.6 + 0.4 * progressRatio);
                frameNotes.add(new AudioNote(frameStartSample, firstMidi, amp, 0.48));
                frameNotes.add(new AudioNote(
                        frameStartSample + subFrameOffset(samplesPerFrame, 25, 2),
                        secondMidi,
                        amp * 0.9,
                        0.55));
            }

            // 5. Terminal leaf completion bloom (micro-staggered 3-note ascending harp arpeggio)
            if (!completedLeaves.isEmpty()) {
                final NodeSnapshot leaf = completedLeaves.get(0);
                final int baseDegree = leaf.col() + Math.floorMod(leaf.identity().hashCode(), 4);
                final double gainBoost = Math.min(1.35, Math.sqrt(completedLeaves.size()));
                for (int i = 0; i < 3; i++) {
                    final int midi = scalePitch(rootMidi - 12, scale, baseDegree + i * 2);
                    final long offset = subFrameOffset(samplesPerFrame, i * 22, 3);
                    final double amp = LEAF_BLOOM_AMPLITUDE * gainBoost * (0.85 + 0.08 * i);
                    frameNotes.add(new AudioNote(frameStartSample + offset, midi, amp, 0.78));
                }
            }

            // 6. Over-execution crystalline harmonic ping
            if (!overExecutedLeaves.isEmpty()) {
                final NodeSnapshot leaf = overExecutedLeaves.get(0);
                final int overCount = Math.max(1, leaf.executions() - leaf.target());
                final int degree = leaf.col() + Math.floorMod(leaf.rowTenths() / 10, scale.length);
                final int midi = Math.min(96, scalePitch(rootMidi, scale, degree));
                final double amp = OVER_EXEC_AMPLITUDE / (1.0 + 0.20 * (overCount - 1));
                frameNotes.add(new AudioNote(
                        frameStartSample + subFrameOffset(samplesPerFrame, 15, 1),
                        midi,
                        amp,
                        0.90));
            }

            // 7. Semantic status harmony: warnings/known-issues (sus4 color) and failures (minor/tritone cadence)
            if (!newlyWarnedNodes.isEmpty()) {
                final NodeSnapshot warned = newlyWarnedNodes.get(0);
                final int baseMidi = scalePitch(rootMidi + 12, scale, warned.col());
                final long offset = subFrameOffset(samplesPerFrame, 18, 1);
                frameNotes.add(new AudioNote(frameStartSample + offset, baseMidi + 5, WARNING_AMPLITUDE, 0.45));
            }
            if (!newlyFailedNodes.isEmpty()) {
                final int failBase = rootMidi;
                final double failBoost = Math.min(1.30, Math.sqrt(newlyFailedNodes.size()));
                final double failAmp = Math.min(0.70, FAILURE_AMPLITUDE * failBoost);
                frameNotes.add(new AudioNote(frameStartSample, failBase, failAmp, 0.75));
                frameNotes.add(new AudioNote(
                        frameStartSample + subFrameOffset(samplesPerFrame, 24, 2),
                        failBase + 3,
                        failAmp * 0.95,
                        0.78));
                frameNotes.add(new AudioNote(
                        frameStartSample + subFrameOffset(samplesPerFrame, 48, 3),
                        failBase + 6,
                        failAmp * 0.90,
                        0.82));
            }

            // 8. Camera cooldown shimmer when no execution events fired but camera is actively zooming/panning
            if (frameNotes.isEmpty() && !Double.isNaN(previousZoom)) {
                final double dz = Math.abs(current.zoom() - previousZoom);
                final double dPan = Math.hypot(current.translateX() - previousTx, current.translateY() - previousTy);
                if ((dz > 0.012 || dPan > 6.0) && frameIndex % 2 == 0) {
                    final double motionIntensity = Math.min(1.0, (dz * 8.0) + (dPan / 240.0));
                    final int shimmerDegree = (frameIndex / 2) % scale.length;
                    final int zoomLift = (current.zoom() > previousZoom) ? 12 : 0;
                    final int shimmerMidi = scalePitch(rootMidi + zoomLift, scale, shimmerDegree);
                    final double amp = CAMERA_SHIMMER_AMPLITUDE * (0.45 + 0.55 * motionIntensity);
                    frameNotes.add(new AudioNote(frameStartSample, shimmerMidi, amp, 0.30));
                }
            }

            previousZoom = current.zoom();
            previousTx = current.translateX();
            previousTy = current.translateY();

            // Update node state memory for the next frame
            previousNodes.clear();
            previousNodes.putAll(current.nodes());

            // 9. Finale resolution chord strum on the last frame
            if (frameIndex == frames.size() - 1 && frames.size() > 1 && !current.nodes().isEmpty()) {
                final int[] chord = everHadFailure ? FAILED_FINALE_CHORD : CLEAN_FINALE_CHORD;
                for (int i = 0; i < chord.length; i++) {
                    final long offset = subFrameOffset(samplesPerFrame, i * 16, chord.length);
                    final int midi = rootMidi + chord[i];
                    final double brightness = everHadFailure ? 0.55 : 0.85;
                    frameNotes.add(new AudioNote(frameStartSample + offset, midi, FINALE_AMPLITUDE, brightness));
                }
            }

            if (!frameNotes.isEmpty()) {
                notesByFrame.put(frameIndex, frameNotes);
            }
        }

        return notesByFrame;
    }

    private static int computeModalStage(final double coverageRatio) {
        if (coverageRatio >= 0.99) {
            return 4;
        }
        if (coverageRatio >= 0.75) {
            return 3;
        }
        if (coverageRatio >= 0.50) {
            return 2;
        }
        if (coverageRatio >= 0.25) {
            return 1;
        }
        return 0;
    }

    private static int scalePitch(final int baseMidi, final int[] scale, final int degree) {
        final int safeDegree = Math.max(0, degree);
        final int octave = Math.min(2, safeDegree / scale.length);
        final int noteInScale = scale[safeDegree % scale.length];
        return Math.max(24, Math.min(96, baseMidi + octave * 12 + noteInScale));
    }

    private static long subFrameOffset(final int samplesPerFrame, final int desiredMillis, final int totalVoices) {
        if (desiredMillis <= 0 || samplesPerFrame <= 4) {
            return 0L;
        }
        final long desiredSamples = (long) WavSynth.SAMPLE_RATE * desiredMillis / 1000L;
        final long maxOffset = Math.max(0L, samplesPerFrame - Math.max(2L, totalVoices));
        return Math.min(desiredSamples, maxOffset);
    }

    private static char typeCodeOf(final NodeData node, final ScenarioType type) {
        if (node.children().isEmpty() && ScenarioType.MONO == type) {
            return 'M';
        }
        if (node.children().isEmpty() && ScenarioType.LEAF == type) {
            return 'L';
        }
        return 'N';
    }

    private static char statusCodeOf(
            final NodeData node,
            final int executions,
            final Map<NodeData, Boolean> childFailureCache) {
        if (TreeHtml.hasFailure(node)) {
            return 'F';
        }
        if (TreeHtml.hasChildFailure(node, childFailureCache)) {
            return 'U';
        }
        if (null != node.result() && ExecutionStatus.WARNING == node.result().status()) {
            return 'W';
        }
        if (null != node.knownIssues() && !node.knownIssues().isEmpty()) {
            return 'K';
        }
        return executions > 0 ? 'P' : 'N';
    }

    private static String path(final NodeData node) {
        final List<String> lineage = new ArrayList<>();
        NodeData current = node;
        while (null != current) {
            lineage.add(current.label());
            current = current.parent();
        }
        java.util.Collections.reverse(lineage);
        final List<String> parts = new ArrayList<>();
        parts.add(node.group());
        parts.addAll(lineage);
        return String.join("/", parts);
    }

    private static FrameSnapshot readFrameState(final File frame) throws IOException {
        final File snapshot = snapshotFile(frame);
        if (snapshot.isFile()) {
            return readSnapshotFile(snapshot);
        }
        if (frame.isFile() && frame.getName().toLowerCase(Locale.ROOT).endsWith(".svg")) {
            return parseSvgFrame(frame);
        }
        throw new IOException("Missing orpheus snapshot for frame: " + frame);
    }

    private static FrameSnapshot readSnapshotFile(final File file) throws IOException {
        final Map<String, NodeSnapshot> nodes = new LinkedHashMap<>();
        double zoom = 1.0;
        double translateX = 0.0;
        double translateY = 0.0;

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while (null != (line = reader.readLine())) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("#camera\t")) {
                    final String[] camFields = line.split("\\t", -1);
                    if (camFields.length >= 4) {
                        try {
                            zoom = Double.parseDouble(camFields[1]);
                            translateX = Double.parseDouble(camFields[2]);
                            translateY = Double.parseDouble(camFields[3]);
                        } catch (final NumberFormatException e) {
                            throw new IOException("Invalid camera header in orpheus snapshot: " + file, e);
                        }
                    }
                    continue;
                }
                final String[] fields = line.split("\\t", -1);
                if (7 != fields.length) {
                    throw new IOException("Invalid orpheus audio snapshot line in: " + file);
                }
                try {
                    final String identity = new String(Base64.getUrlDecoder().decode(fields[0]), StandardCharsets.UTF_8);
                    final int col = Integer.parseInt(fields[1]);
                    final int rowTenths = Integer.parseInt(fields[2]);
                    final char typeCode = fields[3].isEmpty() ? 'N' : fields[3].charAt(0);
                    final int executions = Math.max(0, Integer.parseInt(fields[4]));
                    final int target = Math.max(1, Integer.parseInt(fields[5]));
                    final char statusCode = fields[6].isEmpty() ? 'N' : fields[6].charAt(0);
                    nodes.put(identity, new NodeSnapshot(
                            identity, col, rowTenths, typeCode, executions, target, statusCode, false));
                } catch (final IllegalArgumentException e) {
                    throw new IOException("Invalid orpheus audio snapshot format: " + file, e);
                }
            }
        }
        return new FrameSnapshot(nodes, zoom, translateX, translateY);
    }

    private static FrameSnapshot parseSvgFrame(final File svgFile) throws IOException {
        final String content = Files.readString(svgFile.toPath(), StandardCharsets.UTF_8);
        final Map<String, NodeSnapshot> nodes = new LinkedHashMap<>();

        double zoom = 1.0;
        double translateX = 0.0;
        double translateY = 0.0;
        final Matcher camMatcher = SVG_CAMERA_PATTERN.matcher(content);
        if (camMatcher.find()) {
            try {
                translateX = Double.parseDouble(camMatcher.group(1));
                translateY = Double.parseDouble(camMatcher.group(2));
                zoom = Double.parseDouble(camMatcher.group(3));
            } catch (final NumberFormatException ignored) {
                // Keep default camera transform if unparseable
            }
        }

        final Matcher nodeMatcher = SVG_NODE_PATTERN.matcher(content);
        while (nodeMatcher.find()) {
            final String nodeId = nodeMatcher.group(1);
            final String cssClasses = nodeMatcher.group(2);
            final double x = Double.parseDouble(nodeMatcher.group(3));
            final double y = Double.parseDouble(nodeMatcher.group(4));
            final String groupBody = nodeMatcher.group(0);

            final int col = Math.max(0, (int) Math.round((x - 40.0) / 270.0));
            final int rowTenths = Math.max(0, (int) Math.round(((y - 92.0) / 60.0) * 10.0));

            String titleText = nodeId;
            final Matcher titleMatcher = SVG_TITLE_PATTERN.matcher(groupBody);
            if (titleMatcher.find()) {
                titleText = titleMatcher.group(1);
            }

            boolean recentHighlight = false;
            final Matcher cardBgMatcher = SVG_CARD_BG_PATTERN.matcher(groupBody);
            if (cardBgMatcher.find()) {
                recentHighlight = "#38bdf8".equalsIgnoreCase(cardBgMatcher.group(1).trim());
            }

            String badgeText = "";
            final Matcher badgeMatcher = SVG_BADGE_PATTERN.matcher(groupBody);
            if (badgeMatcher.find()) {
                badgeText = badgeMatcher.group(1).trim();
            }

            final char typeCode = inferSvgTypeCode(titleText, badgeText);
            final int[] execAndTarget = parseBadgeExecutionsAndTarget(badgeText);
            final char statusCode = inferSvgStatusCode(cssClasses, execAndTarget[0]);
            final String identity = col + ":" + nodeId.replaceFirst("^scenario_\\d+_", "");

            nodes.put(identity, new NodeSnapshot(
                    identity,
                    col,
                    rowTenths,
                    typeCode,
                    execAndTarget[0],
                    execAndTarget[1],
                    statusCode,
                    recentHighlight));
        }

        return new FrameSnapshot(nodes, zoom, translateX, translateY);
    }

    private static char inferSvgTypeCode(final String titleText, final String badgeText) {
        if (titleText.contains("◆")) {
            return 'M';
        }
        if (titleText.contains("◼") || badgeText.contains("/")) {
            return 'L';
        }
        return 'N';
    }

    private static char inferSvgStatusCode(final String cssClasses, final int executions) {
        if (cssClasses.contains("failed") && !cssClasses.contains("child_failed")) {
            return 'F';
        }
        if (cssClasses.contains("child_failed")) {
            return 'U';
        }
        if (cssClasses.contains("warning")) {
            return 'W';
        }
        if (cssClasses.contains("known-issue")) {
            return 'K';
        }
        return executions > 0 ? 'P' : 'N';
    }

    private static int[] parseBadgeExecutionsAndTarget(final String badge) {
        if (null == badge || badge.isBlank() || "OFF".equalsIgnoreCase(badge)) {
            return new int[]{0, 1};
        }
        final String cleaned = badge.replace("✓", "").trim();
        final int slashIdx = cleaned.indexOf('/');
        if (slashIdx > 0) {
            final String left = cleaned.substring(0, slashIdx).trim();
            final String rightPart = cleaned.substring(slashIdx + 1).trim();
            final int spaceIdx = rightPart.indexOf(' ');
            final String right = (spaceIdx > 0) ? rightPart.substring(0, spaceIdx) : rightPart;
            try {
                return new int[]{
                        Math.max(0, Integer.parseInt(left)),
                        Math.max(1, Integer.parseInt(right))
                };
            } catch (final NumberFormatException ignored) {
                return new int[]{0, 1};
            }
        }
        final int spaceIdx = cleaned.indexOf(' ');
        if (spaceIdx > 0) {
            try {
                return new int[]{Math.max(0, Integer.parseInt(cleaned.substring(0, spaceIdx).trim())), 1};
            } catch (final NumberFormatException ignored) {
                return new int[]{0, 1};
            }
        }
        return new int[]{0, 1};
    }

    private record NodeSnapshot(
            String identity,
            int col,
            int rowTenths,
            char typeCode,
            int executions,
            int target,
            char statusCode,
            boolean recentHighlight) {

        boolean isTerminal() {
            return 'L' == typeCode || 'M' == typeCode;
        }
    }

    private record FrameSnapshot(
            Map<String, NodeSnapshot> nodes,
            double zoom,
            double translateX,
            double translateY) {

        double coverageRatio() {
            int totalLeaves = 0;
            int coveredLeaves = 0;
            for (final NodeSnapshot node : nodes.values()) {
                if (node.isTerminal()) {
                    totalLeaves++;
                    if (node.executions() >= node.target()) {
                        coveredLeaves++;
                    }
                }
            }
            return totalLeaves > 0 ? (double) coveredLeaves / totalLeaves : 0.0;
        }
    }
}
