package works.lysenko.util.grid.record.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IgnoreHSBTest {

    @Test
    void testIgnoreHSBFactories() {
        IgnoreHSB hsb1 = IgnoreHSB.of();
        assertNull(hsb1.hue());
        assertNull(hsb1.saturation());
        assertNull(hsb1.brightness());

        IgnoreHSB hsb2 = IgnoreHSB.of(null, null, null);
        assertNull(hsb2.hue());
        assertNull(hsb2.saturation());
        assertNull(hsb2.brightness());
    }
}
