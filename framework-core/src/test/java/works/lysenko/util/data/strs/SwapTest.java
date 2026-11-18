package works.lysenko.util.data.strs;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import works.lysenko.util.apis.grid.t._Value;
import works.lysenko.util.data.enums.Marker;
import works.lysenko.util.data.range.FractionRange;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwapTest {

    @Test
    void doubleFormatting() {

        assertEquals("3.14", Swap.d(3.14159, 2));
    }

    @Test
    void formatShortcut() {

        assertEquals("a-3", Swap.f("%s-%d", "a", 3));
    }

    @Test
    void doubleRoundsToInteger() {

        assertEquals(3, Swap.i(2.6));
        assertEquals(2, Swap.i(2.4));
        assertEquals(2, Swap.i(1.6f));
        assertEquals(0, Swap.i((Double) 1e12));
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
    void quoteIfNotNull() {

        assertEquals("'hello'", Swap.qinn("hello", "default"));
        assertEquals("default", Swap.qinn(null, "default"));
        assertEquals("", Swap.qinn(null, null));
    }

    @Test
    void repeatKeys() {

        assertEquals("\uE015\uE015\uE015", Swap.r(Keys.ARROW_DOWN, 3));
        assertEquals("", Swap.r(Keys.ENTER, 0));
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

    @Test
    void markerPairConcatenation() {

        assertEquals("from 'true' to 'false'", Swap.s2(Marker.FROM_TO, true, false));
    }

    @Test
    void keyVisualization() {

        assertEquals("↓", Swap.v(Keys.ARROW_DOWN));
        assertEquals("↑", Swap.v(Keys.ARROW_UP));
        assertEquals(Keys.ENTER.toString(), Swap.v(Keys.ENTER));
    }

    @Test
    void valueWrapping() {

        final _Value<?> valFraction = Swap.v(Fraction.ONE_HALF);
        assertNotNull(valFraction);

        final _Value<?> valRange = Swap.v(new FractionRange(Fraction.ZERO, Fraction.ONE));
        assertNotNull(valRange);
    }
}

