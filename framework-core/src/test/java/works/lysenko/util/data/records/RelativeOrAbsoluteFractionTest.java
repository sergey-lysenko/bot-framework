package works.lysenko.util.data.records;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.RelativeOrAbsolute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RelativeOrAbsoluteFractionTest {

    @Test
    void testRafParsingAndFactory() {
        final RelativeOrAbsoluteFraction raf0 = RelativeOrAbsoluteFraction.raf();
        assertEquals(Fraction.ZERO, raf0.fraction());
        assertEquals(RelativeOrAbsolute.ABSOLUTE, raf0.type());

        final RelativeOrAbsoluteFraction rafAbs = RelativeOrAbsoluteFraction.raf("a1/2");
        assertEquals(0.5, rafAbs.fraction().doubleValue(), 0.001);
        assertEquals(RelativeOrAbsolute.ABSOLUTE, rafAbs.type());

        final RelativeOrAbsoluteFraction rafRel = RelativeOrAbsoluteFraction.raf("r1/2");
        assertEquals(0.5, rafRel.fraction().doubleValue(), 0.001);
        assertEquals(RelativeOrAbsolute.RELATIVE, rafRel.type());
    }

    @Test
    void testDoubleValueCalculation() {
        final RelativeOrAbsoluteFraction rafRel = RelativeOrAbsoluteFraction.raf("r1/2");
        assertEquals(0.25, rafRel.doubleValue(Fraction.ONE_HALF), 0.001);

        final RelativeOrAbsoluteFraction rafAbs = RelativeOrAbsoluteFraction.raf("a1/2");
        assertEquals(0.5, rafAbs.doubleValue(Fraction.ONE_HALF), 0.001);
    }

    @Test
    void testRenderAndData() {
        final RelativeOrAbsoluteFraction rafAbs = RelativeOrAbsoluteFraction.raf("a1/2");
        assertNotNull(rafAbs.data());
        assertNotNull(rafAbs.render());
    }
}
