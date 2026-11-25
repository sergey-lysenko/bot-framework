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
 * Procedural physical-modeling and live-rig synthesizer for the Angine de Poitrine-inspired
 * microtonal math-rock sonifier ({@code adp}).
 * <p>
 * Models the Québécois duo's signature dual-amp live setup in 24-tone equal temperament (24-TET):
 * <ul>
 *     <li><b>Microtonal Double-Neck Guitar/Bass (Khn de Poitrine):</b> Fractional-delay Karplus-Strong
 *     plucked string synthesis with single-coil Stratocaster pickup comb filtering and independent
 *     Precision Bass split-coil low-end reinforcement, capable of exact 50-cent quarter-tone fretting.</li>
 *     <li><b>Foot-Controlled Ring Modulator:</b> True ring-modulation carrier sweeps on the guitar signal
 *     for metallic warning stabs and Dada failure meltdowns.</li>
 *     <li><b>Cloth-Dampened 4-Piece Drum Kit (Klek de Poitrine):</b> Heavily muffled acoustic kick,
 *     tight woody piccolo snare with ghost notes, dampened toms, and dark extra-dry bronze cymbals.</li>
 *     <li><b>Live-Room Stereo Bus:</b> Independent dual-amp stereo placement feeding a tight vintage
 *     spring/room slapback and subtle looper doubler into warm analog console saturation.</li>
 * </ul>
 */
final class AdpSynth {

    static final int SAMPLE_RATE = 44_100;
    static final int CHANNELS = 2;
    static final double BPM = 136.0;

    /** 16th-note step at 136 BPM = (44100 * 60) / (136 * 4) = ~4864 samples (~110.3 ms). */
    static final int STEP_SAMPLES = (int) Math.round(SAMPLE_RATE * 60.0 / (BPM * 4.0));

    /** Odd-meter 7/8 bar = 14 sixteenth-note steps (~1.544 seconds). */
    static final int STEPS_PER_BAR = 14;
    static final int BAR_SAMPLES = STEP_SAMPLES * STEPS_PER_BAR;

    /** 2.0-second tail for spring room decay and final cadence ring-out. */
    static final int TAIL_SAMPLES = (int) (SAMPLE_RATE * 2.00);

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final int MAX_ACTIVE_VOICES = 48;
    private static final int IO_BUFFER_BYTES = 16_384;
    private static final int FILTER_CONTROL_STRIDE = 16;

    // Tight KEXP-style live room & vintage spring tank (short, dry reflections)
    private static final int ROOM_DELAY_L = 1_019; // ~23.1 ms (prime)
    private static final int ROOM_DELAY_R = 1_367; // ~31.0 ms (prime)
    private static final int ROOM_BUFFER_SIZE = 4_096;
    private static final int ROOM_BUFFER_MASK = ROOM_BUFFER_SIZE - 1;
    private static final double ROOM_FEEDBACK = 0.24;
    private static final double ROOM_DAMPING = 0.55;
    private static final double ROOM_WET_MIX = 0.16;

    // Subtle multi-track looper slapback doubler (1 dotted-16th-ish ~150 ms / ~175 ms)
    private static final int LOOP_DELAY_L = 6_619; // ~150.1 ms
    private static final int LOOP_DELAY_R = 7_723; // ~175.1 ms
    private static final int LOOP_BUFFER_SIZE = 16_384;
    private static final int LOOP_BUFFER_MASK = LOOP_BUFFER_SIZE - 1;
    private static final double LOOP_FEEDBACK = 0.20;
    private static final double LOOP_DAMPING = 0.50;
    private static final double LOOP_WET_MIX = 0.14;

    private static final double MASTER_DRIVE = 1.24;
    private static final double PCM_HEADROOM = 31_400.0;

    private AdpSynth() {
    }

