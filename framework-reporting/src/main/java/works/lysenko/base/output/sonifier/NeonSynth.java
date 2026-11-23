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
 * Deterministic true-stereo subtractive analog &amp; cyberpunk synthesizer rendering
 * 44.1 kHz, 16-bit stereo PCM WAV tracks for {@link NeonSonifier}.
 * <p>
 * Built around band-limited PolyBLEP sawtooth and PWM square oscillators, per-voice
 * zero-delay-feedback (ZDF) 2-pole resonant state-variable filters with envelope-modulated
 * cutoffs, a Juno-style LFO-modulated stereo BBD chorus, short asymmetric room reflections,
 * subtle kick-driven sidechain ducking on bass/pad buses, and soft-knee tape saturation.
 */
final class NeonSynth {

    static final int SAMPLE_RATE = 44_100;
    static final int CHANNELS = 2;
    static final int TAIL_SAMPLES = (int) (SAMPLE_RATE * 1.25); // 1.25s stereo chorus & finale tail

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final int MAX_ACTIVE_VOICES = 32;
    private static final int IO_BUFFER_BYTES = 16_384;
    private static final int FILTER_CONTROL_STRIDE = 16;

    // Juno-style stereo BBD chorus parameters
    private static final int CHORUS_BUFFER_SIZE = 2_048;
    private static final int CHORUS_BUFFER_MASK = CHORUS_BUFFER_SIZE - 1;
    private static final double CHORUS_CENTER_DELAY = 660.0; // ~15.0 ms
    private static final double CHORUS_DEPTH = 145.0;        // ~3.3 ms modulation swing
    private static final double CHORUS_LFO_INC = TWO_PI * 0.58 / SAMPLE_RATE;
    private static final double CHORUS_WET_MIX = 0.28;

    // Crisp asymmetric dark-room early reflections (prime sample lengths: ~37.1 ms L, ~53.0 ms R)
    private static final int ROOM_LEFT_SAMPLES = 1_637;
    private static final int ROOM_RIGHT_SAMPLES = 2_339;
    private static final double ROOM_FEEDBACK = 0.19;
    private static final double ROOM_DAMPING = 0.64;
    private static final double ROOM_WET_MIX = 0.14;

    // Kick-driven sidechain envelope recovery (~135 ms time constant)
    private static final double SIDECHAIN_DUCK_FLOOR = 0.56;
    private static final double SIDECHAIN_RECOVERY = Math.exp(-1.0 / (SAMPLE_RATE * 0.135));

    private static final double MASTER_DRIVE = 1.38;
    private static final double PCM_HEADROOM = 31_600.0;

    private NeonSynth() {
    }

    /**
     * Distinct subtractive analog and cyber-telemetry voice models.
     */
    enum VoiceKind {
        /** Driving PolyBLEP sawtooth + sub-octave sine through resonant LP envelope (sidechain bus). */
        ANALOG_BASS,
        /** Lush 3-oscillator detuned PolyBLEP supersaw pad with slow warm filter bloom (sidechain bus). */
        SUPERSAW_PAD,
        /** Snappy PWM square + saw hybrid through fast resonant LP pluck for branch traversals. */
        ARP_PLUCK,
        /** Pitch-snapped bright saw chirp for partial leaf executions and active trajectories. */
        LASER_LEAD,
        /** Detuned octave supersaw chime for newly completed leaves and celebratory arpeggios. */
        NEON_SHIMMER,
        /** Electric-cyan glassy harmonic bell for NOTICE inflections. */
        CYAN_BELL,
        /** Tight high-passed metallic noise tick for Monte-Carlo over-executions &amp; hot branches. */
        CYBER_HAT,
        /** Punchy 808-style pitch-drop kick marking test cycle boundaries (drives sidechain bus). */
        ANALOG_KICK,
        /** Detuned dual-sawtooth mid-band brass stab for WARNING alerts. */
        AMBER_BRASS,
        /** Ring-modulated tritone + bitcrushed static + sub-drop for raw FAILED regressions. */
        GLITCH_FAULT,
        /** Resonant band-pass frequency glide for post-test column-bank compaction lock-in. */
        LOCK_SWEEP,
        /** Warm low-register downward pitch &amp; low-pass fade-out for hidden or pruned leaves. */
        FADE_DISSOLVE
    }

