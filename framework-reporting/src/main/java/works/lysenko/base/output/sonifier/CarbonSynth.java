package works.lysenko.base.output.sonifier;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static works.lysenko.util.data.strs.Swap.s;

/**
 * Deterministic multi-timbral stereo synthesizer rendering 44.1 kHz, 16-bit stereo PCM WAV tracks
 * for {@link CarbonSonifier}.
 * <p>
 * Combines 2-operator FM percussion/bells, chorus-detuned stereo pads, continuous exponential
 * pitch-glide sweeps, a cross-feedback warm stereo delay diffuser, and soft-knee mastering limiting.
 */
final class CarbonSynth {

    static final int SAMPLE_RATE = 44_100;
    static final int CHANNELS = 2;
    static final int TAIL_SAMPLES = SAMPLE_RATE; // 1.0s stereo reverb/chord ring-out on final frame

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final int MAX_ACTIVE_VOICES = 28;
    private static final int IO_BUFFER_BYTES = 16_384;

    // Mutually prime stereo ping-pong delay lengths (~149.0 ms Left, ~211.1 ms Right)
    private static final int DELAY_LEFT_SAMPLES = 6_571;
    private static final int DELAY_RIGHT_SAMPLES = 9_311;
    private static final double DELAY_FEEDBACK = 0.25;
    private static final double DELAY_DAMPING = 0.72;
    private static final double DELAY_WET_MIX = 0.19;
    private static final double MASTER_DRIVE = 1.45;
    private static final double PCM_HEADROOM = 31_600.0;

    private CarbonSynth() {
    }

    /**
     * Distinct synthesis voice models tailored to scenario tree telemetry roles.
     */
    enum VoiceKind {
        /** Warm chorus-detuned stereo pad with slow cosine attack and long release. */
        HORIZON_PAD,
        /** Short 2-operator FM woody marimba pluck for branch traversal steps. */
        MARIMBA_PLUCK,
        /** Luminous 2-operator FM celesta/harp chime for newly discovered leaves. */
        CELESTA_BLOOM,
        /** Crisp, short glass-harmonic tick for Monte-Carlo leaf over-executions. */
        TELEMETRY_TICK,
        /** Grounded sub-bass pulse with subtle pitch transient for limbo cycle boundaries. */
        SUB_HEARTBEAT,
        /** Smooth upward harmonic frequency sweep when inactive branches are pruned. */
        UPDRAFT_SWEEP,
        /** Visceral downward pitch-drop with metallic FM bite on unexpected failures. */
        GRAVITY_DROP
    }

    /**
     * Immutable specification of a scheduled stereo synthesis event.
     *
     * @param startSample     exact sample index at which the voice begins
     * @param durationSamples voice duration in audio samples
     * @param kind            synthesis instrument model
     * @param midiPitch       fractional or integer MIDI note number (69.0 = A4 = 440 Hz)
     * @param amplitude       peak linear voice gain prior to stereo pan law
     * @param brightness      timbral FM index / harmonic brightness in {@code [0.0, 1.0]}
     * @param pan             stereo position in {@code [-1.0, +1.0]} ({@code -1.0} = Left, {@code +1.0} = Right)
     */
    record SynthEvent(
            long startSample,
            int durationSamples,
            VoiceKind kind,
            double midiPitch,
            double amplitude,
            double brightness,
            double pan) {
    }

