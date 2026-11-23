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
 * Deterministic 1970s French cosmic analog synthesizer rendering 44.1 kHz, 16-bit true-stereo
 * PCM WAV tracks for {@link JmjSonifier}, inspired by Jean-Michel Jarre's early classics
 * (<i>Oxygène</i>, <i>Équinoxe</i>, <i>Les Chants Magnétiques</i>).
 * <p>
 * Built around an Eminent 310-style divide-down string ensemble routed through a stereo
 * 4-stage Electro-Harmonix Small Stone all-pass phaser, an ARP/Matrisequencer resonant analog
 * sequencer bass and singing vibrato lead, EMS VCS3 resonant cosmic droplets, pink-noise space
 * wind, Korg MiniPops-7 vintage analog percussion, and a Roland RE-201 style stereo ping-pong
 * Space Echo tape delay with warm high-frequency damping and subtle tape wow/flutter.
 */
final class JmjSynth {

    static final int SAMPLE_RATE = 44_100;
    static final int CHANNELS = 2;
    static final int TAIL_SAMPLES = (int) (SAMPLE_RATE * 1.65); // 1.65s Space Echo & phased string tail

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final int MAX_ACTIVE_VOICES = 34;
    private static final int IO_BUFFER_BYTES = 16_384;
    private static final int FILTER_CONTROL_STRIDE = 16;

    // Electro-Harmonix Small Stone inspired 4-stage stereo all-pass phaser
    private static final double PHASER_LFO_INC = TWO_PI * 0.21 / SAMPLE_RATE; // ~4.76s hypnotic sweep
    private static final double PHASER_MIN_HZ = 230.0;
    private static final double PHASER_MAX_HZ = 2_450.0;
    private static final double PHASER_FEEDBACK = 0.42;
    private static final double PHASER_WET_MIX = 0.62;

    // Roland RE-201 Space Echo inspired asymmetric stereo ping-pong tape delay
    private static final int ECHO_BUFFER_SIZE = 32_768;
    private static final int ECHO_BUFFER_MASK = ECHO_BUFFER_SIZE - 1;
    private static final double ECHO_DELAY_L = 11_686.0; // ~265 ms (dotted-eighth feel)
    private static final double ECHO_DELAY_R = 17_551.0; // ~398 ms (quarter-note feel)
    private static final double ECHO_WOW_DEPTH = 5.5;    // subtle vintage tape wow
    private static final double ECHO_WOW_INC = TWO_PI * 0.73 / SAMPLE_RATE;
    private static final double ECHO_FEEDBACK = 0.37;
    private static final double ECHO_TAPE_DAMP = 0.54;   // warm HF roll-off on each tape repeat
    private static final double ECHO_WET_MIX = 0.34;

    private static final double MASTER_DRIVE = 1.24;
    private static final double PCM_HEADROOM = 31_400.0;

    private JmjSynth() {
    }

    /**
     * Iconic 1976–1981 French analog cosmic synthesis voice models.
     */
    enum VoiceKind {
        /** Eminent 310 Unique divide-down 8'+4' string ensemble fed through the Small Stone phaser. */
        EMINENT_STRINGS,
        /** Hypnotic Matrisequencer / ARP analog bass ostinato with snappy resonant LP envelope. */
        SEQ_BASS,
        /** Crisp ARP 2600 step-sequencer pluck for branch traversals across the stereo stage. */
        ARP_SEQ_NOTE,
        /** Expressive singing Oxygène-style analog lead with delayed vibrato &amp; Space Echo tail. */
        OXYGENE_LEAD,
        /** EMS VCS3 resonant cosmic water-drop / sonar bubble echoing into stereo tape delay. */
        VCS3_DROP,
        /** RMI Harmonic Synthesizer airy glass chime for NOTICE markers. */
        GLASS_HARMONIC,
        /** Vintage Korg MiniPops-7 woody analog bass drum marking cycle boundaries. */
        MINIPOPS_KICK,
        /** Vintage Korg MiniPops-7 sandy maraca / brush hi-hat tick for groove &amp; hot nodes. */
        MINIPOPS_TICK,
        /** Solemn detuned analog horn swell for WARNING cues. */
        ANALOG_HORN,
        /** Vintage 1970s EMS VCS3 ring-modulated dissonant alarm + sub rumble for raw FAILED nodes. */
        VCS3_RING_ALARM,
        /** Resonant filtered pink-noise cosmic wind sweep through the Small Stone phaser. */
        WIND_SWELL,
        /** Warm low-register downward pitch &amp; low-pass fade-out for disappearing/pruned leaves. */
        COSMIC_FADE
    }

