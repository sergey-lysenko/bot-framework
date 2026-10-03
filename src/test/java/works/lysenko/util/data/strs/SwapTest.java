package works.lysenko.util.data.strs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwapTest {

    @Test
    void formatShortcut() {

        assertEquals("a-3", Swap.f("%s-%d", "a", 3));
    }

    @Test
    void doubleRoundsToInteger() {

        assertEquals(3, Swap.i(2.6));
        assertEquals(2, Swap.i(2.4));
        assertEquals(2, Swap.i(1.6f));
    }

    @Test
    void stringParsesWithTrimming() {

        assertEquals(42, Swap.i("  42 "));
        assertNull(Swap.i((String) null));
        assertThrows(NumberFormatException.class, () -> Swap.i("x"));
    }

    @Test
    void nonNullIntegerParsing() {

        assertEquals(7, Swap.inn(" 7 "));
        assertThrows(IllegalArgumentException.class, () -> Swap.inn(null));
        assertThrows(NumberFormatException.class, () -> Swap.inn("seven"));
    }

    @Test
    void concatenationJoinsWithoutSeparator() {

        assertEquals("1a2.5", Swap.s(1, "a", 2.5));
        assertEquals("[a, b]", Swap.s(List.of("a", "b")));
    }

    @Test
    void nullTolerantConcatenation() {

        assertEquals("nulla", Swap.sn(null, "a"));
        assertEquals("ab", Swap.sn("a", "b"));
    }

    @Test
    void pluralSuffix() {

        assertEquals("", Swap.s1(1));
        assertEquals("s", Swap.s1(2));
        assertEquals("s", Swap.s1(0));
        assertEquals("1 cat", Swap.s1(1L, "cat"));
        assertEquals("2 cats", Swap.s1(2L, "cat"));
    }
}
