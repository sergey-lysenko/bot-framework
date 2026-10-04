package works.lysenko.base.output;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Image-driven sonifier ({@code claude}): "visual change scanner".
 * <p>
 * Every frame is reduced to a coarse colour grid and compared with the previous frame. Regions that changed
 * (a card lighting up, a progress colour shifting) sound a note:
 * <ul>
 *     <li>horizontal position selects pitch on a pentatonic scale (tree depth runs left to right, so deeper
 *     levels sound higher);</li>
 *     <li>amount of change in the column selects loudness;</li>
 *     <li>how "green" (close to target) the changed pixels are selects brightness: amber changes sound soft and
 *     pure, completed green changes sound bright with overtones.</li>
 * </ul>
 * The first frame is the silent baseline. Output depends only on pixel data, hence is deterministic.
 */
final class ClaudeSonifier extends AbstractTreeSonifier {

    static final String MODE = "claude";

    private static final int COLUMNS = 15;
    private static final int ROWS = 12;
    private static final int SAMPLES_PER_CELL_AXIS = 6;
    private static final int CHANGE_THRESHOLD = 48;
    private static final int[] PENTATONIC = {0, 2, 4, 7, 9};
    private static final int BASE_MIDI = 48;
    private static final double BASE_AMPLITUDE = 0.06;
    private static final double AMPLITUDE_PER_ROOT_CELL = 0.06;
    private static final double MAX_AMPLITUDE = 0.22;

    @Override
    public String mode() {

        return MODE;
    }

    @Override
    protected Map<Integer, List<AudioNote>> compose(final List<File> frames, final int samplesPerFrame)
            throws IOException {

        final Map<Integer, List<AudioNote>> notesByFrame = new HashMap<>();
        int[][] previous = null;
        for (int frameIndex = 0; frameIndex < frames.size(); frameIndex++) {
            final int[][] current = grid(frames.get(frameIndex));
            if (null != previous) {
                final List<AudioNote> notes = notes(previous, current, (long) frameIndex * samplesPerFrame);
                if (!notes.isEmpty()) {
                    notesByFrame.put(frameIndex, notes);
                }
            }
            previous = current;
        }
        return notesByFrame;
    }

    private static List<AudioNote> notes(final int[][] previous, final int[][] current, final long startSample) {

        final List<AudioNote> notes = new ArrayList<>();
        for (int column = 0; column < COLUMNS; column++) {
            int changed = 0;
            double greenness = 0.0;
            for (int row = 0; row < ROWS; row++) {
                final int index = row * COLUMNS + column;
                if (distance(previous[index], current[index]) >= CHANGE_THRESHOLD) {
                    changed++;
                    // progress colours run amber (g - r < 0) to green (g - r > 0)
                    greenness += Math.max(0.0, Math.min(1.0, (current[index][1] - current[index][0] + 90) / 255.0));
                }
            }
            if (0 < changed) {
                final int midi = BASE_MIDI + 12 * (column / PENTATONIC.length) + PENTATONIC[column % PENTATONIC.length];
                final double amplitude = Math.min(MAX_AMPLITUDE,
                        BASE_AMPLITUDE + AMPLITUDE_PER_ROOT_CELL * Math.sqrt(changed));
                notes.add(new AudioNote(startSample, midi, amplitude, greenness / changed));
            }
        }
        return notes;
    }

    private static int distance(final int[] a, final int[] b) {

        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }

    /**
     * Reduces the image to a fixed ROWS x COLUMNS grid of average RGB values, independent of image size.
     */
    private static int[][] grid(final File file) throws IOException {

        final BufferedImage image = ImageIO.read(file);
        if (null == image) throw new IOException("Unable to read progression frame: " + file);
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[][] cells = new int[ROWS * COLUMNS][3];
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                long red = 0;
                long green = 0;
                long blue = 0;
                for (int sy = 0; sy < SAMPLES_PER_CELL_AXIS; sy++) {
                    final int y = (int) Math.min(height - 1L,
                            ((row * SAMPLES_PER_CELL_AXIS + sy + 0.5) * height) / (ROWS * SAMPLES_PER_CELL_AXIS));
                    for (int sx = 0; sx < SAMPLES_PER_CELL_AXIS; sx++) {
                        final int x = (int) Math.min(width - 1L,
                                ((column * SAMPLES_PER_CELL_AXIS + sx + 0.5) * width) / (COLUMNS * SAMPLES_PER_CELL_AXIS));
                        final int rgb = image.getRGB(x, y);
                        red += (rgb >> 16) & 0xFF;
                        green += (rgb >> 8) & 0xFF;
                        blue += rgb & 0xFF;
                    }
                }
                final int samples = SAMPLES_PER_CELL_AXIS * SAMPLES_PER_CELL_AXIS;
                final int[] cell = cells[row * COLUMNS + column];
                cell[0] = (int) (red / samples);
                cell[1] = (int) (green / samples);
                cell[2] = (int) (blue / samples);
            }
        }
        return cells;
    }
}
