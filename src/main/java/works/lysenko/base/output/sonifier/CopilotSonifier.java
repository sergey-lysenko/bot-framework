package works.lysenko.base.output;

import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data-driven sonifier ({@code copilot}): each new leaf execution plays a note whose pitch derives from the leaf path
 * and whose loudness reflects progress to target. Relies on per-frame leaf state snapshots written alongside frames.
 */
final class CopilotSonifier extends AbstractTreeSonifier {

    static final String MODE = "copilot";

    private static final int FRAMES_PER_BEAT = 5;
    private static final double NORMAL_AMPLITUDE = 0.10;
    private static final double TARGET_AMPLITUDE = 0.20;
    private static final int[] SCALE = {0, 2, 4, 7, 9};
    private static final int[] TRIAD = {0, 2, 4};

    static File eventsFile(final File frame) {

        return new File(new File(frame.getParentFile(), "events"),
                frame.getName().replaceFirst("\\.png$", ".events"));
    }

    @Override
    public String mode() {

        return MODE;
    }

    @Override
    public void capture(final File frame, final TreeLayout layout, final int target) throws IOException {

        final File snapshot = eventsFile(frame);
        Files.createDirectories(snapshot.toPath().getParent());
        final Map<String, NodeData> leaves = new TreeMap<>();
        for (final NodeData node : layout.nodes()) {
            final ScenarioType type = (null != node.result()) ? node.result().getScenarioType() : null;
            if ((ScenarioType.LEAF == type || ScenarioType.MONO == type) && node.children().isEmpty()) {
                leaves.put(path(node), node);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                snapshot.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (final Map.Entry<String, NodeData> entry : leaves.entrySet()) {
                final NodeData node = entry.getValue();
                final int executions = (null != node.result()) ? node.result().getExecutions() : 0;
                final int leafTarget = (ScenarioType.MONO == node.result().getScenarioType()) ? 1 : Math.max(1, target);
                writer.write(Base64.getUrlEncoder().withoutPadding().encodeToString(
                        entry.getKey().getBytes(StandardCharsets.UTF_8)));
                writer.write('\t');
                writer.write(Integer.toString(executions));
                writer.write('\t');
                writer.write(Integer.toString(leafTarget));
                writer.newLine();
            }
        }
    }

    @Override
    public boolean canSonify(final List<File> frames) {

        return !frames.isEmpty() && frames.stream().allMatch(frame -> eventsFile(frame).isFile());
    }

    @Override
    protected Map<Integer, List<AudioNote>> compose(final List<File> frames, final int samplesPerFrame)
            throws IOException {

        final Map<String, Integer> previous = new HashMap<>();
        final Map<Integer, List<AudioNote>> notesByFrame = new HashMap<>();
        final List<String> identities = new ArrayList<>();
        for (final File frame : frames) {
            identities.addAll(readSnapshot(eventsFile(frame)).keySet());
        }
        final List<String> orderedIdentities = identities.stream().distinct().sorted().toList();
        final Map<String, Integer> identityOrder = new HashMap<>();
        for (int i = 0; i < orderedIdentities.size(); i++) {
            identityOrder.put(orderedIdentities.get(i), i);
        }

        for (int frameIndex = 0; frameIndex < frames.size(); frameIndex++) {
            final Map<String, LeafState> current = readSnapshot(eventsFile(frames.get(frameIndex)));
            for (final Map.Entry<String, LeafState> entry : current.entrySet()) {
                final String identity = entry.getKey();
                final LeafState state = entry.getValue();
                final int oldExecutions = previous.getOrDefault(identity, 0);
                final int gained = Math.max(0, Math.min(state.executions(), state.target()) - oldExecutions);
                if (gained > 0) {
                    final int beatFrame = Math.min(frames.size() - 1,
                            ((frameIndex + FRAMES_PER_BEAT - 1) / FRAMES_PER_BEAT) * FRAMES_PER_BEAT);
                    final int midi = pitch(identity, identityOrder.get(identity));
                    final double completion = Math.min(1.0, (double) state.executions() / state.target());
                    final double amplitude = state.executions() >= state.target()
                            ? TARGET_AMPLITUDE
                            : NORMAL_AMPLITUDE * Math.sqrt(gained) * (0.5 + 0.5 * completion);
                    notesByFrame.computeIfAbsent(beatFrame, ignored -> new ArrayList<>())
                            .add(new AudioNote((long) beatFrame * samplesPerFrame, midi, amplitude));
                }
            }
            current.forEach((identity, state) -> previous.put(identity, state.executions()));
        }
        return notesByFrame;
    }

    private static Map<String, LeafState> readSnapshot(final File file) throws IOException {

        final Map<String, LeafState> result = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while (null != (line = reader.readLine())) {
                final String[] fields = line.split("\\t", -1);
                if (3 != fields.length) throw new IOException("Invalid tree progression audio snapshot: " + file);
                final String identity;
                final int executions;
                final int target;
                try {
                    identity = new String(Base64.getUrlDecoder().decode(fields[0]), StandardCharsets.UTF_8);
                    executions = Integer.parseInt(fields[1]);
                    target = Integer.parseInt(fields[2]);
                } catch (final IllegalArgumentException e) {
                    throw new IOException("Invalid tree progression audio snapshot: " + file, e);
                }
                result.put(identity, new LeafState(Math.max(0, executions), Math.max(1, target)));
            }
        }
        return result;
    }

    private static String path(final NodeData node) {

        final List<String> nodes = new ArrayList<>();
        NodeData current = node;
        while (null != current) {
            nodes.add(current.label());
            current = current.parent();
        }
        java.util.Collections.reverse(nodes);
        final List<String> parts = new ArrayList<>();
        parts.add(node.group());
        parts.addAll(nodes);
        return String.join("/", parts);
    }

    private static int pitch(final String identity, final int index) {

        final String[] path = identity.split("/");
        final String branch = (path.length > 1) ? path[1] : path[0];
        final int tonic = SCALE[Math.floorMod(branch.hashCode(), SCALE.length)];
        final int chord = TRIAD[Math.floorMod(identity.hashCode() + index, TRIAD.length)];
        final int depthRegister = Math.min(2, Math.max(0, path.length - 3));
        return 48 + tonic + chord + 12 * depthRegister;
    }

    private record LeafState(int executions, int target) {}
}
