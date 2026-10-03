package works.lysenko.util.func.type;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumbersTest {

    @Test
    void doubleOrNull() {

        assertEquals(1.5, Numbers.getDoubleOrNull("1.5"));
        assertNull(Numbers.getDoubleOrNull("x"));
        assertNull(Numbers.getDoubleOrNull(null));
    }

    @Test
    void positivityDistinguishesSignedZero() {

        assertTrue(Numbers.isPositive(0.0));
        assertFalse(Numbers.isPositive(-0.0));
        assertTrue(Numbers.isPositive(3.2));
        assertFalse(Numbers.isPositive(-3.2));
    }

    @Test
    void randomStaysInRange() {

        for (int i = 0; i < 200; i++) {
            final int n = Numbers.random(3, 8);
            assertTrue(n >= 3 && n <= 8, "out of range: " + n);
        }
    }

    @Test
    void randomHonoursExclusions() {

        for (int i = 0; i < 200; i++) {
            final int n = Numbers.random(0, 4, 0, 1, 2);
            assertTrue(n == 3 || n == 4, "excluded value returned: " + n);
        }
    }

    @Test
    void randomNormalRejectsInvalidInput() {

        assertNull(Numbers.randomNormal(null, 0.5, 10));
        assertNull(Numbers.randomNormal(0.5, null, 10));
        assertNull(Numbers.randomNormal(0.5, 0.5, 10));
        assertNull(Numbers.randomNormal(0.7, 0.2, 10));
        assertNull(Numbers.randomNormal(0.1, 0.9, 0));
        assertNull(Numbers.randomNormal(0.1, 1.5, 10));
        assertNull(Numbers.randomNormal(-0.1, 0.5, 10));
    }

    @Test
    void randomNormalStaysInside() {

        for (int i = 0; i < 100; i++) {
            final Fraction f = Numbers.randomNormal(0.2, 0.8, 10);
            assertNotNull(f);
            assertTrue(f.doubleValue() >= 0.2 && f.doubleValue() <= 0.8, "out of range: " + f);
        }
    }

    @Test
    void weightedIndexSkipsZeroWeights() {

        for (int i = 0; i < 200; i++) {
            assertEquals(1, Numbers.getRandomItemIndex(0, 5, 0));
        }
    }

    @Test
    void weightedIndexRejectsEmpty() {

        assertThrows(IllegalArgumentException.class, Numbers::getRandomItemIndex);
        assertThrows(IllegalArgumentException.class, () -> Numbers.getRandomItemIndex(0, 0));
    }
}
