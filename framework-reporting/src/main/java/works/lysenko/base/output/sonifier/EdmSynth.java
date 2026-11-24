package works.lysenko.base.output.sonifier;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Procedural synthesizer for the deadmau5-style Progressive House sonifier ({@code edm}).
 * <p>
 * Built around band-limited PolyBLEP sawtooth and pulse oscillators, a resonant zero-delay
 * feedback (ZDF) state-variable filter (SVF), kick-driven sidechain compression pumping,
 * tempo-locked stereo ping-pong delay (dotted-8th at 128 BPM), Juno-style stereo chorus,
 * and warm analog saturation.
 */
final class EdmSynth {

    static final int SAMPLE_RATE = 44_100;
    static final int CHANNELS = 2;
    static final double BPM = 128.0;

    /** 16th-note step at 128 BPM = (44100 * 60) / (128 * 4) = 5167.97 samples (~117.2 ms). */
    static final int STEP_SAMPLES = (int) Math.round(SAMPLE_RATE * 60.0 / (BPM * 4.0));

    /** 1 bar = 16 steps = 82,688 samples (~1.875 seconds). */
    static final int BAR_SAMPLES = STEP_SAMPLES * 16;

    /** 2.2-second tail for delay reflections and outro chord fade. */
    static final int TAIL_SAMPLES = (int) (SAMPLE_RATE * 2.20);

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final int MAX_ACTIVE_VOICES = 48;
    private static final int IO_BUFFER_BYTES = 16_384;
    private static final int FILTER_CONTROL_STRIDE = 16;

    // Tempo-locked stereo ping-pong delay (dotted-8th = 3 16th steps = ~351.6 ms)
    private static final int DELAY_STEPS = 3;
    private static final int DELAY_SAMPLES_L = STEP_SAMPLES * DELAY_STEPS;
    private static final int DELAY_SAMPLES_R = (int) Math.round(STEP_SAMPLES * (DELAY_STEPS + 0.5));
    private static final int DELAY_BUFFER_SIZE = 65_536;
    private static final int DELAY_BUFFER_MASK = DELAY_BUFFER_SIZE - 1;
    private static final double DELAY_FEEDBACK = 0.36;
    private static final double DELAY_DAMPING = 0.62;
    private static final double DELAY_WET_MIX = 0.24;

    // Stereo dimension chorus
    private static final int CHORUS_BUFFER_SIZE = 2_048;
    private static final int CHORUS_BUFFER_MASK = CHORUS_BUFFER_SIZE - 1;
    private static final double CHORUS_CENTER_DELAY = 640.0; // ~14.5 ms
    private static final double CHORUS_DEPTH = 130.0;        // ~2.9 ms swing
    private static final double CHORUS_LFO_INC = TWO_PI * 0.45 / SAMPLE_RATE;
    private static final double CHORUS_WET_MIX = 0.26;

    // Signature deadmau5 kick-driven sidechain ducking (~165 ms recovery time constant)
    private static final double SIDECHAIN_DUCK_FLOOR = 0.24;
    private static final double SIDECHAIN_RECOVERY = Math.exp(-1.0 / (SAMPLE_RATE * 0.165));

    private static final double MASTER_DRIVE = 1.28;
    private static final double PCM_HEADROOM = 31_400.0;

    private EdmSynth() {
    }

    /**
     * Progressive house &amp; deadmau5-inspired synthesis voice models.
     */
    enum VoiceKind {
        /** Punchy 909-style transient drop kick (drives sidechain bus). */
        EDM_KICK,
        /** Crisp layered stereo clap/snare on beats 2 and 4. */
        EDM_CLAP,
        /** Offbeat open hi-hat on the "&" of every beat. */
        OPEN_HAT,
        /** Ticking 16th closed hi-hat / shaker for continuous groove propulsion. */
        CLOSED_HAT,
        /** Deep 16th rolling analog saw/sub bass (sidechained to kick). */
        ROLLING_BASS,
        /** Signature deadmau5 syncopated plucked chords (PolyBLEP saw+pulse, sidechained). */
        MAU5_CHORD,
        /** Snappy plucked arp lead note for node execution (sends to ping-pong delay). */
        PLUCK_LEAD,
        /** High-register shimmering bell flourish for newly completed leaves. */
        CHIME_CROWN,
        /** Crystalline cyan harmonic bell for NOTICE events. */
        CYAN_NOTICE,
        /** Warm detuned analog brass chord stab for WARNING cues. */
        AMBER_BRASS,
        /** Downward pitch dive for upset ancestor nodes. */
        UPSET_DIVE,
        /** Heavy analog saw growl + sub drop + tape stop for raw FAILED regressions. */
        FAULT_DROP,
        /** Resonant white-noise filter riser sweep for bank compaction / milestones. */
        NOISE_RISER,
        /** Soft downward low-pass filter fade for pruned/hidden nodes. */
        FILTER_FADE
    }

