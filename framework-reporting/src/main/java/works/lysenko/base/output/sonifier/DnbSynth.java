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
 * Deterministic true-stereo Liquid Drum &amp; Bass synthesizer rendering 44.1 kHz, 16-bit
 * stereo PCM WAV tracks for {@link DnbSonifier}.
 * <p>
 * Built around an exact 175 BPM groove clock ({@code 3,780} samples per 16th note at 44.1 kHz)
 * with silky kick-ducked Fender Rhodes electric piano chords, deep mono-centered sub-bass and
 * resonant detuned Reese bass, sculpted breakbeat percussion (punchy round kick, woody crisp
 * two-step snare, light ghost snares, high-windowed brushed hats/rides, woody rim clicks),
 * tempo-locked dotted-eighth / quarter-note stereo dub tape delay, subtle Juno-style stereo
 * chorus, warm asymmetric room reflections, and smooth soft-knee master saturation with a gentle
 * high-frequency warmth filter so multi-minute test soundtracks remain pleasant and non-fatiguing.
 */
final class DnbSynth {

    static final int SAMPLE_RATE = 44_100;
    static final int CHANNELS = 2;
    /** Exact 1/16th note at 175 BPM: {@code 44,100 * 60 / (175 * 4) = 3,780} samples (~85.71 ms). */
    static final int STEP_SAMPLES = 3_780;
    static final int TAIL_SAMPLES = (int) (SAMPLE_RATE * 1.85); // 1.85s dub delay & Rhodes outro tail

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final int MAX_ACTIVE_VOICES = 34;
    private static final int IO_BUFFER_BYTES = 16_384;
    private static final int FILTER_CONTROL_STRIDE = 16;

    // Gentle stereo chorus for Rhodes chords & warm Reese width
    private static final int CHORUS_BUFFER_SIZE = 2_048;
    private static final int CHORUS_BUFFER_MASK = CHORUS_BUFFER_SIZE - 1;
    private static final double CHORUS_CENTER_DELAY = 620.0; // ~14.1 ms
    private static final double CHORUS_DEPTH = 115.0;        // ~2.6 ms subtle sweep
    private static final double CHORUS_LFO_INC = TWO_PI * 0.44 / SAMPLE_RATE;
    private static final double CHORUS_WET_MIX = 0.24;

    // Tempo-locked 175 BPM stereo dub tape delay: L = dotted-8th (3 steps), R = quarter (4 steps)
    private static final int DUB_BUFFER_SIZE = 32_768;
    private static final int DUB_BUFFER_MASK = DUB_BUFFER_SIZE - 1;
    private static final double DUB_DELAY_L = STEP_SAMPLES * 3.0; // 11,340 samples (~257.1 ms)
    private static final double DUB_DELAY_R = STEP_SAMPLES * 4.0; // 15,120 samples (~342.9 ms)
    private static final double DUB_WOW_DEPTH = 3.2;
    private static final double DUB_WOW_INC = TWO_PI * 0.61 / SAMPLE_RATE;
    private static final double DUB_FEEDBACK = 0.34;
    private static final double DUB_TAPE_DAMP = 0.56;
    private static final double DUB_WET_MIX = 0.30;

    // Warm asymmetric room early reflections (~33.8 ms L, ~48.1 ms R)
    private static final int ROOM_LEFT_SAMPLES = 1_493;
    private static final int ROOM_RIGHT_SAMPLES = 2_119;
    private static final double ROOM_FEEDBACK = 0.16;
    private static final double ROOM_DAMPING = 0.68;
    private static final double ROOM_WET_MIX = 0.12;

    // Kick-driven sidechain ducking on sub-bass, Reese, and Rhodes pad bus (~110 ms recovery)
    private static final double SIDECHAIN_DUCK_FLOOR = 0.58;
    private static final double SIDECHAIN_RECOVERY = Math.exp(-1.0 / (SAMPLE_RATE * 0.110));

    // Subtle master warmth low-pass smoothing (tames brittle upper harmonics for long listening)
    private static final double WARMTH_ALPHA = 0.86;
    private static final double MASTER_DRIVE = 1.22;
    private static final double PCM_HEADROOM = 31_200.0;

    private DnbSynth() {
    }