    /**
     * Immutable specification of a scheduled stereo analog synthesis event.
     *
     * @param startSample     exact sample frame at which the voice begins
     * @param durationSamples voice duration in sample frames
     * @param kind            analog voice model
     * @param midiPitch       starting MIDI pitch (69.0 = A4 = 440 Hz)
     * @param endMidiPitch    ending MIDI pitch (used by gliding voices such as {@link VoiceKind#LOCK_SWEEP} and {@link VoiceKind#FADE_DISSOLVE})
     * @param amplitude       peak linear voice gain prior to constant-power stereo panning
     * @param brightness      filter cutoff energy / bite in {@code [0.0, 1.0]}
     * @param pan             stereo panorama in {@code [-1.0, +1.0]}
     */
    record NeonEvent(
            long startSample,
            int durationSamples,
            VoiceKind kind,
            double midiPitch,
            double endMidiPitch,
            double amplitude,
            double brightness,
            double pan) {

        NeonEvent(
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
     * Renders scheduled stereo synth events into a deterministic 16-bit stereo PCM WAV file.
     *
     * @param output          destination WAV file
     * @param totalSamples    total stereo sample frames (including chorus/reverb tail)
     * @param samplesPerFrame sample frames per video frame
     * @param eventsByFrame   events keyed by video frame index
     * @throws IOException on write failure or WAV size limit overflow
     */
    static void writeStereoWav(
            final File output,
            final long totalSamples,
            final int samplesPerFrame,
            final Map<Integer, List<NeonEvent>> eventsByFrame) throws IOException {

        final long dataBytes = totalSamples * CHANNELS * Short.BYTES;
        if (dataBytes > Integer.MAX_VALUE - 36L) {
            throw new IOException("Progression neon audio exceeds the WAV file size limit");
        }

        final List<ActiveVoice> activeVoices = new ArrayList<>(MAX_ACTIVE_VOICES);
        final double[] chorusBufL = new double[CHORUS_BUFFER_SIZE];
        final double[] chorusBufR = new double[CHORUS_BUFFER_SIZE];
        int chorusWriteIdx = 0;
        double chorusLfoPhase = 0.0;

        final double[] roomBufL = new double[ROOM_LEFT_SAMPLES];
        final double[] roomBufR = new double[ROOM_RIGHT_SAMPLES];
        int roomIdxL = 0;
        int roomIdxR = 0;
        double roomFilterL = 0.0;
        double roomFilterR = 0.0;

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
                    final List<NeonEvent> incoming = eventsByFrame.get(frameIndex);
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

                for (int i = activeVoices.size() - 1; i >= 0; i--) {
                    final ActiveVoice v = activeVoices.get(i);
                    if (sample < v.startSample) {
                        continue;
                    }
                    if (v.age == 0 && v.kind == VoiceKind.ANALOG_KICK) {
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
                }

                // Smooth exponential recovery on kick-ducked bass/pad bus
                sidechainGain = 1.0 - (1.0 - sidechainGain) * SIDECHAIN_RECOVERY;
                final double dryL = busMainL + busDuckL * sidechainGain;
                final double dryR = busMainR + busDuckR * sidechainGain;

                // Juno-60 style LFO-modulated stereo BBD chorus
                chorusBufL[chorusWriteIdx] = chorusSendL;
                chorusBufR[chorusWriteIdx] = chorusSendR;
                final double lfo = Math.sin(chorusLfoPhase);
                chorusLfoPhase += CHORUS_LFO_INC;
                if (chorusLfoPhase >= TWO_PI) {
                    chorusLfoPhase -= TWO_PI;
                }
                final double wetChorusL = readInterpolatedDelay(chorusBufL, chorusWriteIdx, CHORUS_CENTER_DELAY + CHORUS_DEPTH * lfo);
                final double wetChorusR = readInterpolatedDelay(chorusBufR, chorusWriteIdx, CHORUS_CENTER_DELAY - CHORUS_DEPTH * lfo);
                chorusWriteIdx = (chorusWriteIdx + 1) & CHORUS_BUFFER_MASK;

                final double postChorusL = dryL + CHORUS_WET_MIX * wetChorusL;
                final double postChorusR = dryR + CHORUS_WET_MIX * wetChorusR;

                // Subtle asymmetric dark-room reflection tap
                final double tapL = roomBufL[roomIdxL];
                final double tapR = roomBufR[roomIdxR];
                roomFilterL = ROOM_DAMPING * roomFilterL + (1.0 - ROOM_DAMPING) * tapL;
                roomFilterR = ROOM_DAMPING * roomFilterR + (1.0 - ROOM_DAMPING) * tapR;

                roomBufL[roomIdxL] = postChorusL + ROOM_FEEDBACK * roomFilterR;
                roomBufR[roomIdxR] = postChorusR + ROOM_FEEDBACK * roomFilterL;
                if (++roomIdxL == ROOM_LEFT_SAMPLES) {
                    roomIdxL = 0;
                }
                if (++roomIdxR == ROOM_RIGHT_SAMPLES) {
                    roomIdxR = 0;
                }

                // Soft-knee tape saturation & 16-bit stereo PCM framing
                final double mixL = Math.tanh((postChorusL + ROOM_WET_MIX * roomFilterL) * MASTER_DRIVE);
                final double mixR = Math.tanh((postChorusR + ROOM_WET_MIX * roomFilterR) * MASTER_DRIVE);

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

    private static double readInterpolatedDelay(final double[] buffer, final int writeIdx, final double delaySamples) {
        final double readPos = writeIdx - delaySamples;
        final int i0 = ((int) Math.floor(readPos)) & CHORUS_BUFFER_MASK;
        final int i1 = (i0 + 1) & CHORUS_BUFFER_MASK;
        final double frac = readPos - Math.floor(readPos);
        return buffer[i0] + frac * (buffer[i1] - buffer[i0]);
    }

    private static void spawnVoice(
            final List<ActiveVoice> activeVoices,
            final NeonEvent event,
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
                final double priority = (v.kind == VoiceKind.GLITCH_FAULT || v.kind == VoiceKind.SUPERSAW_PAD || v.kind == VoiceKind.ANALOG_KICK)
                        ? 3.0 : 1.0;
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

    private static double polyBlepPulse(final double phase, final double dt, final double width) {
        double value = (phase < width) ? 1.0 : -1.0;
        value += polyBlep(phase, dt);
        double shifted = phase - width;
        if (shifted < 0.0) {
            shifted += 1.0;
        }
        value -= polyBlep(shifted, dt);
        return value;
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

        int age = 0;
        double phaseA = 0.0;
        double phaseB = 0.0;
        double phaseC = 0.0;
        double phaseSub = 0.0;

        // ZDF 2-pole State-Variable Filter state & cached coefficients
        double ic1eq = 0.0;
        double ic2eq = 0.0;
        double svfA1 = 0.0;
        double svfA2 = 0.0;
        double svfA3 = 0.0;
        double svfK = 1.0;

        // Deterministic xorshift32 noise & sample-and-hold state
        int rngState;
        double heldNoise = 0.0;

        double outL = 0.0;
        double outR = 0.0;

        ActiveVoice(final NeonEvent event, final long startSample, final int serial) {
            this.startSample = startSample;
            this.durationSamples = Math.max(1, event.durationSamples());
            this.kind = event.kind();
            this.baseFreqHz = midiToHz(event.midiPitch());
            this.endFreqHz = midiToHz(event.endMidiPitch());
            this.amplitude = event.amplitude();
            this.brightness = Math.max(0.0, Math.min(1.0, event.brightness()));

            final double clampedPan = Math.max(-1.0, Math.min(1.0, event.pan()));
            final double angle = (clampedPan + 1.0) * (Math.PI * 0.25);
            this.gainL = Math.cos(angle);
            this.gainR = Math.sin(angle);

            this.sidechained = (kind == VoiceKind.ANALOG_BASS || kind == VoiceKind.SUPERSAW_PAD);
            this.chorusSend = switch (kind) {
                case SUPERSAW_PAD -> 0.85;
                case NEON_SHIMMER -> 0.72;
                case ARP_PLUCK -> 0.42;
                case LASER_LEAD -> 0.48;
                case AMBER_BRASS -> 0.50;
                case LOCK_SWEEP -> 0.65;
                case FADE_DISSOLVE -> 0.55;
                case CYAN_BELL -> 0.35;
                default -> 0.0;
            };

            // Deterministic initial seed & tiny stereo phase offset for lush supersaws
            this.rngState = 0x5EED1984 ^ (serial * 0x45D9F3B) ^ (int) (startSample * 31L);
            if (this.rngState == 0) {
                this.rngState = 0x13579BDF;
            }
            if (kind == VoiceKind.SUPERSAW_PAD || kind == VoiceKind.NEON_SHIMMER) {
                this.phaseB = 0.31;
                this.phaseC = 0.67;
            }
        }

        private double nextNoise() {
            int x = rngState;
            x ^= (x << 13);
            x ^= (x >>> 17);
            x ^= (x << 5);
            rngState = x;
            return (x / (double) Integer.MAX_VALUE);
        }

        private void updateSvf(final double cutoffHz, final double q) {
            final double clampedHz = Math.max(40.0, Math.min(16_000.0, cutoffHz));
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

        void step() {
            final double t = (double) age / SAMPLE_RATE;
            final double progress = (double) age / durationSamples;
            final boolean updateFilter = (0 == (age & (FILTER_CONTROL_STRIDE - 1)));

            switch (kind) {
                case ANALOG_BASS -> stepAnalogBass(t, progress, updateFilter);
                case SUPERSAW_PAD -> stepSupersawPad(t, progress, updateFilter);
                case ARP_PLUCK -> stepArpPluck(t, progress, updateFilter);
                case LASER_LEAD -> stepLaserLead(t, progress, updateFilter);
                case NEON_SHIMMER -> stepNeonShimmer(t, progress, updateFilter);
                case CYAN_BELL -> stepCyanBell(t, progress);
                case CYBER_HAT -> stepCyberHat(t, progress, updateFilter);
                case ANALOG_KICK -> stepAnalogKick(t, progress);
                case AMBER_BRASS -> stepAmberBrass(t, progress, updateFilter);
                case GLITCH_FAULT -> stepGlitchFault(t, progress, updateFilter);
                case LOCK_SWEEP -> stepLockSweep(progress, updateFilter);
                case FADE_DISSOLVE -> stepFadeDissolve(t, progress, updateFilter);
            }
            age++;
        }

        private void stepAnalogBass(final double t, final double progress, final boolean updateFilter) {
            final double attack = Math.min(1.0, t / 0.004);
            final double release = (progress > 0.78) ? Math.max(0.0, (1.0 - progress) / 0.22) : 1.0;
            final double env = attack * Math.exp(-t / 0.24) * release;

            if (updateFilter) {
                final double fEnv = Math.exp(-t / 0.055);
                final double cutoff = baseFreqHz * (1.35 + (2.4 + 4.8 * brightness) * fEnv);
                updateSvf(cutoff, 2.25);
            }

            final double dt = baseFreqHz / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dt);
            phaseSub = wrapPhase(phaseSub + dt * 0.5);

            final double raw = 0.72 * polyBlepSaw(phaseA, dt) + 0.42 * Math.sin(TWO_PI * phaseSub);
            final double sample = stepSvfLow(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepSupersawPad(final double t, final double progress, final boolean updateFilter) {
            final double attack = 0.5 - 0.5 * Math.cos(Math.PI * Math.min(1.0, progress / 0.26));
            final double release = (progress > 0.52)
                    ? (0.5 + 0.5 * Math.cos(Math.PI * ((progress - 0.52) / 0.48)))
                    : 1.0;
            final double env = attack * release;

            if (updateFilter) {
                final double bloom = Math.sin(Math.PI * Math.min(1.0, progress * 1.05));
                final double cutoff = baseFreqHz * (1.4 + (1.8 + 4.2 * brightness) * bloom);
                updateSvf(cutoff, 1.15);
            }

            final double dtCenter = baseFreqHz / SAMPLE_RATE;
            final double dtFlat = dtCenter * 0.9948;  // -9 cents
            final double dtSharp = dtCenter * 1.0052; // +9 cents
            phaseA = wrapPhase(phaseA + dtCenter);
            phaseB = wrapPhase(phaseB + dtFlat);
            phaseC = wrapPhase(phaseC + dtSharp);

            final double sawA = polyBlepSaw(phaseA, dtCenter);
            final double sawB = polyBlepSaw(phaseB, dtFlat);
            final double sawC = polyBlepSaw(phaseC, dtSharp);

            final double mono = (sawA + 0.78 * sawB + 0.78 * sawC) / 2.56;
            final double filtered = stepSvfLow(mono) * env * amplitude;
            final double stereoSpread = 0.22 * (sawB - sawC) * env * amplitude * 0.45;
            outL = filtered * gainL + stereoSpread;
            outR = filtered * gainR - stereoSpread;
        }

        private void stepArpPluck(final double t, final double progress, final boolean updateFilter) {
            final double attack = Math.min(1.0, t / 0.0025);
            final double release = (progress > 0.80) ? Math.max(0.0, (1.0 - progress) / 0.20) : 1.0;
            final double env = attack * Math.exp(-t / 0.115) * release;

            if (updateFilter) {
                final double fEnv = Math.exp(-t / 0.042);
                final double cutoff = baseFreqHz * (1.25 + (3.2 + 6.5 * brightness) * fEnv);
                updateSvf(cutoff, 2.75);
            }

            final double dt = baseFreqHz / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dt);
            phaseB = wrapPhase(phaseB + dt * 1.0022);
            final double pw = 0.34 + 0.12 * Math.sin(TWO_PI * 3.8 * t);
            final double raw = 0.64 * polyBlepPulse(phaseA, dt, pw) + 0.36 * polyBlepSaw(phaseB, dt * 1.0022);
            final double sample = stepSvfLow(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepLaserLead(final double t, final double progress, final boolean updateFilter) {
            final double attack = Math.min(1.0, t / 0.002);
            final double release = (progress > 0.76) ? Math.max(0.0, (1.0 - progress) / 0.24) : 1.0;
            final double env = attack * Math.exp(-t / 0.145) * release;

            // Quick upward laser snap on the first 18ms
            final double pitchSnap = 1.0 - 0.18 * Math.exp(-t / 0.014);
            final double freq = baseFreqHz * pitchSnap;
            if (updateFilter) {
                final double fEnv = Math.exp(-t / 0.065);
                final double cutoff = freq * (1.8 + (4.5 + 6.0 * brightness) * fEnv);
                updateSvf(cutoff, 3.15);
            }

            final double dt = freq / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dt);
            phaseB = wrapPhase(phaseB + dt * 1.501); // resonant fifth harmonic
            final double raw = 0.75 * polyBlepSaw(phaseA, dt) + 0.25 * polyBlepPulse(phaseB, dt * 1.501, 0.28);
            final double sample = stepSvfLow(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepNeonShimmer(final double t, final double progress, final boolean updateFilter) {
            final double attack = Math.min(1.0, t / 0.005);
            final double release = (progress > 0.68) ? Math.max(0.0, (1.0 - progress) / 0.32) : 1.0;
            final double env = attack * Math.exp(-t / 0.34) * release;

            if (updateFilter) {
                final double fEnv = Math.exp(-t / 0.16);
                final double cutoff = baseFreqHz * (2.2 + (4.0 + 5.5 * brightness) * fEnv);
                updateSvf(cutoff, 2.0);
            }

            final double dt = baseFreqHz / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dt * 0.9965);
            phaseB = wrapPhase(phaseB + dt * 1.0035);
            phaseC = wrapPhase(phaseC + dt * 2.0); // luminous octave shimmer
            final double raw = 0.42 * polyBlepSaw(phaseA, dt * 0.9965)
                    + 0.42 * polyBlepSaw(phaseB, dt * 1.0035)
                    + 0.30 * Math.sin(TWO_PI * phaseC);
            final double sample = stepSvfLow(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepCyanBell(final double t, final double progress) {
            final double attack = Math.min(1.0, t / 0.002);
            final double release = (progress > 0.75) ? Math.max(0.0, (1.0 - progress) / 0.25) : 1.0;
            final double env = attack * Math.exp(-t / 0.24) * release;
            final double sparkleEnv = Math.exp(-t / 0.075);

            phaseA += TWO_PI * baseFreqHz / SAMPLE_RATE;
            phaseB += TWO_PI * (baseFreqHz * 2.756) / SAMPLE_RATE;
            phaseC += TWO_PI * (baseFreqHz * 4.0) / SAMPLE_RATE;

            final double bell = 0.64 * Math.sin(phaseA + 0.45 * sparkleEnv * Math.sin(phaseB))
                    + 0.24 * sparkleEnv * Math.sin(phaseB)
                    + 0.18 * sparkleEnv * Math.sin(phaseC);
            final double sample = bell * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepCyberHat(final double t, final double progress, final boolean updateFilter) {
            final double attack = Math.min(1.0, t / 0.0012);
            final double release = (progress > 0.75) ? Math.max(0.0, (1.0 - progress) / 0.25) : 1.0;
            final double env = attack * Math.exp(-t / (0.024 + 0.022 * brightness)) * release;

            if (updateFilter) {
                updateSvf(5_200.0 + 3_800.0 * brightness, 1.85);
            }

            phaseA = wrapPhase(phaseA + 842.0 / SAMPLE_RATE);
            phaseB = wrapPhase(phaseB + 1_237.0 / SAMPLE_RATE);
            final double metallic = ((phaseA < 0.5 ? 1.0 : -1.0) * (phaseB < 0.5 ? 1.0 : -1.0));
            final double raw = 0.65 * nextNoise() + 0.35 * metallic;
            final double sample = stepSvfHigh(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepAnalogKick(final double t, final double progress) {
            final double attack = Math.min(1.0, t / 0.0015);
            final double release = (progress > 0.80) ? Math.max(0.0, (1.0 - progress) / 0.20) : 1.0;
            final double env = attack * Math.exp(-t / 0.11) * release;
            final double freq = Math.max(38.0, baseFreqHz) * (1.0 + 2.35 * Math.exp(-t / 0.022));
            phaseA += TWO_PI * freq / SAMPLE_RATE;
            final double click = Math.exp(-t / 0.0035) * nextNoise() * 0.24;
            final double body = Math.sin(phaseA);
            final double sample = Math.tanh((body * 1.25 + click) * 1.15) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepAmberBrass(final double t, final double progress, final boolean updateFilter) {
            final double attack = Math.min(1.0, t / 0.012);
            final double release = (progress > 0.70) ? Math.max(0.0, (1.0 - progress) / 0.30) : 1.0;
            final double env = attack * Math.exp(-t / 0.28) * release;

            if (updateFilter) {
                final double fEnv = Math.exp(-t / 0.095);
                final double cutoff = baseFreqHz * (1.2 + (2.0 + 4.8 * brightness) * fEnv);
                updateSvf(cutoff, 2.4);
            }

            final double dtFlat = (baseFreqHz / SAMPLE_RATE) * 0.9960;
            final double dtSharp = (baseFreqHz / SAMPLE_RATE) * 1.0040;
            phaseA = wrapPhase(phaseA + dtFlat);
            phaseB = wrapPhase(phaseB + dtSharp);
            final double raw = 0.52 * polyBlepSaw(phaseA, dtFlat) + 0.52 * polyBlepSaw(phaseB, dtSharp);
            final double sample = stepSvfLow(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepGlitchFault(final double t, final double progress, final boolean updateFilter) {
            final double attack = Math.min(1.0, t / 0.002);
            final double release = (progress > 0.72) ? Math.max(0.0, (1.0 - progress) / 0.28) : 1.0;
            final double env = attack * Math.exp(-t / 0.26) * release;

            // Known-issue failures pass brightness < 0.35 for a contained low-pass pulse
            final boolean rawFailure = brightness >= 0.35;
            if (updateFilter) {
                final double cutoff = rawFailure
                        ? (2_600.0 * Math.exp(-t / 0.15) + 420.0)
                        : (380.0 * Math.exp(-t / 0.12) + 180.0);
                updateSvf(cutoff, rawFailure ? 3.0 : 1.3);
            }

            final double subFreq = Math.max(36.0, baseFreqHz * 0.5 * Math.pow(0.56, progress));
            phaseSub += TWO_PI * subFreq / SAMPLE_RATE;

            if (!rawFailure) {
                final double dt = baseFreqHz / SAMPLE_RATE;
                phaseA = wrapPhase(phaseA + dt);
                final double contained = 0.55 * stepSvfLow(polyBlepSaw(phaseA, dt)) + 0.45 * Math.sin(phaseSub);
                final double sample = contained * env * amplitude;
                outL = sample * gainL;
                outR = sample * gainR;
                return;
            }

            // Raw failure: ring-modulated tritone pair + 60ms sample-and-hold bitcrushed static + sub drop
            final double dtA = baseFreqHz / SAMPLE_RATE;
            final double dtB = (baseFreqHz * 1.41421356) / SAMPLE_RATE; // tritone ring-mod
            phaseA = wrapPhase(phaseA + dtA);
            phaseB = wrapPhase(phaseB + dtB);
            final double ringMod = polyBlepPulse(phaseA, dtA, 0.5) * polyBlepPulse(phaseB, dtB, 0.35);

            if (0 == (age % 22)) {
                heldNoise = Math.round(nextNoise() * 3.0) / 3.0; // coarse 2-bit sample-and-hold glitch
            }
            final double glitchBurst = heldNoise * Math.exp(-t / 0.055);
            final double filtered = stepSvfLow(0.58 * ringMod + 0.48 * glitchBurst);
            final double sample = Math.tanh((filtered + 0.52 * Math.sin(phaseSub)) * 1.25) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepLockSweep(final double progress, final boolean updateFilter) {
            final double env = Math.sin(Math.PI * progress);
            final double freq = baseFreqHz * Math.pow(endFreqHz / Math.max(1.0, baseFreqHz), progress);

            if (updateFilter) {
                updateSvf(freq * (1.25 + 0.75 * brightness), 3.2);
            }

            final double dt = freq / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dt * 0.996);
            phaseB = wrapPhase(phaseB + dt * 1.004);
            final double raw = 0.42 * polyBlepSaw(phaseA, dt * 0.996)
                    + 0.42 * polyBlepSaw(phaseB, dt * 1.004)
                    + 0.24 * nextNoise();
            final double sample = stepSvfBand(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepFadeDissolve(final double t, final double progress, final boolean updateFilter) {
            final double attack = Math.min(1.0, t / 0.022);
            final double decay = Math.pow(Math.max(0.0, 1.0 - progress), 1.85);
            final double env = attack * decay;
            final double freq = baseFreqHz * Math.pow(endFreqHz / Math.max(1.0, baseFreqHz), progress);

            if (updateFilter) {
                final double closing = Math.pow(Math.max(0.0, 1.0 - progress), 1.35);
                final double cutoff = freq * (1.05 + (1.55 + 1.85 * brightness) * closing);
                updateSvf(cutoff, 1.35);
            }

            final double dt = freq / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dt * 0.9965);
            phaseB = wrapPhase(phaseB + dt * 1.0035);
            phaseSub += TWO_PI * dt;
            final double raw = 0.34 * polyBlepSaw(phaseA, dt * 0.9965)
                    + 0.34 * polyBlepSaw(phaseB, dt * 1.0035)
                    + 0.42 * Math.sin(phaseSub);
            final double sample = stepSvfLow(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private static double wrapPhase(final double phase) {
            return (phase >= 1.0) ? (phase - Math.floor(phase)) : phase;
        }
    }

    private static void writeWavHeader(final OutputStream out, final long dataBytes) throws IOException {
        final byte[] header = new byte[44];
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        putLittleEndianInt(header, 4, (int) (dataBytes + 36L));
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        putLittleEndianInt(header, 16, 16);
        putLittleEndianShort(header, 20, 1); // PCM
        putLittleEndianShort(header, 22, CHANNELS);
        putLittleEndianInt(header, 24, SAMPLE_RATE);
        putLittleEndianInt(header, 28, SAMPLE_RATE * CHANNELS * Short.BYTES);
        putLittleEndianShort(header, 32, CHANNELS * Short.BYTES);
        putLittleEndianShort(header, 34, 16);
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        putLittleEndianInt(header, 40, (int) dataBytes);
        out.write(header);
    }

    private static void putLittleEndianInt(final byte[] buf, final int offset, final int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        buf[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        buf[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }

    private static void putLittleEndianShort(final byte[] buf, final int offset, final int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >>> 8) & 0xFF);
    }
}
