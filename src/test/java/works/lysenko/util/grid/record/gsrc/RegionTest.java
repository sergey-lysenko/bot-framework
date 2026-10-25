package works.lysenko.util.grid.record.gsrc;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.type.RelativePosition;

import java.awt.image.BufferedImage;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RegionTest {

    @Test
    void testRegionInstantiation() {
        RelativePosition pos = new RelativePosition(Fraction.ONE_HALF, Fraction.ONE_HALF);
        Region region = new Region(pos, Fraction.ONE, Fraction.ONE);
        assertEquals(pos, region.position());
        assertEquals(Fraction.ONE, region.scale());
        assertEquals(Fraction.ONE, region.aspect());
    }

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
