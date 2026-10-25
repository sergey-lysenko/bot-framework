package works.lysenko.util.grid.record.misc;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class DrawGridResultTest {

    @Test
    void testDrawGridResultInstantiation() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        DrawGridResult res = new DrawGridResult(img, 5, 100);
        assertEquals(img, res.image());
        assertEquals(5, res.samples());
        assertEquals(100, res.area());
    }
}
