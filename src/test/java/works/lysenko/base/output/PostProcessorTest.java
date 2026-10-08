package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

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
        assertEquals(ProgressionSettings.current().treeSonification(), command[17]);
        assertEquals(String.valueOf(ProgressionSettings.current().scenarioGifEnabled()), command[18]);
        assertEquals(String.valueOf(ProgressionSettings.current().scenarioWebpEnabled()), command[19]);
        assertEquals(String.valueOf(ProgressionSettings.current().treeGifEnabled()), command[20]);
        assertEquals(String.valueOf(ProgressionSettings.current().treeWebpEnabled()), command[21]);
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
        assertTrue(Files.notExists(runDir.resolve(prefix + ".progression.gif")));
        assertTrue(Files.exists(runDir.resolve(prefix + ".progression.webp")));
        assertTrue(Files.notExists(runDir.resolve(prefix + ".tree.progression.gif")));
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
        assertTrue(Files.notExists(runDir.resolve(prefix + ".tree.progression.gif")));
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

        assertTrue(Files.notExists(secondRunDir.resolve(secondPrefix + ".progression.gif")));
        assertTrue(Files.exists(secondRunDir.resolve(secondPrefix + ".progression.webp")));
        assertTrue(Files.notExists(secondRunDir.resolve(secondPrefix + ".tree.progression.gif")));
        assertTrue(Files.notExists(secondRunDir.resolve(secondPrefix + ".tree.progression.webp")));
        html = Files.readString(secondHtmlFile);
        assertTrue(html.contains("Scenario Coverage Progression Animation"));
        assertFalse(html.contains("Scenario Tree Progression Animation"));
    }

    @Test
    void canDisableWebpProgressionIndependently(@TempDir final Path tempDir) throws IOException {
        final String prefix = "run-webp";
        final Path runDir = Files.createDirectory(tempDir.resolve(prefix));
        writeFrame(runDir.resolve("progression/frame_0001.png"));
        writeFrame(runDir.resolve("tree_progression/frame_0001.png"));
        final Path logFile = runDir.resolve(prefix + ".run.log");
        Files.writeString(logFile, "");
        final Path htmlFile = runDir.resolve(prefix + ".run.log.html");

        PostProcessor.process(
                logFile.toFile(),
                htmlFile.toFile(),
                false,
                true,
                false,
                false,
                true,
                false,
                false);

        assertTrue(Files.exists(htmlFile));
        assertTrue(Files.notExists(runDir.resolve(prefix + ".progression.webp")));
        assertTrue(Files.notExists(runDir.resolve(prefix + ".tree.progression.webp")));
        final String html = Files.readString(htmlFile);
        assertFalse(html.contains("Progression WebP"));
        assertFalse(html.contains("Tree WebP"));
    }

    @Test
    void mainPassesWebpDisabledFlags(@TempDir final Path tempDir) throws IOException {
        final String prefix = "run-main-webp";
        final Path runDir = Files.createDirectory(tempDir.resolve(prefix));
        writeFrame(runDir.resolve("progression/frame_0001.png"));
        writeFrame(runDir.resolve("tree_progression/frame_0001.png"));
        final Path logFile = runDir.resolve(prefix + ".run.log");
        Files.writeString(logFile, "");
        final Path htmlFile = runDir.resolve(prefix + ".run.log.html");

        final String[] args = new String[]{
                "999999999",
                logFile.toString(),
                htmlFile.toString(),
                "false",
                "true",
                "true",
                "5",
                "16",
                "100",
                "false",
                "false",
                "ffmpeg",
                "false",
                "none",
                "false",
                "false",
                "false",
                "false"
        };

        PostProcessor.main(args);

        assertTrue(Files.exists(htmlFile));
        assertTrue(Files.notExists(runDir.resolve(prefix + ".progression.webp")));
        assertTrue(Files.notExists(runDir.resolve(prefix + ".tree.progression.webp")));
    }

    @Test
    void includesOnlyConfiguredMediaTasks() {
        final List<String> allTasks = PostProcessor.enabledTasks(
                true, true, true, true,
                true, true, true, true);
        assertEquals(List.of(
                "Run timeline report",
                "Scenario progression GIF",
                "Scenario progression WebP",
                "Scenario progression MP4",
                "Tree progression GIF",
                "Tree progression WebP",
                "Tree progression MP4"), allTasks);

        final List<String> defaultTasks = PostProcessor.enabledTasks(
                true, false, true, true,
                true, false, true, true);
        assertEquals(List.of(
                "Run timeline report",
                "Scenario progression WebP",
                "Scenario progression MP4",
                "Tree progression WebP",
                "Tree progression MP4"), defaultTasks);

        final List<String> noScenarioTasks = PostProcessor.enabledTasks(
                false, true, true, true,
                true, false, true, true);
        assertEquals(List.of(
                "Run timeline report",
                "Tree progression WebP",
                "Tree progression MP4"), noScenarioTasks);

        final List<String> onlyReport = PostProcessor.enabledTasks(
                false, false, false, false,
                false, false, false, false);
        assertEquals(List.of("Run timeline report"), onlyReport);
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

    @Test
    void passesSelectedSonificationModeThroughDetachedProcessing(@TempDir final Path tempDir) throws IOException {
        final String prefix = "run-audio";
        final Path runDir = Files.createDirectory(tempDir.resolve(prefix));
        writeFrame(runDir.resolve("tree_progression/frame_0001.png"), Color.BLACK);
        writeFrame(runDir.resolve("tree_progression/frame_0002.png"), Color.GREEN);
        final Path logFile = runDir.resolve(prefix + ".run.log");
        final Path htmlFile = runDir.resolve(prefix + ".run.log.html");
        Files.writeString(logFile, "");
        final Path encoder = tempDir.resolve("fake-ffmpeg-with-audio");
        Files.writeString(
                encoder,
                "#!/bin/sh\nfor output do :; done\nprintf '%s\\n' \"$@\" > \"$output\"\ncat >/dev/null\n");
        assertTrue(encoder.toFile().setExecutable(true));

        PostProcessor.process(
                logFile.toFile(),
                htmlFile.toFile(),
                false,
                false,
                true,
                5,
                16,
                100,
                false,
                true,
                encoder.toString(),
                false,
                ClaudeSonifier.MODE);

        final String ffmpegArguments = Files.readString(runDir.resolve(prefix + ".tree.progression.mp4"));
        assertTrue(ffmpegArguments.contains("-map\n1:a:0"));
        assertTrue(ffmpegArguments.contains("tree-progression-audio-"));
    }

    @Test
    void reportsProgressDuringReportGeneration(@TempDir final Path tempDir) throws IOException {
        final Path logFile = tempDir.resolve("test.run.log");
        final Path htmlFile = tempDir.resolve("test.html");
        final StringBuilder logContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            logContent.append("[ 1 ][ 100 ][2026-01-01 12:00:00.000] Log line ").append(i).append("\n");
        }
        Files.writeString(logFile, logContent.toString());

        final java.util.List<Integer> updates = new java.util.ArrayList<>();
        final boolean[] completed = new boolean[1];

        final ProcessingProgress progress = new ProcessingProgress() {
            @Override
            public void update(final String task, final int percentage) {
                if ("Run timeline report".equals(task)) {
                    updates.add(percentage);
                }
            }

            @Override
            public void complete(final String task) {
                if ("Run timeline report".equals(task)) {
                    completed[0] = true;
                }
            }
        };

        LogHtml.generateReport(
                logFile.toFile(),
                htmlFile.toFile(),
                true,
                true,
                true,
                true,
                progress);

        assertTrue(Files.exists(htmlFile));
        assertTrue(completed[0]);
        assertFalse(updates.isEmpty());
        assertTrue(updates.stream().anyMatch(pct -> pct > 0 && pct < 100));
    }

    private static void writeFrame(final Path path) throws IOException {
        writeFrame(path, Color.BLACK);
    }

    private static void writeFrame(final Path path, final Color color) throws IOException {
        Files.createDirectories(path.getParent());
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        ImageIO.write(image, "png", path.toFile());
    }
}
