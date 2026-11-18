package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionMp4Test {

    @Test
    void streamsFramesToConfiguredEncoder(@TempDir final Path tempDir) throws Exception {
        final Path framePath = tempDir.resolve("frame_0001.png");
        ImageIO.write(new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB), "png", framePath.toFile());
        final Path encoder = tempDir.resolve("fake-ffmpeg");
        Files.writeString(
                encoder,
                "#!/bin/sh\nfor output do :; done\ncat >/dev/null\nprintf mp4 > \"$output\"\n");
        assertTrue(encoder.toFile().setExecutable(true));
        final File output = tempDir.resolve("output.mp4").toFile();
        final AtomicInteger progress = new AtomicInteger();

        ProgressionMp4.write(List.of(framePath.toFile()), output, encoder.toString(), 10, 16, progress::set);

        assertEquals(100, progress.get());
        assertTrue(output.isFile());
        assertEquals("mp4", Files.readString(output.toPath()));
    }

    @Test
    void mapsAndMuxesProvidedAudio(@TempDir final Path tempDir) throws Exception {
        final Path framePath = tempDir.resolve("frame_0001.png");
        ImageIO.write(new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB), "png", framePath.toFile());
        final Path audioPath = tempDir.resolve("audio.wav");
        Files.write(audioPath, new byte[]{1, 2, 3});
        final Path argumentsPath = tempDir.resolve("arguments.txt");
        final Path encoder = tempDir.resolve("fake-ffmpeg");
        Files.writeString(
                encoder,
                "#!/bin/sh\nprintf '%s\\n' \"$@\" > \"" + argumentsPath + "\"\n"
                        + "for output do :; done\ncat >/dev/null\nprintf mp4 > \"$output\"\n");
        assertTrue(encoder.toFile().setExecutable(true));

        ProgressionMp4.write(
                List.of(framePath.toFile()),
                tempDir.resolve("output.mp4").toFile(),
                encoder.toString(),
                10,
                16,
                audioPath.toFile(),
                percentage -> { });

        final String arguments = Files.readString(argumentsPath);
        assertTrue(arguments.contains(audioPath.toString()));
        assertTrue(arguments.contains("-map"));
        assertTrue(arguments.contains("1:a:0"));
        assertTrue(arguments.contains("-c:a"));
    }

    @Test
    void rejectsOddDimensionsBeforeLaunchingEncoder(@TempDir final Path tempDir) throws IOException {
        final Path framePath = tempDir.resolve("odd.png");
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "png", framePath.toFile());

        assertThrows(
                IOException.class,
                () -> ProgressionMp4.write(
                        List.of(framePath.toFile()),
                        tempDir.resolve("output.mp4").toFile(),
                        "missing-ffmpeg",
                        10,
                        16,
                        percentage -> { }));
    }
}