    /**
     * Immutable specification of a scheduled stereo EDM synthesis event.
     */
    record EdmEvent(
            long startSample,
            int durationSamples,
            VoiceKind kind,
            double midiPitch,
            double endMidiPitch,
            double amplitude,
            double brightness,
            double pan) {

        EdmEvent(
                final long startSample,
                final int durationSamples,
                final VoiceKind kind,
                final double midiPitch,
                final double amplitude,
                final double brightness,
                final double pan) {
            this(startSample, durationSamples, kind, midiPitch, midiPitch, amplitude, brightness, pan);
        }
    }

    static int millisToSamples(final int millis) {
        return (int) Math.round(SAMPLE_RATE * (millis / 1000.0));
    }

    static double midiToFreq(final double midi) {
        return 440.0 * Math.pow(2.0, (midi - 69.0) / 12.0);
    }

    /**
     * Renders scheduled EDM events into a deterministic 16-bit stereo PCM WAV file.
     *
     * @param output          destination WAV file
     * @param totalSamples    total stereo sample frames
     * @param samplesPerFrame sample frames per video frame
     * @param eventsByFrame   events keyed by video frame index
     * @throws IOException on write failure or WAV size limit overflow
     */
    static void writeStereoWav(
            final File output,
            final long totalSamples,
            final int samplesPerFrame,
            final Map<Integer, List<EdmEvent>> eventsByFrame) throws IOException {

        final long dataBytes = totalSamples * CHANNELS * Short.BYTES;
        if (dataBytes > Integer.MAX_VALUE - 36L) {
            throw new IOException("Progression EDM audio exceeds the WAV file size limit");
        }

        final List<ActiveVoice> activeVoices = new ArrayList<>(MAX_ACTIVE_VOICES);

        // Ping-pong delay state
        final double[] delayBufL = new double[DELAY_BUFFER_SIZE];
        final double[] delayBufR = new double[DELAY_BUFFER_SIZE];
        int delayWriteIdx = 0;
        double delayLpL = 0.0;
        double delayLpR = 0.0;

        // Stereo chorus state
        final double[] chorusBufL = new double[CHORUS_BUFFER_SIZE];
        final double[] chorusBufR = new double[CHORUS_BUFFER_SIZE];
        int chorusWriteIdx = 0;
        double chorusLfoPhase = 0.0;

        double sidechainGain = 1.0;
        int voiceSerial = 1;

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(output.toPath()), IO_BUFFER_BYTES)) {
            writeWavHeader(out, dataBytes);
            final byte[] pcmBuffer = new byte[IO_BUFFER_BYTES];
            int bufPos = 0;
            int currentFrame = -1;

            for (long sample = 0; sample < totalSamples; sample++) {
                final int frameIndex = (int) (sample / samplesPerFrame);
                if (frameIndex != currentFrame) {
                    currentFrame = frameIndex;
                    final List<EdmEvent> incoming = eventsByFrame.get(frameIndex);
                    if (null != incoming) {
                        for (int i = 0; i < incoming.size(); i++) {
                            spawnVoice(activeVoices, incoming.get(i), sample, voiceSerial++);
                        }
                    }
                }

                double busMainL = 0.0;
                double busMainR = 0.0;
                double busDuckL = 0.0;
                double busDuckR = 0.0;
                double delaySendL = 0.0;
                double delaySendR = 0.0;
                double chorusSendL = 0.0;
                double chorusSendR = 0.0;

                for (int i = activeVoices.size() - 1; i >= 0; i--) {
                    final ActiveVoice v = activeVoices.get(i);
                    if (sample < v.startSample) {
                        continue;
                    }
                    if (v.age == 0 && v.kind == VoiceKind.EDM_KICK) {
                        sidechainGain = Math.min(sidechainGain, SIDECHAIN_DUCK_FLOOR);
                    }
                    if (v.age >= v.durationSamples) {
                        activeVoices.remove(i);
                        continue;
                    }
                    v.step();
                    if (v.sidechained) {
                        busDuckL += v.outL;
                        busDuckR += v.outR;
                    } else {
                        busMainL += v.outL;
                        busMainR += v.outR;
                    }
                    if (v.delaySend > 0.0) {
                        delaySendL += v.outL * v.delaySend;
                        delaySendR += v.outR * v.delaySend;
                    }
                    if (v.chorusSend > 0.0) {
                        chorusSendL += v.outL * v.chorusSend;
                        chorusSendR += v.outR * v.chorusSend;
                    }
                }

                // Recover sidechain envelope towards 1.0
                sidechainGain = 1.0 - (1.0 - sidechainGain) * SIDECHAIN_RECOVERY;
                busDuckL *= sidechainGain;
                busDuckR *= sidechainGain;

                // Ping-pong delay processing (cross-feed: L feeds R, R feeds L)
                final int readIdxL = (delayWriteIdx - DELAY_SAMPLES_L) & DELAY_BUFFER_MASK;
                final int readIdxR = (delayWriteIdx - DELAY_SAMPLES_R) & DELAY_BUFFER_MASK;
                final double delayedL = delayBufL[readIdxL];
                final double delayedR = delayBufR[readIdxR];

                delayLpL += DELAY_DAMPING * (delayedL - delayLpL);
                delayLpR += DELAY_DAMPING * (delayedR - delayLpR);

                delayBufL[delayWriteIdx] = delaySendL + delayLpR * DELAY_FEEDBACK;
                delayBufR[delayWriteIdx] = delaySendR + delayLpL * DELAY_FEEDBACK;
                delayWriteIdx = (delayWriteIdx + 1) & DELAY_BUFFER_MASK;

                // Stereo chorus processing
                chorusLfoPhase += CHORUS_LFO_INC;
                if (chorusLfoPhase >= TWO_PI) {
                    chorusLfoPhase -= TWO_PI;
                }
                final double modL = CHORUS_CENTER_DELAY + CHORUS_DEPTH * Math.sin(chorusLfoPhase);
                final double modR = CHORUS_CENTER_DELAY + CHORUS_DEPTH * Math.cos(chorusLfoPhase);

                final double chorusedL = readChorusBuffer(chorusBufL, chorusWriteIdx, modL);
                final double chorusedR = readChorusBuffer(chorusBufR, chorusWriteIdx, modR);

                chorusBufL[chorusWriteIdx] = chorusSendL;
                chorusBufR[chorusWriteIdx] = chorusSendR;
                chorusWriteIdx = (chorusWriteIdx + 1) & CHORUS_BUFFER_MASK;

                // Combine buses
                final double wetL = delayedL * DELAY_WET_MIX + chorusedL * CHORUS_WET_MIX;
                final double wetR = delayedR * DELAY_WET_MIX + chorusedR * CHORUS_WET_MIX;

                final double sumL = (busMainL + busDuckL + wetL) * MASTER_DRIVE;
                final double sumR = (busMainR + busDuckR + wetR) * MASTER_DRIVE;

                // Soft-knee tanh analog saturation
                final int pcmL = (int) Math.round(Math.tanh(sumL) * PCM_HEADROOM);
                final int pcmR = (int) Math.round(Math.tanh(sumR) * PCM_HEADROOM);

                final short sL = (short) Math.clamp(pcmL, Short.MIN_VALUE, Short.MAX_VALUE);
                final short sR = (short) Math.clamp(pcmR, Short.MIN_VALUE, Short.MAX_VALUE);

                pcmBuffer[bufPos++] = (byte) (sL & 0xFF);
                pcmBuffer[bufPos++] = (byte) ((sL >> 8) & 0xFF);
                pcmBuffer[bufPos++] = (byte) (sR & 0xFF);
                pcmBuffer[bufPos++] = (byte) ((sR >> 8) & 0xFF);

                if (bufPos >= IO_BUFFER_BYTES) {
                    out.write(pcmBuffer, 0, bufPos);
                    bufPos = 0;
                }
            }

            if (bufPos > 0) {
                out.write(pcmBuffer, 0, bufPos);
            }
        }
    }

    private static double readChorusBuffer(final double[] buf, final int writeIdx, final double delaySamples) {
        final double readPos = (writeIdx - delaySamples);
        int i0 = ((int) Math.floor(readPos)) & CHORUS_BUFFER_MASK;
        int i1 = (i0 + 1) & CHORUS_BUFFER_MASK;
        final double frac = readPos - Math.floor(readPos);
        return buf[i0] + frac * (buf[i1] - buf[i0]);
    }

    private static void spawnVoice(
            final List<ActiveVoice> activeVoices,
            final EdmEvent event,
            final long currentSample,
            final int serial) {

        if (activeVoices.size() >= MAX_ACTIVE_VOICES) {
            int quietestIdx = -1;
            double quietestAmp = Double.MAX_VALUE;
            for (int i = 0; i < activeVoices.size(); i++) {
                final ActiveVoice v = activeVoices.get(i);
                if (v.kind != VoiceKind.EDM_KICK && v.amplitude < quietestAmp) {
                    quietestAmp = v.amplitude;
                    quietestIdx = i;
                }
            }
            if (quietestIdx >= 0) {
                activeVoices.remove(quietestIdx);
            } else {
                activeVoices.remove(0);
            }
        }
        activeVoices.add(new ActiveVoice(event, serial));
    }

    private static void writeWavHeader(final OutputStream out, final long dataBytes) throws IOException {
        final long totalBytes = 36L + dataBytes;
        final byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalBytes & 0xFF);
        header[5] = (byte) ((totalBytes >> 8) & 0xFF);
        header[6] = (byte) ((totalBytes >> 16) & 0xFF);
        header[7] = (byte) ((totalBytes >> 24) & 0xFF);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0; // PCM
        header[22] = (byte) CHANNELS; header[23] = 0;
        header[24] = (byte) (SAMPLE_RATE & 0xFF);
        header[25] = (byte) ((SAMPLE_RATE >> 8) & 0xFF);
        header[26] = (byte) ((SAMPLE_RATE >> 16) & 0xFF);
        header[27] = (byte) ((SAMPLE_RATE >> 24) & 0xFF);
        final int byteRate = SAMPLE_RATE * CHANNELS * Short.BYTES;
        header[28] = (byte) (byteRate & 0xFF);
        header[29] = (byte) ((byteRate >> 8) & 0xFF);
        header[30] = (byte) ((byteRate >> 16) & 0xFF);
        header[31] = (byte) ((byteRate >> 24) & 0xFF);
        final int blockAlign = CHANNELS * Short.BYTES;
        header[32] = (byte) (blockAlign & 0xFF);
        header[33] = (byte) ((blockAlign >> 8) & 0xFF);
        header[34] = 16; header[35] = 0; // 16 bits
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (dataBytes & 0xFF);
        header[41] = (byte) ((dataBytes >> 8) & 0xFF);
        header[42] = (byte) ((dataBytes >> 16) & 0xFF);
        header[43] = (byte) ((dataBytes >> 24) & 0xFF);
        out.write(header);
    }

    /**
     * Active synthesiser voice.
     */
    private static final class ActiveVoice {
        final long startSample;
        final int durationSamples;
        final VoiceKind kind;
        final double baseFreqHz;
        final double endFreqHz;
        final double amplitude;
        final double brightness;
        final double gainL;
        final double gainR;
        final boolean sidechained;
        final double delaySend;
        final double chorusSend;

        int age = 0;
        double phaseA = 0.0;
        double phaseB = 0.0;
        double phaseC = 0.0;
        double phaseSub = 0.0;

        // Zero-Delay Feedback state-variable filter state
        double svfA1 = 0.0;
        double svfA2 = 0.0;
        double svfA3 = 0.0;
        double svfK = 0.0;
        double ic1eq = 0.0;
        double ic2eq = 0.0;

        int rngState;
        double outL = 0.0;
        double outR = 0.0;

        ActiveVoice(final EdmEvent event, final int serial) {
            this.startSample = event.startSample();
            this.durationSamples = Math.max(1, event.durationSamples());
            this.kind = event.kind();
            this.baseFreqHz = midiToFreq(event.midiPitch());
            this.endFreqHz = midiToFreq(event.endMidiPitch());
            this.amplitude = event.amplitude();
            this.brightness = Math.clamp(event.brightness(), 0.0, 1.0);

            final double clampedPan = Math.clamp(event.pan(), -1.0, 1.0);
            final double angle = (clampedPan + 1.0) * (Math.PI * 0.25);
            this.gainL = Math.cos(angle);
            this.gainR = Math.sin(angle);

            this.sidechained = (kind == VoiceKind.ROLLING_BASS || kind == VoiceKind.MAU5_CHORD);

            this.delaySend = switch (kind) {
                case PLUCK_LEAD -> 0.65;
                case CHIME_CROWN -> 0.45;
                case CYAN_NOTICE -> 0.38;
                case MAU5_CHORD -> 0.22;
                case AMBER_BRASS -> 0.28;
                default -> 0.0;
            };

            this.chorusSend = switch (kind) {
                case MAU5_CHORD -> 0.58;
                case CHIME_CROWN -> 0.72;
                case NOISE_RISER -> 0.35;
                case AMBER_BRASS -> 0.45;
                default -> 0.0;
            };

            this.rngState = 0x5EED1984 ^ (serial * 0x45D9F3B) ^ (int) (startSample * 31L);
            if (this.rngState == 0) {
                this.rngState = 0x13579BDF;
            }
            if (kind == VoiceKind.MAU5_CHORD || kind == VoiceKind.CHIME_CROWN) {
                this.phaseB = 0.28;
                this.phaseC = 0.57;
            }
        }

        private double nextNoise() {
            int x = rngState;
            x ^= (x << 13);
            x ^= (x >>> 17);
            x ^= (x << 5);
            rngState = x;
            return x / (double) Integer.MAX_VALUE;
        }

        private void updateSvf(final double cutoffHz, final double q) {
            final double clampedHz = Math.clamp(cutoffHz, 40.0, 15_000.0);
            final double g = Math.tan(Math.PI * clampedHz / SAMPLE_RATE);
            svfK = 1.0 / Math.max(0.55, q);
            svfA1 = 1.0 / (1.0 + g * (g + svfK));
            svfA2 = g * svfA1;
            svfA3 = g * svfA2;
        }

        private double stepSvfLow(final double in) {
            final double v3 = in - ic2eq;
            final double v1 = svfA1 * ic1eq + svfA2 * v3;
            final double v2 = ic2eq + svfA2 * ic1eq + svfA3 * v3;
            ic1eq = 2.0 * v1 - ic1eq;
            ic2eq = 2.0 * v2 - ic2eq;
            return v2;
        }

        private double stepSvfBand(final double in) {
            final double v3 = in - ic2eq;
            final double v1 = svfA1 * ic1eq + svfA2 * v3;
            final double v2 = ic2eq + svfA2 * ic1eq + svfA3 * v3;
            ic1eq = 2.0 * v1 - ic1eq;
            ic2eq = 2.0 * v2 - ic2eq;
            return v1;
        }

        private double stepSvfHigh(final double in) {
            final double v3 = in - ic2eq;
            final double v1 = svfA1 * ic1eq + svfA2 * v3;
            final double v2 = ic2eq + svfA2 * ic1eq + svfA3 * v3;
            ic1eq = 2.0 * v1 - ic1eq;
            ic2eq = 2.0 * v2 - ic2eq;
            return in - svfK * v1 - v2;
        }

        private static double polyBlep(final double t, final double dt) {
            if (t < dt) {
                final double r = t / dt;
                return r + r - r * r - 1.0;
            } else if (t > 1.0 - dt) {
                final double r = (t - 1.0) / dt;
                return r * r + r + r + 1.0;
            }
            return 0.0;
        }

        private static double polyBlepSaw(final double phase, final double dt) {
            final double naive = 2.0 * phase - 1.0;
            return naive - polyBlep(phase, dt);
        }

        private static double polyBlepPulse(final double phase, final double dt, final double width) {
            final double naive = (phase < width) ? 1.0 : -1.0;
            double mod = polyBlep(phase, dt);
            double phaseShift = phase - width;
            if (phaseShift < 0.0) {
                phaseShift += 1.0;
            }
            mod -= polyBlep(phaseShift, dt);
            return naive + mod;
        }

        private static double advancePhase(final double phase, final double dt) {
            double next = phase + dt;
            if (next >= 1.0) {
                next -= Math.floor(next);
            }
            return next;
        }

        private static double smoothTail(final double progress, final double threshold) {
            if (progress <= threshold) {
                return 0.0;
            }
            final double p = (progress - threshold) / (1.0 - threshold);
            return p * p * (3.0 - 2.0 * p);
        }

        void step() {
            final double tSec = age / (double) SAMPLE_RATE;
            final double progress = age / (double) durationSamples;

            final double mono = switch (kind) {
                case EDM_KICK -> sampleKick(tSec, progress);
                case EDM_CLAP -> sampleClap(tSec, progress);
                case OPEN_HAT -> sampleOpenHat(tSec, progress);
                case CLOSED_HAT -> sampleClosedHat(tSec, progress);
                case ROLLING_BASS -> sampleRollingBass(tSec, progress);
                case MAU5_CHORD -> sampleMau5Chord(tSec, progress);
                case PLUCK_LEAD -> samplePluckLead(tSec, progress);
                case CHIME_CROWN -> sampleChimeCrown(tSec, progress);
                case CYAN_NOTICE -> sampleCyanNotice(tSec, progress);
                case AMBER_BRASS -> sampleAmberBrass(tSec, progress);
                case UPSET_DIVE -> sampleUpsetDive(tSec, progress);
                case FAULT_DROP -> sampleFaultDrop(tSec, progress);
                case NOISE_RISER -> sampleNoiseRiser(tSec, progress);
                case FILTER_FADE -> sampleFilterFade(tSec, progress);
            };

            outL = mono * gainL;
            outR = mono * gainR;
            age++;
        }

        private double sampleKick(final double tSec, final double progress) {
            // Tight 909-style progressive house kick: 210 Hz down to 48 Hz with punchy transient click
            final double freq = 48.0 + 162.0 * Math.exp(-tSec * 45.0);
            phaseA = advancePhase(phaseA, freq / SAMPLE_RATE);
            final double body = Math.sin(TWO_PI * phaseA);

            phaseB = advancePhase(phaseB, 1_350.0 / SAMPLE_RATE);
            final double click = Math.sin(TWO_PI * phaseB) * Math.exp(-tSec * 420.0) * 0.25;

            final double env = Math.min(1.0, tSec / 0.0012)
                    * Math.exp(-tSec * 15.0)
                    * (1.0 - smoothTail(progress, 0.82));
            return Math.tanh((body + click) * 1.35) * env * amplitude;
        }

        private double sampleClap(final double tSec, final double progress) {
            // Layered stereo progressive house clap with triple flam microbursts
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(1_850.0 + 900.0 * brightness, 0.92);
            }
            final double burst1 = (tSec < 0.010) ? nextNoise() * 0.70 : 0.0;
            final double burst2 = (tSec >= 0.010 && tSec < 0.021) ? nextNoise() * 0.85 : 0.0;
            final double burst3 = (tSec >= 0.021) ? nextNoise() : 0.0;
            final double noiseIn = burst1 + burst2 + burst3;
            final double filteredNoise = stepSvfBand(noiseIn);

            phaseA = advancePhase(phaseA, 220.0 / SAMPLE_RATE);
            final double body = Math.sin(TWO_PI * phaseA) * Math.exp(-tSec * 32.0) * 0.35;

            final double env = Math.min(1.0, tSec / 0.0010)
                    * Math.exp(-Math.max(0.0, tSec - 0.021) * 22.0)
                    * (1.0 - smoothTail(progress, 0.80));
            return Math.tanh((filteredNoise + body) * 1.25) * env * amplitude;
        }

        private double sampleOpenHat(final double tSec, final double progress) {
            // Offbeat open hat (7.2 kHz SVF bandpass + metallic ring)
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(7_200.0 + 2_200.0 * brightness, 1.10);
            }
            phaseA = advancePhase(phaseA, 3_820.0 / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, 5_510.0 / SAMPLE_RATE);
            final double ring = Math.sin(TWO_PI * phaseA) * Math.sin(TWO_PI * phaseB) * 0.30;
            final double filtered = stepSvfBand(nextNoise() * 0.75 + ring);
            final double env = Math.min(1.0, tSec / 0.0010)
                    * Math.exp(-tSec * 32.0)
                    * (1.0 - smoothTail(progress, 0.82));
            return filtered * env * amplitude;
        }

        private double sampleClosedHat(final double tSec, final double progress) {
            // Tight 16th closed hat / shaker (8.5 kHz high-pass)
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(8_500.0, 0.85);
            }
            final double filtered = stepSvfHigh(nextNoise());
            final double env = Math.min(1.0, tSec / 0.0006)
                    * Math.exp(-tSec * 85.0)
                    * (1.0 - smoothTail(progress, 0.75));
            return filtered * env * amplitude;
        }

        private double sampleRollingBass(final double tSec, final double progress) {
            // Driving 16th-note progressive house bass: PolyBLEP saw + sub-octave sine
            final double dt = baseFreqHz / SAMPLE_RATE;
            final double dtSub = (baseFreqHz * 0.5) / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dt);
            phaseSub = advancePhase(phaseSub, dtSub);

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double filterCutoff = 80.0 + (120.0 + 450.0 * brightness) * Math.exp(-tSec * 22.0);
                updateSvf(filterCutoff, 1.30);
            }

            final double saw = polyBlepSaw(phaseA, dt);
            final double filteredSaw = stepSvfLow(saw);
            final double sub = Math.sin(TWO_PI * phaseSub) * 0.42;

            final double attack = Math.min(1.0, tSec / 0.003);
            final double decay = Math.exp(-tSec * 14.0) * (1.0 - smoothTail(progress, 0.78));
            return Math.tanh((filteredSaw * 1.10 + sub) * 1.20) * attack * decay * amplitude;
        }

        private double sampleMau5Chord(final double tSec, final double progress) {
            // Iconic deadmau5 plucked chords: PolyBLEP saw + 30% PWM pulse through resonant SVF LP
            final double dtA = baseFreqHz / SAMPLE_RATE;
            final double dtB = (baseFreqHz * 1.0022) / SAMPLE_RATE; // subtle 3.8 cent chorus spread
            phaseA = advancePhase(phaseA, dtA);
            phaseB = advancePhase(phaseB, dtB);

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                // Filter cutoff dynamically opens with test progress (brightness)
                final double baseCutoff = 380.0 + 3_600.0 * brightness;
                final double envelopeCutoff = baseCutoff * (0.32 + 0.68 * Math.exp(-tSec * 10.5));
                updateSvf(envelopeCutoff, 1.42);
            }

            final double saw = polyBlepSaw(phaseA, dtA);
            final double pulse = polyBlepPulse(phaseB, dtB, 0.30);
            final double raw = 0.55 * saw + 0.45 * pulse;
            final double filtered = stepSvfLow(raw);

            final double attack = Math.min(1.0, tSec / 0.0018);
            final double decay = Math.exp(-tSec * 6.5) * (1.0 - smoothTail(progress, 0.85));
            return Math.tanh(filtered * 1.30) * attack * decay * amplitude;
        }

        private double samplePluckLead(final double tSec, final double progress) {
            // Clockwork deadmau5 arpeggio pluck: 50% pulse wave with resonant pluck filter
            final double dt = baseFreqHz / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dt);

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double cutoff = 650.0 + 3_200.0 * brightness * Math.exp(-tSec * 16.0);
                updateSvf(cutoff, 1.55);
            }

            final double pulse = polyBlepPulse(phaseA, dt, 0.50);
            final double filtered = stepSvfLow(pulse);

            final double attack = Math.min(1.0, tSec / 0.0012);
            final double decay = Math.exp(-tSec * 12.0) * (1.0 - smoothTail(progress, 0.80));
            return filtered * attack * decay * amplitude;
        }

        private double sampleChimeCrown(final double tSec, final double progress) {
            // High-octave celebratory chime for completed leaves: dual bell tones + octave shimmer
            final double dtA = baseFreqHz / SAMPLE_RATE;
            final double dtB = (baseFreqHz * 2.003) / SAMPLE_RATE;
            final double dtC = (baseFreqHz * 3.51) / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dtA);
            phaseB = advancePhase(phaseB, dtB);
            phaseC = advancePhase(phaseC, dtC);

            final double bell = Math.sin(TWO_PI * phaseA) * 0.65
                    + Math.sin(TWO_PI * phaseB) * 0.28 * Math.exp(-tSec * 4.0)
                    + Math.sin(TWO_PI * phaseC) * 0.12 * Math.exp(-tSec * 12.0);

            final double attack = Math.min(1.0, tSec / 0.0015);
            final double decay = Math.exp(-tSec * 3.2) * (1.0 - smoothTail(progress, 0.85));
            return bell * attack * decay * amplitude;
        }

        private double sampleCyanNotice(final double tSec, final double progress) {
            // Glassy pure harmonic bell for NOTICE events
            final double dt = baseFreqHz / SAMPLE_RATE;
            final double dt3 = (baseFreqHz * 3.0) / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dt);
            phaseB = advancePhase(phaseB, dt3);

            final double tone = Math.sin(TWO_PI * phaseA) * 0.78 + Math.sin(TWO_PI * phaseB) * 0.22;
            final double attack = Math.min(1.0, tSec / 0.002);
            final double decay = Math.exp(-tSec * 6.0) * (1.0 - smoothTail(progress, 0.80));
            return tone * attack * decay * amplitude;
        }

        private double sampleAmberBrass(final double tSec, final double progress) {
            // Warm detuned analog brass chord stab for WARNING cues
            final double dtA = baseFreqHz / SAMPLE_RATE;
            final double dtB = (baseFreqHz * 1.008) / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dtA);
            phaseB = advancePhase(phaseB, dtB);

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double cutoff = 650.0 + 850.0 * brightness;
                updateSvf(cutoff, 1.15);
            }

            final double raw = 0.50 * polyBlepSaw(phaseA, dtA) + 0.50 * polyBlepSaw(phaseB, dtB);
            final double filtered = stepSvfLow(raw);

            final double attack = Math.min(1.0, tSec / 0.008);
            final double decay = Math.exp(-tSec * 5.5) * (1.0 - smoothTail(progress, 0.80));
            return Math.tanh(filtered * 1.20) * attack * decay * amplitude;
        }

        private double sampleUpsetDive(final double tSec, final double progress) {
            // Sub downward pitch glide for upset ancestor nodes
            final double freq = baseFreqHz + (endFreqHz - baseFreqHz) * (1.0 - Math.exp(-tSec * 9.0));
            phaseA = advancePhase(phaseA, freq / SAMPLE_RATE);
            final double tone = Math.sin(TWO_PI * phaseA);
            final double env = Math.min(1.0, tSec / 0.005) * Math.exp(-tSec * 6.5);
            return tone * env * amplitude;
        }

        private double sampleFaultDrop(final double tSec, final double progress) {
            // Dramatic deadmau5 tape-stop pitch dive + Reese saw growl for raw failure regressions
            final double glide = Math.min(1.0, Math.pow(progress, 1.8));
            final double freq = Math.max(30.0, baseFreqHz + (endFreqHz - baseFreqHz) * glide);

            final double dtA = freq / SAMPLE_RATE;
            final double dtB = (freq * 1.015) / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dtA);
            phaseB = advancePhase(phaseB, dtB);

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double cutoff = Math.max(80.0, 950.0 * (1.0 - glide * 0.75));
                updateSvf(cutoff, 1.45);
            }

            final double saw = 0.50 * polyBlepSaw(phaseA, dtA) + 0.50 * polyBlepSaw(phaseB, dtB);
            final double filtered = stepSvfLow(saw);
            final double sub = Math.sin(TWO_PI * phaseA * 0.5) * 0.45;

            final double attack = Math.min(1.0, tSec / 0.004);
            final double decay = (1.0 - glide) * (1.0 - smoothTail(progress, 0.85));
            return Math.tanh((filtered + sub) * 1.35) * attack * decay * amplitude;
        }

        private double sampleNoiseRiser(final double tSec, final double progress) {
            // Resonant white-noise filter riser sweep building up over duration
            final double sweepHz = baseFreqHz + (endFreqHz - baseFreqHz) * Math.pow(progress, 1.4);
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(sweepHz, 2.2);
            }
            final double filtered = stepSvfBand(nextNoise());
            final double env = Math.min(1.0, tSec / 0.02)
                    * (0.25 + 0.75 * Math.pow(progress, 1.2))
                    * (1.0 - smoothTail(progress, 0.92));
            return filtered * env * amplitude;
        }

        private double sampleFilterFade(final double tSec, final double progress) {
            // Soft downward low-pass exhale for pruned/hidden leaves
            final double cutoffHz = Math.max(60.0, baseFreqHz + (endFreqHz - baseFreqHz) * progress);
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(cutoffHz, 1.0);
            }
            final double filtered = stepSvfLow(nextNoise() * 0.85);
            final double env = Math.min(1.0, tSec / 0.01) * (1.0 - progress);
            return filtered * env * amplitude;
        }
    }
}
