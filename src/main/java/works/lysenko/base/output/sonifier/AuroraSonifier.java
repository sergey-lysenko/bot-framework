package works.lysenko.base.output.sonifier;

import works.lysenko.base.output.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Ambient sonifier ({@code aurora}): Creates an ethereal soundscape.
 * Uses a Hirajoshi pentatonic scale with low brightness to create soft, sustained pads.
 */
public final class AuroraSonifier extends AbstractTreeSonifier {

    static final String MODE = "aurora";

    private static final int[] SCALE = {0, 2, 3, 7, 8}; // Hirajoshi scale
    private static final int PAD_MIDI_BASE = 45;
    private static final double PAD_AMPLITUDE = 0.18;
    private static final int FRAMES_PER_PAD = 15;

    public static File snapshotFile(final File frame) {
        return new File(new File(frame.getParentFile(), "aurora_events"),
                frame.getName().replaceFirst("\\.(png|svg)$", ".aurora"));
    }

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public void capture(final File frame, final TreeLayout layout, final int target) throws IOException {
        final File snapshot = snapshotFile(frame);
        Files.createDirectories(snapshot.toPath().getParent());

        final Map<Integer, ColumnStats> stats = new TreeMap<>();
        for (final NodeData node : layout.nodes()) {
            final int col = node.col();
            final int executions = (null != node.result()) ? node.result().getExecutions() : 0;
            final ScenarioType type = (null != node.result()) ? node.result().getScenarioType() : null;
            final int leafTarget = (ScenarioType.MONO == type) ? 1 : Math.max(1, target);
            
            final boolean isCompleted = executions >= leafTarget;
            final boolean isActive = executions > 0 && executions < leafTarget;
            
            stats.computeIfAbsent(col, k -> new ColumnStats(0, 0, 0)).add(isActive, isCompleted);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                snapshot.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (final Map.Entry<Integer, ColumnStats> entry : stats.entrySet()) {
                final ColumnStats s = entry.getValue();
                writer.write(entry.getKey() + "\t" + s.total + "\t" + s.active + "\t" + s.completed);
                writer.newLine();
            }
        }
    }

    @Override
    public boolean canSonify(final List<File> frames) {
        return !frames.isEmpty() && frames.stream().allMatch(frame -> snapshotFile(frame).isFile());
    }

    @Override
    protected Map<Integer, List<AudioNote>> compose(final List<File> frames, final int samplesPerFrame) throws IOException {
        final Map<Integer, List<AudioNote>> notesByFrame = new HashMap<>();
        final Map<Integer, Integer> previousCompleted = new HashMap<>();

        for (int frameIndex = 0; frameIndex < frames.size(); frameIndex++) {
            final Map<Integer, ColumnStats> current = readSnapshot(snapshotFile(frames.get(frameIndex)));
            final List<AudioNote> frameNotes = new ArrayList<>();
            final long startSample = (long) frameIndex * samplesPerFrame;

            for (final Map.Entry<Integer, ColumnStats> entry : current.entrySet()) {
                final int col = entry.getKey();
                final ColumnStats stats = entry.getValue();
                final int oldCompleted = previousCompleted.getOrDefault(col, 0);

                if (stats.completed > 0 && frameIndex % FRAMES_PER_PAD == 0) {
                    final int midi = PAD_MIDI_BASE + 12 * (col / SCALE.length) + SCALE[col % SCALE.length];
                    final double amp = PAD_AMPLITUDE * Math.min(3, stats.completed) / 3.0;
                    frameNotes.add(new AudioNote(startSample, midi, amp, 0.1));
                }

                final int gained = stats.completed - oldCompleted;
                if (gained > 0) {
                    for (int i = 0; i < 2; i++) {
                        final int noteOffset = SCALE[(col + i * 2) % SCALE.length] + 12 * ((col + i * 2) / SCALE.length);
                        final int midi = PAD_MIDI_BASE + 12 + noteOffset;
                        final double amp = PAD_AMPLITUDE * Math.sqrt(gained);
                        frameNotes.add(new AudioNote(startSample, midi, amp, 0.3));
                    }
                }
                previousCompleted.put(col, stats.completed);
            }

            if (!frameNotes.isEmpty()) {
                notesByFrame.put(frameIndex, frameNotes);
            }
        }
        return notesByFrame;
    }

    private static Map<Integer, ColumnStats> readSnapshot(final File file) throws IOException {
        final Map<Integer, ColumnStats> result = new TreeMap<>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while (null != (line = reader.readLine())) {
                final String[] parts = line.split("\t");
                if (parts.length >= 4) {
                    final int col = Integer.parseInt(parts[0]);
                    final int total = Integer.parseInt(parts[1]);
                    final int active = Integer.parseInt(parts[2]);
                    final int completed = Integer.parseInt(parts[3]);
                    result.put(col, new ColumnStats(total, active, completed));
                }
            }
        }
        return result;
    }

    private static class ColumnStats {
        int total;
        int active;
        int completed;

        ColumnStats(final int total, final int active, final int completed) {
            this.total = total;
            this.active = active;
            this.completed = completed;
        }

        void add(final boolean isActive, final boolean isCompleted) {
            this.total++;
            if (isActive) this.active++;
            if (isCompleted) this.completed++;
        }
    }
}
