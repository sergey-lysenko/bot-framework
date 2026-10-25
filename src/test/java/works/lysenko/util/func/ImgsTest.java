package works.lysenko.util.func;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImgsTest {

    @Test
    void testInstantiation() {
        Imgs imgs = new Imgs();
        assertNotNull(imgs);
    }

    @Test
    void testAreSameImages() {
        BufferedImage img1 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage img2 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage img3 = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);

        assertTrue(Imgs.areSameImages(img1, img2));
        assertFalse(Imgs.areSameImages(img1, img3));

        img2.setRGB(0, 0, Color.RED.getRGB());
        assertFalse(Imgs.areSameImages(img1, img2));
    }

    @Test
    void testAreSimilarImages() {
        BufferedImage img1 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage img2 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage img3 = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);

        assertTrue(Imgs.areSimilarImages(img1, img2, 0));
        assertFalse(Imgs.areSimilarImages(img1, img3, 0));

        img2.setRGB(0, 0, new Color(10, 10, 10).getRGB());
        assertFalse(Imgs.areSimilarImages(img1, img2, 5));
        assertTrue(Imgs.areSimilarImages(img1, img2, 15));
    }

    @Test
    void testAreSimilarImagesGridBased() {
        BufferedImage img1 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage img2 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage img3 = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);

        assertTrue(Imgs.areSimilarImagesGridBased(img1, img2, 0));
        assertFalse(Imgs.areSimilarImagesGridBased(img1, img3, 0));
    }

    @Test
    void testAreIdentical() {
        BufferedImage img1 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage img2 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

        assertTrue(Imgs.areIdentical(img1, img2));
    }
}
