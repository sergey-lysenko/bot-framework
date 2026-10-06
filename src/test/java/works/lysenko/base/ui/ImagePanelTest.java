package works.lysenko.base.ui;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImagePanelTest {

    @Test
    void fitsLargeImagesWithinBoundsWhilePreservingAspectRatio() {

        final ImagePanel panel = new ImagePanel();
        panel.setImage(new BufferedImage(3840, 2160, BufferedImage.TYPE_INT_ARGB));

        panel.fitTo(new Dimension(1200, 700));

        assertEquals(0.3125F, panel.getScale());
        assertEquals(new Dimension(1200, 675), panel.getPreferredSize());
    }

    @Test
    void doesNotUpscaleImagesThatAlreadyFit() {

        final ImagePanel panel = new ImagePanel();
        panel.setImage(new BufferedImage(1200, 675, BufferedImage.TYPE_INT_ARGB));

        panel.fitTo(new Dimension(3840, 2160));

        assertEquals(1.0F, panel.getScale());
        assertEquals(new Dimension(1200, 675), panel.getPreferredSize());
    }
}
