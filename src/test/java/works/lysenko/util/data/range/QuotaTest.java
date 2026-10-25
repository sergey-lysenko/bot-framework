package works.lysenko.util.data.range;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuotaTest {

    @Test
    void testValidQuotaCreation() {
        final Quota<Integer> quota = Quota.shareOfInteger(10, Fraction.ONE_HALF);
        assertEquals(10, quota.value());
        assertEquals(Fraction.ONE_HALF, quota.min());
        assertEquals(Fraction.ONE_HALF, quota.max());
        assertEquals(0.5, quota.median(), 0.001);
        assertEquals(0.0, quota.width(), 0.001);
    }

    @Test
    void testInvalidMinMaxThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Quota<>(1, null, null, new Fraction(-1), Fraction.ONE_HALF));
        assertThrows(IllegalArgumentException.class, () -> new Quota<>(1, null, null, Fraction.ONE_HALF, new Fraction(2)));
    }

    @Test
    void testActualAbsoluteMargin() {
        final Quota<Integer> quota = new Quota<>(1, null, null, new Fraction(1, 4), new Fraction(3, 4));
        assertEquals(Fraction.ZERO, quota.actualAbsoluteMargin(new Fraction(1, 2)));
        assertEquals(new Fraction(1, 4), quota.actualAbsoluteMargin(Fraction.ZERO));
        assertEquals(new Fraction(1, 4), quota.actualAbsoluteMargin(Fraction.ONE));
    }

    @Test
    void testPercentageAndToString() {
        final Quota<Integer> quota = new Quota<>(1, null, null, Fraction.ZERO, Fraction.ONE);
        assertNotNull(quota.percentage());
        assertNotNull(quota.toString());
        assertNotNull(quota.share());
    }
}
