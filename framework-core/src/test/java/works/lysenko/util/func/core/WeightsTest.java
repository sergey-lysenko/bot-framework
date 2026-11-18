package works.lysenko.util.func.core;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightsTest {

    @Test
    void testDownstreamWeightNullProperties() {
        Fraction f = Weights.downstreamWeight(null);
        assertEquals(Fraction.ZERO, f);
    }
}
