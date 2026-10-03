package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnimatedWebPTest {

    @Test
    void writesAnimatedWebpWithFrameDurations(@TempDir final Path tempDir) throws IOException {
        final List<BufferedImage> frames = List.of(frame(Color.RED), frame(Color.BLUE));
        final File output = tempDir.resolve("animation.webp").toFile();

        AnimatedWebP.write(frames, output, 100, 1500);

        final byte[] data = Files.readAllBytes(output.toPath());
        assertEquals("RIFF", fourCc(data, 0));
        assertEquals("WEBP", fourCc(data, 8));
        assertEquals(data.length - 8, read32(data, 4));
        assertEquals(2, countFrames(output));
        assertEquals(100, frameDuration(data, 0));
        assertEquals(1500, frameDuration(data, 1));
    }

    @Test
    void rejectsFramesWithDifferentDimensions(@TempDir final Path tempDir) {
        final List<BufferedImage> frames = List.of(frame(Color.RED), new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB));

        assertThrows(
                IllegalArgumentException.class,
                () -> AnimatedWebP.write(frames, tempDir.resolve("invalid.webp").toFile(), 100, 1500));
    }

    static int countFrames(final File file) throws IOException {
        final byte[] data = Files.readAllBytes(file.toPath());
        int count = 0;
        for (int offset = 12; offset + 8 <= data.length;) {
            final long size = read32(data, offset + 4);
            if ("ANMF".equals(fourCc(data, offset))) count++;
            offset += (int) (8 + size + (size & 1));
        }
        return count;
    }

    private static int frameDuration(final byte[] data, final int frameNumber) {
        int count = 0;
        for (int offset = 12; offset + 8 <= data.length;) {
            final long size = read32(data, offset + 4);
            if ("ANMF".equals(fourCc(data, offset))) {
                if (count++ == frameNumber) return (int) read24(data, offset + 8 + 12);
            }
            offset += (int) (8 + size + (size & 1));
        }
        throw new AssertionError("Missing animation frame " + frameNumber);
    }

    private static BufferedImage frame(final Color color) {
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, color.getRGB());
        }
        return image;
    }

    private static String fourCc(final byte[] data, final int offset) {
        return new String(data, offset, 4, java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static long read32(final byte[] data, final int offset) {
        return (data[offset] & 0xFFL)
                | ((data[offset + 1] & 0xFFL) << 8)
                | ((data[offset + 2] & 0xFFL) << 16)
                | ((data[offset + 3] & 0xFFL) << 24);
    }

    private static long read24(final byte[] data, final int offset) {
        return (data[offset] & 0xFFL)
                | ((data[offset + 1] & 0xFFL) << 8)
                | ((data[offset + 2] & 0xFFL) << 16);
    }
}