    /**
     * Liquid Drum &amp; Bass acoustic and telemetry synthesis voice models.
     */
    enum VoiceKind {
        /** Punchy, rounded Liquid DnB kick drum (mono-centered, triggers sidechain ducking). */
        DNB_KICK,
        /** Crisp two-step breakbeat snare with woody shell resonance and sculpted silky wire snap. */
        DNB_SNARE,
        /** Ultra-light rolling Amen/Think-break ghost snare for groove flow without fatigue. */
        GHOST_SNARE,
        /** Silky high-windowed brushed hi-hat / ride shimmer for groove &amp; over-execution ticks. */
        LIQUID_HAT,
        /** Tight woody rimshot accent for groove punctuation, compaction lock-in, and warning taps. */
        RIM_CLICK,
        /** Deep mono-centered Liquid sub-bass with subtle 2nd harmonic warmth and smooth pitch glide. */
        SUB_BASS,
        /** Classic detuned dual-saw Reese bass through resonant LP ZDF filter (warm or growling). */
        REESE_BASS,
        /** Lush Fender Rhodes electric piano chord voice with bell-tine attack and gentle tremolo. */
        RHODES_CHORD,
        /** Warm muted electric-piano pluck for branch traversal and partial leaf double-taps. */
        LIQUID_PLUCK,
        /** Luminous shimmering bell-lead cadence for newly completed leaves and clean sunrise crown. */
        LIQUID_ARP,
        /** Crystalline aqua dub-delayed harmonic bell for NOTICE (S3) events. */
        AQUA_NOTICE,
        /** Warm muted detuned brass horn fifth for WARNING (S2) cues (bright raw vs muted known). */
        AMBER_HORN,
        /** Hollow band-passed downward dub fifth for UPSET (child_failed) ancestor nodes. */
        UPSET_DROP,
        /** Visceral yet musical DnB Reese pitch-dive + sub thump + sculpted rewind grit for FAILED regressions. */
        FAULT_IMPACT,
        /** Resonant filtered pink-noise air sweep for hidden-leaf exhales and compaction risers. */
        AIR_SWEEP
    }

