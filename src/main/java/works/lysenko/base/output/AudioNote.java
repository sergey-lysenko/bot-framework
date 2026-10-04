package works.lysenko.base.output;

/**
 * A single synthesized note.
 *
 * @param startSample first sample of the note
 * @param midi        MIDI pitch number
 * @param amplitude   peak amplitude (0..1)
 * @param brightness  0 for a pure sine; up to 1 for a rich, harmonic-heavy timbre
 */
record AudioNote(long startSample, int midi, double amplitude, double brightness) {

    AudioNote(final long startSample, final int midi, final double amplitude) {
        this(startSample, midi, amplitude, 0.0);
    }
}
