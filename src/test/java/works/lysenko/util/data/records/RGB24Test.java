package works.lysenko.util.data.records;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RGB24Test {

    @Test
    void testRgb24Conversion() {
        assertNull(RGB24.rgb24(null));

        final int rgbVal = 0xFF8040; // R=255, G=128, B=64
        final RGB24 color = RGB24.rgb24(rgbVal);
        assertNotNull(color);
        assertEquals(255, color.red());
        assertEquals(128, color.green());
        assertEquals(64, color.blue());
    }

    @Test
    void testStringRepresentations() {
        final int rgbVal = 0xFF8040;
        final String staticLong = RGB24.longRgb(rgbVal);
        assertNotNull(staticLong);

        final String staticShort = RGB24.shortRgb(rgbVal);
        assertNotNull(staticShort);

        final RGB24 color = new RGB24(255, 128, 64);
        assertNotNull(color.longRgb());
        assertNotNull(color.shortRgb());
    }
}
