package works.lysenko.base.output.sonifier;

record AudioNote(long startSample, int midi, double amplitude, double brightness, boolean isSwoosh) {

    AudioNote(final long startSample, final int midi, final double amplitude, final double brightness) {
        this(startSample, midi, amplitude, brightness, false);
    }

    AudioNote(final long startSample, final int midi, final double amplitude) {
        this(startSample, midi, amplitude, 0.0, false);
    }
}
