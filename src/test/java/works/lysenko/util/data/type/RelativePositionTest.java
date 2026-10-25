package works.lysenko.util.data.type;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelativePositionTest {

    @Test
    void testConstructorsAndFactories() {
        final RelativePosition rp = RelativePosition.rp(1, 2, 1, 4);
        assertEquals(0.5, rp.h(), 0.001);
        assertEquals(0.25, rp.v(), 0.001);
        assertEquals(Fraction.ONE_HALF, rp.horizontal());

        final RelativePosition rpDouble = RelativePosition.rp(0.5, 0.5);
        assertEquals(0.5, rpDouble.hor(), 0.001);
        assertEquals(0.5, rpDouble.ver(), 0.001);

        assertEquals(0.5, RelativePosition.CENTRE.h(), 0.001);
    }
}
