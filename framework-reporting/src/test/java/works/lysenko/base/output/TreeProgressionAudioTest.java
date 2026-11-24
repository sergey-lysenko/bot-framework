package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.base.output.sonifier.*;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeProgressionAudioTest {

    private final CopilotSonifier sonifier = new CopilotSonifier();

    @Test
    void registryResolvesModes() {
        assertTrue(TreeProgressionAudio.find("copilot").orElseThrow() instanceof CopilotSonifier);
        assertTrue(TreeProgressionAudio.find("Claude").orElseThrow() instanceof ClaudeSonifier);
        assertTrue(TreeProgressionAudio.find("Gemini").orElseThrow() instanceof GeminiSonifier);
        assertTrue(TreeProgressionAudio.find("orpheus").orElseThrow() instanceof OrpheusSonifier);
        assertTrue(TreeProgressionAudio.find("Orpheus").orElseThrow() instanceof OrpheusSonifier);
        assertTrue(TreeProgressionAudio.find("aurora").orElseThrow() instanceof AuroraSonifier);
        assertTrue(TreeProgressionAudio.find("Aurora").orElseThrow() instanceof AuroraSonifier);
        assertTrue(TreeProgressionAudio.find("carbon").orElseThrow() instanceof CarbonSonifier);
        assertTrue(TreeProgressionAudio.find("Carbon").orElseThrow() instanceof CarbonSonifier);
        assertTrue(TreeProgressionAudio.find("neon").orElseThrow() instanceof NeonSonifier);
        assertTrue(TreeProgressionAudio.find("Neon").orElseThrow() instanceof NeonSonifier);
        assertTrue(TreeProgressionAudio.find("jmj").orElseThrow() instanceof JmjSonifier);
        assertTrue(TreeProgressionAudio.find("JMJ").orElseThrow() instanceof JmjSonifier);
        assertTrue(TreeProgressionAudio.find("dnb").orElseThrow() instanceof DnbSonifier);
        assertTrue(TreeProgressionAudio.find("DnB").orElseThrow() instanceof DnbSonifier);
        assertTrue(TreeProgressionAudio.find("none").isEmpty());
        assertTrue(TreeProgressionAudio.find("true").isEmpty());
        assertTrue(TreeProgressionAudio.find(null).isEmpty());
    }

    @Test
    void selectRandomPicksFromCandidates() {
        assertEquals("none", TreeProgressionAudio.selectRandom(null));
        assertEquals("none", TreeProgressionAudio.selectRandom(""));
        assertEquals("none", TreeProgressionAudio.selectRandom("unknown, invalid, none"));
        assertEquals("dnb", TreeProgressionAudio.selectRandom("dnb"));
        assertEquals("neon", TreeProgressionAudio.selectRandom("neon, invalid"));

        final java.util.Set<String> chosen = new java.util.HashSet<>();
        final String candidates = "dnb, neon, jmj, carbon";
        for (int i = 0; i < 50; i++) {
            final String selected = TreeProgressionAudio.selectRandom(candidates);
            assertTrue(java.util.List.of("dnb", "neon", "jmj", "carbon").contains(selected));
            chosen.add(selected);
        }
        assertTrue(chosen.size() > 1, "Random selection should pick multiple distinct options over 50 iterations");
    }

    @Test
    void carbonSynthesizesStereoSpatialTelemetryDeterministically(@TempDir final Path tempDir) throws Exception {
        final CarbonSonifier carbon = new CarbonSonifier();
        final File frame1 = tempDir.resolve("frame_0001.svg").toFile();
        final File frame2 = tempDir.resolve("frame_0002.svg").toFile();
        final File frame3 = tempDir.resolve("frame_0003.svg").toFile();

        // Frame 1: root branch (col 0), partial leaf (col 2), unvisited sibling leaf (col 4)
        final TreeHtml.NodeData root1 = new TreeHtml.NodeData(
                "root", "SignIn", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 1, 0));
        final TreeHtml.NodeData leafLeft1 = new TreeHtml.NodeData(
                "leafL", "Verify", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 1, 0));
        final TreeHtml.NodeData leafRight1 = new TreeHtml.NodeData(
                "leafR", "Checkout", "Suite", 4, 2, new Result(ScenarioType.LEAF, null, null, null, List.of(), 0, 0));
        leafLeft1.setParent(root1);
        leafRight1.setParent(root1);
        root1.children().addAll(List.of(leafLeft1, leafRight1));
        final TreeHtml.TreeLayout layout1 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root1, leafLeft1, leafRight1)), List.of());

        // Frame 2: leafLeft completes target=2, leafRight pruned (shedding tree mass -> updraft sweep)
        final TreeHtml.NodeData root2 = new TreeHtml.NodeData(
                "root", "SignIn", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leafLeft2 = new TreeHtml.NodeData(
                "leafL", "Verify", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 2, 0));
        leafLeft2.setParent(root2);
        root2.children().add(leafLeft2);
        final TreeHtml.TreeLayout layout2 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root2, leafLeft2)), List.of());

        // Frame 3: over-execution (3/2) with warning status
        final Result overWarn = new Result(ScenarioType.LEAF, null, null, null, List.of(), 3, 1);
        overWarn.updateStatus(ExecutionStatus.WARNING);
        final TreeHtml.NodeData root3 = new TreeHtml.NodeData(
                "root", "SignIn", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 3, 0));
        final TreeHtml.NodeData leafLeft3 = new TreeHtml.NodeData(
                "leafL", "Verify", "Suite", 2, 0, overWarn);
        leafLeft3.setParent(root3);
        root3.children().add(leafLeft3);
        final TreeHtml.TreeLayout layout3 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root3, leafLeft3)), List.of());

        carbon.capture(frame1, layout1, 2);
        carbon.capture(frame2, layout2, 2);
        carbon.capture(frame3, layout3, 2);

        assertTrue(CarbonSonifier.snapshotFile(frame1).isFile());
        assertTrue(carbon.canSonify(List.of(frame1, frame2, frame3)));

        final File firstWav = tempDir.resolve("carbon_1.wav").toFile();
        final File secondWav = tempDir.resolve("carbon_2.wav").toFile();
        carbon.writeWav(List.of(frame1, frame2, frame3), firstWav, 10);
        carbon.writeWav(List.of(frame1, frame2, frame3), secondWav, 10);

        final byte[] wav = Files.readAllBytes(firstWav.toPath());
        assertEquals("RIFF", new String(wav, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("WAVE", new String(wav, 8, 4, StandardCharsets.US_ASCII));
        final int channels = (wav[22] & 0xFF) | ((wav[23] & 0xFF) << 8);
        assertEquals(2, channels, "Carbon must output 2-channel stereo PCM WAV");
        assertEquals(44 + ((3 * 4_410) + 44_100) * 2 * Short.BYTES, wav.length);
        assertTrue(hasSound(wav));
        assertTrue(hasStereoSeparation(wav), "Left and right channels must reflect spatial topology");
        assertArrayEquals(wav, Files.readAllBytes(secondWav.toPath()), "Carbon synthesis must be bit-for-bit deterministic");
    }

    @Test
    void neonSynthesizesStereoAnalogTelemetryAndCompactionSweepDeterministically(@TempDir final Path tempDir) throws Exception {
        final NeonSonifier neon = new NeonSonifier();
        final File frame1 = tempDir.resolve("frame_0001.svg").toFile();
        final File frame2 = tempDir.resolve("frame_0002.svg").toFile();
        final File frame3 = tempDir.resolve("frame_0003.svg").toFile();
        final File frame4 = tempDir.resolve("frame_0004.svg").toFile();

        // Frame 1: wide 5-column layout with partial leaf, passed branch, and unvisited far-right leaf
        final Result branch1Res = new Result(ScenarioType.NODE, null, null, null, List.of(), 1, 0);
        branch1Res.updateStatus(ExecutionStatus.PASSED);
        final TreeHtml.NodeData root1 = new TreeHtml.NodeData("root", "Auth", "Suite", 0, 1, branch1Res);
        final TreeHtml.NodeData leafA1 = new TreeHtml.NodeData(
                "leafA", "Login", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 1, 0));
        final TreeHtml.NodeData leafB1 = new TreeHtml.NodeData(
                "leafB", "Audit", "Suite", 5, 2, new Result(ScenarioType.LEAF, null, null, null, List.of(), 0, 0));
        leafA1.setParent(root1);
        leafB1.setParent(root1);
        root1.children().addAll(List.of(leafA1, leafB1));
        final TreeHtml.TreeLayout layout1 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root1, leafA1, leafB1)), List.of());

        // Frame 2: leafA completes target=2, leafB hidden/pruned (triggers pruning + column contraction sweep)
        final TreeHtml.NodeData root2 = new TreeHtml.NodeData(
                "root", "Auth", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leafA2 = new TreeHtml.NodeData(
                "leafA", "Login", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 2, 0));
        leafA2.setParent(root2);
        root2.children().add(leafA2);
        final TreeHtml.TreeLayout layout2 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root2, leafA2)), List.of());

        // Frame 3: over-execution with warning
        final Result overWarn = new Result(ScenarioType.LEAF, null, null, null, List.of(), 3, 1);
        overWarn.updateStatus(ExecutionStatus.WARNING);
        final TreeHtml.NodeData root3 = new TreeHtml.NodeData(
                "root", "Auth", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 3, 0));
        final TreeHtml.NodeData leafA3 = new TreeHtml.NodeData("leafA", "Login", "Suite", 2, 0, overWarn);
        leafA3.setParent(root3);
        root3.children().add(leafA3);
        final TreeHtml.TreeLayout layout3 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root3, leafA3)), List.of());

        // Frame 4: raw failure glitch + brooding dark finale
        final Result rawFail = new Result(ScenarioType.LEAF, null, null, null, List.of(), 4, 1);
        rawFail.updateStatus(ExecutionStatus.FAILED);
        final TreeHtml.NodeData root4 = new TreeHtml.NodeData(
                "root", "Auth", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 4, 0));
        final TreeHtml.NodeData leafA4 = new TreeHtml.NodeData("leafA", "Login", "Suite", 1, 0, rawFail);
        leafA4.setParent(root4);
        root4.children().add(leafA4);
        final TreeHtml.TreeLayout layout4 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root4, leafA4)), List.of());

        neon.capture(frame1, layout1, 2);
        neon.capture(frame2, layout2, 2);
        neon.capture(frame3, layout3, 2);
        neon.capture(frame4, layout4, 2);

        assertTrue(NeonSonifier.snapshotFile(frame1).isFile());
        assertTrue(neon.canSonify(List.of(frame1, frame2, frame3, frame4)));

        final File firstWav = tempDir.resolve("neon_1.wav").toFile();
        final File secondWav = tempDir.resolve("neon_2.wav").toFile();
        neon.writeWav(List.of(frame1, frame2, frame3, frame4), firstWav, 10);
        neon.writeWav(List.of(frame1, frame2, frame3, frame4), secondWav, 10);

        final byte[] wav = Files.readAllBytes(firstWav.toPath());
        assertEquals("RIFF", new String(wav, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("WAVE", new String(wav, 8, 4, StandardCharsets.US_ASCII));
        final int channels = (wav[22] & 0xFF) | ((wav[23] & 0xFF) << 8);
        assertEquals(2, channels, "Neon must output 2-channel stereo PCM WAV");
        final int tailSamples = (int) (44_100 * 1.25);
        assertEquals(44 + ((4 * 4_410) + tailSamples) * 2 * Short.BYTES, wav.length);
        assertTrue(hasSound(wav));
        assertTrue(hasStereoSeparation(wav), "Neon stereo chorus and panning must produce L/R separation");
        assertArrayEquals(wav, Files.readAllBytes(secondWav.toPath()), "Neon synthesis must be bit-for-bit deterministic");
    }

    @Test
    void neonSonifiesSvgFramesDirectlyWhenSnapshotsAreAbsent(@TempDir final Path tempDir) throws Exception {
        final NeonSonifier neon = new NeonSonifier();
        final File svg1 = tempDir.resolve("frame_0001.svg").toFile();
        final File svg2 = tempDir.resolve("frame_0002.svg").toFile();

        Files.writeString(svg1.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                        + "  <g transform=\"translate(0.00, 0.00) scale(1.0000)\">\n"
                        + "    <g id=\"scenario_0_Users\" class=\"node-card notice\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Users ▷ (Notice)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#06b6d4\" />\n"
                        + "      <text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Verify\" class=\"node-card unvisited\" transform=\"translate(580, 92)\">\n"
                        + "      <title>Verify ◼ (Unvisited)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#64748b\" />\n"
                        + "      <text class=\"tx1\">0/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <g transform=\"scale(1.0000)\">\n"
                        + "    <text class=\"tx3\">Leaves: 0/1</text>\n"
                        + "    <text class=\"tx3\">Test #1</text>\n"
                        + "  </g>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        Files.writeString(svg2.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                        + "  <g transform=\"translate(18.00, -12.00) scale(1.2500)\">\n"
                        + "    <g id=\"scenario_0_Users\" class=\"node-card passed\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Users ▷ (Passed)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#22c55e\" />\n"
                        + "      <text class=\"tx1\">2 execs</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Verify\" class=\"node-card passed\" transform=\"translate(310, 92)\">\n"
                        + "      <title>Verify ◼ (Passed)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#22c55e\" />\n"
                        + "      <text class=\"tx1\">✓ 1/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <g transform=\"scale(1.0000)\">\n"
                        + "    <text class=\"tx3\">Leaves: 1/1</text>\n"
                        + "    <text class=\"tx3\">Test #2</text>\n"
                        + "  </g>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        assertTrue(neon.canSonify(List.of(svg1, svg2)));
        final File wavOut = tempDir.resolve("neon_svg.wav").toFile();
        neon.writeWav(List.of(svg1, svg2), wavOut, 10);
        final byte[] wav = Files.readAllBytes(wavOut.toPath());
        assertTrue(hasSound(wav));
        assertTrue(hasStereoSeparation(wav));
    }

    @Test
    void jmjSynthesizesPhasedCosmicAnalogTelemetryDeterministically(@TempDir final Path tempDir) throws Exception {
        final JmjSonifier jmj = new JmjSonifier();
        final File frame1 = tempDir.resolve("frame_0001.svg").toFile();
        final File frame2 = tempDir.resolve("frame_0002.svg").toFile();
        final File frame3 = tempDir.resolve("frame_0003.svg").toFile();
        final File frame4 = tempDir.resolve("frame_0004.svg").toFile();

        // Frame 1: opening wind + phased strings + partial leaf + unvisited far-right leaf
        final Result branch1Res = new Result(ScenarioType.NODE, null, null, null, List.of(), 1, 0);
        branch1Res.updateStatus(ExecutionStatus.PASSED);
        final TreeHtml.NodeData root1 = new TreeHtml.NodeData("root", "Oxygene", "Suite", 0, 1, branch1Res);
        final TreeHtml.NodeData leafA1 = new TreeHtml.NodeData(
                "leafA", "Part1", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 1, 0));
        final TreeHtml.NodeData leafB1 = new TreeHtml.NodeData(
                "leafB", "Part4", "Suite", 5, 2, new Result(ScenarioType.LEAF, null, null, null, List.of(), 0, 0));
        leafA1.setParent(root1);
        leafB1.setParent(root1);
        root1.children().addAll(List.of(leafA1, leafB1));
        final TreeHtml.TreeLayout layout1 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root1, leafA1, leafB1)), List.of());

        // Frame 2: leafA completes target=2 (Oxygène lead motif), leafB hidden/pruned (warm low COSMIC_FADE)
        final TreeHtml.NodeData root2 = new TreeHtml.NodeData(
                "root", "Oxygene", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leafA2 = new TreeHtml.NodeData(
                "leafA", "Part1", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 2, 0));
        leafA2.setParent(root2);
        root2.children().add(leafA2);
        final TreeHtml.TreeLayout layout2 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root2, leafA2)), List.of());

        // Frame 3: over-execution with warning horn
        final Result overWarn = new Result(ScenarioType.LEAF, null, null, null, List.of(), 3, 1);
        overWarn.updateStatus(ExecutionStatus.WARNING);
        final TreeHtml.NodeData root3 = new TreeHtml.NodeData(
                "root", "Oxygene", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 3, 0));
        final TreeHtml.NodeData leafA3 = new TreeHtml.NodeData("leafA", "Part1", "Suite", 2, 0, overWarn);
        leafA3.setParent(root3);
        root3.children().add(leafA3);
        final TreeHtml.TreeLayout layout3 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root3, leafA3)), List.of());

        // Frame 4: column contraction (wind sweep) + VCS3 ring-mod failure + cosmic finale
        final Result rawFail = new Result(ScenarioType.LEAF, null, null, null, List.of(), 4, 1);
        rawFail.updateStatus(ExecutionStatus.FAILED);
        final TreeHtml.NodeData root4 = new TreeHtml.NodeData(
                "root", "Oxygene", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 4, 0));
        final TreeHtml.NodeData leafA4 = new TreeHtml.NodeData("leafA", "Part1", "Suite", 1, 0, rawFail);
        leafA4.setParent(root4);
        root4.children().add(leafA4);
        final TreeHtml.TreeLayout layout4 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root4, leafA4)), List.of());

        jmj.capture(frame1, layout1, 2);
        jmj.capture(frame2, layout2, 2);
        jmj.capture(frame3, layout3, 2);
        jmj.capture(frame4, layout4, 2);

        assertTrue(JmjSonifier.snapshotFile(frame1).isFile());
        assertTrue(jmj.canSonify(List.of(frame1, frame2, frame3, frame4)));

        final File firstWav = tempDir.resolve("jmj_1.wav").toFile();
        final File secondWav = tempDir.resolve("jmj_2.wav").toFile();
        jmj.writeWav(List.of(frame1, frame2, frame3, frame4), firstWav, 10);
        jmj.writeWav(List.of(frame1, frame2, frame3, frame4), secondWav, 10);

        final byte[] wav = Files.readAllBytes(firstWav.toPath());
        assertEquals("RIFF", new String(wav, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("WAVE", new String(wav, 8, 4, StandardCharsets.US_ASCII));
        final int channels = (wav[22] & 0xFF) | ((wav[23] & 0xFF) << 8);
        assertEquals(2, channels, "JMJ must output 2-channel stereo PCM WAV");
        final int tailSamples = (int) (44_100 * 1.65);
        assertEquals(44 + ((4 * 4_410) + tailSamples) * 2 * Short.BYTES, wav.length);
        assertTrue(hasSound(wav));
        assertTrue(hasStereoSeparation(wav), "JMJ Small Stone phaser and Space Echo must produce stereo width");
        assertArrayEquals(wav, Files.readAllBytes(secondWav.toPath()), "JMJ synthesis must be bit-for-bit deterministic");
    }

    @Test
    void jmjSonifiesSvgFramesDirectlyWhenSnapshotsAreAbsent(@TempDir final Path tempDir) throws Exception {
        final JmjSonifier jmj = new JmjSonifier();
        final File svg1 = tempDir.resolve("frame_0001.svg").toFile();
        final File svg2 = tempDir.resolve("frame_0002.svg").toFile();

        Files.writeString(svg1.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                        + "  <g transform=\"translate(0.00, 0.00) scale(1.0000)\">\n"
                        + "    <g id=\"scenario_0_Equinoxe\" class=\"node-card notice\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Equinoxe ▷ (Notice)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#06b6d4\" />\n"
                        + "      <text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Finale\" class=\"node-card unvisited\" transform=\"translate(580, 92)\">\n"
                        + "      <title>Finale ◼ (Unvisited)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#64748b\" />\n"
                        + "      <text class=\"tx1\">0/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <g transform=\"scale(1.0000)\">\n"
                        + "    <text class=\"tx3\">Leaves: 0/1</text>\n"
                        + "    <text class=\"tx3\">Test #1</text>\n"
                        + "  </g>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        Files.writeString(svg2.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                        + "  <g transform=\"translate(18.00, -12.00) scale(1.2500)\">\n"
                        + "    <g id=\"scenario_0_Equinoxe\" class=\"node-card passed\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Equinoxe ▷ (Passed)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#22c55e\" />\n"
                        + "      <text class=\"tx1\">2 execs</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Finale\" class=\"node-card passed\" transform=\"translate(310, 92)\">\n"
                        + "      <title>Finale ◼ (Passed)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#22c55e\" />\n"
                        + "      <text class=\"tx1\">✓ 1/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <g transform=\"scale(1.0000)\">\n"
                        + "    <text class=\"tx3\">Leaves: 1/1</text>\n"
                        + "    <text class=\"tx3\">Test #2</text>\n"
                        + "  </g>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        assertTrue(jmj.canSonify(List.of(svg1, svg2)));
        final File wavOut = tempDir.resolve("jmj_svg.wav").toFile();
        jmj.writeWav(List.of(svg1, svg2), wavOut, 10);
        final byte[] wav = Files.readAllBytes(wavOut.toPath());
        assertTrue(hasSound(wav));
        assertTrue(hasStereoSeparation(wav));
    }

    @Test
    void dnbSynthesizesLiquidBreakbeatTelemetryDeterministically(@TempDir final Path tempDir) throws Exception {
        final DnbSonifier dnb = new DnbSonifier();
        final File frame1 = tempDir.resolve("frame_0001.svg").toFile();
        final File frame2 = tempDir.resolve("frame_0002.svg").toFile();
        final File frame3 = tempDir.resolve("frame_0003.svg").toFile();
        final File frame4 = tempDir.resolve("frame_0004.svg").toFile();

        // Frame 1: branch + partial leaf + mono leaf + unvisited far-right leaf
        final Result branch1Res = new Result(ScenarioType.NODE, null, null, null, List.of(), 1, 0);
        branch1Res.updateStatus(ExecutionStatus.PASSED);
        final TreeHtml.NodeData root1 = new TreeHtml.NodeData("root", "Checkout", "Suite", 0, 1, branch1Res);
        final TreeHtml.NodeData leafA1 = new TreeHtml.NodeData(
                "leafA", "CardPay", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 1, 0));
        final TreeHtml.NodeData mono1 = new TreeHtml.NodeData(
                "monoA", "Receipt", "Suite", 3, 1, new Result(ScenarioType.MONO, null, null, null, List.of(), 0, 0));
        final TreeHtml.NodeData leafB1 = new TreeHtml.NodeData(
                "leafB", "Promo", "Suite", 5, 2, new Result(ScenarioType.LEAF, null, null, null, List.of(), 0, 0));
        leafA1.setParent(root1);
        mono1.setParent(root1);
        leafB1.setParent(root1);
        root1.children().addAll(List.of(leafA1, mono1, leafB1));
        final TreeHtml.TreeLayout layout1 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root1, leafA1, mono1, leafB1)), List.of());

        // Frame 2: leafA completes target=2 (3-note liquid cadence), monoA completes (crystalline chime), leafB pruned
        final TreeHtml.NodeData root2 = new TreeHtml.NodeData(
                "root", "Checkout", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leafA2 = new TreeHtml.NodeData(
                "leafA", "CardPay", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData mono2 = new TreeHtml.NodeData(
                "monoA", "Receipt", "Suite", 3, 1, new Result(ScenarioType.MONO, null, null, null, List.of(), 1, 0));
        leafA2.setParent(root2);
        mono2.setParent(root2);
        root2.children().addAll(List.of(leafA2, mono2));
        final TreeHtml.TreeLayout layout2 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root2, leafA2, mono2)), List.of());

        // Frame 3: over-execution with raw warning horn + rim tap
        final Result overWarn = new Result(ScenarioType.LEAF, null, null, null, List.of(), 3, 1);
        overWarn.updateStatus(ExecutionStatus.WARNING);
        final TreeHtml.NodeData root3 = new TreeHtml.NodeData(
                "root", "Checkout", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 3, 0));
        final TreeHtml.NodeData leafA3 = new TreeHtml.NodeData("leafA", "CardPay", "Suite", 2, 0, overWarn);
        leafA3.setParent(root3);
        root3.children().add(leafA3);
        final TreeHtml.TreeLayout layout3 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root3, leafA3)), List.of());

        // Frame 4: column contraction + raw failure Reese dive + upset parent + dark verdict outro
        final Result rawFail = new Result(ScenarioType.LEAF, null, null, null, List.of(), 4, 1);
        rawFail.updateStatus(ExecutionStatus.FAILED);
        final TreeHtml.NodeData root4 = new TreeHtml.NodeData(
                "root", "Checkout", "Suite", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 4, 0));
        final TreeHtml.NodeData leafA4 = new TreeHtml.NodeData("leafA", "CardPay", "Suite", 1, 0, rawFail);
        leafA4.setParent(root4);
        root4.children().add(leafA4);
        final TreeHtml.TreeLayout layout4 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(root4, leafA4)), List.of());

        dnb.capture(frame1, layout1, 2);
        dnb.capture(frame2, layout2, 2);
        dnb.capture(frame3, layout3, 2);
        dnb.capture(frame4, layout4, 2);

        assertTrue(DnbSonifier.snapshotFile(frame1).isFile());
        assertTrue(dnb.canSonify(List.of(frame1, frame2, frame3, frame4)));

        final File firstWav = tempDir.resolve("dnb_1.wav").toFile();
        final File secondWav = tempDir.resolve("dnb_2.wav").toFile();
        dnb.writeWav(List.of(frame1, frame2, frame3, frame4), firstWav, 10);
        dnb.writeWav(List.of(frame1, frame2, frame3, frame4), secondWav, 10);

        final byte[] wav = Files.readAllBytes(firstWav.toPath());
        assertEquals("RIFF", new String(wav, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("WAVE", new String(wav, 8, 4, StandardCharsets.US_ASCII));
        final int channels = (wav[22] & 0xFF) | ((wav[23] & 0xFF) << 8);
        assertEquals(2, channels, "DnB must output 2-channel stereo PCM WAV");
        final int tailSamples = (int) (44_100 * 1.85);
        assertEquals(44 + ((4 * 4_410) + tailSamples) * 2 * Short.BYTES, wav.length);
        assertTrue(hasSound(wav));
        assertTrue(hasStereoSeparation(wav), "DnB stereo dub delay, chorus, and panning must produce L/R separation");
        assertArrayEquals(wav, Files.readAllBytes(secondWav.toPath()), "DnB synthesis must be bit-for-bit deterministic");
    }

    @Test
    void dnbDistinguishesCleanWarningAndFailureOutcomesFromSvgFrames(@TempDir final Path tempDir) throws Exception {
        final DnbSonifier dnb = new DnbSonifier();
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
                        + "    <g id=\"scenario_0_Root\" class=\"node-card passed\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Root ▷ (Passed)</title><text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Leaf\" class=\"node-card warning\" transform=\"translate(310, 92)\">\n"
                        + "      <title>Leaf ◼ (Warning)</title><text class=\"tx1\">✓ 1/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <text class=\"tx3\">Leaves: 1/1</text><text class=\"tx3\">Test #1</text>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        Files.writeString(failSvg.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                        + "  <g transform=\"translate(0.00, 0.00) scale(1.0000)\">\n"
                        + "    <g id=\"scenario_0_Root\" class=\"node-card child_failed\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Root ▷ (Upset)</title><text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Leaf\" class=\"node-card failed\" transform=\"translate(310, 92)\">\n"
                        + "      <title>Leaf ◼ (Failed)</title><text class=\"tx1\">1/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <text class=\"tx3\">Leaves: 1/1</text><text class=\"tx3\">Test #1</text>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        final File cleanWav = tempDir.resolve("dnb_clean.wav").toFile();
        final File warnWav = tempDir.resolve("dnb_warn.wav").toFile();
        final File failWav = tempDir.resolve("dnb_fail.wav").toFile();

        assertTrue(dnb.canSonify(List.of(cleanSvg)));
        dnb.writeWav(List.of(cleanSvg), cleanWav, 10);
        dnb.writeWav(List.of(warnSvg), warnWav, 10);
        dnb.writeWav(List.of(failSvg), failWav, 10);

        final byte[] cleanBytes = Files.readAllBytes(cleanWav.toPath());
        final byte[] warnBytes = Files.readAllBytes(warnWav.toPath());
        final byte[] failBytes = Files.readAllBytes(failWav.toPath());

        assertTrue(hasSound(cleanBytes));
        assertTrue(hasSound(warnBytes));
        assertTrue(hasSound(failBytes));
        assertTrue(hasStereoSeparation(cleanBytes));
        assertTrue(!java.util.Arrays.equals(cleanBytes, warnBytes), "Clean and warning verdicts must sound distinct");
        assertTrue(!java.util.Arrays.equals(cleanBytes, failBytes), "Clean and failure verdicts must sound distinct");
        assertTrue(!java.util.Arrays.equals(warnBytes, failBytes), "Warning and failure verdicts must sound distinct");
    }

    @Test
    void carbonSonifiesSvgLineageAndKnownIssuesDirectly(@TempDir final Path tempDir) throws Exception {
        final CarbonSonifier carbon = new CarbonSonifier();
        final File svg1 = tempDir.resolve("frame_0001.svg").toFile();
        final File svg2 = tempDir.resolve("frame_0002.svg").toFile();

        // Two distinct parent branches (Users, Reports) with identically named sibling leaves (VerifyData)
        Files.writeString(svg1.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                        + "  <g transform=\"translate(0.00, 0.00) scale(1.0000)\">\n"
                        + "    <path class=\"edge\" fill=\"none\" d=\"M 260 114 C 285 114, 285 114, 310 114\" stroke=\"#38BDF8\" stroke-width=\"3.0\" />\n"
                        + "    <path class=\"edge\" fill=\"none\" d=\"M 260 234 C 285 234, 285 234, 310 234\" stroke=\"#334155\" stroke-width=\"1.2\" />\n"
                        + "    <g id=\"scenario_0_Users\" class=\"node-card notice\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Users ▷ (Notice)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#38bdf8\" />\n"
                        + "      <text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_VerifyData\" class=\"node-card passed\" transform=\"translate(310, 92)\">\n"
                        + "      <title>VerifyData ◆ (Passed)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#38bdf8\" />\n"
                        + "      <text class=\"tx1\">✓ 1/1</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_2_Reports\" class=\"node-card unvisited\" transform=\"translate(40, 212)\">\n"
                        + "      <title>Reports ▷ (Unvisited)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#64748b\" />\n"
                        + "      <text class=\"tx1\">0 execs</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_3_VerifyData\" class=\"node-card unvisited\" transform=\"translate(310, 212)\">\n"
                        + "      <title>VerifyData ◼ (Unvisited)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#64748b\" />\n"
                        + "      <text class=\"tx1\">0/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <g transform=\"scale(1.0000)\">\n"
                        + "    <text class=\"tx3\">Leafs: 1/2</text>\n"
                        + "    <text class=\"tx3\">Test #1</text>\n"
                        + "  </g>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        Files.writeString(svg2.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                        + "  <g transform=\"translate(12.00, -18.00) scale(1.1500)\">\n"
                        + "    <path class=\"edge\" fill=\"none\" d=\"M 260 114 C 285 114, 285 114, 310 114\" stroke=\"#22C55E\" stroke-width=\"2.2\" />\n"
                        + "    <path class=\"edge\" fill=\"none\" d=\"M 260 234 C 285 234, 285 234, 310 234\" stroke=\"#38BDF8\" stroke-width=\"3.0\" />\n"
                        + "    <g id=\"scenario_0_Users\" class=\"node-card notice\" transform=\"translate(40, 92)\">\n"
                        + "      <title>Users ▷ (Notice)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#06b6d4\" />\n"
                        + "      <text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_VerifyData\" class=\"node-card passed\" transform=\"translate(310, 92)\">\n"
                        + "      <title>VerifyData ◆ (Passed)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#22c55e\" />\n"
                        + "      <text class=\"tx1\">✓ 1/1</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_2_Reports\" class=\"node-card warning known-issue\" transform=\"translate(40, 212)\">\n"
                        + "      <title>Reports ▷ (Warning (Known Issue))</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#fbbf24\" />\n"
                        + "      <text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_3_VerifyData\" class=\"node-card failed known-issue\" transform=\"translate(310, 212)\">\n"
                        + "      <title>VerifyData... (Failed (Known Issue))</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"44\" stroke=\"#38bdf8\" />\n"
                        + "      <text class=\"tx1\">✓ 1/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "  <g transform=\"scale(1.0000)\">\n"
                        + "    <text class=\"tx3\">Leaves: 2/2</text>\n"
                        + "    <text class=\"tx3\">Test #2</text>\n"
                        + "  </g>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        assertTrue(carbon.canSonify(List.of(svg1, svg2)));
        final File wavOut = tempDir.resolve("carbon_svg.wav").toFile();
        carbon.writeWav(List.of(svg1, svg2), wavOut, 10);
        final byte[] wav = Files.readAllBytes(wavOut.toPath());
        assertTrue(hasSound(wav));
        assertTrue(hasStereoSeparation(wav));
    }

    @Test
    void orpheusSnapshotsAndSonifiesNarrativeVoices(@TempDir final Path tempDir) throws Exception {
        final OrpheusSonifier orpheus = new OrpheusSonifier();
        final File frame1 = tempDir.resolve("frame_0001.svg").toFile();
        final File frame2 = tempDir.resolve("frame_0002.svg").toFile();
        final File frame3 = tempDir.resolve("frame_0003.svg").toFile();

        // Frame 1: branch visited once, two leaves (one partial, one unvisited)
        final TreeHtml.NodeData branch1 = new TreeHtml.NodeData(
                "branch", "Branch", "Root", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 1, 0));
        final TreeHtml.NodeData leafA1 = new TreeHtml.NodeData(
                "leafA", "LeafA", "Root", 1, 1, new Result(ScenarioType.LEAF, null, null, null, List.of(), 1, 0));
        final TreeHtml.NodeData leafB1 = new TreeHtml.NodeData(
                "leafB", "LeafB", "Root", 1, 2, new Result(ScenarioType.LEAF, null, null, null, List.of(), 0, 0));
        leafA1.setParent(branch1);
        leafB1.setParent(branch1);
        branch1.children().addAll(List.of(leafA1, leafB1));
        final TreeHtml.TreeLayout layout1 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(branch1, leafA1, leafB1)), List.of());

        // Frame 2: branch visited again, leafA completes target=2, leafB pruned (tree shrinks from 3 -> 2 nodes)
        final TreeHtml.NodeData branch2 = new TreeHtml.NodeData(
                "branch", "Branch", "Root", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leafA2 = new TreeHtml.NodeData(
                "leafA", "LeafA", "Root", 1, 1, new Result(ScenarioType.LEAF, null, null, null, List.of(), 2, 0));
        leafA2.setParent(branch2);
        branch2.children().add(leafA2);
        final TreeHtml.TreeLayout layout2 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(branch2, leafA2)), List.of());

        // Frame 3: leafA over-executes (3/2) with warning status
        final Result overWarnResult = new Result(ScenarioType.LEAF, null, null, null, List.of(), 3, 1);
        overWarnResult.updateStatus(ExecutionStatus.WARNING);
        final TreeHtml.NodeData branch3 = new TreeHtml.NodeData(
                "branch", "Branch", "Root", 0, 1, new Result(ScenarioType.NODE, null, null, null, List.of(), 3, 0));
        final TreeHtml.NodeData leafA3 = new TreeHtml.NodeData(
                "leafA", "LeafA", "Root", 1, 1, overWarnResult);
        leafA3.setParent(branch3);
        branch3.children().add(leafA3);
        final TreeHtml.TreeLayout layout3 = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(branch3, leafA3)), List.of());

        orpheus.capture(frame1, layout1, 2);
        orpheus.capture(frame2, layout2, 2);
        orpheus.capture(frame3, layout3, 2);

        assertTrue(OrpheusSonifier.snapshotFile(frame1).isFile());
        assertTrue(orpheus.canSonify(List.of(frame1, frame2, frame3)));

        final File firstWav = tempDir.resolve("orpheus_1.wav").toFile();
        final File secondWav = tempDir.resolve("orpheus_2.wav").toFile();
        orpheus.writeWav(List.of(frame1, frame2, frame3), firstWav, 10);
        orpheus.writeWav(List.of(frame1, frame2, frame3), secondWav, 10);

        final byte[] wav = Files.readAllBytes(firstWav.toPath());
        assertEquals(44 + ((3 * 4_410) + 17_640) * Short.BYTES, wav.length);
        assertTrue(hasSound(wav));
        assertArrayEquals(wav, Files.readAllBytes(secondWav.toPath()), "Orpheus synthesis must be bit-for-bit deterministic");
    }

    @Test
    void orpheusSonifiesSvgFramesDirectlyWhenSnapshotsAreAbsent(@TempDir final Path tempDir) throws Exception {
        final OrpheusSonifier orpheus = new OrpheusSonifier();
        final File svg1 = tempDir.resolve("frame_0001.svg").toFile();
        final File svg2 = tempDir.resolve("frame_0002.svg").toFile();

        Files.writeString(svg1.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1920\" height=\"1080\">\n"
                        + "  <g transform=\"translate(40.00, 30.00) scale(1.2000)\">\n"
                        + "    <g id=\"scenario_0_SignIn\" class=\"node-card passed\" transform=\"translate(40, 92)\">\n"
                        + "      <title>SignIn ▷ (Passed)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"46\" stroke=\"#38bdf8\" />\n"
                        + "      <text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Action\" class=\"node-card unvisited\" transform=\"translate(310, 92)\">\n"
                        + "      <title>Action ◼ (Unvisited)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"46\" stroke=\"#334155\" />\n"
                        + "      <text class=\"tx1\">0/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        Files.writeString(svg2.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1920\" height=\"1080\">\n"
                        + "  <g transform=\"translate(10.00, -120.00) scale(2.4000)\">\n"
                        + "    <g id=\"scenario_0_SignIn\" class=\"node-card passed\" transform=\"translate(40, 92)\">\n"
                        + "      <title>SignIn ▷ (Passed)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"46\" stroke=\"#22c55e\" />\n"
                        + "      <text class=\"tx1\">1 exec</text>\n"
                        + "    </g>\n"
                        + "    <g id=\"scenario_1_Action\" class=\"node-card passed\" transform=\"translate(310, 92)\">\n"
                        + "      <title>Action ◼ (Passed)</title>\n"
                        + "      <rect class=\"card-bg\" width=\"220\" height=\"46\" stroke=\"#38bdf8\" />\n"
                        + "      <text class=\"tx1\">✓ 1/1</text>\n"
                        + "    </g>\n"
                        + "  </g>\n"
                        + "</svg>\n",
                StandardCharsets.UTF_8);

        assertTrue(orpheus.canSonify(List.of(svg1, svg2)));
        final File wavOut = tempDir.resolve("svg_fallback.wav").toFile();
        orpheus.writeWav(List.of(svg1, svg2), wavOut, 10);
        assertTrue(hasSound(Files.readAllBytes(wavOut.toPath())));
    }

    @Test
    void geminiSnapshotsAndSonifiesTreeStructureChangesDeterministically(@TempDir final Path tempDir) throws Exception {
        final GeminiSonifier gemini = new GeminiSonifier();
        final File frame1 = tempDir.resolve("frame_0001.png").toFile();
        final File frame2 = tempDir.resolve("frame_0002.png").toFile();
        final File frame3 = tempDir.resolve("frame_0003.png").toFile();
        
        final TreeHtml.NodeData branch = new TreeHtml.NodeData(
                "branch", "Branch", "Root", 0, 0, new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leaf = new TreeHtml.NodeData(
                "leaf", "Leaf", "Root", 1, 1, new Result(ScenarioType.LEAF, null, null, null, List.of(), 0, 0));
        final TreeHtml.NodeData leaf2 = new TreeHtml.NodeData(
                "leaf2", "Leaf2", "Root", 1, 2, new Result(ScenarioType.LEAF, null, null, null, List.of(), 0, 0));
        leaf.setParent(branch);
        leaf2.setParent(branch);
        branch.children().addAll(List.of(leaf, leaf2));
        final TreeHtml.TreeLayout layout1 = new TreeHtml.TreeLayout(new ArrayList<>(List.of(branch, leaf, leaf2)), List.of());
        
        final TreeHtml.NodeData leafCompleted = new TreeHtml.NodeData(
                "leaf", "Leaf", "Root", 1, 1, new Result(ScenarioType.LEAF, null, null, null, List.of(), 9, 0));
        leafCompleted.setParent(branch);
        branch.children().set(0, leafCompleted);
        final TreeHtml.TreeLayout layout2 = new TreeHtml.TreeLayout(new ArrayList<>(List.of(branch, leafCompleted, leaf2)), List.of());

        // Frame 3: leaf2 pruned (tree shrinks, triggering swoosh)
        final TreeHtml.TreeLayout layout3 = new TreeHtml.TreeLayout(new ArrayList<>(List.of(branch, leafCompleted)), List.of());
        
        gemini.capture(frame1, layout1, 9);
        gemini.capture(frame2, layout2, 9);
        gemini.capture(frame3, layout3, 9);
        
        final String snapshot1 = Files.readString(GeminiSonifier.snapshotFile(frame1).toPath());
        final String snapshot2 = Files.readString(GeminiSonifier.snapshotFile(frame2).toPath());
        
        assertTrue(snapshot1.contains("1\t2\t0\t0")); // col 1 has 2 total, 0 active, 0 completed
        assertTrue(snapshot2.contains("1\t2\t0\t1")); // col 1 has 2 total, 0 active, 1 completed
        
        Files.createFile(frame1.toPath());
        Files.createFile(frame2.toPath());
        Files.createFile(frame3.toPath());
        assertTrue(gemini.canSonify(List.of(frame1, frame2, frame3)));
        
        final File wavFile1 = tempDir.resolve("gemini1.wav").toFile();
        final File wavFile2 = tempDir.resolve("gemini2.wav").toFile();
        gemini.writeWav(List.of(frame1, frame2, frame3), wavFile1, 10);
        gemini.writeWav(List.of(frame1, frame2, frame3), wavFile2, 10);
        
        final byte[] wav = Files.readAllBytes(wavFile1.toPath());
        assertTrue(hasSound(wav));
        assertArrayEquals(wav, Files.readAllBytes(wavFile2.toPath()), "Gemini swoosh and chime synthesis must be deterministic");
    }

    @Test
    void claudeSonifiesImageAndSvgChanges(@TempDir final Path tempDir) throws Exception {
        final File frame1 = image(tempDir, 1, 0x0F172A);
        final File frame2 = image(tempDir, 2, 0x22C55E);
        final File first = tempDir.resolve("first.wav").toFile();
        final File second = tempDir.resolve("second.wav").toFile();
        final TreeSonifier claude = TreeProgressionAudio.find("claude").orElseThrow();

        assertTrue(claude.canSonify(List.of(frame1, frame2)));
        claude.writeWav(List.of(frame1, frame2), first, 10);
        claude.writeWav(List.of(frame1, frame2), second, 10);

        final byte[] wav = Files.readAllBytes(first.toPath());
        assertEquals(44 + ((2 * 4_410) + 17_640) * Short.BYTES, wav.length);
        assertTrue(hasSound(wav));
        assertArrayEquals(wav, Files.readAllBytes(second.toPath()));

        final File silent = tempDir.resolve("silent.wav").toFile();
        claude.writeWav(List.of(frame1, frame1), silent, 10);
        assertTrue(!hasSound(Files.readAllBytes(silent.toPath())));

        // Verify Claude also renders SVG progression frames without failing
        final File svg1 = tempDir.resolve("frame_0001.svg").toFile();
        final File svg2 = tempDir.resolve("frame_0002.svg").toFile();
        Files.writeString(svg1.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"120\" viewBox=\"0 0 200 120\">"
                        + "<rect width=\"200\" height=\"120\" fill=\"#0F172A\"/></svg>",
                StandardCharsets.UTF_8);
        Files.writeString(svg2.toPath(),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"120\" viewBox=\"0 0 200 120\">"
                        + "<rect width=\"200\" height=\"120\" fill=\"#22C55E\"/></svg>",
                StandardCharsets.UTF_8);
        final File svgWav = tempDir.resolve("claude_svg.wav").toFile();
        claude.writeWav(List.of(svg1, svg2), svgWav, 10);
        assertTrue(hasSound(Files.readAllBytes(svgWav.toPath())));
    }

    @Test
    void auroraSnapshotsAndSonifiesTreeStructureChangesDeterministically(@TempDir final Path tempDir) throws Exception {
        final AuroraSonifier aurora = new AuroraSonifier();
        final File frame1 = tempDir.resolve("frame_0001.png").toFile();
        final File frame2 = tempDir.resolve("frame_0002.png").toFile();
        
        final TreeHtml.NodeData branch = new TreeHtml.NodeData(
                "branch", "Branch", "Root", 0, 0, new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leaf = new TreeHtml.NodeData(
                "leaf", "Leaf", "Root", 1, 1, new Result(ScenarioType.LEAF, null, null, null, List.of(), 0, 0));
        leaf.setParent(branch);
        branch.children().add(leaf);
        final TreeHtml.TreeLayout layout1 = new TreeHtml.TreeLayout(new ArrayList<>(List.of(branch, leaf)), List.of());
        
        aurora.capture(frame1, layout1, 2);
        aurora.capture(frame2, layout1, 2);
        
        Files.createFile(frame1.toPath());
        Files.createFile(frame2.toPath());
        assertTrue(aurora.canSonify(List.of(frame1, frame2)));
        
        final File wavFile = tempDir.resolve("aurora.wav").toFile();
        aurora.writeWav(List.of(frame1, frame2), wavFile, 10);
        assertTrue(hasSound(Files.readAllBytes(wavFile.toPath())));
    }

    private static File image(final Path directory, final int index, final int rgb) throws Exception {
        final java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                150, 120, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 150; x++) {
            for (int y = 0; y < 120; y++) {
                image.setRGB(x, y, rgb);
            }
        }
        final File file = directory.resolve(String.format("img_%04d.png", index)).toFile();
        javax.imageio.ImageIO.write(image, "png", file);
        return file;
    }

    @Test
    void snapshotsTerminalLeafStateFromRenderedTree(@TempDir final Path tempDir) throws Exception {
        final File frame = tempDir.resolve("frame_0001.png").toFile();
        final TreeHtml.NodeData branch = new TreeHtml.NodeData(
                "branch", "Branch", "Root", 0, 0, new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leaf = new TreeHtml.NodeData(
                "leaf", "Leaf", "Root", 1, 1, new Result(ScenarioType.LEAF, null, null, null, List.of(), 3, 0));
        leaf.setParent(branch);
        branch.children().add(leaf);
        final TreeHtml.TreeLayout layout = new TreeHtml.TreeLayout(
                new ArrayList<>(List.of(branch, leaf)), List.of());

        sonifier.capture(frame, layout, 9);

        final String snapshot = Files.readString(CopilotSonifier.eventsFile(frame).toPath());
        final String encodedIdentity = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "Root/Branch/Leaf".getBytes(StandardCharsets.UTF_8));
        assertEquals(encodedIdentity + "\t3\t9" + System.lineSeparator(), snapshot);
    }

    @Test
    void writesDeterministicWavFromPositiveLeafCountDeltas(@TempDir final Path tempDir) throws Exception {
        final File frame1 = frame(tempDir, 1,
                "root/branch/leaf-a", 1, 2,
                "root/branch/leaf-b", 0, 2);
        final File frame2 = frame(tempDir, 2,
                "root/branch/leaf-a", 2, 2,
                "root/branch/leaf-b", 1, 2);
        final File firstOutput = tempDir.resolve("first.wav").toFile();
        final File secondOutput = tempDir.resolve("second.wav").toFile();

        sonifier.writeWav(List.of(frame1, frame2), firstOutput, 10);
        sonifier.writeWav(List.of(frame1, frame2), secondOutput, 10);

        final byte[] wav = Files.readAllBytes(firstOutput.toPath());
        assertEquals("RIFF", new String(wav, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("WAVE", new String(wav, 8, 4, StandardCharsets.US_ASCII));
        assertEquals(44 + ((2 * 4_410) + 17_640) * Short.BYTES, wav.length);
        assertTrue(hasSound(wav), "Positive execution deltas should produce audible PCM samples");
        assertTrue(maxAmplitude(wav, 44 + (600 * Short.BYTES), 44 + (900 * Short.BYTES))
                        > maxAmplitude(wav, 44 + (16_000 * Short.BYTES), 44 + (16_500 * Short.BYTES)),
                "The note should fade as it approaches the end of its 400 ms duration");
        assertArrayEquals(wav, Files.readAllBytes(secondOutput.toPath()));
    }

    @Test
    void rejectsMissingSnapshotsAndInvalidFrameRate(@TempDir final Path tempDir) throws Exception {
        final File frame = tempDir.resolve("frame_0001.png").toFile();
        Files.createFile(frame.toPath());
        assertTrue(!sonifier.canSonify(List.of(frame)));
        Files.createDirectories(CopilotSonifier.eventsFile(frame).toPath().getParent());
        Files.writeString(CopilotSonifier.eventsFile(frame).toPath(), "");

        assertTrue(sonifier.canSonify(List.of(frame)));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> sonifier.writeWav(List.of(frame), tempDir.resolve("invalid.wav").toFile(), 0));
    }

    private static File frame(
            final Path directory,
            final int index,
            final String firstIdentity,
            final int firstExecutions,
            final int target,
            final String secondIdentity,
            final int secondExecutions,
            final int secondTarget) throws Exception {
        final File frame = directory.resolve(String.format("frame_%04d.png", index)).toFile();
        Files.createFile(frame.toPath());
        Files.createDirectories(CopilotSonifier.eventsFile(frame).toPath().getParent());
        Files.writeString(
                CopilotSonifier.eventsFile(frame).toPath(),
                event(firstIdentity, firstExecutions, target) + event(secondIdentity, secondExecutions, secondTarget),
                StandardCharsets.UTF_8);
        return frame;
    }

    private static String event(final String identity, final int executions, final int target) {

        return Base64.getUrlEncoder().withoutPadding().encodeToString(identity.getBytes(StandardCharsets.UTF_8))
                + "\t" + executions + "\t" + target + System.lineSeparator();
    }

    private static boolean hasSound(final byte[] wav) {

        for (int i = 44; i < wav.length; i++) {
            if (0 != wav[i]) return true;
        }
        return false;
    }

    private static boolean hasStereoSeparation(final byte[] wav) {

        for (int index = 44; index + 3 < wav.length; index += 4) {
            final int left = (short) ((wav[index] & 0xFF) | (wav[index + 1] << 8));
            final int right = (short) ((wav[index + 2] & 0xFF) | (wav[index + 3] << 8));
            if (Math.abs(left - right) > 250) {
                return true;
            }
        }
        return false;
    }

    private static int maxAmplitude(final byte[] wav, final int start, final int end) {

        int max = 0;
        for (int index = start; index + 1 < Math.min(end, wav.length); index += Short.BYTES) {
            final int sample = (short) ((wav[index] & 0xFF) | (wav[index + 1] << 8));
            max = Math.max(max, Math.abs(sample));
        }
        return max;
    }
}
