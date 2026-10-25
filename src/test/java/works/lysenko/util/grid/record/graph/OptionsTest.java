package works.lysenko.util.grid.record.graph;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OptionsTest {

    @Test
    void testOptionsInstantiationAndFactory() {
        Options options1 = new Options(100, Fraction.ONE, Fraction.ONE_HALF, null, Fraction.ZERO);
        assertEquals(100, options1.width());
        assertEquals(Fraction.ONE, options1.requested());
        assertEquals(Fraction.ONE_HALF, options1.actual());
        assertNull(options1.slack());
        assertEquals(Fraction.ZERO, options1.edge());

        Options options2 = Options.go(200, Fraction.ONE_HALF, Fraction.ONE, null, Fraction.ZERO);
        assertEquals(200, options2.width());
        assertEquals(Fraction.ONE_HALF, options2.requested());
        assertEquals(Fraction.ONE, options2.actual());
    }
}
