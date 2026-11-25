package works.lysenko.base.output.sonifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.sonifier.AdpSynth.AdpEvent;
import works.lysenko.base.output.sonifier.AdpSynth.VoiceKind;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdpSonifierTest {

    @Test
    void resolveAdpPitchProduces24TetMicrotonesInsideWarmGuitarRegister() {
        final double[] bassRoots = {31.0, 32.5, 34.0, 36.0, 36.5, 37.5};
        boolean sawQuarterTone = false;

        for (int mood = 0; mood <= 2; mood++) {
            for (final double bassRoot : bassRoots) {
                for (int col = 0; col <= 8; col++) {
                    for (int rowTenths = 0; rowTenths <= 120; rowTenths += 5) {
                        final double rowRatio = 1.0 - Math.min(1.0, rowTenths / 120.0);
                        final AdpSonifier.NodeState node = new AdpSonifier.NodeState(
                                "root/n_" + col + "_" + rowTenths, col, rowTenths, 'L', 1, 2, 'P', false);
                        final double pitch = AdpSonifier.resolveAdpPitch(mood, bassRoot, node, rowRatio);
                        assertTrue(pitch >= 43.0 && pitch <= 58.0,
                                () -> "Expected bass-guitar / low-baritone MIDI [43..58], got " + pitch);
                        final double halfSteps = pitch * 2.0;
                        assertEquals(Math.rint(halfSteps), halfSteps, 1e-9,
                                () -> "Pitch must lie on 24-TET quarter-tone grid, got " + pitch);
                        if (Math.abs(pitch - Math.rint(pitch)) > 0.25) {
                            sawQuarterTone = true;
                        }
                    }
                }
            }
        }
        assertTrue(sawQuarterTone, "ADP sonifier must produce genuine 24-TET quarter-tone pitches");
    }

    @Test
    void synthesizesMicrotonalMathRockDeterministicallyWithDualAmpStereo(@TempDir final Path tempDir) throws Exception {
        final AdpSonifier adp = new AdpSonifier();
        final File frame1 = tempDir.resolve("frame_0001.svg").toFile();
        final File frame2 = tempDir.resolve("frame_0002.svg").toFile();
        final File frame3 = tempDir.resolve("frame_0003.svg").toFile();
        final File frame4 = tempDir.resolve("frame_0004.svg").toFile();

        // Frame 1: branch + partial leaf + mono leaf + unvisited leaf
        final Result branch1 = new Result(ScenarioType.NODE, null, null, null, List.of(), 1, 0);
        branch1.updateStatus(ExecutionStatus.PASSED);
        final TreeHtml.NodeData root1 = new TreeHtml.NodeData("root", "MataZyklek", "Suite", 0, 1, branch1);
        final TreeHtml.NodeData leafA1 = new TreeHtml.NodeData(
                "leafA", "Fabienk", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 1, 0));
        final TreeHtml.NodeData mono1 = new TreeHtml.NodeData(
                "monoA", "Sarniezz", "Suite", 3, 1, new Result(ScenarioType.MONO, null, null, null, List.of(), 0, 0));
        final TreeHtml.NodeData leafB1 = new TreeHtml.NodeData(
                "leafB", "Sherpa", "Suite", 5, 2, new Result(ScenarioType.LEAF, null, null, null, List.of(), 0, 0));
        leafA1.setParent(root1);
        mono1.setParent(root1);
        leafB1.setParent(root1);
        root1.children().addAll(List.of(leafA1, mono1, leafB1));

        // Frame 2: leafA completes target=2 (microtonal harmonized phrase), monoA completes, leafB pruned (scrape)
        final TreeHtml.NodeData root2 = new TreeHtml.NodeData(
                "root", "MataZyklek", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leafA2 = new TreeHtml.NodeData(
                "leafA", "Fabienk", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData mono2 = new TreeHtml.NodeData(
                "monoA", "Sarniezz", "Suite", 3, 1, new Result(ScenarioType.MONO, null, null, null, List.of(), 1, 0));
        leafA2.setParent(root2);
        mono2.setParent(root2);
        root2.children().addAll(List.of(leafA2, mono2));

        // Frame 3: raw warning -> foot-controlled ring modulator stab
        final Result warnRes = new Result(ScenarioType.LEAF, null, null, null, List.of(), 3, 1);
        warnRes.updateStatus(ExecutionStatus.WARNING);
        final TreeHtml.NodeData root3 = new TreeHtml.NodeData(
                "root", "MataZyklek", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 3, 0));
        final TreeHtml.NodeData leafA3 = new TreeHtml.NodeData("leafA", "Fabienk", "Suite", 2, 0, warnRes);
        leafA3.setParent(root3);
        root3.children().add(leafA3);

        // Frame 4: column contraction + raw failure meltdown + upset parent
        final Result failRes = new Result(ScenarioType.LEAF, null, null, null, List.of(), 4, 1);
        failRes.updateStatus(ExecutionStatus.FAILED);
        final TreeHtml.NodeData root4 = new TreeHtml.NodeData(
                "root", "MataZyklek", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 4, 0));
        final TreeHtml.NodeData leafA4 = new TreeHtml.NodeData("leafA", "Fabienk", "Suite", 1, 0, failRes);
        leafA4.setParent(root4);
        root4.children().add(leafA4);

        adp.capture(frame1, new TreeHtml.TreeLayout(new ArrayList<>(List.of(root1, leafA1, mono1, leafB1)), List.of()), 2);
        adp.capture(frame2, new TreeHtml.TreeLayout(new ArrayList<>(List.of(root2, leafA2, mono2)), List.of()), 2);
        adp.capture(frame3, new TreeHtml.TreeLayout(new ArrayList<>(List.of(root3, leafA3)), List.of()), 2);
        adp.capture(frame4, new TreeHtml.TreeLayout(new ArrayList<>(List.of(root4, leafA4)), List.of()), 2);

        assertTrue(AdpSonifier.snapshotFile(frame1).isFile());
        assertTrue(adp.canSonify(List.of(frame1, frame2, frame3, frame4)));

        // Verify scheduled musical events include microtonal bass, guitar, dry drums, ring mod & meltdown
        final Map<Integer, List<AdpEvent>> events =
                AdpSonifier.composeAdpEvents(List.of(frame1, frame2, frame3, frame4), AdpSynth.SAMPLE_RATE / 10);
        final List<AdpEvent> all = events.values().stream().flatMap(List::stream).toList();
        assertTrue(all.stream().anyMatch(e -> e.kind() == VoiceKind.P_BASS));
        assertTrue(all.stream().anyMatch(e -> e.kind() == VoiceKind.STRAT_RIFF));
        assertTrue(all.stream().anyMatch(e -> e.kind() == VoiceKind.STRAT_MUTE));
        assertTrue(all.stream().anyMatch(e -> e.kind() == VoiceKind.STRAT_HARMONY));
        assertTrue(all.stream().anyMatch(e -> e.kind() == VoiceKind.DRY_KICK));
        assertTrue(all.stream().anyMatch(e -> e.kind() == VoiceKind.PICCOLO_SNARE));
        assertTrue(all.stream().anyMatch(e -> e.kind() == VoiceKind.RING_MOD_WARN));
        assertTrue(all.stream().anyMatch(e -> e.kind() == VoiceKind.FAULT_MELTDOWN));
        assertTrue(all.stream().anyMatch(e -> Math.abs(e.midiPitch() - Math.rint(e.midiPitch())) > 0.25),
                "Scheduled events must contain quarter-tone microtonal pitches");

        final List<AdpEvent> riffEvents = all.stream()
                .filter(e -> e.kind() == VoiceKind.STRAT_RIFF)
                .toList();
        assertFalse(riffEvents.isEmpty());
        for (final AdpEvent ev : riffEvents) {
            assertTrue(ev.midiPitch() >= 43.0 && ev.midiPitch() <= 58.0,
                    () -> "Main progression / regular leaf riff pitch should stay in bass-guitar / low-baritone range [43..58], got " + ev.midiPitch());
        }

        final List<AdpEvent> muteEvents = all.stream()
                .filter(e -> e.kind() == VoiceKind.STRAT_MUTE)
                .toList();
        assertFalse(muteEvents.isEmpty());
        for (final AdpEvent ev : muteEvents) {
            assertTrue(ev.midiPitch() >= 31.0 && ev.midiPitch() <= 48.0,
                    () -> "Palm-muted chug pitch should stay in bass-guitar range [31..48], got " + ev.midiPitch());
        }

        final List<AdpEvent> bassEvents = all.stream()
                .filter(e -> e.kind() == VoiceKind.P_BASS)
                .toList();
        assertFalse(bassEvents.isEmpty());
        for (final AdpEvent ev : bassEvents) {
            assertTrue(ev.midiPitch() >= 31.0 && ev.midiPitch() <= 48.0,
                    () -> "P-Bass ostinato pitch should stay in deep bass range [31..48], got " + ev.midiPitch());
        }

        // Verify deterministic stereo WAV rendering
        final File wav1 = tempDir.resolve("adp_1.wav").toFile();
        final File wav2 = tempDir.resolve("adp_2.wav").toFile();
        adp.writeWav(List.of(frame1, frame2, frame3, frame4), wav1, 10);
        adp.writeWav(List.of(frame1, frame2, frame3, frame4), wav2, 10);

        final byte[] bytes1 = Files.readAllBytes(wav1.toPath());
        assertEquals("RIFF", new String(bytes1, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("WAVE", new String(bytes1, 8, 4, StandardCharsets.US_ASCII));
        final int channels = (bytes1[22] & 0xFF) | ((bytes1[23] & 0xFF) << 8);
        assertEquals(2, channels, "ADP must output 2-channel stereo PCM WAV");
        assertEquals(44 + ((4 * 4_410) + AdpSynth.TAIL_SAMPLES) * 2 * Short.BYTES, bytes1.length);
        assertTrue(hasSound(bytes1));
        assertTrue(hasStereoSeparation(bytes1), "Dual-amp bass/guitar placement and room bus must produce stereo width");
        assertArrayEquals(bytes1, Files.readAllBytes(wav2.toPath()), "ADP synthesis must be bit-for-bit deterministic");
    }

    @Test
    void distinguishesCleanWarningAndFailureVerdictsFromSvgFrames(@TempDir final Path tempDir) throws Exception {
        final AdpSonifier adp = new AdpSonifier();
        final File cleanSvg = tempDir.resolve("clean_0001.svg").toFile();
        final File warnSvg = tempDir.resolve("warn_0001.svg").toFile();
        final File failSvg = tempDir.resolve("fail_0001.svg").toFile();

        Files.writeString(cleanSvg.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                        + "  <g transform=\"translate(0.00, 0.00) scale(1.0000)\">\n"
                        + "    <g id=\"scenario_0_Root\" class=\"node-card passed\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Root ▷ (Passed)</title><text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Leaf\" class=\"node-card passed\" transform=\"translate(310, 92)\">\n"
                        + "      <title>Leaf ◼ (Passed)</title><text class=\"tx1\">✓ 1/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <text class=\"tx3\">Leaves: 1/1</text><text class=\"tx3\">Test #1</text>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        Files.writeString(warnSvg.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                        + "  <g transform=\"translate(0.00, 0.00) scale(1.0000)\">\n"
                        + "    <g id=\"scenario_0_Root\" class=\"node-card warning\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Root ▷ (Warning)</title><text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Leaf\" class=\"node-card warning\" transform=\"translate(310, 92)\">\n"
                        + "      <title>Leaf ◼ (Warning)</title><text class=\"tx1\">1/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <text class=\"tx3\">Leaves: 1/1</text><text class=\"tx3\">Test #1</text>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        Files.writeString(failSvg.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                        + "  <g transform=\"translate(0.00, 0.00) scale(1.0000)\">\n"
                        + "    <g id=\"scenario_0_Root\" class=\"node-card failed\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Root ▷ (Failed)</title><text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Leaf\" class=\"node-card failed\" transform=\"translate(310, 92)\">\n"
                        + "      <title>Leaf ◼ (Failed)</title><text class=\"tx1\">1/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <text class=\"tx3\">Leaves: 1/1</text><text class=\"tx3\">Test #1</text>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        final File cleanWav = tempDir.resolve("adp_clean.wav").toFile();
        final File warnWav = tempDir.resolve("adp_warn.wav").toFile();
        final File failWav = tempDir.resolve("adp_fail.wav").toFile();

        assertTrue(adp.canSonify(List.of(cleanSvg)));
        adp.writeWav(List.of(cleanSvg), cleanWav, 10);
        adp.writeWav(List.of(warnSvg), warnWav, 10);
        adp.writeWav(List.of(failSvg), failWav, 10);

        final byte[] cleanBytes = Files.readAllBytes(cleanWav.toPath());
        final byte[] warnBytes = Files.readAllBytes(warnWav.toPath());
        final byte[] failBytes = Files.readAllBytes(failWav.toPath());

        assertTrue(hasSound(cleanBytes));
        assertTrue(hasSound(warnBytes));
        assertTrue(hasSound(failBytes));
        assertTrue(hasStereoSeparation(cleanBytes));
        assertFalse(Arrays.equals(cleanBytes, warnBytes), "Clean and warning verdicts must sound distinct");
        assertFalse(Arrays.equals(cleanBytes, failBytes), "Clean and failure verdicts must sound distinct");
        assertFalse(Arrays.equals(warnBytes, failBytes), "Warning and failure verdicts must sound distinct");
    }

    private static boolean hasSound(final byte[] wav) {
        for (int i = 44; i + 1 < wav.length; i += 2) {
            final short sample = (short) ((wav[i] & 0xFF) | ((wav[i + 1] & 0xFF) << 8));
            if (Math.abs(sample) > 500) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasStereoSeparation(final byte[] wav) {
        for (int i = 44; i + 3 < wav.length; i += 4) {
            final short l = (short) ((wav[i] & 0xFF) | ((wav[i + 1] & 0xFF) << 8));
            final short r = (short) ((wav[i + 2] & 0xFF) | ((wav[i + 3] & 0xFF) << 8));
            if (Math.abs(l - r) > 300) {
                return true;
            }
        }
        return false;
    }
}
