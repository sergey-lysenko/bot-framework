package works.lysenko.util.func.imgs;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Rectangle;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class BufferedImagesTest {

    @Test
    void testGetCroppedWithValidBounds() {
        final BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        final Rectangle rect = new Rectangle(10, 10, 50, 50);
        final BufferedImage cropped = BufferedImages.getCropped(img, rect, true);
        assertNotNull(cropped);
        assertEquals(50, cropped.getWidth());
        assertEquals(50, cropped.getHeight());
    }

    @Test
    void testGetCroppedWithZeroOrNegativeDimensions() {
        final BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

        // Zero width
        assertNull(BufferedImages.getCropped(img, new Rectangle(10, 10, 0, 50), true));
        // Zero height
        assertNull(BufferedImages.getCropped(img, new Rectangle(10, 10, 50, 0), true));
        // Negative width
        assertNull(BufferedImages.getCropped(img, new Rectangle(10, 10, -5, 50), true));
        // Negative height
        assertNull(BufferedImages.getCropped(img, new Rectangle(10, 10, 50, -5), true));
        // Null rectangle
        assertNull(BufferedImages.getCropped(img, null, true));
        // Null image
        assertNull(BufferedImages.getCropped(null, new Rectangle(10, 10, 50, 50), true));
    }

    @Test
    void testGetCroppedOutOfBoundsClamping() {
        final BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

        // Rectangle completely outside
        assertNull(BufferedImages.getCropped(img, new Rectangle(200, 200, 50, 50), true));

        // Rectangle partially outside (should clamp to remaining image)
        final Rectangle partiallyOutside = new Rectangle(80, 80, 50, 50);
        final BufferedImage cropped = BufferedImages.getCropped(img, partiallyOutside, true);
        assertNotNull(cropped);
        assertEquals(20, cropped.getWidth());
        assertEquals(20, cropped.getHeight());
    }
}