    /**
     * Angine de Poitrine live-rig voice models.
     */
    enum VoiceKind {
        /** Heavily cloth-muffled single-pedal acoustic bass drum. */
        DRY_KICK,
        /** Crisp, dry, cloth-dampened piccolo snare (accents and ghost notes). */
        PICCOLO_SNARE,
        /** Muffled rack/floor tom for odd-meter fills and compaction turnarounds. */
        DRY_TOM,
        /** Dark, trashy Meinl Extra Dry hi-hat chick/bark. */
        EXTRA_DRY_HAT,
        /** Dark dry bronze ride ping / bell accent. */
        DRY_RIDE_BELL,
        /** Microtonal Precision Bass bottom neck through dedicated warm bass amp. */
        P_BASS,
        /** Microtonal Stratocaster clean/crunchy single-coil plucked riff (top neck). */
        STRAT_RIFF,
        /** Tight palm-muted Stratocaster chug for branch traversals. */
        STRAT_MUTE,
        /** Bright harmonized Stratocaster live-looper overdub for completed leaves & layers. */
        STRAT_HARMONY,
        /** Glassy natural guitar harmonic (12th/7th fret) for NOTICE events. */
        NOTICE_HARMONIC,
        /** Microtonal downward string bend (3/4-tone weep) for upset ancestor nodes. */
        UPSET_BEND,
        /** Foot-controlled ring-modulated guitar stab for WARNING cues. */
        RING_MOD_WARN,
        /** Fuzz-driven microtonal dive + clangorous ring modulator for raw FAILED regressions. */
        FAULT_MELTDOWN,
        /** Percussive left-hand muted guitar string scrape for pruned/hidden nodes. */
        MUTE_SCRAPE
    }

