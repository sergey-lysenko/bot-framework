package works.lysenko.util.grid.record.rgbc;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.RGB24;

import static org.junit.jupiter.api.Assertions.*;

class HSBTest {

    @Test
    void testHSBInstantiationAndFactory() {
        HSB hsb1 = new HSB(Fraction.ZERO, Fraction.ONE, Fraction.ONE_HALF);
        assertEquals(Fraction.ZERO, hsb1.hue());
        assertEquals(Fraction.ONE, hsb1.saturation());
        assertEquals(Fraction.ONE_HALF, hsb1.brightness());

        HSB hsb2 = HSB.hsb(Fraction.ONE, Fraction.ZERO, Fraction.ZERO);
        assertEquals(Fraction.ONE, hsb2.hue());
        assertEquals(Fraction.ZERO, hsb2.saturation());
        assertEquals(Fraction.ZERO, hsb2.brightness());
    }

    @Test
    void testConvertRGBtoHSB() {
        assertNull(HSB.convertRGBtoHSB(null));
        RGB24 rgb = RGB24.rgb24(0xFF0000);
        HSB hsb = HSB.convertRGBtoHSB(rgb);
        assertNotNull(hsb);
    }
}
