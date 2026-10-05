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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Structural/Geometric sonifier ({@code gemini}): "harmonic cascade".
 * <p>
 * Evaluates the execution state of the tree structure column by column (depth).
 * Creates a sweeping ambient soundscape:
 * <ul>
 *     <li>Completed nodes form a low sustained drone.</li>
 *     <li>Active nodes pulse rhythmically in mid-register, panning across columns.</li>
 *     <li>Newly completed nodes trigger bright high-register chimes.</li>
 * </ul>
 * Pitch is determined by column index mapped to a Lydian scale.
 */
final class GeminiSonifier extends AbstractTreeSonifier {

    static final String MODE = "gemini";

    private static final int[] SCALE = {0, 2, 4, 6, 7, 9, 11}; // Lydian mode
    private static final int DRONE_MIDI_BASE = 36;
    private static final int PULSE_MIDI_BASE = 48;
    private static final int CHIME_MIDI_BASE = 60;
    private static final double DRONE_AMPLITUDE = 0.12;
    private static final double PULSE_AMPLITUDE = 0.10;
    private static final double CHIME_AMPLITUDE = 0.15;
    private static final int FRAMES_PER_DRONE = 10;

    static File snapshotFile(final File frame) {
        return new File(new File(frame.getParentFile(), "gemini_events"),
                frame.getName().replaceFirst("\\.png$", ".gemini"));
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
        
        int maxColumns = 1;
        // First pass: find max columns
        for (final File frame : frames) {
            final Map<Integer, ColumnStats> current = readSnapshot(snapshotFile(frame));
            for (final int col : current.keySet()) {
                maxColumns = Math.max(maxColumns, col + 1);
            }
        }

        for (int frameIndex = 0; frameIndex < frames.size(); frameIndex++) {
            final Map<Integer, ColumnStats> current = readSnapshot(snapshotFile(frames.get(frameIndex)));
            final List<AudioNote> frameNotes = new ArrayList<>();
            final long startSample = (long) frameIndex * samplesPerFrame;

            for (final Map.Entry<Integer, ColumnStats> entry : current.entrySet()) {
                final int col = entry.getKey();
                final ColumnStats stats = entry.getValue();
                final int oldCompleted = previousCompleted.getOrDefault(col, 0);

                // Drone: play every FRAMES_PER_DRONE frames if there are any completed nodes in this column
                if (stats.completed > 0 && frameIndex % FRAMES_PER_DRONE == 0) {
                    final int midi = DRONE_MIDI_BASE + 12 * (col / SCALE.length) + SCALE[col % SCALE.length];
                    frameNotes.add(new AudioNote(startSample, midi, DRONE_AMPLITUDE * Math.min(3, stats.completed) / 3.0, 0.0));
                }

                // Chime: play when new nodes are completed
                final int gained = stats.completed - oldCompleted;
                if (gained > 0) {
                    // Play a Lydian triad for the chime
                    for (int i = 0; i < 3; i++) {
                        final int noteOffset = SCALE[(col + i * 2) % SCALE.length] + 12 * ((col + i * 2) / SCALE.length);
                        final int midi = CHIME_MIDI_BASE + noteOffset;
                        final double amp = CHIME_AMPLITUDE * Math.sqrt(gained);
                        frameNotes.add(new AudioNote(startSample, midi, amp, 1.0)); // High brightness
                    }
                }
                
                previousCompleted.put(col, stats.completed);
            }
            
            // Pulse: sweeping arpeggio across columns
            final int pulseCol = frameIndex % maxColumns;
            final ColumnStats pulseStats = current.getOrDefault(pulseCol, new ColumnStats(0, 0, 0));
            if (pulseStats.active > 0) {
                final int midi = PULSE_MIDI_BASE + 12 * (pulseCol / SCALE.length) + SCALE[pulseCol % SCALE.length];
                frameNotes.add(new AudioNote(startSample, midi, PULSE_AMPLITUDE * Math.min(3, pulseStats.active) / 3.0, 0.2));
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
                final String[] fields = line.split("\\t", -1);
                if (4 != fields.length) throw new IOException("Invalid gemini audio snapshot: " + file);
                try {
                    final int col = Integer.parseInt(fields[0]);
                    final int total = Integer.parseInt(fields[1]);
                    final int active = Integer.parseInt(fields[2]);
                    final int completed = Integer.parseInt(fields[3]);
                    result.put(col, new ColumnStats(total, active, completed));
                } catch (final NumberFormatException e) {
                    throw new IOException("Invalid gemini audio snapshot format: " + file, e);
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
            total++;
            if (isActive) active++;
            if (isCompleted) completed++;
        }
    }
}