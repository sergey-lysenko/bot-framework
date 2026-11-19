package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionFramesTest {

    @Test
    void selectsAChronologicalSampleWithinTheConfiguredLimit(@TempDir final Path tempDir) throws IOException {
        for (int i = 1; i <= 10; i++) {
            final Path frame = tempDir.resolve("frame_%04d.png".formatted(i));
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", frame.toFile());
        }

        final List<java.io.File> frames = ProgressionFrames.select(tempDir.toFile(), 3);

        assertEquals(3, frames.size());
        assertEquals("frame_0001.png", frames.get(0).getName());
        assertEquals("frame_0010.png", frames.get(2).getName());
        assertTrue(frames.get(0).getName().compareTo(frames.get(1).getName()) < 0);
        assertTrue(frames.get(1).getName().compareTo(frames.get(2).getName()) < 0);
    }

    @Test
    void rejectsFramesAboveTheConfiguredPixelLimit(@TempDir final Path tempDir) throws IOException {
        final Path frame = tempDir.resolve("frame_0001.png");
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", frame.toFile());

        assertThrows(IOException.class, () -> ProgressionFrames.read(frame.toFile(), 3));
        assertTrue(Files.exists(frame));
    }

    @Test
    void reducesFrameCountToStayWithinTheAnimationPixelBudget(@TempDir final Path tempDir) throws IOException {
        for (int i = 1; i <= 10; i++) {
            final Path frame = tempDir.resolve("frame_%04d.png".formatted(i));
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", frame.toFile());
        }

        final List<java.io.File> frames = ProgressionFrames.select(tempDir.toFile(), 10, 16, 24);

        assertEquals(6, frames.size());
    }

    @Test
    void sortsNumericalFrameIndexesAboveFourDigits(@TempDir final Path tempDir) throws IOException {
        final int[] frameIndexes = {1, 9999, 10000};
        for (final int idx : frameIndexes) {
            final Path frame = tempDir.resolve("frame_%d.png".formatted(idx));
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", frame.toFile());
        }

        final List<java.io.File> frames = ProgressionFrames.select(tempDir.toFile(), 10);

        assertEquals(3, frames.size());
        assertEquals("frame_1.png", frames.get(0).getName());
        assertEquals("frame_9999.png", frames.get(1).getName());
        assertEquals("frame_10000.png", frames.get(2).getName());
    }

    @Test
    void rendersSvgScaledUpToTargetDimensions(@TempDir final Path tempDir) throws IOException {
        final Path svgFile = tempDir.resolve("frame_0001.svg");
        final String svgContent = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\" viewBox=\"0 0 100 100\">" +
                "<rect width=\"100\" height=\"100\" fill=\"#FF0000\" />" +
                "</svg>";
        Files.writeString(svgFile, svgContent);

        final BufferedImage img = ProgressionFrames.renderSvg(svgFile.toFile(), 33554432, 400, 400);
        assertEquals(400, img.getWidth());
        assertEquals(400, img.getHeight());
        // Verify SVG has scaled up and filled the target frame (center & edges are red: 0x00FF0000)
        assertEquals(0x00FF0000, img.getRGB(200, 200) & 0xFFFFFF);
        assertEquals(0x00FF0000, img.getRGB(399, 399) & 0xFFFFFF);
    }

    @Test
    void rasterizesMultipleSvgFramesInParallelPreservingOrderAndMonotonicProgress(@TempDir final Path tempDir) throws IOException {
        final List<java.io.File> svgFrames = new java.util.ArrayList<>(12);
        for (int i = 1; i <= 12; i++) {
            final Path svg = tempDir.resolve("frame_%04d.svg".formatted(i));
            final String hex = String.format("#%02X%02X00", i * 16, 255 - i * 16);
            Files.writeString(svg, "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" viewBox=\"0 0 16 16\">" +
                    "<rect width=\"16\" height=\"16\" fill=\"" + hex + "\" /></svg>");
            svgFrames.add(svg.toFile());
        }

        final Path outDir = tempDir.resolve("raster");
        final List<Integer> reported = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] completed = new boolean[1];
        final ProcessingProgress progress = new ProcessingProgress() {
            @Override
            public void update(final String task, final int percentage) {
                reported.add(percentage);
            }

            @Override
            public void complete(final String task) {
                completed[0] = true;
            }
        };

        final List<java.io.File> pngs = ProgressionFrames.rasterize(svgFrames, outDir.toFile(), "raster", progress, 33554432);

        assertEquals(12, pngs.size());
        assertTrue(completed[0]);
        for (int i = 0; i < 12; i++) {
            assertEquals("frame_%04d.png".formatted(i + 1), pngs.get(i).getName());
            final BufferedImage img = ImageIO.read(pngs.get(i));
            assertEquals(16, img.getWidth());
            assertEquals(16, img.getHeight());
            final int expectedRgb = (((i + 1) * 16) << 16) | ((255 - (i + 1) * 16) << 8);
            assertEquals(expectedRgb, img.getRGB(8, 8) & 0xFFFFFF);
        }
        for (int i = 1; i < reported.size(); i++) {
            assertTrue(reported.get(i) >= reported.get(i - 1),
                    "Progress should be monotonically non-decreasing: " + reported);
        }
    }
}
