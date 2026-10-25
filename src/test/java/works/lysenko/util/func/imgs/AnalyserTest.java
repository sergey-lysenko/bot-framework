package works.lysenko.util.func.imgs;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnalyserTest {

    @Test
    void testAnalyserInstantiation() {
        Analyser analyser = new Analyser();
        assertNotNull(analyser);
    }

    @Test
    void testAnalyse() {
        assertNotNull(Analyser.getMaximallyFarAwayPoints(10));
    }
}
