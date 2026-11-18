package works.lysenko.util.grid.record.rgbc;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.RGB24;

import static org.junit.jupiter.api.Assertions.*;

class ColourTest {

    @Test
    void testByNameAndValue() {
        assertNull(Colour.byName(null));
        assertNull(Colour.byName("nonexistent_colour_name_xyz"));
        assertNull(Colour.rgbByName(null));
        assertNull(Colour.rgbByName(null, true));
    }

    @Test
    void testDistance() {
        RGB24 c1 = RGB24.rgb24(0x000000);
        RGB24 c2 = RGB24.rgb24(0x030400);
        double dist = Colour.distance(c1, c2);
        assertEquals(5.0, dist, 1e-9);
    }

    @Test
    void testStringRepresentations() {
        Colour colour = new Colour(0x00FF00);
        assertNotNull(colour.toShortString());
        assertNotNull(colour.toLongString());
        assertNotNull(Colour.shortOfValue(0x00FF00));
        assertNotNull(Colour.longOfValue(0x00FF00));
    }
}
