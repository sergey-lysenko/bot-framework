package works.lysenko.util.grid.record.misc;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LimitsTest {

    @Test
    void testLimitsInstantiationAndFactories() {
        Limits limits1 = Limits.of();
        assertEquals(Fraction.ZERO, limits1.border());
        assertNull(limits1.margin());
        assertEquals(Fraction.ZERO, limits1.cutoff());

        Limits limits2 = Limits.of(Fraction.ONE_HALF, null);
        assertEquals(Fraction.ONE_HALF, limits2.border());
        assertEquals(Fraction.ONE_HALF, limits2.cutoff());
    }
}
