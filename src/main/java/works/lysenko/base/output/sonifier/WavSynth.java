package works.lysenko.base.output;

import javax.sound.sampled.AudioFormat;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic additive note synthesizer rendering mono 16-bit PCM WAV files.
 */
final class WavSynth {

    static final int SAMPLE_RATE = 44_100;
    static final int NOTE_DURATION_MILLIS = 400;
    private static final int NOTE_ATTACK_MILLIS = 12;
    private static final double NOTE_DECAY_SECONDS = 0.14;

    private WavSynth() {
    }

    /**
     * Renders notes into a WAV file.
     *
     * @param output         destination file
     * @param totalSamples   total number of samples to write
     * @param samplesPerFrame samples per video frame
     * @param notesByFrame   notes grouped by the frame at which they start
     * @throws IOException on write failure
     */
    static void write(
            final File output,
            final long totalSamples,
            final int samplesPerFrame,
            final Map<Integer, List<AudioNote>> notesByFrame) throws IOException {

        final int noteSamples = noteSamples();
        final long dataSize = totalSamples * Short.BYTES;
        if (dataSize > Integer.MAX_VALUE - 36L) {
            throw new IOException("Progression audio exceeds the WAV file size limit");
        }
        final AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(output.toPath())))) {
            writeHeader(stream, dataSize, format);
            final int attackSamples = SAMPLE_RATE * NOTE_ATTACK_MILLIS / 1000;
            final List<AudioNote> activeNotes = new ArrayList<>();
            int currentFrame = -1;
            for (long sample = 0; sample < totalSamples; sample++) {
                final int frameIndex = (int) (sample / samplesPerFrame);
                if (frameIndex != currentFrame) {
                    currentFrame = frameIndex;
                    activeNotes.addAll(notesByFrame.getOrDefault(frameIndex, List.of()));
                }
                for (int noteIndex = activeNotes.size() - 1; noteIndex >= 0; noteIndex--) {
                    if (sample - activeNotes.get(noteIndex).startSample() >= noteSamples) {
                        activeNotes.remove(noteIndex);
                    }
                }
                double value = 0.0;
                final double chordScale = activeNotes.isEmpty() ? 0.0 : 1.0 / Math.sqrt(activeNotes.size());
                for (final AudioNote note : activeNotes) {
                    final long age = sample - note.startSample();
                    final double attack = Math.min(1.0, (double) age / attackSamples);
                    final double decay = Math.exp(-age / (SAMPLE_RATE * NOTE_DECAY_SECONDS));
                    final double envelope = attack * decay;
                    final double frequency = 440.0 * Math.pow(2.0, (note.midi() - 69) / 12.0);
                    final double phase = 2.0 * Math.PI * frequency * age / SAMPLE_RATE;
                    double wave = Math.sin(phase);
                    if (0.0 < note.brightness()) {
                        wave = (wave + note.brightness() * (0.5 * Math.sin(2.0 * phase) + 0.25 * Math.sin(3.0 * phase)))
                                / (1.0 + 0.75 * note.brightness());
                    }
                    value += note.amplitude() * chordScale * envelope * wave;
                }
                final int pcm = (int) Math.round(Math.max(-1.0, Math.min(1.0, value)) * Short.MAX_VALUE);
                writeLittleEndianShort(stream, pcm);
            }
        }
    }

    static int noteSamples() {

        return SAMPLE_RATE * NOTE_DURATION_MILLIS / 1000;
    }

    private static void writeHeader(final DataOutputStream stream, final long dataSize, final AudioFormat format)
            throws IOException {

        stream.writeBytes("RIFF");
        writeLittleEndianInt(stream, (int) (dataSize + 36));
        stream.writeBytes("WAVEfmt ");
        writeLittleEndianInt(stream, 16);
        writeLittleEndianShort(stream, 1);
        writeLittleEndianShort(stream, format.getChannels());
        writeLittleEndianInt(stream, (int) format.getSampleRate());
        writeLittleEndianInt(stream, SAMPLE_RATE * format.getChannels() * Short.BYTES);
        writeLittleEndianShort(stream, format.getChannels() * Short.BYTES);
        writeLittleEndianShort(stream, format.getSampleSizeInBits());
        stream.writeBytes("data");
        writeLittleEndianInt(stream, (int) dataSize);
    }

    private static void writeLittleEndianInt(final DataOutputStream stream, final int value) throws IOException {

        stream.writeByte(value & 0xFF);
        stream.writeByte((value >>> 8) & 0xFF);
        stream.writeByte((value >>> 16) & 0xFF);
        stream.writeByte((value >>> 24) & 0xFF);
    }

    private static void writeLittleEndianShort(final DataOutputStream stream, final int value) throws IOException {

        stream.writeByte(value & 0xFF);
        stream.writeByte((value >>> 8) & 0xFF);
    }
}
