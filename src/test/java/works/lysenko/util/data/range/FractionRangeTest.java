package works.lysenko.util.data.range;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FractionRangeTest {

    private static final Fraction ZERO = Fraction.ZERO;
    private static final Fraction ONE = Fraction.ONE;
    private static final Fraction QUARTER = new Fraction(1, 4);
    private static final Fraction HALF = new Fraction(1, 2);
    private static final Fraction THREE_QUARTERS = new Fraction(3, 4);

    @Test
    void pairConstructorKeepsBounds() {

        final FractionRange r = new FractionRange(QUARTER, THREE_QUARTERS);
        assertEquals(QUARTER, r.min());
        assertEquals(THREE_QUARTERS, r.max());
    }

    @Test
    void singleValueRangeIsStrict() {

        final FractionRange r = new FractionRange(HALF);
        assertTrue(r.includes(HALF));
        assertFalse(r.includes(QUARTER));
    }

    @Test
    void includesIsInclusiveOnBothEnds() {

        final FractionRange r = new FractionRange(QUARTER, THREE_QUARTERS);
        assertTrue(r.includes(QUARTER));
        assertTrue(r.includes(HALF));
        assertTrue(r.includes(THREE_QUARTERS));
        assertFalse(r.includes(ZERO));
        assertFalse(r.includes(ONE));
    }

    @Test
    void marginIsZeroInsideAndSignedOutside() {

        final FractionRange r = new FractionRange(QUARTER, HALF);
        assertEquals(ZERO, r.margin(QUARTER));
        assertEquals(ZERO, r.margin(new Fraction(3, 8)));
        assertEquals(0.25, r.margin(THREE_QUARTERS).doubleValue(), 1e-9);
        assertEquals(-0.25, r.margin(ZERO).doubleValue(), 1e-9);
    }

    @Test
    void extendingRangeToIncludeValue() {

        final FractionRange base = new FractionRange(QUARTER, HALF);

        final FractionRange up = new FractionRange(base, THREE_QUARTERS);
        assertEquals(QUARTER, up.min());
        assertEquals(THREE_QUARTERS, up.max());

        final FractionRange down = new FractionRange(base, ZERO);
        assertEquals(ZERO, down.min());
        assertEquals(HALF, down.max());

        final FractionRange same = new FractionRange(base, new Fraction(3, 8));
        assertEquals(QUARTER, same.min());
        assertEquals(HALF, same.max());
    }

    @Test
    void emptyBaseStaysEmptyWhenExtended() {

        final FractionRange empty = new FractionRange((org.apache.commons.lang3.Range<Fraction>) null);
        assertNull(empty.min());
        assertNull(empty.max());
        assertNull(new FractionRange(empty, HALF).get());
    }

    @Test
    void parsesPairAndNormalisesOrder() {

        final FractionRange r = FractionRange.frr("3/4|1/4", ZERO, ONE);
        assertEquals(QUARTER, r.min());
        assertEquals(THREE_QUARTERS, r.max());
    }

    @Test
    void parsesSingleValue() {

        final FractionRange r = FractionRange.frr("1/2", ZERO, ONE);
        assertEquals(HALF, r.min());
        assertEquals(HALF, r.max());
    }

    @Test
    void parsesOpenEndedForms() {

        final FractionRange more = FractionRange.frr("1/2+", ZERO, ONE);
        assertEquals(HALF, more.min());
        assertEquals(ONE, more.max());

        final FractionRange less = FractionRange.frr("1/2-", ZERO, ONE);
        assertEquals(ZERO, less.min());
        assertEquals(HALF, less.max());
    }

    @Test
    void staticFactoryMatchesConstructor() {

        final FractionRange r = FractionRange.frr(QUARTER, HALF);
        assertEquals(QUARTER, r.min());
        assertEquals(HALF, r.max());
    }
}
