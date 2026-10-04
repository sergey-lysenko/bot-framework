package works.lysenko.base.output;

import works.lysenko.base.output.TreeHtml.TreeLayout;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Strategy turning tree progression frames into an audio track.
 * To add a new variant implement this interface (usually by extending {@link AbstractTreeSonifier})
 * and register it in {@link TreeProgressionAudio}.
 */
interface TreeSonifier {

    /**
     * @return value of the {@code .progression.tree.sonification} property activating this sonifier
     */
    String mode();

    /**
     * Called when a frame PNG is written, allowing the sonifier to persist additional data for it.
     *
     * @param frame  written frame file
     * @param layout layout the frame was rendered from
     * @param target target executions count
     * @throws IOException on write failure
     */
    default void capture(final File frame, final TreeLayout layout, final int target) throws IOException {
    }

    /**
     * @param frames frames about to be sonified
     * @return true if all data required to sonify the frames is available
     */
    boolean canSonify(List<File> frames);

    /**
     * Writes a WAV track aligned with the frames.
     *
     * @param frames    frames in playback order
     * @param output    destination WAV file
     * @param frameRate video frame rate; must divide the audio sample rate
     * @throws IOException on read/write failure
     */
    void writeWav(List<File> frames, File output, int frameRate) throws IOException;
}
