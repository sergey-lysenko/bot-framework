package works.lysenko.util.grid.record.gsrc;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RegionTest {

    @Test
    void testSearchNullRequest() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ColoursSearchResult result = Region.search(null, img);
        assertNull(result);
    }

    @Test
    void testSearchValidRequest() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ColourSearchRequest csr = ColourSearchRequest.csr(null, Collections.emptyList());
        ColoursSearchResult result = Region.search(csr, img);
        assertNotNull(result);
        assertNotNull(result.regions());
    }
}
