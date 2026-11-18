package works.lysenko.util.func.type.fractions;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FractionsFactoryTest {

    @Test
    void testFr() {
        Fraction f = Factory.fr(0.5);
        assertEquals(1, f.getNumerator());
        assertEquals(2, f.getDenominator());
    }
}
