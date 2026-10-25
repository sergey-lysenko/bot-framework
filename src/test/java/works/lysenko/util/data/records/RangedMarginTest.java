package works.lysenko.util.data.records;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import works.lysenko.util.data.enums.RangedMarginType;
import works.lysenko.util.data.range.FractionRange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static works.lysenko.util.data.enums.RelativeOrAbsolute.ABSOLUTE;

@DisplayName("RangedMargin Record Tests")
class RangedMarginTest {

    @Test
    @DisplayName("Should create default RangedMargin via rm()")
    void testDefaultRm() {
        final RangedMargin margin = RangedMargin.rm();

        assertNotNull(margin);
        assertEquals(RangedMarginType.STATIC, margin.type());
    }

    @Test
    @DisplayName("Should create RangedMargin from fraction")
    void testRmFromFraction() {
        final Fraction f = new Fraction(1, 2);
        final RangedMargin margin = RangedMargin.rm(f);

        assertNotNull(margin);
        assertEquals(RangedMarginType.STATIC, margin.type());
    }

    @Test
    @DisplayName("Should create RangedMargin from RelativeOrAbsoluteFraction")
    void testRmFromRaf() {
        final RelativeOrAbsoluteFraction raf = RelativeOrAbsoluteFraction.raf(Fraction.ONE, ABSOLUTE);
        final RangedMargin margin = RangedMargin.rm(raf);

        assertNotNull(margin);
        assertEquals(RangedMarginType.STATIC, margin.type());
    }

    @Test
    @DisplayName("Should create RangedMargin from two fractions")
    void testRmFromTwoRafs() {
        final RelativeOrAbsoluteFraction raf0 = RelativeOrAbsoluteFraction.raf("a1/2");
        final RelativeOrAbsoluteFraction raf1 = RelativeOrAbsoluteFraction.raf("a3/4");
        final RangedMargin margin = RangedMargin.rm(raf0, raf1);

        assertEquals(RangedMarginType.DYNAMIC, margin.type());
    }

    @Test
    @DisplayName("Should parse string representation into RangedMargin")
    void testRmFromString() {
        final RangedMargin m1 = RangedMargin.rm("a1/2");
        assertNotNull(m1);
        assertEquals(RangedMarginType.STATIC, m1.type());

        final RangedMargin m2 = RangedMargin.rm("a1/10|a9/10");
        assertNotNull(m2);
        assertEquals(RangedMarginType.DYNAMIC, m2.type());
    }

    @Test
    @DisplayName("Should throw exception on invalid string parse")
    void testRmFromStringInvalid() {
        assertThrows(Throwable.class, () -> RangedMargin.rm("a1|a2|a3"));
    }

    @Test
    @DisplayName("Should render ranged margin string")
    void testRender() {
        final RangedMargin staticMargin = RangedMargin.rm(RelativeOrAbsoluteFraction.raf("a1/2"));
        assertNotNull(staticMargin.render());

        final RangedMargin dynamicMargin = RangedMargin.rm(RelativeOrAbsoluteFraction.raf("a1/10"), RelativeOrAbsoluteFraction.raf("a9/10"));
        assertNotNull(dynamicMargin.render());
    }

    @Test
    @DisplayName("Should evaluate doubleValue for base fraction within range")
    void testDoubleValue() {
        final RelativeOrAbsoluteFraction at0 = RelativeOrAbsoluteFraction.raf("a0");
        final RelativeOrAbsoluteFraction at1 = RelativeOrAbsoluteFraction.raf("a10");
        final RangedMargin margin = new RangedMargin(at0, at1, FractionRange.frr(Fraction.ZERO, Fraction.ONE));

        final double val = margin.doubleValue(new Fraction(1, 2));
        assertEquals(5.0, val, 0.001);
    }
}