    /**
     * Immutable specification of a scheduled stereo cosmic analog event.
     *
     * @param startSample     exact sample frame at which the voice begins
     * @param durationSamples voice duration in sample frames
     * @param kind            analog voice model
     * @param midiPitch       starting MIDI pitch (69.0 = A4 = 440 Hz)
     * @param endMidiPitch    ending MIDI pitch (used by gliding voices such as {@link VoiceKind#OXYGENE_LEAD},
     *                        {@link VoiceKind#VCS3_DROP}, {@link VoiceKind#WIND_SWELL}, {@link VoiceKind#COSMIC_FADE})
     * @param amplitude       peak linear voice gain prior to constant-power stereo panning
     * @param brightness      filter cutoff energy / warmth in {@code [0.0, 1.0]}
     * @param pan             stereo panorama in {@code [-1.0, +1.0]}
     */
    record JmjEvent(
            long startSample,
            int durationSamples,
            VoiceKind kind,
            double midiPitch,
            double endMidiPitch,
            double amplitude,
            double brightness,
            double pan) {

        JmjEvent(
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
     * Renders scheduled JMJ cosmic analog events into a deterministic 16-bit stereo PCM WAV file.
     *
     * @param output          destination WAV file
     * @param totalSamples    total stereo sample frames (including Space Echo &amp; phaser tail)
     * @param samplesPerFrame sample frames per video frame
     * @param eventsByFrame   events keyed by video frame index
     * @throws IOException on write failure or WAV size limit overflow
     */
    static void writeStereoWav(
            final File output,
            final long totalSamples,
            final int samplesPerFrame,
            final Map<Integer, List<JmjEvent>> eventsByFrame) throws IOException {

        final long dataBytes = totalSamples * CHANNELS * Short.BYTES;
        if (dataBytes > Integer.MAX_VALUE - 36L) {
            throw new IOException("Progression JMJ audio exceeds the WAV file size limit");
        }

        final List<ActiveVoice> activeVoices = new ArrayList<>(MAX_ACTIVE_VOICES);

        // Stereo 4-stage Small Stone phaser state
        final double[] apStateL = new double[4];
        final double[] apStateR = new double[4];
        double phaserFbL = 0.0;
        double phaserFbR = 0.0;
        double phaserLfoPhase = 0.0;
        double phaserCoeffL = 0.0;
        double phaserCoeffR = 0.0;

        // Stereo Space Echo tape delay state
        final double[] echoBufL = new double[ECHO_BUFFER_SIZE];
        final double[] echoBufR = new double[ECHO_BUFFER_SIZE];
        int echoWriteIdx = 0;
        double echoWowPhase = 0.0;
        double echoLpL = 0.0;
        double echoLpR = 0.0;

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
                    final List<JmjEvent> incoming = eventsByFrame.get(frameIndex);
                    if (null != incoming) {
                        for (int i = 0; i < incoming.size(); i++) {
                            spawnVoice(activeVoices, incoming.get(i), sample, voiceSerial++);
                        }
                    }
                }

                double dryL = 0.0;
                double dryR = 0.0;
                double phaserInL = 0.0;
                double phaserInR = 0.0;
                double echoSendL = 0.0;
                double echoSendR = 0.0;

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
                    if (v.phaserSend > 0.0) {
                        phaserInL += v.outL * v.phaserSend;
                        phaserInR += v.outR * v.phaserSend;
                        dryL += v.outL * (1.0 - v.phaserSend);
                        dryR += v.outR * (1.0 - v.phaserSend);
                    } else {
                        dryL += v.outL;
                        dryR += v.outR;
                    }
                    if (v.echoSend > 0.0) {
                        echoSendL += v.outL * v.echoSend;
                        echoSendR += v.outR * v.echoSend;
                    }
                }

                // Update Small Stone 4-stage stereo all-pass phaser coefficients at control rate
                if (0 == (sample & (FILTER_CONTROL_STRIDE - 1))) {
                    final double lfoL = 0.5 + 0.5 * Math.sin(phaserLfoPhase);
                    final double lfoR = 0.5 + 0.5 * Math.sin(phaserLfoPhase + 2.0943951); // 120 deg stereo phase offset
                    final double hzL = PHASER_MIN_HZ * Math.pow(PHASER_MAX_HZ / PHASER_MIN_HZ, lfoL);
                    final double hzR = PHASER_MIN_HZ * Math.pow(PHASER_MAX_HZ / PHASER_MIN_HZ, lfoR);
                    phaserCoeffL = computeAllPassCoeff(hzL);
                    phaserCoeffR = computeAllPassCoeff(hzR);
                }
                phaserLfoPhase += PHASER_LFO_INC;
                if (phaserLfoPhase >= TWO_PI) {
                    phaserLfoPhase -= TWO_PI;
                }

                // Step 4-stage all-pass cascade for L and R
                final double phL = stepAllPass4(phaserInL + phaserFbL * PHASER_FEEDBACK, phaserCoeffL, apStateL);
                final double phR = stepAllPass4(phaserInR + phaserFbR * PHASER_FEEDBACK, phaserCoeffR, apStateR);
                phaserFbL = Math.tanh(phL);
                phaserFbR = Math.tanh(phR);

                final double phasedL = phaserInL * (1.0 - 0.32 * PHASER_WET_MIX) + phL * PHASER_WET_MIX;
                final double phasedR = phaserInR * (1.0 - 0.32 * PHASER_WET_MIX) + phR * PHASER_WET_MIX;

                // Roland RE-201 Space Echo asymmetric ping-pong tape delay with subtle wow
                final double wow = Math.sin(echoWowPhase) * ECHO_WOW_DEPTH;
                echoWowPhase += ECHO_WOW_INC;
                if (echoWowPhase >= TWO_PI) {
                    echoWowPhase -= TWO_PI;
                }
                final double tapL = readInterpolatedDelay(echoBufL, echoWriteIdx, ECHO_DELAY_L + wow);
                final double tapR = readInterpolatedDelay(echoBufR, echoWriteIdx, ECHO_DELAY_R - wow * 0.8);
                echoLpL = ECHO_TAPE_DAMP * echoLpL + (1.0 - ECHO_TAPE_DAMP) * tapL;
                echoLpR = ECHO_TAPE_DAMP * echoLpR + (1.0 - ECHO_TAPE_DAMP) * tapR;

                // Cross-coupled stereo ping-pong feedback with gentle tape saturation
                echoBufL[echoWriteIdx] = Math.tanh(echoSendL + echoLpR * ECHO_FEEDBACK);
                echoBufR[echoWriteIdx] = Math.tanh(echoSendR + echoLpL * ECHO_FEEDBACK);
                echoWriteIdx = (echoWriteIdx + 1) & ECHO_BUFFER_MASK;

                final double sumL = dryL + phasedL + ECHO_WET_MIX * echoLpL;
                final double sumR = dryR + phasedR + ECHO_WET_MIX * echoLpR;

                final double mixL = Math.tanh(sumL * MASTER_DRIVE);
                final double mixR = Math.tanh(sumR * MASTER_DRIVE);

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

    private static double computeAllPassCoeff(final double freqHz) {
        final double t = Math.tan(Math.PI * freqHz / SAMPLE_RATE);
        return (t - 1.0) / (t + 1.0);
    }

    private static double stepAllPass4(final double input, final double coeff, final double[] state) {
        double x = input;
        for (int s = 0; s < 4; s++) {
            final double y = coeff * x + state[s];
            state[s] = x - coeff * y;
            x = y;
        }
        return x;
    }

    private static double readInterpolatedDelay(final double[] buffer, final int writeIdx, final double delaySamples) {
        final double readPos = writeIdx - delaySamples;
        final int i0 = ((int) Math.floor(readPos)) & ECHO_BUFFER_MASK;
        final int i1 = (i0 + 1) & ECHO_BUFFER_MASK;
        final double frac = readPos - Math.floor(readPos);
        return buffer[i0] + frac * (buffer[i1] - buffer[i0]);
    }

    private static void spawnVoice(
            final List<ActiveVoice> activeVoices,
            final JmjEvent event,
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
                final double priority = (v.kind == VoiceKind.VCS3_RING_ALARM
                        || v.kind == VoiceKind.EMINENT_STRINGS
                        || v.kind == VoiceKind.OXYGENE_LEAD) ? 3.0 : 1.0;
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
        final double phaserSend;
        final double echoSend;

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

        // Deterministic xorshift32 noise & Paul Kellet 3-pole pink-noise filter state
        int rngState;
        double pinkB0 = 0.0;
        double pinkB1 = 0.0;
        double pinkB2 = 0.0;

        double outL = 0.0;
        double outR = 0.0;

        ActiveVoice(final JmjEvent event, final long startSample, final int serial) {
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

            this.phaserSend = switch (kind) {
                case EMINENT_STRINGS -> 1.0;
                case WIND_SWELL -> 0.92;
                case COSMIC_FADE -> 0.55;
                case ANALOG_HORN -> 0.35;
                default -> 0.0;
            };
            this.echoSend = switch (kind) {
                case OXYGENE_LEAD -> 0.78;
                case VCS3_DROP -> 0.85;
                case GLASS_HARMONIC -> 0.65;
                case ARP_SEQ_NOTE -> 0.38;
                case VCS3_RING_ALARM -> 0.55;
                case ANALOG_HORN -> 0.30;
                default -> 0.0;
            };

            this.rngState = 0x19761978 ^ (serial * 0x45D9F3B) ^ (int) (startSample * 31L);
            if (this.rngState == 0) {
                this.rngState = 0x2468ACE1;
            }
            if (kind == VoiceKind.EMINENT_STRINGS) {
                this.phaseB = 0.27;
                this.phaseC = 0.63;
            }
        }

        private double nextWhiteNoise() {
            int x = rngState;
            x ^= (x << 13);
            x ^= (x >>> 17);
            x ^= (x << 5);
            rngState = x;
            return (x / (double) Integer.MAX_VALUE);
        }

        private double nextPinkNoise() {
            final double white = nextWhiteNoise();
            pinkB0 = 0.99765 * pinkB0 + white * 0.0990460;
            pinkB1 = 0.96300 * pinkB1 + white * 0.2965164;
            pinkB2 = 0.57000 * pinkB2 + white * 1.0526913;
            return (pinkB0 + pinkB1 + pinkB2 + white * 0.1848) * 0.22;
        }

        private void updateSvf(final double cutoffHz, final double q) {
            final double clampedHz = Math.max(45.0, Math.min(15_000.0, cutoffHz));
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
                case EMINENT_STRINGS -> stepEminentStrings(t, progress, updateFilter);
                case SEQ_BASS -> stepSeqBass(t, progress, updateFilter);
                case ARP_SEQ_NOTE -> stepArpSeqNote(t, progress, updateFilter);
                case OXYGENE_LEAD -> stepOxygeneLead(t, progress, updateFilter);
                case VCS3_DROP -> stepVcs3Drop(t, progress);
                case GLASS_HARMONIC -> stepGlassHarmonic(t, progress);
                case MINIPOPS_KICK -> stepMinipopsKick(t, progress);
                case MINIPOPS_TICK -> stepMinipopsTick(t, progress, updateFilter);
                case ANALOG_HORN -> stepAnalogHorn(t, progress, updateFilter);
                case VCS3_RING_ALARM -> stepVcs3RingAlarm(t, progress, updateFilter);
                case WIND_SWELL -> stepWindSwell(progress, updateFilter);
                case COSMIC_FADE -> stepCosmicFade(t, progress, updateFilter);
            }
            age++;
        }

        private void stepEminentStrings(final double t, final double progress, final boolean updateFilter) {
            // Lush Eminent 310 string machine envelope: gentle cosine swell & graceful release
            final double attack = 0.5 - 0.5 * Math.cos(Math.PI * Math.min(1.0, progress / 0.28));
            final double release = (progress > 0.55)
                    ? (0.5 + 0.5 * Math.cos(Math.PI * ((progress - 0.55) / 0.45)))
                    : 1.0;
            final double env = attack * release;

            if (updateFilter) {
                final double cutoff = baseFreqHz * (2.6 + 3.4 * brightness);
                updateSvf(cutoff, 0.92);
            }

            // Triple-chorus ensemble pitch wobble (classic Solina/Eminent 0.6 Hz + 5.8 Hz shimmer)
            final double ensMod = 1.0 + 0.0022 * Math.sin(TWO_PI * 0.62 * t) + 0.0014 * Math.sin(TWO_PI * 5.7 * t);
            final double dt8 = (baseFreqHz * ensMod) / SAMPLE_RATE;
            final double dt8Detune = (baseFreqHz / ensMod) * 1.0038 / SAMPLE_RATE;
            final double dt4 = (baseFreqHz * 2.0 * (2.0 - ensMod)) / SAMPLE_RATE; // 4' upper octave choir

            phaseA = wrapPhase(phaseA + dt8);
            phaseB = wrapPhase(phaseB + dt8Detune);
            phaseC = wrapPhase(phaseC + dt4);

            // Airy divide-down string waveforms (narrow pulse + sawtooth + 4' sheen)
            final double raw = 0.44 * polyBlepSaw(phaseA, dt8)
                    + 0.34 * polyBlepPulse(phaseB, dt8Detune, 0.26)
                    + 0.28 * polyBlepSaw(phaseC, dt4);
            final double filtered = stepSvfLow(raw) * env * amplitude;
            outL = filtered * gainL;
            outR = filtered * gainR;
        }

        private void stepSeqBass(final double t, final double progress, final boolean updateFilter) {
            // Crisp Matrisequencer / ARP analog bass pulse
            final double attack = Math.min(1.0, t / 0.003);
            final double release = (progress > 0.74) ? Math.max(0.0, (1.0 - progress) / 0.26) : 1.0;
            final double env = attack * Math.exp(-t / 0.16) * release;

            if (updateFilter) {
                final double fEnv = Math.exp(-t / 0.048);
                final double cutoff = baseFreqHz * (1.25 + (2.6 + 5.2 * brightness) * fEnv);
                updateSvf(cutoff, 2.55);
            }

            final double dt = baseFreqHz / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dt);
            phaseB = wrapPhase(phaseB + dt * 0.5);
            final double raw = 0.68 * polyBlepSaw(phaseA, dt)
                    + 0.24 * polyBlepPulse(phaseA, dt, 0.38)
                    + 0.26 * Math.sin(TWO_PI * phaseB);
            final double sample = stepSvfLow(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepArpSeqNote(final double t, final double progress, final boolean updateFilter) {
            final double attack = Math.min(1.0, t / 0.0025);
            final double release = (progress > 0.78) ? Math.max(0.0, (1.0 - progress) / 0.22) : 1.0;
            final double env = attack * Math.exp(-t / 0.105) * release;

            if (updateFilter) {
                final double fEnv = Math.exp(-t / 0.038);
                final double cutoff = baseFreqHz * (1.4 + (3.4 + 5.6 * brightness) * fEnv);
                updateSvf(cutoff, 2.85);
            }

            final double dt = baseFreqHz / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dt);
            final double raw = 0.56 * polyBlepPulse(phaseA, dt, 0.30) + 0.44 * polyBlepSaw(phaseA, dt);
            final double sample = stepSvfLow(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepOxygeneLead(final double t, final double progress, final boolean updateFilter) {
            // Singing monophonic ARP 2600 / Oxygène lead with gentle delayed vibrato & portamento
            final double attack = Math.min(1.0, t / 0.022);
            final double release = (progress > 0.66)
                    ? (0.5 + 0.5 * Math.cos(Math.PI * ((progress - 0.66) / 0.34)))
                    : 1.0;
            final double env = attack * Math.exp(-t / 0.52) * release;

            final double glide = Math.min(1.0, t / 0.045);
            final double base = baseFreqHz * Math.pow(endFreqHz / Math.max(1.0, baseFreqHz), glide);
            final double vibDepth = Math.min(1.0, t / 0.14) * 0.0068; // expressive delayed vibrato
            final double freq = base * (1.0 + vibDepth * Math.sin(TWO_PI * 5.35 * t));

            if (updateFilter) {
                final double fEnv = 0.35 + 0.65 * Math.exp(-t / 0.14);
                final double cutoff = freq * (1.6 + (2.4 + 3.8 * brightness) * fEnv);
                updateSvf(cutoff, 1.95);
            }

            final double dt = freq / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dt);
            phaseB = wrapPhase(phaseB + dt * 1.0018);
            // Slightly reedy, haunting French analog lead (narrow pulse + warm sawtooth)
            final double raw = 0.58 * polyBlepPulse(phaseA, dt, 0.24) + 0.42 * polyBlepSaw(phaseB, dt * 1.0018);
            final double sample = stepSvfLow(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepVcs3Drop(final double t, final double progress) {
            // Iconic EMS VCS3 resonant cosmic bubble / water-drop chirp
            final double attack = Math.min(1.0, t / 0.004);
            final double release = (progress > 0.72) ? Math.max(0.0, (1.0 - progress) / 0.28) : 1.0;
            final double env = attack * Math.exp(-t / 0.115) * release;

            final double glide = 1.0 - Math.exp(-t / 0.045);
            final double freq = baseFreqHz * Math.pow(endFreqHz / Math.max(1.0, baseFreqHz), glide);
            phaseA += TWO_PI * freq / SAMPLE_RATE;
            phaseB += TWO_PI * (freq * 2.0) / SAMPLE_RATE;

            final double bubble = 0.82 * Math.sin(phaseA + 0.22 * Math.exp(-t / 0.03) * Math.sin(phaseB))
                    + 0.18 * Math.exp(-t / 0.05) * Math.sin(phaseB);
            final double sample = bubble * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepGlassHarmonic(final double t, final double progress) {
            // RMI Harmonic Synthesizer pure glassy chime for NOTICE events
            final double attack = Math.min(1.0, t / 0.008);
            final double release = (progress > 0.70) ? Math.max(0.0, (1.0 - progress) / 0.30) : 1.0;
            final double env = attack * Math.exp(-t / 0.32) * release;
            final double upperEnv = Math.exp(-t / 0.12);

            phaseA += TWO_PI * baseFreqHz / SAMPLE_RATE;
            phaseB += TWO_PI * (baseFreqHz * 2.0) / SAMPLE_RATE;
            phaseC += TWO_PI * (baseFreqHz * 3.0) / SAMPLE_RATE;
            phaseSub += TWO_PI * (baseFreqHz * 5.0) / SAMPLE_RATE;

            final double chime = 0.52 * Math.sin(phaseA)
                    + 0.24 * Math.sin(phaseB)
                    + 0.16 * upperEnv * Math.sin(phaseC)
                    + 0.10 * upperEnv * Math.sin(phaseSub);
            final double sample = chime * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepMinipopsKick(final double t, final double progress) {
            // Vintage Korg MiniPops-7 woody, short analog bass drum (not an 808 sub boom)
            final double attack = Math.min(1.0, t / 0.002);
            final double release = (progress > 0.78) ? Math.max(0.0, (1.0 - progress) / 0.22) : 1.0;
            final double env = attack * Math.exp(-t / 0.068) * release;
            final double freq = 52.0 + 78.0 * Math.exp(-t / 0.015);
            phaseA += TWO_PI * freq / SAMPLE_RATE;
            final double woodClick = Math.exp(-t / 0.004) * nextWhiteNoise() * 0.18;
            final double sample = Math.tanh((Math.sin(phaseA) * 1.15 + woodClick)) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepMinipopsTick(final double t, final double progress, final boolean updateFilter) {
            // Vintage Korg MiniPops-7 maraca / guiro sandy analog tick
            final double attack = Math.min(1.0, t / 0.0018);
            final double release = (progress > 0.75) ? Math.max(0.0, (1.0 - progress) / 0.25) : 1.0;
            final double env = attack * Math.exp(-t / (0.022 + 0.020 * brightness)) * release;

            if (updateFilter) {
                updateSvf(3_800.0 + 3_200.0 * brightness, 2.4);
            }
            final double sample = stepSvfBand(nextWhiteNoise()) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepAnalogHorn(final double t, final double progress, final boolean updateFilter) {
            // Warm solemn 1970s analog horn swell for WARNING cues
            final double attack = Math.min(1.0, t / 0.032);
            final double release = (progress > 0.68) ? Math.max(0.0, (1.0 - progress) / 0.32) : 1.0;
            final double env = attack * Math.exp(-t / 0.34) * release;

            if (updateFilter) {
                final double fEnv = Math.sin(Math.PI * Math.min(1.0, progress * 1.15));
                final double cutoff = baseFreqHz * (1.25 + (1.6 + 3.6 * brightness) * fEnv);
                updateSvf(cutoff, 2.0);
            }

            final double dtA = (baseFreqHz * 0.9965) / SAMPLE_RATE;
            final double dtB = (baseFreqHz * 1.0035) / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dtA);
            phaseB = wrapPhase(phaseB + dtB);
            final double raw = 0.52 * polyBlepSaw(phaseA, dtA) + 0.48 * polyBlepSaw(phaseB, dtB);
            final double sample = stepSvfLow(raw) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepVcs3RingAlarm(final double t, final double progress, final boolean updateFilter) {
            // Authentic 1970s EMS VCS3 ring-modulated dissonant alarm + cosmic thunder rumble
            final double attack = Math.min(1.0, t / 0.008);
            final double release = (progress > 0.70) ? Math.max(0.0, (1.0 - progress) / 0.30) : 1.0;
            final double env = attack * Math.exp(-t / 0.32) * release;

            final boolean rawFailure = brightness >= 0.35;
            if (updateFilter) {
                final double cutoff = rawFailure
                        ? (2_100.0 * Math.exp(-t / 0.22) + 340.0)
                        : (320.0 * Math.exp(-t / 0.14) + 150.0);
                updateSvf(cutoff, rawFailure ? 2.6 : 1.2);
            }

            final double subFreq = Math.max(38.0, baseFreqHz * 0.5 * Math.pow(0.62, progress));
            phaseSub += TWO_PI * subFreq / SAMPLE_RATE;

            if (!rawFailure) {
                // Contained muted analog thud for known issues
                final double dt = baseFreqHz / SAMPLE_RATE;
                phaseA = wrapPhase(phaseA + dt);
                final double contained = 0.52 * stepSvfLow(polyBlepSaw(phaseA, dt)) + 0.48 * Math.sin(phaseSub);
                final double sample = contained * env * amplitude;
                outL = sample * gainL;
                outR = sample * gainR;
                return;
            }

            // Raw failure: pure analog VCS3 sine/triangle ring modulator at tritone + slow wobble + pink rumble
            final double wobble = 1.0 + 0.045 * Math.sin(TWO_PI * 7.2 * t);
            phaseA += TWO_PI * (baseFreqHz * wobble) / SAMPLE_RATE;
            phaseB += TWO_PI * (baseFreqHz * 1.41421356 / wobble) / SAMPLE_RATE;
            final double ringMod = Math.sin(phaseA) * Math.sin(phaseB);
            final double thunder = nextPinkNoise() * Math.exp(-t / 0.18) * 0.65;
            final double filtered = stepSvfLow(0.68 * ringMod + thunder);
            final double sample = Math.tanh((filtered + 0.45 * Math.sin(phaseSub)) * 1.2) * env * amplitude;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepWindSwell(final double progress, final boolean updateFilter) {
            // Iconic Oxygène resonant pink-noise cosmic wind sweep through the Small Stone phaser
            final double env = Math.sin(Math.PI * progress);
            final double freq = baseFreqHz * Math.pow(endFreqHz / Math.max(1.0, baseFreqHz), progress);

            if (updateFilter) {
                updateSvf(freq * (1.1 + 0.9 * brightness), 3.4);
            }

            final double pink = nextPinkNoise();
            final double sample = (0.72 * stepSvfBand(pink) + 0.28 * stepSvfLow(pink)) * env * amplitude * 1.85;
            outL = sample * gainL;
            outR = sample * gainR;
        }

        private void stepCosmicFade(final double t, final double progress, final boolean updateFilter) {
            // Warm low-register disappearing analog sigh (pitch & LP filter gently fading down)
            final double attack = Math.min(1.0, t / 0.024);
            final double decay = Math.pow(Math.max(0.0, 1.0 - progress), 1.9);
            final double env = attack * decay;
            final double freq = baseFreqHz * Math.pow(endFreqHz / Math.max(1.0, baseFreqHz), progress);

            if (updateFilter) {
                final double closing = Math.pow(Math.max(0.0, 1.0 - progress), 1.4);
                final double cutoff = freq * (1.05 + (1.45 + 1.65 * brightness) * closing);
                updateSvf(cutoff, 1.25);
            }

            final double dt = freq / SAMPLE_RATE;
            phaseA = wrapPhase(phaseA + dt * 0.9968);
            phaseB = wrapPhase(phaseB + dt * 1.0032);
            phaseSub += TWO_PI * dt;
            final double raw = 0.30 * polyBlepSaw(phaseA, dt * 0.9968)
                    + 0.30 * polyBlepSaw(phaseB, dt * 1.0032)
                    + 0.46 * Math.sin(phaseSub);
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