    /**
     * Immutable specification of a scheduled stereo microtonal rock synthesis event.
     * Supports 24-TET quarter-tone pitches via fractional MIDI numbers (e.g. {@code 56.5} = A-half-flat 3).
     */
    record AdpEvent(
            long startSample,
            int durationSamples,
            VoiceKind kind,
            double midiPitch,
            double endMidiPitch,
            double amplitude,
            double brightness,
            double pan) {

        AdpEvent(
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

    /**
     * Converts a continuous (24-TET quarter-tone capable) MIDI pitch into frequency in Hz.
     */
    static double midiToFreq(final double midi) {
        return 440.0 * Math.pow(2.0, (midi - 69.0) / 12.0);
    }

    /**
     * Renders scheduled Angine de Poitrine events into a deterministic 16-bit stereo PCM WAV file.
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
            final Map<Integer, List<AdpEvent>> eventsByFrame) throws IOException {

        final long dataBytes = totalSamples * CHANNELS * Short.BYTES;
        if (dataBytes > Integer.MAX_VALUE - 36L) {
            throw new IOException("Progression ADP audio exceeds the WAV file size limit");
        }

        final List<ActiveVoice> activeVoices = new ArrayList<>(MAX_ACTIVE_VOICES);

        // Live-room / spring tank state
        final double[] roomBufL = new double[ROOM_BUFFER_SIZE];
        final double[] roomBufR = new double[ROOM_BUFFER_SIZE];
        int roomWriteIdx = 0;
        double roomLpL = 0.0;
        double roomLpR = 0.0;

        // Looper slapback doubler state
        final double[] loopBufL = new double[LOOP_BUFFER_SIZE];
        final double[] loopBufR = new double[LOOP_BUFFER_SIZE];
        int loopWriteIdx = 0;
        double loopLpL = 0.0;
        double loopLpR = 0.0;

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
                    final List<AdpEvent> incoming = eventsByFrame.get(frameIndex);
                    if (null != incoming) {
                        for (int i = 0; i < incoming.size(); i++) {
                            spawnVoice(activeVoices, incoming.get(i), voiceSerial++);
                        }
                    }
                }

                double dryL = 0.0;
                double dryR = 0.0;
                double roomSendL = 0.0;
                double roomSendR = 0.0;
                double loopSendL = 0.0;
                double loopSendR = 0.0;

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
                    if (v.roomSend > 0.0) {
                        roomSendL += v.outL * v.roomSend;
                        roomSendR += v.outR * v.roomSend;
                    }
                    if (v.loopSend > 0.0) {
                        loopSendL += v.outL * v.loopSend;
                        loopSendR += v.outR * v.loopSend;
                    }
                }

                // Short cross-fed live room / spring tank
                final int roomReadL = (roomWriteIdx - ROOM_DELAY_L) & ROOM_BUFFER_MASK;
                final int roomReadR = (roomWriteIdx - ROOM_DELAY_R) & ROOM_BUFFER_MASK;
                final double roomOutL = roomBufL[roomReadL];
                final double roomOutR = roomBufR[roomReadR];
                roomLpL += ROOM_DAMPING * (roomOutL - roomLpL);
                roomLpR += ROOM_DAMPING * (roomOutR - roomLpR);
                roomBufL[roomWriteIdx] = roomSendL + roomLpR * ROOM_FEEDBACK;
                roomBufR[roomWriteIdx] = roomSendR + roomLpL * ROOM_FEEDBACK;
                roomWriteIdx = (roomWriteIdx + 1) & ROOM_BUFFER_MASK;

                // Subtle looper slapback doubler
                final int loopReadL = (loopWriteIdx - LOOP_DELAY_L) & LOOP_BUFFER_MASK;
                final int loopReadR = (loopWriteIdx - LOOP_DELAY_R) & LOOP_BUFFER_MASK;
                final double loopOutL = loopBufL[loopReadL];
                final double loopOutR = loopBufR[loopReadR];
                loopLpL += LOOP_DAMPING * (loopOutL - loopLpL);
                loopLpR += LOOP_DAMPING * (loopOutR - loopLpR);
                loopBufL[loopWriteIdx] = loopSendL + loopLpR * LOOP_FEEDBACK;
                loopBufR[loopWriteIdx] = loopSendR + loopLpL * LOOP_FEEDBACK;
                loopWriteIdx = (loopWriteIdx + 1) & LOOP_BUFFER_MASK;

                final double sumL = (dryL + roomOutL * ROOM_WET_MIX + loopOutL * LOOP_WET_MIX) * MASTER_DRIVE;
                final double sumR = (dryR + roomOutR * ROOM_WET_MIX + loopOutR * LOOP_WET_MIX) * MASTER_DRIVE;

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

    private static void spawnVoice(
            final List<ActiveVoice> activeVoices,
            final AdpEvent event,
            final int serial) {

        if (activeVoices.size() >= MAX_ACTIVE_VOICES) {
            int quietestIdx = -1;
            double quietestAmp = Double.MAX_VALUE;
            for (int i = 0; i < activeVoices.size(); i++) {
                final ActiveVoice v = activeVoices.get(i);
                if (v.kind != VoiceKind.DRY_KICK && v.kind != VoiceKind.P_BASS && v.amplitude < quietestAmp) {
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
     * Active physical-modeling / analog-rig voice.
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
        final double roomSend;
        final double loopSend;

        int age = 0;
        double phaseA = 0.0;
        double phaseB = 0.0;
        double phaseC = 0.0;
        double phaseMod = 0.0;

        // Karplus-Strong fractional-delay string state (only allocated for plucked string voices)
        final double[] ksBuf;
        final int ksLen;
        final int ksIntDelay;
        final double ksFrac;
        final double ksDamping;
        int ksWriteIdx = 0;
        double ksPrev = 0.0;
        double dcPrevIn = 0.0;
        double dcPrevOut = 0.0;

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

        ActiveVoice(final AdpEvent event, final int serial) {
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

            this.roomSend = switch (kind) {
                case STRAT_RIFF -> 0.42;
                case STRAT_HARMONY -> 0.52;
                case NOTICE_HARMONIC -> 0.65;
                case RING_MOD_WARN -> 0.48;
                case FAULT_MELTDOWN -> 0.35;
                case PICCOLO_SNARE -> 0.22;
                case DRY_TOM -> 0.20;
                case DRY_RIDE_BELL -> 0.30;
                default -> 0.08;
            };

            this.loopSend = switch (kind) {
                case STRAT_RIFF -> 0.38;
                case STRAT_HARMONY -> 0.55;
                case NOTICE_HARMONIC -> 0.45;
                case RING_MOD_WARN -> 0.28;
                default -> 0.0;
            };

            this.rngState = 0x4D415441 ^ (serial * 0x45D9F3B) ^ (int) (startSample * 31L);
            if (0 == this.rngState) {
                this.rngState = 0x2468ACE1;
            }

            if (isPluckedString(kind)) {
                final double clampedFreq = Math.clamp(baseFreqHz, 35.0, 3_500.0);
                // Subtract 0.5 samples to compensate for the 2-point loop-averaging filter phase delay
                final double exactDelay = Math.max(2.5, (SAMPLE_RATE / clampedFreq) - 0.5);
                this.ksIntDelay = (int) Math.floor(exactDelay);
                this.ksFrac = exactDelay - this.ksIntDelay;
                this.ksLen = this.ksIntDelay + 3;
                this.ksBuf = new double[this.ksLen];
                this.ksDamping = switch (kind) {
                    case STRAT_MUTE -> 0.968;
                    case P_BASS -> 0.992;
                    case RING_MOD_WARN -> 0.994;
                    default -> 0.9955;
                };
                initPluckedString();
            } else {
                this.ksBuf = null;
                this.ksLen = 0;
                this.ksIntDelay = 0;
                this.ksFrac = 0.0;
                this.ksDamping = 0.0;
            }
        }

        private static boolean isPluckedString(final VoiceKind kind) {
            return kind == VoiceKind.P_BASS
                    || kind == VoiceKind.STRAT_RIFF
                    || kind == VoiceKind.STRAT_MUTE
                    || kind == VoiceKind.STRAT_HARMONY
                    || kind == VoiceKind.RING_MOD_WARN;
        }

        private void initPluckedString() {
            // Shape deterministic pluck noise according to instrument & brightness (warm low-string weight)
            final double lpAlpha = (kind == VoiceKind.P_BASS)
                    ? (0.16 + 0.18 * brightness)
                    : ((kind == VoiceKind.STRAT_MUTE) ? (0.24 + 0.20 * brightness) : (0.36 + 0.30 * brightness));
            double smoothed = 0.0;
            double mean = 0.0;
            for (int i = 0; i < ksLen; i++) {
                smoothed += lpAlpha * (nextNoise() - smoothed);
                ksBuf[i] = smoothed;
                mean += smoothed;
            }
            mean /= ksLen;
            for (int i = 0; i < ksLen; i++) {
                ksBuf[i] -= mean;
            }

            // Warm pickup comb notch preserving strong low-string fundamental body
            final double pickRatio = (kind == VoiceKind.STRAT_HARMONY) ? 0.22 : 0.28;
            final int pickDelay = Math.max(1, (int) Math.round(ksIntDelay * pickRatio));
            final double combMix = (kind == VoiceKind.STRAT_HARMONY) ? 0.42 : 0.30;
            final double[] shaped = new double[ksLen];
            double peak = 1e-6;
            for (int i = 0; i < ksLen; i++) {
                final int shifted = (i - pickDelay + ksLen) % ksLen;
                shaped[i] = ksBuf[i] - combMix * ksBuf[shifted];
                final double abs = Math.abs(shaped[i]);
                if (abs > peak) {
                    peak = abs;
                }
            }
            final double norm = 0.95 / peak;
            for (int i = 0; i < ksLen; i++) {
                ksBuf[i] = shaped[i] * norm;
            }
        }

        private double stepKarplusStrong() {
            int idx0 = ksWriteIdx - ksIntDelay;
            if (idx0 < 0) idx0 += ksLen;
            int idx1 = idx0 - 1;
            if (idx1 < 0) idx1 += ksLen;

            final double y0 = ksBuf[idx0];
            final double y1 = ksBuf[idx1];
            final double delayed = y0 + ksFrac * (y1 - y0);
            final double filtered = 0.5 * (delayed + ksPrev) * ksDamping;
            ksPrev = delayed;

            ksBuf[ksWriteIdx] = filtered;
            ksWriteIdx++;
            if (ksWriteIdx >= ksLen) {
                ksWriteIdx = 0;
            }

            // Gentle DC blocker
            final double dcOut = filtered - dcPrevIn + 0.995 * dcPrevOut;
            dcPrevIn = filtered;
            dcPrevOut = dcOut;
            return dcOut;
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
            final double clampedHz = Math.clamp(cutoffHz, 40.0, 14_500.0);
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
                case DRY_KICK -> sampleDryKick(tSec, progress);
                case PICCOLO_SNARE -> samplePiccoloSnare(tSec, progress);
                case DRY_TOM -> sampleDryTom(tSec, progress);
                case EXTRA_DRY_HAT -> sampleExtraDryHat(tSec, progress);
                case DRY_RIDE_BELL -> sampleDryRideBell(tSec, progress);
                case P_BASS -> samplePBass(tSec, progress);
                case STRAT_RIFF -> sampleStratRiff(tSec, progress);
                case STRAT_MUTE -> sampleStratMute(tSec, progress);
                case STRAT_HARMONY -> sampleStratHarmony(tSec, progress);
                case NOTICE_HARMONIC -> sampleNoticeHarmonic(tSec, progress);
                case UPSET_BEND -> sampleUpsetBend(tSec, progress);
                case RING_MOD_WARN -> sampleRingModWarn(tSec, progress);
                case FAULT_MELTDOWN -> sampleFaultMeltdown(tSec, progress);
                case MUTE_SCRAPE -> sampleMuteScrape(tSec, progress);
            };

            outL = mono * gainL;
            outR = mono * gainR;
            age++;
        }

        private double sampleDryKick(final double tSec, final double progress) {
            // Cloth-dampened single-pedal acoustic bass drum: punchy woody thump, zero club boom
            final double freq = 54.0 + 96.0 * Math.exp(-tSec * 58.0);
            phaseA = advancePhase(phaseA, freq / SAMPLE_RATE);
            final double body = Math.sin(TWO_PI * phaseA);

            phaseB = advancePhase(phaseB, 920.0 / SAMPLE_RATE);
            final double beater = (Math.sin(TWO_PI * phaseB) * 0.55 + nextNoise() * 0.45)
                    * Math.exp(-tSec * 360.0) * 0.30;

            final double env = Math.min(1.0, tSec / 0.0010)
                    * Math.exp(-tSec * 23.0)
                    * (1.0 - smoothTail(progress, 0.78));
            return Math.tanh((body + beater) * 1.40) * env * amplitude;
        }

        private double samplePiccoloSnare(final double tSec, final double progress) {
            // Klek's signature tight cloth-muffled piccolo snare: high woody shell crack + short dry wires
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(2_500.0 + 900.0 * brightness, 0.95);
            }
            phaseA = advancePhase(phaseA, (315.0 + 75.0 * Math.exp(-tSec * 90.0)) / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, 495.0 / SAMPLE_RATE);
            final double shell = (Math.sin(TWO_PI * phaseA) * 0.65 + Math.sin(TWO_PI * phaseB) * 0.25)
                    * Math.exp(-tSec * 42.0);
            final double wires = stepSvfBand(nextNoise()) * Math.exp(-tSec * 36.0) * 0.82;

            final double attack = Math.min(1.0, tSec / 0.0008);
            final double tail = 1.0 - smoothTail(progress, 0.80);
            return Math.tanh((shell + wires) * 1.35) * attack * tail * amplitude;
        }

        private double sampleDryTom(final double tSec, final double progress) {
            // Cloth-muffled rack/floor tom
            final double freq = baseFreqHz + (baseFreqHz * 0.36) * Math.exp(-tSec * 36.0);
            phaseA = advancePhase(phaseA, freq / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, (freq * 1.58) / SAMPLE_RATE);
            final double head = Math.sin(TWO_PI * phaseA) * 0.80
                    + Math.sin(TWO_PI * phaseB) * 0.18 * Math.exp(-tSec * 48.0);
            final double stick = nextNoise() * Math.exp(-tSec * 320.0) * 0.22;

            final double env = Math.min(1.0, tSec / 0.0012)
                    * Math.exp(-tSec * 19.0)
                    * (1.0 - smoothTail(progress, 0.80));
            return Math.tanh((head + stick) * 1.25) * env * amplitude;
        }

        private double sampleExtraDryHat(final double tSec, final double progress) {
            // Meinl Byzance Extra Dry hi-hat: dark, trashy, short bronze chick (no shrill treble)
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(4_400.0 + 1_400.0 * brightness, 1.15);
            }
            phaseA = advancePhase(phaseA, 2_410.0 / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, 3_570.0 / SAMPLE_RATE);
            phaseC = advancePhase(phaseC, 4_930.0 / SAMPLE_RATE);
            final double bronze = Math.sin(TWO_PI * phaseA) * Math.sin(TWO_PI * phaseB) * 0.38
                    + Math.sin(TWO_PI * phaseC) * 0.16;
            final double filtered = stepSvfBand(nextNoise() * 0.65 + bronze);

            final double decayRate = (brightness > 0.65) ? 24.0 : 62.0;
            final double env = Math.min(1.0, tSec / 0.0008)
                    * Math.exp(-tSec * decayRate)
                    * (1.0 - smoothTail(progress, 0.78));
            return filtered * env * amplitude;
        }

        private double sampleDryRideBell(final double tSec, final double progress) {
            // Extra-dry dark ride cymbal ping / bell
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(3_600.0 + 1_100.0 * brightness, 1.20);
            }
            final double pingHz = Math.clamp(baseFreqHz * 2.0, 520.0, 1_450.0);
            phaseA = advancePhase(phaseA, pingHz / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, (pingHz * 2.38) / SAMPLE_RATE);
            phaseC = advancePhase(phaseC, (pingHz * 3.79) / SAMPLE_RATE);
            final double bell = Math.sin(TWO_PI * phaseA) * 0.52
                    + Math.sin(TWO_PI * phaseB) * 0.30 * Math.exp(-tSec * 9.0)
                    + Math.sin(TWO_PI * phaseC) * 0.18 * Math.exp(-tSec * 16.0);
            final double wash = stepSvfBand(nextNoise()) * Math.exp(-tSec * 14.0) * 0.32;

            final double env = Math.min(1.0, tSec / 0.0010)
                    * Math.exp(-tSec * 6.5)
                    * (1.0 - smoothTail(progress, 0.84));
            return (bell + wash) * env * amplitude;
        }

        private double samplePBass(final double tSec, final double progress) {
            // Microtonal Precision Bass (bottom neck): Karplus-Strong pluck + split-coil fundamental + bass cab
            final double ks = stepKarplusStrong();
            phaseA = advancePhase(phaseA, baseFreqHz / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, (baseFreqHz * 2.0) / SAMPLE_RATE);
            final double splitCoil = Math.sin(TWO_PI * phaseA) * 0.42 + Math.sin(TWO_PI * phaseB) * 0.14;

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double cutoff = 310.0 + (320.0 + 520.0 * brightness) * Math.exp(-tSec * 12.0);
                updateSvf(cutoff, 1.15);
            }

            final double cab = stepSvfLow(ks * 0.72 + splitCoil);
            final double attack = Math.min(1.0, tSec / 0.0025);
            final double decay = Math.exp(-tSec * 4.8) * (1.0 - smoothTail(progress, 0.80));
            return Math.tanh(cab * 1.55) * attack * decay * amplitude;
        }

        private double sampleStratRiff(final double tSec, final double progress) {
            // Microtonal low-string Strat / baritone riff: Karplus-Strong pluck + warm fundamental body
            final double ks = stepKarplusStrong();
            phaseA = advancePhase(phaseA, baseFreqHz / SAMPLE_RATE);
            final double body = Math.sin(TWO_PI * phaseA) * 0.24;

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double cutoff = 900.0 + 1_300.0 * brightness * Math.exp(-tSec * 5.5);
                updateSvf(cutoff, 1.20);
            }
            // Subtle asymmetric tube amp crunch
            final double raw = ks * 0.82 + body;
            final double amp = Math.tanh((raw + 0.06 * raw * raw) * (1.45 + 0.50 * brightness));
            final double cab = stepSvfLow(amp);

            final double attack = Math.min(1.0, tSec / 0.0014);
            final double decay = Math.exp(-tSec * 3.8) * (1.0 - smoothTail(progress, 0.82));
            return cab * attack * decay * amplitude;
        }

        private double sampleStratMute(final double tSec, final double progress) {
            // Tight palm-muted bass-guitar / low-string chug for branch traversals
            final double ks = stepKarplusStrong();
            phaseA = advancePhase(phaseA, baseFreqHz / SAMPLE_RATE);
            final double body = Math.sin(TWO_PI * phaseA) * 0.28;

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double cutoff = 480.0 + 680.0 * brightness * Math.exp(-tSec * 16.0);
                updateSvf(cutoff, 1.30);
            }
            final double amp = Math.tanh((ks * 0.78 + body) * 1.60);
            final double cab = stepSvfLow(amp);

            final double attack = Math.min(1.0, tSec / 0.0012);
            final double decay = Math.exp(-tSec * 13.0) * (1.0 - smoothTail(progress, 0.75));
            return cab * attack * decay * amplitude;
        }

        private double sampleStratHarmony(final double tSec, final double progress) {
            // Harmonized Stratocaster looper overdub with warm cabinet voicing
            final double ks = stepKarplusStrong();
            phaseA = advancePhase(phaseA, baseFreqHz / SAMPLE_RATE);
            final double body = Math.sin(TWO_PI * phaseA) * 0.18;

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                final double cutoff = 1_300.0 + 1_650.0 * brightness * Math.exp(-tSec * 4.2);
                updateSvf(cutoff, 1.25);
            }
            final double raw = ks * 0.86 + body;
            final double amp = Math.tanh((raw + 0.08 * raw * raw) * (1.50 + 0.55 * brightness));
            final double cab = stepSvfLow(amp);

            final double attack = Math.min(1.0, tSec / 0.0014);
            final double decay = Math.exp(-tSec * 2.8) * (1.0 - smoothTail(progress, 0.84));
            return cab * attack * decay * amplitude;
        }

        private double sampleNoticeHarmonic(final double tSec, final double progress) {
            // Glassy 12th/7th-fret electric guitar natural harmonic bell
            phaseA = advancePhase(phaseA, baseFreqHz / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, (baseFreqHz * 2.0) / SAMPLE_RATE);
            final double pick = nextNoise() * Math.exp(-tSec * 280.0) * 0.14;
            final double tone = Math.sin(TWO_PI * phaseA) * 0.80
                    + Math.sin(TWO_PI * phaseB) * 0.20 * Math.exp(-tSec * 6.0)
                    + pick;

            final double attack = Math.min(1.0, tSec / 0.0020);
            final double decay = Math.exp(-tSec * 4.2) * (1.0 - smoothTail(progress, 0.82));
            return tone * attack * decay * amplitude;
        }

        private double sampleUpsetBend(final double tSec, final double progress) {
            // Microtonal weeping 3/4-tone downward guitar string bend
            final double glide = 1.0 - Math.exp(-tSec * 8.5);
            final double freq = baseFreqHz + (endFreqHz - baseFreqHz) * glide;
            phaseA = advancePhase(phaseA, freq / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, (freq * 2.0) / SAMPLE_RATE);
            phaseC = advancePhase(phaseC, (freq * 3.0) / SAMPLE_RATE);

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(900.0 + 600.0 * brightness, 1.15);
            }
            final double string = Math.sin(TWO_PI * phaseA) * 0.62
                    + Math.sin(TWO_PI * phaseB) * 0.28
                    + Math.sin(TWO_PI * phaseC) * 0.14;
            final double cab = stepSvfLow(Math.tanh(string * 1.45));

            final double attack = Math.min(1.0, tSec / 0.004);
            final double decay = Math.exp(-tSec * 5.2) * (1.0 - smoothTail(progress, 0.82));
            return cab * attack * decay * amplitude;
        }

        private double sampleRingModWarn(final double tSec, final double progress) {
            // Khn's foot-controlled ring-modulator pedal on microtonal bass/guitar!
            final double ks = stepKarplusStrong();
            final double carrierHz = 115.0 + 140.0 * brightness * (0.40 + 0.60 * progress);
            phaseMod = advancePhase(phaseMod, carrierHz / SAMPLE_RATE);
            final double ringMod = ks * Math.sin(TWO_PI * phaseMod);
            final double blend = 0.35 * ks + 0.65 * ringMod;

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(1_650.0 + 850.0 * brightness, 1.25);
            }
            final double cab = stepSvfLow(Math.tanh(blend * 1.65));

            final double attack = Math.min(1.0, tSec / 0.002);
            final double decay = Math.exp(-tSec * 4.4) * (1.0 - smoothTail(progress, 0.82));
            return cab * attack * decay * amplitude;
        }

        private double sampleFaultMeltdown(final double tSec, final double progress) {
            // Dada failure breakdown: fuzz-saturated microtonal guitar dive + descending ring-mod clang
            final double glide = Math.min(1.0, Math.pow(progress, 1.5));
            final double freq = Math.max(36.0, baseFreqHz + (endFreqHz - baseFreqHz) * glide);

            phaseA = advancePhase(phaseA, freq / SAMPLE_RATE);
            phaseB = advancePhase(phaseB, (freq * 1.031) / SAMPLE_RATE); // slight quarter-tone beating dyad
            final double carrierHz = 290.0 - 175.0 * glide;
            phaseMod = advancePhase(phaseMod, carrierHz / SAMPLE_RATE);

            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(Math.max(240.0, 1_850.0 * (1.0 - 0.65 * glide)), 1.40);
            }

            final double raw = (2.0 * phaseA - 1.0) * 0.55 + (2.0 * phaseB - 1.0) * 0.45;
            final double fuzz = Math.tanh(raw * 2.6);
            final double ringMod = fuzz * Math.sin(TWO_PI * phaseMod);
            final double filtered = stepSvfLow(0.45 * fuzz + 0.55 * ringMod);

            final double attack = Math.min(1.0, tSec / 0.003);
            final double decay = (1.0 - 0.55 * glide) * (1.0 - smoothTail(progress, 0.84));
            return Math.tanh(filtered * 1.40) * attack * decay * amplitude;
        }

        private double sampleMuteScrape(final double tSec, final double progress) {
            // Percussive left-hand muted guitar string scrape for pruned/hidden nodes
            final double cutoff = Math.max(280.0, 1_650.0 - 1_200.0 * progress);
            if (0 == (age & (FILTER_CONTROL_STRIDE - 1))) {
                updateSvf(cutoff, 2.2);
            }
            final double filtered = stepSvfBand(nextNoise());
            final double env = Math.min(1.0, tSec / 0.004)
                    * Math.exp(-tSec * 14.0)
                    * (1.0 - smoothTail(progress, 0.78));
            return filtered * env * amplitude;
        }
    }
}
