package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostProcessorTest {

    @Test
    void serializesNumericPropertiesInDetachedCommand() {
        final String[] command = PostProcessor.detachedCommand(
                "java",
                "classpath",
                123,
                "run.log",
                "run.html",
                false);

        assertDoesNotThrow(() -> Integer.parseInt(command[10]));
        assertDoesNotThrow(() -> Integer.parseInt(command[11]));
        assertDoesNotThrow(() -> Integer.parseInt(command[12]));
    }

    @Test
    void serializesProgressWindowAndBrowserFlagsIndependently() {
        final String[] command = PostProcessor.detachedCommand(
                "java",
                "classpath",
                123,
                "run.log",
                "run.html",
                true,
                false);

        assertEquals("true", command[7]);
        assertEquals("false", command[16]);
    }

    @Test
    void storesPostProcessorLogBesideRunLog(@TempDir final Path tempDir) {
        assertEquals(
                tempDir.resolve("run-6.post-processing.log").toFile(),
                PostProcessor.postProcessorLogFile(tempDir.resolve("run-6.run.log").toFile()));
    }

    @Test
    void generatesProgressionMediaAndFinalReport(@TempDir final Path tempDir) throws IOException {
        final String prefix = "run-1";
        final Path runDir = Files.createDirectory(tempDir.resolve(prefix));
        writeFrame(runDir.resolve("progression/frame_0001.png"));
        writeFrame(runDir.resolve("tree_progression/frame_0001.png"));
        final Path logFile = runDir.resolve(prefix + ".run.log");
        Files.writeString(logFile, "");
        final Path htmlFile = runDir.resolve(prefix + ".run.log.html");

        PostProcessor.process(logFile.toFile(), htmlFile.toFile(), false);

        assertTrue(Files.exists(htmlFile));
        assertTrue(Files.exists(runDir.resolve(prefix + ".progression.gif")));
        assertTrue(Files.exists(runDir.resolve(prefix + ".progression.webp")));
        assertTrue(Files.exists(runDir.resolve(prefix + ".tree.progression.gif")));
        assertTrue(Files.exists(runDir.resolve(prefix + ".tree.progression.webp")));
    }

    @Test
    void canDisableEachProgressionTypeIndependently(@TempDir final Path tempDir) throws IOException {
        final String prefix = "run-2";
        final Path runDir = Files.createDirectory(tempDir.resolve(prefix));
        writeFrame(runDir.resolve("progression/frame_0001.png"));
        writeFrame(runDir.resolve("tree_progression/frame_0001.png"));
        final Path logFile = runDir.resolve(prefix + ".run.log");
        Files.writeString(logFile, "");
        final Path htmlFile = runDir.resolve(prefix + ".run.log.html");

        PostProcessor.process(logFile.toFile(), htmlFile.toFile(), false, false, true);

        assertTrue(Files.notExists(runDir.resolve(prefix + ".progression.gif")));
        assertTrue(Files.notExists(runDir.resolve(prefix + ".progression.webp")));
        assertTrue(Files.exists(runDir.resolve(prefix + ".tree.progression.gif")));
        assertTrue(Files.exists(runDir.resolve(prefix + ".tree.progression.webp")));
        String html = Files.readString(htmlFile);
        assertFalse(html.contains("Scenario Coverage Progression Animation"));
        assertTrue(html.contains("Scenario Tree Progression Animation"));

        final String secondPrefix = "run-3";
        final Path secondRunDir = Files.createDirectory(tempDir.resolve(secondPrefix));
        writeFrame(secondRunDir.resolve("progression/frame_0001.png"));
        writeFrame(secondRunDir.resolve("tree_progression/frame_0001.png"));
        final Path secondLogFile = secondRunDir.resolve(secondPrefix + ".run.log");
        Files.writeString(secondLogFile, "");
        final Path secondHtmlFile = secondRunDir.resolve(secondPrefix + ".run.log.html");

        PostProcessor.process(secondLogFile.toFile(), secondHtmlFile.toFile(), false, true, false);

        assertTrue(Files.exists(secondRunDir.resolve(secondPrefix + ".progression.gif")));
        assertTrue(Files.exists(secondRunDir.resolve(secondPrefix + ".progression.webp")));
        assertTrue(Files.notExists(secondRunDir.resolve(secondPrefix + ".tree.progression.gif")));
        assertTrue(Files.notExists(secondRunDir.resolve(secondPrefix + ".tree.progression.webp")));
        html = Files.readString(secondHtmlFile);
        assertTrue(html.contains("Scenario Coverage Progression Animation"));
        assertFalse(html.contains("Scenario Tree Progression Animation"));
    }

    @Test
    void samplesAnimationFramesToConfiguredMaximum(@TempDir final Path tempDir) throws IOException {
        final String prefix = "run-4";
        final Path runDir = Files.createDirectory(tempDir.resolve(prefix));
        for (int i = 1; i <= 7; i++) {
            writeFrame(runDir.resolve("progression/frame_%04d.png".formatted(i)));
        }
        final Path logFile = runDir.resolve(prefix + ".run.log");
        Files.writeString(logFile, "");

        PostProcessor.process(
                logFile.toFile(),
                runDir.resolve(prefix + ".run.log.html").toFile(),
                false,
                true,
                false,
                3,
                16);

        assertEquals(3, AnimatedWebPTest.countFrames(runDir.resolve(prefix + ".progression.webp").toFile()));
    }

    @Test
    void includesMp4LinksWhenTheirOutputsExist(@TempDir final Path tempDir) throws IOException {
        final Path logFile = tempDir.resolve("1234567890123.run.log");
        final Path htmlFile = tempDir.resolve("run.html");
        Files.writeString(logFile, "");
        Files.writeString(tempDir.resolve("1234567890123.progression.mp4"), "mp4");
        Files.writeString(tempDir.resolve("1234567890123.tree.progression.mp4"), "mp4");

        LogHtml.generateReport(logFile.toFile(), htmlFile.toFile(), true, true, true, true);

        final String html = Files.readString(htmlFile);
        assertTrue(html.contains("1234567890123.progression.mp4"));
        assertTrue(html.contains("1234567890123.tree.progression.mp4"));
    }

    @Test
    void createsOnlyEnabledMp4ArtifactAndAddsItsReportLink(@TempDir final Path tempDir) throws IOException {
        final String prefix = "run-5";
        final Path runDir = Files.createDirectory(tempDir.resolve(prefix));
        writeFrame(runDir.resolve("progression/frame_0001.png"));
        writeFrame(runDir.resolve("tree_progression/frame_0001.png"));
        final Path logFile = runDir.resolve(prefix + ".run.log");
        final Path htmlFile = runDir.resolve(prefix + ".run.log.html");
        Files.writeString(logFile, "");
        final Path encoder = tempDir.resolve("fake-ffmpeg");
        Files.writeString(encoder, "#!/bin/sh\nfor output do :; done\ncat >/dev/null\nprintf mp4 > \"$output\"\n");
        assertTrue(encoder.toFile().setExecutable(true));

        PostProcessor.process(
                logFile.toFile(),
                htmlFile.toFile(),
                false,
                true,
                true,
                5,
                16,
                100,
                true,
                false,
                encoder.toString());

        assertTrue(Files.exists(runDir.resolve(prefix + ".progression.mp4")));
        assertTrue(Files.notExists(runDir.resolve(prefix + ".tree.progression.mp4")));
        assertTrue(Files.readString(htmlFile).contains(prefix + ".progression.mp4"));
    }

    private static void writeFrame(final Path path) throws IOException {
        Files.createDirectories(path.getParent());
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", path.toFile());
    }
}
