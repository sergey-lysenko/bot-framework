package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NumbersTest {

    @Test
    void testNamedNumbers() {
        assertEquals(0, Numbers.ZERO);
        assertEquals(1, Numbers.ONE);
        assertEquals(2, Numbers.TWO);
        assertEquals(3, Numbers.THREE);
        assertEquals(4, Numbers.FOUR);
        assertEquals(5, Numbers.FIVE);
        assertEquals(6, Numbers.SIX);
        assertEquals(7, Numbers.SEVEN);
        assertEquals(8, Numbers.EIGHT);
        assertEquals(9, Numbers.NINE);
        assertEquals(10, Numbers.TEN);
        assertEquals(100, Numbers.HUNDRED);
        assertNotNull(Numbers.ONE_EIGHTS);
        assertNotNull(Numbers.SEVEN_EIGHTS);
        assertEquals(1, Numbers.ONCE);
        assertEquals(2, Numbers.TWICE);
        assertEquals(3, Numbers.THRICE);
        assertEquals(1, Numbers.ABSOLUTE);
        assertNotNull(new Numbers());
    }
}