    /**
     * Renders frame-indexed stereo synthesis events into a deterministic 16-bit stereo WAV file.
     *
     * @param output          destination WAV file
     * @param totalSamples    total stereo sample frames to render (including ring-out tail)
     * @param samplesPerFrame audio sample frames per video frame
     * @param eventsByFrame   scheduled synthesis events keyed by video frame index
     * @throws IOException if writing fails or WAV size limits are exceeded
     */
    static void writeStereoWav(
            final File output,
            final long totalSamples,
            final int samplesPerFrame,
            final Map<Integer, List<SynthEvent>> eventsByFrame) throws IOException {

        final long dataBytes = totalSamples * CHANNELS * Short.BYTES;
        if (dataBytes > Integer.MAX_VALUE - 36L) {
            throw new IOException("Progression stereo audio exceeds the WAV file size limit");
        }

        final List<ActiveVoice> activeVoices = new ArrayList<>(MAX_ACTIVE_VOICES);
        final double[] delayBufL = new double[DELAY_LEFT_SAMPLES];
        final double[] delayBufR = new double[DELAY_RIGHT_SAMPLES];
        int delayIdxL = 0;
        int delayIdxR = 0;
        double filterStateL = 0.0;
        double filterStateR = 0.0;

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(output.toPath()), IO_BUFFER_BYTES)) {
            writeWavHeader(out, dataBytes);
            final byte[] pcmBuffer = new byte[IO_BUFFER_BYTES];
            int bufPos = 0;
            int currentFrame = -1;

            for (long sample = 0; sample < totalSamples; sample++) {
                final int frameIndex = (int) (sample / samplesPerFrame);
                if (frameIndex != currentFrame) {
                    currentFrame = frameIndex;
                    final List<SynthEvent> incoming = eventsByFrame.get(frameIndex);
                    if (null != incoming) {
                        for (int i = 0; i < incoming.size(); i++) {
                            spawnVoice(activeVoices, incoming.get(i), sample);
                        }
                    }
                }

                double dryL = 0.0;
                double dryR = 0.0;

                for (int i = activeVoices.size() - 1; i >= 0; i--) {
                    final ActiveVoice v = activeVoices.get(i);
                    if (sample < v.startSample) {
                        continue;
                    }
                    if (v.age >= v.durationSamples) {
                        activeVoices.remove(i);
                        continue;
                    }
                    v.step();
                    dryL += v.outL;
                    dryR += v.outR;
                }

                // Warm cross-feedback stereo delay diffuser
                final double tapL = delayBufL[delayIdxL];
                final double tapR = delayBufR[delayIdxR];
                filterStateL = DELAY_DAMPING * filterStateL + (1.0 - DELAY_DAMPING) * tapL;
                filterStateR = DELAY_DAMPING * filterStateR + (1.0 - DELAY_DAMPING) * tapR;

                delayBufL[delayIdxL] = dryL + DELAY_FEEDBACK * filterStateR;
                delayBufR[delayIdxR] = dryR + DELAY_FEEDBACK * filterStateL;
                if (++delayIdxL == DELAY_LEFT_SAMPLES) {
                    delayIdxL = 0;
                }
                if (++delayIdxR == DELAY_RIGHT_SAMPLES) {
                    delayIdxR = 0;
                }

                final double mixL = Math.tanh((dryL + DELAY_WET_MIX * filterStateL) * MASTER_DRIVE);
                final double mixR = Math.tanh((dryR + DELAY_WET_MIX * filterStateR) * MASTER_DRIVE);

                final int pcmL = (int) Math.round(mixL * PCM_HEADROOM);
                final int pcmR = (int) Math.round(mixR * PCM_HEADROOM);

                pcmBuffer[bufPos++] = (byte) (pcmL & 0xFF);
                pcmBuffer[bufPos++] = (byte) ((pcmL >>> 8) & 0xFF);
                pcmBuffer[bufPos++] = (byte) (pcmR & 0xFF);
                pcmBuffer[bufPos++] = (byte) ((pcmR >>> 8) & 0xFF);

                if (bufPos == IO_BUFFER_BYTES) {
                    out.write(pcmBuffer, 0, bufPos);
                    bufPos = 0;
                }
            }

            if (bufPos > 0) {
                out.write(pcmBuffer, 0, bufPos);
            }
        }
    }

    static int millisToSamples(final int millis) {
        return Math.max(1, (int) ((long) SAMPLE_RATE * millis / 1000L));
    }

    private static void spawnVoice(
            final List<ActiveVoice> activeVoices,
            final SynthEvent event,
            final long nowSample) {
        if (activeVoices.size() >= MAX_ACTIVE_VOICES) {
            int quietestIndex = 0;
            double quietestEnergy = Double.MAX_VALUE;
            for (int i = 0; i < activeVoices.size(); i++) {
                final ActiveVoice candidate = activeVoices.get(i);
                final double energy = candidate.currentEnergy(nowSample);
                if (energy < quietestEnergy) {
                    quietestEnergy = energy;
                    quietestIndex = i;
                }
            }
            activeVoices.remove(quietestIndex);
        }
        activeVoices.add(new ActiveVoice(event));
    }

    private static void writeWavHeader(final OutputStream out, final long dataBytes) throws IOException {
        final byte[] header = new byte[44];
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        putIntLE(header, 4, (int) (dataBytes + 36L));
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        putIntLE(header, 16, 16);
        putShortLE(header, 20, 1); // PCM
        putShortLE(header, 22, CHANNELS);
        putIntLE(header, 24, SAMPLE_RATE);
        putIntLE(header, 28, SAMPLE_RATE * CHANNELS * Short.BYTES);
        putShortLE(header, 32, CHANNELS * Short.BYTES);
        putShortLE(header, 34, Short.SIZE);
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        putIntLE(header, 40, (int) dataBytes);
        out.write(header);
    }

    private static void putIntLE(final byte[] buf, final int offset, final int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        buf[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        buf[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }

    private static void putShortLE(final byte[] buf, final int offset, final int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >>> 8) & 0xFF);
    }

    /**
     * Stateful per-voice oscillator and envelope generator using fast multiplicative recurrences.
     */
    private static final class ActiveVoice {

        final long startSample;
        final int durationSamples;
        private final VoiceKind kind;
        private final double amplitude;
        private final double gainL;
        private final double gainR;
        private final double padLeftGainL;
        private final double padLeftGainR;
        private final double padRightGainL;
        private final double padRightGainR;

        private final int attackSamples;
        private final int releaseStartSample;

        private double phaseC;
        private double phaseM;
        private double phaseSub;
        private double phaseIncC;
        private double phaseIncM;
        private final double phaseIncDetuneL;
        private final double phaseIncDetuneR;
        private final double pitchGlideMul;

        private double envDecay = 1.0;
        private final double envDecayMul;
        private double modIndex;
        private final double modDecayMul;
        private double octaveShimmer;
        private final double octaveShimmerMul;

        int age;
        double outL;
        double outR;

        ActiveVoice(final SynthEvent event) {
            this.startSample = event.startSample();
            this.durationSamples = Math.max(8, event.durationSamples());
            this.kind = event.kind();
            this.amplitude = Math.max(0.0, event.amplitude());

            final double clampedPan = Math.clamp(event.pan(), -1.0, 1.0);
            final double panAngle = (clampedPan + 1.0) * (Math.PI * 0.25);
            this.gainL = Math.cos(panAngle);
            this.gainR = Math.sin(panAngle);

            final double leftAngle = (Math.max(-1.0, clampedPan - 0.30) + 1.0) * (Math.PI * 0.25);
            final double rightAngle = (Math.min(1.0, clampedPan + 0.30) + 1.0) * (Math.PI * 0.25);
            this.padLeftGainL = Math.cos(leftAngle);
            this.padLeftGainR = Math.sin(leftAngle);
            this.padRightGainL = Math.cos(rightAngle);
            this.padRightGainR = Math.sin(rightAngle);

            final double baseFreq = 440.0 * Math.pow(2.0, (event.midiPitch() - 69.0) / 12.0);
            final double safeBrightness = Math.clamp(event.brightness(), 0.0, 1.0);

            switch (kind) {
                case HORIZON_PAD -> {
                    this.attackSamples = Math.min(durationSamples / 3, millisToSamples(240));
                    this.releaseStartSample = Math.max(attackSamples + 1, durationSamples - millisToSamples(480));
                    this.phaseIncC = TWO_PI * baseFreq / SAMPLE_RATE;
                    this.phaseIncM = TWO_PI * (baseFreq * 2.0) / SAMPLE_RATE;
                    this.phaseIncDetuneL = TWO_PI * (baseFreq * 0.9977) / SAMPLE_RATE;
                    this.phaseIncDetuneR = TWO_PI * (baseFreq * 1.0023) / SAMPLE_RATE;
                    this.pitchGlideMul = 1.0;
                    this.envDecayMul = perSampleDecay(1.40);
                    this.modIndex = 0.18 + 0.22 * safeBrightness;
                    this.modDecayMul = perSampleDecay(0.90);
                    this.octaveShimmer = 0.12 * safeBrightness;
                    this.octaveShimmerMul = perSampleDecay(1.10);
                }
                case MARIMBA_PLUCK -> {
                    this.attackSamples = millisToSamples(3);
                    this.releaseStartSample = Math.max(attackSamples + 1, durationSamples - millisToSamples(24));
                    this.phaseIncC = TWO_PI * baseFreq / SAMPLE_RATE;
                    this.phaseIncM = TWO_PI * (baseFreq * 2.0) / SAMPLE_RATE;
                    this.phaseIncDetuneL = 0.0;
                    this.phaseIncDetuneR = 0.0;
                    this.pitchGlideMul = 1.0;
                    this.envDecayMul = perSampleDecay(0.082);
                    this.modIndex = 0.55 + 1.10 * safeBrightness;
                    this.modDecayMul = perSampleDecay(0.022);
                    this.octaveShimmer = 0.0;
                    this.octaveShimmerMul = 1.0;
                }
                case CELESTA_BLOOM -> {
                    this.attackSamples = millisToSamples(4);
                    this.releaseStartSample = Math.max(attackSamples + 1, durationSamples - millisToSamples(90));
                    this.phaseIncC = TWO_PI * baseFreq / SAMPLE_RATE;
                    this.phaseIncM = TWO_PI * (baseFreq * 3.0) / SAMPLE_RATE;
                    this.phaseIncDetuneL = 0.0;
                    this.phaseIncDetuneR = 0.0;
                    this.pitchGlideMul = 1.0;
                    this.envDecayMul = perSampleDecay(0.240);
                    this.modIndex = 0.65 + 0.85 * safeBrightness;
                    this.modDecayMul = perSampleDecay(0.065);
                    this.octaveShimmer = 0.22 * (0.4 + 0.6 * safeBrightness);
                    this.octaveShimmerMul = perSampleDecay(0.180);
                }
                case TELEMETRY_TICK -> {
                    this.attackSamples = millisToSamples(2);
                    this.releaseStartSample = Math.max(attackSamples + 1, durationSamples - millisToSamples(15));
                    this.phaseIncC = TWO_PI * baseFreq / SAMPLE_RATE;
                    this.phaseIncM = TWO_PI * (baseFreq * 4.0) / SAMPLE_RATE;
                    this.phaseIncDetuneL = 0.0;
                    this.phaseIncDetuneR = 0.0;
                    this.pitchGlideMul = 1.0;
                    this.envDecayMul = perSampleDecay(0.032);
                    this.modIndex = 0.35 + 0.45 * safeBrightness;
                    this.modDecayMul = perSampleDecay(0.011);
                    this.octaveShimmer = 0.0;
                    this.octaveShimmerMul = 1.0;
                }
                case SUB_HEARTBEAT -> {
                    this.attackSamples = millisToSamples(6);
                    this.releaseStartSample = Math.max(attackSamples + 1, durationSamples - millisToSamples(45));
                    final double startHz = baseFreq * 1.14;
                    this.phaseIncC = TWO_PI * startHz / SAMPLE_RATE;
                    this.phaseIncM = TWO_PI * (startHz * 2.0) / SAMPLE_RATE;
                    this.phaseIncDetuneL = 0.0;
                    this.phaseIncDetuneR = 0.0;
                    final int dropWindow = Math.max(16, millisToSamples(45));
                    this.pitchGlideMul = Math.pow(baseFreq / startHz, 1.0 / dropWindow);
                    this.envDecayMul = perSampleDecay(0.105);
                    this.modIndex = 0.40;
                    this.modDecayMul = perSampleDecay(0.040);
                    this.octaveShimmer = 0.32;
                    this.octaveShimmerMul = perSampleDecay(0.095);
                }
                case UPDRAFT_SWEEP -> {
                    this.attackSamples = Math.min(durationSamples / 3, millisToSamples(70));
                    this.releaseStartSample = Math.max(attackSamples + 1, durationSamples - millisToSamples(160));
                    this.phaseIncC = TWO_PI * baseFreq / SAMPLE_RATE;
                    this.phaseIncM = TWO_PI * (baseFreq * 2.0) / SAMPLE_RATE;
                    this.phaseIncDetuneL = 0.0;
                    this.phaseIncDetuneR = 0.0;
                    // Smoothly sweep upward by a pure perfect fifth (1.4983x) across the voice lifetime
                    this.pitchGlideMul = Math.pow(1.498307, 1.0 / durationSamples);
                    this.envDecayMul = perSampleDecay(0.420);
                    this.modIndex = 0.35 + 0.40 * safeBrightness;
                    this.modDecayMul = perSampleDecay(0.220);
                    this.octaveShimmer = 0.25;
                    this.octaveShimmerMul = perSampleDecay(0.320);
                }
                case GRAVITY_DROP -> {
                    this.attackSamples = millisToSamples(4);
                    this.releaseStartSample = Math.max(attackSamples + 1, durationSamples - millisToSamples(90));
                    final double startHz = baseFreq * 1.41421356; // Start a tritone higher and plunge to root
                    this.phaseIncC = TWO_PI * startHz / SAMPLE_RATE;
                    this.phaseIncM = TWO_PI * (startHz * 1.41421356) / SAMPLE_RATE;
                    this.phaseIncDetuneL = 0.0;
                    this.phaseIncDetuneR = 0.0;
                    final int plungeSamples = Math.max(32, millisToSamples(165));
                    this.pitchGlideMul = Math.pow(baseFreq / startHz, 1.0 / plungeSamples);
                    this.envDecayMul = perSampleDecay(0.220);
                    this.modIndex = 1.25 + 0.65 * safeBrightness;
                    this.modDecayMul = perSampleDecay(0.130);
                    this.octaveShimmer = 0.28;
                    this.octaveShimmerMul = perSampleDecay(0.180);
                }
                default -> throw new IllegalStateException(s("Unhandled voice kind: ", kind));
            }
        }

        void step() {
            double env = envDecay;
            if (age < attackSamples) {
                if (VoiceKind.HORIZON_PAD == kind || VoiceKind.UPDRAFT_SWEEP == kind) {
                    env *= 0.5 * (1.0 - Math.cos(Math.PI * age / attackSamples));
                } else {
                    env *= (double) age / attackSamples;
                }
            }
            if (age >= releaseStartSample) {
                final int rem = durationSamples - age;
                final int relLen = Math.max(1, durationSamples - releaseStartSample);
                final double rel = (double) rem / relLen;
                env *= rel * rel;
            }

            if (VoiceKind.HORIZON_PAD == kind) {
                phaseC += phaseIncDetuneL;
                phaseM += phaseIncDetuneR;
                phaseSub += 0.5 * phaseIncC;
                final double sub = 0.28 * Math.sin(phaseSub);
                final double oscL = Math.sin(phaseC + modIndex * Math.sin(2.0 * phaseC)) + sub;
                final double oscR = Math.sin(phaseM + modIndex * Math.sin(2.0 * phaseM)) + sub;
                final double gain = amplitude * env * 0.52;
                outL = gain * (oscL * padLeftGainL + oscR * padRightGainL);
                outR = gain * (oscL * padLeftGainR + oscR * padRightGainR);
            } else {
                if (pitchGlideMul != 1.0) {
                    final boolean applyGlide = (VoiceKind.UPDRAFT_SWEEP == kind)
                            || (VoiceKind.SUB_HEARTBEAT == kind && age < millisToSamples(45))
                            || (VoiceKind.GRAVITY_DROP == kind && age < millisToSamples(165));
                    if (applyGlide) {
                        phaseIncC *= pitchGlideMul;
                        phaseIncM *= pitchGlideMul;
                    }
                }
                phaseC += phaseIncC;
                phaseM += phaseIncM;
                double wave = Math.sin(phaseC + modIndex * Math.sin(phaseM));
                if (octaveShimmer > 0.001) {
                    wave = (wave + octaveShimmer * Math.sin(2.0 * phaseC)) / (1.0 + 0.65 * octaveShimmer);
                }
                final double sampleVal = amplitude * env * wave;
                outL = sampleVal * gainL;
                outR = sampleVal * gainR;
            }

            envDecay *= envDecayMul;
            modIndex *= modDecayMul;
            octaveShimmer *= octaveShimmerMul;
            age++;
        }

        double currentEnergy(final long nowSample) {
            if (nowSample < startSample) {
                return amplitude;
            }
            return amplitude * envDecay;
        }

        private static double perSampleDecay(final double tauSeconds) {
            return Math.exp(-1.0 / (SAMPLE_RATE * tauSeconds));
        }
    }
}