    /**
     * Immutable specification of a scheduled stereo Drum &amp; Bass event.
     *
     * @param startSample     exact sample frame at which the voice begins
     * @param durationSamples voice duration in sample frames
     * @param kind            synthesis voice model
     * @param midiPitch       starting MIDI pitch (69.0 = A4 = 440 Hz)
     * @param endMidiPitch    ending MIDI pitch for gliding/sweeping voices
     * @param amplitude       peak linear voice gain prior to constant-power stereo panning
     * @param brightness      filter cutoff energy / bite in {@code [0.0, 1.0]}
     * @param pan             stereo panorama in {@code [-1.0, +1.0]}
     */
    record DnbEvent(
            long startSample,
            int durationSamples,
            VoiceKind kind,
            double midiPitch,
            double endMidiPitch,
            double amplitude,
            double brightness,
            double pan) {

        DnbEvent(
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
        return Math.max(1, (int) Math.round(SAMPLE_RATE * (millis / 1000.0)));
    }

    static double midiToHz(final double midi) {
        return 440.0 * Math.pow(2.0, (midi - 69.0) / 12.0);
    }

    /**
     * Renders scheduled Liquid Drum &amp; Bass events into a deterministic 16-bit stereo PCM WAV file.
     *
     * @param output          destination WAV file
     * @param totalSamples    total stereo sample frames (including dub delay &amp; outro tail)
     * @param samplesPerFrame sample frames per video frame
     * @param eventsByFrame   events keyed by video frame index
     * @throws IOException on write failure or WAV size limit overflow
     */
    static void writeStereoWav(
            final File output,
            final long totalSamples,
            final int samplesPerFrame,
            final Map<Integer, List<DnbEvent>> eventsByFrame) throws IOException {

        final long dataBytes = totalSamples * CHANNELS * Short.BYTES;
        if (dataBytes > Integer.MAX_VALUE - 36L) {
            throw new IOException("Progression DnB audio exceeds the WAV file size limit");
        }

        final List<ActiveVoice> activeVoices = new ArrayList<>(MAX_ACTIVE_VOICES);

        // Stereo chorus state
        final double[] chorusBufL = new double[CHORUS_BUFFER_SIZE];
        final double[] chorusBufR = new double[CHORUS_BUFFER_SIZE];
        int chorusWriteIdx = 0;
        double chorusLfoPhase = 0.0;

        // Tempo-locked stereo dub delay state
        final double[] dubBufL = new double[DUB_BUFFER_SIZE];
        final double[] dubBufR = new double[DUB_BUFFER_SIZE];
        int dubWriteIdx = 0;
        double dubWowPhase = 0.0;
        double dubLpL = 0.0;
        double dubLpR = 0.0;

        // Asymmetric warm room early reflections
        final double[] roomBufL = new double[ROOM_LEFT_SAMPLES];
        final double[] roomBufR = new double[ROOM_RIGHT_SAMPLES];
        int roomIdxL = 0;
        int roomIdxR = 0;
        double roomLpL = 0.0;
        double roomLpR = 0.0;

        double sidechainGain = 1.0;
        double warmthL = 0.0;
        double warmthR = 0.0;
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
                    final List<DnbEvent> incoming = eventsByFrame.get(frameIndex);
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
                double chorusSendL = 0.0;
                double chorusSendR = 0.0;
                double dubSendL = 0.0;
                double dubSendR = 0.0;

                for (int i = activeVoices.size() - 1; i >= 0; i--) {
                    final ActiveVoice v = activeVoices.get(i);
                    if (sample < v.startSample) {
                        continue;
                    }
                    if (0 == v.age && v.kind == VoiceKind.DNB_KICK) {
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
                    if (v.chorusSend > 0.0) {
                        chorusSendL += v.outL * v.chorusSend;
                        chorusSendR += v.outR * v.chorusSend;
                    }
                    if (v.dubSend > 0.0) {
                        dubSendL += v.outL * v.dubSend;
                        dubSendR += v.outR * v.dubSend;
                    }
                }

                // Smooth kick-driven sidechain envelope recovery
                sidechainGain = 1.0 - (1.0 - sidechainGain) * SIDECHAIN_RECOVERY;
                final double dryL = busMainL + busDuckL * sidechainGain;
                final double dryR = busMainR + busDuckR * sidechainGain;

                // Gentle Juno-style stereo chorus on Rhodes & Reese warmth
                chorusBufL[chorusWriteIdx] = chorusSendL;
                chorusBufR[chorusWriteIdx] = chorusSendR;
                final double cLfo = Math.sin(chorusLfoPhase);
                chorusLfoPhase += CHORUS_LFO_INC;
                if (chorusLfoPhase >= TWO_PI) {
                    chorusLfoPhase -= TWO_PI;
                }
                final double wetChorusL = readDelay(chorusBufL, chorusWriteIdx, CHORUS_BUFFER_MASK,
                        CHORUS_CENTER_DELAY + CHORUS_DEPTH * cLfo);
                final double wetChorusR = readDelay(chorusBufR, chorusWriteIdx, CHORUS_BUFFER_MASK,
                        CHORUS_CENTER_DELAY - CHORUS_DEPTH * cLfo);
                chorusWriteIdx = (chorusWriteIdx + 1) & CHORUS_BUFFER_MASK;

                // Tempo-locked 175 BPM stereo ping-pong dub tape delay
                final double wow = Math.sin(dubWowPhase) * DUB_WOW_DEPTH;
                dubWowPhase += DUB_WOW_INC;
                if (dubWowPhase >= TWO_PI) {
                    dubWowPhase -= TWO_PI;
                }
                final double dubTapL = readDelay(dubBufL, dubWriteIdx, DUB_BUFFER_MASK, DUB_DELAY_L + wow);
                final double dubTapR = readDelay(dubBufR, dubWriteIdx, DUB_BUFFER_MASK, DUB_DELAY_R - wow * 0.8);
                dubLpL = DUB_TAPE_DAMP * dubLpL + (1.0 - DUB_TAPE_DAMP) * dubTapL;
                dubLpR = DUB_TAPE_DAMP * dubLpR + (1.0 - DUB_TAPE_DAMP) * dubTapR;
                dubBufL[dubWriteIdx] = Math.tanh(dubSendL + dubLpR * DUB_FEEDBACK);
                dubBufR[dubWriteIdx] = Math.tanh(dubSendR + dubLpL * DUB_FEEDBACK);
                dubWriteIdx = (dubWriteIdx + 1) & DUB_BUFFER_MASK;

                final double preRoomL = dryL + CHORUS_WET_MIX * wetChorusL + DUB_WET_MIX * dubLpL;
                final double preRoomR = dryR + CHORUS_WET_MIX * wetChorusR + DUB_WET_MIX * dubLpR;

                // Warm asymmetric room diffuser
                final double rTapL = roomBufL[roomIdxL];
                final double rTapR = roomBufR[roomIdxR];
                roomLpL = ROOM_DAMPING * roomLpL + (1.0 - ROOM_DAMPING) * rTapL;
                roomLpR = ROOM_DAMPING * roomLpR + (1.0 - ROOM_DAMPING) * rTapR;
                roomBufL[roomIdxL] = preRoomL + ROOM_FEEDBACK * roomLpR;
                roomBufR[roomIdxR] = preRoomR + ROOM_FEEDBACK * roomLpL;
                if (++roomIdxL == ROOM_LEFT_SAMPLES) {
                    roomIdxL = 0;
                }
                if (++roomIdxR == ROOM_RIGHT_SAMPLES) {
                    roomIdxR = 0;
                }

                final double rawL = preRoomL + ROOM_WET_MIX * roomLpL;
                final double rawR = preRoomR + ROOM_WET_MIX * roomLpR;

                // Subtle one-pole warmth filter + smooth soft-knee tape saturation
                warmthL = warmthL + WARMTH_ALPHA * (rawL - warmthL);
                warmthR = warmthR + WARMTH_ALPHA * (rawR - warmthR);
                final double mixL = Math.tanh(warmthL * MASTER_DRIVE);
                final double mixR = Math.tanh(warmthR * MASTER_DRIVE);

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

    private static double readDelay(
            final double[] buffer,
            final int writeIdx,
            final int mask,
            final double delaySamples) {
        final double readPos = writeIdx - delaySamples;
        final int i0 = ((int) Math.floor(readPos)) & mask;
        final int i1 = (i0 + 1) & mask;
        final double frac = readPos - Math.floor(readPos);
        return buffer[i0] + frac * (buffer[i1] - buffer[i0]);
    }

    private static void spawnVoice(
            final List<ActiveVoice> activeVoices,
            final DnbEvent event,
            final long currentSample,
            final int serial) {
        if (event.amplitude() <= 0.0005 || event.durationSamples() <= 0) {
            return;
        }
        if (activeVoices.size() >= MAX_ACTIVE_VOICES) {
            int victim = -1;
            double lowestScore = Double.MAX_VALUE;
            for (int i = 0; i < activeVoices.size(); i++) {
                final ActiveVoice v = activeVoices.get(i);
                final double remaining = Math.max(0.0, (double) (v.durationSamples - v.age) / v.durationSamples);
                final double priority = switch (v.kind) {
                    case FAULT_IMPACT, DNB_KICK, DNB_SNARE, SUB_BASS -> 4.0;
                    case RHODES_CHORD, REESE_BASS, LIQUID_ARP, AMBER_HORN -> 2.5;
                    default -> 1.0;
                };
                final double score = remaining * priority;
                if (score < lowestScore) {
                    lowestScore = score;
                    victim = i;
                }
            }
            if (victim >= 0) {
                activeVoices.remove(victim);
            }
        }
        activeVoices.add(new ActiveVoice(event, Math.max(currentSample, event.startSample()), serial));
    }

    private static double polyBlep(final double t, final double dt) {
        if (t < dt) {
            final double x = t / dt;
            return x + x - x * x - 1.0;
        }
        if (t > 1.0 - dt) {
            final double x = (t - 1.0) / dt;
            return x * x + x + x + 1.0;
        }
        return 0.0;
    }

    private static double polyBlepSaw(final double phase, final double dt) {
        return 2.0 * phase - 1.0 - polyBlep(phase, dt);
    }

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
        final double chorusSend;
        final double dubSend;

        int age = 0;
        double phaseA = 0.0;
        double phaseB = 0.0;
        double phaseC = 0.0;
        double phaseMod = 0.0;

        // ZDF 2-pole State-Variable Filter state & cached coefficients
        double ic1eq = 0.0;
        double ic2eq = 0.0;
        double svfA1 = 0.0;
        double svfA2 = 0.0;
        double svfA3 = 0.0;
        double svfK = 1.0;

        // Secondary simple high-pass / low-pass helper states
        double hpPrevIn = 0.0;
        double hpPrevOut = 0.0;

        // Deterministic xorshift32 & Paul Kellet 3-pole pink noise filter
        int rngState;
        double pinkB0 = 0.0;
        double pinkB1 = 0.0;
        double pinkB2 = 0.0;

        double outL = 0.0;
        double outR = 0.0;

        ActiveVoice(final DnbEvent event, final long startSample, final int serial) {
            this.startSample = startSample;
            this.durationSamples = Math.max(1, event.durationSamples());
            this.kind = event.kind();
            this.baseFreqHz = midiToHz(event.midiPitch());
            this.endFreqHz = midiToHz(event.endMidiPitch());
            this.amplitude = event.amplitude();
            this.brightness = Math.clamp(event.brightness(), 0.0, 1.0);

            final double clampedPan = Math.clamp(event.pan(), -1.0, 1.0);
            final double angle = (clampedPan + 1.0) * (Math.PI * 0.25);
            this.gainL = Math.cos(angle);
            this.gainR = Math.sin(angle);

            this.sidechained = (kind == VoiceKind.SUB_BASS
                    || kind == VoiceKind.REESE_BASS
                    || kind == VoiceKind.RHODES_CHORD);

            this.chorusSend = switch (kind) {
                case RHODES_CHORD -> 0.80;
                case REESE_BASS -> 0.45;
                case AMBER_HORN -> 0.35;
                case LIQUID_ARP -> 0.30;
                default -> 0.0;
            };

            this.dubSend = switch (kind) {
                case AQUA_NOTICE -> 0.85;
                case LIQUID_ARP -> 0.70;
                case UPSET_DROP -> 0.65;
                case LIQUID_PLUCK -> 0.38;
                case RIM_CLICK -> 0.22;
                case AMBER_HORN -> 0.28;
                case FAULT_IMPACT -> 0.42;
                default -> 0.0;
            };

            this.rngState = 0x175B07F3 ^ (serial * 0x45D9F3B) ^ (int) (startSample * 31L);
            if (0 == this.rngState) {
                this.rngState = 0x13579BDF;
            }
            if (kind == VoiceKind.REESE_BASS || kind == VoiceKind.AMBER_HORN) {
                this.phaseB = 0.31;
                this.phaseC = 0.67;
            }
        }

        private double nextWhiteNoise() {
            int x = rngState;
            x ^= (x << 13);
            x ^= (x >>> 17);
            x ^= (x << 5);
            rngState = x;
            return x / (double) Integer.MAX_VALUE;
        }

        private double nextPinkNoise() {
            final double white = nextWhiteNoise();
            pinkB0 = 0.99765 * pinkB0 + white * 0.0990460;
            pinkB1 = 0.96300 * pinkB1 + white * 0.2965164;
            pinkB2 = 0.57000 * pinkB2 + white * 1.0526913;
            return (pinkB0 + pinkB1 + pinkB2 + white * 0.1848) * 0.22;
        }

        private void updateSvf(final double cutoffHz, final double q) {
            final double clampedHz = Math.clamp(cutoffHz, 45.0, 14_500.0);
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

        private double stepSimpleHighPass(final double in, final double coeff) {
            final double out = coeff * (hpPrevOut + in - hpPrevIn);
            hpPrevIn = in;
            hpPrevOut = out;
            return out;
        }

        private double advancePhase(final double phase, final double dt) {
            double next = phase + dt;
            if (next >= 1.0) {
                next -= Math.floor(next);
            }
            return next;
        }

        void step() {
            final double tSec = age / (double) SAMPLE_RATE;
            final double progress = age / (double) durationSamples;
            final double mono = switch (kind) {
                case DNB_KICK -> sampleDnbKick(tSec, progress);
                case DNB_SNARE -> sampleDnbSnare(tSec, progress);
                case GHOST_SNARE -> sampleGhostSnare(tSec, progress);
                case LIQUID_HAT -> sampleLiquidHat(tSec, progress);
                case RIM_CLICK -> sampleRimClick(tSec, progress);
                case SUB_BASS -> sampleSubBass(tSec, progress);
                case REESE_BASS -> sampleReeseBass(tSec, progress);
                case RHODES_CHORD -> sampleRhodesChord(tSec, progress);
                case LIQUID_PLUCK -> sampleLiquidPluck(tSec, progress);
                case LIQUID_ARP -> sampleLiquidArp(tSec, progress);
                case AQUA_NOTICE -> sampleAquaNotice(tSec, progress);
                case AMBER_HORN -> sampleAmberHorn(tSec, progress);
                case UPSET_DROP -> sampleUpsetDrop(tSec, progress);
                case FAULT_IMPACT -> sampleFaultImpact(tSec, progress);
                case AIR_SWEEP -> sampleAirSweep(tSec, progress);
            };
            outL = mono * gainL;
            outR = mono * gainR;
            age++;
        }

        private double sampleDnbKick(final double tSec, final double progress) {
            // Punchy, tight Liquid DnB kick: 165 Hz -> 52 Hz pitch drop + subtle warm transient click
            final double freq = 52.0 + 113.0 * Math.exp(-tSec * 44.0);
            phaseA = advancePhase(phaseA, freq / SAMPLE_RATE);
            final double body = Math.sin(TWO_PI * phaseA);
            final double click = Math.sin(TWO_PI * phaseB) * Math.exp(-tSec * 290.0) * 0.24;
            phaseB = advancePhase(phaseB, 1_150.0 / SAMPLE_RATE);
            final double env = Math.min(1.0, tSec / 0.0022)
                    * Math.exp(-tSec * 15.5)
                    * (1.0 - smoothTail(progress, 0.85));
            return Math.tanh((body + click) * 1.32) * env * amplitude;
        }

        private double sampleDnbSnare(final double tSec, final double progress) {
            // Crisp two-step breakbeat snare: woody 215 Hz shell + sculpted warm 2.6 kHz snare wire
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double wireHz = 2_100.0 + 1_300.0 * brightness * Math.exp(-tSec * 14.0);
                updateSvf(wireHz, 0.88);
            }
            final double shellHz = 186.0 + 58.0 * Math.exp(-tSec * 65.0);
            phaseA = advancePhase(phaseA, shellHz / SAMPLE_RATE);
            final double shell = Math.sin(TWO_PI * phaseA) * Math.exp(-tSec * 28.0) * 0.62;
            final double wireRaw = 0.55 * nextWhiteNoise() + 0.45 * nextPinkNoise();
            final double wire = stepSvfBand(wireRaw) * Math.exp(-tSec * 19.5) * 0.92;
            final double env = Math.min(1.0, tSec / 0.0015) * (1.0 - smoothTail(progress, 0.82));
            return Math.tanh((shell + wire) * 1.18) * env * amplitude;
        }

        private double sampleGhostSnare(final double tSec, final double progress) {
            // Feather-light rolling Amen ghost snare: tight 265 Hz body + soft short brush
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(2_600.0 + 900.0 * brightness, 1.05);
            }
            phaseA = advancePhase(phaseA, 265.0 / SAMPLE_RATE);
            final double shell = Math.sin(TWO_PI * phaseA) * Math.exp(-tSec * 46.0) * 0.48;
            final double brush = stepSvfBand(nextPinkNoise()) * Math.exp(-tSec * 36.0) * 0.78;
            final double env = Math.min(1.0, tSec / 0.0018) * (1.0 - smoothTail(progress, 0.78));
            return (shell + brush) * env * amplitude;
        }

        private double sampleLiquidHat(final double tSec, final double progress) {
            // Silky jazz-ride / breakbeat hi-hat sculpted in 6–9.5 kHz window (never sibilant)
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(6_400.0 + 2_600.0 * brightness, 0.95);
            }
            final double hpNoise = stepSimpleHighPass(nextWhiteNoise(), 0.74);
            phaseA = advancePhase(phaseA, 3_910.0 / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, 5_640.0 / SAMPLE_RATE);
            final double metallic = Math.sin(TWO_PI * phaseA) * Math.sin(TWO_PI * phaseB) * 0.25;
            final double filtered = stepSvfBand(hpNoise * 0.82 + metallic);
            final double decayRate = 48.0 - 24.0 * brightness; // brighter = slightly more open ride air
            final double env = Math.min(1.0, tSec / 0.0012)
                    * Math.exp(-tSec * decayRate)
                    * (1.0 - smoothTail(progress, 0.80));
            return filtered * env * amplitude;
        }

        private double sampleRimClick(final double tSec, final double progress) {
            // Tight woody rimshot (840 Hz + 1,760 Hz modes)
            phaseA = advancePhase(phaseA, 840.0 / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, 1_760.0 / SAMPLE_RATE);
            final double wood = Math.sin(TWO_PI * phaseA) * 0.70
                    + Math.sin(TWO_PI * phaseB) * 0.30 * Math.exp(-tSec * 90.0);
            final double click = nextWhiteNoise() * Math.exp(-tSec * 240.0) * 0.18;
            final double env = Math.min(1.0, tSec / 0.0008)
                    * Math.exp(-tSec * 52.0)
                    * (1.0 - smoothTail(progress, 0.80));
            return (wood + click) * env * amplitude;
        }

        private double sampleSubBass(final double tSec, final double progress) {
            // Deep velvety mono-compatible Liquid sub-bass with smooth pitch glide & warm 2nd harmonic
            final double glide = 1.0 - Math.exp(-tSec * 14.0);
            final double freq = baseFreqHz + (endFreqHz - baseFreqHz) * glide;
            phaseA = advancePhase(phaseA, freq / SAMPLE_RATE);
            final double fund = Math.sin(TWO_PI * phaseA);
            final double warm2nd = Math.sin(TWO_PI * phaseA * 2.0) * (0.14 + 0.10 * brightness);
            final double attack = Math.min(1.0, tSec / 0.012);
            final double release = 1.0 - smoothTail(progress, 0.76);
            return Math.tanh((fund + warm2nd) * 1.15) * attack * release * amplitude;
        }

        private double sampleReeseBass(final double tSec, final double progress) {
            // Classic Liquid/Techstep detuned dual-saw Reese bass through warm resonant ZDF LP
            final double glide = 1.0 - Math.exp(-tSec * 9.0);
            final double freq = baseFreqHz + (endFreqHz - baseFreqHz) * glide;
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double lfo = 0.5 + 0.5 * Math.sin(TWO_PI * 1.35 * tSec);
                final double cutoff = 160.0 + (420.0 + 980.0 * brightness) * (0.45 + 0.55 * lfo);
                updateSvf(cutoff, 1.35 + 1.15 * brightness);
            }
            final double dtA = (freq * 0.9955) / SAMPLE_RATE;
            final double dtB = (freq * 1.0045) / SAMPLE_RATE;
            final double dtSub = (freq * 0.5) / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dtA);
            phaseB = advancePhase(phaseB, dtB);
            phaseC = advancePhase(phaseC, dtSub);
            final double saws = 0.45 * polyBlepSaw(phaseA, dtA) + 0.45 * polyBlepSaw(phaseB, dtB);
            final double filtered = stepSvfLow(saws);
            final double sub = Math.sin(TWO_PI * phaseC) * 0.42;
            final double attack = Math.min(1.0, tSec / 0.022);
            final double release = 1.0 - smoothTail(progress, 0.78);
            return Math.tanh((filtered * 1.15 + sub) * 1.18) * attack * release * amplitude;
        }

        private double sampleRhodesChord(final double tSec, final double progress) {
            // Lush Fender Rhodes electric piano: silky bell-tine transient -> warm pure body + soft tremolo
            final double dt = baseFreqHz / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dt);
            phaseB = advancePhase(phaseB, dt * 2.0);
            phaseC = advancePhase(phaseC, dt * 7.0);
            phaseMod = advancePhase(phaseMod, 3.6 / SAMPLE_RATE);

            final double tineEnv = Math.exp(-tSec * 14.0);
            final double fmIndex = (0.45 + 0.65 * brightness) * tineEnv;
            final double body = Math.sin(TWO_PI * phaseA + fmIndex * Math.sin(TWO_PI * phaseB));
            final double bellTine = Math.sin(TWO_PI * phaseC) * tineEnv * (0.08 + 0.10 * brightness);
            final double tremolo = 0.90 + 0.10 * Math.sin(TWO_PI * phaseMod);

            final double attack = Math.min(1.0, tSec / 0.016);
            final double decay = Math.exp(-tSec * 0.95);
            final double release = 1.0 - smoothTail(progress, 0.72);
            return (body + bellTine) * tremolo * attack * decay * release * amplitude;
        }

        private double sampleLiquidPluck(final double tSec, final double progress) {
            // Warm muted electric-piano / kalimba-like pluck for branch steps & partial leaves
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double cutoff = 420.0 + (1_500.0 + 1_800.0 * brightness) * Math.exp(-tSec * 16.0);
                updateSvf(cutoff, 0.92);
            }
            final double dt = baseFreqHz / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dt);
            phaseB = advancePhase(phaseB, dt * 2.0);
            // Triangle-ish warm oscillator + subtle 2nd harmonic tine
            final double tri = 1.0 - 4.0 * Math.abs(phaseA - 0.5);
            final double tine = Math.sin(TWO_PI * phaseB) * Math.exp(-tSec * 24.0) * 0.25;
            final double filtered = stepSvfLow(tri * 0.85 + tine);
            final double attack = Math.min(1.0, tSec / 0.004);
            final double decay = Math.exp(-tSec * 9.5);
            final double release = 1.0 - smoothTail(progress, 0.78);
            return filtered * attack * decay * release * amplitude;
        }

        private double sampleLiquidArp(final double tSec, final double progress) {
            // Luminous liquid bell-lead chime with gentle glide and airy octave harmonic
            final double glide = 1.0 - Math.exp(-tSec * 22.0);
            final double vib = 1.0 + 0.0032 * Math.sin(TWO_PI * 5.2 * tSec) * Math.min(1.0, tSec / 0.14);
            final double freq = (baseFreqHz + (endFreqHz - baseFreqHz) * glide) * vib;
            phaseA = advancePhase(phaseA, freq / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, (freq * 2.0) / SAMPLE_RATE);
            phaseC = advancePhase(phaseC, (freq * 3.0) / SAMPLE_RATE);

            final double fund = Math.sin(TWO_PI * phaseA);
            final double oct = Math.sin(TWO_PI * phaseB) * (0.26 + 0.16 * brightness) * Math.exp(-tSec * 5.0);
            final double fifth = Math.sin(TWO_PI * phaseC) * 0.09 * Math.exp(-tSec * 11.0);
            final double attack = Math.min(1.0, tSec / 0.008);
            final double decay = Math.exp(-tSec * 3.4);
            final double release = 1.0 - smoothTail(progress, 0.74);
            return (fund + oct + fifth) * attack * decay * release * amplitude;
        }

        private double sampleAquaNotice(final double tSec, final double progress) {
            // Crystalline aqua FM bell ping (clearly informative, airy, dub-delayed)
            final double dt = baseFreqHz / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dt);
            phaseB = advancePhase(phaseB, dt * 2.0);
            phaseC = advancePhase(phaseC, dt * 4.0);
            final double modEnv = Math.exp(-tSec * 14.0);
            final double bell = Math.sin(TWO_PI * phaseA + 0.65 * modEnv * Math.sin(TWO_PI * phaseB))
                    + 0.18 * Math.sin(TWO_PI * phaseC) * Math.exp(-tSec * 18.0);
            final double attack = Math.min(1.0, tSec / 0.0035);
            final double decay = Math.exp(-tSec * 6.8);
            final double release = 1.0 - smoothTail(progress, 0.78);
            return bell * attack * decay * release * amplitude;
        }

        private double sampleAmberHorn(final double tSec, final double progress) {
            // Warm muted brass horn fifth for WARNING alerts (bright for raw, damped for known issues)
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double swell = Math.min(1.0, tSec / 0.045) * Math.exp(-tSec * 2.6);
                final double cutoff = 280.0 + (320.0 + 1_350.0 * brightness) * swell;
                updateSvf(cutoff, 1.15);
            }
            final double dtA = (baseFreqHz * 0.9965) / SAMPLE_RATE;
            final double dtB = (baseFreqHz * 1.0035) / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dtA);
            phaseB = advancePhase(phaseB, dtB);
            final double brass = 0.50 * polyBlepSaw(phaseA, dtA) + 0.50 * polyBlepSaw(phaseB, dtB);
            final double filtered = stepSvfLow(brass);
            final double attack = Math.min(1.0, tSec / 0.024);
            final double release = 1.0 - smoothTail(progress, 0.72);
            return Math.tanh(filtered * 1.25) * attack * release * amplitude;
        }

        private double sampleUpsetDrop(final double tSec, final double progress) {
            // Hollow band-passed downward dub fifth for UPSET (parent passed, child failed downstream)
            final double glide = 1.0 - Math.exp(-tSec * 11.0);
            final double freq = baseFreqHz + (endFreqHz - baseFreqHz) * glide;
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double bpHz = Math.max(220.0, freq * (2.6 - 1.2 * progress));
                updateSvf(bpHz, 2.2);
            }
            final double dt = freq / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dt);
            final double raw = polyBlepSaw(phaseA, dt) * 0.65 + Math.sin(TWO_PI * phaseA) * 0.35;
            final double filtered = stepSvfBand(raw);
            final double attack = Math.min(1.0, tSec / 0.006);
            final double decay = Math.exp(-tSec * 6.2);
            final double release = 1.0 - smoothTail(progress, 0.76);
            return filtered * attack * decay * release * amplitude;
        }

        private double sampleFaultImpact(final double tSec, final double progress) {
            // Unmistakable DnB Reese pitch-dive + deep sub impact + sculpted rewind grit
            final double dive = 1.0 - Math.exp(-tSec * 6.5);
            final double freq = baseFreqHz + (endFreqHz - baseFreqHz) * dive;
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double cutoff = 220.0 + (260.0 + 1_650.0 * brightness) * Math.exp(-tSec * 3.8);
                updateSvf(cutoff, 1.85 + 1.15 * brightness);
            }
            final double dtA = (freq * 0.991) / SAMPLE_RATE;
            final double dtB = (freq * 1.009) / SAMPLE_RATE;
            final double dtSub = (freq * 0.5) / SAMPLE_RATE;
            phaseA = advancePhase(phaseA, dtA);
            phaseB = advancePhase(phaseB, dtB);
            phaseC = advancePhase(phaseC, dtSub);

            // Detuned growling Reese + subtle ring-mod dissonant edge for raw failures (brightness > 0.5)
            final double reese = 0.50 * polyBlepSaw(phaseA, dtA) + 0.50 * polyBlepSaw(phaseB, dtB);
            final double grit = (brightness > 0.5)
                    ? (reese * Math.sin(TWO_PI * phaseB * 1.414) * 0.32 * Math.exp(-tSec * 5.0))
                    : 0.0;
            final double filtered = stepSvfLow(reese + grit);
            final double sub = Math.sin(TWO_PI * phaseC) * 0.58 * Math.exp(-tSec * 2.4);
            final double attack = Math.min(1.0, tSec / 0.006);
            final double release = 1.0 - smoothTail(progress, 0.72);
            return Math.tanh((filtered * 1.35 + sub) * 1.32) * attack * release * amplitude;
        }

        private double sampleAirSweep(final double tSec, final double progress) {
            // Smooth resonant pink-noise sweep (downward LP exhale for pruning, upward BP riser for compaction)
            final double cutoffHz = baseFreqHz * Math.pow(Math.max(0.15, endFreqHz / Math.max(1.0, baseFreqHz)), progress);
            final boolean isRising = endFreqHz > baseFreqHz;
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(cutoffHz, isRising ? (1.8 + 0.9 * brightness) : 1.1);
            }
            final double pink = nextPinkNoise();
            final double shaped = isRising ? stepSvfBand(pink) : stepSvfLow(pink);
            final double attack = Math.min(1.0, tSec / (isRising ? 0.08 : 0.025));
            final double release = 1.0 - smoothTail(progress, isRising ? 0.84 : 0.55);
            return shaped * attack * release * amplitude;
        }

        private static double smoothTail(final double progress, final double start) {
            if (progress <= start) {
                return 0.0;
            }
            final double x = Math.min(1.0, (progress - start) / (1.0 - start));
            return x * x * (3.0 - 2.0 * x);
        }
    }

    private static void writeWavHeader(final OutputStream out, final long dataBytes) throws IOException {
        final int byteRate = SAMPLE_RATE * CHANNELS * Short.BYTES;
        final int blockAlign = CHANNELS * Short.BYTES;
        final byte[] header = new byte[44];
        writeAscii(header, 0, "RIFF");
        writeLeInt(header, 4, (int) (36L + dataBytes));
        writeAscii(header, 8, "WAVE");
        writeAscii(header, 12, "fmt ");
        writeLeInt(header, 16, 16);
        writeLeShort(header, 20, 1); // PCM
        writeLeShort(header, 22, CHANNELS);
        writeLeInt(header, 24, SAMPLE_RATE);
        writeLeInt(header, 28, byteRate);
        writeLeShort(header, 32, blockAlign);
        writeLeShort(header, 34, Short.SIZE);
        writeAscii(header, 36, "data");
        writeLeInt(header, 40, (int) dataBytes);
        out.write(header);
    }

    private static void writeAscii(final byte[] target, final int offset, final String text) {
        for (int i = 0; i < text.length(); i++) {
            target[offset + i] = (byte) text.charAt(i);
        }
    }

    private static void writeLeShort(final byte[] target, final int offset, final int value) {
        target[offset] = (byte) (value & 0xFF);
        target[offset + 1] = (byte) ((value >>> 8) & 0xFF);
    }

    private static void writeLeInt(final byte[] target, final int offset, final int value) {
        target[offset] = (byte) (value & 0xFF);
        target[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        target[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        target[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }
}
