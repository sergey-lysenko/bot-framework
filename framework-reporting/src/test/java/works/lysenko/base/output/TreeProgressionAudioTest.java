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
        assertTrue(TreeProgressionAudio.find("none").isEmpty());
        assertTrue(TreeProgressionAudio.find("true").isEmpty());
        assertTrue(TreeProgressionAudio.find(null).isEmpty());
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

    private static int maxAmplitude(final byte[] wav, final int start, final int end) {

        int max = 0;
        for (int index = start; index + 1 < Math.min(end, wav.length); index += Short.BYTES) {
            final int sample = (short) ((wav[index] & 0xFF) | (wav[index + 1] << 8));
            max = Math.max(max, Math.abs(sample));
        }
        return max;
    }
}
