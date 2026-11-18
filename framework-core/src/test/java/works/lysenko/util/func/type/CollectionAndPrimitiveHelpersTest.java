package works.lysenko.util.func.type;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CollectionAndPrimitiveHelpersTest {

    @Test
    void randomListElementIsConsistent() {

        final List<String> list = List.of("a", "b", "c");
        for (int i = 0; i < 100; i++) {
            final RandomListElement<String> e = Lists.getRandomListElement(list);
            assertEquals(list.get(e.index()), e.value());
        }
    }

    @Test
    void randomSetElementIsConsistent() {

        final Set<Integer> set = new LinkedHashSet<>(List.of(10, 20, 30));
        for (int i = 0; i < 100; i++) {
            final RandomListElement<Integer> e = Sets.getRandomSetElement(set);
            assertTrue(set.contains(e.value()));
        }
    }

    @Test
    void randomSetElementRejectsEmptyAndNull() {

        assertThrows(IllegalArgumentException.class, () -> Sets.getRandomSetElement(Set.of()));
        assertThrows(IllegalArgumentException.class, () -> Sets.getRandomSetElement(null));
    }

    @Test
    void firstOfCollection() {

        assertEquals("a", Strings.firstOf(List.of("a", "b")));
        assertThrows(NoSuchElementException.class, () -> Strings.firstOf(List.of()));
    }

    @Test
    void trailingZerosAreStrippedUpToLimit() {

        assertEquals("1.23", Strings.stripExcessZeros("1.2300", 2));
        assertEquals("1.230", Strings.stripExcessZeros("1.2300", 1));
        assertEquals("1.23", Strings.stripExcessZeros("1.23", 3));
    }

    @Test
    void frenchLocaleDoubleParsing() throws ParseException {

        assertEquals(1.5, Doubles.parseDouble("1,5"));
        assertEquals(42.0, Doubles.parseDouble("42"));
    }

    @Test
    void booleanOrNull() {

        assertTrue(Booleans.getBooleanOrNull("true"));
        assertFalse(Booleans.getBooleanOrNull("false"));
        assertNull(Booleans.getBooleanOrNull("TRUE"));
        assertNull(Booleans.getBooleanOrNull("yes"));
        assertNull(Booleans.getBooleanOrNull(null));
    }

    @Test
    void probabilityExtremesAreDeterministic() {

        for (int i = 0; i < 50; i++) {
            assertTrue(Booleans.isTrue(1.0));
            assertFalse(Booleans.isTrue(0.0));
            assertTrue(Booleans.isTrue(Fraction.ONE));
            assertFalse(Booleans.isTrue(Fraction.ZERO));
        }
    }

    @Test
    void probabilityOutsideUnitIntervalIsRejected() {

        assertThrows(IllegalArgumentException.class, () -> Booleans.isTrue(1.5));
        assertThrows(IllegalArgumentException.class, () -> Booleans.isTrue(-0.1));
    }

    @Test
    void booleansPackIntoChar() {

        assertEquals(0, Chars.booleansToChar());
        assertEquals(1, Chars.booleansToChar(true));
        assertEquals(5, Chars.booleansToChar(true, false, true));
    }

    @Test
    void charUnpacksIntoBooleans() {

        final boolean[] bits = Chars.charToBooleans((char) 5);
        assertEquals(Character.SIZE, bits.length);
        assertTrue(bits[0]);
        assertFalse(bits[1]);
        assertTrue(bits[2]);
        assertFalse(bits[3]);
    }

    @Test
    void charBooleanRoundTrip() {

        final boolean[] source = {true, true, false, true, false, false, true};
        final boolean[] back = Chars.charToBooleans(Chars.booleansToChar(source));
        for (int i = 0; i < source.length; i++) assertEquals(source[i], back[i]);
    }
}
