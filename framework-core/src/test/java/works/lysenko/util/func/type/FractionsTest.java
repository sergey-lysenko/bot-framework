package works.lysenko.util.func.type;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FractionsTest {

    @Test
    void constantsHaveExpectedValues() {

        assertEquals(new Fraction(1, 7), Fractions.ONE_SEVENTH);
        assertEquals(new Fraction(1, 8), Fractions.ONE_EIGHTH);
        assertEquals(new Fraction(1, 9), Fractions.ONE_NINTH);
        assertEquals(new Fraction(1, 10), Fractions.ONE_TENTH);
        assertEquals(new Fraction(1, 100), Fractions.ONE_HUNDREDTH);
    }

    @Test
    void deviationIsActualMinusExpected() {

        assertEquals(new Fraction(1, 6), Fractions.dV(new Fraction(1, 3), new Fraction(1, 2)));
        assertEquals(new Fraction(-1, 6), Fractions.dV(new Fraction(1, 2), new Fraction(1, 3)));
        assertEquals(Fraction.ZERO, Fractions.dV(Fraction.ONE_HALF, Fraction.ONE_HALF));
    }

    @Test
    void subscriptAndSuperscriptConversion() {

        assertEquals("₁₂₃", Fractions.toSubscript("123", true));
        assertEquals("¹²³", Fractions.toSuperscript("123", true));
        assertEquals("123", Fractions.toSubscript("123", false));
        assertEquals("123", Fractions.toSuperscript("123", false));
        assertEquals("a₁b", Fractions.toSubscript("a1b", true));
    }

    @Test
    void mappedCharsRoundTrip() {

        final String sub = Fractions.toSubscript("2048", true);
        assertEquals("2048", Fractions.replaceDigitsWithMappedChars(sub, Fractions.subscripts, false));
        final String sup = Fractions.toSuperscript("2048", true);
        assertEquals("2048", Fractions.replaceDigitsWithMappedChars(sup, Fractions.superscripts, false));
    }

    @Test
    void deUnicodeRestoresPlainFraction() {

        assertEquals("1/2", Fractions.deUnicode("¹⁄₂"));
        assertEquals("12/34", Fractions.deUnicode("¹²⁄₃₄"));
    }

    @Test
    void verifyTreatsNonNumbersAsPass() {

        assertTrue(Fractions.verify("not a number", Fraction.ONE_HALF));
    }
}
