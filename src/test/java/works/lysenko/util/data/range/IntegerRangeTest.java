package works.lysenko.util.data.range;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegerRangeTest {

    @Test
    void singleValueIsStrictRange() {

        final IntegerRange r = new IntegerRange("5", 0, 100);
        assertEquals(5, r.min());
        assertEquals(5, r.max());
        assertTrue(r.includes(5));
        assertFalse(r.includes(6));
    }

    @Test
    void minMaxPairIsParsed() {

        final IntegerRange r = new IntegerRange("5|10", 0, 100);
        assertEquals(5, r.min());
        assertEquals(10, r.max());
    }

    @Test
    void reversedPairIsNormalised() {

        final IntegerRange r = new IntegerRange("10|5", 0, 100);
        assertEquals(5, r.min());
        assertEquals(10, r.max());
    }

    @Test
    void plusMeansUpToGlobalMax() {

        final IntegerRange r = new IntegerRange("5+", 0, 100);
        assertEquals(5, r.min());
        assertEquals(100, r.max());
    }

    @Test
    void minusMeansDownToGlobalMin() {

        final IntegerRange r = new IntegerRange("5-", 0, 100);
        assertEquals(0, r.min());
        assertEquals(5, r.max());
    }

    @Test
    void garbageGivesNullRange() {

        final IntegerRange r = new IntegerRange("abc", 0, 100);
        assertNull(r.get());
        assertNull(r.min());
        assertNull(r.max());
        assertNull(r.margin(1));
    }

    @Test
    void numericConstructorsBuildRanges() {

        assertEquals(3, new IntegerRange(3).min());
        assertEquals(3, new IntegerRange(3).max());
        assertEquals(2, new IntegerRange(2, 7).min());
        assertEquals(7, new IntegerRange(2, 7).max());
    }

    @Test
    void marginIsZeroInsideAndSignedOutside() {

        final IntegerRange r = new IntegerRange(5, 10);
        assertEquals(0, r.margin(5));
        assertEquals(0, r.margin(7));
        assertEquals(0, r.margin(10));
        assertEquals(2, r.margin(12));
        assertEquals(-2, r.margin(3));
    }

    @Test
    void toStringRendersCompactForm() {

        assertEquals("5", new IntegerRange(5).toString());
        assertEquals("5|10", new IntegerRange(5, 10).toString());
        // Open-ended ranges render with the delimiter, e.g. "5|+" (differs from the "5+" input syntax)
        assertEquals("5|+", new IntegerRange("5+").toString());
        assertEquals("5|-", new IntegerRange("5-").toString());
    }
}
