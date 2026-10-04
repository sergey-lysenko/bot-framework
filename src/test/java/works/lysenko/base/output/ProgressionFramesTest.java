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
}
