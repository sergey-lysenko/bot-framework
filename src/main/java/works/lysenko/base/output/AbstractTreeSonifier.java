package works.lysenko.base.output;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Template for sonifiers: validates input and renders; subclasses only compose notes.
 */
abstract class AbstractTreeSonifier implements TreeSonifier {

    @Override
    public boolean canSonify(final List<File> frames) {

        return !frames.isEmpty();
    }

    @Override
    public final void writeWav(final List<File> frames, final File output, final int frameRate) throws IOException {

        if (frames.isEmpty() || frameRate < 1) {
            throw new IllegalArgumentException("Audio requires frames and a positive frame rate");
        }
        if (WavSynth.SAMPLE_RATE % frameRate != 0) {
            throw new IllegalArgumentException("Frame rate must divide the audio sample rate");
        }
        final int samplesPerFrame = WavSynth.SAMPLE_RATE / frameRate;
        final long totalSamples = (long) frames.size() * samplesPerFrame + WavSynth.noteSamples();
        WavSynth.write(output, totalSamples, samplesPerFrame, compose(frames, samplesPerFrame));
    }

    /**
     * Decides which notes sound at which frame.
     *
     * @param frames          frames in playback order
     * @param samplesPerFrame audio samples per video frame
     * @return notes keyed by the index of the frame they start at
     * @throws IOException on read failure
     */
    protected abstract Map<Integer, List<AudioNote>> compose(List<File> frames, int samplesPerFrame) throws IOException;
}
